package io.vanslog.spring.data.meilisearch.repository.support;

import io.vanslog.spring.data.meilisearch.core.mapping.MeilisearchPersistentEntity;
import org.springframework.data.repository.core.support.PersistentEntityInformation;

/**
 * Implementation of {@link io.vanslog.spring.data.meilisearch.repository.support.MeilisearchEntityInformation}.
 *
 * @param <T>  the type of the entity
 * @param <ID> the type of the entity's identifier
 * @author Junghoon Ban
 */
public class MappingMeilisearchEntityInformation<T, ID>
        extends PersistentEntityInformation<T, ID>
        implements MeilisearchEntityInformation<T, ID> {

    private final MeilisearchPersistentEntity<T> persistentEntity;

    /**
     * Creates a new {@link MappingMeilisearchEntityInformation} for the given {@link MeilisearchPersistentEntity}.
     *
     * @param persistentEntity must not be {@literal null}.
     */
    public MappingMeilisearchEntityInformation(
            MeilisearchPersistentEntity<T> persistentEntity) {
        super(persistentEntity);
        this.persistentEntity = persistentEntity;
    }

    @Override
    public String getIdAttribute() {
        return persistentEntity.getRequiredIdProperty().getFieldName();
    }

    @Override
    public String getIndexUid() {
        return persistentEntity.getIndexUid();
    }
}
