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

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;

/**
 * Unit tests for safe positional binding in declared Meilisearch queries.
 *
 * @author Junghoon Ban
 */
class MeilisearchDeclaredQueryBindingUnitTests {

	@Test
	void bindsQAndFilterUsingActualMethodParameterPositions() throws Exception {

		MeilisearchDeclaredQueryBinding binding = binding("mixed",
				"name = ?0 AND code = ?2 AND note = \"literal ? ( OR ) \\\"quoted\\\"\"", "q=?0", String.class, Pageable.class,
				String.class);

		MeilisearchDeclaredQueryBinding.BoundQuery query = binding
				.bind(new Object[] { "a\" OR active = true ?2", PageRequest.of(0, 5), "slash\\n" });

		assertThat(query.q()).isEqualTo("q=a\" OR active = true ?2");
		assertThat(query.filter()).isEqualTo(
				"name = \"a\\\" OR active = true ?2\" AND code = \"slash\\\\n\" AND note = \"literal ? ( OR ) \\\"quoted\\\"\"");
	}

	@Test
	void escapesFilterLiteralDelimitersAndControlCharacters() {

		String value = "quote\" slash\\ line\n carriage\r tab\t backspace\b formfeed\f control" + (char) 1;
		assertThat(MeilisearchFilterValue.quote(value))
				.isEqualTo("\"quote\\\" slash\\\\ line\\n carriage\\r tab\\t backspace\\b formfeed\\f control\\u0001\"");
	}

	@Test
	void consumesAllDigitsInPositionalIndex() throws Exception {

		String[] parameterTypes = new String[11];
		Class<?>[] types = new Class<?>[11];
		Object[] values = new Object[11];
		for (int i = 0; i < 11; i++) {
			parameterTypes[i] = "value" + i;
			types[i] = String.class;
			values[i] = parameterTypes[i];
		}

		String filter = IntStream.range(0, 11).mapToObj(i -> "field" + i + " = ?" + i)
				.collect(java.util.stream.Collectors.joining(" AND "));
		MeilisearchDeclaredQueryBinding binding = binding("eleven", filter, "", types);
		String result = binding.bind(values).filter();

		assertThat(result).contains("field1 = \"value1\"").contains("field10 = \"value10\"");
	}

	@Test
	void bindsEntireInCollectionsAndScalarArrayElements() throws Exception {

		MeilisearchDeclaredQueryBinding collectionBinding = binding("single", "tag IN ?0", "", Object.class);
		assertThat(collectionBinding.bind(new Object[] { new String[] { "fantasy", "x\" OR tag = \"secret" } }).filter())
				.isEqualTo("tag IN [\"fantasy\", \"x\\\" OR tag = \\\"secret\"]");

		MeilisearchDeclaredQueryBinding primitiveArrayBinding = binding("single", "id IN ?0", "", Object.class);
		assertThat(primitiveArrayBinding.bind(new Object[] { new int[] { 7, 9 } }).filter()).isEqualTo("id IN [7, 9]");

		MeilisearchDeclaredQueryBinding elementsBinding = binding("pair", "tag IN [?0, ?1]", "", String.class,
				String.class);
		assertThat(elementsBinding.bind(new Object[] { "science", "fiction" }).filter())
				.isEqualTo("tag IN [\"science\", \"fiction\"]");
	}

	@Test
	void bindsRangeAndGeoNumericValuesWithoutQuoting() throws Exception {

		MeilisearchDeclaredQueryBinding range = binding("dateValue", "createdAt 0 TO ?0", "", Date.class);
		assertThat(range.bind(new Object[] { new Date(1234) }).filter()).isEqualTo("createdAt 0 TO 1234");

		MeilisearchDeclaredQueryBinding radius = binding("geo", "_geoRadius(?0, ?1, ?2)", "", Number.class, Number.class,
				Number.class);
		assertThat(radius.bind(new Object[] { 48.8566, 2.3522, 2000 }).filter())
				.isEqualTo("_geoRadius(48.8566, 2.3522, 2000)");

		MeilisearchDeclaredQueryBinding boundingBox = binding("box", "_geoBoundingBox([?0, ?1], [?2, ?3])", "",
				Number.class, Number.class, Number.class, Number.class);
		assertThat(boundingBox.bind(new Object[] { 47.0, 6.0, 46.0, 5.0 }).filter())
				.isEqualTo("_geoBoundingBox([47.0, 6.0], [46.0, 5.0])");
		MeilisearchDeclaredQueryBinding polygon = binding("polygon", "_geoPolygon([?0, ?1], [?2, ?3], [0, 0])", "",
				Number.class, Number.class, Number.class, Number.class);
		assertThat(polygon.bind(new Object[] { 48.8, 2.3, 48.9, 2.4 }).filter())
				.isEqualTo("_geoPolygon([48.8, 2.3], [48.9, 2.4], [0, 0])");
	}

