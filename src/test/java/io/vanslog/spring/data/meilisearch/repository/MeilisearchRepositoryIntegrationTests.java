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
package io.vanslog.spring.data.meilisearch.repository;

import static org.assertj.core.api.Assertions.*;

import com.meilisearch.sdk.exceptions.MeilisearchException;
import com.meilisearch.sdk.model.TaskInfo;

import io.vanslog.spring.data.meilisearch.annotations.Document;
import io.vanslog.spring.data.meilisearch.client.MeilisearchClient;
import io.vanslog.spring.data.meilisearch.entities.Movie;
import io.vanslog.spring.data.meilisearch.annotations.Setting;
import io.vanslog.spring.data.meilisearch.entities.TotalHitsLimited;
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTest;
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTestConfiguration;
import io.vanslog.spring.data.meilisearch.repository.config.EnableMeilisearchRepositories;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.lang.Nullable;

/**
 * Integration tests for {@link MeilisearchRepository}.
 *
 * @author Junghoon Ban
 */
@MeilisearchTest
@ContextConfiguration(classes = MeilisearchRepositoryIntegrationTests.Config.class)
class MeilisearchRepositoryIntegrationTests {

	@Autowired private MovieRepository movieRepository;
	@Autowired private TotalHitsLimitedRepository totalHitsLimitedRepository;
	@Autowired private NestedMovieRepository nestedMovieRepository;
	@Autowired private MeilisearchClient meilisearchClient;

	@BeforeEach
	void setUp() {
		movieRepository.deleteAll();
		totalHitsLimitedRepository.deleteAll();
		nestedMovieRepository.deleteAll();
	}

	@Test
	void shouldSaveDocument() {
		// given
		int documentId = 1;
		Movie movie = new Movie();
		movie.setId(documentId);
		movie.setTitle("Carol");
		movie.setDescription("A love story");
		movie.setGenres(new String[] { "Romance", "Drama" });

		// when
		movieRepository.save(movie);

		// then
		Optional<Movie> saved = movieRepository.findById(documentId);
		assertThat(saved).isPresent();
	}

	@Test
	void shouldFindNestedDocumentByIdThroughRepository() {
		// given
		NestedMovie nestedMovie = new NestedMovie("nested-1", "Nested Search", new MovieDetails("Director", 2026));

		// when
		nestedMovieRepository.save(nestedMovie);

		// then
		Optional<NestedMovie> saved = nestedMovieRepository.findById("nested-1");
		assertThat(saved).isPresent();
		assertThat(saved.get().getDetails()).isNotNull();
		assertThat(saved.get().getDetails().getDirector()).isEqualTo("Director");
		assertThat(saved.get().getDetails().getYear()).isEqualTo(2026);
	}

	@Test
	void shouldSaveDocuments() {
		// given
		int documentId1 = 1;
		Movie movie1 = new Movie();
		movie1.setId(documentId1);
		movie1.setTitle("Carol");
		movie1.setDescription("A love story");
		movie1.setGenres(new String[] { "Romance", "Drama" });

		int documentId2 = 2;
		Movie movie2 = new Movie();
		movie2.setId(documentId2);
		movie2.setTitle("Wonder Woman");
		movie2.setDescription("A superhero film");
		movie2.setGenres(new String[] { "Action", "Adventure" });

		List<Movie> movies = List.of(movie1, movie2);

		// when
		movieRepository.saveAll(movies);

		// then
		Optional<Movie> saved1 = movieRepository.findById(documentId1);
		assertThat(saved1).isPresent();
		Optional<Movie> saved2 = movieRepository.findById(documentId2);
		assertThat(saved2).isPresent();
	}

	@Test
	void shouldSaveNoDocumentsForEmptyIterable() {
		Movie movie = new Movie(1, "Carol", "A love story", new String[] { "Romance" });
		movieRepository.save(movie);

		assertThat(movieRepository.saveAll(List.<Movie> of())).isEmpty();
		assertThat(movieRepository.findAll()).containsExactly(movie);
	}

