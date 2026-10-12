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
package io.vanslog.spring.data.meilisearch.core

import org.reactivestreams.Publisher
import org.springframework.data.domain.Sort
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

inline fun <reified T : Any> ReactiveDocumentOperations.get(documentId: String): Mono<T> =
    get(documentId, T::class.java)

inline fun <reified T : Any> ReactiveDocumentOperations.multiGet(documentIds: Publisher<String>): Flux<T> =
    multiGet(T::class.java, documentIds)

inline fun <reified T : Any> ReactiveDocumentOperations.findAll(): Flux<T> = findAll(T::class.java)

inline fun <reified T : Any> ReactiveDocumentOperations.findAll(sort: Sort): Flux<T> = findAll(T::class.java, sort)

inline fun <reified T : Any> ReactiveDocumentOperations.exists(documentId: String): Mono<Boolean> =
    exists(documentId, T::class.java)

inline fun <reified T : Any> ReactiveDocumentOperations.count(): Mono<Long> = count(T::class.java)

inline fun <reified T : Any> ReactiveDocumentOperations.count(filter: String): Mono<Long> =
    count(T::class.java, filter)

inline fun <reified T : Any> ReactiveDocumentOperations.delete(documentId: String): Mono<Void> =
    delete(documentId, T::class.java)

inline fun <reified T : Any> ReactiveDocumentOperations.deleteAllById(documentIds: Publisher<String>): Mono<Void> =
    deleteAllById(documentIds, T::class.java)

inline fun <reified T : Any> ReactiveDocumentOperations.deleteAll(): Mono<Void> = deleteAll(T::class.java)

inline fun <reified T : Any> ReactiveDocumentOperations.deleteByFilter(filter: String): Mono<Long> =
    deleteByFilter(T::class.java, filter)
