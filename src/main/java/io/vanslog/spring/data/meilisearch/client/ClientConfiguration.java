package io.vanslog.spring.data.meilisearch.client;

/**
 * Interface for Meilisearch {@link com.meilisearch.sdk.Config}.
 */
public interface ClientConfiguration {

    /**
     * Create a new {@link ClientConfigurationBuilder}.
     * @return ClientConfigurationBuilder
     */
    static ClientConfigurationBuilder builder() {
        return new ClientConfigurationBuilder();
    }
}
