package io.vanslog.spring.data.meilisearch.client;

import com.meilisearch.sdk.Config;
import com.meilisearch.sdk.json.JsonHandler;
import org.springframework.lang.Nullable;

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
    @Nullable
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

    /**
     * Set the JSON handler.
     * @param jsonHandler the JSON handler
     * @return {@link ClientConfiguration}
     */
    ClientConfiguration withJsonHandler(JsonHandler jsonHandler);
}