	@Test
	void shouldFindDocumentByIdBeyondFirstDocumentsPage() {
		List<Movie> movies = new ArrayList<>();
		for (int id = 1; id <= 25; id++) {
			movies.add(new Movie(id, "Movie " + id, "description", new String[] { "Drama" }));
		}
		movieRepository.saveAll(movies);

		assertThat(movieRepository.findById(25)).isPresent();
		assertThat(movieRepository.findAllById(List.of(25))).extracting(Movie::getId).containsExactly(25);
	}

	@Test
	void shouldDeleteDocument() {
		// given
		int documentId = 1;
		Movie movie = new Movie();
		movie.setId(documentId);
		movie.setTitle("Carol");
		movie.setDescription("A love story");
		movie.setGenres(new String[] { "Romance", "Drama" });

		// when
		movieRepository.save(movie);
		movieRepository.delete(movie);

		// then
		Optional<Movie> saved = movieRepository.findById(documentId);
		assertThat(saved).isEmpty();
	}

	@Test
	void shouldDeleteDocumentById() {
		// given
		int documentId = 1;
		Movie movie = new Movie();
		movie.setId(documentId);
		movie.setTitle("Carol");
		movie.setDescription("A love story");
		movie.setGenres(new String[] { "Romance", "Drama" });

		// when
		movieRepository.save(movie);
		movieRepository.deleteById(documentId);

		// then
		Optional<Movie> saved = movieRepository.findById(documentId);
		assertThat(saved).isEmpty();
	}

	@Test
	void shouldDeleteDocuments() {
		// given
		int documentId1 = 1;
		Movie movie1 = new Movie();
		movie1.setId(documentId1);
		movie1.setTitle("Carol");
		movie1.setDescription("A love story");
		movie1.setGenres(new String[] { "Romance", "Drama" });

		int documentId2 = 2;
		Movie movie2 = new Movie();
		movie2.setId(documentId2);
		movie2.setTitle("Wonder Woman");
		movie2.setDescription("A superhero film");
		movie2.setGenres(new String[] { "Action", "Adventure" });

		int documentId3 = 3;
		Movie movie3 = new Movie();
		movie3.setId(documentId3);
		movie3.setTitle("Life of Pi");
		movie3.setDescription("A survival film");
		movie3.setGenres(new String[] { "Adventure", "Drama" });

		List<Movie> movies = List.of(movie1, movie2, movie3);

		// when
		movieRepository.saveAll(movies);
		movieRepository.deleteAll(List.of(movie1, movie2));

		// then
		Iterable<Movie> saved = movieRepository.findAll();
		assertThat(saved).hasSize(1);
	}

	@Test
	void shouldDeleteNoDocumentsForEmptyIterable() {
		Movie movie = new Movie(1, "Carol", "A love story", new String[] { "Romance" });
		movieRepository.save(movie);

		movieRepository.deleteAll(List.<Movie> of());

		assertThat(movieRepository.findAll()).containsExactly(movie);
	}

	@Test
	void shouldDeleteDocumentsById() {
		// given
		int documentId1 = 1;
		Movie movie1 = new Movie();
		movie1.setId(documentId1);
		movie1.setTitle("Carol");
		movie1.setDescription("A love story");
		movie1.setGenres(new String[] { "Romance", "Drama" });

		int documentId2 = 2;
		Movie movie2 = new Movie();
		movie2.setId(documentId2);
		movie2.setTitle("Wonder Woman");
		movie2.setDescription("A superhero film");
		movie2.setGenres(new String[] { "Action", "Adventure" });

		int documentId3 = 3;
		Movie movie3 = new Movie();
		movie3.setId(documentId3);
		movie3.setTitle("Life of Pi");
		movie3.setDescription("A survival film");
		movie3.setGenres(new String[] { "Adventure", "Drama" });

		List<Movie> movies = List.of(movie1, movie2, movie3);

		// when
		movieRepository.saveAll(movies);
		movieRepository.deleteAllById(List.of(documentId1, documentId2));

		// then
		Iterable<Movie> saved = movieRepository.findAll();
		assertThat(saved).hasSize(1);
	}

