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
package io.vanslog.spring.data.meilisearch.core.mapping;

import com.meilisearch.sdk.model.Settings;
import org.springframework.data.mapping.PersistentEntity;

/**
 * Meilisearch specific {@link org.springframework.data.mapping.PersistentEntity} abstraction.
 *
 * @param <T>
 * @author Junghoon Ban
 */
public interface MeilisearchPersistentEntity<T> extends PersistentEntity<T, MeilisearchPersistentProperty> {

	/**
	 * Returns the Index UID of the persistent entity.
	 * 
	 * @return Index UID
	 */
	String getIndexUid();

	/**
	 * Returns whether to apply the settings to the index on repository bootstrapping.
	 * 
	 * @return applySettings
	 */
	boolean isApplySettings();

	/**
	 * Returns the default settings for an index.
	 *
	 * @return settings
	 */
	Settings getDefaultSettings();
}
