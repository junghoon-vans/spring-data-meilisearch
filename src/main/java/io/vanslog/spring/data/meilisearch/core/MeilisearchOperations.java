/*
 * Copyright 2023-2024 the original author or authors.
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

import io.vanslog.spring.data.meilisearch.core.convert.MeilisearchConverter;

import java.util.List;

import org.springframework.lang.Nullable;

import com.meilisearch.sdk.SearchRequest;

/**
 * The operations for <a href="https://www.meilisearch.com/docs/reference/api/overview">Meilisearch APIs</a>.
 *
 * @author Junghoon Ban
 * @see com.meilisearch.sdk.Client
 * @see com.meilisearch.sdk.Index
 */
public interface MeilisearchOperations {

	/**
	 * Saves an entity.
	 *
	 * @param entity the entity to save
	 * @param <T> the type of the entity
	 * @return the saved entity
	 */
	<T> T save(T entity);

	/**
	 * Saves all given entities.
	 *
	 * @param entities the entities to save
	 * @param <T> the type of the entity
	 * @return the saved entities as list
	 */
	<T> List<T> save(List<T> entities);

	/**
	 * Retrieves an entity by its document id.
	 *
	 * @param documentId the document id of the entity
	 * @param clazz the entity class, must be annotated with
	 *          {@link io.vanslog.spring.data.meilisearch.annotations.Document}
	 * @param <T> the type of the entity
	 * @return the entity with the given document id or {@literal null} if none found
	 */
	@Nullable
	<T> T get(String documentId, Class<T> clazz);

	/**
	 * Retrieves all entities of the given type.
	 *
	 * @param clazz the entity class, must be annotated with
	 *          {@link io.vanslog.spring.data.meilisearch.annotations.Document}
	 * @param <T> the type of the entity
	 * @return all entities
	 */
	<T> List<T> multiGet(Class<T> clazz);

	/**
	 * Retrieves all entities of the given type with the given offset and limit.
	 * 
	 * @param clazz must not be {@literal null}.
	 * @param offset must not be {@literal null}.
	 * @param limit must not be {@literal null}.
	 * @return entities
	 * @param <T> entity type
	 */
	<T> List<T> multiGet(Class<T> clazz, int offset, int limit);

	/**
	 * Retrieves all entities of the given type with the given document ids.
	 *
	 * @param documentIds the document ids of the entities
	 * @param clazz the entity class, must be annotated with
	 *          {@link io.vanslog.spring.data.meilisearch.annotations.Document}
	 * @param <T> the type of the entity
	 * @return all entities with the given document ids
	 */
	<T> List<T> multiGet(Class<T> clazz, List<String> documentIds);

	/**
	 * Retrieves all entities of the given type with the given document ids and offset and limit.
	 * 
	 * @param clazz must not be {@literal null}.
	 * @param documentIds must not be {@literal null}.
	 * @param offset must not be {@literal null}.
	 * @param limit must not be {@literal null}.
	 * @return entities
	 * @param <T> entity type
	 */
	<T> List<T> multiGet(Class<T> clazz, List<String> documentIds, int offset, int limit);

	/**
	 * Checks whether an entity with the given document id exists.
	 *
	 * @param documentId the document id of the entity
	 * @param clazz the entity class, must be annotated with
	 *          {@link io.vanslog.spring.data.meilisearch.annotations.Document}
	 * @return {@literal true} if an entity with the given document id exists
	 */
	boolean exists(String documentId, Class<?> clazz);

	/**
	 * Returns the number of entities available.
	 *
	 * @param clazz the entity class, must be annotated with
	 *          {@link io.vanslog.spring.data.meilisearch.annotations.Document}
	 * @return the number of entities
	 */
	long count(Class<?> clazz);

	/**
	 * Deletes the entity with the given document id.
	 *
	 * @param documentId the document id of the entity
	 * @param clazz the entity class, must be annotated with
	 *          {@link io.vanslog.spring.data.meilisearch.annotations.Document}
	 * @return {@literal true} if an entity was deleted
	 */
	boolean delete(String documentId, Class<?> clazz);

	/**
	 * Deletes a given entity.
	 *
	 * @param entity the entity to delete
	 * @param <T> the type of the entity
	 * @return {@literal true} if an entity was deleted
	 */
	<T> boolean delete(T entity);

	/**
	 * Deletes all entities of the given type with the given document ids.
	 *
	 * @param clazz the entity class, must be annotated with
	 *          {@link io.vanslog.spring.data.meilisearch.annotations.Document}
	 * @param documentIds the document ids of the entities
	 * @return {@literal true} if all entities were deleted
	 */
	boolean delete(Class<?> clazz, List<String> documentIds);

	/**
	 * Deletes the given entities.
	 *
	 * @param entities the entities to delete
	 * @param <T> the type of the entity
	 * @return {@literal true} if all entities were deleted
	 */
	<T> boolean delete(List<T> entities);

	/**
	 * Deletes all entities of the given type.
	 *
	 * @param clazz the entity class, must be annotated with
	 *          {@link io.vanslog.spring.data.meilisearch.annotations.Document}
	 * @return {@literal true} if all entities were deleted
	 */
	boolean deleteAll(Class<?> clazz);

	/**
	 * Search for entities that meet the criteria using the given {@link SearchRequest}. Note that by default,
	 * {@literal MaxTotalHits} in {@link io.vanslog.spring.data.meilisearch.annotations.Pagination} is 1000. If you want
	 * to change the value, you can use {@link io.vanslog.spring.data.meilisearch.annotations.Setting}.
	 *
	 * @param searchRequest the search request
	 * @param clazz the entity class, must be annotated with
	 *          {@link io.vanslog.spring.data.meilisearch.annotations.Document}
	 * @return the entities found by the search request
	 * @param <T> the type of the entity
	 */
	<T> List<T> search(SearchRequest searchRequest, Class<?> clazz);

	/**
	 * Apply the default settings for the given entity class.
	 * 
	 * @param clazz the entity class, must be annotated with
	 *          {@link io.vanslog.spring.data.meilisearch.annotations.Document}
	 * @param <T> the type of the entity
	 */
	<T> void applySettings(Class<T> clazz);

	/**
	 * Return the {@link io.vanslog.spring.data.meilisearch.core.convert.MeilisearchConverter}.
	 *
	 * @return {@link io.vanslog.spring.data.meilisearch.core.convert.MeilisearchConverter}
	 */
	MeilisearchConverter getMeilisearchConverter();
}
