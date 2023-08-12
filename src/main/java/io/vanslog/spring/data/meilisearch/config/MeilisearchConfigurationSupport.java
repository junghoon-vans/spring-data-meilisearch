package io.vanslog.spring.data.meilisearch.config;

import com.meilisearch.sdk.json.GsonJsonHandler;
import com.meilisearch.sdk.json.JsonHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Support class for{@link io.vanslog.spring.data.meilisearch.config.MeilisearchConfiguration}.
 */

@Configuration(proxyBeanMethods = false)
public class MeilisearchConfigurationSupport {

    /**
     * Register a {@link JsonHandler} bean.
     * @return {@link com.meilisearch.sdk.json.JsonHandler}
     */
    @Bean
    public JsonHandler jsonHandler() {
        return new GsonJsonHandler();
    }
}
