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
import java.util.List;
import java.util.Objects;

import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.lang.Nullable;

import io.vanslog.spring.data.meilisearch.core.ReactiveMeilisearchOperations;
import io.vanslog.spring.data.meilisearch.core.SearchHit;
import io.vanslog.spring.data.meilisearch.core.SearchHits;
import io.vanslog.spring.data.meilisearch.core.TotalHitsRelation;
import io.vanslog.spring.data.meilisearch.core.mapping.MeilisearchPersistentEntity;
import io.vanslog.spring.data.meilisearch.core.mapping.MeilisearchPersistentProperty;
import io.vanslog.spring.data.meilisearch.core.query.BasicQuery;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Executes derived and declared search methods without invoking blocking operations.
 *
 * @author Junghoon Ban
 */
final class ReactiveMeilisearchQuery implements RepositoryQuery {

	private static final int FETCH_PAGE_SIZE = 1000;

	private final Method method;

	private final QueryMethod queryMethod;

	private final Class<?> domainType;

	private final ReactiveMeilisearchOperations operations;

	private final Mono<Void> initialization;

	private final boolean declared;

	private final MeilisearchReactiveQueryReturnShape returnShape;

	private final MeilisearchQueryPlanner planner;

	private final @Nullable MeilisearchDeclaredQueryBinding binding;

	private final @Nullable PartTree tree;

	private final Sort staticSort;

	ReactiveMeilisearchQuery(Method method, RepositoryMetadata metadata, ProjectionFactory projectionFactory,
			ReactiveMeilisearchOperations operations, Mono<Void> initialization, boolean declared, String filter, String q) {

		this.method = method;
		this.queryMethod = new ReactiveMeilisearchQueryMethod(method, metadata, projectionFactory);
		this.domainType = metadata.getDomainType();
		this.operations = operations;
		this.initialization = Objects.requireNonNull(initialization, "Initialization publisher must not be null");
		this.declared = declared;

		var converter = operations.getMeilisearchConverter();
		MappingContext<? extends MeilisearchPersistentEntity<?>, MeilisearchPersistentProperty> mappingContext = converter
				.getMappingContext();
		this.planner = new MeilisearchQueryPlanner(method, queryMethod, domainType, mappingContext,
				converter.getConversionService(), declared ? "declared" : "derived");

		if (declared) {
			this.tree = null;
			this.returnShape = MeilisearchReactiveQueryReturnShape.resolveFinder(method, metadata);
			this.binding = new MeilisearchDeclaredQueryBinding(method, queryMethod, filter, q);
		} else {
			this.tree = planner.parseDerivedQuery();
			this.returnShape = MeilisearchReactiveQueryReturnShape.resolve(tree, method, metadata);
			this.binding = null;
		}

		validateSpecialParameters();
		this.staticSort = planner.mapSort(tree == null ? Sort.unsorted() : tree.getSort());
	}

	@Override
	public @Nullable Object execute(Object[] values) {
		return switch (returnShape) {
			case FLUX -> executeCollection(values);
			case MONO -> executeSingle(values);
			case COUNT -> executeCount(values);
			case EXISTS -> executeExists(values);
			case DELETE_COUNT, DELETE_VOID -> executeDelete(values);
		};
	}

	@Override
	public QueryMethod getQueryMethod() {
		return queryMethod;
	}

	private Flux<Object> executeCollection(Object[] values) {
		return Flux.defer(() -> {
			QueryPlan plan = plan(values);
			return initialization.thenMany(searchCollection(plan));
		});
	}

	private Mono<Object> executeSingle(Object[] values) {
		return Mono.defer(() -> {
			QueryPlan plan = plan(values);
			Sort sort = staticSort.and(plan.dynamicSort());
			return initialization.then(search(plan, sort, PageRequest.of(0, 2))).flatMap(hits -> singleResult(hits));
		});
	}

	private Mono<Long> executeCount(Object[] values) {
		return Mono.defer(() -> {
			QueryPlan plan = plan(values);
			String filter = plan.filters().isEmpty() ? null : String.join(" AND ", plan.filters());
			return initialization
					.then(Mono.defer(() -> filter == null ? operations.count(domainType) : operations.count(domainType, filter)));
		});
	}

	private Mono<Boolean> executeExists(Object[] values) {
		return Mono.defer(() -> {
			QueryPlan plan = plan(values);
			return initialization.then(search(plan, Sort.unsorted(), PageRequest.of(0, 1)))
					.map(hits -> !hits.getSearchHits().isEmpty());
		});
	}

	private Mono<?> executeDelete(Object[] values) {
		return Mono.defer(() -> {
			QueryPlan plan = plan(values);
			if (plan.filters().isEmpty()) {
				throw new IllegalArgumentException(
						"Cannot delete without an effective filter for derived query " + method.toGenericString());
			}
			String filter = String.join(" AND ", plan.filters());
			Mono<Long> deleted = initialization.then(Mono.defer(() -> operations.deleteByFilter(domainType, filter)));
			return returnShape == MeilisearchReactiveQueryReturnShape.DELETE_VOID ? deleted.then() : deleted;
		});
	}

	private QueryPlan plan(Object[] values) {

		ParametersParameterAccessor accessor = new ParametersParameterAccessor(queryMethod.getParameters(), values);
		String q = "";
		List<String> filters;
		if (declared) {
			MeilisearchDeclaredQueryBinding.BoundQuery bound = binding.bind(values);
			q = bound.q();
			filters = bound.filter().isEmpty() ? List.of() : List.of(bound.filter());
		} else {
			filters = planner.createFilters(tree, accessor);
		}

		boolean hasPageable = queryMethod.getParameters().hasPageableParameter();
		Pageable pageable = hasPageable ? accessor.getPageable() : null;
		Sort requestedSort = hasPageable ? pageable.getSort() : accessor.getSort();
		Sort dynamicSort = planner.mapSort(requestedSort);
		return new QueryPlan(q, filters, pageable, dynamicSort);
	}

