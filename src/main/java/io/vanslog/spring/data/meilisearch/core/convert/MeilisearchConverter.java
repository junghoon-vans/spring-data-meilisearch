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
package io.vanslog.spring.data.meilisearch.core.convert;

import io.vanslog.spring.data.meilisearch.core.document.Document;
import io.vanslog.spring.data.meilisearch.core.mapping.MeilisearchPersistentEntity;
import io.vanslog.spring.data.meilisearch.core.mapping.MeilisearchPersistentProperty;

import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.util.Assert;

/**
 * Meilisearch converter aware of {@link MappingContext}.
 *
 * @author Junghoon Ban
 * @see MappingContext
 */
public interface MeilisearchConverter {

	/**
	 * Returns the {@link org.springframework.data.mapping.context.MappingContext}. used by the converter.
	 *
	 * @return never {@literal null}.
	 */
	MappingContext<? extends MeilisearchPersistentEntity<?>, MeilisearchPersistentProperty> getMappingContext();

	/**
	 * Returns the {@link org.springframework.core.convert.ConversionService} used by the converter.
	 *
	 * @return never {@literal null}.
	 */
	ConversionService getConversionService();

	/**
	 * Reads the given source into the given type.
	 *
	 * @param type must not be {@literal null}.
	 * @param source must not be {@literal null}.
	 * @return the converted object, will never be {@literal null}.
	 */
	<R> R read(Class<R> type, Document source);

	/**
	 * Writes the given object to the given sink.
	 *
	 * @param source must not be {@literal null}.
	 * @param sink must not be {@literal null}.
	 */
	void write(Object source, Document sink);

	default String convertId(Object idValue) {

		Assert.notNull(idValue, "idValue must not be null!");

		if (!getConversionService().canConvert(idValue.getClass(), String.class)) {
			return idValue.toString();
		}

		String converted = getConversionService().convert(idValue, String.class);

		if (converted == null) {
			return idValue.toString();
		}

		return converted;
	}
}
