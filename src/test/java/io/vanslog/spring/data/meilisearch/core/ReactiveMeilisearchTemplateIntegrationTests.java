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
package io.vanslog.spring.data.meilisearch.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Sort;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.test.context.ContextConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meilisearch.sdk.exceptions.MeilisearchApiException;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import com.meilisearch.sdk.model.TaskInfo;

import io.vanslog.spring.data.meilisearch.MeilisearchRestException;
import io.vanslog.spring.data.meilisearch.ReactiveTaskException;
import io.vanslog.spring.data.meilisearch.annotations.Document;
import io.vanslog.spring.data.meilisearch.annotations.Setting;
import io.vanslog.spring.data.meilisearch.client.ClientConfiguration;
import io.vanslog.spring.data.meilisearch.client.MeilisearchClient;
import io.vanslog.spring.data.meilisearch.client.msc.ReactiveMeilisearchTemplate;
import io.vanslog.spring.data.meilisearch.core.convert.MappingMeilisearchConverter;
import io.vanslog.spring.data.meilisearch.core.convert.MeilisearchConverter;
import io.vanslog.spring.data.meilisearch.core.convert.MeilisearchCustomConversions;
import io.vanslog.spring.data.meilisearch.core.mapping.SimpleMeilisearchMappingContext;
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTest;
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTestConfiguration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Real Meilisearch coverage for reactive document behavior. Fixture indexes are unique to GH-214.
 *
 * @author Junghoon Ban
 */
@MeilisearchTest
@ContextConfiguration(classes = { MeilisearchTestConfiguration.class,
		ReactiveMeilisearchTemplateIntegrationTests.ReactiveTemplateConfiguration.class })
class ReactiveMeilisearchTemplateIntegrationTests {

	private static final String DOCUMENT_INDEX = "gh214-reactive-documents";
	private static final String CUSTOM_INDEX = "gh214-reactive-custom-mapping";
	private static final String MISSING_INDEX = "gh214-reactive-missing-index";

	@Autowired MeilisearchClient meilisearchClient;
	@Autowired
	@Qualifier("meilisearchClientConfiguration") ClientConfiguration clientConfiguration;
	@Autowired
	@Qualifier("meilisearchObjectMapper") ObjectMapper objectMapper;
	@Autowired ReactiveMeilisearchOperations operations;

	@BeforeEach
	void createDocumentIndex() throws MeilisearchException {
		deleteIndexIfExists(DOCUMENT_INDEX);
		deleteIndexIfExists(CUSTOM_INDEX);
		deleteIndexIfExists(MISSING_INDEX);
		createIndex(DOCUMENT_INDEX, "key");
		operations.applySettings(ReactiveEntry.class).block();
	}

	@AfterEach
	void deleteFixtureIndexes() throws MeilisearchException {
		deleteIndexIfExists(DOCUMENT_INDEX);
		deleteIndexIfExists(CUSTOM_INDEX);
		deleteIndexIfExists(MISSING_INDEX);
	}

	@Test
	void saveIsColdAndEachSubscriptionExecutesTheWrite() {
		ReactiveEntry entry = entry("cold-1", "Cold write", "fiction", 1);
		Mono<ReactiveEntry> write = operations.save(entry);

		assertThat(operations.count(ReactiveEntry.class).block()).isZero();
		StepVerifier.create(write).expectNext(entry).verifyComplete();
		operations.delete(entry).block();

		StepVerifier.create(write).expectNext(entry).verifyComplete();
		assertThat(operations.get("cold-1", ReactiveEntry.class).block().getTitle()).isEqualTo("Cold write");
	}

	@Test
	void savesInBoundedBatchesAndFindsSortedPagesAcrossBatchBoundaries() {
		List<ReactiveEntry> entries = IntStream.range(0, 19)
				.mapToObj(rank -> entry("rank-" + rank, "Entry " + rank, "fiction", rank)).toList();

		StepVerifier.create(operations.saveAll(Flux.fromIterable(entries))).expectNextSequence(entries).verifyComplete();

		List<ReactiveEntry> sorted = operations.findAll(ReactiveEntry.class, Sort.by(Sort.Direction.DESC, "rank"))
				.collectList().block();
		assertThat(sorted).extracting(ReactiveEntry::getRank)
				.containsExactlyElementsOf(IntStream.iterate(18, value -> value - 1).limit(19).boxed().toList());
	}

