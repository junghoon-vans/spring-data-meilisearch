package io.vanslog.spring.data.meilisearch.client;

import com.meilisearch.sdk.Config;
import com.meilisearch.sdk.json.GsonJsonHandler;
import com.meilisearch.sdk.json.JacksonJsonHandler;
import com.meilisearch.sdk.json.JsonHandler;

/**
 * A builder for Meilisearch {@link Config}.
 */
public class ClientConfigurationBuilder {

  private String hostUrl;
  private String apiKey;
  private JsonHandler jsonHandler;
  private String[] clientAgents;

  public ClientConfigurationBuilder() {
    this.jsonHandler = new JacksonJsonHandler();
    this.clientAgents = new String[0];
  }

  public ClientConfigurationBuilder connectedToLocalhost() {
    this.hostUrl = "http://localhost:7700";
    return this;
  }

  public ClientConfigurationBuilder connectedTo(String hostUrl) {
    this.hostUrl = hostUrl;
    return this;
  }

  public ClientConfigurationBuilder withApiKey(String apiKey) {
    this.apiKey = apiKey;
    return this;
  }

  public ClientConfigurationBuilder withJacksonJsonHandler() {
    this.jsonHandler = new JacksonJsonHandler();
    return this;
  }

  public ClientConfigurationBuilder withGsonJsonHandler() {
    this.jsonHandler = new GsonJsonHandler();
    return this;
  }

  public ClientConfigurationBuilder withClientAgents(String[] clientAgents) {
    this.clientAgents = clientAgents;
    return this;
  }

  public Config build() {
    return new Config(this.hostUrl, this.apiKey, this.jsonHandler, this.clientAgents);
  }
}
