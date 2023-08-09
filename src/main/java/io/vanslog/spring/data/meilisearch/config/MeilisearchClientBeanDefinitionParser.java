package io.vanslog.spring.data.meilisearch.config;

import io.vanslog.spring.data.meilisearch.client.MeilisearchClientFactoryBean;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.Assert;
import org.w3c.dom.Element;

import java.util.Arrays;

/**
 * BeanDefinitionParser class that parses the client tag
 * in the XML configuration file.The client tag is parsed
 * by setting the host URL, API key, JSON handler, and client agents.
 *
 * @author Junghoon Ban
 */

public class MeilisearchClientBeanDefinitionParser
        extends AbstractBeanDefinitionParser {

    @Override
    protected AbstractBeanDefinition parseInternal(Element element,
                                                   ParserContext parserContext) {
        BeanDefinitionBuilder builder =
                BeanDefinitionBuilder.rootBeanDefinition(
                        MeilisearchClientFactoryBean.class);
        setLocalSettings(element, builder);
        return getSourcedBeanDefinition(builder, element, parserContext);
    }

    private void setLocalSettings(Element element,
                                  BeanDefinitionBuilder builder) {

        Assert.hasText(element.getAttribute("api-key"),
                "The attribute 'api-key' is required.");

        builder.addPropertyValue("hostUrl", element.getAttribute("host-url"));
        builder.addPropertyValue("apiKey", element.getAttribute("api-key"));
        builder.addPropertyValue("clientAgents",
                element.getAttribute("client-agents"));

        String jsonHandlerName = element.getAttribute("json-handler");
        Assert.isTrue(JsonHandlerBuilder.contains(jsonHandlerName),
                "JsonHandler must be one of "
                        + Arrays.toString(JsonHandlerBuilder.values()));

        JsonHandlerBuilder handlerBuilder =
                JsonHandlerBuilder.valueOf(jsonHandlerName.toUpperCase());
        builder.addPropertyValue("jsonHandler", handlerBuilder.build());
    }

    private AbstractBeanDefinition getSourcedBeanDefinition(
            BeanDefinitionBuilder builder, Element source,
            ParserContext context) {
        AbstractBeanDefinition definition = builder.getBeanDefinition();
        definition.setSource(context.extractSource(source));
        return definition;
    }
}
