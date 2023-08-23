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

package io.vanslog.spring.data.meilisearch.core.mapping;

import java.lang.reflect.Field;
import java.util.List;

import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.util.Assert;

/**
 * Meilisearch specific {@link PersistentEntity} implementation holding.
 *
 * @author Junghoon Ban
 */
public class SimpleMeilisearchPersistentProperty
		extends AnnotationBasedPersistentProperty<MeilisearchPersistentProperty> implements MeilisearchPersistentProperty {

	private static final List<String> SUPPORTED_ID_PROPERTY_NAMES = List.of("id");

	/**
	 * Creates a new {@link SimpleMeilisearchPersistentProperty}.
	 *
	 * @param property The property to be persisted.
	 * @param owner The entity that owns this property.
	 * @param simpleTypeHolder The holder for simple types.
	 */
	public SimpleMeilisearchPersistentProperty(Property property,
			PersistentEntity<?, MeilisearchPersistentProperty> owner, SimpleTypeHolder simpleTypeHolder) {
		super(property, owner, simpleTypeHolder);
	}

	@Override
	protected Association<MeilisearchPersistentProperty> createAssociation() {
		throw new UnsupportedOperationException("No association supported on MeilisearchPersistentProperty.");
	}

	@Override
	public String getFieldName() {
		Field field = super.getField();
		Assert.notNull(field, String.format("Invalid field name for property %s.", field));
		return field.getName();
	}

	@Override
	public boolean isIdProperty() {
		return super.isIdProperty() || SUPPORTED_ID_PROPERTY_NAMES.contains(getFieldName());
	}
}
