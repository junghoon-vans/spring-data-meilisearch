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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Defensive copies of nested JSON-shaped search metadata.
 *
 * @author Junghoon Ban
 */
final class SearchMetadataSupport {

	private SearchMetadataSupport() {}

	static Map<String, Object> immutableMap(Map<String, Object> source) {

		Map<String, Object> copy = new LinkedHashMap<>(source.size());
		source.forEach((key, value) -> copy.put(key, immutableValue(value)));
		return Collections.unmodifiableMap(copy);
	}

	private static Object immutableValue(Object value) {

		if (value instanceof Map<?, ?> source) {
			Map<Object, Object> copy = new LinkedHashMap<>(source.size());
			source.forEach((key, nestedValue) -> copy.put(key, immutableValue(nestedValue)));
			return Collections.unmodifiableMap(copy);
		}

		if (value instanceof List<?> source) {
			List<Object> copy = new ArrayList<>(source.size());
			for (Object element : source) {
				copy.add(immutableValue(element));
			}
			return Collections.unmodifiableList(copy);
		}

		return value;
	}
}
