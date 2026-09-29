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
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.core.convert.ConversionService;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

import io.vanslog.spring.data.meilisearch.core.MeilisearchOperations;
import io.vanslog.spring.data.meilisearch.core.SearchHit;
import io.vanslog.spring.data.meilisearch.core.SearchHits;
import io.vanslog.spring.data.meilisearch.core.TotalHitsRelation;
import io.vanslog.spring.data.meilisearch.core.mapping.MeilisearchPersistentEntity;
import io.vanslog.spring.data.meilisearch.core.mapping.MeilisearchPersistentProperty;
import io.vanslog.spring.data.meilisearch.core.query.BasicQuery;
import io.vanslog.spring.data.meilisearch.core.query.BasicQueryBuilder;

/**
 * Executes the supported, filter-backed subset of Spring Data derived queries.
 *
 * @author Junghoon Ban
 */
class MeilisearchPartTreeQuery implements RepositoryQuery {

	private static final int FETCH_PAGE_SIZE = 1000;

	private final Method method;
	private final QueryMethod queryMethod;
	private final PartTree tree;
	private final List<Part> parts;
	private final Class<?> domainType;
	private final MeilisearchOperations operations;
	private final MappingContext<? extends MeilisearchPersistentEntity<?>, MeilisearchPersistentProperty> mappingContext;
	private final ConversionService conversionService;
	private final ReturnShape returnShape;
	private final Sort staticSort;

	MeilisearchPartTreeQuery(Method method, RepositoryMetadata metadata, ProjectionFactory projectionFactory,
			MeilisearchOperations operations) {

		this.method = method;
		this.queryMethod = new QueryMethod(method, metadata, projectionFactory);
		this.domainType = metadata.getDomainType();
		this.operations = operations;
		this.mappingContext = operations.getMeilisearchConverter().getMappingContext();
		this.conversionService = operations.getMeilisearchConverter().getConversionService();
		this.tree = createPartTree();
		this.parts = validateTreeAndGetParts();
		this.returnShape = resolveReturnShape();
		validateSpecialParameters();
		this.staticSort = mapSort(tree.getSort());
	}

	@Override
	public Object execute(Object[] values) {

		ParametersParameterAccessor accessor = new ParametersParameterAccessor(queryMethod.getParameters(), values);
		List<String> filters = createFilters(accessor);
		switch (returnShape) {
			case COUNT:
				return filters.isEmpty() ? operations.count(domainType)
						: operations.count(domainType, String.join(" AND ", filters));
			case EXISTS:
				return !operations.search(createQuery(filters, Sort.unsorted(), PageRequest.of(0, 1)), domainType)
						.getSearchHits().isEmpty();
			case DELETE_COUNT, DELETE_VOID:
				if (filters.isEmpty()) {
					throw new IllegalArgumentException(
							"Cannot delete without an effective filter for derived query " + method.toGenericString());
				}
				long deleted = operations.deleteByFilter(domainType, String.join(" AND ", filters));
				return returnShape == ReturnShape.DELETE_VOID ? null : deleted;
			default:
				break;
		}
		boolean hasPageable = queryMethod.getParameters().hasPageableParameter();
		Pageable pageable = hasPageable ? accessor.getPageable() : null;
		Sort dynamicSort = hasPageable ? pageable.getSort() : accessor.getSort();
		Sort mappedDynamicSort = mapSort(dynamicSort);
		Sort completeSort = staticSort.and(mappedDynamicSort);

		return switch (returnShape) {
			case ENTITY, OPTIONAL -> executeSingle(filters, completeSort);
			case PAGE -> executePage(filters, pageable, mappedDynamicSort);
			case LIST, ITERABLE -> executeCollection(filters, pageable, completeSort);
			case COUNT, EXISTS, DELETE_COUNT, DELETE_VOID -> throw new IllegalStateException("Projection already handled");
		};
	}

	@Override
	public QueryMethod getQueryMethod() {
		return queryMethod;
	}

	private @Nullable Object executeSingle(List<String> filters, Sort sort) {

		SearchHits<?> hits = operations.search(createQuery(filters, sort, PageRequest.of(0, 2)), domainType);
		long totalHits = hits.getTotalHits();
		int loadedHits = hits.getSearchHits().size();

		if (loadedHits > 1 || hits.getTotalHitsRelation() == TotalHitsRelation.EQUAL_TO && totalHits > 1) {
			int actualSize = totalHits > 1 ? (int) Math.min(totalHits, Integer.MAX_VALUE) : loadedHits;
			throw new IncorrectResultSizeDataAccessException(
					"Derived query " + method.toGenericString() + " returned more than one result", 1, actualSize);
		}

		if (hits.getTotalHitsRelation() == TotalHitsRelation.EQUAL_TO && totalHits > loadedHits) {
			throw incompleteResultSet(totalHits, loadedHits);
		}

		Object result = loadedHits == 0 ? null : hits.getSearchHit(0).getContent();
		return returnShape == ReturnShape.OPTIONAL ? Optional.ofNullable(result) : result;
	}

