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

import io.vanslog.spring.data.meilisearch.core.MeilisearchOperations;

import java.lang.reflect.Method;
import java.util.Optional;

import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.lang.Nullable;

/**
 * Factory to create {@link SimpleMeilisearchRepository} instances.
 *
 * @author Junghoon Ban
 */
public class MeilisearchRepositoryFactory extends RepositoryFactorySupport {

	private static final String QUERY_METHODS_NOT_SUPPORTED =
			"Meilisearch repository query methods are not supported yet";

	private static final String QUERY_METHODS_GUIDANCE =
			"Use MeilisearchOperations for custom searches. Derived, declared, and named query support is tracked by issue #78.";

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
			QueryMethodEvaluationContextProvider evaluationContextProvider)
	{
		return Optional.of(new UnsupportedMeilisearchQueryLookupStrategy());
	}

	private static class UnsupportedMeilisearchQueryLookupStrategy implements QueryLookupStrategy
	{

		@Override
		public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, ProjectionFactory factory,
				NamedQueries namedQueries)
		{

			throw new IllegalStateException(QUERY_METHODS_NOT_SUPPORTED + ": " + method.getName()
					+ ". " + QUERY_METHODS_GUIDANCE);
		}
	}
}
