package io.vanslog.spring.data.meilisearch.client;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A builder for Meilisearch {@link ClientConfiguration}.
 *
 * @author Junghoon Ban
 */
public class ClientConfigurationBuilder {

    @Nullable
    private String hostUrl;
    @Nullable
    private String apiKey;
    private String[] clientAgents;

    /**
     * Create a new {@link ClientConfigurationBuilder}.
     */
    public ClientConfigurationBuilder() {
        this.clientAgents = new String[0];
    }

    /**
     * Connect to localhost:7700.
     *
     * @return {@link ClientConfigurationBuilder}
     */
    public ClientConfigurationBuilder connectedToLocalhost() {
        this.hostUrl = "http://localhost:7700";
        return this;
    }

    /**
     * Connect to the given hostUrl.
     *
     * @param hostUrl the hostUrl to connect to
     * @return {@link ClientConfigurationBuilder}
     */
    public ClientConfigurationBuilder connectedTo(String hostUrl) {
        this.hostUrl = hostUrl;
        return this;
    }

    /**
     * Configure API key to access Meilisearch instance.
     *
     * @param apiKey the apiKey to use
     * @return {@link ClientConfigurationBuilder}
     */
    public ClientConfigurationBuilder withApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    /**
     * Configure client agents that will be sent with User-Agent header.
     *
     * @param clientAgents String array of client agents
     * @return {@link ClientConfigurationBuilder}
     */
    public ClientConfigurationBuilder withClientAgents(String[] clientAgents) {
        this.clientAgents = clientAgents;
        return this;
    }

    /**
     * Build a {@link ClientConfiguration} with the given parameters.
     *
     * @return {@link ClientConfiguration}
     */
    public ClientConfiguration build() {
        Assert.notNull(this.hostUrl, "Host URL must not be null");
        Assert.notNull(this.apiKey, "API Key must not be null");
        return new DefaultClientConfiguration(this.hostUrl, this.apiKey,
                this.clientAgents);
    }
}
