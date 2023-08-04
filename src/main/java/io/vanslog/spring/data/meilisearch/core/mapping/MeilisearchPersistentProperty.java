package io.vanslog.spring.data.meilisearch.core.mapping;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mapping.PersistentProperty;

/**
 * Meilisearch specific {@link PersistentProperty} abstraction.
 *
 * @author Junghoon Ban
 */
public interface MeilisearchPersistentProperty
        extends PersistentProperty<MeilisearchPersistentProperty> {

    /**
     * Returns the name of the field a property is persisted to.
     * @return the field name
     */
    String getFieldName();

    enum PropertyToFieldNameConverter implements
            Converter<MeilisearchPersistentProperty, String> {

        INSTANCE;

        /**
         * Convert the persistent property into a Meilisearch field name.
         * @param source the persistent property
         * @return the field name
         */
        public String convert(MeilisearchPersistentProperty source) {
            return source.getFieldName();
        }
    }
}
