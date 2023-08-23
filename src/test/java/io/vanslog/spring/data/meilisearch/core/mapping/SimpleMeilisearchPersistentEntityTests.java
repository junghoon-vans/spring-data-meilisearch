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

import io.vanslog.spring.data.meilisearch.annotations.Document;
import io.vanslog.spring.data.meilisearch.entities.Movie;
import org.junit.jupiter.api.Test;
import org.springframework.data.util.TypeInformation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SimpleMeilisearchPersistentEntity}.
 *
 * @author Junghoon Ban
 */
class SimpleMeilisearchPersistentEntityTests {

    @Test
    void shouldReturnIndexUid() {
        TypeInformation<Movie> entityTypeInfo = TypeInformation.of(Movie.class);
        SimpleMeilisearchPersistentEntity<Movie> entityPersistentEntity =
                new SimpleMeilisearchPersistentEntity<>(entityTypeInfo);
        String indexUid = entityPersistentEntity.getIndexUid();
        assertThat(indexUid).isEqualTo("movies");
    }

    @Test
    void shouldThrowExceptionWhenIndexUidIsBlank() {
        TypeInformation<EmptyIndexUidDocument> entityTypeInfo =
                TypeInformation.of(
                        EmptyIndexUidDocument.class);
        assertThatThrownBy(
                () -> new SimpleMeilisearchPersistentEntity<>(entityTypeInfo))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Document(indexUid = "")
    static class EmptyIndexUidDocument {
    }
}
