/*
 * Copyright 2023 the original author or authors.
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

import io.vanslog.spring.data.meilisearch.core.MeilisearchTemplate;
import io.vanslog.spring.data.meilisearch.entities.Movie;
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTest;
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTestConfiguration;
import io.vanslog.spring.data.meilisearch.repository.config.EnableMeilisearchRepositories;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;

/**
 * Integration tests for {@link MeilisearchRepository}.
 *
 * @author Junghoon Ban
 */
@MeilisearchTest
@ContextConfiguration(classes = MeilisearchRepositoryIntegrationTests.Config.class)
class MeilisearchRepositoryIntegrationTests {

	@Autowired private MovieRepository movieRepository;
	@Autowired private MeilisearchTemplate meilisearchTemplate;

	@BeforeEach
	void setUp() {
		movieRepository.deleteAll();
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
		List<Movie> saved = (List<Movie>) movieRepository.findAll();
		assertThat(saved.size()).isEqualTo(1);
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
		List<Movie> saved = (List<Movie>) movieRepository.findAll();
		assertThat(saved.size()).isEqualTo(1);
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
	void shouldFindDocumentsWithPaging() {
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

		assertThat(page1.getContent()).hasSize(2);
		assertThat(page1.getContent().get(0)).isEqualTo(movie1);
		assertThat(page1.getContent().get(1)).isEqualTo(movie2);

		assertThat(page2.getContent()).hasSize(1);
		assertThat(page2.getContent().get(0)).isEqualTo(movie3);
	}

	@Test
	void shouldFindDocumentsWithSorting() {
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

		List<Movie> movies = List.of(movie1, movie3, movie2);
		movieRepository.saveAll(movies);

		// when
		Iterable<Movie> sorted = movieRepository.findAll(Sort.by(Sort.Direction.DESC, "title"));

		// then
		assertThat(sorted).hasSize(3)
				.containsExactly(movie2, movie3, movie1);
	}

	interface MovieRepository extends MeilisearchRepository<Movie, Integer> {}

	@Configuration
	@Import(MeilisearchTestConfiguration.class)
	@EnableMeilisearchRepositories(basePackages = "io.vanslog.spring.data.meilisearch.repository",
			considerNestedRepositories = true)
	static class Config {}
}