	@Test
	void shouldCountDocuments() {
		// given
		int documentId1 = 1;
		Movie movie1 = new Movie();
		movie1.setId(documentId1);
		movie1.setTitle("Carol");
		movie1.setDescription("A love story");
		movie1.setGenres(new String[] { "Romance", "Drama" });

		int documentId2 = 2;
		Movie movie2 = new Movie();
		movie2.setId(documentId2);
		movie2.setTitle("Wonder Woman");
		movie2.setDescription("A superhero film");
		movie2.setGenres(new String[] { "Action", "Adventure" });

		List<Movie> movies = List.of(movie1, movie2);

		// when
		movieRepository.saveAll(movies);
		long count = movieRepository.count();

		// then
		assertThat(count).isEqualTo(movies.size());
	}

	@Test
	void shouldExistsDocument() {
		// given
		int documentId = 1;
		Movie movie = new Movie();
		movie.setId(documentId);
		movie.setTitle("Carol");
		movie.setDescription("A love story");
		movie.setGenres(new String[] { "Romance", "Drama" });

		int nonExistingDocumentId = 2;

		// when
		movieRepository.save(movie);
		boolean exists = movieRepository.existsById(documentId);
		boolean notExists = movieRepository.existsById(nonExistingDocumentId);

		// then
		assertThat(exists).isTrue();
		assertThat(notExists).isFalse();
	}

	@Test
	void shouldFindDocumentsWithPagingAndUseSearchTotalHitsAsPageTotal() {
		// given
		int pagingSize = 2;

		int documentId1 = 1;
		Movie movie1 = new Movie();
		movie1.setId(documentId1);
		movie1.setTitle("Carol");
		movie1.setDescription("A love story");
		movie1.setGenres(new String[] { "Romance", "Drama" });

		int documentId2 = 2;
		Movie movie2 = new Movie();
		movie2.setId(documentId2);
		movie2.setTitle("Wonder Woman");
		movie2.setDescription("A superhero film");
		movie2.setGenres(new String[] { "Action", "Adventure" });

		int documentId3 = 3;
		Movie movie3 = new Movie();
		movie3.setId(documentId3);
		movie3.setTitle("Life of Pi");
		movie3.setDescription("A survival film");
		movie3.setGenres(new String[] { "Adventure", "Drama" });

		List<Movie> movies = List.of(movie1, movie2, movie3);
		movieRepository.saveAll(movies);

		// when
		Page<Movie> page1 = movieRepository.findAll(PageRequest.of(0, pagingSize));
		Page<Movie> page2 = movieRepository.findAll(PageRequest.of(1, pagingSize));

		// then
		assertThat(page1.getTotalElements()).isEqualTo(movies.size());
		assertThat(page2.getTotalElements()).isEqualTo(movies.size());

		assertThat(page1.getContent()).hasSize(2);
		assertThat(page1.getContent().get(0)).isEqualTo(movie1);
		assertThat(page1.getContent().get(1)).isEqualTo(movie2);

		assertThat(page2.getContent()).hasSize(1);
		assertThat(page2.getContent().get(0)).isEqualTo(movie3);
	}

