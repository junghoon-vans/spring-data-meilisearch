/*
 * Copyright 2023-2026 the original author or authors.
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

import io.vanslog.spring.data.meilisearch.core.convert.MeilisearchConverter;

/**
 * The operations for <a href="https://www.meilisearch.com/docs/reference/api/overview">Meilisearch APIs</a>.
 *
 * @author Junghoon Ban
 * @see com.meilisearch.sdk.Client
 * @see com.meilisearch.sdk.Index
 */
public interface MeilisearchOperations extends DocumentOperations, SearchOperations {

	/**
	 * Return operations for instance-level APIs.
	 *
	 * @return instance operations
	 */
	MeilisearchInstanceOperations instanceOps();

	/**
	 * Return operations bound to the index resolved for the given entity class.
	 *
	 * @param entityClass the entity class, must be annotated with
	 *          {@link io.vanslog.spring.data.meilisearch.annotations.Document}
	 * @return index operations
	 */
	MeilisearchIndexOperations indexOps(Class<?> entityClass);

	/**
	 * Return operations bound to the given index uid.
	 *
	 * @param indexUid the index uid, must not be empty
	 * @return index operations
	 */
	MeilisearchIndexOperations indexOps(String indexUid);

	/**
	 * Apply the default settings for the given entity class.
	 * 
	 * @param clazz the entity class, must be annotated with
	 *          {@link io.vanslog.spring.data.meilisearch.annotations.Document}
	 * @param <T> the type of the entity
	 */
	<T> void applySettings(Class<T> clazz);

	/**
	 * Return the {@link io.vanslog.spring.data.meilisearch.core.convert.MeilisearchConverter}.
	 *
	 * @return {@link io.vanslog.spring.data.meilisearch.core.convert.MeilisearchConverter}
	 */
	MeilisearchConverter getMeilisearchConverter();
}
