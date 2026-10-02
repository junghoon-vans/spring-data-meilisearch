/*
 * Copyright 2023-2026 the original author or authors.
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

import java.util.Optional;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.annotation.QueryAnnotation;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.lang.Nullable;

import io.vanslog.spring.data.meilisearch.core.MeilisearchOperations;

/**
 * Factory to create {@link SimpleMeilisearchRepository} instances.
 *
 * @author Junghoon Ban
 */
public class MeilisearchRepositoryFactory extends RepositoryFactorySupport {

	private static final String DECLARED_QUERIES_NOT_SUPPORTED = "Declared and named Meilisearch repository queries are not supported";

	private final MeilisearchOperations meilisearchOperations;
	private final MeilisearchEntityInformationCreator entityInformationCreator;

	public MeilisearchRepositoryFactory(MeilisearchOperations meilisearchOperations) {
		this.meilisearchOperations = meilisearchOperations;
		this.entityInformationCreator = new MeilisearchEntityInformationCreatorImpl(
				meilisearchOperations.getMeilisearchConverter().getMappingContext());
	}

	@Override
	public <T, ID> EntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
		return entityInformationCreator.getEntityInformation(domainClass);
	}

	@Override
	protected Object getTargetRepository(RepositoryInformation metadata) {
		return getTargetRepositoryViaReflection(metadata, getEntityInformation(metadata.getDomainType()),
				meilisearchOperations);
	}

	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
		return SimpleMeilisearchRepository.class;
	}

	@Override
	protected Optional<QueryLookupStrategy> getQueryLookupStrategy(@Nullable QueryLookupStrategy.Key key,
			QueryMethodEvaluationContextProvider evaluationContextProvider) {
		return Optional.of((method, metadata, factory, namedQueries) -> {
			QueryMethod queryMethod = new QueryMethod(method, metadata, factory);
			if (AnnotatedElementUtils.hasAnnotation(method, QueryAnnotation.class)
					|| namedQueries.hasQuery(queryMethod.getNamedQueryName())) {
				throw new IllegalStateException(DECLARED_QUERIES_NOT_SUPPORTED + ": " + method.getName()
						+ ". Use MeilisearchOperations for explicit searches.");
			}
			if (key == QueryLookupStrategy.Key.USE_DECLARED_QUERY) {
				throw new IllegalStateException("Derived Meilisearch repository query " + method.getName()
						+ " requires CREATE or CREATE_IF_NOT_FOUND query lookup.");
			}
			return new MeilisearchPartTreeQuery(method, metadata, factory, meilisearchOperations);
		});
	}
}
