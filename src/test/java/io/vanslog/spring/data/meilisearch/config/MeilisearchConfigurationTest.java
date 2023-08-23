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

package io.vanslog.spring.data.meilisearch.config;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import io.vanslog.spring.data.meilisearch.client.ClientConfiguration;
import io.vanslog.spring.data.meilisearch.core.MeilisearchOperations;
import io.vanslog.spring.data.meilisearch.entities.Movie;
import io.vanslog.spring.data.meilisearch.repository.MeilisearchRepository;
import io.vanslog.spring.data.meilisearch.repository.config.EnableMeilisearchRepositories;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.json.JacksonJsonHandler;
import com.meilisearch.sdk.json.JsonHandler;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
class MeilisearchConfigurationTest {

	@Autowired private Client client;
	@Autowired private MeilisearchOperations meilisearchTemplate;
	@Autowired private MovieRepository movieRepository;

	@Test
	void shouldCreateMeilisearchClient() {
		assertThat(client).isNotNull();
	}

	@Test
	void shouldCreateMeilisearchTemplate() {
		assertThat(meilisearchTemplate).isNotNull();
	}

	@Test
	void shouldCreateMeilisearchRepository() {
		assertThat(movieRepository).isNotNull();
	}

	interface MovieRepository extends MeilisearchRepository<Movie, String> {}

	@Configuration
	@EnableMeilisearchRepositories(basePackages = { "io.vanslog.spring.data.meilisearch.config" },
			considerNestedRepositories = true)
	static class CustomConfiguration extends MeilisearchConfiguration {
		@Override
		public ClientConfiguration clientConfiguration() {
			return ClientConfiguration.builder().connectedToLocalhost().withApiKey("masterKey").build();
		}

		@Override
		public JsonHandler jsonHandler() {
			return new JacksonJsonHandler();
		}
	}
}
