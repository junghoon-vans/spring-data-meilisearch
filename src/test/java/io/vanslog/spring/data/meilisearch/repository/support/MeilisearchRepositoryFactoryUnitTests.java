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
package io.vanslog.spring.data.meilisearch.repository.support;

import static org.assertj.core.api.Assertions.*;

import io.vanslog.spring.data.meilisearch.annotations.Document;
import io.vanslog.spring.data.meilisearch.annotations.Setting;
import io.vanslog.spring.data.meilisearch.consumer.UnannotatedMovieRepository;
import io.vanslog.spring.data.meilisearch.core.MeilisearchOperations;
import io.vanslog.spring.data.meilisearch.core.SearchHit;
import io.vanslog.spring.data.meilisearch.core.SearchHits;
import io.vanslog.spring.data.meilisearch.core.TotalHitsRelation;
import io.vanslog.spring.data.meilisearch.core.convert.MappingMeilisearchConverter;
import io.vanslog.spring.data.meilisearch.core.mapping.SimpleMeilisearchMappingContext;
import io.vanslog.spring.data.meilisearch.core.query.BaseQuery;
import io.vanslog.spring.data.meilisearch.entities.Movie;
import io.vanslog.spring.data.meilisearch.repository.MeilisearchRepository;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.QueryAnnotation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.core.NamedQueries;

/**
 * Tests repository query creation and execution.
 *
 * @author Junghoon Ban
 */
class MeilisearchRepositoryFactoryUnitTests {

	private MeilisearchRepositoryFactory repositoryFactory;
	private List<BaseQuery> executedQueries;
	private List<String> countedFilters;
	private List<String> deletedFilters;
	private long deletedCount;
	private List<SearchHit<QueryDocument>> searchHits;
	private long totalHits;
	private TotalHitsRelation totalHitsRelation;
	private boolean emptyAfterFirstPage;

	@BeforeEach
	void setUp() {
		MappingMeilisearchConverter converter = new MappingMeilisearchConverter(new SimpleMeilisearchMappingContext());
		executedQueries = new ArrayList<>();
		countedFilters = new ArrayList<>();
		deletedFilters = new ArrayList<>();
		deletedCount = 0;
		searchHits = List.of();
		totalHits = 0;
		totalHitsRelation = TotalHitsRelation.EQUAL_TO;
		emptyAfterFirstPage = false;
		repositoryFactory = new MeilisearchRepositoryFactory(operationsWithConverter(converter));
	}

	@Test
	void shouldCreateRepositoryForBaseRepositoryMethods() {
		assertThat(repositoryFactory.getRepository(MovieRepository.class))
				.as("base repository methods should not require query lookup resolution").isNotNull();
	}

	@Test
	void shouldTranslateSupportedPredicatesToEscapedMeilisearchFilters() {
		SupportedRepository repository = repositoryFactory.getRepository(SupportedRepository.class);
		willReturn(1, new QueryDocument("1", "Arrival", "science fiction", 8, true));

		assertFilter(() -> repository.findByTitle("Arrival"), "title = \"Arrival\"");
		assertFilter(() -> repository.findByTitle("Director's \"cut\""), "title = \"Director's \\\"cut\\\"\"");
		assertFilter(() -> repository.findByGenreIn(List.of("drama", "science fiction")),
				"genre IN [\"drama\", \"science fiction\"]");
		assertFilter(() -> repository.findByGenreNotIn(List.of("drama", "science fiction")),
				"genre NOT IN [\"drama\", \"science fiction\"]");
		assertThatThrownBy(() -> repository.findByGenreIn(List.of())).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("IN requires at least one value").hasMessageContaining("findByGenreIn");
		BaseQuery queryWithoutEmptyExclusion = captureQuery(() -> repository.findByGenreNotIn(List.of()));
		assertThat(queryWithoutEmptyExclusion.getFilter() == null || queryWithoutEmptyExclusion.getFilter().length == 0)
				.isTrue();
		assertFilter(() -> repository.findByPriceGreaterThan(5), "price > 5");
		assertFilter(() -> repository.findByPriceGreaterThanEqual(5), "price >= 5");
		assertFilter(() -> repository.findByPriceLessThan(5), "price < 5");
		assertFilter(() -> repository.findByPriceLessThanEqual(5), "price <= 5");
		assertFilter(() -> repository.findByPriceBetween(5, 9), "price 5 TO 9");
		assertFilter(() -> repository.findByAvailableTrue(), "available = true");
		assertFilter(() -> repository.findByAvailableFalse(), "available = false");
		assertFilter(() -> repository.findByTitleAndPriceGreaterThan("Arrival", 5), "title = \"Arrival\"", "price > 5");
	}

