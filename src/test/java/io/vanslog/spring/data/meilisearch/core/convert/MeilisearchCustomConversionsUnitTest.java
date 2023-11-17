package io.vanslog.spring.data.meilisearch.core.convert;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import io.vanslog.spring.data.meilisearch.config.MeilisearchConfigurationSupport;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.convert.Jsr310Converters;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
public class MeilisearchCustomConversionsUnitTest {

	@Autowired MeilisearchConverter meilisearchConverter;

	@Test
	void shouldRegisterAndUseCustomConverter() {
		LocalDateTime toConvert = LocalDateTime.of(2009, 1, 3, 18, 15, 5);
		LocalDate converted = meilisearchConverter.getConversionService().convert(toConvert, LocalDate.class);
		
		assertThat(converted).isEqualTo(LocalDate.of(2009, 1, 3));
	}

	@Configuration
	static class Config extends MeilisearchConfigurationSupport {

		@Bean
		public MeilisearchCustomConversions meilisearchCustomConversions() {
			return new MeilisearchCustomConversions(Jsr310Converters.getConvertersToRegister());
		}
	}
}
