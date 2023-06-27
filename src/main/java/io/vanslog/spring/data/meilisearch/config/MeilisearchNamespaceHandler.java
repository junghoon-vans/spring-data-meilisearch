package io.vanslog.spring.data.meilisearch.config;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * NamespaceHandler class that registers namespaces in the XML configuration file.
 *
 * @since 1.0.0
 * @author Junghoon Ban
 */
public class MeilisearchNamespaceHandler extends NamespaceHandlerSupport {

  @Override
  public void init() {
    registerBeanDefinitionParser("meilisearch-client", new MeilisearchClientBeanDefinitionParser());
  }
}
