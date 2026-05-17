/*
 * Copyright 2023-2026 the original author or authors.
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

import io.vanslog.spring.data.meilisearch.annotations.Document;
import io.vanslog.spring.data.meilisearch.client.MeilisearchClient;
import io.vanslog.spring.data.meilisearch.core.federation.FederationResponse;
import io.vanslog.spring.data.meilisearch.core.query.BaseQuery;
import io.vanslog.spring.data.meilisearch.core.query.BasicQuery;
import io.vanslog.spring.data.meilisearch.core.query.FacetQuery;
import io.vanslog.spring.data.meilisearch.core.query.IndexQuery;
import io.vanslog.spring.data.meilisearch.entities.ComicsMovie;
import io.vanslog.spring.data.meilisearch.entities.Movie;
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTest;
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTestConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.test.context.ContextConfiguration;

import com.meilisearch.sdk.MergeFacets;
import com.meilisearch.sdk.MultiSearchFederation;
import com.meilisearch.sdk.exceptions.MeilisearchApiException;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import com.meilisearch.sdk.model.TaskInfo;

/**
 * Integration tests for {@link MeilisearchOperations}.
 *
 * @author Junghoon Ban
 */
@MeilisearchTest
@ContextConfiguration(classes = { MeilisearchTestConfiguration.class })
class MeilisearchTemplateIntegrationTests {

	@Autowired MeilisearchClient meilisearchClient;
	@Autowired MeilisearchOperations meilisearchTemplate;

	Movie movie1 = new Movie(1, "Carol", "A love story", new String[] { "Romance", "Drama" });
	Movie movie2 = new Movie(2, "Wonder Woman", "A superhero film", new String[] { "Action", "Adventure" });
	Movie movie3 = new Movie(3, "Life of Pi", "A survival film", new String[] { "Adventure", "Drama" });

	ComicsMovie comics1 = new ComicsMovie(1, "Wonder Woman", "A superhero comic", new String[] { "Comics", "Action" });
	ComicsMovie comics2 = new ComicsMovie(2, "Batman", "A superhero comic", new String[] { "Comics", "Action" });

	private static final List<String> LIFECYCLE_INDEX_UIDS = List.of("lifecycle-create-index",
			"lifecycle-get-list-index", "lifecycle-update-index", "lifecycle-delete-index", "runtime-settings-index",
			"runtime-settings-reset-index");

	@BeforeEach
	void setUp() throws MeilisearchException {
		meilisearchClient.index("movies").deleteAllDocuments();
		deleteLifecycleIndexes();
	}

	@AfterEach
	void tearDown() throws MeilisearchException {
		deleteLifecycleIndexes();
	}

	@Test
	void shouldSaveEntity() {

		Movie saved = meilisearchTemplate.save(movie1);

		assertThat(saved).isEqualTo(movie1);
	}

	@Test
	void shouldSaveEntities() {

		List<Movie> movies = List.of(movie1, movie2);

		List<Movie> saved = meilisearchTemplate.save(movies);

		assertThat(saved).isEqualTo(movies);
	}

	@Test
	void shouldGetEntity() {

		meilisearchTemplate.save(movie1);

		Movie saved = meilisearchTemplate.get("1", Movie.class);

		assertThat(saved.getId()).isEqualTo(movie1.getId());
		assertThat(saved.getTitle()).isEqualTo(movie1.getTitle());
		assertThat(saved.getDescription()).isEqualTo(movie1.getDescription());
		assertThat(saved.getGenres()).isEqualTo(movie1.getGenres());
	}

	@Test
	void shouldGetEntities() {

		List<Movie> movies = List.of(movie1, movie2, movie3);
		meilisearchTemplate.save(movies);

		List<Movie> savedMovies = meilisearchTemplate.multiGet(Movie.class);

		assertThat(savedMovies).containsExactlyInAnyOrder(movie1, movie2, movie3);
	}

	@Test
	void shouldGetEntitiesWithPagination() {

		List<Movie> movies = List.of(movie1, movie2, movie3);
		meilisearchTemplate.save(movies);

		List<Movie> savedMovies = meilisearchTemplate.multiGet(Movie.class, 1, 2);

		assertThat(savedMovies).containsExactlyInAnyOrder(movie2, movie3);
	}

