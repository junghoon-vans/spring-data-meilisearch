package io.vanslog.spring.data.meilisearch.core.mapping;

import org.springframework.data.mapping.PersistentEntity;

/**
 * Meilisearch specific {@link PersistentEntity} abstraction.
 *
 * @param <T>
 * @since 1.0.0
 * @author Junghoon Ban
 */
public interface MeilisearchPersistentEntity<T> extends PersistentEntity<T, MeilisearchPersistentProperty> {

}
