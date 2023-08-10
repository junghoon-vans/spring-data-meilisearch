package io.vanslog.spring.data.meilisearch.config;

import com.meilisearch.sdk.Client;
import io.vanslog.spring.data.meilisearch.client.ClientConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Base class for a @{@link org.springframework.context.annotation.Configuration}
 * class to set up the Meilisearch connection using the Meilisearch Client.
 *
 * @author Junghoon Ban
 */
@Configuration(proxyBeanMethods = false)
public abstract class MeilisearchConfiguration {

    /**
     * Create a Meilisearch client configuration.
     * @return {@link com.meilisearch.sdk.Config}
     */
    @Bean(name = "meilisearchClientConfiguration")
    public abstract ClientConfiguration clientConfiguration();

    /**
     * Create a Meilisearch client.
     * @param clientConfiguration the client configuration
     * @return {@link com.meilisearch.sdk.Client}
     */
    @Bean(name = "meilisearchClient")
    public Client meilisearchClient(ClientConfiguration clientConfiguration) {
        return new Client(clientConfiguration.getConfig());
    }
}
