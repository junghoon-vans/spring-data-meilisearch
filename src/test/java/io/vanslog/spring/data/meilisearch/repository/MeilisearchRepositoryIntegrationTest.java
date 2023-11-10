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

import io.vanslog.spring.data.meilisearch.entities.FilmDistributor;
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
import org.springframework.data.repository.CrudRepository;
import org.springframework.test.context.ContextConfiguration;

/**
 * Integration tests for {@link MeilisearchRepository}.
 *
 * @author Junghoon Ban
 */
@MeilisearchTest
@ContextConfiguration(classes = MeilisearchRepositoryIntegrationTest.Config.class)
class MeilisearchRepositoryIntegrationTest {

	@Autowired private MovieTheaterRepository repository;

	@BeforeEach
	void setUp() {
		repository.deleteAll();
	}

	@Test
	void shouldSaveSimpleEntity() {

		FilmDistributor waltDisney = new FilmDistributor();
		waltDisney.setId(1);

		repository.save(waltDisney);

		Optional<FilmDistributor> saved = repository.findById(1);
		assertThat(saved).isPresent();
	}

	@Test
	void shouldSaveSimpleEntities() {

		FilmDistributor waltDisney = new FilmDistributor();
		waltDisney.setId(1);
		FilmDistributor warnerBros = new FilmDistributor();
		warnerBros.setId(2);

		List<FilmDistributor> distributors = List.of(waltDisney, warnerBros);

		repository.saveAll(distributors);

		assertThat(repository.findById(1)).isPresent();
		assertThat(repository.findById(2)).isPresent();
	}

	@Test
	void shouldSaveComplexEntity() {

		FilmDistributor waltDisney = new FilmDistributor();
		waltDisney.setId(1);
		waltDisney.setMovies(List.of(
				new Movie(1, "Frozen", "Let it go", new String[] {"Animation", "Adventure", "Comedy"}),
				new Movie(2, "The Lion King", "The circle of life", new String[] {"Animation", "Adventure", "Drama"}),
				new Movie(3, "Toy Story", "You've got a friend in me", new String[] {"Animation", "Adventure", "Comedy"})
		));

		repository.save(waltDisney);

		Optional<FilmDistributor> saved = repository.findById(1);
		assertThat(saved).isPresent();
	}

	@Test
	void shouldDeleteEntity() {

		FilmDistributor waltDisney = new FilmDistributor();
		waltDisney.setId(1);
		repository.save(waltDisney);

		repository.delete(waltDisney);

		Optional<FilmDistributor> saved = repository.findById(1);
		assertThat(saved).isEmpty();
	}

	@Test
	void shouldDeleteEntityById() {

		FilmDistributor waltDisney = new FilmDistributor();
		waltDisney.setId(1);
		repository.save(waltDisney);

		repository.deleteById(1);

		Optional<FilmDistributor> saved = repository.findById(1);
		assertThat(saved).isEmpty();
	}

	@Test
	void shouldDeleteAllEntities() {

		FilmDistributor waltDisney = new FilmDistributor();
		waltDisney.setId(1);
		FilmDistributor warnerBros = new FilmDistributor();
		warnerBros.setId(2);
		FilmDistributor universal = new FilmDistributor();
		universal.setId(3);

		List<FilmDistributor> distributors = List.of(waltDisney, warnerBros, universal);
		repository.saveAll(distributors);

		repository.deleteAll();

		List<FilmDistributor> saved = (List<FilmDistributor>) repository.findAll();
		assertThat(saved).isEmpty();
	}

	@Test
	void shouldDeleteEntitiesById() {

		FilmDistributor waltDisney = new FilmDistributor();
		waltDisney.setId(1);
		FilmDistributor warnerBros = new FilmDistributor();
		warnerBros.setId(2);
		FilmDistributor universal = new FilmDistributor();
		universal.setId(3);

		List<FilmDistributor> distributors = List.of(waltDisney, warnerBros, universal);
		repository.saveAll(distributors);

		repository.deleteAll(List.of(waltDisney, warnerBros));

		List<FilmDistributor> saved = (List<FilmDistributor>) repository.findAll();
		assertThat(saved).hasSize(1);
	}

	@Test
	void shouldDeleteDocumentsById() {

		FilmDistributor waltDisney = new FilmDistributor();
		waltDisney.setId(1);
		FilmDistributor warnerBros = new FilmDistributor();
		warnerBros.setId(2);
		FilmDistributor universal = new FilmDistributor();
		universal.setId(3);

		List<FilmDistributor> distributors = List.of(waltDisney, warnerBros, universal);
		repository.saveAll(distributors);

		repository.deleteAllById(List.of(1, 2));

		List<FilmDistributor> saved = (List<FilmDistributor>) repository.findAll();
		assertThat(saved).hasSize(1);
	}

	@Test
	void shouldCountDocuments() {

		FilmDistributor waltDisney = new FilmDistributor();
		waltDisney.setId(1);
		FilmDistributor warnerBros = new FilmDistributor();
		warnerBros.setId(2);

		List<FilmDistributor> distributors = List.of(waltDisney, warnerBros);
		repository.saveAll(distributors);

		long count = repository.count();

		assertThat(count).isEqualTo(distributors.size());
	}

	@Test
	void shouldExistsDocument() {

		FilmDistributor waltDisney = new FilmDistributor();
		waltDisney.setId(1);

		repository.save(waltDisney);
		boolean exists = repository.existsById(1);
		boolean notExists = repository.existsById(2);

		assertThat(exists).isTrue();
		assertThat(notExists).isFalse();
	}

	interface MovieTheaterRepository extends CrudRepository<FilmDistributor, Integer> {}

	@Configuration
	@Import(MeilisearchTestConfiguration.class)
	@EnableMeilisearchRepositories(basePackages = "io.vanslog.spring.data.meilisearch.repository",
			considerNestedRepositories = true)
	static class Config {}
}
