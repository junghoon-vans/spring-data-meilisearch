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

package io.vanslog.spring.data.meilisearch.core;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import io.vanslog.spring.data.meilisearch.annotations.Document;
import io.vanslog.spring.data.meilisearch.entities.Movie;
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTest;
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTestConfiguration;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.test.context.ContextConfiguration;

@MeilisearchTest
@ContextConfiguration(classes = { MeilisearchTestConfiguration.class })
class MeilisearchTemplateTest {

	@Autowired MeilisearchOperations meilisearchTemplate;

	Movie movie1 = new Movie(1, "Carol", "A love story", new String[] { "Romance", "Drama" });
	Movie movie2 = new Movie(2, "Wonder Woman", "A superhero film", new String[] { "Action", "Adventure" });
	Movie movie3 = new Movie(3, "Life of Pi", "A survival film", new String[] { "Adventure", "Drama" });

	@BeforeEach
	void setUp() {
		meilisearchTemplate.deleteAll(Movie.class);
	}

	@Test
	void shouldSaveDocument() {
		meilisearchTemplate.save(movie1);
		Movie saved = meilisearchTemplate.get("1", Movie.class);

		assertThat(saved.getId()).isEqualTo(movie1.getId());
		assertThat(saved.getTitle()).isEqualTo(movie1.getTitle());
		assertThat(saved.getDescription()).isEqualTo(movie1.getDescription());
		assertThat(saved.getGenres()).isEqualTo(movie1.getGenres());
	}

	@Test
	void shouldSaveDocuments() {
		List<Movie> movies = List.of(movie1, movie2);

		meilisearchTemplate.save(movies);
		List<Movie> saved = meilisearchTemplate.multiGet(Movie.class);

		assertThat(saved.size()).isEqualTo(movies.size());
	}

	@Test
	void shouldDeleteDocument() {
		meilisearchTemplate.save(movie1);
		meilisearchTemplate.delete(movie1);

		Movie saved = meilisearchTemplate.get("1", Movie.class);
		assertThat(saved).isNull();
	}

	@Test
	void shouldDeleteDocuments() {
		meilisearchTemplate.save(List.of(movie1, movie2, movie3));
		meilisearchTemplate.delete(Movie.class, List.of("1", "2"));

		List<Movie> saved = meilisearchTemplate.multiGet(Movie.class);

		assertThat(saved.size()).isEqualTo(1);
	}

	@Test
	void shouldDeleteAllDocuments() {
		meilisearchTemplate.save(List.of(movie1, movie2));
		meilisearchTemplate.deleteAll(Movie.class);

		List<Movie> saved = meilisearchTemplate.multiGet(Movie.class);

		assertThat(saved.size()).isZero();
	}

	@Test
	void shouldCountDocuments() {
		meilisearchTemplate.save(List.of(movie1, movie2));
		long count = meilisearchTemplate.count(Movie.class);
		assertThat(count).isEqualTo(2);
	}

	@Test
	void shouldExistsDocument() {
		meilisearchTemplate.save(movie1);

		boolean exists = meilisearchTemplate.exists("1", Movie.class);
		boolean nonExists = meilisearchTemplate.exists("2", Movie.class);

		assertThat(exists).isTrue();
		assertThat(nonExists).isFalse();
	}

	@Test
	void shouldGetCertainDocuments() {
		List<Movie> movies = List.of(movie1, movie2, movie3);

		meilisearchTemplate.save(movies);
		List<Movie> saved = meilisearchTemplate.multiGet(Movie.class, List.of("1", "3", "4"));

		assertThat(saved.size()).isEqualTo(2);
		assertThat(saved.get(0).getTitle()).isEqualTo(movie1.getTitle());
		assertThat(saved.get(1).getTitle()).isEqualTo(movie3.getTitle());
	}

	@Test
	void shouldSaveDocumentWithAnnotatedIdField() {
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
