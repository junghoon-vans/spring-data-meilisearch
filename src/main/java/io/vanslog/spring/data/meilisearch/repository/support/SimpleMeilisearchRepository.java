/*
 * Copyright 2023 the original author or authors.
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
import io.vanslog.spring.data.meilisearch.repository.MeilisearchRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.core.EntityInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Meilisearch specific repository implementation.
 *
 * @param <T>
 * @param <ID>
 * @author Junghoon Ban
 * @see MeilisearchRepository
 */
public class SimpleMeilisearchRepository<T, ID> implements MeilisearchRepository<T, ID> {

	private final MeilisearchOperations meilisearchOperations;
	private final Class<T> entityType;

	public SimpleMeilisearchRepository(EntityInformation<T, ID> entityInformation,
			MeilisearchOperations meilisearchOperations) {
		this.meilisearchOperations = meilisearchOperations;

		Assert.notNull(entityInformation, "EntityInformation must not be null!");
		this.entityType = entityInformation.getJavaType();
	}

	@Override
	public <S extends T> S save(S entity) {
		return meilisearchOperations.save(entity);
	}

	@Override
	public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {
		List<S> entityList = new ArrayList<>();
		entities.forEach(entityList::add);
		return meilisearchOperations.save(entityList);
	}

	@Override
	public Optional<T> findById(ID id) {
		return Optional.ofNullable(meilisearchOperations.get(stringIdRepresentation(id), entityType));
	}

	@Override
	public boolean existsById(ID id) {
		return meilisearchOperations.exists(stringIdRepresentation(id), entityType);
	}

	@Override
	public Iterable<T> findAll() {
		return meilisearchOperations.multiGet(entityType);
	}

	@Override
	public Iterable<T> findAllById(Iterable<ID> ids) {
		List<String> idList = new ArrayList<>();
		ids.forEach(id -> idList.add(stringIdRepresentation(id)));
		return meilisearchOperations.multiGet(entityType, idList);
	}

	@Override
	public long count() {
		return meilisearchOperations.count(entityType);
	}

	@Override
	public void deleteById(ID id) {
		meilisearchOperations.delete(stringIdRepresentation(id), entityType);
	}

	@Override
	public void delete(T entity) {
		meilisearchOperations.delete(entity);
	}

	@Override
	public void deleteAllById(Iterable<? extends ID> ids) {
		List<String> idList = new ArrayList<>();
		ids.forEach(id -> idList.add(stringIdRepresentation(id)));
		meilisearchOperations.delete(entityType, idList);
	}

	@Override
	public void deleteAll(Iterable<? extends T> entities) {
		List<T> entityList = new ArrayList<>();
		entities.forEach(entityList::add);
		meilisearchOperations.delete(entityList);
	}

	@Override
	public void deleteAll() {
		meilisearchOperations.deleteAll(entityType);
	}

	protected @Nullable String stringIdRepresentation(@Nullable ID id) {
		return id != null ? meilisearchOperations.getMeilisearchConverter().convertId(id) : null;
	}
}
