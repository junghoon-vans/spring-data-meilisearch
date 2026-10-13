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
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

import io.vanslog.spring.data.meilisearch.core.mapping.MeilisearchPersistentEntity;
import io.vanslog.spring.data.meilisearch.core.mapping.MeilisearchPersistentProperty;
import io.vanslog.spring.data.meilisearch.core.query.BasicQuery;
import io.vanslog.spring.data.meilisearch.core.query.BasicQueryBuilder;

/**
 * Pure query parsing, field mapping, sorting, filter binding, and search request planning shared by blocking and
 * reactive repository queries.
 *
 * @author Junghoon Ban
 */
final class MeilisearchQueryPlanner {

	private static final String AND = " AND ";

	private final Method method;

	private final QueryMethod queryMethod;

	private final Class<?> domainType;

	private final MappingContext<? extends MeilisearchPersistentEntity<?>, MeilisearchPersistentProperty> mappingContext;

	private final ConversionService conversionService;

	private final String queryKind;

	MeilisearchQueryPlanner(Method method, QueryMethod queryMethod, Class<?> domainType,
			MappingContext<? extends MeilisearchPersistentEntity<?>, MeilisearchPersistentProperty> mappingContext,
			ConversionService conversionService, String queryKind) {

		this.method = method;
		this.queryMethod = queryMethod;
		this.domainType = domainType;
		this.mappingContext = mappingContext;
		this.conversionService = conversionService;
		this.queryKind = queryKind;
	}

	PartTree parseDerivedQuery() {

		PartTree tree;
		try {
			tree = new PartTree(method.getName(), domainType);
		} catch (RuntimeException exception) {
			throw new IllegalArgumentException(
					"Cannot parse derived query method " + method.toGenericString() + ": " + exception.getMessage(), exception);
		}

		validateTree(tree);
		return tree;
	}

	List<String> createFilters(PartTree tree, ParametersParameterAccessor accessor) {

		List<String> groups = new ArrayList<>();
		List<String> singleGroup = List.of();
		int groupCount = 0;
		int parameterIndex = 0;

		for (PartTree.OrPart orPart : tree) {
			List<String> filters = new ArrayList<>();
			for (Part part : orPart) {
				PropertyReference property = resolveProperty(part.getProperty().toDotPath(), FilterSyntax.operatorName(part));
				String filter = createFilter(part, property, accessor, parameterIndex);
				if (filter != null) {
					filters.add(filter);
				}
				parameterIndex += part.getNumberOfArguments();
			}
			groupCount++;
			singleGroup = filters;
			if (!filters.isEmpty()) {
				groups.add("(" + String.join(AND, filters) + ")");
			}
		}

		if (groupCount == 1) {
			return singleGroup;
		}
		if (groups.size() != groupCount) {
			throw new IllegalArgumentException(
					"Cannot evaluate an Or branch without an effective filter for derived query " + method.toGenericString());
		}
		return List.of(String.join(" OR ", groups));
	}

	Sort mapSort(Sort sort) {

		if (sort == null || sort.isUnsorted()) {
			return Sort.unsorted();
		}

		List<Sort.Order> mappedOrders = new ArrayList<>();
		for (Sort.Order order : sort) {
			if (order.isIgnoreCase() || order.getNullHandling() != Sort.NullHandling.NATIVE) {
				throw unsupportedOperator("Sort options for " + order.getProperty());
			}
			PropertyReference property = resolveProperty(order.getProperty(), "Sort");
			mappedOrders.add(order.withProperty(property.fieldName()));
		}
		return Sort.by(mappedOrders);
	}

	BasicQuery createQuery(String q, List<String> filters, Sort sort, Pageable pageable) {

		BasicQueryBuilder builder = BasicQuery.builder().withQ(q);
		if (!filters.isEmpty()) {
			builder.withFilter(filters.toArray(String[]::new));
		}
		if (sort.isSorted()) {
			builder.withSort(sort);
		}
		return builder.withPageable(pageable).build();
	}

	IllegalArgumentException unsupportedOperator(String operator) {
		String category = "derived".equals(queryKind) ? "operator" : "option";
		return new IllegalArgumentException("Unsupported " + queryKind + " query " + category + " '" + operator
				+ "' in method " + method.toGenericString());
	}

	private @Nullable String createFilter(Part part, PropertyReference property, ParametersParameterAccessor accessor,
			int parameterIndex) {

		String field = property.fieldName();
		return switch (part.getType()) {
			case SIMPLE_PROPERTY -> {
				Object value = accessor.getBindableValue(parameterIndex);
				yield value == null ? "(" + field + " IS NULL OR " + field + " NOT EXISTS)"
						: field + " = " + toLiteral(value, property, part);
			}
			case IS_NULL -> "(" + field + " IS NULL OR " + field + " NOT EXISTS)";
			case IS_NOT_NULL -> "(" + field + " EXISTS AND " + field + " IS NOT NULL)";
			case EXISTS -> field + " EXISTS";
			case IN -> createInFilter(field, property, part, accessor.getBindableValue(parameterIndex), false);
			case NOT_IN -> createInFilter(field, property, part, accessor.getBindableValue(parameterIndex), true);
			case STARTING_WITH -> createPrefixFilter(field, part, accessor.getBindableValue(parameterIndex));
			case GREATER_THAN, GREATER_THAN_EQUAL, LESS_THAN, LESS_THAN_EQUAL, BETWEEN, TRUE, FALSE ->
				createComparisonFilter(part, property, accessor, parameterIndex);
			default -> throw unsupportedOperator(FilterSyntax.operatorName(part));
		};
	}

