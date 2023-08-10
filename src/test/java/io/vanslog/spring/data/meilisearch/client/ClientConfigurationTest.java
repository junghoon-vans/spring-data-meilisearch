package io.vanslog.spring.data.meilisearch.client;

import com.meilisearch.sdk.Config;
import com.meilisearch.sdk.json.GsonJsonHandler;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Tests for {@link ClientConfiguration}.
 */
class ClientConfigurationTest {

    @Test
    void shouldBuildConfiguration() {
        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .connectedToLocalhost()
                .withApiKey("masterKey")
                .withGsonJsonHandler()
                .build();

        assertThat(clientConfiguration.getHostUrl()).isEqualTo(
                "http://localhost:7700");
        assertThat(clientConfiguration.getApiKey()).isEqualTo("masterKey");
        assertThat(clientConfiguration.getJsonHandler()).isInstanceOf(
                GsonJsonHandler.class);
        assertThat(clientConfiguration.getClientAgents()).isEmpty();
    }
}
