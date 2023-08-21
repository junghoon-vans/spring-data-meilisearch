package io.vanslog.spring.data.meilisearch.repository.config;

import java.lang.annotation.Annotation;
import org.springframework.data.repository.config.RepositoryBeanDefinitionRegistrarSupport;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

/**
 * Meilisearch specific {@link org.springframework.context.annotation.ImportBeanDefinitionRegistrar}.
 *
 * @author Junghoon Ban
 */
class MeilisearchRepositoriesRegistrar
        extends RepositoryBeanDefinitionRegistrarSupport {

    @Override
    protected Class<? extends Annotation> getAnnotation() {
        return EnableMeilisearchRepositories.class;
    }

    @Override
    protected RepositoryConfigurationExtension getExtension() {
        return new MeilisearchRepositoryConfigExtension();
    }
}
