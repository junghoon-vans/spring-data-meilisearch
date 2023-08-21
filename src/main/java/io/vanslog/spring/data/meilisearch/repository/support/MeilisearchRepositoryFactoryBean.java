package io.vanslog.spring.data.meilisearch.repository.support;

import io.vanslog.spring.data.meilisearch.core.MeilisearchOperations;
import java.io.Serializable;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.beans.factory.FactoryBean} to create {@link org.springframework.data.repository.Repository} instances.
 *
 * @param <T>  The type of the repository interface.
 * @param <S>  The type of the entity managed by the repository.
 * @param <ID> The type of the identifier of the entity.
 * @author Junghoon Ban
 */
public class MeilisearchRepositoryFactoryBean<T extends Repository<S, ID>, S, ID extends Serializable>
        extends RepositoryFactoryBeanSupport<T, S, ID> {

    @Nullable
    private MeilisearchOperations meilisearchOperations;

    /**
     * Creates a new {@link RepositoryFactoryBeanSupport} for the given repository interface.
     *
     * @param repositoryInterface must not be {@literal null}.
     */
    protected MeilisearchRepositoryFactoryBean(
            Class<? extends T> repositoryInterface) {
        super(repositoryInterface);
    }

    /**
     * Configures the {@link MeilisearchOperations} to be used to create the repository.
     * @param meilisearchOperations operations to be used
     */
    public void setMeilisearchOperations(MeilisearchOperations meilisearchOperations) {
        Assert.notNull(meilisearchOperations, "MeilisearchOperations must not be null!");

        setMappingContext(meilisearchOperations.getMeilisearchConverter().getMappingContext());
        this.meilisearchOperations = meilisearchOperations;
    }

    @Override
    protected RepositoryFactorySupport createRepositoryFactory() {
        Assert.notNull(meilisearchOperations,
                "MeilisearchOperations must be configured!");
        return new MeilisearchRepositoryFactory(meilisearchOperations);
    }
}