	@Test
	void shouldGetCertainEntities() {

		List<Movie> movies = List.of(movie1, movie2, movie3);
		meilisearchTemplate.save(movies);

		List<Movie> savedMovies = meilisearchTemplate.multiGet(Movie.class, List.of("1", "3"));

		assertThat(savedMovies).containsExactlyInAnyOrder(movie1, movie3);
	}

	@Test
	void shouldGetCertainEntitiesWithPagination() {

		List<Movie> movies = List.of(movie1, movie2, movie3);
		meilisearchTemplate.save(movies);

		List<Movie> savedMovies = meilisearchTemplate.multiGet(Movie.class, List.of("1", "3"), 0, 1);

		assertThat(savedMovies).containsExactlyInAnyOrder(movie1);
	}

	@Test
	void returnTrueWhenDocumentExists() {

		meilisearchTemplate.save(movie1);

		boolean exists = meilisearchTemplate.exists("1", Movie.class);

		assertThat(exists).isTrue();
	}

	@Test
	void returnFalseWhenDocumentDoesNotExist() {

		meilisearchTemplate.save(movie1);
		meilisearchTemplate.delete(movie1);

		boolean exists = meilisearchTemplate.exists("1", Movie.class);

		assertThat(exists).isFalse();
	}

	@Test
	void shouldCountDocuments() {

		meilisearchTemplate.save(List.of(movie1, movie2));

		long count = meilisearchTemplate.count(Movie.class);

		assertThat(count).isEqualTo(2);
	}

	@Test
	void shouldDeleteDocument() {

		meilisearchTemplate.save(movie1);

		meilisearchTemplate.delete(movie1);

		assertThat(meilisearchTemplate.get("1", Movie.class)).isNull();
	}

	@Test
	void shouldDeleteDocuments() {

		meilisearchTemplate.save(List.of(movie1, movie2, movie3));

		boolean result = meilisearchTemplate.delete(Movie.class, List.of("1", "2"));

		assertThat(result).isTrue();
	}

	@Test
	void shouldDeleteAllDocuments() {

		meilisearchTemplate.save(List.of(movie1, movie2));

		boolean result = meilisearchTemplate.deleteAll(Movie.class);

		assertThat(result).isTrue();
	}

	@Test
	void shouldGetInstanceHealth() {

		MeilisearchHealth health = meilisearchTemplate.instanceOps().health();

		assertThat(health.getStatus()).isEqualTo("available");
		assertThat(meilisearchTemplate.instanceOps().isHealthy()).isTrue();
	}

	@Test
	void shouldGetInstanceVersion() {

		MeilisearchVersion version = meilisearchTemplate.instanceOps().version();

		assertThat(version.getCommitSha()).isNotBlank();
		assertThat(version.getCommitDate()).isNotBlank();
		assertThat(version.getPackageVersion()).isNotBlank();
	}

	@Test
	void shouldGetStatsFromInstanceAndIndexOperations() {

		meilisearchTemplate.save(List.of(movie1, movie2));

		MeilisearchStats instanceStats = meilisearchTemplate.instanceOps().stats();
		MeilisearchIndexStats indexStatsByClass = meilisearchTemplate.indexOps(Movie.class).stats();
		MeilisearchIndexStats indexStatsByIndexUid = meilisearchTemplate.indexOps("movies").stats();

		assertThat(instanceStats.getDatabaseSize()).isGreaterThanOrEqualTo(instanceStats.getUsedDatabaseSize());
		assertThat(instanceStats.getLastUpdate()).isNotNull();
		assertThat(instanceStats.getIndexes()).containsKey("movies");
		assertThat(instanceStats.getIndexes().get("movies").getNumberOfDocuments()).isEqualTo(2);
		assertThat(indexStatsByClass.getNumberOfDocuments()).isEqualTo(2);
		assertThat(indexStatsByClass.isIndexing()).isFalse();
		assertThat(indexStatsByClass.getFieldDistribution()).containsEntry("title", 2L);
		assertThat(indexStatsByIndexUid.getNumberOfDocuments()).isEqualTo(2);
	}

	@Test
	void shouldCreateIndex() {

		MeilisearchIndex index = meilisearchTemplate.indexOps("lifecycle-create-index")
				.create(new MeilisearchIndexCreateRequest("id"));

		assertThat(index.getUid()).isEqualTo("lifecycle-create-index");
		assertThat(index.getPrimaryKey()).isEqualTo("id");
		assertThat(index.getCreatedAt()).isNotBlank();
	}

