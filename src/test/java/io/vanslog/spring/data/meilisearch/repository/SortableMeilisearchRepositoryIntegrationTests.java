/*
 * Copyright 2025 the original author or authors.
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

import io.vanslog.spring.data.meilisearch.entities.SortableMovie;
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTest;
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTestConfiguration;
import io.vanslog.spring.data.meilisearch.repository.config.EnableMeilisearchRepositories;

import java.util.List;

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
@ContextConfiguration(classes = SortableMeilisearchRepositoryIntegrationTests.Config.class)
class SortableMeilisearchRepositoryIntegrationTests {

	private SortableMovieRepository movieRepository;

	@Autowired
	void SortableMeilisearchRepositoryIntegrationTests(SortableMovieRepository movieRepository) {
		this.movieRepository = movieRepository;
	}

	@BeforeEach
	void setUp() {
		movieRepository.deleteAll();
	}

	@Test
	void shouldFindDocumentsWithSorting() {
		// given
		int documentId1 = 1;
		SortableMovie movie1 = new SortableMovie();
		movie1.setId(documentId1);
		movie1.setTitle("Carol");
		movie1.setDescription("A love story");
		movie1.setGenres(new String[] { "Romance", "Drama" });

		int documentId2 = 2;
		SortableMovie movie2 = new SortableMovie();
		movie2.setId(documentId2);
		movie2.setTitle("Wonder Woman");
		movie2.setDescription("A superhero film");
		movie2.setGenres(new String[] { "Action", "Adventure" });

		int documentId3 = 3;
		SortableMovie movie3 = new SortableMovie();
		movie3.setId(documentId3);
		movie3.setTitle("Life of Pi");
		movie3.setDescription("A survival film");
		movie3.setGenres(new String[] { "Adventure", "Drama" });

		List<SortableMovie> movies = List.of(movie1, movie3, movie2);
		movieRepository.saveAll(movies);

		// when
		Iterable<SortableMovie> descOrdered = movieRepository.findAll(Sort.by(Sort.Direction.DESC, "title"));
		Iterable<SortableMovie> ascOrdered = movieRepository.findAll(Sort.by(Sort.Direction.ASC, "title"));

		// then
		assertThat(descOrdered).hasSize(3).containsExactly(movie2, movie3, movie1);
		assertThat(ascOrdered).hasSize(3).containsExactly(movie1, movie3, movie2);
	}

	@Test
	void shouldFindDocumentsWithPagingAndSorting() {
		// given
		int pagingSize = 2;

		int documentId1 = 1;
		SortableMovie movie1 = new SortableMovie();
		movie1.setId(documentId1);
		movie1.setTitle("Carol");
		movie1.setDescription("A love story");
		movie1.setGenres(new String[] { "Romance", "Drama" });

		int documentId2 = 2;
		SortableMovie movie2 = new SortableMovie();
		movie2.setId(documentId2);
		movie2.setTitle("Wonder Woman");
		movie2.setDescription("A superhero film");
		movie2.setGenres(new String[] { "Action", "Adventure" });

		int documentId3 = 3;
		SortableMovie movie3 = new SortableMovie();
		movie3.setId(documentId3);
		movie3.setTitle("Life of Pi");
		movie3.setDescription("A survival film");
		movie3.setGenres(new String[] { "Adventure", "Drama" });

		List<SortableMovie> movies = List.of(movie1, movie2, movie3);
		movieRepository.saveAll(movies);

		// when
		Page<SortableMovie> page1 = movieRepository
				.findAll(PageRequest.of(0, pagingSize, Sort.by(Sort.Direction.ASC, "title")));
		Page<SortableMovie> page2 = movieRepository
				.findAll(PageRequest.of(1, pagingSize, Sort.by(Sort.Direction.ASC, "title")));

		// then
		assertThat(page1.getTotalElements()).isEqualTo(movies.size());

		assertThat(page1.getContent()).hasSize(2);
		assertThat(page1.getContent().get(0)).isEqualTo(movie1);
		assertThat(page1.getContent().get(1)).isEqualTo(movie3);

		assertThat(page2.getContent()).hasSize(1);
		assertThat(page2.getContent().get(0)).isEqualTo(movie2);
	}

	interface SortableMovieRepository extends MeilisearchRepository<SortableMovie, Integer> {}

	@Configuration
	@Import(MeilisearchTestConfiguration.class)
	@EnableMeilisearchRepositories(basePackages = "io.vanslog.spring.data.meilisearch.repository",
			considerNestedRepositories = true)
	static class Config {}
}
