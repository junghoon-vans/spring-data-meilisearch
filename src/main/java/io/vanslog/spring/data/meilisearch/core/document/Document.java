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

import java.io.IOException;
import java.util.Map;
import java.util.function.Function;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import io.vanslog.spring.data.meilisearch.core.support.StringObjectMap;

/**
 * A representation of a Meilisearch document as extended {@link StringObjectMap Map}. All iterators preserve
 * original insertion order.
 * <p>
 * Document does not allow {@code null} keys. It allows {@literal null} values.
 * <p>
 * Implementing classes can be either mutable or immutable. In case a subclass is immutable, its methods may throw
 * {@link UnsupportedOperationException} when calling modifying methods.
 *
 * @author Junghoon Ban
 */
public interface Document extends StringObjectMap<Document> {

    /**
     * Create a new mutable {@link Document}.
     *
     * @return a new {@link Document}.
     */
    static Document create() {
        return new MapDocument();
    }

    /**
     * Create a {@link Document} from a {@link Map} containing key-value pairs and sub-documents.
     *
     * @param map source map containing key-value pairs and sub-documents. must not be {@literal null}.
     * @return a new {@link Document}.
     */
    static Document from(Map<String, ?> map) {

        Assert.notNull(map, "Map must not be null");

        return new MapDocument(map);
    }

    /**
     * Parse JSON to {@link Document}.
     *
     * @param json must not be {@literal null}.
     * @return the parsed {@link Document}.
     */
    static Document parse(String json) {

        Assert.notNull(json, "JSON must not be null");

        return new MapDocument().fromJson(json);
    }

    @Override
    default Document fromJson(String json) {
        Assert.notNull(json, "JSON must not be null");

        clear();
        try {
            putAll(MapDocument.OBJECT_MAPPER.readerFor(Map.class).readValue(json));
        } catch (Exception e) {
            throw new RuntimeException("Cannot parse JSON", e);
        }
        return this;
    }

    /**
     * Return {@literal true} if this {@link Document} is associated with an identifier.
     *
     * @return {@literal true} if this {@link Document} is associated with an identifier, {@literal false} otherwise.
     */
    default boolean hasId() {
        return false;
    }

    /**
     * Retrieve the identifier associated with this {@link Document}.
     * <p>
     * The default implementation throws {@link UnsupportedOperationException}. It's recommended to check {@link #hasId()}
     * prior to calling this method.
     *
     * @return the identifier associated with this {@link Document}.
     * @throws IllegalStateException if the underlying implementation supports Id's but no Id was yet associated with the
     *           document.
     */
    default String getId() {
        throw new UnsupportedOperationException();
    }

    /**
     * Set the identifier for this {@link Document}.
     * <p>
     * The default implementation throws {@link UnsupportedOperationException}.
     */
    default void setId(String id) {
        throw new UnsupportedOperationException();
    }

    /**
     * This method allows the application of a function to {@code this} {@link Document}. The function should expect a
     * single {@link Document} argument and produce an {@code R} result.
     * <p>
     * Any exception thrown by the function will be propagated to the caller.
     *
     * @param transformer functional interface to a apply. must not be {@literal null}.
     * @param <R> class of the result
     * @return the result of applying the function to this string
     * @see java.util.function.Function
     */
    default <R> R transform(Function<? super Document, ? extends R> transformer) {

        Assert.notNull(transformer, "transformer must not be null");

        return transformer.apply(this);
    }
}
