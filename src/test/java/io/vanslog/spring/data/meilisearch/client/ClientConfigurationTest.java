package io.vanslog.spring.data.meilisearch.client;

import com.meilisearch.sdk.json.GsonJsonHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Tests for {@link ClientConfiguration}.
 */
class ClientConfigurationTest {

    ClientConfiguration clientConfiguration;

    @BeforeEach
    void setup() {
        clientConfiguration = ClientConfiguration.builder()
                .connectedToLocalhost()
                .withApiKey("masterKey")
                .build();
    }

    @Test
    void shouldBuildConfiguration() {
        assertThat(clientConfiguration.getHostUrl()).isEqualTo(
                "http://localhost:7700");
        assertThat(clientConfiguration.getApiKey()).isEqualTo("masterKey");
        assertThat(clientConfiguration.getClientAgents()).isEmpty();
    }

    @Test
    void shouldConfigureJsonHandler() {
        clientConfiguration.withJsonHandler(new GsonJsonHandler());
        assertThat(clientConfiguration.getJsonHandler()).isInstanceOf(
                GsonJsonHandler.class);
    }
}
