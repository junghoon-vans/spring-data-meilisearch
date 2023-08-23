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

package io.vanslog.spring.data.meilisearch.client;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.meilisearch.sdk.json.GsonJsonHandler;

/**
 * Tests for {@link ClientConfiguration}.
 *
 * @author Junghoon Ban
 */
class ClientConfigurationTest {

	ClientConfiguration clientConfiguration;

	@BeforeEach
	void setup() {
		clientConfiguration = ClientConfiguration.builder().connectedToLocalhost().withApiKey("masterKey").build();
	}

	@Test
	void shouldBuildConfiguration() {
		assertThat(clientConfiguration.getHostUrl()).isEqualTo("http://localhost:7700");
		assertThat(clientConfiguration.getApiKey()).isEqualTo("masterKey");
		assertThat(clientConfiguration.getClientAgents()).isEmpty();
	}

	@Test
	void shouldConfigureJsonHandler() {
		clientConfiguration.withJsonHandler(new GsonJsonHandler());
		assertThat(clientConfiguration.getJsonHandler()).isInstanceOf(GsonJsonHandler.class);
	}
}
