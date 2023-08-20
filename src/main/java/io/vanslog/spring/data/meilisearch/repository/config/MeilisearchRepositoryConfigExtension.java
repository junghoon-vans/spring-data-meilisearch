package io.vanslog.spring.data.meilisearch.repository.config;

import io.vanslog.spring.data.meilisearch.repository.support.MeilisearchRepositoryFactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.config.XmlRepositoryConfigurationSource;
import org.w3c.dom.Element;

/**
 * {@link org.springframework.data.repository.config.RepositoryConfigurationExtension}
 * implementation to configure Meilisearch repository configuration support.
 *
 * @author Junghoon Ban
 */
public class MeilisearchRepositoryConfigExtension
        extends RepositoryConfigurationExtensionSupport {

    private static final String MODULE_NAME = "Meilisearch";

    @Override
    public String getRepositoryFactoryBeanClassName() {
        return MeilisearchRepositoryFactoryBean.class.getName();
    }

    @Override
    protected String getModulePrefix() {
        return getModuleIdentifier();
    }

    @Override
    public String getModuleName() {
        return MODULE_NAME;
    }

    @Override
    public void postProcess(BeanDefinitionBuilder builder,
                            AnnotationRepositoryConfigurationSource config) {
        AnnotationAttributes attributes = config.getAttributes();
        builder.addPropertyReference("meilisearchOperations",
                attributes.getString("meilisearchTemplateRef"));
    }

    @Override
    public void postProcess(BeanDefinitionBuilder builder,
                            XmlRepositoryConfigurationSource config) {
        Element element = config.getElement();
        builder.addPropertyReference("meilisearchOperations",
                element.getAttribute("meilisearch-template-ref"));
    }
}
