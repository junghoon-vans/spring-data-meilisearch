package io.vanslog.spring.data.meilisearch.client;

import com.meilisearch.sdk.Config;
import com.meilisearch.sdk.json.JsonHandler;
import org.springframework.lang.Nullable;

/**
 * Default implementation of {@link ClientConfiguration}.
 *
 * @author Junghoon Ban
 */
public class DefaultClientConfiguration implements ClientConfiguration {

    private final String hostUrl;
    private final String apiKey;
    @Nullable private JsonHandler jsonHandler;
    private final String[] clientAgents;

    /**
     * Create a new {@link DefaultClientConfiguration}.
     *
     * @param hostUrl      the host url
     * @param apiKey       the api key
     * @param clientAgents the client agents
     */
    public DefaultClientConfiguration(String hostUrl, String apiKey,
                                      String[] clientAgents) {
        this.hostUrl = hostUrl;
        this.apiKey = apiKey;
        this.jsonHandler = null;
        this.clientAgents = clientAgents;
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
    @Nullable
    public JsonHandler getJsonHandler() {
        return jsonHandler;
    }

    @Override
    public String[] getClientAgents() {
        return clientAgents;
    }

    @Override
    public Config getConfig() {
        if (jsonHandler == null) {
            return new Config(hostUrl, apiKey, clientAgents);
        }
        return new Config(hostUrl, apiKey, jsonHandler, clientAgents);
    }

    @Override
    public ClientConfiguration withJsonHandler(JsonHandler jsonHandler) {
        this.jsonHandler = jsonHandler;
        return this;
    }
}
