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
package io.vanslog.spring.data.meilisearch.config;

import static org.assertj.core.api.Assertions.*;

import io.vanslog.spring.data.meilisearch.annotations.Document;
import io.vanslog.spring.data.meilisearch.client.ClientConfiguration;
import io.vanslog.spring.data.meilisearch.client.MeilisearchClient;
import io.vanslog.spring.data.meilisearch.core.MeilisearchOperations;
import io.vanslog.spring.data.meilisearch.repository.MeilisearchRepository;
import io.vanslog.spring.data.meilisearch.repository.config.EnableMeilisearchRepositories;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.meilisearch.sdk.json.JacksonJsonHandler;
import com.meilisearch.sdk.json.JsonHandler;

/**
 * Annotation based configuration test.
 *
 * @author Junghoon Ban
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration
class MeilisearchConfigurationUnitTests {

	@Autowired private MeilisearchClient meilisearchClient;
	@Autowired private MeilisearchOperations meilisearchTemplate;
	@Autowired private ApplySettingsFalseRepository applySettingsFalseRepository;

	@Test
	void shouldCreateMeilisearchClient() {
		assertThat(meilisearchClient).isNotNull();
	}

	@Test
	void shouldConfigureMeilisearchClient() {
		assertThat(meilisearchClient.getJsonHandler()).isInstanceOf(JacksonJsonHandler.class);
		assertThat(meilisearchClient.getRequestTimeout()).isEqualTo(2000);
		assertThat(meilisearchClient.getRequestInterval()).isEqualTo(20);
	}

	@Test
	void shouldCreateMeilisearchTemplate() {
		assertThat(meilisearchTemplate).isNotNull();
	}

	@Test
	void shouldCreateMeilisearchRepository() {
		assertThat(applySettingsFalseRepository).isNotNull();
	}

	interface ApplySettingsFalseRepository extends MeilisearchRepository<ApplySettingsFalseEntity, String> {}

	@Document(indexUid = "test-index-config-namespace", applySettings = false)
	record ApplySettingsFalseEntity(@Id String id) {
	}

	@Configuration
	@EnableMeilisearchRepositories(basePackages = { "io.vanslog.spring.data.meilisearch.config" },
			considerNestedRepositories = true)
	static class CustomConfiguration extends MeilisearchConfiguration {
		@Override
		public ClientConfiguration clientConfiguration() {
			return ClientConfiguration.builder() //
					.connectedToLocalhost() //
					.withApiKey("masterKey") //
					.withRequestTimeout(2000) //
					.withRequestInterval(20).build();
		}

		@Override
		public JsonHandler jsonHandler() {
			return new JacksonJsonHandler();
		}
	}
}
