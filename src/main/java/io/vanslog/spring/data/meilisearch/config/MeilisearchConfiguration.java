package io.vanslog.spring.data.meilisearch.config;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Base class for a @{@link org.springframework.context.annotation.Configuration}
 * class to set up the Meilisearch connection using the Meilisearch Client.
 */
@Configuration(proxyBeanMethods = false)
public abstract class MeilisearchConfiguration {

    /**
     * Create a Meilisearch client configuration.
     * @return {@link com.meilisearch.sdk.Config}
     */
    @Bean(name = "meilisearchClientConfiguration")
    public abstract Config clientConfiguration();

    /**
     * Create a Meilisearch client.
     * @param clientConfiguration the client configuration
     * @return {@link com.meilisearch.sdk.Client}
     */
    @Bean(name = "meilisearchClient")
    public Client meilisearchClient(Config clientConfiguration) {
        return new Client(clientConfiguration);
    }
}
