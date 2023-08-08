package io.vanslog.spring.data.meilisearch.client;

import com.meilisearch.sdk.Config;
import com.meilisearch.sdk.json.GsonJsonHandler;
import com.meilisearch.sdk.json.JacksonJsonHandler;
import com.meilisearch.sdk.json.JsonHandler;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A builder for Meilisearch {@link Config}.
 *
 * @author Junghoon Ban
 */
public class ClientConfigurationBuilder {

    @Nullable private String hostUrl;
    @Nullable private String apiKey;
    private JsonHandler jsonHandler;
    private String[] clientAgents;

    /**
     * Create a new {@link ClientConfigurationBuilder}.
     */
    public ClientConfigurationBuilder() {
        this.jsonHandler = new JacksonJsonHandler();
        this.clientAgents = new String[0];
    }

    /**
     * Connect to localhost:7700.
     * @return {@link ClientConfigurationBuilder}
     */
    public ClientConfigurationBuilder connectedToLocalhost() {
        this.hostUrl = "http://localhost:7700";
        return this;
    }

    /**
     * Connect to the given hostUrl.
     * @param hostUrl the hostUrl to connect to
     * @return {@link ClientConfigurationBuilder}
     */
    public ClientConfigurationBuilder connectedTo(String hostUrl) {
        this.hostUrl = hostUrl;
        return this;
    }

    /**
     * Configure API key to access Meilisearch instance.
     * @param apiKey the apiKey to use
     * @return {@link ClientConfigurationBuilder}
     */
    public ClientConfigurationBuilder withApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    /**
     * Configure json handler as {@link JacksonJsonHandler}.
     * @return {@link ClientConfigurationBuilder}
     */
    public ClientConfigurationBuilder withJacksonJsonHandler() {
        this.jsonHandler = new JacksonJsonHandler();
        return this;
    }

    /**
     * Configure json handler as {@link GsonJsonHandler}.
     * @return {@link ClientConfigurationBuilder}
     */
    public ClientConfigurationBuilder withGsonJsonHandler() {
        this.jsonHandler = new GsonJsonHandler();
        return this;
    }

    /**
     * Configure client agents that will be sent with User-Agent header.
     * @param clientAgents String array of client agents
     * @return {@link ClientConfigurationBuilder}
     */
    public ClientConfigurationBuilder withClientAgents(String[] clientAgents) {
        this.clientAgents = clientAgents;
        return this;
    }

    /**
     * Build a {@link Config} with the given parameters.
     * @return {@link Config}
     */
    public Config build() {
        Assert.notNull(this.apiKey, "API Key must not be null");
        return new Config(this.hostUrl, this.apiKey, this.jsonHandler,
                this.clientAgents);
    }
}