	private Object executePage(List<String> filters, Pageable pageable, Sort dynamicSort) {

		if (pageable.isUnpaged()) {
			List<Object> content = fetchAll(filters, staticSort.and(dynamicSort));
			return new PageImpl<>(content, pageable, content.size());
		}

		Pageable mappedPageable = mapPageable(pageable);
		SearchHits<?> hits = operations.search(createQuery(filters, staticSort, mappedPageable), domainType);
		return new PageImpl<>(contentsOf(hits), pageable, hits.getTotalHits());
	}

	private Object executeCollection(List<String> filters, Pageable pageable, Sort completeSort) {

		List<Object> content;
		if (pageable == null || pageable.isUnpaged()) {
			content = fetchAll(filters, completeSort);
		} else {
			Pageable mappedPageable = mapPageable(pageable);
			SearchHits<?> hits = operations.search(createQuery(filters, staticSort, mappedPageable), domainType);
			content = contentsOf(hits);
		}

		return returnShape == ReturnShape.LIST ? content : (Iterable<?>) content;
	}

	private List<Object> fetchAll(List<String> filters, Sort sort) {

		List<Object> results = new ArrayList<>();
		Long expectedTotal = null;
		int pageNumber = 0;

		while (true) {
			Pageable pageable = PageRequest.of(pageNumber, FETCH_PAGE_SIZE);
			SearchHits<?> hits = operations.search(createQuery(filters, sort, pageable), domainType);

			if (hits.getTotalHitsRelation() != TotalHitsRelation.EQUAL_TO) {
				throw new IllegalStateException("Cannot safely return all results for derived query " + method.toGenericString()
						+ ": Meilisearch did not report an exact total hit count");
			}

			long total = hits.getTotalHits();
			if (total < 0 || total > Integer.MAX_VALUE) {
				throw new IllegalStateException(
						"Cannot materialize " + total + " results for derived query " + method.toGenericString());
			}

			if (expectedTotal == null) {
				expectedTotal = total;
			} else if (expectedTotal.longValue() != total) {
				throw new IllegalStateException(
						"Result count changed while loading all results for derived query " + method.toGenericString());
			}

			List<? extends SearchHit<?>> pageHits = hits.getSearchHits();
			if (pageHits.isEmpty() && results.size() < expectedTotal) {
				throw incompleteResultSet(expectedTotal, results.size());
			}

			for (SearchHit<?> hit : pageHits) {
				results.add(hit.getContent());
			}

			if (results.size() > expectedTotal) {
				throw new IllegalStateException(
						"Search returned more results than the exact total for derived query " + method.toGenericString());
			}
			if (results.size() == expectedTotal) {
				return results;
			}
			if (pageHits.size() < FETCH_PAGE_SIZE) {
				throw incompleteResultSet(expectedTotal, results.size());
			}

			pageNumber++;
		}
	}

	private IllegalStateException incompleteResultSet(long expected, long loaded) {
		return new IllegalStateException("Meilisearch returned " + loaded + " of " + expected
				+ " exact matches for derived query " + method.toGenericString()
				+ "; the index maxTotalHits setting may prevent loading the complete result set");
	}

	private List<Object> contentsOf(SearchHits<?> hits) {

		List<Object> contents = new ArrayList<>(hits.getSearchHits().size());
		for (SearchHit<?> hit : hits.getSearchHits()) {
			contents.add(hit.getContent());
		}
		return contents;
	}

	private BasicQuery createQuery(List<String> filters, Sort sort, Pageable pageable) {

		BasicQueryBuilder builder = BasicQuery.builder().withQ("");
		if (!filters.isEmpty()) {
			builder.withFilter(filters.toArray(String[]::new));
		}
		if (sort.isSorted()) {
			builder.withSort(sort);
		}
		return builder.withPageable(pageable).build();
	}

