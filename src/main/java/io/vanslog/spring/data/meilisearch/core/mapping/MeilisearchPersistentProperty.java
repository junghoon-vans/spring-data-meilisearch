package io.vanslog.spring.data.meilisearch.core.mapping;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mapping.PersistentProperty;

/**
 * Meilisearch specific {@link PersistentProperty} abstraction.
 *
 * @since 1.0.0
 * @author Junghoon Ban
 */
public interface MeilisearchPersistentProperty extends PersistentProperty<MeilisearchPersistentProperty> {
  String getFieldName();

  public enum PropertyToFieldNameConverter implements
      Converter<MeilisearchPersistentProperty, String> {

    INSTANCE;

    public String convert(MeilisearchPersistentProperty source) {
      return source.getFieldName();
    }
  }
}
