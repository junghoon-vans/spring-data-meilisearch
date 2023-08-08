package io.vanslog.spring.data.meilisearch.core;

import com.meilisearch.sdk.Client;
import io.vanslog.spring.data.meilisearch.core.convert.MappingMeilisearchConverter;
import io.vanslog.spring.data.meilisearch.core.convert.MeilisearchConverter;
import io.vanslog.spring.data.meilisearch.core.mapping.SimpleMeilisearchMappingContext;
import java.util.List;

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

    public MeilisearchTemplate(Client client) {
        this.client = client;
        this.meilisearchConverter = new MappingMeilisearchConverter(
                new SimpleMeilisearchMappingContext());
    }

    public MeilisearchTemplate(Client client,
                               MeilisearchConverter meilisearchConverter) {
        this.client = client;
        this.meilisearchConverter = meilisearchConverter;
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
}
