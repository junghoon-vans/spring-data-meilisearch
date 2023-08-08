package io.vanslog.spring.data.meilisearch.junit.jupiter;

import com.meilisearch.sdk.Config;
import io.vanslog.spring.data.meilisearch.client.ClientConfiguration;
import io.vanslog.spring.data.meilisearch.config.MeilisearchConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * Custom Meilisearch configuration for integration tests.
 */
@Configuration
public class MeilisearchTestConfiguration extends MeilisearchConfiguration {

    private static final String HTTP = "http://";

    private final MeilisearchConnectionInfo meilisearchConnectionInfo
            = MeilisearchConnection.meilisearchConnectionInfo();

    @Override
    public Config clientConfiguration() {
        return ClientConfiguration.builder()
                .connectedTo(HTTP + meilisearchConnectionInfo.getHost() + ":" +
                        meilisearchConnectionInfo.getPort())
                .withApiKey(meilisearchConnectionInfo.getMasterKey())
                .build();
    }
}
