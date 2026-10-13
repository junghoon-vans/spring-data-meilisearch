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

import com.fasterxml.jackson.databind.ObjectMapper
import com.meilisearch.sdk.FederationOptions
import com.meilisearch.sdk.exceptions.MeilisearchApiException
import io.vanslog.spring.data.meilisearch.annotations.Document
import io.vanslog.spring.data.meilisearch.annotations.Embedder
import io.vanslog.spring.data.meilisearch.annotations.Setting
import io.vanslog.spring.data.meilisearch.client.ClientConfiguration
import io.vanslog.spring.data.meilisearch.client.MeilisearchClient
import io.vanslog.spring.data.meilisearch.client.msc.ReactiveMeilisearchTemplate
import io.vanslog.spring.data.meilisearch.core.ReactiveMeilisearchOperations
import io.vanslog.spring.data.meilisearch.core.convert.MeilisearchConverter
import io.vanslog.spring.data.meilisearch.core.federation.Federation
import io.vanslog.spring.data.meilisearch.core.query.BaseQuery
import io.vanslog.spring.data.meilisearch.core.query.BasicQuery
import io.vanslog.spring.data.meilisearch.core.query.FacetQuery
import io.vanslog.spring.data.meilisearch.core.query.IndexQuery
import io.vanslog.spring.data.meilisearch.core.query.SimilarQuery
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTest
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTestConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.annotation.Id
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.test.context.ContextConfiguration
import reactor.core.publisher.Flux
import reactor.test.StepVerifier
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import io.vanslog.spring.data.meilisearch.core.applySettings as applyReactiveSettings
import io.vanslog.spring.data.meilisearch.core.count as reactiveCount
import io.vanslog.spring.data.meilisearch.core.delete as reactiveDelete
import io.vanslog.spring.data.meilisearch.core.deleteAll as reactiveDeleteAll
import io.vanslog.spring.data.meilisearch.core.deleteAllById as reactiveDeleteAllById
import io.vanslog.spring.data.meilisearch.core.deleteByFilter as reactiveDeleteByFilter
import io.vanslog.spring.data.meilisearch.core.exists as reactiveExists
import io.vanslog.spring.data.meilisearch.core.facetSearch as reactiveFacetSearch
import io.vanslog.spring.data.meilisearch.core.findAll as reactiveFindAll
import io.vanslog.spring.data.meilisearch.core.get as reactiveGet
import io.vanslog.spring.data.meilisearch.core.multiGet as reactiveMultiGet
import io.vanslog.spring.data.meilisearch.core.multiSearch as reactiveMultiSearch
import io.vanslog.spring.data.meilisearch.core.search as reactiveSearch
import io.vanslog.spring.data.meilisearch.core.similarSearch as reactiveSimilarSearch

private const val DOCUMENT_INDEX = "kotlin-reactive-extension-documents"
private const val VECTOR_INDEX = "kotlin-reactive-extension-vectors"
private val BLOCK_TIMEOUT: Duration = Duration.ofSeconds(30)

/** Exercises reified extensions through the real reactive template and Meilisearch HTTP API. */
@MeilisearchTest
@ContextConfiguration(
    classes = [MeilisearchTestConfiguration::class, ReactiveExtensionTestConfiguration::class],
)
class ReactiveOperationsExtensionsTests {

    @Autowired
    private lateinit var meilisearchClient: MeilisearchClient

    @Autowired
    private lateinit var operations: ReactiveMeilisearchOperations

    @BeforeEach
    fun setUpIndexes() {
        deleteIndexIfExists(DOCUMENT_INDEX)
        deleteIndexIfExists(VECTOR_INDEX)
        createIndex(DOCUMENT_INDEX)
        createIndex(VECTOR_INDEX)
        operations.applyReactiveSettings<ReactiveExtensionEntry>().block(BLOCK_TIMEOUT)
        operations.applyReactiveSettings<ReactiveVectorEntry>().block(BLOCK_TIMEOUT)
    }

    @AfterEach
    fun deleteIndexes() {
        deleteIndexIfExists(DOCUMENT_INDEX)
        deleteIndexIfExists(VECTOR_INDEX)
    }

