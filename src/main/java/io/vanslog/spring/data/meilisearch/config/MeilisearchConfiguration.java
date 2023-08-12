package io.vanslog.spring.data.meilisearch.config;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.json.GsonJsonHandler;
import com.meilisearch.sdk.json.JsonHandler;
import io.vanslog.spring.data.meilisearch.client.ClientConfiguration;
import io.vanslog.spring.data.meilisearch.core.MeilisearchOperations;
import io.vanslog.spring.data.meilisearch.core.MeilisearchTemplate;
import io.vanslog.spring.data.meilisearch.core.convert.MeilisearchConverter;
import org.springframework.context.annotation.Bean;

/**
 * Base class for a @{@link org.springframework.context.annotation.Configuration}
 * class to set up the Meilisearch connection using the Meilisearch Client.
 *
 * @author Junghoon Ban
 */
public abstract class MeilisearchConfiguration extends MeilisearchConfigurationSupport {

    /**
     * Create a Meilisearch client configuration.
     *
     * @return {@link ClientConfiguration}
     */
    @Bean(name = "meilisearchClientConfiguration")
    public abstract ClientConfiguration clientConfiguration();

    /**
     * Create a Meilisearch client.
     *
     * @param clientConfiguration the client configuration
     * @param jsonHandler the json handler
     * @return {@link com.meilisearch.sdk.Client}
     */
    @Bean(name = "meilisearchClient")
    public Client meilisearchClient(ClientConfiguration clientConfiguration,
                                    JsonHandler jsonHandler) {
        return new Client(clientConfiguration
                .withJsonHandler(jsonHandler).getConfig());
    }

    /**
     * Create a {@link MeilisearchOperations} bean.
     *
     * @param client               the Meilisearch client
     * @param meilisearchConverter the Meilisearch converter
     * @return the created {@link MeilisearchOperations} bean.
     */
    @Bean(name = {"meilisearchOperations", "meilisearchTemplate"})
    public MeilisearchOperations meilisearchOperations(Client client,
                                                       MeilisearchConverter meilisearchConverter,
                                                       ClientConfiguration clientConfiguration) {
        return new MeilisearchTemplate(client, meilisearchConverter,
                clientConfiguration.getJsonHandler());
    }

    /**
     * Register a {@link JsonHandler} bean.
     *
     * @return {@link com.meilisearch.sdk.json.JsonHandler}
     */
    @Bean
    public JsonHandler jsonHandler() {
        return new GsonJsonHandler();
    }
}
