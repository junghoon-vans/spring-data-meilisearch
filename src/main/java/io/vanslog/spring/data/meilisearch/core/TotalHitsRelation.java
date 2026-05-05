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

/**
 * Relation that describes how {@link SearchHits#getTotalHits()} relates to the total matching result set.
 */
public enum TotalHitsRelation {

	/**
	 * The total hit count is equal to the total matching result set reported by the search engine.
	 */
	EQUAL_TO,

	/**
	 * Total hit metadata is unavailable.
	 */
	OFF
}
