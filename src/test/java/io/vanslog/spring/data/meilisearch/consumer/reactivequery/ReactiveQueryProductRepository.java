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
package io.vanslog.spring.data.meilisearch.consumer.reactivequery;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import io.vanslog.spring.data.meilisearch.repository.Query;
import io.vanslog.spring.data.meilisearch.repository.ReactiveMeilisearchRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Consumer-facing query methods for reactive query integration tests.
 *
 * @author Junghoon Ban
 */
public interface ReactiveQueryProductRepository extends ReactiveMeilisearchRepository<ReactiveQueryProduct, String> {

	Flux<ReactiveQueryProduct> findByCategoryAndPriceLessThanEqual(String category, int maximumPrice);

	Flux<ReactiveQueryProduct> findByCategoryOrderByPriceDesc(String category);

	Flux<ReactiveQueryProduct> findByCategory(String category, Sort sort);

	Mono<ReactiveQueryProduct> findByTitle(String title);

	Mono<Long> countByCategory(String category);

	Mono<Boolean> existsByTitle(String title);

	Mono<Long> deleteByTitle(String title);

	Mono<Void> deleteByActiveFalse();

	@Query(filter = "title = ?0")
	Flux<ReactiveQueryProduct> byLiteralTitle(String title);

	@Query(q = "?0")
	Flux<ReactiveQueryProduct> fullText(String keyword);

	@Query(filter = "category = ?0")
	Flux<ReactiveQueryProduct> filterOnly(String category);

	@Query(filter = "category = ?1 AND price <= ?2", q = "?0")
	Flux<ReactiveQueryProduct> combined(String keyword, String category, int maximumPrice, Pageable pageable);

	Flux<ReactiveQueryProduct> namedSearch(String keyword, String category);

	@Query(filter = "category = ?1", q = "?0")
	Flux<ReactiveQueryProduct> preferred(String keyword, String category);

}
