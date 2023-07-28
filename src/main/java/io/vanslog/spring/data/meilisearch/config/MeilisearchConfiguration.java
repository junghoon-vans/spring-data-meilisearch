package io.vanslog.spring.data.meilisearch.config;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Base class for a @{@link org.springframework.context.annotation.Configuration} class to set up the Meilisearch
 * connection using the Meilisearch Client.
 */
@Configuration(proxyBeanMethods = false)
public abstract class MeilisearchConfiguration {

  @Bean(name = "meilisearchClientConfiguration")
  public abstract Config clientConfiguration();

  @Bean(name = "meilisearchClient")
  public Client meilisearchClient(Config clientConfiguration) {
    return new Client(clientConfiguration);
  }
}
