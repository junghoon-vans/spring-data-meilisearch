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
package io.vanslog.spring.data.meilisearch.core;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Statistics for a Meilisearch instance.
 *
 * @author Junghoon Ban
 */
public class MeilisearchStats {

	private final long databaseSize;
	private final long usedDatabaseSize;
	@Nullable private final Instant lastUpdate;
	private final Map<String, MeilisearchIndexStats> indexes;

	public MeilisearchStats(long databaseSize, long usedDatabaseSize, @Nullable Instant lastUpdate,
			Map<String, MeilisearchIndexStats> indexes) {

		Assert.notNull(indexes, "Indexes must not be null");

		this.databaseSize = databaseSize;
		this.usedDatabaseSize = usedDatabaseSize;
		this.lastUpdate = lastUpdate;
		this.indexes = Collections.unmodifiableMap(new LinkedHashMap<>(indexes));
	}

	public long getDatabaseSize() {
		return databaseSize;
	}

	public long getUsedDatabaseSize() {
		return usedDatabaseSize;
	}

	@Nullable
	public Instant getLastUpdate() {
		return lastUpdate;
	}

	public Map<String, MeilisearchIndexStats> getIndexes() {
		return indexes;
	}
}
