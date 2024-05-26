/*
 * Copyright 2023-2024 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import io.vanslog.spring.data.meilisearch.entities.Movie;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SimpleMeilisearchPersistentProperty}.
 *
 * @author Junghoon Ban
 */
class SimpleMeilisearchPersistentPropertyUnitTests {
	private final SimpleMeilisearchMappingContext context = new SimpleMeilisearchMappingContext();

	@Test
	void shouldReturnTrueWhenPropertyIsId() {
		SimpleMeilisearchPersistentEntity<?> entity = context.getRequiredPersistentEntity(Movie.class);
		SimpleMeilisearchPersistentProperty property = (SimpleMeilisearchPersistentProperty) entity
				.getPersistentProperty("id");
		assertThat(property.isIdProperty()).isTrue();
	}

	@Test
	void shouldReturnFieldName() {
		SimpleMeilisearchPersistentEntity<?> entity = context.getRequiredPersistentEntity(Movie.class);

		SimpleMeilisearchPersistentProperty idProperty = (SimpleMeilisearchPersistentProperty) entity
				.getPersistentProperty("id");
		SimpleMeilisearchPersistentProperty titleProperty = (SimpleMeilisearchPersistentProperty) entity
				.getPersistentProperty("title");
		SimpleMeilisearchPersistentProperty descriptionProperty = (SimpleMeilisearchPersistentProperty) entity
				.getPersistentProperty("description");
		SimpleMeilisearchPersistentProperty genresProperty = (SimpleMeilisearchPersistentProperty) entity
				.getPersistentProperty("genres");

		assertThat(idProperty).isNotNull();
		assertThat(titleProperty).isNotNull();
		assertThat(descriptionProperty).isNotNull();
		assertThat(genresProperty).isNotNull();
	}

	@Test
	void shouldReturnTrueWhenPropertyIsIdAndHasIdAnnotation() {
		SimpleMeilisearchPersistentEntity<?> entity = context.getRequiredPersistentEntity(NoIdAnnotation.class);
		SimpleMeilisearchPersistentProperty property = (SimpleMeilisearchPersistentProperty) entity
				.getPersistentProperty("id");
		assertThat(property.isIdProperty()).isTrue();
	}

	@SuppressWarnings("unused")
	static class NoIdAnnotation {
		private String id;
	}
}