	@Test
	void multiGetKeepsRequestedOrderAndDuplicatesAndOmitsMissingIds() {
		List<ReactiveEntry> entries = IntStream.range(0, 10)
				.mapToObj(id -> entry("multi-" + id, "Entry " + id, "fiction", id)).toList();
		operations.saveAll(Flux.fromIterable(entries)).then().block();

		List<ReactiveEntry> found = operations.multiGet(ReactiveEntry.class,
				Flux.just("multi-9", "missing", "multi-1", "multi-9", "multi-2", "multi-3", "multi-4", "multi-5", "multi-9"))
				.collectList().block();

		assertThat(found).extracting(ReactiveEntry::getKey).containsExactly("multi-9", "multi-1", "multi-9", "multi-2",
				"multi-3", "multi-4", "multi-5", "multi-9");

		operations.deleteAllById(
				Flux.just("multi-0", "multi-1", "multi-3", "multi-4", "multi-5", "multi-6", "multi-7", "multi-8"),
				ReactiveEntry.class).block();
		assertThat(operations.count(ReactiveEntry.class).block()).isEqualTo(2L);
		assertThat(operations.exists("multi-2", ReactiveEntry.class).block()).isTrue();
	}

	@Test
	void countsAndDeletesFilteredDocumentsUsingCompletedTaskDetails() {
		operations.saveAll(Flux.just(entry("filter-1", "Novel", "fiction", 1), entry("filter-2", "Poem", "fiction", 2),
				entry("filter-3", "Story", "fiction", 3), entry("filter-4", "Manual", "reference", 4))).then().block();

		String filter = "category = \"fiction\"";
		assertThat(operations.count(ReactiveEntry.class, filter).block()).isEqualTo(3L);
		assertThat(operations.deleteByFilter(ReactiveEntry.class, filter).block()).isEqualTo(3L);
		assertThat(operations.count(ReactiveEntry.class).block()).isEqualTo(1L);
		assertThat(operations.exists("filter-4", ReactiveEntry.class).block()).isTrue();
	}

	@Test
	void failedBatchDoesNotEmitEntitiesBeforeTheTaskSucceeds() {
		ReactiveEntry valid = entry("failed-batch-1", "Valid row", "fiction", 1);
		ReactiveEntry withoutId = entry(null, "Missing key", "fiction", 2);

		StepVerifier.create(operations.saveAll(Flux.just(valid, withoutId))).expectErrorSatisfies(error -> {
			assertThat(error).isInstanceOf(ReactiveTaskException.class);
			assertThat(((ReactiveTaskException) error).getStatus()).isEqualToIgnoringCase("failed");
		}).verify();
		assertThat(operations.count(ReactiveEntry.class).block()).isZero();
	}

	@Test
	void missingDocumentIsEmptyButMissingIndexRemainsAnError() {
		StepVerifier.create(operations.get("absent", ReactiveEntry.class)).verifyComplete();

		StepVerifier.create(operations.get("absent", MissingIndexEntry.class)).expectErrorSatisfies(error -> {
			assertThat(error).isInstanceOf(MeilisearchRestException.class);
			assertThat(((MeilisearchRestException) error).getCode()).isEqualTo("index_not_found");
		}).verify();
	}

	@Test // GH-214
	void canceledBulkWriteDoesNotDrainAnUnboundedSourceOrSubmitAnotherBatch() {
		AtomicInteger generated = new AtomicInteger();
		Flux<ReactiveEntry> source = Flux.generate(sink -> {
			int rank = generated.getAndIncrement();
			sink.next(entry("bounded-" + rank, "Entry " + rank, "fiction", rank));
		});

		List<ReactiveEntry> written = operations.saveAll(source).take(7).collectList().block(Duration.ofSeconds(15));

		assertThat(written).extracting(ReactiveEntry::getKey).containsExactly("bounded-0", "bounded-1", "bounded-2",
				"bounded-3", "bounded-4", "bounded-5", "bounded-6");
		assertThat(generated.get()).isBetween(7, 14);
		assertThat(operations.count(ReactiveEntry.class).block()).isEqualTo(7L);
	}