	@Test
	void shouldReturnEveryUnpagedDocumentWhilePageHitsRemainCapped() {
		// given
		int elementCount = 11;

		for (int i = 0; i < elementCount; i++) {
			TotalHitsLimited entity = new TotalHitsLimited(); // It is limited to 10 hits.
			entity.id = i;
			entity.name = "name" + i;
			totalHitsLimitedRepository.save(entity);
		}

		// when
		Page<TotalHitsLimited> page = totalHitsLimitedRepository.findAll(PageRequest.of(0, elementCount));
		Page<TotalHitsLimited> unpaged = totalHitsLimitedRepository.findAll(Pageable.unpaged());

		// then
		assertThat(totalHitsLimitedRepository.count()).isEqualTo(elementCount);
		assertThat(page).hasSize(10);
		assertThat(page.getTotalElements()).isEqualTo(elementCount);
		assertThat(page.getTotalPages()).isEqualTo(1);
		assertThat(unpaged.getContent()).hasSize(elementCount);
		assertThat(unpaged.getTotalElements()).isEqualTo(elementCount);
		assertThat(unpaged.getContent()).extracting(entity -> entity.name)
				.containsExactlyInAnyOrderElementsOf(IntStream.range(0, elementCount).mapToObj(i -> "name" + i).toList());
		assertThat(totalHitsLimitedRepository.findAll()).hasSize(elementCount);
		assertThat(totalHitsLimitedRepository.findAll(Sort.by("name"))).extracting(entity -> entity.name).containsExactly(
				"name0", "name1", "name10", "name2", "name3", "name4", "name5", "name6", "name7", "name8", "name9");
	}

	@Test
	void shouldRetrieveMoreThanLegacyBatchSize() {
		List<Movie> movies = IntStream.range(0, 501)
				.mapToObj(id -> new Movie(id, "Movie " + id, "Description", new String[] { "Drama" })).toList();
		movieRepository.saveAll(movies);

		assertThat(movieRepository.findAll()).containsExactlyInAnyOrderElementsOf(movies);
	}

	@Test
	void shouldFindFilterBackedDerivedQueriesWithPageTotals() {
		Movie first = new Movie(1, "Carol", "A love story", new String[] { "Drama" });
		Movie second = new Movie(2, "Life of Pi", "A survival film", new String[] { "Drama", "Adventure" });
		Movie other = new Movie(3, "Wonder Woman", "A superhero film", new String[] { "Action" });
		movieRepository.saveAll(List.of(first, second, other));

		assertThat(movieRepository.findByGenres("Drama")).containsExactlyInAnyOrder(first, second);
		assertThat(movieRepository.findByGenresIn(List.of("Drama", "Action"))).containsExactlyInAnyOrder(first, second,
				other);
		assertThat(movieRepository.findByGenresNotIn(List.of("Drama"))).containsExactly(other);
		Page<Movie> page = movieRepository.findByGenres("Drama", PageRequest.of(0, 1));
		assertThat(page.getContent()).hasSize(1);
		assertThat(page.getTotalElements()).isEqualTo(2);
		assertThat(page.getTotalPages()).isEqualTo(2);
	}

	@Test
	void shouldFilterTitlesByLiteralPrefixAcrossRepositoryOperations() {
		Movie first = new Movie(1, "Star Trek", "Starts with Star", new String[] { "Sci-Fi" });
		Movie quoted = new Movie(2, "Star \"Quest\"", "Escaped prefix", new String[] { "Sci-Fi" });
		Movie middle = new Movie(3, "The Star", "Contains Star", new String[] { "Sci-Fi" });
		movieRepository.saveAll(List.of(first, quoted, middle));

		assertThat(movieRepository.findByTitleStartingWith("Star")).containsExactlyInAnyOrder(first, quoted);
		assertThat(movieRepository.findByTitleStartingWith("Star \"")).containsExactly(quoted);
		assertThat(movieRepository.countByTitleStartingWith("Star")).isEqualTo(2);
		assertThat(movieRepository.existsByTitleStartingWith("The")).isTrue();
		assertThat(movieRepository.deleteByTitleStartingWith("Star \"")).isEqualTo(1);
		assertThat(movieRepository.findAll()).containsExactlyInAnyOrder(first, middle);
	}

