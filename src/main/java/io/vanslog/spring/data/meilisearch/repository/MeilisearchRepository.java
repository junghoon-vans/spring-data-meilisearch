/*
 * Copyright 2023-2026 the original author or authors.
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

import org.springframework.data.domain.Sort;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * Repository interface for Meilisearch.
 *
 * @param <T> The type of the domain class
 * @param <ID> The type of the id field
 * @author Junghoon Ban
 * @see org.springframework.data.repository.CrudRepository
 */
@NoRepositoryBean
public interface MeilisearchRepository<T, ID> extends CrudRepository<T, ID>, PagingAndSortingRepository<T, ID> {

	/**
	 * Retrieves documents by ID using native ID-list fetch, available from Meilisearch 1.14.
	 *
	 * @param ids the document IDs to retrieve
	 * @return documents found for the requested IDs
	 */
	@Override
	Iterable<T> findAllById(Iterable<ID> ids);

	/**
	 * Retrieves every document in batches. When sorting is requested, the server's document sort option requires
	 * Meilisearch 1.16 or later. On Meilisearch 1.16 through 1.26, sorted pagination may duplicate or omit documents
	 * across batches; use Meilisearch 1.27 or later for reliable sorted retrieval.
	 *
	 * @param sort the sort order; attributes must be sortable in the index settings
	 * @return the documents in the requested order when sorted
	 * @see <a href="https://github.com/meilisearch/meilisearch/issues/5943">Meilisearch sorted pagination issue</a>
	 */
	@Override
	Iterable<T> findAll(Sort sort);

}