	@Test
	void shouldGetAndListIndexes() {

		meilisearchTemplate.indexOps("lifecycle-get-list-index").create(new MeilisearchIndexCreateRequest("id"));

		MeilisearchIndex index = meilisearchTemplate.indexOps("lifecycle-get-list-index").get();
		MeilisearchIndexList indexes = meilisearchTemplate.indexOps("lifecycle-get-list-index")
				.list(new MeilisearchIndexQuery(0, 100));

		assertThat(index.getUid()).isEqualTo("lifecycle-get-list-index");
		assertThat(indexes.getIndexes()).extracting(MeilisearchIndex::getUid).contains("lifecycle-get-list-index");
		assertThat(indexes.getTotal()).isGreaterThanOrEqualTo(indexes.getIndexes().size());
	}

	@Test
	void shouldUpdateIndex() {

		MeilisearchIndex created = meilisearchTemplate.indexOps("lifecycle-update-index").create();

		MeilisearchIndex updated = meilisearchTemplate.indexOps("lifecycle-update-index")
				.update(new MeilisearchIndexUpdateRequest("id"));

		assertThat(created.getPrimaryKey()).isNull();
		assertThat(updated.getUid()).isEqualTo("lifecycle-update-index");
		assertThat(updated.getPrimaryKey()).isEqualTo("id");
	}

	@Test
	void shouldDeleteIndex() {

		meilisearchTemplate.indexOps("lifecycle-delete-index").create(new MeilisearchIndexCreateRequest("id"));
		meilisearchTemplate.indexOps("lifecycle-get-list-index").create(new MeilisearchIndexCreateRequest("id"));

		boolean deleted = meilisearchTemplate.indexOps("lifecycle-delete-index").delete();
		MeilisearchIndexList indexes = meilisearchTemplate.indexOps("lifecycle-delete-index")
				.list(new MeilisearchIndexQuery(0, 100));
		List<String> indexUids = indexes.getIndexes().stream().map(MeilisearchIndex::getUid).toList();

		assertThat(deleted).isTrue();
		assertThat(indexUids).isNotEmpty()
				.contains("lifecycle-get-list-index")
				.doesNotContain("lifecycle-delete-index");
	}

	@Test
	void shouldRejectBlankIndexUid() {

		assertThatIllegalArgumentException().isThrownBy(() -> meilisearchTemplate.indexOps(""));
	}

	@Test
	void shouldReadCurrentIndexSettings() {

		meilisearchTemplate.applySettings(Movie.class);

		MeilisearchIndexSettings settings = meilisearchTemplate.indexOps(Movie.class).getSettings();

		assertThat(settings.getFilterableAttributes()).contains("genres");
	}

	@Test
	void shouldUpdateIndexSettings() {

		meilisearchTemplate.indexOps("runtime-settings-index").create(new MeilisearchIndexCreateRequest("id"));
		MeilisearchIndexSettings settings = MeilisearchIndexSettings.builder() //
				.withSearchableAttributes(List.of("title", "description")) //
				.withDisplayedAttributes(List.of("id", "title", "description")) //
				.withFilterableAttributes(List.of("genres")) //
				.withSortableAttributes(List.of("title")) //
				.withSynonyms(Map.of("hero", List.of("superhero"))) //
				.withPagination(new MeilisearchIndexSettings.PaginationSettings(1500)) //
				.withFaceting(new MeilisearchIndexSettings.FacetingSettings(75)) //
				.withTypoTolerance(new MeilisearchIndexSettings.TypoToleranceSettings(true, 5, 9,
						List.of("skype"), List.of("serial_number"))) //
				.withProximityPrecision("byWord") //
				.withSearchCutoffMs(50) //
				.build();

		MeilisearchIndexSettings updated = meilisearchTemplate.indexOps("runtime-settings-index")
				.updateSettings(settings);

		assertThat(updated.getSearchableAttributes()).containsExactly("title", "description");
		assertThat(updated.getDisplayedAttributes()).containsExactly("id", "title", "description");
		assertThat(updated.getFilterableAttributes()).contains("genres");
		assertThat(updated.getSortableAttributes()).contains("title");
		assertThat(updated.getSynonyms()).containsEntry("hero", List.of("superhero"));
		assertThat(updated.getPagination().getMaxTotalHits()).isEqualTo(1500);
		assertThat(updated.getFaceting().getMaxValuesPerFacet()).isEqualTo(75);
		assertThat(updated.getTypoTolerance().getOneTypo()).isEqualTo(5);
		assertThat(updated.getTypoTolerance().getTwoTypos()).isEqualTo(9);
		assertThat(updated.getTypoTolerance().getDisableOnWords()).containsExactly("skype");
		assertThat(updated.getTypoTolerance().getDisableOnAttributes()).containsExactly("serial_number");
		assertThat(updated.getProximityPrecision()).isEqualTo("byWord");
		assertThat(updated.getSearchCutoffMs()).isEqualTo(50);
	}