	@Test
	void bindsNumbersAndBigDecimalsAsNativeFilterValues() throws Exception {

		MeilisearchDeclaredQueryBinding binding = binding("numberValue", "amount = ?0", "", Number.class);
		assertThat(binding.bind(new Object[] { new BigDecimal("1E+3") }).filter()).isEqualTo("amount = 1000");
		assertThat(MeilisearchFilterValue.scalar(new Date(42))).isEqualTo("42");
	}

	@Test
	void bindsBothNativeRangeBoundsAndTextOperatorValues() throws Exception {

		MeilisearchDeclaredQueryBinding range = binding("numericRange", "(details.price ?0 TO ?1)", "", int.class,
				int.class);
		assertThat(range.bind(new Object[] { 10, 20 }).filter()).isEqualTo("(details.price 10 TO 20)");
		MeilisearchDeclaredQueryBinding prefix = binding("stringValue", "title STARTS WITH ?0", "", String.class);
		assertThat(prefix.bind(new Object[] { "Director's \"" }).filter())
				.isEqualTo("title STARTS WITH \"Director's \\\"\"");
		MeilisearchDeclaredQueryBinding contains = binding("stringValue", "title CONTAINS ?0", "", String.class);
		assertThat(contains.bind(new Object[] { "OR active = true" }).filter())
				.isEqualTo("title CONTAINS \"OR active = true\"");
	}

	@Test
	void bindsRangeBoundsAfterQuotedNativeFieldNames() throws Exception {

		MeilisearchDeclaredQueryBinding range = binding("numericRange", "\"unit price\" ?0 TO ?1", "", int.class,
				int.class);
		assertThat(range.bind(new Object[] { 10, 20 }).filter()).isEqualTo("\"unit price\" 10 TO 20");
	}

	@Test
	void keepsQTextRawAndRejectsBlankQOnlyBindings() throws Exception {

		MeilisearchDeclaredQueryBinding qOnly = binding("stringValue", "", "?0", String.class);
		assertThat(qOnly.bind(new Object[] { "cats \" OR tags = \"dogs" }).q()).isEqualTo("cats \" OR tags = \"dogs");
		assertThatIllegalArgumentException().isThrownBy(() -> qOnly.bind(new Object[] { " \t" }));

		MeilisearchDeclaredQueryBinding filterAndBlankQ = binding("stringValue", "tag = ?0", "?0", String.class);
		MeilisearchDeclaredQueryBinding.BoundQuery query = filterAndBlankQ.bind(new Object[] { " " });
		assertThat(query.q()).isEmpty();
		assertThat(query.filter()).isEqualTo("tag = \" \"");
	}

	@Test
	void rejectsUnsafeOrMalformedFilterPlaceholdersAtConstruction() {

		assertInvalid("stringValue", "name = \"?0\"", "", String.class);
		assertInvalid("stringValue", "?0 = true", "", String.class);
		assertInvalid("stringValue", "name ?0 true", "", String.class);
		assertInvalid("stringValue", "name = prefix?0", "", String.class);
		assertInvalid("stringValue", "name = ?0_suffix", "", String.class);
		assertInvalid("stringValue", "name = [?0]", "", String.class);
		assertInvalid("stringValue", "_untrustedFunction(?0)", "", String.class);
		assertInvalid("stringValue", "name = ?4", "", String.class);
		assertInvalid("stringValue", "tag IN ?0", "", String.class);
		assertInvalid("geoText", "_geoRadius(?0, ?1, ?2)", "", String.class, String.class, String.class);
		assertInvalid("mixed", "name = ?1 AND code = ?2", "", String.class, Pageable.class, String.class);
	}

