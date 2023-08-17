package io.vanslog.spring.data.meilisearch.config;

import io.vanslog.spring.data.meilisearch.core.convert.MappingMeilisearchConverter;
import io.vanslog.spring.data.meilisearch.core.convert.MeilisearchConverter;
import io.vanslog.spring.data.meilisearch.core.mapping.SimpleMeilisearchMappingContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Support class for{@link io.vanslog.spring.data.meilisearch.config.MeilisearchConfiguration}.
 */

@Configuration(proxyBeanMethods = false)
public class MeilisearchConfigurationSupport {

    /**
     * Create a {@link MeilisearchConverter} bean.
     *
     * @param meilisearchMappingContext the {@link SimpleMeilisearchMappingContext} to use
     * @return the created {@link MeilisearchConverter} bean.
     */
    @Bean
    public MeilisearchConverter meilisearchConverter(
            SimpleMeilisearchMappingContext meilisearchMappingContext) {

        return new MappingMeilisearchConverter(meilisearchMappingContext);
    }

    /**
     * Create a {@link SimpleMeilisearchMappingContext} bean.
     *
     * @return the created {@link SimpleMeilisearchMappingContext} bean.
     */
    @Bean
    public SimpleMeilisearchMappingContext meilisearchMappingContext() {
        return new SimpleMeilisearchMappingContext();
    }
}
