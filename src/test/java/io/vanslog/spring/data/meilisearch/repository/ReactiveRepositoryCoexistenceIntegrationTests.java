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
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vanslog.spring.data.meilisearch.client.ClientConfiguration;
import io.vanslog.spring.data.meilisearch.client.msc.ReactiveMeilisearchTemplate;
import io.vanslog.spring.data.meilisearch.consumer.reactivecoexistence.BlockingCoexistingRepository;
import io.vanslog.spring.data.meilisearch.consumer.reactivecoexistence.CoexistingDocument;
import io.vanslog.spring.data.meilisearch.consumer.reactivecoexistence.ReactiveCoexistingRepository;
import io.vanslog.spring.data.meilisearch.core.ReactiveMeilisearchOperations;
import io.vanslog.spring.data.meilisearch.core.convert.MeilisearchConverter;
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTest;
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTestConfiguration;
import io.vanslog.spring.data.meilisearch.repository.config.EnableMeilisearchRepositories;
import io.vanslog.spring.data.meilisearch.repository.config.EnableReactiveMeilisearchRepositories;
import reactor.core.publisher.Flux;

/**
 * Both scanners discover the same package but execute only their own repository contracts.
 *
 * @author Junghoon Ban
 */
@MeilisearchTest
@ContextConfiguration(classes = { MeilisearchTestConfiguration.class,
		ReactiveRepositoryCoexistenceIntegrationTests.CoexistingConfiguration.class })
class ReactiveRepositoryCoexistenceIntegrationTests {

	private static final Duration TIMEOUT = Duration.ofSeconds(15);

	@Autowired BlockingCoexistingRepository blocking;

	@Autowired ReactiveCoexistingRepository reactive;

	@BeforeEach
	void clearDocuments() {
		reactive.deleteAll().block(TIMEOUT);
	}

	@Test // GH-215
	void bothFactoriesShareMappingAndObserveEachOthersCompletedWritesAndDeletes() {
		CoexistingDocument first = new CoexistingDocument("shared-1", "First", "fiction");
		CoexistingDocument second = new CoexistingDocument("shared-2", "Second", "fiction");
		reactive.save(first).block(TIMEOUT);
		assertThat(blocking.findById("shared-1")).get().extracting(CoexistingDocument::getTitle).isEqualTo("First");
		blocking.save(second);
		assertThat(reactive.findById("shared-2").block(TIMEOUT).getTitle()).isEqualTo("Second");
		assertThat(
				reactive.findByCategoryOrderByTitleAsc("fiction").map(CoexistingDocument::getId).collectList().block(TIMEOUT))
				.containsExactly("shared-1", "shared-2");
		reactive.deleteById("shared-1").block(TIMEOUT);
		assertThat(blocking.existsById("shared-1")).isFalse();
		blocking.deleteById("shared-2");
		assertThat(reactive.count().block(TIMEOUT)).isZero();
	}

	@Test // GH-215
	void reactiveIdLookupPreservesDuplicateOrderingAlongsideBlockingRepository() {
		reactive.saveAll(Flux.just(new CoexistingDocument("ordered-1", "One", "fiction"),
				new CoexistingDocument("ordered-2", "Two", "reference"))).then().block(TIMEOUT);
		assertThat(reactive.findAllById(Flux.just("ordered-2", "absent", "ordered-1", "ordered-2"))
				.map(CoexistingDocument::getId).collectList().block(TIMEOUT))
				.containsExactly("ordered-2", "ordered-1", "ordered-2");
		assertThat(blocking.findById("ordered-2")).get().extracting(CoexistingDocument::getCategory).isEqualTo("reference");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableMeilisearchRepositories(basePackageClasses = BlockingCoexistingRepository.class)
	@EnableReactiveMeilisearchRepositories(basePackageClasses = ReactiveCoexistingRepository.class,
			reactiveMeilisearchTemplateRef = "coexistingReactiveOperations")
	static class CoexistingConfiguration {

		@Bean("coexistingReactiveOperations")
		ReactiveMeilisearchOperations reactiveOperations(
				@Qualifier("meilisearchClientConfiguration") ClientConfiguration configuration, MeilisearchConverter converter,
				@Qualifier("meilisearchObjectMapper") ObjectMapper mapper) {
			return new ReactiveMeilisearchTemplate(configuration, converter, mapper, 7);
		}

	}

}
