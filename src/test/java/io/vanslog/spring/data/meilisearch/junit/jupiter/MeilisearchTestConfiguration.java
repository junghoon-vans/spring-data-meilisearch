/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    public ClientConfiguration clientConfiguration() {
        return ClientConfiguration.builder()
                .connectedTo(HTTP + meilisearchConnectionInfo.getHost() + ":" +
                        meilisearchConnectionInfo.getPort())
                .withApiKey(meilisearchConnectionInfo.getMasterKey())
                .build();
    }
}
