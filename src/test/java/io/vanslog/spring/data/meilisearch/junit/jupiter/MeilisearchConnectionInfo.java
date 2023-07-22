package io.vanslog.spring.data.meilisearch.junit.jupiter;

import io.vanslog.testcontainers.meilisearch.MeilisearchContainer;

/**
 * This class is used to store the connection information for Meilisearch.
 *
 * @see MeilisearchConnection
 * @author Junghoon Ban
 */
public class MeilisearchConnectionInfo {

  private final String host;
  private final int port;
  private final String masterKey;
  private final MeilisearchContainer meilisearchContainer;

  private MeilisearchConnectionInfo(String host, int port, String masterKey,
      MeilisearchContainer meilisearchContainer) {
    this.host = host;
    this.port = port;
    this.masterKey = masterKey;
    this.meilisearchContainer = meilisearchContainer;
  }

  @Override
  public String toString() {
    return "MeilisearchConnectionInfo{" +
        "host='" + host + '\'' +
        ", port=" + port +
        ", masterKey='" + masterKey + '\'' +
        ", meilisearchContainer=" + meilisearchContainer +
        '}';
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public String getMasterKey() {
    return masterKey;
  }

  public MeilisearchContainer getMeilisearchContainer() {
    return meilisearchContainer;
  }

  public static class Builder {

    private String host;
    private int port;
    private String masterKey;
    private MeilisearchContainer meilisearchContainer;

    public Builder host(String host) {
      this.host = host;
      return this;
    }

    public Builder port(int port) {
      this.port = port;
      return this;
    }

    public Builder masterKey(String masterKey) {
      this.masterKey = masterKey;
      return this;
    }

    public Builder meilisearchContainer(MeilisearchContainer meilisearchContainer) {
      this.meilisearchContainer = meilisearchContainer;
      return this;
    }

    public MeilisearchConnectionInfo build() {
      return new MeilisearchConnectionInfo(host, port, masterKey, meilisearchContainer);
    }
  }
}
