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

import com.meilisearch.sdk.MultiSearchFederation
import io.vanslog.spring.data.meilisearch.core.convert.MeilisearchConverter
import io.vanslog.spring.data.meilisearch.core.query.BaseQuery
import io.vanslog.spring.data.meilisearch.core.query.BasicQuery
import io.vanslog.spring.data.meilisearch.core.query.FacetQuery
import io.vanslog.spring.data.meilisearch.core.query.SimilarQuery
import java.time.Duration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OperationsExtensionsTests {

    @Test
    fun `document extensions delegate with reified entity type`() {
        // given
        val operations = RecordingOperations()
        val documentId = "movie-1"

        // when
        operations.get<Movie>(documentId)
        operations.multiGet<Movie>()
        operations.multiGet<Movie>(10, 20)
        operations.multiGet<Movie>(listOf(documentId))
        operations.multiGet<Movie>(listOf(documentId), 10, 20)
        operations.exists<Movie>(documentId)
        operations.delete<Movie>(documentId)
        operations.delete<Movie>(listOf(documentId))
        operations.deleteAll<Movie>()

        // then
        assertThat(operations.calls).containsExactly(
            "get:Movie:$documentId",
            "multiGet:Movie",
            "multiGet:Movie:10:20",
            "multiGet:Movie:$documentId",
            "multiGet:Movie:$documentId:10:20",
            "exists:Movie:$documentId",
            "delete:Movie:$documentId",
            "delete:Movie:$documentId",
            "deleteAll:Movie",
        )
    }

    @Test
    fun `search extensions delegate with reified entity type`() {
        // given
        val operations = RecordingOperations()
        val query = BasicQuery("wonder")
        val facetQuery = FacetQuery("genre")
        val similarQuery = SimilarQuery("movie-1", "default")
        val federation = MultiSearchFederation()

        // when
        operations.count<Movie>()
        operations.search<Movie>(query)
        operations.multiSearch<Movie>(listOf(query))
        operations.multiSearch<Movie>(listOf(query), federation)
        operations.facetSearch<Movie>(facetQuery)
        operations.similarSearch<Movie>(similarQuery)

        // then
        assertThat(operations.calls).containsExactly(
            "count:Movie",
            "search:Movie",
            "multiSearch:Movie",
            "multiSearchFederated:Movie",
            "facetSearch:Movie",
            "similarSearch:Movie",
        )
    }

    @Test
    fun `meilisearch operations extensions delegate with reified entity type`() {
        // given
        val operations = RecordingOperations()

        // when
        val indexOperations = operations.indexOps<Movie>()
        operations.applySettings<Movie>()

        // then
        assertThat(indexOperations === operations.indexOperations).isTrue()
        assertThat(operations.calls).containsExactly(
            "indexOps:Movie",
            "applySettings:Movie",
        )
    }

    private class RecordingOperations : MeilisearchOperations {

        val calls = mutableListOf<String>()
        val indexOperations = RecordingIndexOperations()

        override fun instanceOps(): MeilisearchInstanceOperations {
            throw UnsupportedOperationException()
        }

        override fun indexOps(entityClass: Class<*>): MeilisearchIndexOperations {
            calls.add("indexOps:${entityClass.simpleName}")
            return indexOperations
        }

        override fun indexOps(indexUid: String): MeilisearchIndexOperations {
            throw UnsupportedOperationException()
        }

        override fun <T : Any?> applySettings(clazz: Class<T>) {
            calls.add("applySettings:${clazz.simpleName}")
        }

        override fun getMeilisearchConverter(): MeilisearchConverter {
            throw UnsupportedOperationException()
        }

        override fun <T : Any> save(entity: T): T {
            throw UnsupportedOperationException()
        }

        override fun <T : Any> save(entities: MutableList<T>): MutableList<T> {
            throw UnsupportedOperationException()
        }

        override fun <T : Any?> get(documentId: String, clazz: Class<T>): T? {
            calls.add("get:${clazz.simpleName}:$documentId")
            return null
        }

        override fun <T : Any?> multiGet(clazz: Class<T>): MutableList<T> {
            calls.add("multiGet:${clazz.simpleName}")
            return mutableListOf()
        }

        override fun <T : Any?> multiGet(clazz: Class<T>, offset: Int, limit: Int): MutableList<T> {
            calls.add("multiGet:${clazz.simpleName}:$offset:$limit")
            return mutableListOf()
        }

        override fun <T : Any?> multiGet(clazz: Class<T>, documentIds: MutableList<String>): MutableList<T> {
            calls.add("multiGet:${clazz.simpleName}:${documentIds.joinToString()}")
            return mutableListOf()
        }

        override fun <T : Any?> multiGet(
            clazz: Class<T>,
            documentIds: MutableList<String>,
            offset: Int,
            limit: Int,
        ): MutableList<T> {
            calls.add("multiGet:${clazz.simpleName}:${documentIds.joinToString()}:$offset:$limit")
            return mutableListOf()
        }

        override fun exists(documentId: String, clazz: Class<*>): Boolean {
            calls.add("exists:${clazz.simpleName}:$documentId")
            return false
        }

        override fun delete(documentId: String, clazz: Class<*>): Boolean {
            calls.add("delete:${clazz.simpleName}:$documentId")
            return true
        }

        override fun <T : Any?> delete(entity: T): Boolean {
            throw UnsupportedOperationException()
        }

        override fun delete(clazz: Class<*>, documentIds: MutableList<String>): Boolean {
            calls.add("delete:${clazz.simpleName}:${documentIds.joinToString()}")
            return true
        }

        override fun <T : Any?> delete(entities: MutableList<T>): Boolean {
            throw UnsupportedOperationException()
        }

        override fun deleteAll(clazz: Class<*>): Boolean {
            calls.add("deleteAll:${clazz.simpleName}")
            return true
        }

        override fun count(clazz: Class<*>): Long {
            calls.add("count:${clazz.simpleName}")
            return 0
        }

        override fun <T : Any?, Q : BaseQuery?> search(query: Q, clazz: Class<T>): SearchHits<T> {
            calls.add("search:${clazz.simpleName}")
            return SearchHitsImpl(Duration.ZERO, listOf())
        }

        override fun <T : Any?, Q : BaseQuery?> multiSearch(queries: MutableList<Q>, clazz: Class<T>): SearchHits<T> {
            calls.add("multiSearch:${clazz.simpleName}")
            return SearchHitsImpl(Duration.ZERO, listOf())
        }

        override fun <T : Any?, Q : BaseQuery?> multiSearch(
            queries: MutableList<Q>,
            federation: MultiSearchFederation,
            clazz: Class<T>,
        ): SearchHits<T> {
            calls.add("multiSearchFederated:${clazz.simpleName}")
            return SearchHitsImpl(Duration.ZERO, listOf())
        }

        override fun facetSearch(query: FacetQuery, clazz: Class<*>): SearchHits<FacetHit> {
            calls.add("facetSearch:${clazz.simpleName}")
            return SearchHitsImpl(Duration.ZERO, listOf())
        }

        override fun <T : Any?> similarSearch(query: SimilarQuery, clazz: Class<T>): SearchHits<T> {
            calls.add("similarSearch:${clazz.simpleName}")
            return SearchHitsImpl(Duration.ZERO, listOf())
        }
    }

    private class RecordingIndexOperations : MeilisearchIndexOperations {

        override fun create(): MeilisearchIndex {
            throw UnsupportedOperationException()
        }

        override fun create(request: MeilisearchIndexCreateRequest): MeilisearchIndex {
            throw UnsupportedOperationException()
        }

        override fun get(): MeilisearchIndex {
            throw UnsupportedOperationException()
        }

        override fun list(): MeilisearchIndexList {
            throw UnsupportedOperationException()
        }

        override fun list(query: MeilisearchIndexQuery): MeilisearchIndexList {
            throw UnsupportedOperationException()
        }

        override fun update(request: MeilisearchIndexUpdateRequest): MeilisearchIndex {
            throw UnsupportedOperationException()
        }

        override fun delete(): Boolean {
            throw UnsupportedOperationException()
        }

        override fun stats(): MeilisearchIndexStats {
            throw UnsupportedOperationException()
        }

        override fun getSettings(): MeilisearchIndexSettings {
            throw UnsupportedOperationException()
        }

        override fun updateSettings(settings: MeilisearchIndexSettings): MeilisearchIndexSettings {
            throw UnsupportedOperationException()
        }

        override fun resetSettings(): MeilisearchIndexSettings {
            throw UnsupportedOperationException()
        }
    }

    private class Movie
}
