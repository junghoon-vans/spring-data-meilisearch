package io.vanslog.spring.data.meilisearch.repository.support;

import org.springframework.data.repository.core.EntityInformation;

/**
 * Entity information for Meilisearch.
 *
 * @param <T>  the type of the entity
 * @param <ID> the type of the entity's identifier
 * @author Junghoon Ban
 */
public interface MeilisearchEntityInformation<T, ID>
        extends EntityInformation<T, ID> {

    /**
     * Returns the id attribute.
     *
     * @return Id attribute
     */
    String getIdAttribute();

    /**
     * Returns the uid of the index.
     *
     * @return Index UID
     */
    String getIndexUid();
}
