package io.vanslog.spring.data.meilisearch.client;

import com.meilisearch.sdk.Config;

/**
 * Interface for Meilisearch {@link Config}.
 */
public interface ClientConfiguration {

  static ClientConfigurationBuilder builder() {
    return new ClientConfigurationBuilder();
  }
}