	private String createPrefixFilter(String field, Part part, @Nullable Object value) {
		if (!(value instanceof String prefix)) {
			throw invalidParameter(part, "requires a non-null String prefix");
		}
		if (prefix.isEmpty()) {
			throw invalidParameter(part, "empty prefixes are not supported");
		}
		return field + " STARTS WITH " + MeilisearchFilterValue.quote(prefix);
	}

	private String createComparisonFilter(Part part, PropertyReference property, ParametersParameterAccessor accessor,
			int parameterIndex) {

		String field = property.fieldName();
		return switch (part.getType()) {
			case GREATER_THAN -> field + " > " + toLiteral(accessor.getBindableValue(parameterIndex), property, part);
			case GREATER_THAN_EQUAL -> field + " >= " + toLiteral(accessor.getBindableValue(parameterIndex), property, part);
			case LESS_THAN -> field + " < " + toLiteral(accessor.getBindableValue(parameterIndex), property, part);
			case LESS_THAN_EQUAL -> field + " <= " + toLiteral(accessor.getBindableValue(parameterIndex), property, part);
			case BETWEEN -> field + " " + toLiteral(accessor.getBindableValue(parameterIndex), property, part) + " TO "
					+ toLiteral(accessor.getBindableValue(parameterIndex + 1), property, part);
			case TRUE -> field + " = true";
			case FALSE -> field + " = false";
			default -> throw unsupportedOperator(FilterSyntax.operatorName(part));
		};
	}

	private @Nullable String createInFilter(String field, PropertyReference property, Part part, @Nullable Object value,
			boolean negated) {

		if (value == null) {
			throw invalidParameter(part, "IN/NotIn requires a non-null collection or array");
		}

		List<String> literals = new ArrayList<>();
		if (value instanceof Iterable<?> iterable) {
			for (Object item : iterable) {
				if (item == null) {
					throw invalidParameter(part, "IN/NotIn does not support null collection elements");
				}
				literals.add(toLiteral(item, property, part));
			}
		} else if (value.getClass().isArray()) {
			for (int i = 0; i < Array.getLength(value); i++) {
				Object item = Array.get(value, i);
				if (item == null) {
					throw invalidParameter(part, "IN/NotIn does not support null collection elements");
				}
				literals.add(toLiteral(item, property, part));
			}
		} else {
			throw invalidParameter(part, "IN/NotIn requires a collection or array parameter");
		}

		if (literals.isEmpty()) {
			if (negated) {
				return null;
			}
			throw invalidParameter(part, "IN requires at least one value");
		}

		return field + (negated ? " NOT IN [" : " IN [") + String.join(", ", literals) + "]";
	}

	private String toLiteral(@Nullable Object value, PropertyReference property, Part part) {

		if (value == null) {
			throw invalidParameter(part, "null is not a supported filter value");
		}

		Class<?> targetType = property.valueType();
		if (targetType == Object.class) {
			targetType = value.getClass();
		}

		Object converted = value;
		Class<?> boxedTargetType = ClassUtils.resolvePrimitiveIfNecessary(targetType);
		if (!boxedTargetType.isInstance(value)) {
			if (!conversionService.canConvert(value.getClass(), targetType)) {
				throw invalidParameter(part,
						"cannot convert value of type " + value.getClass().getName() + " to " + targetType.getName());
			}
			converted = conversionService.convert(value, targetType);
		}
		if (converted == null) {
			throw invalidParameter(part, "conversion produced a null filter value");
		}

		try {
			return MeilisearchFilterValue.scalar(converted);
		} catch (IllegalArgumentException exception) {
			throw invalidParameter(part, exception.getMessage());
		}
	}

	private void validateTree(PartTree tree) {

		if (tree.isDistinct()) {
			throw unsupportedOperator("Distinct");
		}
		if (tree.isLimiting()) {
			throw unsupportedOperator("Top/First");
		}
		if (!tree.hasPredicate()) {
			throw unsupportedOperator("predicate");
		}

		List<Part> parsedParts = new ArrayList<>();
		for (PartTree.OrPart orPart : tree) {
			for (Part part : orPart) {
				validatePart(part);
				parsedParts.add(part);
			}
		}

		int expectedParameters = parsedParts.stream().mapToInt(Part::getNumberOfArguments).sum();
		int actualParameters = queryMethod.getParameters().getBindableParameters().getNumberOfParameters();
		if (expectedParameters != actualParameters) {
			throw unsupportedOperator(
					"parameter count (expected " + expectedParameters + ", found " + actualParameters + ")");
		}
		validateFilterParameters(parsedParts);
	}

