package io.vanslog.spring.data.meilisearch.core.mapping;

import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;

/**
 * Meilisearch specific {@link AbstractMappingContext} implementation.
 *
 * @author Junghoon Ban
 * @since 1.0.0
 */
public class SimpleMeilisearchMappingContext extends
        AbstractMappingContext<SimpleMeilisearchPersistentEntity<?>,
                MeilisearchPersistentProperty> {

    @Override
    protected <T> SimpleMeilisearchPersistentEntity<?> createPersistentEntity(
            TypeInformation<T> typeInformation) {
        return new SimpleMeilisearchPersistentEntity<T>(typeInformation);
    }

    @Override
    protected MeilisearchPersistentProperty createPersistentProperty(
            Property property, SimpleMeilisearchPersistentEntity<?> owner,
            SimpleTypeHolder simpleTypeHolder) {
        return new SimpleMeilisearchPersistentProperty(property, owner,
                simpleTypeHolder);
    }
}