	private Pageable mapPageable(Pageable pageable) {
		return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), mapSort(pageable.getSort()));
	}

	private List<String> createFilters(ParametersParameterAccessor accessor) {

		List<String> filters = new ArrayList<>(parts.size());
		int parameterIndex = 0;

		for (Part part : parts) {
			PropertyReference property = resolveProperty(part.getProperty().toDotPath(), operatorName(part));
			String filter = createFilter(part, property, accessor, parameterIndex);
			if (filter != null) {
				filters.add(filter);
			}
			parameterIndex += part.getNumberOfArguments();
		}

		return filters;
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
			case IN -> createInFilter(field, property, part, accessor.getBindableValue(parameterIndex), false);
			case NOT_IN -> createInFilter(field, property, part, accessor.getBindableValue(parameterIndex), true);
			case GREATER_THAN -> field + " > " + toLiteral(accessor.getBindableValue(parameterIndex), property, part);
			case GREATER_THAN_EQUAL -> field + " >= " + toLiteral(accessor.getBindableValue(parameterIndex), property, part);
			case LESS_THAN -> field + " < " + toLiteral(accessor.getBindableValue(parameterIndex), property, part);
			case LESS_THAN_EQUAL -> field + " <= " + toLiteral(accessor.getBindableValue(parameterIndex), property, part);
			case BETWEEN -> field + " " + toLiteral(accessor.getBindableValue(parameterIndex), property, part) + " TO "
					+ toLiteral(accessor.getBindableValue(parameterIndex + 1), property, part);
			case TRUE -> field + " = true";
			case FALSE -> field + " = false";
			default -> throw unsupportedOperator(operatorName(part));
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

		if (converted instanceof Boolean || converted instanceof Number) {
			if (converted instanceof Double doubleValue && !Double.isFinite(doubleValue)) {
				throw invalidParameter(part, "non-finite numbers cannot be used in filters");
			}
			if (converted instanceof Float floatValue && !Float.isFinite(floatValue)) {
				throw invalidParameter(part, "non-finite numbers cannot be used in filters");
			}
			return converted.toString();
		}
		if (converted instanceof Date date) {
			return Long.toString(date.getTime());
		}
		if (converted instanceof Enum<?> enumValue) {
			return quote(enumValue.name());
		}
		if (converted instanceof CharSequence || converted instanceof Character || converted instanceof TemporalAccessor
				|| converted instanceof UUID) {
			return quote(converted.toString());
		}

		throw invalidParameter(part, "unsupported filter value type " + converted.getClass().getName());
	}

	private static String quote(String value) {

		StringBuilder literal = new StringBuilder(value.length() + 2);
		literal.append('"');
		for (int i = 0; i < value.length(); i++) {
			char character = value.charAt(i);
			switch (character) {
				case '\\', '"' -> literal.append('\\').append(character);
				case '\n' -> literal.append("\\n");
				case '\r' -> literal.append("\\r");
				case '\t' -> literal.append("\\t");
				case '\b' -> literal.append("\\b");
				case '\f' -> literal.append("\\f");
				default -> {
					if (character < 0x20) {
						literal.append("\\u00");
						literal.append(Character.forDigit((character >> 4) & 0xf, 16));
						literal.append(Character.forDigit(character & 0xf, 16));
					} else {
						literal.append(character);
					}
				}
			}
		}
		literal.append('"');
		return literal.toString();
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
					"Invalid property '" + pathName + "' for derived query method " + method.toGenericString(), exception);
		}
	}

	private Sort mapSort(Sort sort) {

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

	private boolean isFilterFieldName(String fieldName) {
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

	private PartTree createPartTree() {

		try {
			return new PartTree(method.getName(), domainType);
		} catch (RuntimeException exception) {
			throw new IllegalArgumentException(
					"Cannot parse derived query method " + method.toGenericString() + ": " + exception.getMessage(), exception);
		}
	}

	private List<Part> validateTreeAndGetParts() {
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
		int disjunctions = 0;
		for (PartTree.OrPart orPart : tree) {
			if (++disjunctions > 1) {
				throw unsupportedOperator("Or");
			}
			for (Part part : orPart) {
				if (!isSupported(part.getType())) {
					throw unsupportedOperator(operatorName(part));
				}
				if (part.shouldIgnoreCase() != Part.IgnoreCaseType.NEVER) {
					throw unsupportedOperator("IgnoreCase");
				}

				PropertyReference property = resolveProperty(part.getProperty().toDotPath(), operatorName(part));
				if ((part.getType() == Part.Type.TRUE || part.getType() == Part.Type.FALSE)
						&& ClassUtils.resolvePrimitiveIfNecessary(property.valueType()) != Boolean.class) {
					throw unsupportedOperator(operatorName(part) + " on non-boolean property " + part.getProperty().toDotPath());
				}
				parsedParts.add(part);
			}
		}

		int expectedParameters = parsedParts.stream().mapToInt(Part::getNumberOfArguments).sum();
		int actualParameters = queryMethod.getParameters().getBindableParameters().getNumberOfParameters();
		if (expectedParameters != actualParameters) {
			throw unsupportedOperator(
					"parameter count (expected " + expectedParameters + ", found " + actualParameters + ")");
		}
		validateInParameters(parsedParts);

		return List.copyOf(parsedParts);
	}

	private void validateInParameters(List<Part> parsedParts) {

		int parameterIndex = 0;
		for (Part part : parsedParts) {
			if (part.getType() == Part.Type.IN || part.getType() == Part.Type.NOT_IN) {
				Class<?> parameterType = queryMethod.getParameters().getBindableParameter(parameterIndex).getType();
				if (!Iterable.class.isAssignableFrom(parameterType) && !parameterType.isArray()) {
					throw unsupportedOperator(operatorName(part) + " requires a collection or array parameter");
				}
			}
			parameterIndex += part.getNumberOfArguments();
		}
	}

	private void validateSpecialParameters() {

		var parameters = queryMethod.getParameters();
		if (parameters.hasDynamicProjection()) {
			throw unsupportedOperator("dynamic projection");
		}
		if (parameters.hasScrollPositionParameter()) {
			throw unsupportedOperator("ScrollPosition");
		}
		if (parameters.hasLimitParameter()) {
			throw unsupportedOperator("Limit");
		}
		if ((returnShape == ReturnShape.COUNT || returnShape == ReturnShape.EXISTS
				|| returnShape == ReturnShape.DELETE_COUNT || returnShape == ReturnShape.DELETE_VOID)
				&& (tree.getSort().isSorted() || parameters.hasSortParameter() || parameters.hasPageableParameter())) {
			throw unsupportedOperator("Sort/Pageable for a count, exists, or delete method");
		}
		if ((returnShape == ReturnShape.ENTITY || returnShape == ReturnShape.OPTIONAL)
				&& parameters.hasPageableParameter()) {
			throw unsupportedOperator("Pageable for a single-result method");
		}
	}

	private ReturnShape resolveReturnShape() {

		Class<?> returnType = method.getReturnType();
		if (tree.isCountProjection()) {
			if (returnType == long.class || returnType == Long.class) {
				return ReturnShape.COUNT;
			}
			throw unsupportedReturnType(returnType);
		}
		if (tree.isExistsProjection()) {
			if (returnType == boolean.class || returnType == Boolean.class) {
				return ReturnShape.EXISTS;
			}
			throw unsupportedReturnType(returnType);
		}
		if (tree.isDelete()) {
			if (returnType == void.class) {
				return ReturnShape.DELETE_VOID;
			}
			if (returnType == long.class || returnType == Long.class) {
				return ReturnShape.DELETE_COUNT;
			}
			throw unsupportedReturnType(returnType);
		}
		if (returnType == Page.class) {
			if (!queryMethod.isQueryForEntity()) {
				throw unsupportedReturnType(returnType);
			}
			return ReturnShape.PAGE;
		}
		if (returnType == Optional.class) {
			if (!queryMethod.isQueryForEntity()) {
				throw unsupportedReturnType(returnType);
			}
			return ReturnShape.OPTIONAL;
		}
		if (returnType == List.class) {
			if (!queryMethod.isQueryForEntity()) {
				throw unsupportedReturnType(returnType);
			}
			return ReturnShape.LIST;
		}
		if (returnType == Iterable.class) {
			if (!queryMethod.isQueryForEntity()) {
				throw unsupportedReturnType(returnType);
			}
			return ReturnShape.ITERABLE;
		}
		if (queryMethod.isQueryForEntity() && returnType.isAssignableFrom(domainType)) {
			return ReturnShape.ENTITY;
		}
		throw unsupportedReturnType(returnType);
	}

	private IllegalArgumentException unsupportedReturnType(Class<?> returnType) {
		return new IllegalArgumentException(
				"Unsupported derived query return type " + returnType.getName() + " in method " + method.toGenericString());
	}

	private static boolean isSupported(Part.Type type) {
		return switch (type) {
			case SIMPLE_PROPERTY, IN, NOT_IN, GREATER_THAN, GREATER_THAN_EQUAL, LESS_THAN, LESS_THAN_EQUAL, BETWEEN, TRUE,
					FALSE ->
				true;
			default -> false;
		};
	}

	private static String operatorName(Part part) {
		return part.getType().getKeywords().stream().findFirst().orElse(part.getType().name());
	}

	private IllegalArgumentException unsupportedOperator(String operator) {
		return new IllegalArgumentException(
				"Unsupported derived query operator '" + operator + "' in method " + method.toGenericString());
	}

	private IllegalArgumentException invalidParameter(Part part, String reason) {
		return new IllegalArgumentException("Invalid value for derived query operator '" + operatorName(part)
				+ "' in method " + method.toGenericString() + ": " + reason);
	}

	private record PropertyReference(String fieldName, Class<?> valueType) {
	}

	private enum ReturnShape {
		ENTITY, OPTIONAL, LIST, ITERABLE, PAGE, COUNT, EXISTS, DELETE_COUNT, DELETE_VOID
	}
}
