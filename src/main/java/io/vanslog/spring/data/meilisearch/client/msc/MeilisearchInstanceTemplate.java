/*
 * Copyright 2026 the original author or authors.
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
package io.vanslog.spring.data.meilisearch.client.msc;

import org.springframework.util.Assert;

import io.vanslog.spring.data.meilisearch.core.MeilisearchHealth;
import io.vanslog.spring.data.meilisearch.core.MeilisearchInstanceOperations;
import io.vanslog.spring.data.meilisearch.core.MeilisearchStats;
import io.vanslog.spring.data.meilisearch.core.MeilisearchVersion;

/**
 * Meilisearch Java client-backed instance operations.
 *
 * @author Junghoon Ban
 */
class MeilisearchInstanceTemplate implements MeilisearchInstanceOperations {

	private final MeilisearchClientExecutor executor;
	private final InstanceResponseConverter responseConverter;

	MeilisearchInstanceTemplate(MeilisearchClientExecutor executor, InstanceResponseConverter responseConverter) {

		Assert.notNull(executor, "MeilisearchClientExecutor must not be null");
		Assert.notNull(responseConverter, "InstanceResponseConverter must not be null");
		this.executor = executor;
		this.responseConverter = responseConverter;
	}

	@Override
	public MeilisearchHealth health() {
		return responseConverter.mapHealth(executor.execute(client -> client.health()));
	}

	@Override
	public boolean isHealthy() {
		return Boolean.TRUE.equals(executor.execute(client -> client.isHealthy()));
	}

	@Override
	public MeilisearchVersion version() {
		return responseConverter.mapVersion(executor.execute(client -> client.getVersion()));
	}

	@Override
	public MeilisearchStats stats() {
		return responseConverter.mapStats(executor.execute(client -> client.getStats()));
	}
}
