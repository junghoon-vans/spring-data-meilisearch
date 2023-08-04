package io.vanslog.spring.data.meilisearch.core.mapping;

import org.springframework.data.mapping.PersistentEntity;

/**
 * Meilisearch specific {@link PersistentEntity} abstraction.
 *
 * @param <T>
 * @author Junghoon Ban
 * @since 1.0.0
 */
public interface MeilisearchPersistentEntity<T>
        extends PersistentEntity<T, MeilisearchPersistentProperty> {

    /**
     * Returns the Index UID of the persistent entity.
     * @return Index UID
     */
    String getIndexUid();
}
