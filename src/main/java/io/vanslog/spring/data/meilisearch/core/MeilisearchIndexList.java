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

import java.util.List;

import org.springframework.util.Assert;

/**
 * A page of Meilisearch indexes.
 *
 * @author Junghoon Ban
 */
public class MeilisearchIndexList {

	private final List<MeilisearchIndex> indexes;
	private final int offset;
	private final int limit;
	private final int total;

	public MeilisearchIndexList(List<MeilisearchIndex> indexes, int offset, int limit, int total) {

		Assert.notNull(indexes, "Indexes must not be null");
		this.indexes = List.copyOf(indexes);
		this.offset = offset;
		this.limit = limit;
		this.total = total;
	}

	public List<MeilisearchIndex> getIndexes() {
		return indexes;
	}

	public int getOffset() {
		return offset;
	}

	public int getLimit() {
		return limit;
	}

	public int getTotal() {
		return total;
	}
}
