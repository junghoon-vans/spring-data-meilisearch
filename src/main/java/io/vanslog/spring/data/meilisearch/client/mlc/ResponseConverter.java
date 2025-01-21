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
package io.vanslog.spring.data.meilisearch.client.mlc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meilisearch.sdk.model.MultiSearchResult;
import com.meilisearch.sdk.model.Results;
import com.meilisearch.sdk.model.Searchable;
import java.util.Arrays;
import java.util.List;

/**
 * Class to convert Meilisearch classes into Spring Data Meilisearch responses.
 */
public class ResponseConverter {

	private final ObjectMapper objectMapper;

	public ResponseConverter() {
		this.objectMapper = new ObjectMapper();
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> bySearchable(Searchable searchable, Class<?> clazz) {
		return (List<T>) searchable.getHits().stream() //
				.map(hit -> objectMapper.convertValue(hit, clazz)) //
				.toList();
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> byMultiSearchResults(Results<MultiSearchResult> results, Class<?> clazz) {
		return (List<T>) Arrays.stream(results.getResults()) //
				.flatMap(result -> bySearchable(result, clazz).stream())
				.toList();
	}
}
