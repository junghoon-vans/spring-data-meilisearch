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

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.data.repository.query.QueryMethod;
import org.springframework.lang.Nullable;

/**
 * Compiles and binds positional parameters in a declared Meilisearch filter and {@code q} template.
 *
 * @author Junghoon Ban
 */
final class MeilisearchDeclaredQueryBinding {

	private final Method method;
	private final int parameterCount;
	private final Class<?>[] parameterTypes;
	private final boolean[] specialParameters;
	private final String filter;
	private final String q;
	private final List<Placeholder> filterPlaceholders;
	private final List<Placeholder> qPlaceholders;
	private final boolean qOnly;

	MeilisearchDeclaredQueryBinding(Method method, QueryMethod queryMethod, String filter, String q) {

		this.method = Objects.requireNonNull(method, "Method must not be null");
		Objects.requireNonNull(queryMethod, "QueryMethod must not be null");
		Objects.requireNonNull(queryMethod.getParameters(), "Query method parameters must not be null");

		this.parameterCount = method.getParameterCount();
		this.parameterTypes = new Class<?>[parameterCount];
		if (queryMethod.getParameters().getNumberOfParameters() != parameterCount) {
			throw new IllegalArgumentException("Query method parameters do not match method " + method);
		}

		this.specialParameters = new boolean[parameterCount];
		for (int i = 0; i < parameterCount; i++) {
			var parameter = queryMethod.getParameters().getParameter(i);
			this.parameterTypes[i] = parameter.getType();
			this.specialParameters[i] = parameter.isSpecialParameter();
		}

		this.filter = normalize(filter);
		this.q = normalize(q);
		if (this.filter.isEmpty() && this.q.isEmpty()) {
			throw new IllegalArgumentException("Declared query must specify a filter or q template");
		}

		this.qOnly = this.filter.isEmpty();
		this.filterPlaceholders = compileFilter(this.filter);
		this.qPlaceholders = compile(this.q, Context.QUERY_TEXT);
		validateParameters();
	}

	BoundQuery bind(@Nullable Object[] values) {

		if (values == null || values.length != parameterCount) {
			throw new IllegalArgumentException("Expected " + parameterCount + " arguments for method " + method);
		}

		String boundFilter = render(filter, filterPlaceholders, values);
		String boundQuery = render(q, qPlaceholders, values);
		if (boundQuery.isBlank()) {
			boundQuery = "";
			if (qOnly && !qPlaceholders.isEmpty()) {
				throw new IllegalArgumentException("A q-only declared query must bind to non-blank text");
			}
		}

		return new BoundQuery(boundQuery, boundFilter);
	}

	private void validateParameters() {

		boolean[] used = new boolean[parameterCount];
		markUsed(filterPlaceholders, used);
		markUsed(qPlaceholders, used);

		for (int i = 0; i < parameterCount; i++) {
			if (!specialParameters[i] && !used[i]) {
				throw new IllegalArgumentException("Declared query does not use bindable parameter ?" + i + " of " + method);
			}
		}
	}

	private void markUsed(List<Placeholder> placeholders, boolean[] used) {
		for (Placeholder placeholder : placeholders) {
			int index = placeholder.parameterIndex();
			if (index < 0 || index >= parameterCount) {
				throw new IllegalArgumentException("Placeholder ?" + index + " is outside the parameters of " + method);
			}
			if (specialParameters[index]) {
				throw new IllegalArgumentException("Placeholder ?" + index + " refers to a special parameter of " + method);
			}
			validateParameterType(placeholder, index);
			used[index] = true;
		}
	}

	private void validateParameterType(Placeholder placeholder, int index) {

		Class<?> type = parameterTypes[index];
		boolean supported = switch (placeholder.context()) {
			case QUERY_TEXT, FILTER_SCALAR -> MeilisearchFilterValue.supportsScalarType(type);
			case FILTER_COLLECTION -> type == Object.class || Collection.class.isAssignableFrom(type)
					|| type.isArray() && MeilisearchFilterValue.supportsScalarType(type.getComponentType());
			case GEO_NUMBER -> type == Object.class || MeilisearchFilterValue.supportsNumberType(type);
		};
		if (!supported) {
			throw new IllegalArgumentException("Parameter ?" + index + " has unsupported declared type " + type.getTypeName()
					+ " for " + placeholder.context());
		}
	}