	@Test
	void shouldFindDocumentWithMissingFieldByNullArgument() {
		Movie withoutGenres = new Movie(1, "Untyped", "No genres", null);
		Movie withGenres = new Movie(2, "Typed", "Has genres", new String[] { "Drama" });
		movieRepository.saveAll(List.of(withoutGenres, withGenres));

		assertThat(movieRepository.findByGenres(null)).containsExactly(withoutGenres);
		assertThat(movieRepository.findByGenres("Drama")).containsExactly(withGenres);
	}

	@Test
	void shouldDistinguishMissingAndPresentFilterableFields() {
		Movie missing = new Movie(1, "Untyped", "No genres", null);
		Movie present = new Movie(2, "Typed", "Has genres", new String[] { "Drama" });
		movieRepository.saveAll(List.of(missing, present));

		assertThat(movieRepository.findByGenresIsNull()).containsExactly(missing);
		assertThat(movieRepository.findByGenresIsNotNull()).containsExactly(present);
		assertThat(movieRepository.findByGenresExists()).containsExactly(present);
	}

	@Test
	void shouldDistinguishExplicitNullFromMissingField() throws MeilisearchException {
		Movie missing = new Movie(1, "Missing", "No genres field", null);
		Movie present = new Movie(2, "Present", "Has genres", new String[] { "Drama" });
		movieRepository.saveAll(List.of(missing, present));

		var index = meilisearchClient.index("movies");
		TaskInfo task = index.addDocuments("""
				[{"id":3,"title":"Explicit null","description":"Null genres field","genres":null}]
				""");
		index.waitForTask(task.getTaskUid(), meilisearchClient.getRequestTimeout(), meilisearchClient.getRequestInterval());

		assertThat(movieRepository.findByGenres(null)).extracting(Movie::getId).containsExactlyInAnyOrder(1, 3);
		assertThat(movieRepository.findByGenresIsNull()).extracting(Movie::getId).containsExactlyInAnyOrder(1, 3);
		assertThat(movieRepository.findByGenresIsNotNull()).extracting(Movie::getId).containsExactly(2);
		assertThat(movieRepository.findByGenresExists()).extracting(Movie::getId).containsExactlyInAnyOrder(2, 3);
	}

	@Test
	void shouldGroupBooleanPredicatesOnNestedMappedFields() {
		NestedMovie first = new NestedMovie("nested-1", "First", new MovieDetails("Director", 2026));
		NestedMovie second = new NestedMovie("nested-2", "Second", new MovieDetails("Other", 2025));
		NestedMovie absent = new NestedMovie("nested-3", "Missing", null);
		nestedMovieRepository.saveAll(List.of(first, second, absent));

		assertThat(nestedMovieRepository.findByDetailsDirector("Director")).extracting(NestedMovie::getId)
				.containsExactly("nested-1");
		assertThat(nestedMovieRepository.findByDetailsDirectorIsNull()).extracting(NestedMovie::getId)
				.containsExactly("nested-3");
		assertThat(nestedMovieRepository.findByDetailsDirectorIsNotNull()).extracting(NestedMovie::getId)
				.containsExactlyInAnyOrder("nested-1", "nested-2");
		assertThat(nestedMovieRepository.findByDetailsDirectorExists()).extracting(NestedMovie::getId)
				.containsExactlyInAnyOrder("nested-1", "nested-2");
		assertThat(nestedMovieRepository.findByDetailsDirectorAndTitleOrDetailsDirector("Director", "First", "Other"))
				.extracting(NestedMovie::getId).containsExactlyInAnyOrder("nested-1", "nested-2");
		assertThat(nestedMovieRepository.countByDetailsDirectorOrTitle("Director", "Second")).isEqualTo(2);
		assertThat(nestedMovieRepository.existsByDetailsDirectorOrTitle("Director", "No match")).isTrue();
		assertThat(nestedMovieRepository.deleteByDetailsDirectorOrTitle("Director", "Missing")).isEqualTo(2);
		assertThat(nestedMovieRepository.findAll()).extracting(NestedMovie::getId).containsExactly("nested-2");
	}

