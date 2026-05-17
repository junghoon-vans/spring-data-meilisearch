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

/**
 * Operations for APIs scoped to a single Meilisearch index.
 *
 * @author Junghoon Ban
 * @see com.meilisearch.sdk.Index
 */
public interface MeilisearchIndexOperations {

	/**
	 * Create the bound index without declaring a primary key.
	 *
	 * @return the created index
	 */
	MeilisearchIndex create();

	/**
	 * Create the bound index using the given request.
	 *
	 * @param request index creation options
	 * @return the created index
	 */
	MeilisearchIndex create(MeilisearchIndexCreateRequest request);

	/**
	 * Return the bound index metadata.
	 *
	 * @return index metadata
	 */
	MeilisearchIndex get();

	/**
	 * Return a page of indexes using Meilisearch defaults.
	 *
	 * @return indexes page
	 */
	MeilisearchIndexList list();

	/**
	 * Return a page of indexes using the given query.
	 *
	 * @param query index listing options
	 * @return indexes page
	 */
	MeilisearchIndexList list(MeilisearchIndexQuery query);

	/**
	 * Update the bound index using the given request.
	 *
	 * @param request index update options
	 * @return the updated index
	 */
	MeilisearchIndex update(MeilisearchIndexUpdateRequest request);

	/**
	 * Delete the bound index.
	 *
	 * @return {@literal true} if the delete task succeeded
	 */
	boolean delete();

	/**
	 * Return statistics for the bound index.
	 *
	 * @return index statistics
	 */
	MeilisearchIndexStats stats();

	/**
	 * Return runtime settings for the bound index.
	 *
	 * @return index settings
	 */
	MeilisearchIndexSettings getSettings();

	/**
	 * Update runtime settings for the bound index.
	 *
	 * @param settings index settings to update
	 * @return index settings after the update task succeeds
	 */
	MeilisearchIndexSettings updateSettings(MeilisearchIndexSettings settings);

	/**
	 * Reset all runtime settings for the bound index to Meilisearch defaults.
	 *
	 * @return index settings after the reset task succeeds
	 */
	MeilisearchIndexSettings resetSettings();
}