	private List<Placeholder> compileFilter(String template) {

		List<Token> tokens = tokenizeFilter(template);
		List<Placeholder> placeholders = new ArrayList<>();
		Deque<Container> containers = new ArrayDeque<>();

		for (int i = 0; i < tokens.size(); i++) {
			Token token = tokens.get(i);
			if (token.type() == TokenType.PLACEHOLDER) {
				Context context = classifyFilterPlaceholder(tokens, i, containers);
				placeholders.add(new Placeholder(token.start(), token.end(), token.parameterIndex(), context));
			}
			updateContainers(tokens, i, containers);
		}

		return List.copyOf(placeholders);
	}

	private Context classifyFilterPlaceholder(List<Token> tokens, int index, Deque<Container> containers) {

		Token previous = index > 0 ? tokens.get(index - 1) : null;
		Token next = index + 1 < tokens.size() ? tokens.get(index + 1) : null;
		boolean atEndOfValue = isValueBoundary(next);
		Container array = nearestContainer(containers, TokenType.LEFT_BRACKET);
		String geoFunction = nearestGeoFunction(containers);

		if (array != null
				&& (previous != null && (previous.type() == TokenType.LEFT_BRACKET || previous.type() == TokenType.COMMA))) {
			if (isGeoArrayFunction(geoFunction) && atEndOfGeoNumber(next)) {
				return Context.GEO_NUMBER;
			}
			if (array.inCollection() && atEndOfArrayElement(next)) {
				return Context.FILTER_SCALAR;
			}
			throw unsafePlaceholder(tokenIndex(tokens, index), "in an unsupported array position");
		}

		if ("_GEORADIUS".equals(geoFunction) && previous != null
				&& (previous.type() == TokenType.LEFT_PAREN || previous.type() == TokenType.COMMA) && atEndOfGeoNumber(next)) {
			return Context.GEO_NUMBER;
		}

		if (previous != null && previous.type() == TokenType.WORD && previous.value().equalsIgnoreCase("IN")
				&& atEndOfValue) {
			return Context.FILTER_COLLECTION;
		}

		if (previous != null && (previous.type() == TokenType.WORD || previous.type() == TokenType.LITERAL) && next != null
				&& next.type() == TokenType.WORD && next.value().equalsIgnoreCase("TO") && startsPredicate(tokens, index - 1)) {
			return Context.FILTER_SCALAR;
		}

		if (previous != null && previous.type() == TokenType.WORD && previous.value().equalsIgnoreCase("WITH") && index > 1
				&& tokens.get(index - 2).value().equalsIgnoreCase("STARTS") && atEndOfValue) {
			return Context.FILTER_SCALAR;
		}

		if (previous != null && isValueOperator(previous) && atEndOfValue) {
			return Context.FILTER_SCALAR;
		}

		throw unsafePlaceholder(tokenIndex(tokens, index), "not a complete filter value");
	}

	private static int tokenIndex(List<Token> tokens, int index) {
		return tokens.get(index).parameterIndex();
	}

	private static boolean isValueOperator(Token token) {
		return token.type() == TokenType.WORD
				&& (token.value().equalsIgnoreCase("TO") || token.value().equalsIgnoreCase("CONTAINS"))
				|| token.type() == TokenType.OPERATOR && switch (token.value()) {
					case "=", "!=", ">", ">=", "<", "<=" -> true;
					default -> false;
				};
	}

	private static boolean isValueBoundary(@Nullable Token next) {
		return next == null || next.type() == TokenType.RIGHT_PAREN || next.type() == TokenType.WORD
				&& (next.value().equalsIgnoreCase("AND") || next.value().equalsIgnoreCase("OR"));
	}

	private static boolean startsPredicate(List<Token> tokens, int fieldIndex) {

		if (fieldIndex == 0) {
			return true;
		}
		Token preceding = tokens.get(fieldIndex - 1);
		return preceding.type() == TokenType.LEFT_PAREN
				|| preceding.type() == TokenType.WORD && (preceding.value().equalsIgnoreCase("AND")
						|| preceding.value().equalsIgnoreCase("OR") || preceding.value().equalsIgnoreCase("NOT"));
	}

	private static boolean atEndOfArrayElement(@Nullable Token next) {
		return next != null && (next.type() == TokenType.COMMA || next.type() == TokenType.RIGHT_BRACKET);
	}

