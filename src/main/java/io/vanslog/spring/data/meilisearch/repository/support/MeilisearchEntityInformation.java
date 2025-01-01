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

import org.springframework.data.repository.core.EntityInformation;

/**
 * Entity information for Meilisearch.
 *
 * @param <T> the type of the entity
 * @param <ID> the type of the entity's identifier
 * @author Junghoon Ban
 */
public interface MeilisearchEntityInformation<T, ID> extends EntityInformation<T, ID> {

	/**
	 * Returns the id attribute.
	 *
	 * @return Id attribute
	 */
	String getIdAttribute();

	/**
	 * Returns the uid of the index.
	 *
	 * @return Index UID
	 */
	String getIndexUid();
}
