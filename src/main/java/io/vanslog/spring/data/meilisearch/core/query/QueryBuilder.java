/*
 * Copyright 2025 the original author or authors.
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
package io.vanslog.spring.data.meilisearch.core.query;

import org.springframework.lang.Nullable;

public abstract class QueryBuilder<Q extends Query, SELF extends QueryBuilder<Q, SELF>> {

	@Nullable protected String q;

	public QueryBuilder() {}

	public SELF withQ(String q) {
		this.q = q;
		return self();
	}

	@Nullable
	public String getQ() {
		return q;
	}

	public abstract Q build();

	protected final SELF self() {
		// noinspection unchecked
		return (SELF) this;
	}
}
