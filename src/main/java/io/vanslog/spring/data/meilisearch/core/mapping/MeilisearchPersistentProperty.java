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

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mapping.PersistentProperty;

/**
 * Meilisearch specific {@link org.springframework.data.mapping.PersistentProperty} abstraction.
 *
 * @author Junghoon Ban
 */
public interface MeilisearchPersistentProperty extends PersistentProperty<MeilisearchPersistentProperty> {

	/**
	 * Returns the name of the field a property is persisted to.
	 * 
	 * @return the field name
	 */
	String getFieldName();

	enum PropertyToFieldNameConverter implements Converter<MeilisearchPersistentProperty, String> {

		INSTANCE;

		/**
		 * Convert the persistent property into a Meilisearch field name.
		 * 
		 * @param source the persistent property
		 * @return the field name
		 */
		public String convert(MeilisearchPersistentProperty source) {
			return source.getFieldName();
		}
	}
}