	@Test
	void shouldFilterNestedPropertiesInArrays() {
		NestedMovie first = new NestedMovie("nested-1", "First", null,
				List.of(new MovieDetails("Other", 2025), new MovieDetails("Director", 2026)));
		NestedMovie second = new NestedMovie("nested-2", "Second", null, List.of(new MovieDetails("Other", 2024)));
		NestedMovie absent = new NestedMovie("nested-3", "Missing", null);
		nestedMovieRepository.saveAll(List.of(first, second, absent));

		assertThat(nestedMovieRepository.findByCreditsDirector("Director")).extracting(NestedMovie::getId)
				.containsExactly("nested-1");
		assertThat(nestedMovieRepository.findByCreditsDirector("Other")).extracting(NestedMovie::getId)
				.containsExactlyInAnyOrder("nested-1", "nested-2");
		assertThat(nestedMovieRepository.findByCreditsDirectorIsNull()).extracting(NestedMovie::getId)
				.containsExactly("nested-3");
	}

	@Test
	void shouldCountAndCheckExistenceWithDerivedFilters() {
		Movie first = new Movie(1, "Carol", "A love story", new String[] { "Drama" });
		Movie second = new Movie(2, "Life of Pi", "A survival film", new String[] { "Drama", "Adventure" });
		Movie other = new Movie(3, "Wonder Woman", "A superhero film", new String[] { "Action" });
		movieRepository.saveAll(List.of(first, second, other));

		assertThat(movieRepository.countByGenres("Drama")).isEqualTo(2);
		assertThat(movieRepository.countByGenres("Comedy")).isZero();
		assertThat(movieRepository.existsByGenres("Drama")).isTrue();
		assertThat(movieRepository.existsByGenres("Comedy")).isFalse();
	}

	@Test
	void shouldCheckBoxedExistenceWithNullableCompoundFilter() {
		Movie withoutGenres = new Movie(1, "Untyped", "No genres", null);
		Movie withGenres = new Movie(2, "Typed", "Drama", new String[] { "Drama" });
		movieRepository.saveAll(List.of(withoutGenres, withGenres));

		assertThat(movieRepository.existsByGenresAndTitle(null, "Untyped")).isTrue();
		assertThat(movieRepository.existsByGenresAndTitle(null, "Typed")).isFalse();
		assertThat(movieRepository.existsByGenresAndTitle("Drama", "Typed")).isTrue();
	}

	@Test
	void shouldDeleteByDerivedFilterAndPreserveUnmatchedDocuments() {
		Movie first = new Movie(1, "Carol", "A love story", new String[] { "Drama" });
		Movie second = new Movie(2, "Life of Pi", "A survival film", new String[] { "Drama", "Adventure" });
		Movie other = new Movie(3, "Wonder Woman", "A superhero film", new String[] { "Action" });
		movieRepository.saveAll(List.of(first, second, other));

		assertThat(movieRepository.deleteByGenres("Drama")).isEqualTo(2);
		assertThat(movieRepository.deleteByGenres("Comedy")).isZero();
		assertThat(movieRepository.findByGenres("Drama")).isEmpty();
		assertThat(movieRepository.findById(other.getId())).contains(other);
	}

	@Test
	void shouldRemoveByDerivedFilterAndPreserveUnmatchedDocuments() {
		Movie first = new Movie(1, "Carol", "A love story", new String[] { "Drama" });
		Movie second = new Movie(2, "Life of Pi", "A survival film", new String[] { "Drama", "Adventure" });
		Movie other = new Movie(3, "Wonder Woman", "A superhero film", new String[] { "Action" });
		movieRepository.saveAll(List.of(first, second, other));

		assertThat(movieRepository.removeByGenres("Drama")).isEqualTo(2);
		assertThat(movieRepository.removeByGenres("Comedy")).isZero();
		assertThat(movieRepository.findByGenres("Drama")).isEmpty();
		assertThat(movieRepository.findById(other.getId())).contains(other);
	}

