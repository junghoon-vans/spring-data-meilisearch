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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

import org.springframework.core.KotlinDetector;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.data.util.TypeInformation;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Validates the concrete Reactor wrapper and payload supported by a repository query method.
 *
 * @author Junghoon Ban
 */
enum MeilisearchReactiveQueryReturnShape {

	FLUX, MONO, COUNT, EXISTS, DELETE_COUNT, DELETE_VOID;

	static MeilisearchReactiveQueryReturnShape resolve(PartTree tree, Method method, RepositoryMetadata metadata) {
		if (tree.isCountProjection()) {
			return resolveMonoProjection(method, metadata, Long.class, COUNT);
		}
		if (tree.isExistsProjection()) {
			return resolveMonoProjection(method, metadata, Boolean.class, EXISTS);
		}
		if (tree.isDelete()) {
			if (isCoroutineUnit(method, metadata) || isMonoPayload(method, metadata, Void.class)) {
				return DELETE_VOID;
			}
			return resolveMonoProjection(method, metadata, Long.class, DELETE_COUNT);
		}
		return resolveFinder(method, metadata, "derived query");
	}

	static MeilisearchReactiveQueryReturnShape resolveFinder(Method method, RepositoryMetadata metadata) {
		return resolveFinder(method, metadata, "declared query");
	}

	private static MeilisearchReactiveQueryReturnShape resolveFinder(Method method, RepositoryMetadata metadata,
			String queryKind) {

		Class<?> domainType = metadata.getDomainType();

		if (isCoroutineFlow(method.getReturnType())) {
			if (resolveCoroutineFlowPayloadType(method, metadata, queryKind) != domainType) {
				throw unsupportedReturnType(method, queryKind);
			}
			return FLUX;
		}

		if (isSuspending(method)) {
			if (resolveCoroutineSingleType(method, metadata, queryKind) != domainType) {
				throw unsupportedReturnType(method, queryKind);
			}
			return MONO;
		}

		Class<?> returnType = method.getReturnType();
		Class<?> payloadType = resolveReactivePayloadType(method, metadata, queryKind);
		if (payloadType != domainType) {
			throw unsupportedReturnType(method, queryKind);
		}
		return returnType == Flux.class ? FLUX : MONO;
	}

	private static MeilisearchReactiveQueryReturnShape resolveMonoProjection(Method method, RepositoryMetadata metadata,
			Class<?> expectedPayload, MeilisearchReactiveQueryReturnShape shape) {

		if (isSuspending(method)) {
			if (!matchesLogicalType(resolveCoroutineSingleType(method, metadata, "derived query"), expectedPayload)) {
				throw unsupportedReturnType(method, "derived query");
			}
			return shape;
		}

		if (method.getReturnType() != Mono.class
				|| resolveReactivePayloadType(method, metadata, "derived query") != expectedPayload) {
			throw unsupportedReturnType(method, "derived query");
		}
		return shape;
	}

	private static boolean isMonoPayload(Method method, RepositoryMetadata metadata, Class<?> expectedPayload) {
		if (isCoroutineUnit(method, metadata)) {
			return expectedPayload == Void.class;
		}
		return method.getReturnType() == Mono.class
				&& resolveReactivePayloadType(method, metadata, "derived query") == expectedPayload;
	}

	private static Class<?> resolveCoroutineSingleType(Method method, RepositoryMetadata metadata, String queryKind) {
		if (CoroutineQuerySupport.hasUnresolvedMethodTypeParameter(method)) {
			throw unsupportedReturnType(method, queryKind);
		}
		return metadata.getReturnType(method).getType();
	}

	private static Class<?> resolveCoroutineFlowPayloadType(Method method, RepositoryMetadata metadata,
			String queryKind) {
		if (CoroutineQuerySupport.hasUnresolvedMethodTypeParameter(method)) {
			throw unsupportedReturnType(method, queryKind);
		}

		TypeInformation<?> returnInformation = metadata.getReturnType(method);
		if (!isCoroutineFlow(returnInformation.getType()) || returnInformation.getTypeArguments().size() != 1) {
			throw unsupportedReturnType(method, queryKind);
		}

		return returnInformation.getTypeArguments().get(0).getType();
	}

	private static Class<?> resolveReactivePayloadType(Method method, RepositoryMetadata metadata, String queryKind) {

		Class<?> returnType = method.getReturnType();
		if (returnType != Mono.class && returnType != Flux.class) {
			throw unsupportedReturnType(method, queryKind);
		}

		Type declaration = method.getGenericReturnType();
		if (!(declaration instanceof ParameterizedType parameterizedType)
				|| parameterizedType.getActualTypeArguments().length != 1) {
			throw unsupportedReturnType(method, queryKind);
		}
		Type declaredPayload = parameterizedType.getActualTypeArguments()[0];
		if (declaredPayload instanceof WildcardType
				|| declaredPayload instanceof TypeVariable<?> variable && variable.getGenericDeclaration() instanceof Method) {
			throw unsupportedReturnType(method, queryKind);
		}

		TypeInformation<?> returnInformation = metadata.getReturnType(method);
		if (returnInformation.getType() != returnType || returnInformation.getTypeArguments().size() != 1) {
			throw unsupportedReturnType(method, queryKind);
		}
		return returnInformation.getTypeArguments().get(0).getType();
	}

	private static boolean isCoroutineUnit(Method method, RepositoryMetadata metadata) {
		return isSuspending(method) && !CoroutineQuerySupport.hasUnresolvedMethodTypeParameter(method)
				&& "kotlin.Unit".equals(metadata.getReturnType(method).getType().getName());
	}

	private static boolean isSuspending(Method method) {
		return KotlinDetector.isKotlinReflectPresent() && CoroutineQuerySupport.isSuspend(method);
	}

	private static boolean isCoroutineFlow(Class<?> type) {
		return "kotlinx.coroutines.flow.Flow".equals(type.getName());
	}

	private static boolean matchesLogicalType(Class<?> actualType, Class<?> expectedType) {
		return wrapPrimitive(actualType) == wrapPrimitive(expectedType);
	}

	private static Class<?> wrapPrimitive(Class<?> type) {
		if (!type.isPrimitive()) {
			return type;
		}
		if (type == long.class) {
			return Long.class;
		}
		if (type == boolean.class) {
			return Boolean.class;
		}
		return type;
	}

	private static IllegalArgumentException unsupportedReturnType(Method method, String queryKind) {
		return new IllegalArgumentException("Unsupported " + queryKind + " return type " + method.getReturnType().getName()
				+ " in method " + method.toGenericString());
	}

}
