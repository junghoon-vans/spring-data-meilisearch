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

package io.vanslog.spring.data.meilisearch.repository.support;

import io.vanslog.spring.data.meilisearch.core.mapping.MeilisearchPersistentEntity;

import org.springframework.data.repository.core.support.PersistentEntityInformation;

/**
 * Implementation of {@link io.vanslog.spring.data.meilisearch.repository.support.MeilisearchEntityInformation}.
 *
 * @param <T> the type of the entity
 * @param <ID> the type of the entity's identifier
 * @author Junghoon Ban
 */
public class MappingMeilisearchEntityInformation<T, ID> extends PersistentEntityInformation<T, ID>
		implements MeilisearchEntityInformation<T, ID> {

	private final MeilisearchPersistentEntity<T> persistentEntity;

	/**
	 * Creates a new {@link MappingMeilisearchEntityInformation} for the given {@link MeilisearchPersistentEntity}.
	 *
	 * @param persistentEntity must not be {@literal null}.
	 */
	public MappingMeilisearchEntityInformation(MeilisearchPersistentEntity<T> persistentEntity) {
		super(persistentEntity);
		this.persistentEntity = persistentEntity;
	}

	@Override
	public String getIdAttribute() {
		return persistentEntity.getRequiredIdProperty().getFieldName();
	}

	@Override
	public String getIndexUid() {
		return persistentEntity.getIndexUid();
	}
}
