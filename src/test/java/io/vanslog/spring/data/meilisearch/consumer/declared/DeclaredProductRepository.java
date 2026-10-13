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
package io.vanslog.spring.data.meilisearch.consumer.declared;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import io.vanslog.spring.data.meilisearch.repository.MeilisearchRepository;
import io.vanslog.spring.data.meilisearch.repository.Query;

/**
 * Searches declared with annotation and properties templates.
 *
 * @author Junghoon Ban
 */
public interface DeclaredProductRepository extends MeilisearchRepository<DeclaredProduct, String> {

	@Query(q = "?0", filter = "category = ?1 AND price <= ?2")
	Page<DeclaredProduct> search(String keyword, String category, int maxPrice, Pageable pageable);

	@Query("title = ?0")
	Optional<DeclaredProduct> byTitle(String title);

	@Query(filter = "category IN ?0 AND active = ?1")
	List<DeclaredProduct> inCategories(Collection<String> categories, boolean active, Sort sort);

	@Query(q = "?0")
	List<DeclaredProduct> fullText(String keyword);

	@Query(filter = "category = ?0")
	List<DeclaredProduct> findByTitle(String category);

	@Query(filter = "category = ?1", q = "?0")
	List<DeclaredProduct> preferred(String keyword, String category);

	@Query(filter = "price ?0 TO ?1 AND title STARTS WITH ?2")
	List<DeclaredProduct> inRange(int minimum, int maximum, String prefix);

	List<DeclaredProduct> namedSearch(String keyword, String category);

	Iterable<DeclaredProduct> namedFilter(String category);

	List<DeclaredProduct> namedText(String keyword);
}