	@Test
	void shouldResetIndexSettings() {

		meilisearchTemplate.indexOps("runtime-settings-reset-index").create(new MeilisearchIndexCreateRequest("id"));
		MeilisearchIndexSettings settings = MeilisearchIndexSettings.builder() //
				.withFilterableAttributes(List.of("genres")) //
				.build();
		MeilisearchIndexOperations indexOperations = meilisearchTemplate.indexOps("runtime-settings-reset-index");
		assertThat(indexOperations.updateSettings(settings).getFilterableAttributes()).contains("genres");

		MeilisearchIndexSettings reset = indexOperations.resetSettings();

		assertThat(reset.getFilterableAttributes()).doesNotContain("genres");
	}

	@Test
	void shouldSearchWithBasicQuery() {
		meilisearchTemplate.save(List.of(movie1, movie2, movie3));

		BaseQuery query = new BasicQuery(movie2.getTitle());
		SearchHits<Movie> result = meilisearchTemplate.search(query, Movie.class);
		List<SearchHit<Movie>> searchHits = result.getSearchHits();
		List<Movie> movies = searchHits.stream().map(SearchHit::getContent).toList();

		assertThat(movies).isEqualTo(List.of(movie2));
	}

	@Test
	void shouldSearchWithIndexQuery() {
		meilisearchTemplate.save(List.of(movie1, movie2, movie3));

		BaseQuery query = new IndexQuery(movie2.getTitle());
		SearchHits<Movie> result = meilisearchTemplate.search(query, Movie.class);
		List<SearchHit<Movie>> searchHits = result.getSearchHits();
		List<Movie> movies = searchHits.stream().map(SearchHit::getContent).toList();

		assertThat(movies).isEqualTo(List.of(movie2));
	}

	@Test
	void shouldSearchWithFilter() {
		meilisearchTemplate.applySettings(Movie.class); // make genres filterable
		meilisearchTemplate.save(List.of(movie1, movie2, movie3));

		BaseQuery query = BasicQuery.builder().withFilter(new String[] { "genres = Drama" }).build();

		SearchHits<Movie> result = meilisearchTemplate.search(query, Movie.class);
		List<SearchHit<Movie>> searchHits = result.getSearchHits();
		List<Movie> movies = searchHits.stream().map(SearchHit::getContent).toList();

		assertThat(movies).isEqualTo(List.of(movie1, movie3));
	}

	@Test
	void shouldMultiSearchWithIndexQuery() {
		meilisearchTemplate.save(List.of(movie1, movie2, movie3));

		List<BaseQuery> queries = List.of(new IndexQuery(movie1.getTitle()), new IndexQuery(movie2.getTitle()));

		SearchHits<Movie> result = meilisearchTemplate.multiSearch(queries, Movie.class);
		List<SearchHit<Movie>> searchHits = result.getSearchHits();
		List<Movie> movies = searchHits.stream().map(SearchHit::getContent).toList();

		assertThat(movies).isEqualTo(List.of(movie1, movie2));
	}

	@Test
	void shouldMultiSearchWithBasicQuery() {
		meilisearchTemplate.save(List.of(movie1, movie2, movie3));

		List<BaseQuery> queries = List.of(new BasicQuery(movie1.getTitle()), new BasicQuery(movie2.getTitle()));

		SearchHits<Movie> result = meilisearchTemplate.multiSearch(queries, Movie.class);
		List<SearchHit<Movie>> searchHits = result.getSearchHits();
		List<Movie> movies = searchHits.stream().map(SearchHit::getContent).toList();

		assertThat(movies).isEqualTo(List.of(movie1, movie2));
	}

