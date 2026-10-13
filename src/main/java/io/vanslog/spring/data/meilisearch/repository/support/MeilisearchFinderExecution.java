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
package io.vanslog.spring.data.meilisearch.repository.support;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.lang.Nullable;

import io.vanslog.spring.data.meilisearch.core.MeilisearchOperations;
import io.vanslog.spring.data.meilisearch.core.SearchHit;
import io.vanslog.spring.data.meilisearch.core.SearchHits;
import io.vanslog.spring.data.meilisearch.core.TotalHitsRelation;
import io.vanslog.spring.data.meilisearch.core.query.BasicQuery;

/**
 * Executes finder queries through {@link MeilisearchOperations}.
 *
 * @author Junghoon Ban
 */
final class MeilisearchFinderExecution {

	private static final int FETCH_PAGE_SIZE = 1000;

	private final Method method;

	private final QueryMethod queryMethod;

	private final Class<?> domainType;

	private final MeilisearchOperations operations;

	private final MeilisearchQueryReturnShape returnShape;

	private final String queryKind;

	private final MeilisearchQueryPlanner planner;

	private final Sort staticSort;

	MeilisearchFinderExecution(Method method, QueryMethod queryMethod, Class<?> domainType,
			MeilisearchOperations operations, MeilisearchQueryReturnShape returnShape, Sort staticSort,
			MeilisearchQueryPlanner planner, String queryKind) {

		this.method = method;
		this.queryMethod = queryMethod;
		this.domainType = domainType;
		this.operations = operations;
		this.returnShape = returnShape;
		this.planner = planner;
		this.queryKind = queryKind;
		this.staticSort = planner.mapSort(staticSort);
	}

	@Nullable
	Object execute(String q, List<String> filters, ParametersParameterAccessor accessor) {

		boolean hasPageable = queryMethod.getParameters().hasPageableParameter();
		Pageable pageable = hasPageable ? accessor.getPageable() : null;
		Sort dynamicSort = hasPageable ? pageable.getSort() : accessor.getSort();
		Sort mappedDynamicSort = mapSort(dynamicSort);
		Sort completeSort = staticSort.and(mappedDynamicSort);

		return switch (returnShape) {
			case ENTITY, OPTIONAL -> executeSingle(q, filters, completeSort);
			case PAGE -> executePage(q, filters, pageable, mappedDynamicSort);
			case LIST, ITERABLE -> executeCollection(q, filters, pageable, completeSort, mappedDynamicSort);
			case COUNT, EXISTS, DELETE_COUNT, DELETE_VOID -> throw new IllegalStateException("Projection already handled");
		};
	}

	BasicQuery createQuery(String q, List<String> filters, Sort sort, Pageable pageable) {
		return planner.createQuery(q, filters, sort, pageable);
	}

	@Nullable
	private Object executeSingle(String q, List<String> filters, Sort sort) {

		SearchHits<?> hits = operations.search(createQuery(q, filters, sort, PageRequest.of(0, 2)), domainType);
		long totalHits = hits.getTotalHits();
		int loadedHits = hits.getSearchHits().size();

		if (loadedHits > 1 || hits.getTotalHitsRelation() == TotalHitsRelation.EQUAL_TO && totalHits > 1) {
			int actualSize = totalHits > 1 ? (int) Math.min(totalHits, Integer.MAX_VALUE) : loadedHits;
			throw new IncorrectResultSizeDataAccessException(
					capitalize(queryKind) + " query " + method.toGenericString() + " returned more than one result", 1,
					actualSize);
		}

		if (hits.getTotalHitsRelation() == TotalHitsRelation.EQUAL_TO && totalHits > loadedHits) {
			throw incompleteResultSet(totalHits, loadedHits);
		}

		Object result = loadedHits == 0 ? null : hits.getSearchHit(0).getContent();
		return returnShape == MeilisearchQueryReturnShape.OPTIONAL ? Optional.ofNullable(result) : result;
	}

