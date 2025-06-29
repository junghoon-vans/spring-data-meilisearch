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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.util.NumberUtils;

/**
 * Set of {@link ReadingConverter} and {@link WritingConverter} used to convert Objects for Meilisearch.
 *
 * @author Junghoon Ban
 */
final class MeilisearchConverters {

	private MeilisearchConverters() {}

	/**
	 * Returns the converters to be registered.
	 *
	 * @return the converters to register.
	 */
	static Collection<?> getConvertersToRegister() {
		List<Object> converters = new ArrayList<>(6);

		converters.add(StringToUUIDConverter.INSTANCE);
		converters.add(UUIDToStringConverter.INSTANCE);
		converters.add(BigDecimalToDoubleConverter.INSTANCE);
		converters.add(DoubleToBigDecimalConverter.INSTANCE);
		converters.add(ByteArrayToBase64Converter.INSTANCE);
		converters.add(Base64ToByteArrayConverter.INSTANCE);

		return converters;
	}

	/**
	 * {@link Converter} to read a {@link UUID} from its {@link String} representation.
	 */
	@ReadingConverter
	enum StringToUUIDConverter implements Converter<String, UUID> {

		INSTANCE;

		@Override
		public UUID convert(String source) {
			return UUID.fromString(source);
		}
	}

	/**
	 * {@link Converter} to write a {@link UUID} to its {@link String} representation.
	 */
	@WritingConverter
	enum UUIDToStringConverter implements Converter<UUID, String> {

		INSTANCE;

		@Override
		public String convert(UUID source) {
			return source.toString();
		}
	}

	/**
	 * {@link Converter} to read a {@link BigDecimal} from a {@link Double} value.
	 */
	@ReadingConverter
	enum DoubleToBigDecimalConverter implements Converter<Double, BigDecimal> {

		INSTANCE;

		@Override
		public BigDecimal convert(Double source) {
			return NumberUtils.convertNumberToTargetClass(source, BigDecimal.class);
		}
	}

	/**
	 * {@link Converter} to write a {@link BigDecimal} to a {@link Double} value.
	 */
	@WritingConverter
	enum BigDecimalToDoubleConverter implements Converter<BigDecimal, Double> {

		INSTANCE;

		@Override
		public Double convert(BigDecimal source) {
			return NumberUtils.convertNumberToTargetClass(source, Double.class);
		}
	}

	/**
	 * {@link Converter} to write a byte[] to a base64 encoded {@link String} value.
	 */
	@WritingConverter
	enum ByteArrayToBase64Converter implements Converter<byte[], String> {

		INSTANCE;

		@Override
		public String convert(byte[] source) {
			return Base64.getEncoder().encodeToString(source);
		}
	}

	/**
	 * {@link Converter} to read a byte[] from a base64 encoded {@link String} value.
	 */
	@ReadingConverter
	enum Base64ToByteArrayConverter implements Converter<String, byte[]> {

		INSTANCE;

		@Override
		public byte[] convert(String source) {
			return Base64.getDecoder().decode(source);
		}
	}
}
