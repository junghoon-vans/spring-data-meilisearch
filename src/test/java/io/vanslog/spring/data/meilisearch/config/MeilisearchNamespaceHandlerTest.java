package io.vanslog.spring.data.meilisearch.config;

import com.meilisearch.sdk.Client;
import io.vanslog.spring.data.meilisearch.client.MeilisearchClientFactoryBean;
import io.vanslog.spring.data.meilisearch.core.MeilisearchTemplate;
import io.vanslog.spring.data.meilisearch.entities.Movie;
import io.vanslog.spring.data.meilisearch.repository.MeilisearchRepository;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration("namespace.xml")
class MeilisearchNamespaceHandlerTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void shouldCreateMeilisearchClient() {
        assertThat(context.getBean(MeilisearchClientFactoryBean.class))
                .isInstanceOf(MeilisearchClientFactoryBean.class);
    }

    @Test
    void shouldCreateMeilisearchTemplate() {
        assertThat(context.getBean(MeilisearchTemplate.class))
                .isInstanceOf(MeilisearchTemplate.class);
    }

    @Test
    void shouldUseGsonJsonHandler()
            throws NoSuchFieldException, IllegalAccessException {
        Client client = (Client) context.getBean("meilisearchClient");

        Field jsonHandlerField =
                client.getClass().getDeclaredField("jsonHandler");
        jsonHandlerField.setAccessible(true);
        assertThat(jsonHandlerField.get(client))
                .isInstanceOf(
                        com.meilisearch.sdk.json.GsonJsonHandler.class);
    }

    @Test
    void shouldCreateMeilisearchRepository() {
        assertThat(context.getBean(MovieRepository.class))
                .isInstanceOf(MovieRepository.class);
    }

    interface MovieRepository extends MeilisearchRepository<Movie, String> {}
}
