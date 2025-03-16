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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.springframework.data.util.TypeInformation;

import io.vanslog.spring.data.meilisearch.annotations.Document;

/**
 * Unit tests for {@link SimpleMeilisearchPersistentEntity}.
 *
 * @author Junghoon Ban
 */
class SimpleMeilisearchPersistentEntityUnitTests {

	@Test
	void shouldThrowExceptionWhenIndexUidIsBlank() {
		TypeInformation<EmptyIndexUidDocument> entityTypeInfo = TypeInformation.of(EmptyIndexUidDocument.class);
		assertThatThrownBy(() -> new SimpleMeilisearchPersistentEntity<>(entityTypeInfo))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldUseClassNameAsIndexUidWhenNotSpecified() {
		// given
		SimpleMeilisearchPersistentEntity<EntityWithoutExplicitIndexUid> entity = //
				new SimpleMeilisearchPersistentEntity<>(TypeInformation.of(EntityWithoutExplicitIndexUid.class));

		// when
		String indexUid = entity.getIndexUid();

		// then
		assertThat(indexUid).isEqualTo("EntityWithoutExplicitIndexUid");
	}

	@Test
	void shouldUseSpecifiedIndexUid() {
		// given
		SimpleMeilisearchPersistentEntity<EntityWithExplicitIndexUid> entity = new SimpleMeilisearchPersistentEntity<>(
				TypeInformation.of(EntityWithExplicitIndexUid.class));

		// when
		String indexUid = entity.getIndexUid();

		// then
		assertThat(indexUid).isEqualTo("custom-index");
	}

	@Document(indexUid = "")
	static class EmptyIndexUidDocument {}

	static class EntityWithoutExplicitIndexUid {
		private String field;

		public String getField() {
			return field;
		}

		public void setField(String field) {
			this.field = field;
		}
	}

	@Document(indexUid = "custom-index")
	static class EntityWithExplicitIndexUid {
		private String field;

		public String getField() {
			return field;
		}

		public void setField(String field) {
			this.field = field;
		}
	}
}
