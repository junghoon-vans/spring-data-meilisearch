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

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Library-owned options for merging facet results across federated indexes. A null maximum leaves the server default
 * unchanged.
 *
 * @param maxValuesPerFacet maximum values per merged facet, or null for the server default
 * @author Junghoon Ban
 */
public record FacetMergeOptions(@Nullable Integer maxValuesPerFacet) {

	public FacetMergeOptions {
		Assert.isTrue(maxValuesPerFacet == null || maxValuesPerFacet >= 0, "Maximum facet values must not be negative");
	}
}