	@Test
	void shouldMapSupportedReturnTypesAndHonorPagingAndSorting() {
		SupportedRepository repository = repositoryFactory.getRepository(SupportedRepository.class);
		QueryDocument first = new QueryDocument("1", "Arrival", "science fiction", 8, true);
		QueryDocument second = new QueryDocument("2", "Moon", "drama", 10, false);

		willReturn(1, first);
		assertThat(repository.findByTitle("Arrival")).isSameAs(first);

		willReturn(0);
		assertThatThrownBy(() -> repository.findByTitle("missing")).isInstanceOf(EmptyResultDataAccessException.class);

		willReturn(1, first);
		assertThat(repository.findByPrice(8)).containsSame(first);

		willReturn(0);
		assertThat(repository.findByPrice(8)).isEmpty();

		willReturn(2, first, second);
		assertThat(repository.findByGenre("science fiction")).containsExactly(first, second);

		willReturn(2, first, second);
		Iterable<QueryDocument> iterable = repository.findByAvailableTrue();
		assertThat(StreamSupport.stream(iterable.spliterator(), false).toList()).containsExactly(first, second);

		Sort sort = Sort.by(Sort.Order.desc("price"));
		BaseQuery sortedQuery = captureQuery(() -> repository.findByGenre("science fiction", sort));
		assertThat(sortedQuery.getSort()).isEqualTo(sort);

		willReturn(1, first);
		BaseQuery orderedQuery = captureQuery(() -> repository.findByTitleOrderByPriceDesc("Arrival"));
		assertThat(orderedQuery.getSort()).isEqualTo(Sort.by(Sort.Order.desc("price")));

		Pageable pageable = PageRequest.of(1, 2, Sort.by(Sort.Order.asc("price")));
		willReturn(18, first, second);
		Page<QueryDocument> page = repository.findByPriceBetween(5, 10, pageable);
		assertThat(page.getContent()).containsExactly(first, second);
		assertThat(page.getTotalElements()).isEqualTo(18);
		assertThat(page.getPageable()).isEqualTo(pageable);
		assertThat(lastQuery().getPageable()).isEqualTo(pageable);
		assertThat(lastQuery().getSort()).isEqualTo(pageable.getSort());
	}

	@Test
	void shouldApplyEachSortOnceForPagedCollections() {
		SupportedRepository repository = repositoryFactory.getRepository(SupportedRepository.class);
		Pageable pageable = PageRequest.of(0, 10, Sort.by("title"));
		willReturn(0);

		BaseQuery query = captureQuery(() -> repository.findByGenreOrderByPriceDesc("drama", pageable));

		assertThat(query.getSort()).isEqualTo(Sort.by(Sort.Order.desc("price"), Sort.Order.asc("title")));
	}

	@Test
	void shouldRejectMultipleMatchesForSingleEntityAndOptionalResults() {
		SupportedRepository repository = repositoryFactory.getRepository(SupportedRepository.class);
		QueryDocument first = new QueryDocument("1", "Arrival", "science fiction", 8, true);
		QueryDocument second = new QueryDocument("2", "Moon", "drama", 10, false);

		willReturn(2, first, second);
		assertThatThrownBy(() -> repository.findByTitle("Arrival"))
				.isInstanceOf(IncorrectResultSizeDataAccessException.class);

		willReturn(2, first, second);
		assertThatThrownBy(() -> repository.findByPrice(8)).isInstanceOf(IncorrectResultSizeDataAccessException.class);
	}

