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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
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
	void preservesNumericPrecisionBeyondFloatingPointRange() throws Exception {

		MeilisearchDeclaredQueryBinding binding = binding("numberValue", "amount = ?0", "", Number.class);
		assertThat(binding.bind(new Object[] { new BigInteger("9007199254740993") }).filter())
				.as("Integer filter binding must not round through double precision").isEqualTo("amount = 9007199254740993");
		assertThat(binding.bind(new Object[] { new BigDecimal("1234567890.12345678901234567890") }).filter())
				.as("Decimal filter binding must preserve all significant digits and scale")
				.isEqualTo("amount = 1234567890.12345678901234567890");
	}

	@Test
	void rejectsNumericSubclassesWithUntrustedStringRepresentations() throws Exception {

		MeilisearchDeclaredQueryBinding binding = binding("numberValue", "amount = ?0", "", Number.class);
		BigDecimal value = new BigDecimal("1") {

			@Override
			public String toString() {
				return "0 OR active = true";
			}
		};
		assertThatIllegalArgumentException().isThrownBy(() -> binding.bind(new Object[] { value }));
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
	void rejectsMalformedFilterAndQPlaceholdersAtConstruction() {

		for (String placeholder : List.of("?name", "?", "?-1", "?+0", "?{}")) {
			assertInvalid("noArguments", "name = " + placeholder, "", new Class<?>[0]);
			assertInvalid("noArguments", "", placeholder, new Class<?>[0]);
		}
	}

	@Test
	void rejectsNonAsciiPositionalDigitsInFilterAndQ() {

		String placeholder = "?\u0660";
		assertInvalid("stringValue", "name = " + placeholder, "", String.class);
		assertInvalid("stringValue", "", placeholder, String.class);
	}

	@Test
	void rejectsMixedAsciiAndNonAsciiPositionalDigits() {
		String placeholder = "?0\u0660";
		assertInvalid("stringValue", "name = " + placeholder, "", String.class);
		assertInvalid("stringValue", "", placeholder, String.class);
	}

	@Test
	void rejectsBmpDigitImmediatelyAfterMultiDigitPlaceholder() {

		assertInvalidDigitAfterElevenPlaceholder("\u0661");
	}

	@Test
	void rejectsSupplementaryDigitImmediatelyAfterMultiDigitPlaceholder() {

		assertInvalidDigitAfterElevenPlaceholder("\uD835\uDFD8");
	}

	@Test
	void preservesUnicodeQTextSeparatedFromPlaceholder() throws Exception {

		MeilisearchDeclaredQueryBinding binding = binding("stringValue", "", "?0 \u0661", String.class);
		assertThat(binding.bind(new Object[] { "cats ?10" }).q()).isEqualTo("cats ?10 \u0661");
	}

	@Test
	void rejectsUnsupportedDeclaredCollectionElementTypes() {

		assertInvalid("unsupportedCollection", "name IN ?0", "", List.class);
		assertInvalid("nestedCollection", "name IN ?0", "", List.class);
	}

	@Test
	void rejectsNestedArraysAndArraysOfCollections() {
		assertInvalid("nestedArrays", "name IN ?0", "", String[][].class);
		assertInvalid("arrayOfCollections", "name IN ?0", "", List[].class);
		assertInvalid("arrayElements", "name IN ?0", "", List.class);
	}

	@Test
	void supportsScalarArraysAndRuntimeChecksRawCollections() throws Exception {

		MeilisearchDeclaredQueryBinding strings = binding("stringArrayValues", "name IN ?0", "", String[].class);
		assertThat(strings.bind(new Object[] { new String[] { "books" } }).filter()).isEqualTo("name IN [\"books\"]");

		MeilisearchDeclaredQueryBinding integers = binding("integerArrayValues", "id IN ?0", "", int[].class);
		assertThat(integers.bind(new Object[] { new int[] { 7, 9 } }).filter()).isEqualTo("id IN [7, 9]");

		MeilisearchDeclaredQueryBinding numbers = binding("numberCollection", "amount IN ?0", "", List.class);
		assertThat(numbers.bind(new Object[] { List.of(1, 2) }).filter()).isEqualTo("amount IN [1, 2]");

		MeilisearchDeclaredQueryBinding raw = binding("rawCollection", "name IN ?0", "", List.class);
		assertThat(raw.bind(new Object[] { List.of("books") }).filter()).isEqualTo("name IN [\"books\"]");
		assertThatIllegalArgumentException().isThrownBy(() -> raw.bind(new Object[] { List.of(List.of("nested")) }));
	}

	@Test
	void resolvesInheritedGenericParameterTypesUsingRepositoryMetadata() throws Exception {

		Method method = StringNameRepository.class.getMethod("search", Serializable.class);
		var metadata = new DefaultRepositoryMetadata(StringNameRepository.class);
		QueryMethod queryMethod = new QueryMethod(method, metadata, new SpelAwareProxyProjectionFactory());
		MeilisearchDeclaredQueryBinding binding = new MeilisearchDeclaredQueryBinding(method, queryMethod,
				metadata.getRepositoryInterface(), "name = ?0", "");

		assertThat(binding.bind(new Object[] { "name" }).filter()).isEqualTo("name = \"name\"");
	}

	@Test
	void resolvesInheritedCollectionElementTypesFromRepositoryMetadata() throws Exception {

		Method supportedMethod = StringCollectionRepository.class.getMethod("searchByValues", List.class);
		MeilisearchDeclaredQueryBinding supported = repositoryBinding(supportedMethod, StringCollectionRepository.class);
		assertThat(supported.bind(new Object[] { List.of("books") }).filter()).isEqualTo("name IN [\"books\"]");

		Method unsupportedMethod = UnsupportedCollectionRepository.class.getMethod("searchByValues", List.class);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> repositoryBinding(unsupportedMethod, UnsupportedCollectionRepository.class));
	}

	@Test
	void rejectsKnownCollectionBindingsDespiteUnresolvedRepositoryVariables() throws Exception {

		for (Class<?> repository : List.of(SerializableCollectionRepository.class,
				PartiallyResolvedCollectionRepository.class)) {
			Method method = repository.getMethod("searchByValues", List.class);
			assertThatIllegalArgumentException().isThrownBy(() -> repositoryBinding(method, repository));
		}
	}

	@Test
	void keepsUnresolvedCollectionElementTypesUnderRuntimeValidation() throws Exception {

		Method method = GenericCollectionRepository.class.getMethod("searchByValues", List.class);
		MeilisearchDeclaredQueryBinding binding = repositoryBinding(method, GenericCollectionRepository.class);
		assertThat(binding.bind(new Object[] { List.of("books") }).filter()).isEqualTo("name IN [\"books\"]");
		assertThatIllegalArgumentException().isThrownBy(() -> binding.bind(new Object[] { List.of(List.of("nested")) }));
	}

	@Test
	void rejectsUnsupportedSerializableParameterType() throws Exception {

		Method method = SerializableNameRepository.class.getMethod("search", Serializable.class);
		var metadata = new DefaultRepositoryMetadata(SerializableNameRepository.class);
		QueryMethod queryMethod = new QueryMethod(method, metadata, new SpelAwareProxyProjectionFactory());

		assertThatThrownBy(() -> new MeilisearchDeclaredQueryBinding(method, queryMethod, metadata.getRepositoryInterface(),
				"name = ?0", "")).isInstanceOf(IllegalArgumentException.class);
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

	private static void assertInvalidDigitAfterElevenPlaceholder(String digit) {

		Class<?>[] parameterTypes = new Class<?>[11];
		for (int i = 0; i < parameterTypes.length; i++) {
			parameterTypes[i] = String.class;
		}
		assertInvalid("eleven", "", "?0 ?1 ?2 ?3 ?4 ?5 ?6 ?7 ?8 ?9 ?10" + digit, parameterTypes);
	}

	private static MeilisearchDeclaredQueryBinding repositoryBinding(Method method, Class<?> repositoryInterface) {

		var metadata = new DefaultRepositoryMetadata(repositoryInterface);
		QueryMethod queryMethod = new QueryMethod(method, metadata, new SpelAwareProxyProjectionFactory());
		return new MeilisearchDeclaredQueryBinding(method, queryMethod, repositoryInterface, "name IN ?0", "");
	}

	private static MeilisearchDeclaredQueryBinding binding(String methodName, String filter, String q,
			Class<?>... parameterTypes) throws NoSuchMethodException {
		Method method = SampleRepository.class.getMethod(methodName, parameterTypes);
		var metadata = new DefaultRepositoryMetadata(SampleRepository.class);
		QueryMethod queryMethod = new QueryMethod(method, metadata, new SpelAwareProxyProjectionFactory());
		return new MeilisearchDeclaredQueryBinding(method, queryMethod, SampleRepository.class, filter, q);
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

		List<Sample> unsupportedCollection(List<Sample> values);

		List<Sample> nestedCollection(List<List<String>> values);

		List<Sample> nestedArrays(String[][] values);

		List<Sample> arrayOfCollections(List<?>[] values);

		List<Sample> arrayElements(List<String[]> values);

		List<Sample> stringArrayValues(String[] values);

		List<Sample> integerArrayValues(int[] values);

		List<Sample> numberCollection(List<? extends Number> values);

		List<Sample> rawCollection(List values);
	}

	@NoRepositoryBean
	interface GenericNameRepository<T extends Serializable> extends Repository<Sample, String> {

		List<Sample> search(T name);
	}

	@NoRepositoryBean
	interface StringNameRepository extends GenericNameRepository<String> {}

	@NoRepositoryBean
	interface GenericCollectionRepository<T extends Serializable> extends Repository<Sample, String> {

		List<Sample> searchByValues(List<T> values);
	}

	@NoRepositoryBean
	interface StringCollectionRepository extends GenericCollectionRepository<String> {}

	@NoRepositoryBean
	interface UnsupportedCollectionRepository extends GenericCollectionRepository<UnsupportedCollectionElement> {}

	@NoRepositoryBean
	interface SerializableCollectionRepository extends GenericCollectionRepository<Serializable> {}

	@NoRepositoryBean
	interface PartiallyResolvedCollectionRepository<U>
			extends GenericCollectionRepository<UnsupportedCollectionElement> {}

	@NoRepositoryBean
	interface SerializableNameRepository extends Repository<Sample, String> {

		List<Sample> search(Serializable name);
	}

	static final class UnsupportedCollectionElement implements Serializable {}

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
