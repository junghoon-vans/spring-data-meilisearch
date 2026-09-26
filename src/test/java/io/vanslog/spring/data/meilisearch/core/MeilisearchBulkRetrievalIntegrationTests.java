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
import java.util.stream.IntStream;

import io.vanslog.spring.data.meilisearch.entities.Movie;
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTest;
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTestConfiguration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

/**
 * Native document ID-list retrieval on the default Meilisearch release.
 *
 * @author Junghoon Ban
 */
@MeilisearchTest
@ContextConfiguration(classes = MeilisearchBulkRetrievalIntegrationTests.Config.class)
class MeilisearchBulkRetrievalIntegrationTests {

	@Autowired MeilisearchOperations operations;

	@AfterEach
	void tearDown() {
		operations.deleteAll(Movie.class);
	}

	@Test // GH-229
	void retrievesBeyondInternalBatchInRequestOrderAndOmitsMissingIds() {
		List<Movie> movies = IntStream.range(0, 501)
				.mapToObj(id -> new Movie(id, "Movie " + id, "Description", new String[] { "Drama" })).toList();
		operations.save(movies);

		List<String> ids = IntStream.range(0, 501).mapToObj(String::valueOf).toList();
		assertThat(operations.multiGet(Movie.class, ids)).extracting(Movie::getId)
				.containsExactlyElementsOf(IntStream.range(0, 501).boxed().toList());
		assertThat(operations.multiGet(Movie.class, List.of("500", "9999", "0", "21", "500"))).extracting(Movie::getId)
				.containsExactly(500, 0, 21, 500);
	}

	@Configuration
	static class Config extends MeilisearchTestConfiguration {}
}
