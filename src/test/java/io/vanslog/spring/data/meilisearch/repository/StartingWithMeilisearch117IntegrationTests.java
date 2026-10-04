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
package io.vanslog.spring.data.meilisearch.repository;

import static org.assertj.core.api.Assertions.*;

import io.vanslog.spring.data.meilisearch.annotations.Document;
import io.vanslog.spring.data.meilisearch.annotations.Setting;
import io.vanslog.spring.data.meilisearch.entities.Movie;
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTest;
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTestConfiguration;
import io.vanslog.spring.data.meilisearch.repository.config.EnableMeilisearchRepositories;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

/**
 * Verifies the minimum Meilisearch version for derived prefix filters.
 *
 * @author Junghoon Ban
 */
@MeilisearchTest(version = "v1.17.0")
@ContextConfiguration(classes = StartingWithMeilisearch117IntegrationTests.Config.class)
class StartingWithMeilisearch117IntegrationTests {

	@Autowired private PrefixRepository movieRepository;

	@BeforeEach
	void setUp() {
		movieRepository.deleteAll();
	}

	@Test
	void shouldFindLiteralPrefixOnMeilisearch117() {
		PrefixMovie first = new PrefixMovie(1, "Star Trek");
		PrefixMovie quoted = new PrefixMovie(2, "Star \"Quest\"");
		PrefixMovie middle = new PrefixMovie(3, "The Star");
		movieRepository.saveAll(List.of(first, quoted, middle));

		assertThat(movieRepository.findByTitleStartingWith("Star")).containsExactlyInAnyOrder(first, quoted);
		assertThat(movieRepository.findByTitleStartingWith("Star \"")).containsExactly(quoted);
	}

	interface PrefixRepository extends MeilisearchRepository<PrefixMovie, Integer> {

		List<PrefixMovie> findByTitle(String title);

		List<PrefixMovie> findByTitleStartingWith(String prefix);
	}

	@Setting(filterableAttributes = { "title" })
	@Document(indexUid = "prefix-version-movies")
	static class PrefixMovie extends Movie {

		PrefixMovie() {}

		PrefixMovie(int id, String title) {
			super(id, title, "", null);
		}
	}

	@Configuration
	@Import(MeilisearchTestConfiguration.class)
	@EnableMeilisearchRepositories(basePackages = "io.vanslog.spring.data.meilisearch.repository",
			considerNestedRepositories = true)
	static class Config {}
}
