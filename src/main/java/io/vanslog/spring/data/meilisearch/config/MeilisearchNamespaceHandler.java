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
