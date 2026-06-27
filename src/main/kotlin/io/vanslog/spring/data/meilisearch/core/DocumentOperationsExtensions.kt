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

inline fun <reified T : Any> DocumentOperations.get(documentId: String): T? = get(documentId, T::class.java)

inline fun <reified T : Any> DocumentOperations.multiGet(): List<T> = multiGet(T::class.java)

inline fun <reified T : Any> DocumentOperations.multiGet(offset: Int, limit: Int): List<T> =
    multiGet(T::class.java, offset, limit)

inline fun <reified T : Any> DocumentOperations.multiGet(documentIds: List<String>): List<T> =
    multiGet(T::class.java, documentIds)

inline fun <reified T : Any> DocumentOperations.multiGet(
    documentIds: List<String>,
    offset: Int,
    limit: Int,
): List<T> = multiGet(T::class.java, documentIds, offset, limit)

inline fun <reified T : Any> DocumentOperations.exists(documentId: String): Boolean = exists(documentId, T::class.java)

inline fun <reified T : Any> DocumentOperations.delete(documentId: String): Boolean = delete(documentId, T::class.java)

inline fun <reified T : Any> DocumentOperations.delete(documentIds: List<String>): Boolean =
    delete(T::class.java, documentIds)

inline fun <reified T : Any> DocumentOperations.deleteAll(): Boolean = deleteAll(T::class.java)
