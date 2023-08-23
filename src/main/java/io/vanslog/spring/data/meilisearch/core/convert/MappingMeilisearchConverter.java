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
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link MeilisearchConverter} Implementation based on {@link MappingContext}.
 *
 * @author Junghoon Ban
 */
public class MappingMeilisearchConverter
        implements MeilisearchConverter, ApplicationContextAware {

    private final MappingContext<? extends MeilisearchPersistentEntity<?>,
            MeilisearchPersistentProperty> mappingContext;
    private final GenericConversionService conversionService;

    @SuppressWarnings("unused, FieldCanBeLocal")
    @Nullable private ApplicationContext applicationContext;

    /**
     * Creates a new {@link MappingMeilisearchConverter} given the {@link MappingContext}.
     * @param mappingContext must not be {@literal null}.
     */
    public MappingMeilisearchConverter(
            MappingContext<? extends MeilisearchPersistentEntity<?>,
                    MeilisearchPersistentProperty> mappingContext) {
        Assert.notNull(mappingContext, "MappingContext must not be null!");
        this.mappingContext = mappingContext;
        this.conversionService = new DefaultConversionService();
    }

    @Override
    public MappingContext<? extends MeilisearchPersistentEntity<?>,
            MeilisearchPersistentProperty> getMappingContext() {
        return mappingContext;
    }

    @Override
    public ConversionService getConversionService() {
        return this.conversionService;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        this.applicationContext = applicationContext;
    }
}
