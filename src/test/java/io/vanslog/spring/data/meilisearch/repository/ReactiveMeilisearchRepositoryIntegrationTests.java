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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meilisearch.sdk.exceptions.MeilisearchApiException;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import com.meilisearch.sdk.model.TaskInfo;

import io.vanslog.spring.data.meilisearch.ReactiveTaskException;
import io.vanslog.spring.data.meilisearch.client.ClientConfiguration;
import io.vanslog.spring.data.meilisearch.client.MeilisearchClient;
import io.vanslog.spring.data.meilisearch.consumer.reactivecrud.ReactiveCrudEntry;
import io.vanslog.spring.data.meilisearch.consumer.reactivecrud.ReactiveCrudEntryRepository;
import io.vanslog.spring.data.meilisearch.core.ReactiveMeilisearchOperations;
import io.vanslog.spring.data.meilisearch.core.convert.MeilisearchConverter;
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTest;
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTestConfiguration;
import io.vanslog.spring.data.meilisearch.repository.config.EnableReactiveMeilisearchRepositories;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Exercises reactive CRUD through the public repository proxy against a real Meilisearch
 * index.
 *
 * @author Junghoon Ban
 */
@MeilisearchTest
@ContextConfiguration(classes = ReactiveMeilisearchRepositoryIntegrationTests.Config.class)
class ReactiveMeilisearchRepositoryIntegrationTests {

	private static final String INDEX_UID = "gh215-reactive-crud";

	@Autowired
	MeilisearchClient meilisearchClient;

	@Autowired
	ReactiveMeilisearchOperations operations;

	@Autowired
	ReactiveCrudEntryRepository repository;

	@BeforeEach
	void createIndex() throws MeilisearchException {
		deleteIndexIfExists();
		TaskInfo task = meilisearchClient.createIndex(INDEX_UID, "id");
		meilisearchClient.index(INDEX_UID)
			.waitForTask(task.getTaskUid(), meilisearchClient.getRequestTimeout(),
					meilisearchClient.getRequestInterval());
	}

	@AfterEach
	void deleteIndex() throws MeilisearchException {
		deleteIndexIfExists();
	}

	@Test
	void repositoryPerformsColdCrudSortedReadsTaskFailureAndBoundedBulkCancellation() {
		ReactiveCrudEntry first = entry("first", "First", "fiction", 1);
		ReactiveCrudEntry second = entry("second", "Second", "fiction", 2);
		ReactiveCrudEntry third = entry("third", "Third", "reference", 3);
		ReactiveCrudEntry fourth = entry("fourth", "Fourth", "fiction", 4);

		Mono<ReactiveCrudEntry> coldSave = repository.save(first);
		assertThat(operations.count(ReactiveCrudEntry.class).block()).isZero();
		StepVerifier.create(coldSave).expectNext(first).verifyComplete();

		assertThat(repository.saveAll(List.of(second, third)).collectList().block()).containsExactly(second, third);
		assertThat(repository.saveAll(Flux.just(fourth)).collectList().block()).containsExactly(fourth);

		assertThat(repository.findById(first.getId()).block().getId()).isEqualTo(first.getId());
		assertThat(repository.findById(Flux.just(second.getId(), third.getId())).block().getId())
			.isEqualTo(second.getId());
		assertThat(repository.existsById(third.getId()).block()).isTrue();
		assertThat(repository.existsById(Flux.just(third.getId(), first.getId())).block()).isTrue();
		assertThat(repository.findAllById(List.of(third.getId(), missingId(), first.getId(), third.getId()))
			.collectList()
			.block()).extracting(ReactiveCrudEntry::getId).containsExactly(third.getId(), first.getId(), third.getId());
		assertThat(repository.findAllById(Flux.just(second.getId(), missingId(), fourth.getId())).collectList().block())
			.extracting(ReactiveCrudEntry::getId)
			.containsExactly(second.getId(), fourth.getId());
		assertThat(repository.findAll().collectList().block()).extracting(ReactiveCrudEntry::getId)
			.containsExactlyInAnyOrder(first.getId(), second.getId(), third.getId(), fourth.getId());
		assertThat(repository.findAll(Sort.by(Sort.Direction.DESC, "rank")).collectList().block())
			.extracting(ReactiveCrudEntry::getRank)
			.containsExactly(4, 3, 2, 1);
		assertThat(repository.count().block()).isEqualTo(4L);

		repository.deleteById(first.getId()).block();
		repository.delete(second).block();
		repository.deleteById(Mono.just(third.getId())).block();
		repository.deleteAllById(List.of(fourth.getId())).block();
		assertThat(repository.count().block()).isZero();

		repository.saveAll(List.of(first, second)).then().block();
		repository.deleteAll(Flux.just(first, second)).block();
		assertThat(repository.count().block()).isZero();
		repository.save(fourth).block();
		repository.deleteAll().block();
		assertThat(repository.count().block()).isZero();

		ReactiveCrudEntry invalid = new ReactiveCrudEntry(null, "Invalid", "fiction", 5);
		StepVerifier.create(repository.save(invalid)).expectErrorSatisfies(error -> {
			assertThat(error).isInstanceOf(ReactiveTaskException.class);
			assertThat(((ReactiveTaskException) error).getStatus()).isEqualToIgnoringCase("failed");
		}).verify(Duration.ofSeconds(30));
		assertThat(repository.count().block()).isZero();

		AtomicInteger generated = new AtomicInteger();
		Flux<ReactiveCrudEntry> unbounded = Flux.generate(sink -> {
			int rank = generated.getAndIncrement();
			sink.next(entry("bulk-" + rank, "Bulk " + rank, "fiction", rank));
		});
		List<ReactiveCrudEntry> written = repository.saveAll(unbounded)
			.take(7)
			.collectList()
			.block(Duration.ofSeconds(30));
		assertThat(written).hasSize(7);
		assertThat(generated.get()).isBetween(7, 14);
		assertThat(repository.count().block()).isEqualTo(7L);
	}

	private ReactiveCrudEntry entry(String id, String title, String category, int rank) {
		return new ReactiveCrudEntry(Integer.toUnsignedLong(id.hashCode()), title, category, rank);
	}

	private Long missingId() {
		return Long.MAX_VALUE;
	}

	private void deleteIndexIfExists() throws MeilisearchException {
		try {
			TaskInfo task = meilisearchClient.deleteIndex(INDEX_UID);
			meilisearchClient.index(INDEX_UID)
				.waitForTask(task.getTaskUid(), meilisearchClient.getRequestTimeout(),
						meilisearchClient.getRequestInterval());
		}
		catch (MeilisearchApiException error) {
			if (!"index_not_found".equals(error.getCode())) {
				throw error;
			}
		}
	}

	@Configuration(proxyBeanMethods = false)
	@Import(MeilisearchTestConfiguration.class)
	@EnableReactiveMeilisearchRepositories(basePackageClasses = ReactiveCrudEntryRepository.class)
	static class Config {

		@Bean(name = { "reactiveMeilisearchOperations", "reactiveMeilisearchTemplate" })
		ReactiveMeilisearchOperations reactiveMeilisearchOperations(
				@Qualifier("meilisearchClientConfiguration") ClientConfiguration clientConfiguration,
				MeilisearchConverter meilisearchConverter,
				@Qualifier("meilisearchObjectMapper") ObjectMapper objectMapper) {
			return new io.vanslog.spring.data.meilisearch.client.msc.ReactiveMeilisearchTemplate(clientConfiguration,
					meilisearchConverter, objectMapper, 7);
		}

	}

}
