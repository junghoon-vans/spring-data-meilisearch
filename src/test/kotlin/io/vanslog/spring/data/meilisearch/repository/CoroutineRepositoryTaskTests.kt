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

import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import com.fasterxml.jackson.databind.ObjectMapper

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

import io.vanslog.spring.data.meilisearch.ReactiveTaskException
import io.vanslog.spring.data.meilisearch.ReactiveTaskTimeoutException
import io.vanslog.spring.data.meilisearch.client.ClientConfiguration
import io.vanslog.spring.data.meilisearch.client.msc.ReactiveMeilisearchTemplate
import io.vanslog.spring.data.meilisearch.consumer.coroutinerepository.CoroutineTaskDocument
import io.vanslog.spring.data.meilisearch.consumer.coroutinerepository.CoroutineTaskRepository
import io.vanslog.spring.data.meilisearch.repository.CoroutineMeilisearchRepository
import io.vanslog.spring.data.meilisearch.repository.support.ReactiveMeilisearchRepositoryFactory


/** Exercises coroutine cancellation and task observation through the repository proxy and real HTTP transport. */
class CoroutineRepositoryTaskTests {
    @Test
    fun rejectsBlockingCustomMethodsOnCoroutineRepositories() {
        val configuration = ClientConfiguration.builder()
            .connectedTo("http://127.0.0.1:1")
            .withApiKey("")
            .build()
        val template = ReactiveMeilisearchTemplate(configuration)

        assertThrows(IllegalArgumentException::class.java) {
            ReactiveMeilisearchRepositoryFactory(template).getRepository(MixedCoroutineTaskRepository::class.java)
        }

    }

    @Test
    fun saveTimesOutWithoutEmittingOrResubmittingTheAcceptedWrite() {
        val submissions = AtomicInteger()
        val server = server { exchange ->
            val path = exchange.requestURI.path
            if (path == "/tasks/73") {
                respond(exchange, 200, """{"uid":73,"status":"processing"}""")
            } else {
                submissions.incrementAndGet()
                respond(exchange, 202, """{"taskUid":73,"status":"enqueued"}""")
            }
        }
        try {
            val repository = repository(server, timeout = 100)
            val error = assertThrows(ReactiveTaskTimeoutException::class.java) {
                runBlocking { repository.save(CoroutineTaskDocument("one", "Pending")) }
            }
            assertThat(error.taskUid).isEqualTo(73)
            assertThat(submissions.get()).isEqualTo(1)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun failedAcceptedDeleteTaskPropagatesItsServerFailure() {
        val submissions = AtomicInteger()
        val server = server { exchange ->
            if (exchange.requestURI.path == "/tasks/74") {
                respond(exchange, 200, """{"uid":74,"status":"failed","error":{"code":"index_not_found","message":"Index was removed"}}""")
            } else {
                submissions.incrementAndGet()
                respond(exchange, 202, """{"taskUid":74,"status":"enqueued"}""")
            }
        }
        try {
            val repository = repository(server, timeout = 2_000)
            val error = assertThrows(ReactiveTaskException::class.java) {
                runBlocking { repository.deleteById("one") }
            }
            assertThat(error.taskUid).isEqualTo(74)
            assertThat(error.status).isEqualTo("failed")
            assertThat(error.errorCode).isEqualTo("index_not_found")
            assertThat(submissions.get()).isEqualTo(1)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun cancellingSuspendedSaveStopsTaskObservationWithoutCancellingTheServerTask(): Unit = runBlocking {
        val submissions = AtomicInteger()
        val polls = AtomicInteger()
        val server = server { exchange ->
            if (exchange.requestURI.path == "/tasks/75") {
                polls.incrementAndGet()
                respond(exchange, 200, """{"uid":75,"status":"processing"}""")
            } else {
                submissions.incrementAndGet()
                respond(exchange, 202, """{"taskUid":75,"status":"enqueued"}""")
            }
        }
        try {
            val repository = repository(server, timeout = 10_000)
            val save = launch(Dispatchers.Default) {
                repository.save(CoroutineTaskDocument("one", "Still processing"))
            }
            withTimeout(5_000) {
                while (submissions.get() == 0 || polls.get() == 0) {
                    delay(5)
                }
            }

            save.cancelAndJoin()
            val pollsAfterCancellation = polls.get()
            delay(50)

            assertThat(submissions.get()).isEqualTo(1)
            assertThat(polls.get()).isEqualTo(pollsAfterCancellation)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun takingOneRepositoryFlowValueCancelsItsUpstreamFlow() {
        val fetchRequests = AtomicInteger()
        val server = server { exchange ->
            if (exchange.requestURI.path == "/indexes/gh216-coroutine-task/documents/fetch") {
                fetchRequests.incrementAndGet()
                respond(exchange, 200, """{"results":[{"id":"one","title":"First"}]}""")
            } else {
                respond(exchange, 404, """{"message":"unexpected request"}""")
            }
        }
        try {
            val repository = repository(server, timeout = 2_000, batchSize = 1)
            val cancelled = AtomicBoolean()
            val ids: Flow<String> = flow {
                try {
                    emit("one")
                    awaitCancellation()
                } finally {
                    cancelled.set(true)
                }
            }
            val results = runBlocking { repository.findAllById(ids).take(1).toList() }

            assertThat(results.map { it.id }).containsExactly("one")
            assertThat(cancelled.get()).isTrue()
            assertThat(fetchRequests.get()).isEqualTo(1)
        } finally {
            server.stop(0)
        }
    }

    private fun repository(server: HttpServer, timeout: Int, batchSize: Int = 100): CoroutineTaskRepository {
        val configuration = ClientConfiguration.builder()
            .connectedTo("http://127.0.0.1:${server.address.port}")
            .withApiKey("")
            .withRequestTimeout(timeout)
            .withRequestInterval(5)
            .build()
        val template = ReactiveMeilisearchTemplate(configuration, null, ObjectMapper(), batchSize)
        return ReactiveMeilisearchRepositoryFactory(template).getRepository(CoroutineTaskRepository::class.java)
    }

    private fun server(handler: (HttpExchange) -> Unit): HttpServer {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            try {
                exchange.requestBody.readAllBytes()
                handler(exchange)
            } finally {
                exchange.close()
            }
        }
        server.start()
        return server
    }

    private fun respond(exchange: HttpExchange, status: Int, json: String) {
        val body = json.toByteArray(StandardCharsets.UTF_8)
        exchange.responseHeaders.set("Content-Type", "application/json")
        exchange.sendResponseHeaders(status, body.size.toLong())
        exchange.responseBody.write(body)
    }

    private interface MixedCoroutineTaskRepository :
        CoroutineMeilisearchRepository<CoroutineTaskDocument, String> {

        fun blockingFindByTitle(title: String): CoroutineTaskDocument?
    }
}
