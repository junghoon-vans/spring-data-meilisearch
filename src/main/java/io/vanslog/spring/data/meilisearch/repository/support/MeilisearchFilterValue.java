/*
 * Copyright 2026 the original author or authors.
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
package io.vanslog.spring.data.meilisearch.repository.support;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import org.springframework.lang.Nullable;

/**
 * Serializes scalar values used in Meilisearch filter expressions.
 *
 * @author Junghoon Ban
 */
final class MeilisearchFilterValue {

	private static final Set<Class<?>> NUMBER_TYPES = Set.of(Number.class, Byte.class, byte.class, Short.class,
			short.class, Integer.class, int.class, Long.class, long.class, Float.class, float.class, Double.class,
			double.class, BigInteger.class, BigDecimal.class);

	private static final Set<Class<?>> SIMPLE_TEXT_TYPES = Set.of(Character.class, Boolean.class, UUID.class);

	private MeilisearchFilterValue() {}

	static String quote(@Nullable String value) {

		if (value == null) {
			throw new IllegalArgumentException("Filter string value must not be null");
		}

		StringBuilder result = new StringBuilder(value.length() + 2).append('"');
		for (int i = 0; i < value.length(); i++) {
			char character = value.charAt(i);
			switch (character) {
				case '\\', '"' -> result.append('\\').append(character);
				case '\n' -> result.append("\\n");
				case '\r' -> result.append("\\r");
				case '\t' -> result.append("\\t");
				case '\b' -> result.append("\\b");
				case '\f' -> result.append("\\f");
				default -> {
					if (character < 0x20) {
						result.append("\\u00");
						result.append(Character.forDigit((character >>> 4) & 0xf, 16));
						result.append(Character.forDigit(character & 0xf, 16));
					} else {
						result.append(character);
					}
				}
			}
		}
		return result.append('"').toString();
	}

	static String scalar(@Nullable Object value) {

		if (value == null) {
			throw new IllegalArgumentException("Filter scalar value must not be null");
		}
		String result;
		if (value instanceof Boolean booleanValue) {
			result = booleanValue.toString();
		} else if (value instanceof Number) {
			result = number(value);
		} else if (value instanceof Date date) {
			result = Long.toString(date.getTime());
		} else {
			result = quote(text(value));
		}
		return result;
	}

	static boolean supportsScalarType(Class<?> type) {
		return type == Object.class || CharSequence.class.isAssignableFrom(type) || type == Character.class
				|| type == char.class || type == Boolean.class || type == boolean.class || supportsNumberType(type)
				|| Date.class.isAssignableFrom(type) || Enum.class.isAssignableFrom(type)
				|| TemporalAccessor.class.isAssignableFrom(type) || UUID.class.isAssignableFrom(type);
	}

	static boolean supportsNumberType(Class<?> type) {
		return NUMBER_TYPES.contains(type);
	}

	static String number(@Nullable Object value) {

		if (value == null || !supportsNumberType(value.getClass())) {
			throw new IllegalArgumentException("Unsupported numeric filter value: " + typeName(value));
		}
		if ((value instanceof Float floatValue && !Float.isFinite(floatValue))
				|| (value instanceof Double doubleValue && !Double.isFinite(doubleValue))) {
			throw new IllegalArgumentException("Filter number must be finite");
		}
		if (value instanceof BigDecimal decimal) {
			return decimal.toPlainString();
		}
		return value.toString();
	}

	static String text(@Nullable Object value) {

		if (value == null) {
			throw new IllegalArgumentException("Query parameter value must not be null");
		}
		String result;
		if (supportsSimpleTextValue(value)) {
			result = value.toString();
		} else if (value instanceof Number) {
			result = number(value);
		} else if (value instanceof Date date) {
			result = Long.toString(date.getTime());
		} else if (value instanceof Enum<?> enumValue) {
			result = enumValue.name();
		} else if (value instanceof TemporalAccessor temporal) {
			result = temporal.toString();
		} else {
			throw new IllegalArgumentException("Unsupported query parameter value: " + typeName(value));
		}
		return result;
	}

	private static boolean supportsSimpleTextValue(Object value) {
		return SIMPLE_TEXT_TYPES.contains(value.getClass()) || value instanceof CharSequence;
	}

	private static String typeName(@Nullable Object value) {
		return value == null ? "null" : value.getClass().getName();
	}
}
