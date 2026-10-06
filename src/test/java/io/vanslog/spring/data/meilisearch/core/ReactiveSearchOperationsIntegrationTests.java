/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.vanslog.spring.data.meilisearch.core;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meilisearch.sdk.FederationOptions;
import com.meilisearch.sdk.exceptions.MeilisearchApiException;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import com.meilisearch.sdk.model.TaskInfo;

import io.vanslog.spring.data.meilisearch.annotations.Document;
import io.vanslog.spring.data.meilisearch.annotations.Embedder;
import io.vanslog.spring.data.meilisearch.annotations.Setting;
import io.vanslog.spring.data.meilisearch.client.ClientConfiguration;
import io.vanslog.spring.data.meilisearch.client.MeilisearchClient;
import io.vanslog.spring.data.meilisearch.client.msc.ReactiveMeilisearchTemplate;
import io.vanslog.spring.data.meilisearch.core.convert.MeilisearchConverter;
import io.vanslog.spring.data.meilisearch.core.federation.FacetMergeOptions;
import io.vanslog.spring.data.meilisearch.core.federation.Federation;
import io.vanslog.spring.data.meilisearch.core.query.BaseQuery;
import io.vanslog.spring.data.meilisearch.core.query.BasicQuery;
import io.vanslog.spring.data.meilisearch.core.query.FacetQuery;
import io.vanslog.spring.data.meilisearch.core.query.IndexQuery;
import io.vanslog.spring.data.meilisearch.core.query.SimilarQuery;
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTest;
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTestConfiguration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Live Meilisearch behavior coverage for reactive search operations.
 *
 * @author Junghoon Ban
 */
// GH-214
@MeilisearchTest
@ContextConfiguration(classes = { MeilisearchTestConfiguration.class,
		ReactiveSearchOperationsIntegrationTests.ReactiveOperationsTestConfiguration.class })
class ReactiveSearchOperationsIntegrationTests {

	private static final Duration BLOCK_TIMEOUT = Duration.ofSeconds(30);
	private static final String BOOKS_INDEX = "reactive-search-books";
	private static final String COMICS_INDEX = "reactive-search-comics";
	private static final String VECTORS_INDEX = "reactive-search-vectors";

	@Autowired MeilisearchClient meilisearchClient;
	@Autowired MeilisearchOperations blockingOperations;
	@Autowired ReactiveMeilisearchOperations reactiveOperations;

	@BeforeEach
	void setUpIndexes() throws MeilisearchException {
		for (Class<?> entityType : List.of(SearchBook.class, SearchComic.class, UserVectorDocument.class)) {
			String indexUid = entityType.getAnnotation(Document.class).indexUid();
			deleteIndexIfExists(indexUid);
			blockingOperations.indexOps(entityType).create(new MeilisearchIndexCreateRequest("id"));
			blockingOperations.applySettings(entityType);
		}
	}

	@AfterEach
	void deleteIndexes() throws MeilisearchException {
		deleteIndexIfExists(BOOKS_INDEX);
		deleteIndexIfExists(COMICS_INDEX);
		deleteIndexIfExists(VECTORS_INDEX);
	}

	@Test
	void shouldReturnColdFilteredPagedSearchWithTotalsFacetsAndMappedContent() {
		reactiveOperations.saveAll(Flux.just(book("book-1", "Hero Alpha", "drama"), book("book-3", "Hero Charlie", "drama"),
				book("book-4", "Hero Delta", "action"))).collectList().block(BLOCK_TIMEOUT);

		BasicQuery query = BasicQuery.builder().withQ("hero").withFilter("category = drama").withFacets("category")
				.withSort(Sort.by("title")).withPageable(PageRequest.of(1, 1)).build();
		Mono<SearchHits<SearchBook>> resultPublisher = reactiveOperations.search(query, SearchBook.class);

		// A cold search observes the index state when subscribed, not when the publisher is assembled.
		reactiveOperations.save(book("book-2", "Hero Bravo", "drama")).block(BLOCK_TIMEOUT);
		SearchHits<SearchBook> result = resultPublisher.block(BLOCK_TIMEOUT);

		assertThat(result).isNotNull();
		assertThat(result.getTotalHits()).isEqualTo(3);
		assertThat(result.getSearchHits()).hasSize(1);
		assertThat(result.getSearchHit(0).getContent().getId()).isEqualTo("book-2");
		assertThat(result.getSearchHit(0).getQuery()).isEqualTo("hero");
		assertThat(asMap(result.getSearchHit(0).getFacetDistribution()).get("category")).isEqualTo(Map.of("drama", 3));
	}

