package io.vanslog.spring.data.meilisearch.junit.jupiter;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Extension for JUnit Jupiter to provide a Meilisearch connection.
 *
 * @author Junghoon Ban
 */
public class MeilisearchExtension implements BeforeAllCallback{

  @Override
  public void beforeAll(ExtensionContext context) {
    new MeilisearchConnection();
  }
}
