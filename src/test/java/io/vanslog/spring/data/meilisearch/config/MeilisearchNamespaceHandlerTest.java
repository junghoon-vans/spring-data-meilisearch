package io.vanslog.spring.data.meilisearch.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.vanslog.spring.data.meilisearch.client.MeilisearchClientFactoryBean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration("namespace.xml")
class MeilisearchNamespaceHandlerTest {

  @Autowired
  private ApplicationContext context;

  @Test
  public void shouldCreateMeilisearchClient() {
    assertThat(context.getBean(MeilisearchClientFactoryBean.class))
        .isInstanceOf(MeilisearchClientFactoryBean.class);
  }
}
