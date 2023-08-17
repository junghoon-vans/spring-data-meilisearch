package io.vanslog.spring.data.meilisearch;

import org.springframework.dao.NonTransientDataAccessResourceException;

/**
 * Exception indicating that an index could not be found.
 *
 * @author Junghoon Ban
 */
public class IndexAccessException extends
        NonTransientDataAccessResourceException {

    private final String indexUid;

    /**
     * Constructor for IndexAccessException.
     * @param indexUid the index uid
     */
    public IndexAccessException(String indexUid) {
        super(String.format("Index %s not should be created.", indexUid));
        this.indexUid = indexUid;
    }

    /**
     * Constructor for IndexAccessException.
     * @param indexUid the index uid
     * @param cause the root cause from the data access API in use
     */
    public IndexAccessException(String indexUid, Throwable cause) {
        super(String.format("Index %s not should be created.", indexUid), cause);
        this.indexUid = indexUid;
    }

    public String getIndexUid() {
        return indexUid;
    }
}
