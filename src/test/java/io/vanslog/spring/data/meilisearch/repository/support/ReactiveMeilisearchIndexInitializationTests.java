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
package io.vanslog.spring.data.meilisearch.repository.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;

import io.vanslog.spring.data.meilisearch.annotations.Document;
import io.vanslog.spring.data.meilisearch.annotations.Setting;
import io.vanslog.spring.data.meilisearch.core.ReactiveMeilisearchOperations;
import io.vanslog.spring.data.meilisearch.core.convert.MappingMeilisearchConverter;
import io.vanslog.spring.data.meilisearch.core.mapping.SimpleMeilisearchMappingContext;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

/**
 * Verifies lazy, successful-only, cancellation-aware index-settings initialization.
 *
 * @author Junghoon Ban
 */
class ReactiveMeilisearchIndexInitializationTests {

	@Test
	void initializationIsLazyAndCachesOnlySuccessfulSettingsApplication() {
		AtomicInteger applications = new AtomicInteger();
		MappingMeilisearchConverter converter = converter();
		ReactiveMeilisearchOperations operations = operations(converter,
				() -> Mono.defer(() -> applications.incrementAndGet() == 1
						? Mono.error(new IllegalStateException("settings task failed")) : Mono.empty()));
		ReactiveMeilisearchIndexInitialization initialization = new ReactiveMeilisearchIndexInitialization(
				SettingsEntry.class, operations);

		assertThat(applications).hasValue(0);
		StepVerifier.create(initialization.initialize()).expectErrorMessage("settings task failed").verify();
		assertThat(applications).hasValue(1);
		StepVerifier.create(initialization.initialize()).verifyComplete();
		StepVerifier.create(initialization.initialize()).verifyComplete();
		assertThat(applications).hasValue(2);
	}

	@Test
	void settingsPolicyFalseSkipsTheReactiveSettingsOperation() {
		AtomicInteger applications = new AtomicInteger();
		MappingMeilisearchConverter converter = converter();
		ReactiveMeilisearchOperations operations = operations(converter,
				() -> Mono.fromRunnable(applications::incrementAndGet));
		ReactiveMeilisearchIndexInitialization initialization = new ReactiveMeilisearchIndexInitialization(
				SettingsDisabledEntry.class, operations);

		StepVerifier.create(initialization.initialize()).verifyComplete();
		StepVerifier.create(initialization.initialize()).verifyComplete();

		assertThat(applications).hasValue(0);
	}

	@Test
	void concurrentWaitersShareOneAttemptAndOneCancellationDoesNotCancelTheOther() {
		AtomicInteger attempts = new AtomicInteger();
		AtomicInteger cancellations = new AtomicInteger();
		List<Sinks.One<Void>> taskSignals = new CopyOnWriteArrayList<>();
		MappingMeilisearchConverter converter = converter();
		ReactiveMeilisearchOperations operations = operations(converter, () -> Mono.defer(() -> {
			attempts.incrementAndGet();
			Sinks.One<Void> task = Sinks.one();
			taskSignals.add(task);
			return task.asMono().doOnCancel(cancellations::incrementAndGet);
		}));
		ReactiveMeilisearchIndexInitialization initialization = new ReactiveMeilisearchIndexInitialization(
				SettingsEntry.class, operations);
		AtomicBoolean secondCompleted = new AtomicBoolean();
		AtomicReference<Throwable> secondFailure = new AtomicReference<>();

		Disposable first = initialization.initialize().subscribe();
		Disposable second = initialization.initialize()
			.doOnSuccess(ignored -> secondCompleted.set(true))
			.doOnError(secondFailure::set)
			.subscribe();
		assertThat(attempts).hasValue(1);

		first.dispose();
		assertThat(cancellations).hasValue(0);
		taskSignals.get(0).tryEmitEmpty();
		assertThat(secondCompleted.get()).isTrue();
		assertThat(secondFailure.get()).isNull();
		StepVerifier.create(initialization.initialize()).verifyComplete();
		assertThat(attempts).hasValue(1);
		second.dispose();
	}

