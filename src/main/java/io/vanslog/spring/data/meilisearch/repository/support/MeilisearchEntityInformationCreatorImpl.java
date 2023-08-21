package io.vanslog.spring.data.meilisearch.repository.support;

import io.vanslog.spring.data.meilisearch.core.mapping.MeilisearchPersistentEntity;
import io.vanslog.spring.data.meilisearch.core.mapping.MeilisearchPersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.util.Assert;

/**
 * Implementation of {@link io.vanslog.spring.data.meilisearch.repository.support.MeilisearchEntityInformationCreator}.
 *
 * @author Junghoon Ban
 */
public class MeilisearchEntityInformationCreatorImpl
        implements MeilisearchEntityInformationCreator {

    private final MappingContext<? extends MeilisearchPersistentEntity<?>, MeilisearchPersistentProperty>
            mappingContext;

    /**
     * Creates a new {@link MeilisearchEntityInformationCreatorImpl}
     * for the given {@link org.springframework.data.mapping.context.MappingContext}.
     *
     * @param mappingContext must not be {@literal null}.
     */
    public MeilisearchEntityInformationCreatorImpl(
            MappingContext<? extends MeilisearchPersistentEntity<?>, MeilisearchPersistentProperty> mappingContext) {

        Assert.notNull(mappingContext, "MappingContext must not be null!");

        this.mappingContext = mappingContext;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T, ID> MeilisearchEntityInformation<T, ID> getEntityInformation(
            Class<T> domainClass) {

        MeilisearchPersistentEntity<T> persistentEntity =
                (MeilisearchPersistentEntity<T>) mappingContext
                        .getRequiredPersistentEntity(domainClass);

        Assert.notNull(persistentEntity,
                String.format("Unable to obtain mapping metadata for %s!",
                        domainClass));
        Assert.notNull(persistentEntity.getIdProperty(),
                String.format("No id property found for %s!", domainClass));

        return new MappingMeilisearchEntityInformation<>(persistentEntity);
    }
}
