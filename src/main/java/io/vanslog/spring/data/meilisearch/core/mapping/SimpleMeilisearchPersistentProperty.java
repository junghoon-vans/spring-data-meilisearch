package io.vanslog.spring.data.meilisearch.core.mapping;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.util.Assert;

/**
 * Meilisearch specific {@link PersistentEntity} implementation holding
 *
 * @since 1.0.0
 * @author Junghoon Ban
 */
public class SimpleMeilisearchPersistentProperty extends
		AnnotationBasedPersistentProperty<MeilisearchPersistentProperty> implements
		MeilisearchPersistentProperty {

	private static final List<String> SUPPORTED_ID_PROPERTY_NAMES = Arrays.asList("id");

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
		Assert.notNull(field, String.format("Invalid field name for property %s.", field));
		return field.getName();
	}

	@Override
	public boolean isIdProperty() {
		return super.isIdProperty() || SUPPORTED_ID_PROPERTY_NAMES.contains(getFieldName());
	}
}
