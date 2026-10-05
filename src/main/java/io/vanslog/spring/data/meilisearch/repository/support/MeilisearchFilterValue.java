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
import java.util.UUID;

import org.springframework.lang.Nullable;

/**
 * Serializes scalar values used in Meilisearch filter expressions.
 *
 * @author Junghoon Ban
 */
final class MeilisearchFilterValue {

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
		if (value instanceof Boolean booleanValue) {
			return booleanValue.toString();
		}
		if (value instanceof Number) {
			return number(value);
		}
		if (value instanceof Date date) {
			return Long.toString(date.getTime());
		}
		return quote(text(value));
	}

	static boolean supportsScalarType(Class<?> type) {
		return type == Object.class || CharSequence.class.isAssignableFrom(type) || type == Character.class
				|| type == char.class || type == Boolean.class || type == boolean.class || supportsNumberType(type)
				|| Date.class.isAssignableFrom(type) || Enum.class.isAssignableFrom(type)
				|| TemporalAccessor.class.isAssignableFrom(type) || UUID.class.isAssignableFrom(type);
	}

	static boolean supportsNumberType(Class<?> type) {
		return type == Number.class || type == Byte.class || type == byte.class || type == Short.class
				|| type == short.class || type == Integer.class || type == int.class || type == Long.class || type == long.class
				|| type == Float.class || type == float.class || type == Double.class || type == double.class
				|| type == BigInteger.class || type == BigDecimal.class;
	}

	static String number(@Nullable Object value) {

		if (value != null && value.getClass() == BigDecimal.class) {
			return ((BigDecimal) value).toPlainString();
		}
		if (value != null && value.getClass() == BigInteger.class) {
			return ((BigInteger) value).toString();
		}
		if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
			return value.toString();
		}
		if (value instanceof Float floatValue) {
			if (!Float.isFinite(floatValue)) {
				throw new IllegalArgumentException("Filter number must be finite");
			}
			return Float.toString(floatValue);
		}
		if (value instanceof Double doubleValue) {
			if (!Double.isFinite(doubleValue)) {
				throw new IllegalArgumentException("Filter number must be finite");
			}
			return Double.toString(doubleValue);
		}
		throw new IllegalArgumentException("Unsupported numeric filter value: " + typeName(value));
	}

	static String text(@Nullable Object value) {

		if (value == null) {
			throw new IllegalArgumentException("Query parameter value must not be null");
		}
		if (value instanceof CharSequence sequence) {
			return sequence.toString();
		}
		if (value instanceof Character character) {
			return character.toString();
		}
		if (value instanceof Boolean booleanValue) {
			return booleanValue.toString();
		}
		if (value instanceof Number) {
			return number(value);
		}
		if (value instanceof Date date) {
			return Long.toString(date.getTime());
		}
		if (value instanceof Enum<?> enumValue) {
			return enumValue.name();
		}
		if (value instanceof TemporalAccessor temporal) {
			return temporal.toString();
		}
		if (value instanceof UUID uuid) {
			return uuid.toString();
		}
		throw new IllegalArgumentException("Unsupported query parameter value: " + typeName(value));
	}

	private static String typeName(@Nullable Object value) {
		return value == null ? "null" : value.getClass().getName();
	}
}
