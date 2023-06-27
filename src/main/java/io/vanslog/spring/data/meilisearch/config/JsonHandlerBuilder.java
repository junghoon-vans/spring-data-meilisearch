package io.vanslog.spring.data.meilisearch.config;

import com.meilisearch.sdk.json.GsonJsonHandler;
import com.meilisearch.sdk.json.JacksonJsonHandler;
import com.meilisearch.sdk.json.JsonHandler;

/**
 * Builder class that creates a JSON handler for Meilisearch client.
 *
 * @since 1.0.0
 * @see JsonHandler
 * @see GsonJsonHandler
 * @see JacksonJsonHandler
 * @author Junghoon Ban
 */
public enum JsonHandlerBuilder {
  GSON {
    @Override
    public JsonHandler build() {
      return new GsonJsonHandler();
    }
  },
  JACKSON {
    @Override
    public JsonHandler build() {
      return new JacksonJsonHandler();
    }
  };

  public abstract JsonHandler build();
}
