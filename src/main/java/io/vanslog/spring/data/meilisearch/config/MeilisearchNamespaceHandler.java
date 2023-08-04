package io.vanslog.spring.data.meilisearch.config;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * NamespaceHandler class that registers namespaces
 * in the XML configuration file.
 *
 * @author Junghoon Ban
 */
public class MeilisearchNamespaceHandler extends NamespaceHandlerSupport {

    /**
     * Registers the MeilisearchClientBeanDefinitionParser.
     */
    @Override
    public void init() {
        registerBeanDefinitionParser("meilisearch-client",
                new MeilisearchClientBeanDefinitionParser());
    }
}