	private void validatePart(Part part) {
		if (!FilterSyntax.isSupported(part.getType())) {
			throw unsupportedOperator(FilterSyntax.operatorName(part));
		}
		if (part.shouldIgnoreCase() != Part.IgnoreCaseType.NEVER) {
			throw unsupportedOperator("IgnoreCase");
		}

		PropertyReference property = resolveProperty(part.getProperty().toDotPath(), FilterSyntax.operatorName(part));
		if ((part.getType() == Part.Type.TRUE || part.getType() == Part.Type.FALSE)
				&& ClassUtils.resolvePrimitiveIfNecessary(property.valueType()) != Boolean.class) {
			throw unsupportedOperator(
					FilterSyntax.operatorName(part) + " on non-boolean property " + part.getProperty().toDotPath());
		}
		if (part.getType() == Part.Type.STARTING_WITH
				&& mappingContext.getPersistentPropertyPath(part.getProperty().toDotPath(), domainType).getLeafProperty()
						.getType() != String.class) {
			throw unsupportedOperator(
					FilterSyntax.operatorName(part) + " on non-string property " + part.getProperty().toDotPath());
		}
	}

	private void validateFilterParameters(List<Part> parsedParts) {

		int parameterIndex = 0;
		for (Part part : parsedParts) {
			if (part.getType() == Part.Type.IN || part.getType() == Part.Type.NOT_IN) {
				Class<?> parameterType = queryMethod.getParameters().getBindableParameter(parameterIndex).getType();
				if (!Iterable.class.isAssignableFrom(parameterType) && !parameterType.isArray()) {
					throw unsupportedOperator(FilterSyntax.operatorName(part) + " requires a collection or array parameter");
				}
			}
			if (part.getType() == Part.Type.STARTING_WITH
					&& queryMethod.getParameters().getBindableParameter(parameterIndex).getType() != String.class) {
				throw unsupportedOperator(FilterSyntax.operatorName(part) + " requires a String parameter");
			}
			parameterIndex += part.getNumberOfArguments();
		}
	}

	private PropertyReference resolveProperty(String pathName, String operator) {

		try {
			PersistentPropertyPath<MeilisearchPersistentProperty> path = mappingContext.getPersistentPropertyPath(pathName,
					domainType);
			StringBuilder fieldName = new StringBuilder();
			for (MeilisearchPersistentProperty property : path) {
				if (property.isTransient()) {
					throw unsupportedOperator(operator + " on transient property " + pathName);
				}
				String mappedName = property.getFieldName();
				if (!isFilterFieldName(mappedName)) {
					throw unsupportedOperator(operator + " on mapped field " + mappedName);
				}
				if (fieldName.length() > 0) {
					fieldName.append('.');
				}
				fieldName.append(mappedName);
			}
			MeilisearchPersistentProperty leaf = path.getLeafProperty();
			Class<?> valueType = leaf.getActualType();
			if (valueType == null || valueType == Object.class) {
				valueType = leaf.getType();
			}
			return new PropertyReference(fieldName.toString(), valueType);
		} catch (RuntimeException exception) {
			if (exception.getMessage() != null && exception.getMessage().contains(method.toGenericString())) {
				throw exception;
			}
			throw new IllegalArgumentException(
					"Invalid property '" + pathName + "' for " + queryKind + " query method " + method.toGenericString(),
					exception);
		}
	}

	private static boolean isFilterFieldName(String fieldName) {
		if (fieldName.isEmpty()) {
			return false;
		}
		for (int i = 0; i < fieldName.length(); i++) {
			char character = fieldName.charAt(i);
			if (!(Character.isLetterOrDigit(character) || character == '_' || character == '-' || character == '.')) {
				return false;
			}
		}
		return fieldName.charAt(0) != '.' && fieldName.charAt(fieldName.length() - 1) != '.' && !fieldName.contains("..");
	}

	private IllegalArgumentException invalidParameter(Part part, String reason) {
		return new IllegalArgumentException("Invalid value for derived query operator '" + FilterSyntax.operatorName(part)
				+ "' in method " + method.toGenericString() + ": " + reason);
	}

	private static final class FilterSyntax {

		private FilterSyntax() {}

		private static boolean isSupported(Part.Type type) {
			return switch (type) {
				case SIMPLE_PROPERTY, IN, NOT_IN, GREATER_THAN, GREATER_THAN_EQUAL, LESS_THAN, LESS_THAN_EQUAL, BETWEEN, TRUE,
						FALSE, IS_NULL, IS_NOT_NULL, EXISTS, STARTING_WITH ->
					true;
				default -> false;
			};
		}

		private static String operatorName(Part part) {
			return part.getType().getKeywords().stream().findFirst().orElse(part.getType().name());
		}

	}

	record PropertyReference(String fieldName, Class<?> valueType) {
	}

}
