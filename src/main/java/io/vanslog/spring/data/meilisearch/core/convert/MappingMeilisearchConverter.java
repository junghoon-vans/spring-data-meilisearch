package io.vanslog.spring.data.meilisearch.core.convert;

import io.vanslog.spring.data.meilisearch.core.mapping.MeilisearchPersistentEntity;
import io.vanslog.spring.data.meilisearch.core.mapping.MeilisearchPersistentProperty;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.util.Assert;

/**
 * MappingMeilisearchConverter
 *
 * @since 1.0.0
 * @author Junghoon Ban
 */
public class MappingMeilisearchConverter implements MeilisearchConverter, ApplicationContextAware {

  private final MappingContext<? extends MeilisearchPersistentEntity<?>, MeilisearchPersistentProperty> mappingContext;
  private final GenericConversionService conversionService;

  @SuppressWarnings("unused, FieldCanBeLocal")
  private ApplicationContext applicationContext;

  public MappingMeilisearchConverter(
      MappingContext<? extends MeilisearchPersistentEntity<?>, MeilisearchPersistentProperty> mappingContext) {
    Assert.notNull(mappingContext, "MappingContext must not be null!");
    this.mappingContext = mappingContext;
    this.conversionService = new DefaultConversionService();
  }

  @Override
  public MappingContext<? extends MeilisearchPersistentEntity<?>, MeilisearchPersistentProperty> getMappingContext() {
    return mappingContext;
  }

  @Override
  public ConversionService getConversionService() {
    return this.conversionService;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }
}
