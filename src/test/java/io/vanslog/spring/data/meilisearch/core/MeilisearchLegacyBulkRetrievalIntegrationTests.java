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
package io.vanslog.spring.data.meilisearch.core;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import io.vanslog.spring.data.meilisearch.entities.Movie;
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTest;
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTestConfiguration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

/**
 * Individual document lookups on Meilisearch before native ID-list retrieval.
 *
 * @author Junghoon Ban
 */
@MeilisearchTest(version = "v1.13.3")
@ContextConfiguration(classes = MeilisearchLegacyBulkRetrievalIntegrationTests.Config.class)
class MeilisearchLegacyBulkRetrievalIntegrationTests {

	@Autowired MeilisearchOperations operations;

	@AfterEach
	void tearDown() {
		operations.deleteAll(Movie.class);
	}

	@Test // GH-233
	void fallsBackToIndividualLookupsInRequestOrder() {
		assertThat(operations.instanceOps().version().getPackageVersion()).isEqualTo("1.13.3");
		operations.save(List.of(new Movie(1, "One", "Description", new String[] { "Drama" }),
				new Movie(3, "Three", "Description", new String[] { "Drama" })));

		assertThat(operations.multiGet(Movie.class, List.of("3", "99", "1", "3"))).extracting(Movie::getId)
				.containsExactly(3, 1, 3);
	}

	@Configuration
	static class Config extends MeilisearchTestConfiguration {}
}
