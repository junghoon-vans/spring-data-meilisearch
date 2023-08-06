package io.vanslog.spring.data.meilisearch.client;

/**
 * Interface for Meilisearch {@link com.meilisearch.sdk.Config}.
 *
 * @author Junghoon Ban
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
