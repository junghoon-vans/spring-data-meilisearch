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

import io.vanslog.spring.data.meilisearch.UncategorizedMeilisearchException;
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTest;
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTestConfiguration;
import io.vanslog.spring.data.meilisearch.repository.StartingWithMeilisearch117IntegrationTests.PrefixMovie;
import io.vanslog.spring.data.meilisearch.repository.StartingWithMeilisearch117IntegrationTests.PrefixRepository;
import io.vanslog.spring.data.meilisearch.repository.config.EnableMeilisearchRepositories;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

/**
 * Verifies that the previous Meilisearch release rejects derived prefix filters.
 *
 * @author Junghoon Ban
 */
@MeilisearchTest(version = "v1.16.0")
@ContextConfiguration(classes = StartingWithMeilisearch116IntegrationTests.Config.class)
class StartingWithMeilisearch116IntegrationTests {

	@Autowired private PrefixRepository movieRepository;

	@Test
	void shouldRejectStartingWithOnMeilisearch116() {
		PrefixMovie movie = new PrefixMovie(1, "Star Trek");
		movieRepository.save(movie);

		assertThat(movieRepository.findByTitle("Star Trek")).containsExactly(movie);
		assertThatThrownBy(() -> movieRepository.findByTitleStartingWith("Star"))
				.isInstanceOf(UncategorizedMeilisearchException.class).hasMessageContaining("STARTS WITH");
	}

	@Configuration
	@Import(MeilisearchTestConfiguration.class)
	@EnableMeilisearchRepositories(basePackages = "io.vanslog.spring.data.meilisearch.repository",
			considerNestedRepositories = true)
	static class Config {}
}
