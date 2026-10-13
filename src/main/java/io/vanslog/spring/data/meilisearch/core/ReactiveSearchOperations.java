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
import io.vanslog.spring.data.meilisearch.core.federation.Federation;
import io.vanslog.spring.data.meilisearch.core.query.BaseQuery;
import io.vanslog.spring.data.meilisearch.core.query.FacetQuery;
import io.vanslog.spring.data.meilisearch.core.query.SimilarQuery;
import reactor.core.publisher.Mono;

/**
 * Cold, non-blocking search operations. Each publisher consumes a finite JSON response and preserves mapped hit
 * metadata; it does not promise server-side streaming.
 *
 * @author Junghoon Ban
 */
public interface ReactiveSearchOperations {

	/** Search using the supplied query, retaining hit and pagination metadata. */
	<T, Q extends BaseQuery> Mono<SearchHits<T>> search(Q query, Class<T> clazz);

	/** Execute non-federated multi-search and retain each hit's query metadata. */
	<T, Q extends BaseQuery> Mono<SearchHits<T>> multiSearch(List<Q> queries, Class<T> clazz);

	/** Execute federated multi-search; federation limit/offset, not Pageable, control its window. */
	<T, Q extends BaseQuery> Mono<SearchHits<T>> multiSearch(List<Q> queries, Federation federation, Class<T> clazz);

	/** Search the values of a configured searchable facet. */
	Mono<SearchHits<FacetHit>> facetSearch(FacetQuery query, Class<?> clazz);

	/** Search similar documents using the configured embedder. */
	<T> Mono<SearchHits<T>> similarSearch(SimilarQuery query, Class<T> clazz);
}
