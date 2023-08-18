package io.vanslog.spring.data.meilisearch.core;

import io.vanslog.spring.data.meilisearch.core.convert.MeilisearchConverter;
import java.util.List;
import org.springframework.lang.Nullable;

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
     * @param <T>    the type of the entity
     * @return the saved entity
     */
    <T> T save(T entity);

    /**
     * Saves all given entities.
     *
     * @param entities the entities to save
     * @param <T>      the type of the entity
     * @return the saved entities as list
     */
    <T> List<T> save(List<T> entities);

    /**
     * Retrieves an entity by its document id.
     *
     * @param documentId the document id of the entity
     * @param clazz      the entity class, must be annotated with {@link io.vanslog.spring.data.meilisearch.annotations.Document}
     * @param <T>        the type of the entity
     * @return the entity with the given document id or {@literal null} if none found
     */
    @Nullable
    <T> T get(String documentId, Class<T> clazz);

    /**
     * Retrieves all entities of the given type.
     *
     * @param clazz the entity class, must be annotated with {@link io.vanslog.spring.data.meilisearch.annotations.Document}
     * @param <T>   the type of the entity
     * @return all entities
     */
    <T> List<T> multiGet(Class<T> clazz);

    /**
     * Retrieves all entities of the given type with the given document ids.
     *
     * @param documentIds the document ids of the entities
     * @param clazz       the entity class, must be annotated with {@link io.vanslog.spring.data.meilisearch.annotations.Document}
     * @param <T>         the type of the entity
     * @return all entities with the given document ids
     */
    <T> List<T> multiGet(Class<T> clazz, List<String> documentIds);

    /**
     * Checks whether an entity with the given document id exists.
     *
     * @param documentId the document id of the entity
     * @param clazz      the entity class, must be annotated with {@link io.vanslog.spring.data.meilisearch.annotations.Document}
     * @return {@literal true} if an entity with the given document id exists
     */
    boolean exists(String documentId, Class<?> clazz);

    /**
     * Returns the number of entities available.
     *
     * @param clazz the entity class, must be annotated with {@link io.vanslog.spring.data.meilisearch.annotations.Document}
     * @return the number of entities
     */
    long count(Class<?> clazz);

    /**
     * Deletes the entity with the given document id.
     *
     * @param documentId the document id of the entity
     * @param clazz      the entity class, must be annotated with {@link io.vanslog.spring.data.meilisearch.annotations.Document}
     * @return {@literal true} if an entity was deleted
     */
    boolean delete(String documentId, Class<?> clazz);

    /**
     * Deletes a given entity.
     *
     * @param entity the entity to delete
     * @param <T>    the type of the entity
     * @return {@literal true} if an entity was deleted
     */
    <T> boolean delete(T entity);

    /**
     * Deletes all entities of the given type with the given document ids.
     *
     * @param clazz       the entity class, must be annotated with {@link io.vanslog.spring.data.meilisearch.annotations.Document}
     * @param documentIds the document ids of the entities
     * @return {@literal true} if all entities were deleted
     */
    boolean delete(Class<?> clazz, List<String> documentIds);

    /**
     * Deletes the given entities.
     *
     * @param entities the entities to delete
     * @param <T>      the type of the entity
     * @return {@literal true} if all entities were deleted
     */
    <T> boolean delete(List<T> entities);

    /**
     * Deletes all entities of the given type.
     *
     * @param clazz the entity class, must be annotated with {@link io.vanslog.spring.data.meilisearch.annotations.Document}
     * @return {@literal true} if all entities were deleted
     */
    boolean deleteAll(Class<?> clazz);

    /**
     * Return the {@link MeilisearchConverter}.
     *
     * @return {@link MeilisearchConverter}
     */
    MeilisearchConverter getMeilisearchConverter();
}
