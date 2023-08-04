package io.vanslog.spring.data.meilisearch.core.mapping;

import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.util.Assert;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Meilisearch specific {@link PersistentEntity} implementation holding.
 *
 * @author Junghoon Ban
 * @since 1.0.0
 */
public class SimpleMeilisearchPersistentProperty
        extends AnnotationBasedPersistentProperty<MeilisearchPersistentProperty>
        implements MeilisearchPersistentProperty {

    private static final List<String> SUPPORTED_ID_PROPERTY_NAMES =
            List.of("id");

    /**
     * Creates a new {@link SimpleMeilisearchPersistentProperty}.
     *
     * @param property The property to be persisted.
     * @param owner The entity that owns this property.
     * @param simpleTypeHolder The holder for simple types.
        */
    public SimpleMeilisearchPersistentProperty(Property property,
       PersistentEntity<?, MeilisearchPersistentProperty> owner,
       SimpleTypeHolder simpleTypeHolder) {
        super(property, owner, simpleTypeHolder);
    }

    @Override
    protected Association<MeilisearchPersistentProperty> createAssociation() {
        return null;
    }

    @Override
    public String getFieldName() {
        Field field = super.getField();
        Assert.notNull(field,
                String.format("Invalid field name for property %s.", field));
        return field.getName();
    }

    @Override
    public boolean isIdProperty() {
        return super.isIdProperty()
                || SUPPORTED_ID_PROPERTY_NAMES.contains(getFieldName());
    }
}
