/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.vanslog.spring.data.meilisearch.core.convert;

import java.time.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

/**
 * Helper class that provides converters for JSR-310 date/time types to String and back.
 *
 * @author Junghoon Ban
 */
public abstract class Jsr310Converters {

	/**
	 * Returns the converters to be registered.
	 *
	 * @return the converters to register.
	 */
	public static Collection<Converter<?, ?>> getConvertersToRegister() {
		List<Converter<?, ?>> converters = new ArrayList<>();

		converters.add(LocalDateTimeToStringConverter.INSTANCE);
		converters.add(StringToLocalDateTimeConverter.INSTANCE);
		converters.add(LocalDateToStringConverter.INSTANCE);
		converters.add(StringToLocalDateConverter.INSTANCE);
		converters.add(LocalTimeToStringConverter.INSTANCE);
		converters.add(StringToLocalTimeConverter.INSTANCE);
		converters.add(ZonedDateTimeToStringConverter.INSTANCE);
		converters.add(StringToZonedDateTimeConverter.INSTANCE);
		converters.add(InstantToStringConverter.INSTANCE);
		converters.add(StringToInstantConverter.INSTANCE);
		converters.add(ZoneIdToStringConverter.INSTANCE);
		converters.add(StringToZoneIdConverter.INSTANCE);
		converters.add(PeriodToStringConverter.INSTANCE);
		converters.add(StringToPeriodConverter.INSTANCE);
		converters.add(DurationToStringConverter.INSTANCE);
		converters.add(StringToDurationConverter.INSTANCE);
		converters.add(OffsetDateTimeToStringConverter.INSTANCE);
		converters.add(StringToOffsetDateTimeConverter.INSTANCE);
		converters.add(OffsetTimeToStringConverter.INSTANCE);
		converters.add(StringToOffsetTimeConverter.INSTANCE);

		return converters;
	}

	/**
	 * Returns whether the given type is a supported JSR-310 date/time type.
	 *
	 * @param type must not be {@literal null}.
	 * @return whether the given type is a supported JSR-310 date/time type.
	 */
	public static boolean supports(Class<?> type) {
		return Arrays.asList(LocalDateTime.class, LocalDate.class, LocalTime.class, Instant.class, ZonedDateTime.class,
				OffsetDateTime.class, OffsetTime.class, ZoneId.class, Period.class, Duration.class).contains(type);
	}

	@WritingConverter
	enum LocalDateTimeToStringConverter implements Converter<LocalDateTime, String> {
		INSTANCE;

		@Override
		public String convert(LocalDateTime source) {
			return source.toString();
		}
	}

	@ReadingConverter
	enum StringToLocalDateTimeConverter implements Converter<String, LocalDateTime> {
		INSTANCE;

		@Override
		public LocalDateTime convert(String source) {
			return LocalDateTime.parse(source);
		}
	}

	@WritingConverter
	enum LocalDateToStringConverter implements Converter<LocalDate, String> {
		INSTANCE;

		@Override
		public String convert(LocalDate source) {
			return source.toString();
		}
	}

	@ReadingConverter
	enum StringToLocalDateConverter implements Converter<String, LocalDate> {
		INSTANCE;

		@Override
		public LocalDate convert(String source) {
			return LocalDate.parse(source);
		}
	}

	@WritingConverter
	enum LocalTimeToStringConverter implements Converter<LocalTime, String> {
		INSTANCE;

		@Override
		public String convert(LocalTime source) {
			return source.toString();
		}
	}

	@ReadingConverter
	enum StringToLocalTimeConverter implements Converter<String, LocalTime> {
		INSTANCE;

		@Override
		public LocalTime convert(String source) {
			return LocalTime.parse(source);
		}
	}

	@WritingConverter
	enum ZonedDateTimeToStringConverter implements Converter<ZonedDateTime, String> {
		INSTANCE;

		@Override
		public String convert(ZonedDateTime source) {
			return source.toString();
		}
	}

	@ReadingConverter
	enum StringToZonedDateTimeConverter implements Converter<String, ZonedDateTime> {
		INSTANCE;

		@Override
		public ZonedDateTime convert(String source) {
			return ZonedDateTime.parse(source);
		}
	}

	@WritingConverter
	enum InstantToStringConverter implements Converter<Instant, String> {
		INSTANCE;

		@Override
		public String convert(Instant source) {
			return source.toString();
		}
	}

	@ReadingConverter
	enum StringToInstantConverter implements Converter<String, Instant> {
		INSTANCE;

		@Override
		public Instant convert(String source) {
			return Instant.parse(source);
		}
	}

	@WritingConverter
	enum ZoneIdToStringConverter implements Converter<ZoneId, String> {
		INSTANCE;

		@Override
		public String convert(ZoneId source) {
			return source.toString();
		}
	}

	@ReadingConverter
	enum StringToZoneIdConverter implements Converter<String, ZoneId> {
		INSTANCE;

		@Override
		public ZoneId convert(String source) {
			return ZoneId.of(source);
		}
	}

	@WritingConverter
	enum PeriodToStringConverter implements Converter<Period, String> {
		INSTANCE;

		@Override
		public String convert(Period source) {
			return source.toString();
		}
	}

	@ReadingConverter
	enum StringToPeriodConverter implements Converter<String, Period> {
		INSTANCE;

		@Override
		public Period convert(String source) {
			return Period.parse(source);
		}
	}

	@WritingConverter
	enum DurationToStringConverter implements Converter<Duration, String> {
		INSTANCE;

		@Override
		public String convert(Duration source) {
			return source.toString();
		}
	}

	@ReadingConverter
	enum StringToDurationConverter implements Converter<String, Duration> {
		INSTANCE;

		@Override
		public Duration convert(String source) {
			return Duration.parse(source);
		}
	}

	@WritingConverter
	enum OffsetDateTimeToStringConverter implements Converter<OffsetDateTime, String> {
		INSTANCE;

		@Override
		public String convert(OffsetDateTime source) {
			return source.toString();
		}
	}

	@ReadingConverter
	enum StringToOffsetDateTimeConverter implements Converter<String, OffsetDateTime> {
		INSTANCE;

		@Override
		public OffsetDateTime convert(String source) {
			return OffsetDateTime.parse(source);
		}
	}

	@WritingConverter
	enum OffsetTimeToStringConverter implements Converter<OffsetTime, String> {
		INSTANCE;

		@Override
		public String convert(OffsetTime source) {
			return source.toString();
		}
	}

	@ReadingConverter
	enum StringToOffsetTimeConverter implements Converter<String, OffsetTime> {
		INSTANCE;

		@Override
		public OffsetTime convert(String source) {
			return OffsetTime.parse(source);
		}
	}
}
