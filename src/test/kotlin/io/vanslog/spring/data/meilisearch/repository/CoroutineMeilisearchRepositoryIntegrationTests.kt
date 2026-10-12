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

import java.time.Duration

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.domain.Sort
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ContextConfiguration

import com.fasterxml.jackson.databind.ObjectMapper

import io.vanslog.spring.data.meilisearch.client.ClientConfiguration
import io.vanslog.spring.data.meilisearch.client.msc.ReactiveMeilisearchTemplate
import io.vanslog.spring.data.meilisearch.consumer.reactivecoexistence.BlockingCoexistingRepository
import io.vanslog.spring.data.meilisearch.consumer.reactivecoexistence.CoexistingDocument
import io.vanslog.spring.data.meilisearch.consumer.reactivecoexistence.CoroutineCoexistingRepository
import io.vanslog.spring.data.meilisearch.consumer.reactivecoexistence.ReactiveCoexistingRepository
import io.vanslog.spring.data.meilisearch.core.ReactiveMeilisearchOperations
import io.vanslog.spring.data.meilisearch.core.convert.MeilisearchConverter
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTest
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTestConfiguration
import io.vanslog.spring.data.meilisearch.repository.config.EnableMeilisearchRepositories
import io.vanslog.spring.data.meilisearch.repository.config.EnableReactiveMeilisearchRepositories

/** Exercises the coroutine repository proxy against Meilisearch and alongside both existing repository models. */
@MeilisearchTest
@ContextConfiguration(classes = [CoroutineMeilisearchRepositoryIntegrationTests.Config::class])
class CoroutineMeilisearchRepositoryIntegrationTests {

    @Autowired
    private lateinit var coroutineRepository: CoroutineCoexistingRepository

    @Autowired
    private lateinit var blockingRepository: BlockingCoexistingRepository

    @Autowired
    private lateinit var reactiveRepository: ReactiveCoexistingRepository

    @BeforeEach
    fun populateIndex(): Unit = runBlocking {
        coroutineRepository.deleteAll()
        coroutineRepository.saveAll(flowOf(
            CoexistingDocument("1", "Pine Alpha", "games"),
            CoexistingDocument("2", "Pine Beta", "news"),
            CoexistingDocument("3", "Twin Alpha", "games"),
            CoexistingDocument("4", "Twin Beta", "games"),
            CoexistingDocument("5", "Oak", "news"),
            CoexistingDocument("6", "Maple", "news")
        )).toList()
    }

    @Test
    fun supportsCrudSortingMissingDocumentsAndBoundedFlowConsumption(): Unit = runBlocking {
        assertThat(coroutineRepository.count()).isEqualTo(6L)
        assertThat(coroutineRepository.findById("missing")).isNull()
        assertThat(coroutineRepository.existsById("1")).isTrue()
        assertThat(coroutineRepository.existsById("missing")).isFalse()
        assertThat(coroutineRepository.findAllById(flowOf("5", "1", "missing")).map { it.id }.toList())
            .containsExactly("5", "1")
        assertThat(coroutineRepository.findAll(Sort.by("title")).map { it.title }.toList())
            .containsExactly("Maple", "Oak", "Pine Alpha", "Pine Beta", "Twin Alpha", "Twin Beta")
        assertThat(coroutineRepository.findAll().take(2).toList()).hasSize(2)
    }

    @Test
    fun executesDerivedAndDeclaredQueriesWithNullableSingleAndCardinalitySemantics(): Unit = runBlocking {
        assertThat(coroutineRepository.findByCategoryOrderByTitleAsc("games").map { it.id }.toList())
            .containsExactly("1", "3", "4")
        assertThat(coroutineRepository.declaredSearch("Pine", "games").map { it.id }.toList())
            .containsExactly("1")

        assertThat(coroutineRepository.declaredSingleSearch("Pine", "games")?.id).isEqualTo("1")
        assertThat(coroutineRepository.findByCategory("games").map { it.id }.toList())
            .containsExactly("1", "3", "4")

        assertThat(coroutineRepository.findByCategory("games", PageRequest.of(0, 2, Sort.by("title")))
            .map { it.title }.toList()).containsExactly("Pine Alpha", "Twin Alpha")
        assertThat(coroutineRepository.findByCategory("games", PageRequest.of(1, 2, Sort.by("title")))
            .map { it.title }.toList()).containsExactly("Twin Beta")

        assertThat(coroutineRepository.findByTitle("Pine Alpha")?.id).isEqualTo("1")
        assertThat(coroutineRepository.findByTitle("Pine Beta")).isNull()
        assertThat(coroutineRepository.findByTitle("missing")).isNull()
        assertThat(coroutineRepository.getByTitle("Pine Beta").id).isEqualTo("2")
        assertThatThrownBy { runBlocking { coroutineRepository.getByCategory("games") } }
            .isInstanceOf(org.springframework.dao.IncorrectResultSizeDataAccessException::class.java)

        assertThat(coroutineRepository.countByCategory("games")).isEqualTo(3L)
        assertThat(coroutineRepository.existsByTitle("Oak")).isTrue()
        assertThat(coroutineRepository.existsByTitle("missing")).isFalse()
        assertThat(coroutineRepository.deleteByTitle("Pine Beta")).isEqualTo(1L)
        coroutineRepository.deleteByCategory("games")
        assertThat(coroutineRepository.count()).isEqualTo(2L)
    }

    @Test
    fun blockingReactiveAndCoroutineRepositoriesRemainIndependent(): Unit = runBlocking {
        assertThat(blockingRepository.findById("2").orElseThrow().title).isEqualTo("Pine Beta")
        assertThat(reactiveRepository.findById("2").block(Duration.ofSeconds(15))?.title).isEqualTo("Pine Beta")
        assertThat(coroutineRepository.findById("2")?.title).isEqualTo("Pine Beta")
    }

    @Configuration(proxyBeanMethods = false)
    @Import(MeilisearchTestConfiguration::class)
    @EnableMeilisearchRepositories(basePackageClasses = [BlockingCoexistingRepository::class])
    @EnableReactiveMeilisearchRepositories(
        basePackageClasses = [ReactiveCoexistingRepository::class],
        namedQueriesLocation = "classpath:io/vanslog/spring/data/meilisearch/coroutine-query-queries.properties"
    )
    class Config {

        @Bean(name = ["reactiveMeilisearchOperations", "reactiveMeilisearchTemplate"])
        fun reactiveMeilisearchOperations(
            @Qualifier("meilisearchClientConfiguration") clientConfiguration: ClientConfiguration,
            converter: MeilisearchConverter,
            @Qualifier("meilisearchObjectMapper") objectMapper: ObjectMapper
        ): ReactiveMeilisearchOperations = ReactiveMeilisearchTemplate(clientConfiguration, converter, objectMapper)
    }
}
