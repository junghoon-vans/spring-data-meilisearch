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

import io.vanslog.spring.data.meilisearch.core.convert.MappingMeilisearchConverter;
import io.vanslog.spring.data.meilisearch.core.convert.MeilisearchConverter;
import io.vanslog.spring.data.meilisearch.core.convert.MeilisearchCustomConversions;
import io.vanslog.spring.data.meilisearch.core.mapping.SimpleMeilisearchMappingContext;

import java.util.Collections;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;

/**
 * Support class for{@link io.vanslog.spring.data.meilisearch.config.MeilisearchConfiguration}.
 *
 * @author Junghoon Ban
 */

@Configuration(proxyBeanMethods = false)
public class MeilisearchConfigurationSupport {

	/**
	 * Create a {@link io.vanslog.spring.data.meilisearch.core.convert.MeilisearchConverter} bean.
	 *
	 * @param meilisearchMappingContext the {@link SimpleMeilisearchMappingContext} to use
	 * @return the created {@link io.vanslog.spring.data.meilisearch.core.convert.MeilisearchConverter} bean.
	 */
	@Bean
	public MeilisearchConverter meilisearchConverter(SimpleMeilisearchMappingContext meilisearchMappingContext,
			MeilisearchCustomConversions meilisearchCustomConversions) {

		MappingMeilisearchConverter meilisearchConverter = new MappingMeilisearchConverter(meilisearchMappingContext);
		meilisearchConverter.setConversions(meilisearchCustomConversions);
		return meilisearchConverter;
	}

	/**
	 * Create a {@link io.vanslog.spring.data.meilisearch.core.mapping.SimpleMeilisearchMappingContext} bean.
	 *
	 * @return the created {@link io.vanslog.spring.data.meilisearch.core.mapping.SimpleMeilisearchMappingContext} bean.
	 */
	@Bean
	public SimpleMeilisearchMappingContext meilisearchMappingContext() {
		return new SimpleMeilisearchMappingContext();
	}

	/**
	 * Register custom {@link Converter}s in a {@link MeilisearchCustomConversions} object if required.
	 *
	 * @return never {@literal null}.
	 */
	@Bean
	public MeilisearchCustomConversions meilisearchCustomConversions() {
		return new MeilisearchCustomConversions(Collections.emptyList());
	}
}
