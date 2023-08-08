package io.vanslog.spring.data.meilisearch.core;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTest;
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

@MeilisearchTest
@ContextConfiguration(classes = {MeilisearchTestConfiguration.class})
class MeilisearchTemplateTest {

  @Autowired
  Client client;

  @Test
  void test() throws MeilisearchException {
    assertThat(client.isHealthy()).isTrue();
  }
}
