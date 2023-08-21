package io.vanslog.spring.data.meilisearch.config;

import io.vanslog.spring.data.meilisearch.repository.config.MeilisearchRepositoryConfigExtension;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.data.repository.config.RepositoryBeanDefinitionParser;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

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
        RepositoryConfigurationExtension extension =
                new MeilisearchRepositoryConfigExtension();
        RepositoryBeanDefinitionParser repositoryBeanDefinitionParser =
                new RepositoryBeanDefinitionParser(extension);

        registerBeanDefinitionParser("repositories",
                repositoryBeanDefinitionParser);
        registerBeanDefinitionParser("meilisearch-client",
                new MeilisearchClientBeanDefinitionParser());
    }
}
