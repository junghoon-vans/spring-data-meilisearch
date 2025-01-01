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

import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;

/**
 * Meilisearch specific {@link org.springframework.data.mapping.context.AbstractMappingContext} implementation.
 *
 * @author Junghoon Ban
 */
public class SimpleMeilisearchMappingContext
		extends AbstractMappingContext<SimpleMeilisearchPersistentEntity<?>, MeilisearchPersistentProperty> {

	@Override
	protected <T> SimpleMeilisearchPersistentEntity<?> createPersistentEntity(TypeInformation<T> typeInformation) {
		return new SimpleMeilisearchPersistentEntity<>(typeInformation);
	}

	@Override
	protected MeilisearchPersistentProperty createPersistentProperty(Property property,
			SimpleMeilisearchPersistentEntity<?> owner, SimpleTypeHolder simpleTypeHolder) {
		return new SimpleMeilisearchPersistentProperty(property, owner, simpleTypeHolder);
	}
}
