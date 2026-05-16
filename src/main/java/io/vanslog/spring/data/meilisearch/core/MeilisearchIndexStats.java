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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * Statistics for a Meilisearch index.
 *
 * @author Junghoon Ban
 */
public class MeilisearchIndexStats {

	private final long numberOfDocuments;
	private final boolean indexing;
	private final Map<String, Long> fieldDistribution;
	private final long rawDocumentDbSize;
	private final long avgDocumentSize;
	private final long numberOfEmbeddedDocuments;
	private final long numberOfEmbeddings;

	public MeilisearchIndexStats(long numberOfDocuments, boolean indexing, Map<String, Long> fieldDistribution,
			long rawDocumentDbSize, long avgDocumentSize, long numberOfEmbeddedDocuments, long numberOfEmbeddings) {

		Assert.notNull(fieldDistribution, "Field distribution must not be null");

		this.numberOfDocuments = numberOfDocuments;
		this.indexing = indexing;
		this.fieldDistribution = Collections.unmodifiableMap(new LinkedHashMap<>(fieldDistribution));
		this.rawDocumentDbSize = rawDocumentDbSize;
		this.avgDocumentSize = avgDocumentSize;
		this.numberOfEmbeddedDocuments = numberOfEmbeddedDocuments;
		this.numberOfEmbeddings = numberOfEmbeddings;
	}

	public long getNumberOfDocuments() {
		return numberOfDocuments;
	}

	public boolean isIndexing() {
		return indexing;
	}

	public Map<String, Long> getFieldDistribution() {
		return fieldDistribution;
	}

	public long getRawDocumentDbSize() {
		return rawDocumentDbSize;
	}

	public long getAvgDocumentSize() {
		return avgDocumentSize;
	}

	public long getNumberOfEmbeddedDocuments() {
		return numberOfEmbeddedDocuments;
	}

	public long getNumberOfEmbeddings() {
		return numberOfEmbeddings;
	}
}
