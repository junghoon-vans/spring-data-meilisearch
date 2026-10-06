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
package io.vanslog.spring.data.meilisearch.config;

import io.vanslog.spring.data.meilisearch.client.ClientConfiguration;
import io.vanslog.spring.data.meilisearch.client.msc.ReactiveMeilisearchTemplate;
import io.vanslog.spring.data.meilisearch.core.ReactiveMeilisearchOperations;
import io.vanslog.spring.data.meilisearch.core.convert.MeilisearchConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Explicit Java configuration for reactive operations, without a blocking SDK client bean. Extend in an application
 * Configuration class, or register the template directly when sharing mapping beans with an existing blocking
 * configuration.
 *
 * @author Junghoon Ban
 */
public abstract class ReactiveMeilisearchConfiguration extends MeilisearchConfigurationSupport {

	/** Configure the reactive endpoint, credentials and task-observation timeout/interval. */
	@Bean(name = "reactiveMeilisearchClientConfiguration")
	public abstract ClientConfiguration reactiveClientConfiguration();

	/** Create distinct reactive operations/template aliases without constructor-time HTTP requests. */
	@Bean(name = { "reactiveMeilisearchOperations", "reactiveMeilisearchTemplate" })
	public ReactiveMeilisearchOperations reactiveMeilisearchOperations(
			@Qualifier("reactiveMeilisearchClientConfiguration") ClientConfiguration clientConfiguration,
			MeilisearchConverter meilisearchConverter, @Qualifier("meilisearchObjectMapper") ObjectMapper objectMapper) {
		return new ReactiveMeilisearchTemplate(clientConfiguration, meilisearchConverter, objectMapper);
	}
}