	private static boolean isGeoArrayFunction(@Nullable String function) {
		return "_GEOBOUNDINGBOX".equals(function) || "_GEOPOLYGON".equals(function);
	}

	private static boolean atEndOfGeoNumber(@Nullable Token next) {
		return next == null || next.type() == TokenType.COMMA || next.type() == TokenType.RIGHT_PAREN
				|| next.type() == TokenType.RIGHT_BRACKET;
	}

	private static IllegalArgumentException unsafePlaceholder(int index, String reason) {
		return new IllegalArgumentException("Placeholder ?" + index + " " + reason);
	}

	@Nullable
	private static Container nearestContainer(Deque<Container> containers, TokenType type) {
		for (Container container : containers) {
			if (container.type() == type) {
				return container;
			}
		}
		return null;
	}

	@Nullable
	private static String nearestGeoFunction(Deque<Container> containers) {
		for (Container container : containers) {
			if (container.type() == TokenType.LEFT_PAREN) {
				return container.geoFunction();
			}
			if (container.geoFunction() != null) {
				return container.geoFunction();
			}
		}
		return null;
	}

	private static void updateContainers(List<Token> tokens, int index, Deque<Container> containers) {

		Token token = tokens.get(index);
		if (token.type() == TokenType.LEFT_PAREN || token.type() == TokenType.LEFT_BRACKET) {
			String function = null;
			if (token.type() == TokenType.LEFT_PAREN && index > 0 && tokens.get(index - 1).type() == TokenType.WORD) {
				String candidate = tokens.get(index - 1).value().toUpperCase(Locale.ROOT);
				if (candidate.equals("_GEORADIUS") || candidate.equals("_GEOBOUNDINGBOX") || candidate.equals("_GEOPOLYGON")) {
					function = candidate;
				}
			}
			if (token.type() == TokenType.LEFT_BRACKET) {
				function = nearestGeoFunction(containers);
			}
			boolean inCollection = token.type() == TokenType.LEFT_BRACKET && index > 0
					&& tokens.get(index - 1).type() == TokenType.WORD && tokens.get(index - 1).value().equalsIgnoreCase("IN");
			containers.push(new Container(token.type(), inCollection, function));
		} else if (token.type() == TokenType.RIGHT_PAREN || token.type() == TokenType.RIGHT_BRACKET) {
			TokenType expected = token.type() == TokenType.RIGHT_PAREN ? TokenType.LEFT_PAREN : TokenType.LEFT_BRACKET;
			while (!containers.isEmpty()) {
				Container container = containers.pop();
				if (container.type() == expected) {
					break;
				}
			}
		}
	}

	private static List<Token> tokenizeFilter(String template) {

		List<Token> tokens = new ArrayList<>();
		for (int i = 0; i < template.length();) {
			char current = template.charAt(i);
			if (Character.isWhitespace(current)) {
				i++;
				continue;
			}

			if (current == '"' || current == '\'') {
				int end = i + 1;
				while (end < template.length()) {
					char character = template.charAt(end);
					if (character == '\\') {
						end = Math.min(end + 2, template.length());
						continue;
					}
					if (character == current) {
						end++;
						break;
					}
					if (character == '?' && end + 1 < template.length() && isIndexDigit(template.charAt(end + 1))) {
						Placeholder placeholder = parsePlaceholder(template, end, Context.FILTER_SCALAR);
						throw unsafePlaceholder(placeholder.parameterIndex(), "inside a quoted filter literal");
					}
					end++;
				}
				tokens.add(new Token(TokenType.LITERAL, template.substring(i, end), i, end, -1));
				i = end;
				continue;
			}

			if (current == '?') {
				Placeholder placeholder = parsePlaceholder(template, i, Context.FILTER_SCALAR);
				tokens.add(new Token(TokenType.PLACEHOLDER, "", i, placeholder.end(), placeholder.parameterIndex()));
				i = placeholder.end();
				continue;
			}

			if (Character.isLetter(current) || current == '_') {
				int end = i + 1;
				while (end < template.length() && (Character.isLetterOrDigit(template.charAt(end))
						|| template.charAt(end) == '_' || template.charAt(end) == '.' || template.charAt(end) == '-')) {
					end++;
				}
				tokens.add(new Token(TokenType.WORD, template.substring(i, end), i, end, -1));
				i = end;
				continue;
			}

			if (current == '=' || current == '!' || current == '>' || current == '<') {
				int end = i + 1;
				if (end < template.length() && template.charAt(end) == '=') {
					end++;
				}
				tokens.add(new Token(TokenType.OPERATOR, template.substring(i, end), i, end, -1));
				i = end;
				continue;
			}

			TokenType type = switch (current) {
				case '(' -> TokenType.LEFT_PAREN;
				case ')' -> TokenType.RIGHT_PAREN;
				case '[' -> TokenType.LEFT_BRACKET;
				case ']' -> TokenType.RIGHT_BRACKET;
				case ',' -> TokenType.COMMA;
				default -> TokenType.OTHER;
			};
			tokens.add(new Token(type, String.valueOf(current), i, i + 1, -1));
			i++;
		}
		return tokens;
	}

