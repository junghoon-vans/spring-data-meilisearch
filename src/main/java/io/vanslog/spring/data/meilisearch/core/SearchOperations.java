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

import io.vanslog.spring.data.meilisearch.core.query.BaseQuery;
import io.vanslog.spring.data.meilisearch.core.query.FacetQuery;
import io.vanslog.spring.data.meilisearch.core.query.IndexQuery;

import java.util.List;

import com.meilisearch.sdk.SearchRequest;

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
	 * @return the entities found by the query
	 * @param <T> the type of the entity
	 */
	<T> SearchHits<T> search(BaseQuery query, Class<T> clazz);

	/**
	 * Execute the multi search query against meilisearch and return the result.
	 * 
	 * @param queries the list of queries to execute
	 * @param clazz the entity class, must be annotated with
	 *          {@link io.vanslog.spring.data.meilisearch.annotations.Document}
	 * @return the list of entities found by the queries
	 */
	<T> SearchHits<T> multiSearch(List<IndexQuery> queries, Class<T> clazz);

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
