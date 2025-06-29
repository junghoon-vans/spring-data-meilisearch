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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import org.springframework.data.mapping.MappingException;
import org.springframework.lang.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.vanslog.spring.data.meilisearch.core.support.DefaultStringObjectMap;

/**
 * {@link Document} implementation backed by a {@link LinkedHashMap}.
 *
 * @author Junghoon Ban
 */
class MapDocument implements Document {

    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final DefaultStringObjectMap<?> documentAsMap;

    private @Nullable String id;

    MapDocument() {
        this(new LinkedHashMap<>());
    }

    MapDocument(Map<String, ?> documentAsMap) {
        this.documentAsMap = new DefaultStringObjectMap<>(documentAsMap);
    }

    /*
     * (non-Javadoc)
     * @see io.vanslog.spring.data.meilisearch.core.document.Document#hasId()
     */
    @Override
    public boolean hasId() {
        return this.id != null;
    }

    /*
     * (non-Javadoc)
     * @see io.vanslog.spring.data.meilisearch.core.document.Document#getId()
     */
    @Override
    public String getId() {

        if (!hasId()) {
            throw new IllegalStateException("No Id associated with this Document");
        }

        return this.id;
    }

    /*
     * (non-Javadoc)
     * @see io.vanslog.spring.data.meilisearch.core.document.Document#setId(java.lang.String)
     */
    @Override
    public void setId(String id) {
        this.id = id;
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#size()
     */
    @Override
    public int size() {
        return documentAsMap.size();
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        return documentAsMap.isEmpty();
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    @Override
    public boolean containsKey(Object key) {
        return documentAsMap.containsKey(key);
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#containsValue(java.lang.Object)
     */
    @Override
    public boolean containsValue(Object value) {
        return documentAsMap.containsValue(value);
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#get(java.lang.Object)
     */
    @Override
    public Object get(Object key) {
        return documentAsMap.get(key);
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#getOrDefault(java.lang.Object, java.lang.Object)
     */
    @Override
    public Object getOrDefault(Object key, Object defaultValue) {
        return documentAsMap.getOrDefault(key, defaultValue);
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    @Override
    public Object put(String key, Object value) {
        return documentAsMap.put(key, value);
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#remove(java.lang.Object)
     */
    @Override
    public Object remove(Object key) {
        return documentAsMap.remove(key);
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#putAll(Map)
     */
    @Override
    public void putAll(Map<? extends String, ?> m) {
        documentAsMap.putAll(m);
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#clear()
     */
    @Override
    public void clear() {
        documentAsMap.clear();
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#keySet()
     */
    @Override
    public Set<String> keySet() {
        return documentAsMap.keySet();
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#values()
     */
    @Override
    public Collection<Object> values() {
        return documentAsMap.values();
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#entrySet()
     */
    @Override
    public Set<Entry<String, Object>> entrySet() {
        return documentAsMap.entrySet();
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        return documentAsMap.equals(o);
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return documentAsMap.hashCode();
    }

    /*
     * (non-Javadoc)
     * @see java.util.Map#forEach(java.util.function.BiConsumer)
     */
    @Override
    public void forEach(BiConsumer<? super String, ? super Object> action) {
        documentAsMap.forEach(action);
    }

    /*
     * (non-Javadoc)
     * @see io.vanslog.spring.data.meilisearch.core.document.Document#toJson()
     */
    @Override
    public String toJson() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new MappingException("Cannot render document to JSON", e);
        }
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        String id = hasId() ? getId() : "?";
        return getClass().getSimpleName() + '@' + id + ' ' + toJson();
    }
}