	private Flux<Object> searchCollection(QueryPlan plan) {

		if (plan.pageable() != null && !plan.pageable().isUnpaged()) {
			Pageable pageable = PageRequest.of(plan.pageable().getPageNumber(), plan.pageable().getPageSize(),
					plan.dynamicSort());
			return search(plan, staticSort, pageable).flatMapMany(this::contentsOf);
		}

		FetchState state = new FetchState();
		Sort sort = staticSort.and(plan.dynamicSort());
		return searchPage(plan, sort, 0, state)
				.expand(page -> page.complete() ? Mono.empty() : searchPage(plan, sort, page.number() + 1, state), 1)
				.concatMap(page -> contentsOf(page.hits()), 0);
	}

	private Mono<SearchPage> searchPage(QueryPlan plan, Sort sort, int pageNumber, FetchState state) {
		return search(plan, sort, PageRequest.of(pageNumber, FETCH_PAGE_SIZE)).map(hits -> {
			state.validate(hits, hits.getSearchHits());
			return new SearchPage(pageNumber, hits, state.isComplete());
		});
	}

	private Mono<SearchHits<Object>> search(QueryPlan plan, Sort sort, Pageable pageable) {
		return Mono.defer(() -> {
			BasicQuery query = planner.createQuery(plan.q(), plan.filters(), sort, pageable);
			return operations.search(query, domainClass());
		});
	}

	@SuppressWarnings("unchecked")
	private Class<Object> domainClass() {
		return (Class<Object>) domainType;
	}

	private Mono<Object> singleResult(SearchHits<Object> hits) {

		long totalHits = hits.getTotalHits();
		int loadedHits = hits.getSearchHits().size();
		if (loadedHits > 1 || hits.getTotalHitsRelation() == TotalHitsRelation.EQUAL_TO && totalHits > 1) {
			int actualSize = totalHits > 1 ? (int) Math.min(totalHits, Integer.MAX_VALUE) : loadedHits;
			throw new IncorrectResultSizeDataAccessException(capitalize(declared ? "declared" : "derived") + " query "
					+ method.toGenericString() + " returned more than one result", 1, actualSize);
		}
		if (hits.getTotalHitsRelation() == TotalHitsRelation.EQUAL_TO && totalHits > loadedHits) {
			throw incompleteResultSet(totalHits, loadedHits);
		}
		return loadedHits == 0 ? Mono.empty() : Mono.just(hits.getSearchHit(0).getContent());
	}

	private Flux<Object> contentsOf(SearchHits<Object> hits) {
		return Flux.fromIterable(hits.getSearchHits()).map(SearchHit::getContent);
	}

	private void validateSpecialParameters() {

		var parameters = queryMethod.getParameters();
		if (parameters.hasDynamicProjection()) {
			throw planner.unsupportedOperator("dynamic projection");
		}
		if (parameters.hasScrollPositionParameter()) {
			throw planner.unsupportedOperator("ScrollPosition");
		}
		if (parameters.hasLimitParameter()) {
			throw planner.unsupportedOperator("Limit");
		}
		if (isProjection() && ((tree != null && tree.getSort().isSorted()) || parameters.hasSortParameter()
				|| parameters.hasPageableParameter())) {
			throw planner.unsupportedOperator("Sort/Pageable for a count, exists, or delete method");
		}
		if (returnShape == MeilisearchReactiveQueryReturnShape.MONO && parameters.hasPageableParameter()) {
			throw planner.unsupportedOperator("Pageable for a single-result method");
		}
	}

	private boolean isProjection() {
		return switch (returnShape) {
			case COUNT, EXISTS, DELETE_COUNT, DELETE_VOID -> true;
			default -> false;
		};
	}

	private IllegalStateException incompleteResultSet(long expected, long loaded) {
		return new IllegalStateException("Meilisearch returned " + loaded + " of " + expected + " exact matches for "
				+ queryDescription() + "; the index maxTotalHits setting may prevent loading the complete result set");
	}

	private String queryDescription() {
		return (declared ? "declared" : "derived") + " query " + method.toGenericString();
	}

	private static String capitalize(String value) {
		return Character.toUpperCase(value.charAt(0)) + value.substring(1);
	}

	private final class FetchState {

		private @Nullable Long expectedTotal;

		private long loaded;

		private void validate(SearchHits<?> hits, List<? extends SearchHit<?>> pageHits) {

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
			if (pageHits.isEmpty() && loaded < expectedTotal) {
				throw incompleteResultSet(expectedTotal, loaded);
			}

			long nextLoaded = loaded + pageHits.size();
			if (nextLoaded > expectedTotal) {
				throw new IllegalStateException("Search returned more results than the exact total for " + queryDescription());
			}
			if (nextLoaded < expectedTotal && pageHits.size() < FETCH_PAGE_SIZE) {
				throw incompleteResultSet(expectedTotal, nextLoaded);
			}
			loaded = nextLoaded;
		}

		private boolean isComplete() {
			return expectedTotal != null && loaded == expectedTotal;
		}

	}

	private record QueryPlan(String q, List<String> filters, @Nullable Pageable pageable, Sort dynamicSort) {
	}

	private record SearchPage(int number, SearchHits<Object> hits, boolean complete) {
	}

}