    @Test
    fun `document extensions preserve absence CRUD counts and task-backed deletes`() {
        val entries = listOf(
            entry("doc-1", "Hero Alpha", "drama", 1),
            entry("doc-2", "Hero Bravo", "drama", 2),
            entry("doc-3", "Hero Charlie", "drama", 3),
            entry("doc-4", "Villain Delta", "action", 4),
        )
        operations.saveAll(Flux.fromIterable(entries)).then().block(BLOCK_TIMEOUT)

        StepVerifier.create(operations.reactiveGet<ReactiveExtensionEntry>("absent")).verifyComplete()
        assertThat(operations.reactiveGet<ReactiveExtensionEntry>("doc-1").block(BLOCK_TIMEOUT)?.title)
            .isEqualTo("Hero Alpha")

        val requested = operations.reactiveMultiGet<ReactiveExtensionEntry>(
            Flux.just("doc-3", "absent", "doc-1", "doc-3"),
        ).collectList().block(BLOCK_TIMEOUT)!!
        assertThat(requested.map { it.id }).containsExactly("doc-3", "doc-1", "doc-3")

        val all = operations.reactiveFindAll<ReactiveExtensionEntry>().collectList().block(BLOCK_TIMEOUT)!!
        assertThat(all.map { it.id }).containsExactlyInAnyOrder("doc-1", "doc-2", "doc-3", "doc-4")
        val sorted = operations.reactiveFindAll<ReactiveExtensionEntry>(Sort.by(Sort.Direction.DESC, "rank"))
            .collectList().block(BLOCK_TIMEOUT)!!
        assertThat(sorted.map { it.rank }).containsExactly(4, 3, 2, 1)

        assertThat(operations.reactiveExists<ReactiveExtensionEntry>("doc-1").block(BLOCK_TIMEOUT)).isTrue()
        assertThat(operations.reactiveExists<ReactiveExtensionEntry>("absent").block(BLOCK_TIMEOUT)).isFalse()
        assertThat(operations.reactiveCount<ReactiveExtensionEntry>().block(BLOCK_TIMEOUT)).isEqualTo(4L)
        assertThat(operations.reactiveCount<ReactiveExtensionEntry>("category = drama").block(BLOCK_TIMEOUT))
            .isEqualTo(3L)

        operations.reactiveDelete<ReactiveExtensionEntry>("doc-4").block(BLOCK_TIMEOUT)
        assertThat(operations.reactiveExists<ReactiveExtensionEntry>("doc-4").block(BLOCK_TIMEOUT)).isFalse()

        operations.reactiveDeleteAllById<ReactiveExtensionEntry>(Flux.just("doc-1", "doc-2")).block(BLOCK_TIMEOUT)
        assertThat(operations.reactiveCount<ReactiveExtensionEntry>().block(BLOCK_TIMEOUT)).isEqualTo(1L)
        assertThat(operations.reactiveDeleteByFilter<ReactiveExtensionEntry>("category = drama").block(BLOCK_TIMEOUT))
            .isEqualTo(1L)
        assertThat(operations.reactiveCount<ReactiveExtensionEntry>().block(BLOCK_TIMEOUT)).isZero()

        operations.reactiveDeleteAll<ReactiveExtensionEntry>().block(BLOCK_TIMEOUT)
        assertThat(operations.reactiveCount<ReactiveExtensionEntry>().block(BLOCK_TIMEOUT)).isZero()
    }

    @Test
    fun `search extensions retain query facet federation and similar hit metadata`() {
        val entries = listOf(
            entry("book-1", "Hero Alpha", "drama", 1),
            entry("book-2", "Hero Bravo", "drama", 2),
            entry("book-3", "Hero Charlie", "drama", 3),
            entry("book-4", "Villain Delta", "action", 4),
        )
        operations.saveAll(Flux.fromIterable(entries)).then().block(BLOCK_TIMEOUT)

        val pageQuery = BasicQuery.builder()
            .withQ("hero")
            .withFilter("category = drama")
            .withFacets("category")
            .withSort(Sort.by("rank"))
            .withPageable(PageRequest.of(1, 1))
            .build()
        val page = operations.reactiveSearch<ReactiveExtensionEntry>(pageQuery).block(BLOCK_TIMEOUT)!!
        assertThat(page.totalHits).isEqualTo(3L)
        assertThat(page.searchHits).hasSize(1)
        assertThat(page.getSearchHit(0).content.id).isEqualTo("book-2")
        assertThat(page.getSearchHit(0).query).isEqualTo("hero")
        @Suppress("UNCHECKED_CAST")
        val distribution = page.getSearchHit(0).facetDistribution as Map<String, Map<String, Int>>
        assertThat(distribution["category"]).containsEntry("drama", 3)

        val queries: List<BaseQuery> = listOf(
            BasicQuery.builder().withQ("hero").withFacets("category").build(),
            BasicQuery.builder().withQ("villain").withFacets("category").build(),
        )
        val multiSearch = operations.reactiveMultiSearch<ReactiveExtensionEntry>(queries).block(BLOCK_TIMEOUT)!!
        assertThat(multiSearch.searchHits.map { it.query }).containsExactly("hero", "hero", "hero", "villain")
        assertThat(multiSearch.getSearchHit(0).facetDistribution).isNotNull()

        val federatedQueries = listOf(
            IndexQuery.builder()
                .withQ("hero")
                .withIndexUid(DOCUMENT_INDEX)
                .withFederationOptions(FederationOptions().setWeight(2.0))
                .build(),
        )
        val federation = Federation(10, 0, mapOf(DOCUMENT_INDEX to listOf("category")), null)
        val federated = operations.reactiveMultiSearch<ReactiveExtensionEntry>(federatedQueries, federation)
            .block(BLOCK_TIMEOUT)!!
        assertThat(federated.searchHits).hasSize(3)
        val hitFederation = checkNotNull(federated.getSearchHit(0).federation)
        assertThat(hitFederation.indexUid).isEqualTo(DOCUMENT_INDEX)
        assertThat(hitFederation.weightedRankingScore).isPositive()
        @Suppress("UNCHECKED_CAST")
        val federatedFacets = federated.getSearchHit(0).facetDistribution as Map<String, Any>
        assertThat(federatedFacets).containsKey("facetsByIndex")

        operations.saveAll(
            Flux.just(
                vectorEntry("reference", "Reference", listOf(1.0, 0.0, 0.0)),
                vectorEntry("near", "Near", listOf(0.9, 0.1, 0.0)),
                vectorEntry("far", "Far", listOf(0.2, 0.8, 0.0)),
            ),
        ).then().block(BLOCK_TIMEOUT)
        val facets = operations.reactiveFacetSearch<ReactiveExtensionEntry>(FacetQuery("category"))
            .block(BLOCK_TIMEOUT)!!
        assertThat(facets.searchHits.map { it.content.value }).containsExactlyInAnyOrder("drama", "action")
        assertThat(facets.searchHits.first { it.content.value == "drama" }.content.count).isEqualTo(3)

        val similar = operations.reactiveSimilarSearch<ReactiveVectorEntry>(SimilarQuery("reference", "manual"))
            .block(BLOCK_TIMEOUT)!!
        assertThat(similar.searchHits.map { it.content.id }).contains("near", "far")
        assertThat(similar.getSearchHit(0).content.id).isEqualTo("near")
    }

