package io.vanslog.spring.data.meilisearch.core.convert;

import io.vanslog.spring.data.meilisearch.core.mapping.MeilisearchPersistentEntity;
import io.vanslog.spring.data.meilisearch.core.mapping.MeilisearchPersistentProperty;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.context.MappingContext;

/**
 * Meilisearch converter aware of {@link MappingContext}.
 *
 * @author Junghoon Ban
 * @see MappingContext
 */
public interface MeilisearchConverter {

    /**
     * Returns the {@link MappingContext} used by the converter.
     *
     * @return never {@literal null}.
     */
    MappingContext<? extends MeilisearchPersistentEntity<?>,
            MeilisearchPersistentProperty> getMappingContext();

    /**
     * Returns the {@link ConversionService} used by the converter.
     *
     * @return never {@literal null}.
     */
    ConversionService getConversionService();
}
