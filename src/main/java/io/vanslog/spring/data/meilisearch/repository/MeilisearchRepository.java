package io.vanslog.spring.data.meilisearch.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Repository interface for Meilisearch.
 *
 * @param <T>  The type of the domain class
 * @param <ID> The type of the id field
 * @author Junghoon Ban
 * @see org.springframework.data.repository.CrudRepository
 */
@NoRepositoryBean
public interface MeilisearchRepository<T, ID> extends CrudRepository<T, ID> {

}
