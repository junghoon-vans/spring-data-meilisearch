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
package io.vanslog.spring.data.meilisearch.repository.support;

/**
 * Interface to create {@link io.vanslog.spring.data.meilisearch.repository.support.MeilisearchEntityInformation}.
 *
 * @author Junghoon Ban
 */
public interface MeilisearchEntityInformationCreator {

	/**
	 * Returns {@link io.vanslog.spring.data.meilisearch.repository.support.MeilisearchEntityInformation} for the given
	 * domain class.
	 *
	 * @param domainClass must not be {@literal null}.
	 * @param <T> the type of the entity
	 * @param <ID> the type of the entity's identifier
	 * @return {@link io.vanslog.spring.data.meilisearch.repository.support.MeilisearchEntityInformation}
	 */
	<T, ID> MeilisearchEntityInformation<T, ID> getEntityInformation(Class<T> domainClass);
}
