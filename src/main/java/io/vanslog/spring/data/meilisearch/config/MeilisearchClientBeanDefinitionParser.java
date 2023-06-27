package io.vanslog.spring.data.meilisearch.config;

import io.vanslog.spring.data.meilisearch.client.MeilisearchClientFactoryBean;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * BeanDefinitionParser class that parses the client tag in the XML configuration file.
 * The client tag is parsed by setting the host URL, API key, JSON handler, and client agents.
 *
 * @since 1.0.0
 * @author Junghoon Ban
 */

public class MeilisearchClientBeanDefinitionParser extends AbstractBeanDefinitionParser {

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(
        MeilisearchClientFactoryBean.class);
		setLocalSettings(element, builder);
		return getSourcedBeanDefinition(builder, element, parserContext);
	}

	private void setLocalSettings(Element element, BeanDefinitionBuilder builder) {
		builder.addPropertyValue("hostUrl", element.getAttribute("host-url"));
		builder.addPropertyValue("apiKey", element.getAttribute("api-key"));
		builder.addPropertyValue("clientAgents", element.getAttribute("client-agents"));

		String jsonHandlerName = element.getAttribute("json-handler");
		JsonHandlerBuilder handlerBuilder = JsonHandlerBuilder.valueOf(jsonHandlerName.toUpperCase());

		if (handlerBuilder != null) {
			builder.addPropertyValue("jsonHandler", handlerBuilder.build());
		}
	}

	private AbstractBeanDefinition getSourcedBeanDefinition(BeanDefinitionBuilder builder, Element source,
															ParserContext context) {
		AbstractBeanDefinition definition = builder.getBeanDefinition();
		definition.setSource(context.extractSource(source));
		return definition;
	}
}