    @Test
    fun `search stays cold and stopping multi-get cancels its input`() {
        val delayedSearch = operations.reactiveSearch<ReactiveExtensionEntry>(
            BasicQuery.builder().withQ("late arrival").build(),
        )
        operations.save(entry("late-1", "Late Arrival", "drama", 1)).block(BLOCK_TIMEOUT)
        assertThat(delayedSearch.block(BLOCK_TIMEOUT)!!.totalHits).isEqualTo(1L)

        operations.saveAll(
            Flux.range(0, 7).map { rank -> entry("bounded-$rank", "Bounded $rank", "drama", rank) },
        ).then().block(BLOCK_TIMEOUT)
        val cancelled = AtomicBoolean()
        val ids = Flux.range(0, 10_000)
            .doOnCancel { cancelled.set(true) }
            .map { "bounded-$it" }
        val firstFive = operations.reactiveMultiGet<ReactiveExtensionEntry>(ids)
            .take(5)
            .map { it.id }
            .collectList()
            .block(BLOCK_TIMEOUT)!!

        assertThat(firstFive).containsExactly("bounded-0", "bounded-1", "bounded-2", "bounded-3", "bounded-4")
        assertThat(cancelled.get()).isTrue()
    }

    private fun entry(id: String, title: String, category: String, rank: Int) =
        ReactiveExtensionEntry(id, title, category, rank)

    private fun vectorEntry(id: String, title: String, vector: List<Double>) =
        ReactiveVectorEntry(id, title, mapOf("manual" to vector))

    private fun createIndex(indexUid: String) {
        val task = meilisearchClient.createIndex(indexUid, "id")
        meilisearchClient.index(indexUid).waitForTask(
            task.taskUid,
            meilisearchClient.requestTimeout,
            meilisearchClient.requestInterval,
        )
    }

    private fun deleteIndexIfExists(indexUid: String) {
        try {
            val task = meilisearchClient.deleteIndex(indexUid)
            meilisearchClient.index(indexUid).waitForTask(
                task.taskUid,
                meilisearchClient.requestTimeout,
                meilisearchClient.requestInterval,
            )
        } catch (exception: MeilisearchApiException) {
            if (exception.code != "index_not_found") {
                throw exception
            }
        }
    }
}

@Configuration(proxyBeanMethods = false)
class ReactiveExtensionTestConfiguration {

    @Bean
    fun reactiveMeilisearchOperations(
        @Qualifier("meilisearchClientConfiguration") clientConfiguration: ClientConfiguration,
        converter: MeilisearchConverter,
        @Qualifier("meilisearchObjectMapper") objectMapper: ObjectMapper,
    ): ReactiveMeilisearchOperations = ReactiveMeilisearchTemplate(clientConfiguration, converter, objectMapper, 7)
}

@Setting(filterableAttributes = ["category"], sortableAttributes = ["rank"])
@Document(indexUid = DOCUMENT_INDEX)
class ReactiveExtensionEntry() {

    @field:Id
    var id: String? = null
    var title: String = ""
    var category: String = ""
    var rank: Int = 0

    constructor(id: String, title: String, category: String, rank: Int) : this() {
        this.id = id
        this.title = title
        this.category = category
        this.rank = rank
    }
}

@Setting(embedders = [Embedder(name = "manual", source = Embedder.Source.USER_PROVIDED, dimensions = 3)])
@Document(indexUid = VECTOR_INDEX)
class ReactiveVectorEntry() {

    @field:Id
    var id: String? = null
    var title: String = ""
    var _vectors: Map<String, List<Double>> = emptyMap()

    constructor(id: String, title: String, vectors: Map<String, List<Double>>) : this() {
        this.id = id
        this.title = title
        this._vectors = vectors
    }
}
