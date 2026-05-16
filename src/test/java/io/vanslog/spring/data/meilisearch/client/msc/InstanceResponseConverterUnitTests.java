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

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import com.meilisearch.sdk.json.GsonJsonHandler;
import com.meilisearch.sdk.model.IndexStats;
import com.meilisearch.sdk.model.Results;
import com.meilisearch.sdk.model.Stats;

import io.vanslog.spring.data.meilisearch.core.MeilisearchHealth;
import io.vanslog.spring.data.meilisearch.core.MeilisearchIndex;
import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexList;
import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexStats;
import io.vanslog.spring.data.meilisearch.core.MeilisearchStats;
import io.vanslog.spring.data.meilisearch.core.MeilisearchVersion;

/**
 * Unit tests for {@link InstanceResponseConverter}.
 *
 * @author Junghoon Ban
 */
class InstanceResponseConverterUnitTests {

	private final InstanceResponseConverter converter = new InstanceResponseConverter(new GsonJsonHandler());

	@Test
	void shouldConvertHealthPayload() {

		MeilisearchHealth health = converter.mapHealth("{\"status\":\"available\"}");

		assertThat(health.getStatus()).isEqualTo("available");
	}

	@Test
	void shouldConvertVersionPayload() {

		MeilisearchVersion version = converter.mapVersion("""
				{
				  "commitSha": "b46889b5e94202a4186a0f80831d4891d2e0c67e",
				  "commitDate": "2024-12-09T10:00:00Z",
				  "pkgVersion": "1.12.3"
				}
				""");

		assertThat(version.getCommitSha()).isEqualTo("b46889b5e94202a4186a0f80831d4891d2e0c67e");
		assertThat(version.getCommitDate()).isEqualTo("2024-12-09T10:00:00Z");
		assertThat(version.getPackageVersion()).isEqualTo("1.12.3");
	}

	@Test
	void shouldConvertStats() {

		Instant lastUpdate = Instant.parse("2024-12-09T10:00:00Z");
		Map<String, IndexStats> indexes = new LinkedHashMap<>();
		indexes.put("movies", indexStats());

		MeilisearchStats stats = converter.mapStats(new Stats(2048, Date.from(lastUpdate), indexes, 1024));

		assertThat(stats.getDatabaseSize()).isEqualTo(2048);
		assertThat(stats.getUsedDatabaseSize()).isEqualTo(1024);
		assertThat(stats.getLastUpdate()).isEqualTo(lastUpdate);
		assertThat(stats.getIndexes()).containsOnlyKeys("movies");
		assertThat(stats.getIndexes().get("movies").getNumberOfDocuments()).isEqualTo(10);
	}

	@Test
	void shouldConvertIndex() throws MeilisearchException {

		Index sdkIndex = new GsonJsonHandler().decode("""
				{
				  "uid": "movies",
				  "primaryKey": "id",
				  "createdAt": "2026-05-16T01:00:00.000000Z",
				  "updatedAt": "2026-05-16T01:01:00.000000Z"
				}
				""", Index.class);

		MeilisearchIndex index = converter.mapIndex(sdkIndex);

		assertThat(index.getUid()).isEqualTo("movies");
		assertThat(index.getPrimaryKey()).isEqualTo("id");
		assertThat(index.getCreatedAt()).isEqualTo("2026-05-16T01:00:00.000000Z");
		assertThat(index.getUpdatedAt()).isEqualTo("2026-05-16T01:01:00.000000Z");
	}

	@Test
	void shouldConvertIndexList() throws MeilisearchException {

		Results<Index> sdkResults = new GsonJsonHandler().decode("""
				{
				  "results": [
				    {
				      "uid": "movies",
				      "primaryKey": "id",
				      "createdAt": "2026-05-16T01:00:00.000000Z",
				      "updatedAt": "2026-05-16T01:01:00.000000Z"
				    }
				  ],
				  "offset": 0,
				  "limit": 20,
				  "total": 1
				}
				""", Results.class, Index.class);

		MeilisearchIndexList indexes = converter.mapIndexes(sdkResults);

		assertThat(indexes.getIndexes()).extracting(MeilisearchIndex::getUid).containsExactly("movies");
		assertThat(indexes.getOffset()).isZero();
		assertThat(indexes.getLimit()).isEqualTo(20);
		assertThat(indexes.getTotal()).isEqualTo(1);
	}

	@Test
	void shouldConvertIndexStats() {

		MeilisearchIndexStats stats = converter.mapIndexStats(indexStats());

		assertThat(stats.getNumberOfDocuments()).isEqualTo(10);
		assertThat(stats.isIndexing()).isTrue();
		assertThat(stats.getFieldDistribution()).containsEntry("title", 10L).containsEntry("genre", 8L);
		assertThat(stats.getRawDocumentDbSize()).isEqualTo(512);
		assertThat(stats.getAvgDocumentSize()).isEqualTo(51);
		assertThat(stats.getNumberOfEmbeddedDocuments()).isEqualTo(7);
		assertThat(stats.getNumberOfEmbeddings()).isEqualTo(14);
	}

	private static IndexStats indexStats() {

		Map<String, Integer> fieldDistribution = new LinkedHashMap<>();
		fieldDistribution.put("title", 10);
		fieldDistribution.put("genre", 8);
		return new IndexStats(10, true, fieldDistribution, 512, 51, 7, 14);
	}
}
