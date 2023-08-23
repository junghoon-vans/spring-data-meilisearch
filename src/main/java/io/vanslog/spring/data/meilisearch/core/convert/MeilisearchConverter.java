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

package io.vanslog.spring.data.meilisearch.core.convert;

import io.vanslog.spring.data.meilisearch.core.mapping.MeilisearchPersistentEntity;
import io.vanslog.spring.data.meilisearch.core.mapping.MeilisearchPersistentProperty;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.context.MappingContext;

/**
 * Meilisearch converter aware of {@link MappingContext}.
 *
 * @author Junghoon Ban
 * @see MappingContext
 */
public interface MeilisearchConverter {

    /**
     * Returns the {@link MappingContext} used by the converter.
     *
     * @return never {@literal null}.
     */
    MappingContext<? extends MeilisearchPersistentEntity<?>,
            MeilisearchPersistentProperty> getMappingContext();

    /**
     * Returns the {@link ConversionService} used by the converter.
     *
     * @return never {@literal null}.
     */
    ConversionService getConversionService();
}
