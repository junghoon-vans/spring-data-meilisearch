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
package io.vanslog.spring.data.meilisearch.core.reactive

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.vanslog.spring.data.meilisearch.ReactiveTaskException
import io.vanslog.spring.data.meilisearch.ReactiveTaskTimeoutException
import io.vanslog.spring.data.meilisearch.annotations.Document
import io.vanslog.spring.data.meilisearch.client.ClientConfiguration
import io.vanslog.spring.data.meilisearch.client.msc.ReactiveMeilisearchTemplate
import io.vanslog.spring.data.meilisearch.core.ReactiveMeilisearchOperations
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.annotation.Id
import reactor.test.StepVerifier
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.time.Duration
import io.vanslog.spring.data.meilisearch.core.deleteAll as reactiveDeleteAll

private const val FAILURE_INDEX = "kotlin-reactive-extension-errors"
private const val FAILURE_TASK_UID = 8101L
private const val TIMEOUT_TASK_UID = 8102L

class ReactiveOperationFailureExtensionsTests {

    @Test
    fun `reified delete propagates terminal task failure and UID`() {
        withServer(
            requestTimeoutMs = 1_000,
            handler = { exchange ->
                when {
                    exchange.requestMethod == "DELETE" && exchange.requestURI.path == "/indexes/$FAILURE_INDEX/documents" ->
                        respond(exchange, 202, "{\"taskUid\":$FAILURE_TASK_UID,\"status\":\"enqueued\"}")

                    exchange.requestURI.path == "/tasks/$FAILURE_TASK_UID" ->
                        respond(
                            exchange,
                            200,
                            """{"uid":$FAILURE_TASK_UID,"status":"failed","error":{"code":"invalid_document","message":"task rejected","type":"invalid_request","link":"https://example.test/errors"}}""",
                        )

                    else -> respond(exchange, 404, "{\"code\":\"not_found\",\"type\":\"invalid_request\"}")
                }
            },
        ) { operations ->
            StepVerifier.create(operations.reactiveDeleteAll<ReactiveFailureEntry>())
                .expectErrorSatisfies { error ->
                    assertThat(error).isInstanceOf(ReactiveTaskException::class.java)
                    val taskFailure = error as ReactiveTaskException
                    assertThat(taskFailure.taskUid).isEqualTo(FAILURE_TASK_UID)
                    assertThat(taskFailure.status).isEqualTo("failed")
                    assertThat(taskFailure.errorCode).isEqualTo("invalid_document")
                    assertThat(taskFailure.errorMessage).isEqualTo("task rejected")
                }
                .verify(Duration.ofSeconds(3))
        }
    }

    @Test
    fun `reified delete propagates task observation timeout and UID`() {
        withServer(
            requestTimeoutMs = 60,
            handler = { exchange ->
                when {
                    exchange.requestMethod == "DELETE" && exchange.requestURI.path == "/indexes/$FAILURE_INDEX/documents" ->
                        respond(exchange, 202, "{\"taskUid\":$TIMEOUT_TASK_UID,\"status\":\"enqueued\"}")

                    exchange.requestURI.path == "/tasks/$TIMEOUT_TASK_UID" ->
                        respond(exchange, 200, "{\"uid\":$TIMEOUT_TASK_UID,\"status\":\"processing\"}")

                    else -> respond(exchange, 404, "{\"code\":\"not_found\",\"type\":\"invalid_request\"}")
                }
            },
        ) { operations ->
            StepVerifier.create(operations.reactiveDeleteAll<ReactiveFailureEntry>())
                .expectErrorSatisfies { error ->
                    assertThat(error).isInstanceOf(ReactiveTaskTimeoutException::class.java)
                    assertThat((error as ReactiveTaskTimeoutException).taskUid).isEqualTo(TIMEOUT_TASK_UID)
                }
                .verify(Duration.ofSeconds(3))
        }
    }

    private fun withServer(
        requestTimeoutMs: Int,
        handler: (HttpExchange) -> Unit,
        exercise: (ReactiveMeilisearchOperations) -> Unit,
    ) {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange -> handler(exchange) }
        server.start()
        try {
            val configuration = ClientConfiguration.builder()
                .connectedTo("http://127.0.0.1:${server.address.port}")
                .withApiKey("behavior-test")
                .withRequestTimeout(requestTimeoutMs)
                .withRequestInterval(5)
                .build()
            exercise(ReactiveMeilisearchTemplate(configuration))
        } finally {
            server.stop(0)
        }
    }

    private fun respond(exchange: HttpExchange, status: Int, body: String) {
        try {
            val bytes = body.toByteArray(StandardCharsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        } finally {
            exchange.close()
        }
    }
}

@Document(indexUid = FAILURE_INDEX)
class ReactiveFailureEntry {

    @field:Id
    var id: String? = null
}
