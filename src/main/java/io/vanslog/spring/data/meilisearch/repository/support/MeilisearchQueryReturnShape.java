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
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.parser.PartTree;

/**
 * Validates the supported result shapes of repository query methods.
 *
 * @author Junghoon Ban
 */
enum MeilisearchQueryReturnShape {

	ENTITY, OPTIONAL, LIST, ITERABLE, PAGE, COUNT, EXISTS, DELETE_COUNT, DELETE_VOID;

	static MeilisearchQueryReturnShape resolve(PartTree tree, Method method, QueryMethod queryMethod,
			Class<?> domainType) {
		Class<?> returnType = method.getReturnType();
		if (tree.isCountProjection()) {
			return resolveCount(returnType, method);
		}
		if (tree.isExistsProjection()) {
			return resolveExists(returnType, method);
		}
		if (tree.isDelete()) {
			return resolveDelete(returnType, method);
		}
		return resolveFinder(returnType, method, queryMethod, domainType, "derived query");
	}

	static MeilisearchQueryReturnShape resolveFinder(Method method, QueryMethod queryMethod, Class<?> domainType) {
		return resolveFinder(method.getReturnType(), method, queryMethod, domainType, "repository query");
	}

	private static MeilisearchQueryReturnShape resolveCount(Class<?> returnType, Method method) {
		if (returnType != long.class && returnType != Long.class) {
			throw unsupportedReturnType(returnType, method, "derived query");
		}
		return COUNT;
	}

	private static MeilisearchQueryReturnShape resolveExists(Class<?> returnType, Method method) {
		if (returnType != boolean.class && returnType != Boolean.class) {
			throw unsupportedReturnType(returnType, method, "derived query");
		}
		return EXISTS;
	}

	private static MeilisearchQueryReturnShape resolveDelete(Class<?> returnType, Method method) {
		if (returnType == void.class) {
			return DELETE_VOID;
		}
		if (returnType == long.class || returnType == Long.class) {
			return DELETE_COUNT;
		}
		throw unsupportedReturnType(returnType, method, "derived query");
	}

	private static MeilisearchQueryReturnShape resolveFinder(Class<?> returnType, Method method, QueryMethod queryMethod,
			Class<?> domainType, String queryKind) {
		if (!queryMethod.isQueryForEntity()) {
			throw unsupportedReturnType(returnType, method, queryKind);
		}
		if (returnType == Page.class) {
			return PAGE;
		}
		if (returnType == Optional.class) {
			return OPTIONAL;
		}
		if (returnType == List.class) {
			return LIST;
		}
		if (returnType == Iterable.class) {
			return ITERABLE;
		}
		if (returnType.isAssignableFrom(domainType)) {
			return ENTITY;
		}
		throw unsupportedReturnType(returnType, method, queryKind);
	}

	private static IllegalArgumentException unsupportedReturnType(Class<?> returnType, Method method, String queryKind) {
		return new IllegalArgumentException(
				"Unsupported " + queryKind + " return type " + returnType.getName() + " in method " + method.toGenericString());
	}
}
