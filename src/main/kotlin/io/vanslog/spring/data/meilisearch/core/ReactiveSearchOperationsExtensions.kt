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
package io.vanslog.spring.data.meilisearch.core

import io.vanslog.spring.data.meilisearch.core.federation.Federation
import io.vanslog.spring.data.meilisearch.core.query.BaseQuery
import io.vanslog.spring.data.meilisearch.core.query.FacetQuery
import io.vanslog.spring.data.meilisearch.core.query.SimilarQuery
import reactor.core.publisher.Mono

inline fun <reified T : Any> ReactiveSearchOperations.search(query: BaseQuery): Mono<SearchHits<T>> =
    search(query, T::class.java)

inline fun <reified T : Any> ReactiveSearchOperations.multiSearch(queries: List<BaseQuery>): Mono<SearchHits<T>> =
    multiSearch(queries, T::class.java)

inline fun <reified T : Any> ReactiveSearchOperations.multiSearch(
    queries: List<BaseQuery>,
    federation: Federation,
): Mono<SearchHits<T>> = multiSearch(queries, federation, T::class.java)

inline fun <reified T : Any> ReactiveSearchOperations.facetSearch(query: FacetQuery): Mono<SearchHits<FacetHit>> =
    facetSearch(query, T::class.java)

inline fun <reified T : Any> ReactiveSearchOperations.similarSearch(query: SimilarQuery): Mono<SearchHits<T>> =
    similarSearch(query, T::class.java)
