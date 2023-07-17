package io.vanslog.spring.data.meilisearch.core.convert;

import io.vanslog.spring.data.meilisearch.core.mapping.SimpleMeilisearchMappingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MappingMeilisearchConverter}.
 *
 * @author Junghoon Ban
 */
class MappingMeilisearchConverterTest {

  private MappingMeilisearchConverter converter;

  @BeforeEach
  void setUp() {
    converter = new MappingMeilisearchConverter(new SimpleMeilisearchMappingContext());
  }

  @Test
  void shouldReturnMappingContext() {
    assertThat(converter.getMappingContext()).isNotNull();
  }

  @Test
  void shouldReturnConversionService() {
    assertThat(converter.getConversionService()).isNotNull();
  }
}