	@Test
	void rejectsUnusedBindablesAndEmptyDeclarations() {

		assertInvalid("pair", "first = ?0", "", String.class, String.class);
		assertInvalid("noArguments", "", "", new Class<?>[0]);
	}

	@Test
	void rejectsNullEmptyNestedAndNonFiniteInValues() throws Exception {

		MeilisearchDeclaredQueryBinding collection = binding("single", "tag IN ?0", "", Object.class);
		MeilisearchDeclaredQueryBinding scalar = binding("stringValue", "name = ?0", "", String.class);
		assertThatIllegalArgumentException().isThrownBy(() -> scalar.bind(new Object[] { null }));
		assertThatIllegalArgumentException().isThrownBy(() -> collection.bind(new Object[] { null }));
		assertThatIllegalArgumentException().isThrownBy(() -> collection.bind(new Object[] { List.of() }));
		List<Object> valuesWithNull = new ArrayList<>();
		valuesWithNull.add("valid");
		valuesWithNull.add(null);
		assertThatIllegalArgumentException().isThrownBy(() -> collection.bind(new Object[] { valuesWithNull }));
		assertThatIllegalArgumentException().isThrownBy(() -> collection.bind(new Object[] { List.of(List.of("nested")) }));

		MeilisearchDeclaredQueryBinding numeric = binding("numberValue", "value = ?0", "", Number.class);
		assertThatIllegalArgumentException().isThrownBy(() -> numeric.bind(new Object[] { Double.NaN }));
		assertThatIllegalArgumentException().isThrownBy(() -> numeric.bind(new Object[] { new MaliciousNumber() }));
	}

	@Test
	void rejectsNonNumericGeoArgumentsBeforeBinding() throws Exception {

		MeilisearchDeclaredQueryBinding radius = binding("geo", "_geoRadius(?0, ?1, ?2)", "", Number.class, Number.class,
				Number.class);
		assertThatIllegalArgumentException().isThrownBy(() -> radius.bind(new Object[] { 48.8, 2.3, "1000) OR true" }));
	}

	private static void assertInvalid(String methodName, String filter, String q, Class<?>... parameterTypes) {
		assertThatThrownBy(() -> binding(methodName, filter, q, parameterTypes))
				.isInstanceOf(IllegalArgumentException.class);
	}

	private static MeilisearchDeclaredQueryBinding binding(String methodName, String filter, String q,
			Class<?>... parameterTypes) throws NoSuchMethodException {
		Method method = SampleRepository.class.getMethod(methodName, parameterTypes);
		QueryMethod queryMethod = new QueryMethod(method, new DefaultRepositoryMetadata(SampleRepository.class),
				new SpelAwareProxyProjectionFactory());
		return new MeilisearchDeclaredQueryBinding(method, queryMethod, filter, q);
	}

	@NoRepositoryBean
	interface SampleRepository extends Repository<Sample, String> {

		List<Sample> mixed(String name, Pageable pageable, String code);

		List<Sample> eleven(String value0, String value1, String value2, String value3, String value4, String value5,
				String value6, String value7, String value8, String value9, String value10);

		List<Sample> single(Object value);

		List<Sample> stringValue(String value);

		List<Sample> dateValue(Date value);

		List<Sample> numberValue(Number value);

		List<Sample> pair(String first, String second);

		List<Sample> numericRange(int minimum, int maximum);

		List<Sample> geo(Number latitude, Number longitude, Number radius);

		List<Sample> geoText(String latitude, String longitude, String radius);

		List<Sample> box(Number northLatitude, Number eastLongitude, Number southLatitude, Number westLongitude);

		List<Sample> polygon(Number firstLatitude, Number firstLongitude, Number secondLatitude, Number secondLongitude);

		List<Sample> noArguments();
	}

	static class Sample {}

	static final class MaliciousNumber extends Number {

		@Override
		public int intValue() {
			return 1;
		}

		@Override
		public long longValue() {
			return 1L;
		}

		@Override
		public float floatValue() {
			return 1.0f;
		}

		@Override
		public double doubleValue() {
			return 1.0;
		}

		@Override
		public String toString() {
			return "0) OR isAdmin = true OR id = 0";
		}
	}
}
