package io.vanslog.spring.data.meilisearch.config;

import com.meilisearch.sdk.json.GsonJsonHandler;
import com.meilisearch.sdk.json.JacksonJsonHandler;
import com.meilisearch.sdk.json.JsonHandler;

/**
 * Builder class that creates a JSON handler for Meilisearch client.
 *
 * @author Junghoon Ban
 * @see JsonHandler
 * @see GsonJsonHandler
 * @see JacksonJsonHandler
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

    /**
     * Check if the given name is a valid JSON handler.
     * @param name the name to check
     * @return true if the name is a valid JSON handler
     */
    public static boolean contains(String name) {
        for (JsonHandlerBuilder builder : JsonHandlerBuilder.values()) {
            if (builder.name().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Build a JSON handler.
     * @return {@link JsonHandler}
     */
    public abstract JsonHandler build();
}
