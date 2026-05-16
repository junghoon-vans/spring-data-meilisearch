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

import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import com.meilisearch.sdk.exceptions.MeilisearchException;
import com.meilisearch.sdk.json.JsonHandler;
import com.meilisearch.sdk.model.IndexStats;
import com.meilisearch.sdk.model.Stats;

import io.vanslog.spring.data.meilisearch.UncategorizedMeilisearchException;
import io.vanslog.spring.data.meilisearch.core.MeilisearchHealth;
import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexStats;
import io.vanslog.spring.data.meilisearch.core.MeilisearchStats;
import io.vanslog.spring.data.meilisearch.core.MeilisearchVersion;

/**
 * Converts Meilisearch instance-level SDK responses into Spring Data Meilisearch responses.
 *
 * @author Junghoon Ban
 */
class InstanceResponseConverter {

	private final JsonHandler jsonHandler;

	InstanceResponseConverter(JsonHandler jsonHandler) {

		Assert.notNull(jsonHandler, "JsonHandler must not be null");
		this.jsonHandler = jsonHandler;
	}

	MeilisearchHealth mapHealth(String payload) {

		try {
			HealthResponse response = jsonHandler.decode(payload, HealthResponse.class);
			return new MeilisearchHealth(response.status);
		} catch (MeilisearchException e) {
			throw new UncategorizedMeilisearchException("Failed to read health response.", e);
		}
	}

	MeilisearchVersion mapVersion(String payload) {

		try {
			VersionResponse response = jsonHandler.decode(payload, VersionResponse.class);
			return new MeilisearchVersion(response.commitSha, response.commitDate, response.pkgVersion);
		} catch (MeilisearchException e) {
			throw new UncategorizedMeilisearchException("Failed to read version response.", e);
		}
	}

	MeilisearchStats mapStats(Stats stats) {

		Map<String, MeilisearchIndexStats> indexes = new LinkedHashMap<>();
		if (stats.getIndexes() != null) {
			stats.getIndexes().forEach((indexUid, indexStats) -> indexes.put(indexUid, mapIndexStats(indexStats)));
		}

		Date lastUpdate = stats.getLastUpdate();
		Instant lastUpdateInstant = lastUpdate != null ? lastUpdate.toInstant() : null;

		return new MeilisearchStats(stats.getDatabaseSize(), stats.getUsedDatabaseSize(), lastUpdateInstant, indexes);
	}

	MeilisearchIndexStats mapIndexStats(IndexStats stats) {

		Map<String, Long> fieldDistribution = new LinkedHashMap<>();
		if (stats.getFieldDistribution() != null) {
			stats.getFieldDistribution()
					.forEach((fieldName, count) -> fieldDistribution.put(fieldName, count.longValue()));
		}

		return new MeilisearchIndexStats(stats.getNumberOfDocuments(), stats.isIndexing(), fieldDistribution,
				stats.getRawDocumentDbSize(), stats.getAvgDocumentSize(), stats.getNumberOfEmbeddedDocuments(),
				stats.getNumberOfEmbeddings());
	}

	private static class HealthResponse {

		@Nullable String status;
	}

	private static class VersionResponse {

		@Nullable String commitSha;
		@Nullable String commitDate;
		@Nullable String pkgVersion;
	}
}
