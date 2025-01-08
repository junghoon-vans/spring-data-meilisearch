/*
 * Copyright 2023-2025 the original author or authors.
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

import java.io.Serializable;

import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.beans.factory.FactoryBean} to create
 * {@link org.springframework.data.repository.Repository} instances.
 *
 * @param <T> The type of the repository interface.
 * @param <S> The type of the entity managed by the repository.
 * @param <ID> The type of the identifier of the entity.
 * @author Junghoon Ban
 */
public class MeilisearchRepositoryFactoryBean<T extends Repository<S, ID>, S, ID extends Serializable>
		extends RepositoryFactoryBeanSupport<T, S, ID> {

	@Nullable private MeilisearchOperations meilisearchOperations;

	/**
	 * Creates a new {@link RepositoryFactoryBeanSupport} for the given repository interface.
	 *
	 * @param repositoryInterface must not be {@literal null}.
	 */
	protected MeilisearchRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
		super(repositoryInterface);
	}

	/**
	 * Configures the {@link MeilisearchOperations} to be used to create the repository.
	 * 
	 * @param meilisearchOperations operations to be used
	 */
	public void setMeilisearchOperations(MeilisearchOperations meilisearchOperations) {
		Assert.notNull(meilisearchOperations, "MeilisearchOperations must not be null!");

		setMappingContext(meilisearchOperations.getMeilisearchConverter().getMappingContext());
		this.meilisearchOperations = meilisearchOperations;
	}

	@Override
	protected RepositoryFactorySupport createRepositoryFactory() {
		Assert.notNull(meilisearchOperations, "MeilisearchOperations must be configured!");
		return new MeilisearchRepositoryFactory(meilisearchOperations);
	}
}
