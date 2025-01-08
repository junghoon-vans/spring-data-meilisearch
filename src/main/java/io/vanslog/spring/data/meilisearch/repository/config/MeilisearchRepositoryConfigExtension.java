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
package io.vanslog.spring.data.meilisearch.repository.config;

import io.vanslog.spring.data.meilisearch.annotations.Document;
import io.vanslog.spring.data.meilisearch.repository.MeilisearchRepository;
import io.vanslog.spring.data.meilisearch.repository.support.MeilisearchRepositoryFactoryBean;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;

import java.util.List;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.config.XmlRepositoryConfigurationSource;
import org.w3c.dom.Element;

/**
 * {@link org.springframework.data.repository.config.RepositoryConfigurationExtension} implementation to configure
 * Meilisearch repository configuration support.
 *
 * @author Junghoon Ban
 */
public class MeilisearchRepositoryConfigExtension extends RepositoryConfigurationExtensionSupport {

	private static final String MODULE_NAME = "Meilisearch";

	@Override
	public String getRepositoryFactoryBeanClassName() {
		return MeilisearchRepositoryFactoryBean.class.getName();
	}

	@Override
	protected String getModulePrefix() {
		return getModuleIdentifier();
	}

	@Override
	public String getModuleName() {
		return MODULE_NAME;
	}

	@Override
	public void postProcess(BeanDefinitionBuilder builder, AnnotationRepositoryConfigurationSource config) {
		AnnotationAttributes attributes = config.getAttributes();
		builder.addPropertyReference("meilisearchOperations", attributes.getString("meilisearchTemplateRef"));
	}

	@Override
	public void postProcess(BeanDefinitionBuilder builder, XmlRepositoryConfigurationSource config) {
		Element element = config.getElement();
		builder.addPropertyReference("meilisearchOperations", element.getAttribute("meilisearch-template-ref"));
	}

	@Override
	protected Collection<Class<? extends Annotation>> getIdentifyingAnnotations() {
		return Collections.singleton(Document.class);
	}

	@Override
	protected Collection<Class<?>> getIdentifyingTypes() {
		return List.of(MeilisearchRepository.class);
	}
}
