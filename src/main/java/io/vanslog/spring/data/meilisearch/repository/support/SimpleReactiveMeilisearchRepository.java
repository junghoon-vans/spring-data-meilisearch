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

import org.reactivestreams.Publisher;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.util.Assert;

import io.vanslog.spring.data.meilisearch.core.ReactiveMeilisearchOperations;
import io.vanslog.spring.data.meilisearch.repository.ReactiveMeilisearchRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive CRUD implementation backed by cold {@link ReactiveMeilisearchOperations}
 * publishers.
 *
 * @param <T> the document type
 * @param <ID> the document identifier type
 * @author Junghoon Ban
 * @see ReactiveMeilisearchRepository
 */
public class SimpleReactiveMeilisearchRepository<T, ID> implements ReactiveMeilisearchRepository<T, ID> {

	private final EntityInformation<T, ID> entityInformation;

	private final ReactiveMeilisearchOperations operations;

	private final Class<T> entityType;

	private final Mono<Void> initialization;

	public SimpleReactiveMeilisearchRepository(EntityInformation<T, ID> entityInformation,
			ReactiveMeilisearchOperations operations) {
		this(entityInformation, operations, createInitialization(entityInformation, operations));
	}

	public SimpleReactiveMeilisearchRepository(EntityInformation<T, ID> entityInformation,
			ReactiveMeilisearchOperations operations, Mono<Void> initialization) {

		Assert.notNull(entityInformation, "EntityInformation must not be null");
		Assert.notNull(operations, "ReactiveMeilisearchOperations must not be null");
		Assert.notNull(initialization, "Initialization publisher must not be null");

		this.entityInformation = entityInformation;
		this.operations = operations;
		this.entityType = entityInformation.getJavaType();
		this.initialization = initialization;
	}

	@Override
	public <S extends T> Mono<S> save(S entity) {
		return Mono.defer(() -> {
			Assert.notNull(entity, "Entity must not be null");
			return initialization.then(Mono.defer(() -> operations.save(entity)));
		});
	}

	@Override
	public <S extends T> Flux<S> saveAll(Iterable<S> entities) {
		return Flux.defer(() -> {
			Assert.notNull(entities, "Entities must not be null");
			return saveAll(Flux.fromIterable(entities));
		});
	}

	@Override
	public <S extends T> Flux<S> saveAll(Publisher<S> entityStream) {
		return Flux.defer(() -> {
			Assert.notNull(entityStream, "Entity publisher must not be null");
			return initialization.thenMany(Flux.defer(() -> operations.saveAll(entityStream)));
		});
	}

	@Override
	public Mono<T> findById(ID id) {
		return Mono.defer(() -> {
			Assert.notNull(id, "Id must not be null");
			return initialization.then(Mono.defer(() -> operations.get(stringIdRepresentation(id), entityType)));
		});
	}

	@Override
	public Mono<T> findById(Publisher<ID> id) {
		return Mono.defer(() -> {
			Assert.notNull(id, "Id publisher must not be null");
			return Mono.from(id).flatMap(this::findById);
		});
	}

	@Override
	public Mono<Boolean> existsById(ID id) {
		return Mono.defer(() -> {
			Assert.notNull(id, "Id must not be null");
			return initialization.then(Mono.defer(() -> operations.exists(stringIdRepresentation(id), entityType)));
		});
	}

	@Override
	public Mono<Boolean> existsById(Publisher<ID> id) {
		return Mono.defer(() -> {
			Assert.notNull(id, "Id publisher must not be null");
			return Mono.from(id).flatMap(this::existsById);
		});
	}

	@Override
	public Flux<T> findAll() {
		return Flux.defer(() -> initialization.thenMany(Flux.defer(() -> operations.findAll(entityType))));
	}

	@Override
	public Flux<T> findAll(Sort sort) {
		return Flux.defer(() -> {
			Assert.notNull(sort, "Sort must not be null");
			return initialization.thenMany(Flux.defer(() -> operations.findAll(entityType, sort)));
		});
	}

	@Override
	public Flux<T> findAllById(Iterable<ID> ids) {
		return Flux.defer(() -> {
			Assert.notNull(ids, "Ids must not be null");
			return findAllById(Flux.fromIterable(ids));
		});
	}

	@Override
	public Flux<T> findAllById(Publisher<ID> idStream) {
		return Flux.defer(() -> {
			Assert.notNull(idStream, "Id publisher must not be null");
			return initialization.thenMany(Flux
				.defer(() -> operations.multiGet(entityType, Flux.from(idStream).map(this::stringIdRepresentation))));
		});
	}

	@Override
	public Mono<Long> count() {
		return Mono.defer(() -> initialization.then(Mono.defer(() -> operations.count(entityType))));
	}

	@Override
	public Mono<Void> deleteById(ID id) {
		return Mono.defer(() -> {
			Assert.notNull(id, "Id must not be null");
			return initialization.then(Mono.defer(() -> operations.delete(stringIdRepresentation(id), entityType)));
		});
	}

	@Override
	public Mono<Void> deleteById(Publisher<ID> id) {
		return Mono.defer(() -> {
			Assert.notNull(id, "Id publisher must not be null");
			return Mono.from(id).flatMap(this::deleteById);
		});
	}

	@Override
	public Mono<Void> delete(T entity) {
		return Mono.defer(() -> {
			Assert.notNull(entity, "Entity must not be null");
			return initialization
				.then(Mono.defer(() -> operations.delete(stringIdRepresentation(requiredId(entity)), entityType)));
		});
	}

	@Override
	public Mono<Void> deleteAllById(Iterable<? extends ID> ids) {
		return Mono.defer(() -> {
			Assert.notNull(ids, "Ids must not be null");
			return initialization.then(Mono.defer(() -> operations
				.deleteAllById(Flux.fromIterable(ids).map(this::stringIdRepresentation), entityType)));
		});
	}

	@Override
	public Mono<Void> deleteAll(Iterable<? extends T> entities) {
		return Mono.defer(() -> {
			Assert.notNull(entities, "Entities must not be null");
			return deleteAll(Flux.fromIterable(entities));
		});
	}

	@Override
	public Mono<Void> deleteAll(Publisher<? extends T> entityStream) {
		return Mono.defer(() -> {
			Assert.notNull(entityStream, "Entity publisher must not be null");
			return initialization.then(Mono.defer(() -> operations.deleteAllById(
					Flux.from(entityStream).map(entity -> stringIdRepresentation(requiredId(entity))), entityType)));
		});
	}

	@Override
	public Mono<Void> deleteAll() {
		return Mono.defer(() -> initialization.then(Mono.defer(() -> operations.deleteAll(entityType))));
	}

	private ID requiredId(T entity) {
		return entityInformation.getRequiredId(entity);
	}

	private String stringIdRepresentation(ID id) {
		Assert.notNull(id, "Id must not be null");
		return operations.getMeilisearchConverter().convertId(id);
	}

	private static <T, ID> Mono<Void> createInitialization(EntityInformation<T, ID> entityInformation,
			ReactiveMeilisearchOperations operations) {
		Assert.notNull(entityInformation, "EntityInformation must not be null");
		Assert.notNull(operations, "ReactiveMeilisearchOperations must not be null");
		return new ReactiveMeilisearchIndexInitialization(entityInformation.getJavaType(), operations).initialize();
	}

}