	@Test
	void cancellationOfEveryWaiterCancelsTheAttemptAndAllowsAnExplicitRetry() {
		AtomicInteger attempts = new AtomicInteger();
		AtomicInteger cancellations = new AtomicInteger();
		List<Sinks.One<Void>> taskSignals = new CopyOnWriteArrayList<>();
		MappingMeilisearchConverter converter = converter();
		ReactiveMeilisearchOperations operations = operations(converter, () -> Mono.defer(() -> {
			attempts.incrementAndGet();
			Sinks.One<Void> task = Sinks.one();
			taskSignals.add(task);
			return task.asMono().doOnCancel(cancellations::incrementAndGet);
		}));
		ReactiveMeilisearchIndexInitialization initialization = new ReactiveMeilisearchIndexInitialization(
				SettingsEntry.class, operations);

		Disposable first = initialization.initialize().subscribe();
		Disposable second = initialization.initialize().subscribe();
		first.dispose();
		second.dispose();

		assertThat(attempts).hasValue(1);
		assertThat(cancellations).hasValue(1);

		AtomicBoolean retried = new AtomicBoolean();
		initialization.initialize().doOnSuccess(ignored -> retried.set(true)).subscribe();
		assertThat(attempts).hasValue(2);
		taskSignals.get(1).tryEmitEmpty();
		assertThat(retried.get()).isTrue();
		StepVerifier.create(initialization.initialize()).verifyComplete();
		assertThat(attempts).hasValue(2);
	}

	@Test
	void aTimedOutSettingsTaskIsNotCachedAndItsObservationIsCancelled() {
		AtomicInteger attempts = new AtomicInteger();
		AtomicInteger cancellations = new AtomicInteger();
		MappingMeilisearchConverter converter = converter();
		ReactiveMeilisearchOperations operations = operations(converter, () -> Mono.defer(() -> {
			attempts.incrementAndGet();
			return Mono.<Void>never().doOnCancel(cancellations::incrementAndGet).timeout(Duration.ofMillis(10));
		}));
		ReactiveMeilisearchIndexInitialization initialization = new ReactiveMeilisearchIndexInitialization(
				SettingsEntry.class, operations);

		StepVerifier.create(initialization.initialize())
			.expectError(TimeoutException.class)
			.verify(Duration.ofSeconds(2));
		StepVerifier.create(initialization.initialize())
			.expectError(TimeoutException.class)
			.verify(Duration.ofSeconds(2));

		assertThat(attempts).hasValue(2);
		assertThat(cancellations).hasValue(2);
	}

	@Test
	void simpleRepositoryConstructionDoesNotStartSettingsIO() {
		AtomicInteger converterReads = new AtomicInteger();
		AtomicInteger applications = new AtomicInteger();
		MappingMeilisearchConverter converter = converter();
		ReactiveMeilisearchOperations operations = operations(converter,
				() -> Mono.fromRunnable(applications::incrementAndGet), converterReads);
		var entityInformation = new MeilisearchEntityInformationCreatorImpl(converter.getMappingContext())
			.getEntityInformation(SettingsEntry.class);

		new SimpleReactiveMeilisearchRepository<>(entityInformation, operations);

		assertThat(converterReads).hasValue(0);
		assertThat(applications).hasValue(0);
	}

	private static MappingMeilisearchConverter converter() {
		MappingMeilisearchConverter converter = new MappingMeilisearchConverter(new SimpleMeilisearchMappingContext());
		converter.afterPropertiesSet();
		return converter;
	}

	private static ReactiveMeilisearchOperations operations(MappingMeilisearchConverter converter,
			Supplier<Mono<Void>> applySettings) {
		return operations(converter, applySettings, new AtomicInteger());
	}

	private static ReactiveMeilisearchOperations operations(MappingMeilisearchConverter converter,
			Supplier<Mono<Void>> applySettings, AtomicInteger converterReads) {
		return (ReactiveMeilisearchOperations) java.lang.reflect.Proxy.newProxyInstance(
				ReactiveMeilisearchOperations.class.getClassLoader(),
				new Class<?>[] { ReactiveMeilisearchOperations.class }, (proxy, method, arguments) -> {
					if (method.getName().equals("getMeilisearchConverter")) {
						converterReads.incrementAndGet();
						return converter;
					}
					if (method.getName().equals("applySettings")) {
						return applySettings.get();
					}
					if (method.getName().equals("toString")) {
						return "ReactiveMeilisearchOperations test double";
					}
					throw new UnsupportedOperationException(method.toString());
				});
	}

	@Setting(filterableAttributes = { "category" })
	@Document(indexUid = "gh215-initialization")
	static class SettingsEntry {

		@Id
		private String id;

		private String category;

	}

	@Setting(filterableAttributes = { "category" })
	@Document(indexUid = "gh215-initialization-disabled", applySettings = false)
	static class SettingsDisabledEntry {

		@Id
		private String id;

		private String category;

	}

}
