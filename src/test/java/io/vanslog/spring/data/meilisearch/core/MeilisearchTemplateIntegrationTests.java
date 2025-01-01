/*
 * Copyright 2023-2025 the original author or authors.
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

import com.meilisearch.sdk.SearchRequest;
import io.vanslog.spring.data.meilisearch.annotations.Document;
import io.vanslog.spring.data.meilisearch.client.MeilisearchClient;
import io.vanslog.spring.data.meilisearch.entities.Movie;
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTest;
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTestConfiguration;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.test.context.ContextConfiguration;

import com.meilisearch.sdk.exceptions.MeilisearchException;

/**
 * Integration tests for {@link MeilisearchTemplate}.
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

	@BeforeEach
	void setUp() throws MeilisearchException {
		meilisearchClient.index("movies").deleteAllDocuments();
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
	void shouldSearchDocuments() {

		meilisearchTemplate.save(List.of(movie1, movie2, movie3));

		SearchRequest searchRequest = new SearchRequest("Wonder");
		List<Movie> result = meilisearchTemplate.search(searchRequest, Movie.class);

		assertThat(result).containsExactlyInAnyOrder(movie2);
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
}
