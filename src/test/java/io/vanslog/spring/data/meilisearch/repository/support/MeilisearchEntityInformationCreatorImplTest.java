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

import io.vanslog.spring.data.meilisearch.annotations.Document;
import io.vanslog.spring.data.meilisearch.core.mapping.SimpleMeilisearchMappingContext;
import io.vanslog.spring.data.meilisearch.entities.Movie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mapping.MappingException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

/**
 * Unit tests for {@link io.vanslog.spring.data.meilisearch.repository.support.MeilisearchEntityInformationCreatorImpl}.
 */
class MeilisearchEntityInformationCreatorImplTest {

    MeilisearchEntityInformationCreatorImpl entityInformationCreator;

    @BeforeEach
    void setUp() {
        SimpleMeilisearchMappingContext context =
                new SimpleMeilisearchMappingContext();
        entityInformationCreator =
                new MeilisearchEntityInformationCreatorImpl(context);
    }

    @Test
    void shouldCreateEntityInformation() {
        assertThat(entityInformationCreator.getEntityInformation(
                Movie.class)).isNotNull();
    }

    @Test
    void shouldThrowMappingExceptionOnMissingEntity() {
        assertThatThrownBy(() -> entityInformationCreator.getEntityInformation(String.class))
                .isInstanceOf(MappingException.class);
    }

    @Test
    void shouldThrowIllegalArgumentExceptionOnMissingIdAnnotation() {
        assertThatThrownBy(() -> entityInformationCreator.getEntityInformation(EntityNoId.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No id property found");
    }

    @Document(indexUid = "entity-no-id")
    class EntityNoId {

    }
}
