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
package io.vanslog.spring.data.meilisearch.core;

import java.util.List;

import com.meilisearch.sdk.MultiSearchFederation;

import io.vanslog.spring.data.meilisearch.core.query.BaseQuery;
import io.vanslog.spring.data.meilisearch.core.query.FacetQuery;
import io.vanslog.spring.data.meilisearch.core.query.IndexQuery;

/**
 * The operations for the below Meilisearch APIs.
 * <ul>
 * <li><a href="https://www.meilisearch.com/docs/reference/api/search">Search API</a></li>
 * <li><a href="https://www.meilisearch.com/docs/reference/api/multi_search">Multi-Search API</a></li>
 * </ul>
 */
public interface SearchOperations {

	/**
	 * Returns the number of entities available.
	 *
	 * @param clazz the entity class, must be annotated with
	 *          {@link io.vanslog.spring.data.meilisearch.annotations.Document}
	 * @return the number of entities
	 */
	long count(Class<?> clazz);

	/**
	 * Search for entities that meet the criteria using Spring Data style query. Note that by default,
	 * {@literal MaxTotalHits} in {@link io.vanslog.spring.data.meilisearch.annotations.Pagination} is 1000. If you want
	 * to change the value, you can use {@link io.vanslog.spring.data.meilisearch.annotations.Setting}.
	 *
	 * @param query the Spring Data style query supporting Pageable, Sort, etc.
	 * @param clazz the entity class, must be annotated with
	 *          {@link io.vanslog.spring.data.meilisearch.annotations.Document}
	 * @param <T> the type of the entity
	 * @param <Q> the type of query extending BaseQuery
	 * @return the entities found by the query
	 */
	<T, Q extends BaseQuery> SearchHits<T> search(Q query, Class<T> clazz);

	/**
	 * Execute non-federated multi-search query against Meilisearch and return the result. This method allows you to
	 * perform multiple search queries in a single HTTP request, When using {@link IndexQuery}, you can search across
	 * multiple indexes simultaneously.
	 * 
	 * @param queries the list of queries to execute. Each query can target different indexes and have different search
	 *          parameters
	 * @param clazz the entity class, must be annotated with
	 *          {@link io.vanslog.spring.data.meilisearch.annotations.Document}
	 * @param <T> the type of the entity
	 * @param <Q> the type of query extending BaseQuery. When using {@link IndexQuery}, multi-index search is available
	 * @return the list of entities found by the queries
	 */
	<T, Q extends BaseQuery> SearchHits<T> multiSearch(List<Q> queries, Class<T> clazz);

	/**
	 * Execute federated multi-search query against Meilisearch and return the result. Federated search allows you to
	 * combine and process results from multiple indexes using different strategies like merging or joining the results.
	 * But the pageable option in query is not supported in federated multi-search.
	 *
	 * @param queries the list of queries to execute. Each query can target different indexes and have different search
	 *          parameters
	 * @param federation the federation configuration that defines how to combine results from multiple indexes
	 * @param clazz the entity class, must be annotated with
	 *          {@link io.vanslog.spring.data.meilisearch.annotations.Document}
	 * @param <T> the type of the entity
	 * @param <Q> the type of query extending BaseQuery. When using {@link IndexQuery}, multi-index search is available
	 * @return the list of entities found by the queries, combined according to the federation strategy
	 */
	<T, Q extends BaseQuery> SearchHits<T> multiSearch(List<Q> queries, MultiSearchFederation federation, Class<T> clazz);

	/**
	 * Execute the facet search query against meilisearch and return the result.
	 *
	 * @param query the facet query to execute
	 * @param clazz the entity class, must be annotated with
	 *          {@link io.vanslog.spring.data.meilisearch.annotations.Document}
	 * @return the list of entities found by the query
	 */
	SearchHits<FacetHit> facetSearch(FacetQuery query, Class<?> clazz);
}