	private List<Placeholder> compile(String template, Context context) {

		List<Placeholder> placeholders = new ArrayList<>();
		for (int i = 0; i < template.length();) {
			if (template.charAt(i) == '?') {
				Placeholder placeholder = parsePlaceholder(template, i, context);
				placeholders.add(placeholder);
				i = placeholder.end();
			} else {
				i++;
			}
		}
		return List.copyOf(placeholders);
	}

	private static Placeholder parsePlaceholder(String template, int start, Context context) {

		int end = start + 1;
		while (end < template.length() && isIndexDigit(template.charAt(end))) {
			end++;
		}
		int index;
		try {
			index = Integer.parseInt(template.substring(start + 1, end));
		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException("Invalid positional placeholder in declared query", exception);
		}
		return new Placeholder(start, end, index, context);
	}

	private static boolean isIndexDigit(char character) {
		return character >= '0' && character <= '9';
	}

	private static String normalize(@Nullable String template) {
		return template == null || template.isBlank() ? "" : template;
	}

	private static String render(String template, List<Placeholder> placeholders, Object[] values) {

		if (placeholders.isEmpty()) {
			return template;
		}

		StringBuilder result = new StringBuilder(template.length());
		int offset = 0;
		for (Placeholder placeholder : placeholders) {
			result.append(template, offset, placeholder.start());
			Object value = values[placeholder.parameterIndex()];
			String boundValue = switch (placeholder.context()) {
				case QUERY_TEXT -> MeilisearchFilterValue.text(value);
				case FILTER_SCALAR -> MeilisearchFilterValue.scalar(value);
				case FILTER_COLLECTION -> collection(value);
				case GEO_NUMBER -> MeilisearchFilterValue.number(value);
			};
			result.append(boundValue);
			offset = placeholder.end();
		}
		result.append(template, offset, template.length());
		return result.toString();
	}

	private static String collection(@Nullable Object value) {

		if (value == null) {
			throw new IllegalArgumentException("Filter IN parameter must not be null");
		}
		StringBuilder result = new StringBuilder("[");
		boolean first = true;
		if (value instanceof Collection<?> values) {
			for (Object element : values) {
				if (!first) {
					result.append(", ");
				}
				result.append(MeilisearchFilterValue.scalar(element));
				first = false;
			}
		} else if (value.getClass().isArray()) {
			for (int i = 0; i < Array.getLength(value); i++) {
				if (!first) {
					result.append(", ");
				}
				result.append(MeilisearchFilterValue.scalar(Array.get(value, i)));
				first = false;
			}
		} else {
			throw new IllegalArgumentException("Filter IN parameter must be a collection or array");
		}
		if (first) {
			throw new IllegalArgumentException("Filter IN parameter must not be empty");
		}
		return result.append(']').toString();
	}

	private record Placeholder(int start, int end, int parameterIndex, Context context) {
	}

	record BoundQuery(String q, String filter) {
	}

	private record Token(TokenType type, String value, int start, int end, int parameterIndex) {
	}

	private record Container(TokenType type, boolean inCollection, @Nullable String geoFunction) {
	}

	private enum Context {

		QUERY_TEXT, FILTER_SCALAR, FILTER_COLLECTION, GEO_NUMBER
	}

	private enum TokenType {

		WORD, OPERATOR, PLACEHOLDER, LITERAL, LEFT_PAREN, RIGHT_PAREN, LEFT_BRACKET, RIGHT_BRACKET, COMMA, OTHER
	}
}
