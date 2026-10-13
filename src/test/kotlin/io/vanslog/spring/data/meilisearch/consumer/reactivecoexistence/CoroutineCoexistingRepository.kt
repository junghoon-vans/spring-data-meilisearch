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
package io.vanslog.spring.data.meilisearch.consumer.reactivecoexistence

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable

import io.vanslog.spring.data.meilisearch.repository.CoroutineMeilisearchRepository
import io.vanslog.spring.data.meilisearch.repository.Query

interface CoroutineCoexistingRepository : CoroutineMeilisearchRepository<CoexistingDocument, String> {

    fun findByCategoryOrderByTitleAsc(category: String): Flow<CoexistingDocument>

    @Query(filter = "category = ?0")
    fun findByCategory(category: String): Flow<CoexistingDocument>

    @Query(filter = "category = ?0")
    fun findByCategory(category: String, pageable: Pageable): Flow<CoexistingDocument>

    @Query(filter = "category = ?1", q = "?0")
    fun declaredSearch(query: String, category: String): Flow<CoexistingDocument>

    @Query(filter = "category = ?1", q = "?0")
    suspend fun declaredSingleSearch(query: String, category: String): CoexistingDocument?

    suspend fun findByTitle(title: String): CoexistingDocument?

    suspend fun getByTitle(title: String): CoexistingDocument

    suspend fun getByCategory(category: String): CoexistingDocument?

    suspend fun countByCategory(category: String): Long

    suspend fun existsByTitle(title: String): Boolean

    suspend fun deleteByTitle(title: String): Long

    suspend fun deleteByCategory(category: String)
}
