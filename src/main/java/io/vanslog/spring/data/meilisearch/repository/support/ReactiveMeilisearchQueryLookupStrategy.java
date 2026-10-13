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
import java.util.Objects;
import java.util.function.Function;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.annotation.QueryAnnotation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.lang.Nullable;

import io.vanslog.spring.data.meilisearch.core.ReactiveMeilisearchOperations;
import io.vanslog.spring.data.meilisearch.repository.Query;
import reactor.core.publisher.Mono;

/**
 * Selects declared, named, or derived reactive query execution using the configured Spring Data lookup strategy.
 *
 * @author Junghoon Ban
 */
final class ReactiveMeilisearchQueryLookupStrategy implements QueryLookupStrategy {

	@Nullable private final QueryLookupStrategy.Key key;

	private final ReactiveMeilisearchOperations operations;

	private final Function<Class<?>, Mono<Void>> initialization;

	ReactiveMeilisearchQueryLookupStrategy(@Nullable QueryLookupStrategy.Key key,
			ReactiveMeilisearchOperations operations, Function<Class<?>, Mono<Void>> initialization) {
		this.key = key;
		this.operations = Objects.requireNonNull(operations, "ReactiveMeilisearchOperations must not be null");
		this.initialization = Objects.requireNonNull(initialization, "Initialization function must not be null");
	}

	@Override
	public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata,
			org.springframework.data.projection.ProjectionFactory factory, NamedQueries namedQueries) {

		if (key == QueryLookupStrategy.Key.CREATE) {
			return create(method, metadata, factory, false, "", "");
		}

		Query annotation = AnnotatedElementUtils.findMergedAnnotation(method, Query.class);
		if (annotation == null && AnnotatedElementUtils.hasAnnotation(method, QueryAnnotation.class)) {
			throw new IllegalArgumentException(
					"Unsupported query annotation on Meilisearch repository method " + method.toGenericString());
		}
		if (annotation != null) {
			return create(method, metadata, factory, true, annotation.filter(), annotation.q());
		}

		QueryMethod queryMethod = new ReactiveMeilisearchQueryMethod(method, metadata, factory);
		String name = queryMethod.getNamedQueryName();
		boolean hasFilter = namedQueries.hasQuery(name);
		boolean hasText = namedQueries.hasQuery(name + ".q");
		if (hasFilter || hasText) {
			return create(method, metadata, factory, true, hasFilter ? namedQueries.getQuery(name) : "",
					hasText ? namedQueries.getQuery(name + ".q") : "");
		}

		if (key == QueryLookupStrategy.Key.USE_DECLARED_QUERY) {
			throw new IllegalStateException(
					"No declared Meilisearch query for repository method " + method.toGenericString());
		}
		return create(method, metadata, factory, false, "", "");
	}

	private ReactiveMeilisearchQuery create(Method method, RepositoryMetadata metadata,
			org.springframework.data.projection.ProjectionFactory factory, boolean declared, String filter, String q) {
		return new ReactiveMeilisearchQuery(method, metadata, factory, operations,
				initialization.apply(metadata.getDomainType()), declared, filter, q);
	}

}
