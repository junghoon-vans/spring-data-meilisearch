package io.vanslog.spring.data.meilisearch;

import org.springframework.dao.UncategorizedDataAccessException;

/**
 * Uncategorized exception for Meilisearch.
 *
 * @author Junghoon Ban
 */
public class UncategorizedMeilisearchException extends
        UncategorizedDataAccessException {

    /**
     * Constructor for UncategorizedMeilisearchException.
     * @param message the detail message
     */
    public UncategorizedMeilisearchException(String message) {
        super(message, null);
    }

    /**
     * Constructor for UncategorizedMeilisearchException.
     * @param message the detail message
     * @param cause the root cause from the data access API in use
     */
    public UncategorizedMeilisearchException(String message, Throwable cause) {
        super(message, cause);
    }
}
