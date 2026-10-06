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

import org.springframework.util.Assert;

import io.vanslog.spring.data.meilisearch.core.ReactiveMeilisearchOperations;
import io.vanslog.spring.data.meilisearch.core.mapping.MeilisearchPersistentEntity;
import reactor.core.publisher.Mono;

/**
 * Lazily applies a domain type's default settings once, sharing an active attempt without
 * retaining failed or abandoned attempts.
 *
 * @author Junghoon Ban
 */
final class ReactiveMeilisearchIndexInitialization {

	private final Mono<Void> initialization;

	ReactiveMeilisearchIndexInitialization(Class<?> domainType, ReactiveMeilisearchOperations operations) {

		Assert.notNull(domainType, "Domain type must not be null");
		Assert.notNull(operations, "ReactiveMeilisearchOperations must not be null");

		this.initialization = Mono.defer(() -> initialize(domainType, operations))
			.thenReturn(Boolean.TRUE)
			.cacheInvalidateIf(ignored -> false)
			.then();
	}

	Mono<Void> initialize() {
		return initialization;
	}

	private static Mono<Void> initialize(Class<?> domainType, ReactiveMeilisearchOperations operations) {

		MeilisearchPersistentEntity<?> entity = operations.getMeilisearchConverter()
			.getMappingContext()
			.getRequiredPersistentEntity(domainType);

		return entity.isApplySettings() ? applySettings(operations, domainType) : Mono.empty();
	}

	private static <T> Mono<Void> applySettings(ReactiveMeilisearchOperations operations, Class<T> domainType) {
		return operations.applySettings(domainType);
	}

}
