package io.vanslog.spring.data.meilisearch.repository.support;

/**
 * Interface to create {@link io.vanslog.spring.data.meilisearch.repository.support.MeilisearchEntityInformation}.
 *
 * @author Junghoon Ban
 */
public interface MeilisearchEntityInformationCreator {

    /**
     * Returns {@link io.vanslog.spring.data.meilisearch.repository.support.MeilisearchEntityInformation} for the given domain class.
     *
     * @param domainClass must not be {@literal null}.
     * @param <T>         the type of the entity
     * @param <ID>        the type of the entity's identifier
     * @return {@link io.vanslog.spring.data.meilisearch.repository.support.MeilisearchEntityInformation}
     */
    <T, ID> MeilisearchEntityInformation<T, ID> getEntityInformation(
            Class<T> domainClass);
}
