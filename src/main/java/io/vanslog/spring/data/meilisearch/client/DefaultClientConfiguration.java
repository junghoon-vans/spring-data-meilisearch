package io.vanslog.spring.data.meilisearch.client;

import com.meilisearch.sdk.Config;
import com.meilisearch.sdk.json.JsonHandler;

/**
 * Default implementation of {@link ClientConfiguration}.
 *
 * @author Junghoon Ban
 */
public class DefaultClientConfiguration implements ClientConfiguration {

    private final String hostUrl;
    private final String apiKey;
    private final JsonHandler jsonHandler;
    private final String[] clientAgents;
    private final Config config;

    /**
     * Create a new {@link DefaultClientConfiguration}.
     * @param hostUrl the host url
     * @param apiKey the api key
     * @param jsonHandler the json handler
     * @param clientAgents the client agents
     */
    public DefaultClientConfiguration(String hostUrl, String apiKey,
                                      JsonHandler jsonHandler,
                                      String[] clientAgents) {
        this.hostUrl = hostUrl;
        this.apiKey = apiKey;
        this.jsonHandler = jsonHandler;
        this.clientAgents = clientAgents;
        this.config = new Config(hostUrl, apiKey, jsonHandler, clientAgents);
    }

    @Override
    public String getHostUrl() {
        return hostUrl;
    }

    @Override
    public String getApiKey() {
        return apiKey;
    }

    @Override
    public JsonHandler getJsonHandler() {
        return jsonHandler;
    }

    @Override
    public String[] getClientAgents() {
        return clientAgents;
    }

    @Override
    public Config getConfig() {
        return config;
    }
}
