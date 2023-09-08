package io.vanslog.spring.data.meilisearch;

import org.springframework.dao.DataAccessException;

/**
 * Exception thrown when there is an error accessing a document.
 *
 * @author Junghoon Ban
 */
public class DocumentAccessException extends DataAccessException {

  public DocumentAccessException(String message) {
    super(message);
  }

  public DocumentAccessException(String message, Throwable cause) {
    super(message, cause);
  }

}
