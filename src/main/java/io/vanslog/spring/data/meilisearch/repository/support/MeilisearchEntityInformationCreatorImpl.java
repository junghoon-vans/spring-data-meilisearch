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
import io.vanslog.spring.data.meilisearch.core.mapping.MeilisearchPersistentProperty;

import org.springframework.data.mapping.context.MappingContext;
import org.springframework.util.Assert;

/**
 * Implementation of {@link io.vanslog.spring.data.meilisearch.repository.support.MeilisearchEntityInformationCreator}.
 *
 * @author Junghoon Ban
 */
public class MeilisearchEntityInformationCreatorImpl implements MeilisearchEntityInformationCreator {

	private final MappingContext<? extends MeilisearchPersistentEntity<?>, MeilisearchPersistentProperty> mappingContext;

	/**
	 * Creates a new {@link MeilisearchEntityInformationCreatorImpl} for the given
	 * {@link org.springframework.data.mapping.context.MappingContext}.
	 *
	 * @param mappingContext must not be {@literal null}.
	 */
	public MeilisearchEntityInformationCreatorImpl(
			MappingContext<? extends MeilisearchPersistentEntity<?>, MeilisearchPersistentProperty> mappingContext) {

		Assert.notNull(mappingContext, "MappingContext must not be null!");

		this.mappingContext = mappingContext;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T, ID> MeilisearchEntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {

		MeilisearchPersistentEntity<T> persistentEntity = (MeilisearchPersistentEntity<T>) mappingContext
				.getRequiredPersistentEntity(domainClass);

		Assert.notNull(persistentEntity, String.format("Unable to obtain mapping metadata for %s!", domainClass));
		Assert.notNull(persistentEntity.getIdProperty(), String.format("No id property found for %s!", domainClass));

		return new MappingMeilisearchEntityInformation<>(persistentEntity);
	}
}