	@Test
	void shouldRejectUnpagedCollectionsWhenTheTotalIsNotExact() {
		SupportedRepository repository = repositoryFactory.getRepository(SupportedRepository.class);
		QueryDocument first = new QueryDocument("1", "Arrival", "science fiction", 8, true);

		willReturn(TotalHitsRelation.OFF, 2, first);

		assertThatThrownBy(() -> repository.findByGenre("science fiction")).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void shouldRejectUnpagedCollectionsWhenPagingCannotRetrieveTheExactTotal() {
		SupportedRepository repository = repositoryFactory.getRepository(SupportedRepository.class);
		QueryDocument first = new QueryDocument("1", "Arrival", "science fiction", 8, true);

		willReturn(2, first);
		emptyAfterFirstPage = true;

		assertThatThrownBy(() -> repository.findByGenre("science fiction")).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void shouldReturnNullForUnannotatedConsumerRepositoryWithoutNonNullDefault() {
		UnannotatedMovieRepository repository = repositoryFactory.getRepository(UnannotatedMovieRepository.class);
		willReturn(0);

		assertThat(repository.findByTitle("missing")).isNull();
	}

	@Test
	void shouldRejectOrPredicatesDuringRepositoryBootstrap() {
		assertThatThrownBy(() -> repositoryFactory.getRepository(OrQueryRepository.class))
				.hasMessageContaining("findByTitleOrGenre").hasMessageContaining("Or");
	}

	@Test
	void shouldRejectTextPredicatesDuringRepositoryBootstrap() {
		assertThatThrownBy(() -> repositoryFactory.getRepository(TextQueryRepository.class))
				.hasMessageContaining("findByTitleContaining").hasMessageContaining("Containing");
	}

	@Test
	void shouldExecuteCountAndExistsProjectionsWithMappedFilters() {
		CountQueryRepository countRepository = repositoryFactory.getRepository(CountQueryRepository.class);
		ExistsQueryRepository existsRepository = repositoryFactory.getRepository(ExistsQueryRepository.class);

		willReturn(42);
		assertThat(countRepository.countByTitle("Arrival")).isEqualTo(42);
		assertThat(countRepository.countByGenreAndPriceGreaterThan("drama", 5)).isEqualTo(42);
		assertThat(countRepository.countByAvailableTrue()).isEqualTo(42L);
		assertThat(countedFilters).containsExactly("title = \"Arrival\"", "genre = \"drama\" AND price > 5",
				"available = true");
		assertThat(countRepository.countByGenreNotIn(List.of())).isEqualTo(42);
		assertThat(countedFilters).hasSize(3);
		assertThat(executedQueries).isEmpty();

		willReturn(0);
		assertThat(existsRepository.existsByTitle("missing")).isFalse();
		willReturn(1, new QueryDocument("1", "Arrival", "drama", 8, true));
		assertThat(existsRepository.existsByTitle("Arrival")).isTrue();
		assertThat(existsRepository.existsByGenre("drama")).isTrue();
		assertThat(lastQuery().getPageable().getPageSize()).isEqualTo(1);
		assertThat(lastQuery().getFilter()).containsExactly("genre = \"drama\"");
	}

	@Test
	void shouldDeleteByFilterWithoutMaterializingMatchingDocuments() {
		DeleteQueryRepository repository = repositoryFactory.getRepository(DeleteQueryRepository.class);
		deletedCount = 3;

		assertThat(repository.deleteByTitle("Arrival")).isEqualTo(3);
		assertThat(repository.removeByGenre("drama")).isEqualTo(3);
		assertThat(repository.deleteByGenre("drama")).isEqualTo(3L);
		repository.deleteByAvailableFalse();
		assertThat(deletedFilters).containsExactly("title = \"Arrival\"", "genre = \"drama\"", "genre = \"drama\"",
				"available = false");
		assertThat(executedQueries).isEmpty();

		assertThatThrownBy(() -> repository.deleteByGenreNotIn(List.of())).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("effective filter");
		assertThat(deletedFilters).hasSize(4);
	}

	@Test
	void shouldRejectInvalidProjectionReturnsAndSortingAtBootstrap() {
		assertThatThrownBy(() -> repositoryFactory.getRepository(InvalidCountReturnRepository.class))
				.hasMessageContaining("countByTitle").hasMessageContaining("return type");
		assertThatThrownBy(() -> repositoryFactory.getRepository(InvalidExistsReturnRepository.class))
				.hasMessageContaining("existsByTitle").hasMessageContaining("return type");
		assertThatThrownBy(() -> repositoryFactory.getRepository(InvalidDeleteReturnRepository.class))
				.hasMessageContaining("deleteByTitle").hasMessageContaining("return type");
		assertThatThrownBy(() -> repositoryFactory.getRepository(SortedProjectionRepository.class))
				.hasMessageContaining("countByTitleOrderByPriceDesc").hasMessageContaining("Sort/Pageable");
		assertThatThrownBy(() -> repositoryFactory.getRepository(DynamicSortedProjectionRepository.class))
				.hasMessageContaining("existsByTitle").hasMessageContaining("Sort/Pageable");
	}

	@Test
	void shouldRejectDeclaredAndNamedQueriesDuringRepositoryBootstrap() {
		assertThatThrownBy(() -> repositoryFactory.getRepository(DeclaredQueryRepository.class))
				.hasMessageContaining("Declared and named Meilisearch repository queries are not supported")
				.hasMessageContaining("findByTitle");

		repositoryFactory.setNamedQueries(new NamedQueries() {

			@Override
			public boolean hasQuery(String queryName) {
				return queryName.endsWith(".findByTitle");
			}

			@Override
			public String getQuery(String queryName) {
				return "title = ?0";
			}
		});

		assertThatThrownBy(() -> repositoryFactory.getRepository(NamedQueryRepository.class))
				.hasMessageContaining("Declared and named Meilisearch repository queries are not supported")
				.hasMessageContaining("findByTitle");
	}

	@Test
	void shouldRejectUnsupportedReturnTypesDuringRepositoryBootstrap() {
		assertThatThrownBy(() -> repositoryFactory.getRepository(ScalarQueryRepository.class))
				.hasMessageContaining("findByTitle").hasMessageContaining("String");
	}

	private void assertFilter(Runnable invocation, String... expectedFilters) {
		BaseQuery query = captureQuery(invocation);
		assertThat(query.getFilter()).containsExactly(expectedFilters);
		assertThat(query.getQ()).isEqualTo("");
	}

	private BaseQuery captureQuery(Runnable invocation) {
		executedQueries.clear();
		invocation.run();
		assertThat(executedQueries).hasSize(1);
		return executedQueries.get(0);
	}

	private BaseQuery lastQuery() {
		assertThat(executedQueries).isNotEmpty();
		return executedQueries.get(executedQueries.size() - 1);
	}

	private void willReturn(long totalHits, QueryDocument... documents) {
		willReturn(TotalHitsRelation.EQUAL_TO, totalHits, documents);
	}

	private void willReturn(TotalHitsRelation totalHitsRelation, long totalHits, QueryDocument... documents) {
		this.totalHitsRelation = totalHitsRelation;
		this.totalHits = totalHits;
		this.searchHits = List.of(documents).stream().map(document -> new SearchHit<>(document, 0, "", null, null, null))
				.toList();
		emptyAfterFirstPage = false;
	}

	private MeilisearchOperations operationsWithConverter(MappingMeilisearchConverter converter) {
		return (MeilisearchOperations) Proxy.newProxyInstance(getClass().getClassLoader(),
				new Class<?>[] { MeilisearchOperations.class }, (proxy, method, args) -> {
					switch (method.getName()) {
						case "getMeilisearchConverter":
							return converter;
						case "applySettings":
							return null;
						case "search":
							BaseQuery query = (BaseQuery) args[0];
							executedQueries.add(query);
							if (emptyAfterFirstPage && query.getPageable().getOffset() > 0) {
								return new StubSearchHits<>(List.of(), totalHits, totalHitsRelation);
							}
							return new StubSearchHits<>(searchHits, totalHits, totalHitsRelation);
						case "count":
							if (args.length == 2) {
								countedFilters.add((String) args[1]);
							}
							return totalHits;
						case "deleteByFilter":
							deletedFilters.add((String) args[1]);
							return deletedCount;
						case "toString":
							return "test MeilisearchOperations proxy";
						case "hashCode":
							return System.identityHashCode(proxy);
						case "equals":
							return proxy == args[0];
						default:
							throw new UnsupportedOperationException(method.getName());
					}
				});
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@QueryAnnotation
	@interface Query {

		String value();
	}

	@NoRepositoryBean
	interface MovieRepository extends MeilisearchRepository<Movie, Integer> {}

	@NoRepositoryBean
	interface SupportedRepository extends MeilisearchRepository<QueryDocument, String> {

		QueryDocument findByTitle(String title);

		Optional<QueryDocument> findByPrice(int price);

		List<QueryDocument> findByGenre(String genre);

		List<QueryDocument> findByGenre(String genre, Sort sort);

		Iterable<QueryDocument> findByAvailableTrue();

		List<QueryDocument> findByTitleOrderByPriceDesc(String title);

		List<QueryDocument> findByGenreOrderByPriceDesc(String genre, Pageable pageable);

		Page<QueryDocument> findByPriceBetween(int minimum, int maximum, Pageable pageable);

		List<QueryDocument> findByGenreIn(Collection<String> genres);

		List<QueryDocument> findByGenreNotIn(Collection<String> genres);

		List<QueryDocument> findByPriceGreaterThan(int price);

		List<QueryDocument> findByPriceGreaterThanEqual(int price);

		List<QueryDocument> findByPriceLessThan(int price);

		List<QueryDocument> findByPriceLessThanEqual(int price);

		List<QueryDocument> findByPriceBetween(int minimum, int maximum);

		List<QueryDocument> findByAvailableFalse();

		List<QueryDocument> findByTitleAndPriceGreaterThan(String title, int price);
	}

	@NoRepositoryBean
	interface OrQueryRepository extends MeilisearchRepository<QueryDocument, String> {

		List<QueryDocument> findByTitleOrGenre(String title, String genre);
	}

	@NoRepositoryBean
	interface TextQueryRepository extends MeilisearchRepository<QueryDocument, String> {

		List<QueryDocument> findByTitleContaining(String title);
	}

	@NoRepositoryBean
	interface CountQueryRepository extends MeilisearchRepository<QueryDocument, String> {

		long countByTitle(String title);

		long countByGenreAndPriceGreaterThan(String genre, int price);

		Long countByAvailableTrue();

		long countByGenreNotIn(Collection<String> genres);
	}

	@NoRepositoryBean
	interface ExistsQueryRepository extends MeilisearchRepository<QueryDocument, String> {

		boolean existsByTitle(String title);

		boolean existsByGenre(String genre);
	}

	@NoRepositoryBean
	interface DeleteQueryRepository extends MeilisearchRepository<QueryDocument, String> {

		long deleteByTitle(String title);

		long removeByGenre(String genre);

		Long deleteByGenre(String genre);

		void deleteByAvailableFalse();

		long deleteByGenreNotIn(Collection<String> genres);
	}

	@NoRepositoryBean
	interface InvalidCountReturnRepository extends MeilisearchRepository<QueryDocument, String> {

		String countByTitle(String title);
	}

	@NoRepositoryBean
	interface InvalidExistsReturnRepository extends MeilisearchRepository<QueryDocument, String> {

		long existsByTitle(String title);
	}

	@NoRepositoryBean
	interface InvalidDeleteReturnRepository extends MeilisearchRepository<QueryDocument, String> {

		boolean deleteByTitle(String title);
	}

	@NoRepositoryBean
	interface SortedProjectionRepository extends MeilisearchRepository<QueryDocument, String> {

		long countByTitleOrderByPriceDesc(String title);
	}

	@NoRepositoryBean
	interface DynamicSortedProjectionRepository extends MeilisearchRepository<QueryDocument, String> {

		boolean existsByTitle(String title, Sort sort);
	}

	@NoRepositoryBean
	interface DeclaredQueryRepository extends MeilisearchRepository<QueryDocument, String> {

		@Query("title = ?0")
		QueryDocument findByTitle(String title);
	}

	@NoRepositoryBean
	interface NamedQueryRepository extends MeilisearchRepository<QueryDocument, String> {

		QueryDocument findByTitle(String title);
	}

	@NoRepositoryBean
	interface ScalarQueryRepository extends MeilisearchRepository<QueryDocument, String> {

		String findByTitle(String title);
	}

	@Document(indexUid = "query-documents", applySettings = false)
	@Setting(filterableAttributes = { "title", "genre", "price", "available" }, sortableAttributes = { "title", "price" })
	static class QueryDocument {

		@Id private String id;
		private String title;
		private String genre;
		private int price;
		private boolean available;

		QueryDocument() {}

		QueryDocument(String id, String title, String genre, int price, boolean available) {
			this.id = id;
			this.title = title;
			this.genre = genre;
			this.price = price;
			this.available = available;
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

		public String getGenre() {
			return genre;
		}

		public void setGenre(String genre) {
			this.genre = genre;
		}

		public int getPrice() {
			return price;
		}

		public void setPrice(int price) {
			this.price = price;
		}

		public boolean isAvailable() {
			return available;
		}

		public void setAvailable(boolean available) {
			this.available = available;
		}
	}

	private static class StubSearchHits<T> implements SearchHits<T> {

		private final List<SearchHit<T>> searchHits;
		private final long totalHits;
		private final TotalHitsRelation totalHitsRelation;

		StubSearchHits(List<SearchHit<T>> searchHits, long totalHits, TotalHitsRelation totalHitsRelation) {
			this.searchHits = searchHits;
			this.totalHits = totalHits;
			this.totalHitsRelation = totalHitsRelation;
		}

		@Override
		public Duration getExecutionDuration() {
			return Duration.ZERO;
		}

		@Override
		public SearchHit<T> getSearchHit(int index) {
			return searchHits.get(index);
		}

		@Override
		public List<SearchHit<T>> getSearchHits() {
			return searchHits;
		}

		@Override
		public long getTotalHits() {
			return totalHitsRelation == TotalHitsRelation.EQUAL_TO ? totalHits : searchHits.size();
		}

		@Override
		public TotalHitsRelation getTotalHitsRelation() {
			return totalHitsRelation;
		}
	}
}