	@Test
	void converterCustomMappingRoundTripsThroughTheReactiveDocumentApi() throws MeilisearchException {
		createIndex(CUSTOM_INDEX, "id");
		MappingMeilisearchConverter converter = new MappingMeilisearchConverter(new SimpleMeilisearchMappingContext());
		converter.setConversions(
				new MeilisearchCustomConversions(List.of(new PriceToDocumentConverter(), new DocumentToPriceConverter())));
		converter.afterPropertiesSet();
		ReactiveMeilisearchOperations mappedOperations = new ReactiveMeilisearchTemplate(clientConfiguration, converter,
				objectMapper, 7);
		CustomMappedEntry entry = new CustomMappedEntry("custom-1", new Price(new BigDecimal("42.75"), "USD"));

		mappedOperations.save(entry).block();
		CustomMappedEntry found = mappedOperations.get("custom-1", CustomMappedEntry.class).block();

		assertThat(found.getPrice()).isEqualTo(entry.getPrice());
	}

	private ReactiveEntry entry(String key, String title, String category, int rank) {
		return new ReactiveEntry(key, title, category, rank);
	}

	private void createIndex(String indexUid, String primaryKey) throws MeilisearchException {
		TaskInfo task = meilisearchClient.createIndex(indexUid, primaryKey);
		meilisearchClient.index(indexUid).waitForTask(task.getTaskUid(), meilisearchClient.getRequestTimeout(),
				meilisearchClient.getRequestInterval());
	}

	private void deleteIndexIfExists(String indexUid) throws MeilisearchException {
		try {
			TaskInfo task = meilisearchClient.deleteIndex(indexUid);
			meilisearchClient.index(indexUid).waitForTask(task.getTaskUid(), meilisearchClient.getRequestTimeout(),
					meilisearchClient.getRequestInterval());
		} catch (MeilisearchApiException e) {
			if (!"index_not_found".equals(e.getCode())) {
				throw e;
			}
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class ReactiveTemplateConfiguration {

		@Bean(name = { "reactiveMeilisearchOperations", "reactiveMeilisearchTemplate" })
		ReactiveMeilisearchOperations reactiveMeilisearchTemplate(
				@Qualifier("meilisearchClientConfiguration") ClientConfiguration clientConfiguration,
				MeilisearchConverter meilisearchConverter, @Qualifier("meilisearchObjectMapper") ObjectMapper objectMapper) {
			return new ReactiveMeilisearchTemplate(clientConfiguration, meilisearchConverter, objectMapper, 7);
		}
	}

	@Setting(filterableAttributes = { "category" }, sortableAttributes = { "rank" })
	@Document(indexUid = DOCUMENT_INDEX)
	static class ReactiveEntry {

		@Id private String key;
		private String title;
		private String category;
		private int rank;

		ReactiveEntry() {}

		ReactiveEntry(String key, String title, String category, int rank) {
			this.key = key;
			this.title = title;
			this.category = category;
			this.rank = rank;
		}

		public String getKey() {
			return key;
		}

		public String getTitle() {
			return title;
		}

		public String getCategory() {
			return category;
		}

		public int getRank() {
			return rank;
		}
	}

	@Document(indexUid = MISSING_INDEX)
	static class MissingIndexEntry {

		@Id private String id;

		public String getId() {
			return id;
		}
	}

	@Document(indexUid = CUSTOM_INDEX)
	static class CustomMappedEntry {

		@Id private String id;
		private Price price;

		CustomMappedEntry() {}

		CustomMappedEntry(String id, Price price) {
			this.id = id;
			this.price = price;
		}

		public String getId() {
			return id;
		}

		public Price getPrice() {
			return price;
		}
	}

	private record Price(BigDecimal amount, String currency) {
	}

	@WritingConverter
	private static class PriceToDocumentConverter
			implements Converter<Price, io.vanslog.spring.data.meilisearch.core.document.Document> {

		@Override
		public io.vanslog.spring.data.meilisearch.core.document.Document convert(Price source) {
			return io.vanslog.spring.data.meilisearch.core.document.Document.create()
					.append("amount", source.amount().toPlainString()).append("currency", source.currency());
		}
	}

	@ReadingConverter
	private static class DocumentToPriceConverter
			implements Converter<io.vanslog.spring.data.meilisearch.core.document.Document, Price> {

		@Override
		public Price convert(io.vanslog.spring.data.meilisearch.core.document.Document source) {
			return new Price(new BigDecimal((String) source.get("amount")), (String) source.get("currency"));
		}
	}
}
