package io.vanslog.spring.data.meilisearch.config;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import io.vanslog.spring.data.meilisearch.client.ClientConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
class MeilisearchConfigurationTest {

    @Autowired
    private Client client;

    @Test
    void shouldCreateClient() throws MeilisearchException {
        assertThat(client).isNotNull();
    }

    @Configuration
    static class CustomConfiguration extends MeilisearchConfiguration {
        @Override
        public ClientConfiguration clientConfiguration() {
            return ClientConfiguration.builder()
                    .connectedToLocalhost()
                    .withApiKey("masterKey")
                    .withGsonJsonHandler()
                    .build();
        }
    }
}