/*
 * Copyright 2026 the original author or authors.
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
package io.vanslog.spring.data.meilisearch.repository.config;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.core.RepositoryMetadata;
import io.vanslog.spring.data.meilisearch.annotations.Document;
import io.vanslog.spring.data.meilisearch.repository.ReactiveMeilisearchRepository;
import io.vanslog.spring.data.meilisearch.repository.support.ReactiveMeilisearchRepositoryFactoryBean;

/**
 * Selects reactive repository metadata and its distinct operations bean reference.
 * 
 * @author Junghoon Ban
 */
public class ReactiveMeilisearchRepositoryConfigExtension extends RepositoryConfigurationExtensionSupport {

	@Override
	public String getRepositoryFactoryBeanClassName() {
		return ReactiveMeilisearchRepositoryFactoryBean.class.getName();
	}

	@Override
	protected String getModulePrefix() {
		return "reactive-meilisearch";
	}

	@Override
	public String getModuleName() {
		return "Reactive Meilisearch";
	}

	@Override
	public void postProcess(BeanDefinitionBuilder builder, AnnotationRepositoryConfigurationSource config) {
		AnnotationAttributes attributes = config.getAttributes();
		builder.addPropertyReference("reactiveMeilisearchOperations",
				attributes.getString("reactiveMeilisearchTemplateRef"));
	}

	@Override
	protected Collection<Class<? extends Annotation>> getIdentifyingAnnotations() {
		return List.of(Document.class);
	}

	@Override
	protected Collection<Class<?>> getIdentifyingTypes() {
		return List.of(ReactiveMeilisearchRepository.class);
	}

	@Override
	protected boolean useRepositoryConfiguration(RepositoryMetadata metadata) {
		return metadata.isReactiveRepository();
	}

}
