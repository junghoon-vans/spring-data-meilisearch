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

import io.vanslog.testcontainers.meilisearch.MeilisearchContainer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.utility.DockerImageName;

/**
 * This class manages the connection to Meilisearch Instance.
 *
 * @author Junghoon Ban
 */
public class MeilisearchConnection implements ExtensionContext.Store.CloseableResource {

	private static final Log LOGGER = LogFactory.getLog(MeilisearchConnection.class);
	private static final String TESTCONTAINERS_IMAGE_NAME = "getmeili/meilisearch";
	private static final String TESTCONTAINERS_IMAGE_VERSION = "v1.2.0";
	private static final int MEILISEARCH_DEFAULT_PORT = 7700;
	private static final String MEILISEARCH_DEFAULT_MASTER_KEY = "masterKey";
	private static final ThreadLocal<MeilisearchConnectionInfo> meilisearchConnectionInfoThreadLocal = new ThreadLocal<>();
	private final MeilisearchConnectionInfo meilisearchConnectionInfo;

	public MeilisearchConnection() {
		meilisearchConnectionInfo = createConnectionInfo();
		if (meilisearchConnectionInfo != null) {
			meilisearchConnectionInfoThreadLocal.set(meilisearchConnectionInfo);
		}
	}

	public static MeilisearchConnectionInfo meilisearchConnectionInfo() {
		return meilisearchConnectionInfoThreadLocal.get();
	}

	public MeilisearchConnectionInfo getMeilisearchConnectionInfo() {
		return meilisearchConnectionInfo;
	}

	public MeilisearchConnectionInfo createConnectionInfo() {
		DockerImageName dockerImageName = DockerImageName.parse(TESTCONTAINERS_IMAGE_NAME)
				.withTag(TESTCONTAINERS_IMAGE_VERSION);
		MeilisearchContainer meilisearchContainer = new MeilisearchContainer(dockerImageName)
				.withMasterKey(MEILISEARCH_DEFAULT_MASTER_KEY);
		meilisearchContainer.start();

		return MeilisearchConnectionInfo.builder().host(meilisearchContainer.getHost())
				.port(meilisearchContainer.getMappedPort(MEILISEARCH_DEFAULT_PORT)).masterKey(MEILISEARCH_DEFAULT_MASTER_KEY)
				.meilisearchContainer(meilisearchContainer).build();
	}

	@Override
	public void close() {
		if (meilisearchConnectionInfo != null && meilisearchConnectionInfo.getMeilisearchContainer() != null) {
			LOGGER.debug("stopping MeilisearchConnection");
			meilisearchConnectionInfo.getMeilisearchContainer().stop();
		}
		LOGGER.debug("closed MeilisearchConnection");
	}
}