	@Test
	void shouldMultiSearchWithMixedQuery() {
		meilisearchTemplate.save(List.of(movie1, movie2, movie3));

		List<BaseQuery> queries = List.of(new BasicQuery(movie1.getTitle()), new IndexQuery(movie2.getTitle()));

		SearchHits<Movie> result = meilisearchTemplate.multiSearch(queries, Movie.class);
		List<SearchHit<Movie>> searchHits = result.getSearchHits();
		List<Movie> movies = searchHits.stream().map(SearchHit::getContent).toList();

		assertThat(movies).isEqualTo(List.of(movie1, movie2));
	}

	@Test
	void shouldMultiSearchWithMultiIndexedQuery() {
		meilisearchTemplate.save(List.of(movie1, movie2, movie3));
		meilisearchTemplate.save(List.of(comics1, comics2));

		List<BaseQuery> queries = List.of( //
				IndexQuery.builder().withQ(movie1.getTitle()).withIndexUid("movies").build(), //
				IndexQuery.builder().withQ(comics1.getTitle()).withIndexUid("comics").build() //
		);

		SearchHits<Movie> result = meilisearchTemplate.multiSearch(queries, Movie.class);
		List<SearchHit<Movie>> searchHits = result.getSearchHits();
		List<Movie> movies = searchHits.stream().map(SearchHit::getContent).toList();

		assertThat(movies).hasSize(2);
		assertThat(movies).extracting("id", "title") //
				.containsExactlyInAnyOrder( //
						tuple(movie1.getId(), movie1.getTitle()), //
						tuple(comics1.getId(), comics1.getTitle()) //
				);
	}

	@Test
	void shouldFacetSearchWithQuery() {
		meilisearchTemplate.applySettings(Movie.class); // make genres filterable
		meilisearchTemplate.save(List.of(movie1, movie2, movie3));

		FacetQuery query = new FacetQuery("genres");
		SearchHits<FacetHit> result = meilisearchTemplate.facetSearch(query, Movie.class);

		assertThat(result.getSearchHits()).hasSize(4);
	}

	@Test
	void shouldSearchWithFederation() {
		meilisearchTemplate.applySettings(Movie.class); // make genres filterable
		meilisearchTemplate.applySettings(ComicsMovie.class); // make genres filterable
		meilisearchTemplate.save(List.of(movie1, movie2, movie3));
		meilisearchTemplate.save(List.of(comics1, comics2));

		List<BaseQuery> queries = List.of(IndexQuery.builder().withQ("Wonder").withIndexUid("movies").build(),
				IndexQuery.builder().withQ("Wonder").withIndexUid("comics").build());

		MultiSearchFederation federation = new MultiSearchFederation();
		federation.setLimit(10);
		federation.setOffset(0);
		MergeFacets mergeFacets = new MergeFacets();
		mergeFacets.setMaxValuesPerFacet(10);
		federation.setMergeFacets(mergeFacets);
		federation.setFacetsByIndex(new HashMap<>());

		SearchHits<Movie> result = meilisearchTemplate.multiSearch(queries, federation, Movie.class);
		List<Movie> movies = result.get().map(SearchHit::getContent).toList();

		assertThat(result.getSearchHit(0).getFederation()).isInstanceOf(FederationResponse.class);

		assertThat(movies).hasSize(2);
		assertThat(movies).extracting("id", "title").containsExactlyInAnyOrder(tuple(movie2.getId(), movie2.getTitle()),
				tuple(comics1.getId(), comics1.getTitle()));

	}

	@Test
	void shouldSaveEntityWithAnnotatedIdField() {

		AnnotatedIdField annotatedIdField = new AnnotatedIdField();
		String documentId = "idField";
		annotatedIdField.setName(documentId);

		meilisearchTemplate.save(annotatedIdField);

		assertThat(meilisearchTemplate.exists(documentId, AnnotatedIdField.class)).isTrue();
	}

	@Document(indexUid = "annotatedIdField")
	static class AnnotatedIdField {

		@Id private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	private void deleteLifecycleIndexes() throws MeilisearchException {

		for (String indexUid : LIFECYCLE_INDEX_UIDS) {
			deleteIndexIfExists(indexUid);
		}
	}

	private void deleteIndexIfExists(String indexUid) throws MeilisearchException {

		try {
			TaskInfo taskInfo = meilisearchClient.deleteIndex(indexUid);
			meilisearchClient.index(indexUid).waitForTask(taskInfo.getTaskUid(), meilisearchClient.getRequestTimeout(),
					meilisearchClient.getRequestInterval());
		} catch (MeilisearchApiException e) {
			if (!"index_not_found".equals(e.getCode())) {
				throw e;
			}
		}
	}
}
