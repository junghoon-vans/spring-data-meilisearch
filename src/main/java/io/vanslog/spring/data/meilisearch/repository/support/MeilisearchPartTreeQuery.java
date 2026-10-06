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

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.lang.Nullable;

import io.vanslog.spring.data.meilisearch.core.MeilisearchOperations;

/**
 * Executes the supported, filter-backed subset of Spring Data derived queries.
 *
 * @author Junghoon Ban
 */
class MeilisearchPartTreeQuery implements RepositoryQuery {

	private final Method method;

	private final QueryMethod queryMethod;

	private final PartTree tree;

	private final Class<?> domainType;

	private final MeilisearchOperations operations;

	private final MeilisearchQueryReturnShape returnShape;

	private final MeilisearchQueryPlanner planner;

	private final MeilisearchFinderExecution finderExecution;

	MeilisearchPartTreeQuery(Method method, RepositoryMetadata metadata, ProjectionFactory projectionFactory,
			MeilisearchOperations operations) {

		this.method = method;
		this.queryMethod = new QueryMethod(method, metadata, projectionFactory);
		this.domainType = metadata.getDomainType();
		this.operations = operations;
		var converter = operations.getMeilisearchConverter();
		this.planner = new MeilisearchQueryPlanner(method, queryMethod, domainType, converter.getMappingContext(),
				converter.getConversionService(), "derived");
		this.tree = planner.parseDerivedQuery();
		this.returnShape = MeilisearchQueryReturnShape.resolve(tree, method, metadata);
		validateSpecialParameters();
		this.finderExecution = new MeilisearchFinderExecution(method, queryMethod, domainType, operations, returnShape,
				tree.getSort(), planner, "derived");
	}

	@Override
	public @Nullable Object execute(Object[] values) {

		ParametersParameterAccessor accessor = new ParametersParameterAccessor(queryMethod.getParameters(), values);
		List<String> filters = planner.createFilters(tree, accessor);
		return switch (returnShape) {
			case COUNT -> executeCount(filters);
			case EXISTS -> !operations
				.search(finderExecution.createQuery("", filters, Sort.unsorted(), PageRequest.of(0, 1)), domainType)
				.getSearchHits()
				.isEmpty();
			case DELETE_COUNT, DELETE_VOID -> executeDelete(filters);
			case ENTITY, OPTIONAL, PAGE, LIST, ITERABLE -> finderExecution.execute("", filters, accessor);
		};
	}

	private long executeCount(List<String> filters) {
		if (filters.isEmpty()) {
			return operations.count(domainType);
		}
		return operations.count(domainType, String.join(" AND ", filters));
	}

	private @Nullable Object executeDelete(List<String> filters) {
		if (filters.isEmpty()) {
			throw new IllegalArgumentException(
					"Cannot delete without an effective filter for derived query " + method.toGenericString());
		}
		long deleted = operations.deleteByFilter(domainType, String.join(" AND ", filters));
		return returnShape == MeilisearchQueryReturnShape.DELETE_VOID ? null : deleted;
	}

	@Override
	public QueryMethod getQueryMethod() {
		return queryMethod;
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
		if (isProjection() && hasProjectionSortOrPageable()) {
			throw planner.unsupportedOperator("Sort/Pageable for a count, exists, or delete method");
		}
		if ((returnShape == MeilisearchQueryReturnShape.ENTITY || returnShape == MeilisearchQueryReturnShape.OPTIONAL)
				&& parameters.hasPageableParameter()) {
			throw planner.unsupportedOperator("Pageable for a single-result method");
		}
	}

	private boolean hasProjectionSortOrPageable() {
		var parameters = queryMethod.getParameters();
		return tree.getSort().isSorted() || parameters.hasSortParameter() || parameters.hasPageableParameter();
	}

	private boolean isProjection() {
		return switch (returnShape) {
			case COUNT, EXISTS, DELETE_COUNT, DELETE_VOID -> true;
			default -> false;
		};
	}

}
