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

package io.vanslog.spring.data.meilisearch.config;

import io.vanslog.spring.data.meilisearch.client.MeilisearchClientFactoryBean;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.Assert;
import org.w3c.dom.Element;

/**
 * BeanDefinitionParser class that parses the client tag in the XML configuration file.The client tag is parsed by
 * setting the host URL, API key, JSON handler, and client agents.
 *
 * @author Junghoon Ban
 */

public class MeilisearchClientBeanDefinitionParser extends AbstractBeanDefinitionParser {

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(MeilisearchClientFactoryBean.class);
		setLocalSettings(element, builder);
		return getSourcedBeanDefinition(builder, element, parserContext);
	}

	private void setLocalSettings(Element element, BeanDefinitionBuilder builder) {

		Assert.hasText(element.getAttribute("api-key"), "The attribute 'api-key' is required.");

		builder.addPropertyValue("hostUrl", element.getAttribute("host-url"));
		builder.addPropertyValue("apiKey", element.getAttribute("api-key"));
		builder.addPropertyValue("clientAgents", element.getAttribute("client-agents"));
		builder.addPropertyReference("jsonHandler", element.getAttribute("json-handler-ref"));

		if (element.hasAttribute("request-timeout")) {
			builder.addPropertyValue("requestTimeout", element.getAttribute("request-timeout"));
		}
		if (element.hasAttribute("request-interval")) {
			builder.addPropertyValue("requestInterval", element.getAttribute("request-interval"));
		}
	}

	private AbstractBeanDefinition getSourcedBeanDefinition(BeanDefinitionBuilder builder, Element source,
			ParserContext context) {
		AbstractBeanDefinition definition = builder.getBeanDefinition();
		definition.setSource(context.extractSource(source));
		return definition;
	}
}
