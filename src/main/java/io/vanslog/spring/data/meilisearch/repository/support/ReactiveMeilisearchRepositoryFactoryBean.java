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

import java.io.Serializable;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import io.vanslog.spring.data.meilisearch.core.ReactiveMeilisearchOperations;

/**
 * Factory bean for reactive Meilisearch repositories.
 * 
 * @author Junghoon Ban
 */
public class ReactiveMeilisearchRepositoryFactoryBean<T extends Repository<S, ID>, S, ID extends Serializable>
		extends RepositoryFactoryBeanSupport<T, S, ID> {

	@Nullable
	private ReactiveMeilisearchOperations reactiveMeilisearchOperations;

	public ReactiveMeilisearchRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
		super(repositoryInterface);
	}

	public void setReactiveMeilisearchOperations(ReactiveMeilisearchOperations operations) {
		Assert.notNull(operations, "ReactiveMeilisearchOperations must not be null");
		setMappingContext(operations.getMeilisearchConverter().getMappingContext());
		this.reactiveMeilisearchOperations = operations;
	}

	@Override
	protected RepositoryFactorySupport createRepositoryFactory() {
		Assert.notNull(reactiveMeilisearchOperations, "ReactiveMeilisearchOperations must be configured");
		return new ReactiveMeilisearchRepositoryFactory(reactiveMeilisearchOperations);
	}

}
