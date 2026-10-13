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
package io.vanslog.spring.data.meilisearch.repository

import com.sun.net.httpserver.HttpServer
import io.vanslog.spring.data.meilisearch.client.ClientConfiguration
import io.vanslog.spring.data.meilisearch.client.msc.ReactiveMeilisearchTemplate
import io.vanslog.spring.data.meilisearch.consumer.coroutinerepository.CoroutineTaskDocument
import io.vanslog.spring.data.meilisearch.repository.support.ReactiveMeilisearchRepositoryFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.data.repository.query.QueryCreationException
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

/**
 * Checks Kotlin logical types and inherited generic binding through consumer-visible repository methods.
 *
 * @author Junghoon Ban
 */
class CoroutineRepositoryQueryContractTests {

    @Test
    fun inheritedGenericSuspendAndFlowQueriesResolvePayloadAndArgumentInConcreteRepository(): Unit = runBlocking {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            exchange.use {
                exchange.requestBody.readAllBytes()
                val body = """{"hits":[{"id":"one","title":"First"}],"totalHits":1,"totalPages":1,"page":1,"hitsPerPage":2,"processingTimeMs":0,"query":""}"""
                    .toByteArray(StandardCharsets.UTF_8)
                exchange.responseHeaders.set("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.write(body)
            }
        }
        server.start()
        try {
            val repository = factory("http://127.0.0.1:${server.address.port}")
                .getRepository(ConcreteGenericQueries::class.java)
            withTimeout(5_000) {
                assertThat(repository.single("First")?.id).isEqualTo("one")
                assertThat(repository.matches("First").toList().map { it.id }).containsExactly("one")
            }
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun rejectsNestedFlowPayloadsBeforeAnySubscription() {
        assertThrows(QueryCreationException::class.java) {
            factory().getRepository(NestedFlowQueries::class.java)
        }
    }

    @Test
    fun rejectsSuspendCollectionsRatherThanCollectingUnboundedResults() {
        assertThrows(QueryCreationException::class.java) {
            factory().getRepository(SuspendCollectionQueries::class.java)
        }
    }

    @Test
    fun rejectsMethodTypeVariablesEvenWhenTheirBoundMatchesTheDomain() {
        assertThrows(QueryCreationException::class.java) {
            factory().getRepository(UnresolvedMethodQueries::class.java)
        }
    }

    private fun factory(endpoint: String = "http://127.0.0.1:1"): ReactiveMeilisearchRepositoryFactory =
        ReactiveMeilisearchRepositoryFactory(ReactiveMeilisearchTemplate(ClientConfiguration.builder()
            .connectedTo(endpoint).withApiKey("").withRequestTimeout(2_000).withRequestInterval(5).build()))

    private interface GenericQueries<T, V> : CoroutineMeilisearchRepository<T, String> {
        @Query(filter = "title = ?0")
        suspend fun single(value: V): T?

        @Query(filter = "title = ?0")
        fun matches(value: V): Flow<T>
    }

    private interface ConcreteGenericQueries : GenericQueries<CoroutineTaskDocument, String>

    private interface NestedFlowQueries : CoroutineMeilisearchRepository<CoroutineTaskDocument, String> {
        @Query(filter = "title = ?0")
        fun nested(value: String): Flow<List<CoroutineTaskDocument>>
    }

    private interface SuspendCollectionQueries : CoroutineMeilisearchRepository<CoroutineTaskDocument, String> {
        @Query(filter = "title = ?0")
        suspend fun collection(value: String): List<CoroutineTaskDocument>
    }

    private interface UnresolvedMethodQueries : CoroutineMeilisearchRepository<CoroutineTaskDocument, String> {
        @Query(filter = "title = ?0")
        suspend fun <R : CoroutineTaskDocument> unresolved(value: String): R?
    }
}
