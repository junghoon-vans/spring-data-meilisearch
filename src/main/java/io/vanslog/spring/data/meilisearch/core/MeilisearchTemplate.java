package io.vanslog.spring.data.meilisearch.core;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import com.meilisearch.sdk.json.GsonJsonHandler;
import com.meilisearch.sdk.json.JsonHandler;
import io.vanslog.spring.data.meilisearch.IndexAccessException;
import io.vanslog.spring.data.meilisearch.UncategorizedMeilisearchException;
import io.vanslog.spring.data.meilisearch.annotations.Document;
import io.vanslog.spring.data.meilisearch.core.convert.MappingMeilisearchConverter;
import io.vanslog.spring.data.meilisearch.core.convert.MeilisearchConverter;
import io.vanslog.spring.data.meilisearch.core.mapping.MeilisearchPersistentEntity;
import io.vanslog.spring.data.meilisearch.core.mapping.SimpleMeilisearchMappingContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Implementation of {@link MeilisearchOperations}.
 *
 * @author Junghoon Ban
 * @see com.meilisearch.sdk.Client
 * @see com.meilisearch.sdk.Index
 */
public class MeilisearchTemplate implements MeilisearchOperations {

    private final Client client;
    private final MeilisearchConverter meilisearchConverter;

    private final JsonHandler jsonHandler;

    public MeilisearchTemplate(Client client,
                               JsonHandler jsonHandler) {
        this(client, null, jsonHandler);
    }

    public MeilisearchTemplate(Client client,
                               @Nullable MeilisearchConverter meilisearchConverter,
                               @Nullable JsonHandler jsonHandler) {
        this.client = client;
        this.meilisearchConverter = meilisearchConverter != null ? meilisearchConverter
                : new MappingMeilisearchConverter(new SimpleMeilisearchMappingContext());
        this.jsonHandler = jsonHandler != null ? jsonHandler : new GsonJsonHandler();
    }

    @Override
    public <T> T save(T entity) {
        return null;
    }

    @Override
    public <T> List<T> save(List<T> entities) {
        return null;
    }

    @Override
    public <T> T get(String documentId, Class<T> clazz) {
        return null;
    }

    @Override
    public <T> List<T> multiGet(Class<T> clazz) {
        return null;
    }

    @Override
    public <T> List<T> multiGet(Class<T> clazz, String... documentIds) {
        return null;
    }

    @Override
    public boolean exists(String documentId, Class<?> clazz) {
        return false;
    }

    @Override
    public long count(Class<?> clazz) {
        return 0;
    }

    @Override
    public boolean delete(String documentId, Class<?> clazz) {
        return false;
    }

    @Override
    public boolean delete(Object entity) {
        return false;
    }

    @Override
    public boolean delete(Class<?> clazz, String... documentIds) {
        return false;
    }

    @Override
    public boolean delete(Iterable<?> entities) {
        return false;
    }

    @Override
    public boolean deleteAll(Class<?> clazz) {
        return false;
    }

    private <T> Index getIndexFor(Class<T> clazz) {
        MeilisearchPersistentEntity<?> persistentEntity =
                getPersistentEntityFor(clazz);
        String uid = persistentEntity.getIndexUid();
        try {
            return client.index(uid);
        } catch (MeilisearchException e) {
            throw new IndexAccessException(uid);
        }
    }

    private MeilisearchPersistentEntity<?> getPersistentEntityFor(
            Class<?> clazz) {
        Assert.hasText(clazz.getAnnotation(Document.class).indexUid(),
                "Given class must be annotated with @Document(indexUid = \"foo\")!");

        return meilisearchConverter.getMappingContext()
                .getRequiredPersistentEntity(clazz);
    }
}
