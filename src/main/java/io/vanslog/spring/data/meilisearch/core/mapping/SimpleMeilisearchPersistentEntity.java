package io.vanslog.spring.data.meilisearch.core.mapping;

import io.vanslog.spring.data.meilisearch.annotations.Document;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.expression.BeanFactoryAccessor;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.util.TypeInformation;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Meilisearch specific {@link BasicPersistentEntity} implementation holding.
 *
 * @param <T>
 * @author Junghoon Ban
 */
public class SimpleMeilisearchPersistentEntity<T>
        extends BasicPersistentEntity<T, MeilisearchPersistentProperty>
        implements MeilisearchPersistentEntity<T>, ApplicationContextAware {

    private final StandardEvaluationContext context;
    @Nullable private String indexUid;

    /**
     * Creates a new {@link SimpleMeilisearchPersistentEntity} with the given {@link TypeInformation}.
     *
     * @param information must not be {@literal null}.
     */
    public SimpleMeilisearchPersistentEntity(TypeInformation<T> information) {
        super(information);
        this.context = new StandardEvaluationContext();

        Class<T> rawType = information.getType();
        if (rawType.isAnnotationPresent(Document.class)) {
            Document document = rawType.getAnnotation(Document.class);
            Assert.hasText(document.indexUid(),
                    "Unknown indexUid. Make sure the indexUid is defined."
                            + "e.g @Document(indexUid=\"foo\")");
            this.indexUid = document.indexUid();
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        context.addPropertyAccessor(new BeanFactoryAccessor());
        context.setBeanResolver(new BeanFactoryResolver(applicationContext));
        context.setRootObject(applicationContext);
    }

    @Override
    public String getIndexUid() {
        return indexUid;
    }
}
