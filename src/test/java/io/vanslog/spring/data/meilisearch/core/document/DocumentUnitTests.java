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
package io.vanslog.spring.data.meilisearch.core.document;

import static org.assertj.core.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Document}.
 *
 * @author Junghoon Ban
 */
class DocumentUnitTests {

    @Test
    void shouldCreateEmptyDocument() {
        Document document = Document.create();
        assertThat(document).isNotNull();
        assertThat(document.isEmpty()).isTrue();
    }

    @Test
    void shouldCreateDocumentFromMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", 123);

        Document document = Document.from(map);
        assertThat(document).isNotNull();
        assertThat(document.isEmpty()).isFalse();
        assertThat(document.size()).isEqualTo(2);
        assertThat(document.get("key1")).isEqualTo("value1");
        assertThat(document.get("key2")).isEqualTo(123);
    }

    @Test
    void shouldParseJson() {
        String json = "{\"key1\":\"value1\",\"key2\":123}";
        Document document = Document.parse(json);
        assertThat(document).isNotNull();
        assertThat(document.isEmpty()).isFalse();
        assertThat(document.size()).isEqualTo(2);
        assertThat(document.get("key1")).isEqualTo("value1");
        assertThat(document.get("key2")).isEqualTo(123);
    }

    @Test
    void shouldSetAndGetId() {
        Document document = Document.create();
        assertThat(document.hasId()).isFalse();
        
        document.setId("test-id");
        assertThat(document.hasId()).isTrue();
        assertThat(document.getId()).isEqualTo("test-id");
    }

    @Test
    void shouldThrowExceptionWhenGettingIdWithoutSettingIt() {
        Document document = Document.create();
        assertThatThrownBy(document::getId)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No Id associated with this Document");
    }

    @Test
    void shouldPutAndGetValues() {
        Document document = Document.create();
        document.put("key1", "value1");
        document.put("key2", 123);

        assertThat(document.get("key1")).isEqualTo("value1");
        assertThat(document.get("key2")).isEqualTo(123);
        assertThat(document.get("key3")).isNull();
    }

    @Test
    void shouldGetTypedValues() {
        Document document = Document.create();
        document.put("stringKey", "value");
        document.put("intKey", 123);
        document.put("booleanKey", true);

        assertThat(document.getString("stringKey")).isEqualTo("value");
        assertThat(document.getInt("intKey")).isEqualTo(123);
        assertThat(document.getBoolean("booleanKey")).isTrue();
    }

    @Test
    void shouldGetDefaultValuesWhenKeyDoesNotExist() {
        Document document = Document.create();

        assertThat(document.getStringOrDefault("nonExistingKey", "default")).isEqualTo("default");
        assertThat(document.getIntOrDefault("nonExistingKey", 456)).isEqualTo(456);
        assertThat(document.getBooleanOrDefault("nonExistingKey", true)).isTrue();
    }

    @Test
    void shouldConvertToJson() {
        Document document = Document.create();
        document.put("key1", "value1");
        document.put("key2", 123);

        String json = document.toJson();
        assertThat(json).contains("\"key1\":\"value1\"");
        assertThat(json).contains("\"key2\":123");
    }

    @Test
    void shouldTransformDocument() {
        Document document = Document.create();
        document.put("key1", "value1");
        document.put("key2", 123);

        String result = document.transform(doc -> doc.getString("key1") + "-" + doc.getInt("key2"));
        assertThat(result).isEqualTo("value1-123");
    }
}
