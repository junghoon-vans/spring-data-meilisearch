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
package io.vanslog.spring.data.meilisearch.core.federation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Library-owned federated multi-search options. Null components leave server defaults unchanged. These options control
 * the federation window instead of the individual queries' Pageable.
 *
 * @param limit maximum federated hits
 * @param offset federated result offset
 * @param facetsByIndex requested facets keyed by index UID
 * @param mergeFacets optional facet merging configuration
 * @author Junghoon Ban
 */
public record Federation(@Nullable Integer limit, @Nullable Integer offset,
		@Nullable Map<String, List<String>> facetsByIndex, @Nullable FacetMergeOptions mergeFacets) {

	public Federation {
		Assert.isTrue(limit == null || limit >= 0, "Federation limit must not be negative");
		Assert.isTrue(offset == null || offset >= 0, "Federation offset must not be negative");
		if (facetsByIndex != null) {
			Map<String, List<String>> copy = new LinkedHashMap<>();
			facetsByIndex.forEach((index, facets) -> copy.put(Objects.requireNonNull(index), List.copyOf(facets)));
			facetsByIndex = Collections.unmodifiableMap(copy);
		}
	}
}
