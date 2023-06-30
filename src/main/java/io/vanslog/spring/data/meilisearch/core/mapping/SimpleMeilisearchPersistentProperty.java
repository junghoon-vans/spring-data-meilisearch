package io.vanslog.spring.data.meilisearch.core.mapping;

import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;

/**
 * Meilisearch specific {@link PersistentEntity} implementation holding
 *
 * @since 1.0.0
 * @author Junghoon Ban
 */
public class SimpleMeilisearchPersistentProperty extends
		AnnotationBasedPersistentProperty<MeilisearchPersistentProperty> implements
		MeilisearchPersistentProperty {

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
		return super.getField().getName();
	}
}
