package io.vanslog.spring.data.meilisearch.repository.support;

import io.vanslog.spring.data.meilisearch.core.MeilisearchOperations;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

/**
 * Factory to create {@link SimpleMeilisearchRepository} instances.
 *
 * @author Junghoon Ban
 */
public class MeilisearchRepositoryFactory extends RepositoryFactorySupport {

    private final MeilisearchOperations meilisearchOperations;
    private final MeilisearchEntityInformationCreator entityInformationCreator;

    public MeilisearchRepositoryFactory(
            MeilisearchOperations meilisearchOperations) {
        this.meilisearchOperations = meilisearchOperations;
        this.entityInformationCreator =
                new MeilisearchEntityInformationCreatorImpl(
                        meilisearchOperations.getMeilisearchConverter()
                                .getMappingContext());
    }

    @Override
    public <T, ID> EntityInformation<T, ID> getEntityInformation(
            Class<T> domainClass) {
        return entityInformationCreator.getEntityInformation(domainClass);
    }

    @Override
    protected Object getTargetRepository(RepositoryInformation metadata) {
        return getTargetRepositoryViaReflection(metadata,
                getEntityInformation(metadata.getDomainType()),
                meilisearchOperations);
    }

    @Override
    protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
        return SimpleMeilisearchRepository.class;
    }
}
