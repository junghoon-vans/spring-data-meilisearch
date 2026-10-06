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

import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.lang.Nullable;

import io.vanslog.spring.data.meilisearch.core.MeilisearchOperations;
import io.vanslog.spring.data.meilisearch.core.mapping.MeilisearchPersistentEntity;
import io.vanslog.spring.data.meilisearch.core.mapping.MeilisearchPersistentProperty;

/**
 * Executes a declared Meilisearch repository query.
 *
 * @author Junghoon Ban
 */
class MeilisearchStringQuery implements RepositoryQuery {

	private final Method method;
	private final QueryMethod queryMethod;
	private final MeilisearchDeclaredQueryBinding binding;
	private final MeilisearchFinderExecution finderExecution;

	MeilisearchStringQuery(Method method, RepositoryMetadata metadata, ProjectionFactory projectionFactory,
			MeilisearchOperations operations, String filter, String q) {

		this.method = method;
		this.queryMethod = new QueryMethod(method, metadata, projectionFactory);
		validateUnsupportedSpecialParameters();
		MeilisearchQueryReturnShape returnShape = MeilisearchQueryReturnShape.resolveFinder(method, metadata);
		validateSingleResultParameters(returnShape);
		MappingContext<? extends MeilisearchPersistentEntity<?>, MeilisearchPersistentProperty> mappingContext = operations
				.getMeilisearchConverter().getMappingContext();
		this.finderExecution = new MeilisearchFinderExecution(method, queryMethod, metadata.getDomainType(), operations,
				returnShape, Sort.unsorted(), mappingContext, "declared");
		this.binding = new MeilisearchDeclaredQueryBinding(method, queryMethod, metadata, filter, q);
	}

	@Override
	public @Nullable Object execute(Object[] values) {

		MeilisearchDeclaredQueryBinding.BoundQuery boundQuery = binding.bind(values);
		ParametersParameterAccessor accessor = new ParametersParameterAccessor(queryMethod.getParameters(), values);
		List<String> filters = boundQuery.filter().isEmpty() ? List.of() : List.of(boundQuery.filter());
		return finderExecution.execute(boundQuery.q(), filters, accessor);
	}

	@Override
	public QueryMethod getQueryMethod() {
		return queryMethod;
	}

	private void validateUnsupportedSpecialParameters() {

		var parameters = queryMethod.getParameters();
		if (parameters.hasDynamicProjection()) {
			throw unsupportedOption("dynamic projection");
		}
		if (parameters.hasLimitParameter()) {
			throw unsupportedOption("Limit");
		}
		if (parameters.hasScrollPositionParameter()) {
			throw unsupportedOption("ScrollPosition");
		}
	}

	private void validateSingleResultParameters(MeilisearchQueryReturnShape returnShape) {

		if ((returnShape == MeilisearchQueryReturnShape.ENTITY || returnShape == MeilisearchQueryReturnShape.OPTIONAL)
				&& queryMethod.getParameters().hasPageableParameter()) {
			throw unsupportedOption("Pageable for a single-result method");
		}
	}

	private IllegalArgumentException unsupportedOption(String option) {
		return new IllegalArgumentException(
				"Unsupported declared query option '" + option + "' in method " + method.toGenericString());
	}
}
