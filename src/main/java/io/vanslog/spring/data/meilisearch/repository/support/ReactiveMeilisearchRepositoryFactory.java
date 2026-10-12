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
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.KotlinDetector;
import org.springframework.core.ResolvableType;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.ReactiveRepositoryFactorySupport;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.util.ReactiveWrappers;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import io.vanslog.spring.data.meilisearch.core.ReactiveMeilisearchOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Factory for repositories executing exclusively through reactive Meilisearch operations.
 *
 * @author Junghoon Ban
 */
public class ReactiveMeilisearchRepositoryFactory extends ReactiveRepositoryFactorySupport {

	private static final String COROUTINE_REPOSITORY_NAME = "io.vanslog.spring.data.meilisearch.repository.CoroutineMeilisearchRepository";

	private static final String COROUTINE_CRUD_REPOSITORY_NAME = "org.springframework.data.repository.kotlin.CoroutineCrudRepository";

	private static final String COROUTINE_SORTING_REPOSITORY_NAME = "org.springframework.data.repository.kotlin.CoroutineSortingRepository";

	private static final String KOTLIN_FLOW_NAME = "kotlinx.coroutines.flow.Flow";
	private final ReactiveMeilisearchOperations operations;

	private final MeilisearchEntityInformationCreator entityInformationCreator;

	private final Map<Class<?>, Mono<Void>> initializations = new ConcurrentHashMap<>();

	public ReactiveMeilisearchRepositoryFactory(ReactiveMeilisearchOperations operations) {
		Assert.notNull(operations, "ReactiveMeilisearchOperations must not be null");
		this.operations = operations;
		this.entityInformationCreator = new MeilisearchEntityInformationCreatorImpl(
				operations.getMeilisearchConverter().getMappingContext());
	}

	@Override
	public <T, ID> EntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
		return entityInformationCreator.getEntityInformation(domainClass);
	}

	@Override
	protected Object getTargetRepository(RepositoryInformation metadata) {
		return getTargetRepositoryViaReflection(metadata, getEntityInformation(metadata.getDomainType()), operations,
				initialization(metadata.getDomainType()));
	}

	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
		return SimpleReactiveMeilisearchRepository.class;
	}

	@Override
	protected Optional<QueryLookupStrategy> getQueryLookupStrategy(@Nullable QueryLookupStrategy.Key key,
			QueryMethodEvaluationContextProvider evaluationContextProvider) {
		return Optional.of(new ReactiveMeilisearchQueryLookupStrategy(key, operations, this::initialization));
	}

	private Mono<Void> initialization(Class<?> domainType) {
		return initializations.computeIfAbsent(domainType,
				type -> new ReactiveMeilisearchIndexInitialization(type, operations).initialize());
	}

	@Override
	protected void validate(RepositoryMetadata metadata) {
		super.validate(metadata);

		if (isCoroutineRepository(metadata.getRepositoryInterface())) {
			validateCoroutineRepository(metadata.getRepositoryInterface());
			return;
		}

		for (Method method : metadata.getRepositoryInterface().getMethods()) {
			if (!Modifier.isAbstract(method.getModifiers()))
				continue;
			Class<?> result = ResolvableType.forMethodReturnType(method, metadata.getRepositoryInterface()).resolve();
			if (result != Mono.class && result != Flux.class) {
				throw new IllegalArgumentException("Reactive Meilisearch repositories require Mono/Flux return types; "
						+ "mixed blocking/reactive contracts are not supported: " + method.toGenericString());
			}
		}
	}

	private void validateCoroutineRepository(Class<?> repositoryInterface) {
		if (!KotlinDetector.isKotlinReflectPresent()
				|| !ReactiveWrappers.isAvailable(ReactiveWrappers.ReactiveLibrary.KOTLIN_COROUTINES)) {
			throw new IllegalStateException(
					"Kotlin coroutine repositories require kotlin-reflect and " + "kotlinx-coroutines-reactor on the classpath");
		}

		for (Method method : repositoryInterface.getMethods()) {
			if (!Modifier.isAbstract(method.getModifiers()) || isCoroutineBaseMethod(method))
				continue;

			if (!CoroutineQuerySupport.isSuspend(method) && !isCoroutineFlow(method.getReturnType())) {
				throw new IllegalArgumentException("Coroutine Meilisearch repositories only support suspend and Flow "
						+ "methods; mixed blocking/reactive contracts are not supported: " + method.toGenericString());
			}
		}
	}

	private boolean isCoroutineRepository(Class<?> repositoryInterface) {
		if (COROUTINE_REPOSITORY_NAME.equals(repositoryInterface.getName()))
			return true;

		for (Class<?> parent : repositoryInterface.getInterfaces()) {
			if (isCoroutineRepository(parent))
				return true;
		}
		return false;
	}

	private boolean isCoroutineBaseMethod(Method method) {
		String declaringType = method.getDeclaringClass().getName();
		return COROUTINE_CRUD_REPOSITORY_NAME.equals(declaringType)
				|| COROUTINE_SORTING_REPOSITORY_NAME.equals(declaringType);
	}

	private boolean isCoroutineFlow(Class<?> type) {
		return KOTLIN_FLOW_NAME.equals(type.getName());
	}
}