	@Test
	void shouldPreserveEachNonFederatedMultiSearchQueryAndFacetMetadata() {
		reactiveOperations
				.saveAll(Flux.just(book("book-1", "Hero one", "drama"), book("book-2", "Hero two", "action"),
						book("book-3", "Quiet story", "drama"), book("book-4", "Hero excluded", "horror")))
				.collectList().block(BLOCK_TIMEOUT);
		List<BaseQuery> queries = List.of(BasicQuery.builder().withQ("hero")
				.withFilterArray(new String[][] { { "category = drama", "category = action" } }).withFacets("category").build(),
				BasicQuery.builder().withQ("quiet").withFacets("category").build());

		SearchHits<SearchBook> result = reactiveOperations.multiSearch(queries, SearchBook.class).block(BLOCK_TIMEOUT);

		assertThat(result).isNotNull();
		assertThat(result.getSearchHits()).extracting(SearchHit::getQuery).containsExactly("hero", "hero", "quiet");
		assertThat(result.getSearchHits()).allSatisfy(hit -> {
			assertThat(hit.getFacetDistribution()).isInstanceOf(Map.class);
			assertThat(asMap(hit.getFacetDistribution()).get("category")).isInstanceOf(Map.class);
		});
		assertThat(asMap(result.getSearchHits().get(0).getFacetDistribution()).get("category"))
				.isEqualTo(Map.of("action", 1, "drama", 1));
	}

	@Test
	void shouldPreserveFederatedWeightsHitMetadataAndPerIndexFacets() {
		reactiveOperations.save(book("book-1", "hero", "drama")).block(BLOCK_TIMEOUT);
		reactiveOperations.save(comic("comic-1", "hero", "comics")).block(BLOCK_TIMEOUT);
		List<IndexQuery> queries = List.of(
				IndexQuery.builder().withQ("hero").withIndexUid(BOOKS_INDEX)
						.withFederationOptions(new FederationOptions().setWeight(1.0)).build(),
				IndexQuery.builder().withQ("hero").withIndexUid(COMICS_INDEX)
						.withFederationOptions(new FederationOptions().setWeight(3.0)).build());
		Federation federation = new Federation(10, 0,
				Map.of(BOOKS_INDEX, List.of("category"), COMICS_INDEX, List.of("category")), null);

		SearchHits<SearchBook> result = reactiveOperations.multiSearch(queries, federation, SearchBook.class)
				.block(BLOCK_TIMEOUT);

		assertThat(result).isNotNull();
		assertThat(result.getSearchHits()).hasSize(2);
		assertThat(result.getSearchHit(0).getFederation()).isNotNull();
		assertThat(result.getSearchHit(0).getFederation().getIndexUid()).isEqualTo(COMICS_INDEX);
		assertThat(result.getSearchHit(0).getFederation().getWeightedRankingScore()).isNotNull().isPositive();
		assertThat(result.getSearchHits()).allSatisfy(hit -> assertThat(hit.getFederation()).isNotNull());

		Map<?, ?> facetMetadata = asMap(result.getSearchHit(0).getFacetDistribution());
		Map<?, ?> facetsByIndex = asMap(facetMetadata.get("facetsByIndex"));
		Map<?, ?> booksFacets = asMap(facetsByIndex.get(BOOKS_INDEX));
		assertThat(asMap(booksFacets.get("distribution")).get("category")).isEqualTo(Map.of("drama", 1));
		Map<?, ?> comicsFacets = asMap(facetsByIndex.get(COMICS_INDEX));
		assertThat(asMap(comicsFacets.get("distribution")).get("category")).isEqualTo(Map.of("comics", 1));

		Federation merged = new Federation(10, 0, federation.facetsByIndex(), new FacetMergeOptions(10));
		SearchHits<SearchBook> mergedResult = reactiveOperations.multiSearch(queries, merged, SearchBook.class)
				.block(BLOCK_TIMEOUT);
		assertThat(asMap(mergedResult.getSearchHit(0).getFacetDistribution()).get("category"))
				.isEqualTo(Map.of("drama", 1, "comics", 1));

	}