	@Test
	void shouldDeleteByDerivedFilterWithVoidReturn() {
		Movie first = new Movie(1, "Carol", "A love story", new String[] { "Drama" });
		Movie second = new Movie(2, "Life of Pi", "A survival film", new String[] { "Drama", "Adventure" });
		Movie other = new Movie(3, "Wonder Woman", "A superhero film", new String[] { "Action" });
		movieRepository.saveAll(List.of(first, second, other));

		movieRepository.deleteByGenresIn(List.of("Drama"));

		assertThat(movieRepository.findById(first.getId())).isEmpty();
		assertThat(movieRepository.findById(second.getId())).isEmpty();
		assertThat(movieRepository.findById(other.getId())).contains(other);
	}

	interface MovieRepository extends MeilisearchRepository<Movie, Integer> {

		List<Movie> findByGenres(@Nullable String genre);

		List<Movie> findByGenresIsNull();

		List<Movie> findByGenresIsNotNull();

		List<Movie> findByGenresExists();

		List<Movie> findByGenresIn(List<String> genres);

		List<Movie> findByGenresNotIn(List<String> genres);

		Page<Movie> findByGenres(String genre, Pageable pageable);

		List<Movie> findByTitleStartingWith(String prefix);

		long countByTitleStartingWith(String prefix);

		boolean existsByTitleStartingWith(String prefix);

		long deleteByTitleStartingWith(String prefix);

		long countByGenres(String genre);

		boolean existsByGenres(String genre);

		Boolean existsByGenresAndTitle(@Nullable String genre, String title);

		long deleteByGenres(String genre);

		long removeByGenres(String genre);

		void deleteByGenresIn(List<String> genres);
	}

	interface TotalHitsLimitedRepository extends MeilisearchRepository<TotalHitsLimited, String> {}

	interface NestedMovieRepository extends MeilisearchRepository<NestedMovie, String> {

		List<NestedMovie> findByDetailsDirector(String director);

		List<NestedMovie> findByDetailsDirectorIsNull();

		List<NestedMovie> findByDetailsDirectorIsNotNull();

		List<NestedMovie> findByDetailsDirectorExists();

		List<NestedMovie> findByCreditsDirector(String director);

		List<NestedMovie> findByCreditsDirectorIsNull();

		List<NestedMovie> findByDetailsDirectorAndTitleOrDetailsDirector(String director, String title,
				String otherDirector);

		long countByDetailsDirectorOrTitle(String director, String title);

		boolean existsByDetailsDirectorOrTitle(String director, String title);

		long deleteByDetailsDirectorOrTitle(String director, String title);
	}

	@Setting(filterableAttributes = { "title", "details.director", "credits.director" })
	@Document(indexUid = "nested-movies")
	static class NestedMovie {

		@Id private String id;
		private String title;
		private MovieDetails details;
		private List<MovieDetails> credits;

		@SuppressWarnings("unused")
		NestedMovie() {}

		NestedMovie(String id, String title, MovieDetails details) {
			this(id, title, details, null);
		}

		NestedMovie(String id, String title, MovieDetails details, List<MovieDetails> credits) {
			this.id = id;
			this.title = title;
			this.details = details;
			this.credits = credits;
		}

		public String getId() {
			return id;
		}

		public String getTitle() {
			return title;
		}

		public MovieDetails getDetails() {
			return details;
		}

		public List<MovieDetails> getCredits() {
			return credits;
		}
	}

	static class MovieDetails {

		private String director;
		private int year;

		@SuppressWarnings("unused")
		MovieDetails() {}

		MovieDetails(String director, int year) {
			this.director = director;
			this.year = year;
		}

		public String getDirector() {
			return director;
		}

		public int getYear() {
			return year;
		}
	}

	@Configuration
	@Import(MeilisearchTestConfiguration.class)
	@EnableMeilisearchRepositories(basePackages = "io.vanslog.spring.data.meilisearch.repository",
			considerNestedRepositories = true)
	static class Config {}
}
