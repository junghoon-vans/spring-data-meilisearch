package io.vanslog.spring.data.meilisearch.config;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import com.meilisearch.sdk.json.GsonJsonHandler;
import com.meilisearch.sdk.json.JacksonJsonHandler;
import com.meilisearch.sdk.json.JsonHandler;
import io.vanslog.spring.data.meilisearch.client.ClientConfiguration;
import io.vanslog.spring.data.meilisearch.core.MeilisearchOperations;
import io.vanslog.spring.data.meilisearch.core.MeilisearchTemplate;
import io.vanslog.spring.data.meilisearch.entities.Movie;
import io.vanslog.spring.data.meilisearch.repository.MeilisearchRepository;
import io.vanslog.spring.data.meilisearch.repository.config.EnableMeilisearchRepositories;
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

    @Autowired
    private MeilisearchOperations meilisearchTemplate;

    @Autowired
    private MovieRepository movieRepository;

    @Test
    void shouldCreateMeilisearchClient() {
        assertThat(client).isNotNull();
    }

    @Test
    void shouldCreateMeilisearchTemplate() {
        assertThat(meilisearchTemplate).isNotNull();
    }

    @Test
    void shouldCreateMeilisearchRepository() {
        assertThat(movieRepository).isNotNull();
    }

    @Configuration
    @EnableMeilisearchRepositories(basePackages = {"io.vanslog.spring.data.meilisearch.config"},
            considerNestedRepositories = true)
    static class CustomConfiguration extends MeilisearchConfiguration {
        @Override
        public ClientConfiguration clientConfiguration() {
            return ClientConfiguration.builder()
                    .connectedToLocalhost()
                    .withApiKey("masterKey")
                    .build();
        }

        @Override
        public JsonHandler jsonHandler() {
            return new JacksonJsonHandler();
        }
    }

    interface MovieRepository extends MeilisearchRepository<Movie, String> {}
}