	@Test
	void shouldReturnFacetSearchValuesAndCounts() {
		reactiveOperations.saveAll(
				Flux.just(book("book-1", "One", "drama"), book("book-2", "Two", "drama"), book("book-3", "Three", "action")))
				.collectList().block(BLOCK_TIMEOUT);

		SearchHits<FacetHit> result = reactiveOperations.facetSearch(new FacetQuery("category"), SearchBook.class)
				.block(BLOCK_TIMEOUT);

		assertThat(result).isNotNull();
		assertThat(result.getSearchHits()).hasSize(2);
		assertThat(result.getSearchHits()).extracting(hit -> hit.getContent().value()).containsExactlyInAnyOrder("drama",
				"action");
		assertThat(result.getSearchHits()).filteredOn(hit -> hit.getContent().value().equals("drama"))
				.extracting(hit -> hit.getContent().count()).containsExactly(2);
	}

	@Test
	void shouldSearchSimilarDocumentsUsingUserProvidedVectors() {
		reactiveOperations.saveAll(Flux.just(vectorDocument("reference", "Reference", List.of(1.0, 0.0, 0.0)),
				vectorDocument("near", "Near", List.of(0.9, 0.1, 0.0)), vectorDocument("far", "Far", List.of(0.2, 0.8, 0.0))))
				.collectList().block(BLOCK_TIMEOUT);

		SearchHits<UserVectorDocument> result = reactiveOperations
				.similarSearch(new SimilarQuery("reference", "manual"), UserVectorDocument.class).block(BLOCK_TIMEOUT);

		assertThat(result).isNotNull();
		assertThat(result.getSearchHits()).extracting(hit -> hit.getContent().getId()).contains("near", "far");
		assertThat(result.getSearchHit(0).getContent().getId()).isEqualTo("near");
	}

	private static SearchBook book(String id, String title, String category) {
		return new SearchBook(id, title, category);
	}

	private static SearchComic comic(String id, String title, String category) {
		return new SearchComic(id, title, category);
	}

	private static UserVectorDocument vectorDocument(String id, String title, List<Double> vector) {
		return new UserVectorDocument(id, title, Map.of("manual", vector));
	}

	private static Map<?, ?> asMap(Object value) {
		return (Map<?, ?>) value;
	}

	private void deleteIndexIfExists(String indexUid) throws MeilisearchException {
		try {
			TaskInfo taskInfo = meilisearchClient.deleteIndex(indexUid);
			meilisearchClient.index(indexUid).waitForTask(taskInfo.getTaskUid(), meilisearchClient.getRequestTimeout(),
					meilisearchClient.getRequestInterval());
		} catch (MeilisearchApiException exception) {
			if (!"index_not_found".equals(exception.getCode())) {
				throw exception;
			}
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class ReactiveOperationsTestConfiguration {

		@Bean(name = { "reactiveMeilisearchOperations", "reactiveMeilisearchTemplate" })
		ReactiveMeilisearchOperations reactiveMeilisearchOperations(
				@Qualifier("meilisearchClientConfiguration") ClientConfiguration clientConfiguration,
				MeilisearchConverter converter, @Qualifier("meilisearchObjectMapper") ObjectMapper objectMapper) {
			return new ReactiveMeilisearchTemplate(clientConfiguration, converter, objectMapper);
		}
	}

	@Setting(filterableAttributes = { "category" }, sortableAttributes = { "title" })
	@Document(indexUid = BOOKS_INDEX)
	static class SearchBook {

		@Id private String id;
		private String title;
		private String category;

		public SearchBook() {}

		SearchBook(String id, String title, String category) {
			this.id = id;
			this.title = title;
			this.category = category;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getCategory() {
			return category;
		}

		public void setCategory(String category) {
			this.category = category;
		}
	}

	@Setting(filterableAttributes = { "category" })
	@Document(indexUid = COMICS_INDEX)
	static class SearchComic {

		@Id private String id;
		private String title;
		private String category;

		public SearchComic() {}

		SearchComic(String id, String title, String category) {
			this.id = id;
			this.title = title;
			this.category = category;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getCategory() {
			return category;
		}

		public void setCategory(String category) {
			this.category = category;
		}
	}

	@Setting(embedders = @Embedder(name = "manual", source = Embedder.Source.USER_PROVIDED, dimensions = 3))
	@Document(indexUid = VECTORS_INDEX)
	static class UserVectorDocument {

		@Id private String id;
		private String title;
		private Map<String, List<Double>> _vectors;

		public UserVectorDocument() {}

		UserVectorDocument(String id, String title, Map<String, List<Double>> vectors) {
			this.id = id;
			this.title = title;
			this._vectors = vectors;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public Map<String, List<Double>> get_vectors() {
			return _vectors;
		}

		public void set_vectors(Map<String, List<Double>> vectors) {
			this._vectors = vectors;
		}
	}
}
