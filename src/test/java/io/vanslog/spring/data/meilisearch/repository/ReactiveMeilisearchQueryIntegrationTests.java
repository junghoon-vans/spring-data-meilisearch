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
package io.vanslog.spring.data.meilisearch.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.vanslog.spring.data.meilisearch.client.ClientConfiguration;
import io.vanslog.spring.data.meilisearch.client.msc.ReactiveMeilisearchTemplate;
import io.vanslog.spring.data.meilisearch.consumer.reactivequery.ReactiveQueryProduct;
import io.vanslog.spring.data.meilisearch.consumer.reactivequery.ReactiveQueryProductRepository;
import io.vanslog.spring.data.meilisearch.core.ReactiveMeilisearchOperations;
import io.vanslog.spring.data.meilisearch.core.convert.MeilisearchConverter;
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTest;
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTestConfiguration;
import io.vanslog.spring.data.meilisearch.repository.config.EnableReactiveMeilisearchRepositories;

/**
 * Exercises derived and declared reactive searches against Meilisearch.
 *
 * @author Junghoon Ban
 */
@MeilisearchTest
@ContextConfiguration(classes = ReactiveMeilisearchQueryIntegrationTests.Config.class)
class ReactiveMeilisearchQueryIntegrationTests {

	private static final Duration TIMEOUT = Duration.ofSeconds(15);

	@Autowired
	private ReactiveQueryProductRepository repository;

	@BeforeEach
	void populateIndex() {
		repository.deleteAll()
			.thenMany(repository
				.saveAll(List.of(product("1", "Alpha Reactor", 10, true), product("2", "Beta Reactor", 20, false),
						product("3", "Gamma Reactor", 30, true), product("4", "Other Record", "other", 15, true),
						product("5", "Director's \"cut\"", "other", 40, false),
						product("6", "Duplicate", "other", 50, true), product("7", "Duplicate", "other", 60, false))))
			.then()
			.block(TIMEOUT);
	}

	@Test
	void runsDerivedPredicatesAndReactiveProjections() {
		assertThat(repository.findByCategoryAndPriceLessThanEqual("media", 20)
			.map(ReactiveQueryProduct::getId)
			.collectList()
			.block(TIMEOUT)).containsExactly("1", "2");
		assertThat(repository.findByCategoryOrderByPriceDesc("media")
			.map(ReactiveQueryProduct::getId)
			.collectList()
			.block(TIMEOUT)).containsExactly("3", "2", "1");
		assertThat(repository.findByCategory("media", Sort.by("price"))
			.map(ReactiveQueryProduct::getId)
			.collectList()
			.block(TIMEOUT)).containsExactly("1", "2", "3");
		assertThat(repository.findByTitle("Alpha Reactor").block(TIMEOUT).getId()).isEqualTo("1");
		assertThat(repository.countByCategory("media").block(TIMEOUT)).isEqualTo(3L);
		assertThat(repository.existsByTitle("Beta Reactor").block(TIMEOUT)).isTrue();
		assertThat(repository.existsByTitle("Missing").block(TIMEOUT)).isFalse();
	}

	@Test
	void returnsFiniteDeclaredWindowsWithMappedSortAndBindsQAndFilters() {
		assertThat(repository.combined("Reactor", "media", 30, PageRequest.of(0, 2, Sort.by(Sort.Order.desc("price"))))
			.map(ReactiveQueryProduct::getId)
			.collectList()
			.block(TIMEOUT)).containsExactly("3", "2");
		assertThat(repository.combined("Reactor", "media", 30, PageRequest.of(1, 2, Sort.by(Sort.Order.desc("price"))))
			.map(ReactiveQueryProduct::getId)
			.collectList()
			.block(TIMEOUT)).containsExactly("1");
		assertThat(repository.fullText("Reactor").map(ReactiveQueryProduct::getId).collectList().block(TIMEOUT))
			.containsExactly("1", "2", "3");
		assertThat(repository.filterOnly("media").map(ReactiveQueryProduct::getId).collectList().block(TIMEOUT))
			.containsExactly("1", "2", "3");
	}

	@Test
	void streamsUnpagedCollectionsAcrossBoundedSearchPages() {
		long matches = repository
			.saveAll(reactor.core.publisher.Flux.range(0, 1001)
				.map(index -> new ReactiveQueryProduct("bulk-" + index, "Bulk result " + index, "bulk", index, true)))
			.thenMany(repository.filterOnly("bulk"))
			.count()
			.block(Duration.ofSeconds(90));
		assertThat(matches).isEqualTo(1001L);
	}

	@Test
	void bindsEscapedDeclaredValuesAndHonorsAnnotationAndNamedQueryPrecedence() {
		assertThat(repository.byLiteralTitle("Director's \"cut\"").single().block(TIMEOUT).getId()).isEqualTo("5");
		assertThat(repository.namedSearch("Reactor", "media")
			.map(ReactiveQueryProduct::getId)
			.collectList()
			.block(TIMEOUT)).containsExactly("1", "3");
		assertThat(
				repository.preferred("Reactor", "media").map(ReactiveQueryProduct::getId).collectList().block(TIMEOUT))
			.containsExactly("1", "2", "3");
	}

	@Test
	void preservesSingleResultCardinalityAndReactiveDeleteResults() {
		assertThatThrownBy(() -> repository.findByTitle("Duplicate").block(TIMEOUT))
			.isInstanceOf(org.springframework.dao.IncorrectResultSizeDataAccessException.class);
		assertThat(repository.deleteByTitle("Beta Reactor").block(TIMEOUT)).isEqualTo(1L);
		assertThat(repository.countByCategory("media").block(TIMEOUT)).isEqualTo(2L);
		repository.deleteByActiveFalse().block(TIMEOUT);
		assertThat(repository.findByCategory("media", Sort.unsorted())
			.map(ReactiveQueryProduct::getId)
			.collectList()
			.block(TIMEOUT)).containsExactly("1", "3");
	}

	private static ReactiveQueryProduct product(String id, String title, int price, boolean active) {
		return product(id, title, "media", price, active);
	}

	private static ReactiveQueryProduct product(String id, String title, String category, int price, boolean active) {
		return new ReactiveQueryProduct(id, title, category, price, active);
	}

	@Configuration
	@Import(MeilisearchTestConfiguration.class)
	@EnableReactiveMeilisearchRepositories(basePackageClasses = ReactiveQueryProductRepository.class,
			namedQueriesLocation = "classpath:io/vanslog/spring/data/meilisearch/reactive-query-queries.properties")
	static class Config {

		@Bean(name = { "reactiveMeilisearchOperations", "reactiveMeilisearchTemplate" })
		ReactiveMeilisearchOperations reactiveMeilisearchOperations(
				@Qualifier("meilisearchClientConfiguration") ClientConfiguration clientConfiguration,
				MeilisearchConverter converter, @Qualifier("meilisearchObjectMapper") ObjectMapper objectMapper) {
			return new ReactiveMeilisearchTemplate(clientConfiguration, converter, objectMapper);
		}

	}

}