	private Object executePage(String q, List<String> filters, Pageable pageable, Sort dynamicSort) {

		if (pageable.isUnpaged()) {
			List<Object> content = fetchAll(q, filters, staticSort.and(dynamicSort));
			return new PageImpl<>(content, pageable, content.size());
		}

		Pageable mappedPageable = mapPageable(pageable, dynamicSort);
		SearchHits<?> hits = operations.search(createQuery(q, filters, staticSort, mappedPageable), domainType);
		return new PageImpl<>(contentsOf(hits), pageable, hits.getTotalHits());
	}

	private Object executeCollection(String q, List<String> filters, Pageable pageable, Sort completeSort,
			Sort dynamicSort) {

		List<Object> content;
		if (pageable == null || pageable.isUnpaged()) {
			content = fetchAll(q, filters, completeSort);
		} else {
			Pageable mappedPageable = mapPageable(pageable, dynamicSort);
			SearchHits<?> hits = operations.search(createQuery(q, filters, staticSort, mappedPageable), domainType);
			content = contentsOf(hits);
		}

		return returnShape == MeilisearchQueryReturnShape.LIST ? content : (Iterable<?>) content;
	}

	private List<Object> fetchAll(String q, List<String> filters, Sort sort) {

		List<Object> results = new ArrayList<>();
		Long expectedTotal = null;
		int pageNumber = 0;

		while (true) {
			Pageable pageable = PageRequest.of(pageNumber, FETCH_PAGE_SIZE);
			SearchHits<?> hits = operations.search(createQuery(q, filters, sort, pageable), domainType);

			if (hits.getTotalHitsRelation() != TotalHitsRelation.EQUAL_TO) {
				throw new IllegalStateException("Cannot safely return all results for " + queryDescription()
						+ ": Meilisearch did not report an exact total hit count");
			}

			long total = hits.getTotalHits();
			if (total < 0 || total > Integer.MAX_VALUE) {
				throw new IllegalStateException("Cannot materialize " + total + " results for " + queryDescription());
			}

			if (expectedTotal == null) {
				expectedTotal = total;
			} else if (expectedTotal.longValue() != total) {
				throw new IllegalStateException("Result count changed while loading all results for " + queryDescription());
			}

			List<? extends SearchHit<?>> pageHits = hits.getSearchHits();
			if (pageHits.isEmpty() && results.size() < expectedTotal) {
				throw incompleteResultSet(expectedTotal, results.size());
			}

			for (SearchHit<?> hit : pageHits) {
				results.add(hit.getContent());
			}

			if (results.size() > expectedTotal) {
				throw new IllegalStateException("Search returned more results than the exact total for " + queryDescription());
			}
			if (results.size() == expectedTotal) {
				return results;
			}
			if (pageHits.size() < FETCH_PAGE_SIZE) {
				throw incompleteResultSet(expectedTotal, results.size());
			}

			pageNumber++;
		}
	}

	private IllegalStateException incompleteResultSet(long expected, long loaded) {
		return new IllegalStateException("Meilisearch returned " + loaded + " of " + expected + " exact matches for "
				+ queryDescription() + "; the index maxTotalHits setting may prevent loading the complete result set");
	}

	private List<Object> contentsOf(SearchHits<?> hits) {

		List<Object> contents = new ArrayList<>(hits.getSearchHits().size());
		for (SearchHit<?> hit : hits.getSearchHits()) {
			contents.add(hit.getContent());
		}
		return contents;
	}

	private Sort mapSort(Sort sort) {
		return planner.mapSort(sort);
	}

	private Pageable mapPageable(Pageable pageable, Sort mappedSort) {
		return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), mappedSort);
	}

	private String queryDescription() {
		return queryKind + " query " + method.toGenericString();
	}

	private static String capitalize(String value) {
		return Character.toUpperCase(value.charAt(0)) + value.substring(1);
	}

}
