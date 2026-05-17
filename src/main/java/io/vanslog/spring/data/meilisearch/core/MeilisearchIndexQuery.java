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

import org.springframework.util.Assert;

/**
 * Query options to list Meilisearch indexes.
 *
 * @author Junghoon Ban
 */
public class MeilisearchIndexQuery {

	private final int offset;
	private final int limit;

	public MeilisearchIndexQuery(int offset, int limit) {

		Assert.isTrue(offset >= 0, "Offset must not be negative");
		Assert.isTrue(limit > 0, "Limit must be greater than zero");
		this.offset = offset;
		this.limit = limit;
	}

	public int getOffset() {
		return offset;
	}

	public int getLimit() {
		return limit;
	}
}
