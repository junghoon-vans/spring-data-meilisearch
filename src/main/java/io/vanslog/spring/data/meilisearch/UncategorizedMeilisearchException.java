package io.vanslog.spring.data.meilisearch;

import org.springframework.dao.UncategorizedDataAccessException;

public class UncategorizedMeilisearchException extends
        UncategorizedDataAccessException {

    public UncategorizedMeilisearchException(String message) {
        super(message, null);
    }

    public UncategorizedMeilisearchException(String message, Throwable cause) {
        super(message, cause);
    }
}
