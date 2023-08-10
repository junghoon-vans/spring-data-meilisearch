package io.vanslog.spring.data.meilisearch.client;

import com.meilisearch.sdk.Config;
import com.meilisearch.sdk.json.JsonHandler;

/**
 * Interface for Meilisearch Configuration.
 *
 * @author Junghoon Ban
 */
public interface ClientConfiguration {

    /**
     * Create a new {@link ClientConfigurationBuilder}.
     *
     * @return ClientConfigurationBuilder
     */
    static ClientConfigurationBuilder builder() {
        return new ClientConfigurationBuilder();
    }

    /**
     * Get the hostUrl.
     *
     * @return hostUrl
     */
    String getHostUrl();

    /**
     * Get the apiKey.
     *
     * @return apiKey
     */
    String getApiKey();

    /**
     * Get the jsonHandler.
     *
     * @return jsonHandler
     */
    JsonHandler getJsonHandler();

    /**
     * Get the clientAgents.
     *
     * @return clientAgents
     */
    String[] getClientAgents();

    /**
     * Get the config.
     *
     * @return config
     */
    Config getConfig();
}
