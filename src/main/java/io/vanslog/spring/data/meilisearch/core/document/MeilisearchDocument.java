/*
 * Copyright 2023-2026 the original author or authors.
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
package io.vanslog.spring.data.meilisearch.core.document;

import java.util.LinkedHashMap;

/**
 * Map-shaped Meilisearch document representation used between entity mapping and JSON serialization.
 *
 * @author Junghoon Ban
 */
public class MeilisearchDocument extends LinkedHashMap<String, Object> {

	private static final long serialVersionUID = 1L;

	public static MeilisearchDocument create() {
		return new MeilisearchDocument();
	}

	public MeilisearchDocument append(String key, Object value) {

		put(key, value);
		return this;
	}
}
