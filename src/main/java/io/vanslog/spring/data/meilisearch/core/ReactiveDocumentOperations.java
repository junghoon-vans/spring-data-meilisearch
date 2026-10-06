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
package io.vanslog.spring.data.meilisearch.core;

import org.reactivestreams.Publisher;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Cold, non-blocking document operations. Writes emit or complete only after the corresponding server task succeeds.
 * Cancelling a subscription does not cancel an accepted server task. Collection operations use bounded batches/pages,
 * not an unbounded whole-result buffer.
 *
 * @author Junghoon Ban
 */
public interface ReactiveDocumentOperations {

	/** Save an entity and emit it after successful task completion. */
	<T> Mono<T> save(T entity);

	/** Save bounded batches sequentially, emitting each batch only after its task succeeds. */
	<T> Flux<T> saveAll(Publisher<T> entities);

	/** Fetch a document by ID; complete empty if the document is missing. */
	<T> Mono<T> get(String documentId, Class<T> clazz);

	/** Fetch bounded ID batches, preserving requested order and duplicates and omitting missing IDs. */
	<T> Flux<T> multiGet(Class<T> clazz, Publisher<String> documentIds);

	/** Fetch document pages on demand; traversal is not a snapshot under concurrent writes. */
	<T> Flux<T> findAll(Class<T> clazz);

	/** Fetch sorted document pages. Sorting requires Meilisearch 1.16 or later. */
	<T> Flux<T> findAll(Class<T> clazz, Sort sort);

	/** Check for a document by ID without turning unrelated server errors into absence. */
	Mono<Boolean> exists(String documentId, Class<?> clazz);

	/** Count documents without retrieving their contents. */
	Mono<Long> count(Class<?> clazz);

	/** Count documents matching the supplied filter. */
	Mono<Long> count(Class<?> clazz, String filter);

	/** Delete a document and complete only after its task succeeds. */
	Mono<Void> delete(String documentId, Class<?> clazz);

	/** Delete the mapped entity by ID and await successful task completion. */
	<T> Mono<Void> delete(T entity);

	/** Delete bounded ID batches sequentially, awaiting each task. */
	Mono<Void> deleteAllById(Publisher<String> documentIds, Class<?> clazz);

	/** Delete all documents in the mapped index and await successful task completion. */
	Mono<Void> deleteAll(Class<?> clazz);

	/** Delete matching documents and emit the completed task's actual deleted-document count. */
	Mono<Long> deleteByFilter(Class<?> clazz, String filter);
}
