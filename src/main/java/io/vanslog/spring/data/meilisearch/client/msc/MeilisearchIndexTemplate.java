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

import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexOperations;
import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexStats;

/**
 * Meilisearch Java client-backed operations bound to one index.
 *
 * @author Junghoon Ban
 */
class MeilisearchIndexTemplate implements MeilisearchIndexOperations {

	private final String indexUid;
	private final MeilisearchClientExecutor executor;
	private final InstanceResponseConverter responseConverter;

	MeilisearchIndexTemplate(String indexUid, MeilisearchClientExecutor executor,
			InstanceResponseConverter responseConverter) {

		Assert.hasText(indexUid, "Index uid must not be empty");
		Assert.notNull(executor, "MeilisearchClientExecutor must not be null");
		Assert.notNull(responseConverter, "InstanceResponseConverter must not be null");
		this.indexUid = indexUid;
		this.executor = executor;
		this.responseConverter = responseConverter;
	}

	@Override
	public MeilisearchIndexStats stats() {
		return responseConverter.mapIndexStats(executor.execute(client -> client.index(indexUid).getStats()));
	}
}
