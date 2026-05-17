/*
 * Copyright 2023-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.vanslog.spring.data.meilisearch.core.convert;

import io.vanslog.spring.data.meilisearch.core.document.MeilisearchDocument;
import io.vanslog.spring.data.meilisearch.core.mapping.MeilisearchPersistentEntity;
import io.vanslog.spring.data.meilisearch.core.mapping.MeilisearchPersistentProperty;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.EntityInstantiators;
import org.springframework.data.mapping.model.PersistentEntityParameterValueProvider;
import org.springframework.data.mapping.model.PropertyValueProvider;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link io.vanslog.spring.data.meilisearch.core.convert.MeilisearchConverter} Implementation based on
 * {@link org.springframework.data.mapping.context.MappingContext}.
 *
 * @author Junghoon Ban
 */
public class MappingMeilisearchConverter implements MeilisearchConverter, ApplicationContextAware, InitializingBean {

	private final MappingContext<? extends MeilisearchPersistentEntity<?>, MeilisearchPersistentProperty> mappingContext;
	private final GenericConversionService conversionService;
	private final EntityInstantiators instantiators = new EntityInstantiators();
	private CustomConversions conversions = new MeilisearchCustomConversions(Collections.emptyList());

	@SuppressWarnings("unused, FieldCanBeLocal")
	@Nullable private ApplicationContext applicationContext;

	/**
	 * Creates a new {@link io.vanslog.spring.data.meilisearch.core.convert.MappingMeilisearchConverter} given the
	 * {@link org.springframework.data.mapping.context.MappingContext}.
	 * 
	 * @param mappingContext must not be {@literal null}.
	 */
	public MappingMeilisearchConverter(
			MappingContext<? extends MeilisearchPersistentEntity<?>, MeilisearchPersistentProperty> mappingContext) {
		Assert.notNull(mappingContext, "MappingContext must not be null!");
		this.mappingContext = mappingContext;
		this.conversionService = new DefaultConversionService();
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public MappingContext<? extends MeilisearchPersistentEntity<?>, MeilisearchPersistentProperty> getMappingContext() {
		return mappingContext;
	}

	@Override
	public ConversionService getConversionService() {
		return this.conversionService;
	}

	@Override
	public <R> R read(Class<R> type, MeilisearchDocument source) {

		Assert.notNull(type, "Type must not be null");
		Assert.notNull(source, "Source document must not be null");

		if (conversions.hasCustomReadTarget(MeilisearchDocument.class, type)) {
			R converted = conversionService.convert(source, type);
			if (converted != null) {
				return converted;
			}
		}

		MeilisearchPersistentEntity<R> entity = getPersistentEntity(type);
		R instance = instantiators.getInstantiatorFor(entity).createInstance(entity,
				new PersistentEntityParameterValueProvider<>(entity, propertyValueProvider(source), null));
		PersistentPropertyAccessor<R> accessor = entity.getPropertyAccessor(instance);

		entity.doWithProperties((PropertyHandler<MeilisearchPersistentProperty>) property -> {
			if (entity.isCreatorArgument(property)) {
				return;
			}
			String fieldName = property.getFieldName();
			if (!source.containsKey(fieldName)) {
				return;
			}
			Class<?> componentType = property.isCollectionLike() ? property.getComponentType() : property.getActualType();
			accessor.setProperty(property, readValue(source.get(fieldName), property.getType(), componentType));
		});

		return instance;
	}

	@SuppressWarnings("unchecked")
	private <R> MeilisearchPersistentEntity<R> getPersistentEntity(Class<R> type) {
		return (MeilisearchPersistentEntity<R>) mappingContext.getRequiredPersistentEntity(type);
	}

	private PropertyValueProvider<MeilisearchPersistentProperty> propertyValueProvider(MeilisearchDocument source) {

		return new PropertyValueProvider<MeilisearchPersistentProperty>() {
			@Override
			@SuppressWarnings("unchecked")
			public <T> T getPropertyValue(MeilisearchPersistentProperty property) {
				String fieldName = property.getFieldName();
				Class<?> componentType = property.isCollectionLike() ? property.getComponentType()
						: property.getActualType();
				return (T) readValue(source.get(fieldName), property.getType(), componentType);
			}
		};
	}

	@Override
	public void write(Object source, MeilisearchDocument sink) {

		Assert.notNull(source, "Source object must not be null");
		Assert.notNull(sink, "Sink document must not be null");

		MeilisearchPersistentEntity<?> entity = mappingContext.getRequiredPersistentEntity(source.getClass());
		PersistentPropertyAccessor<?> accessor = entity.getPropertyAccessor(source);

		entity.doWithProperties((PropertyHandler<MeilisearchPersistentProperty>) property -> {
			Object value = accessor.getProperty(property);
			if (value != null) {
				sink.put(property.getFieldName(), writeValue(value));
			}
		});
	}

	@Nullable
	private Object writeValue(@Nullable Object value) {

		if (value == null) {
			return null;
		}

		if (conversions.hasCustomWriteTarget(value.getClass(), MeilisearchDocument.class)) {
			Object converted = conversionService.convert(value, MeilisearchDocument.class);
			if (converted != null) {
				return converted;
			}
		}

		if (isSimpleType(value.getClass())) {
			return value;
		}

		if (value instanceof Collection<?>) {
			Collection<?> collection = (Collection<?>) value;
			Collection<Object> converted = new ArrayList<>(collection.size());
			for (Object element : collection) {
				converted.add(writeValue(element));
			}
			return converted;
		}

		if (value.getClass().isArray()) {
			int length = Array.getLength(value);
			Collection<Object> converted = new ArrayList<>(length);
			for (int i = 0; i < length; i++) {
				converted.add(writeValue(Array.get(value, i)));
			}
			return converted;
		}

		if (value instanceof Map<?, ?>) {
			Map<?, ?> map = (Map<?, ?>) value;
			Map<String, Object> converted = new LinkedHashMap<>(map.size());
			map.forEach((key, mapValue) -> converted.put(String.valueOf(key), writeValue(mapValue)));
			return converted;
		}

		MeilisearchDocument document = MeilisearchDocument.create();
		write(value, document);
		return document;
	}

	@Nullable
	private Object readValue(@Nullable Object value, Class<?> targetType, @Nullable Class<?> componentType) {

		if (value == null) {
			return null;
		}

		if (conversions.hasCustomReadTarget(value.getClass(), targetType)) {
			Object converted = conversionService.convert(value, targetType);
			if (converted != null) {
				return converted;
			}
		}

		if (Collection.class.isAssignableFrom(targetType) && value instanceof Collection<?>) {
			Collection<?> collection = (Collection<?>) value;
			Collection<Object> converted = new ArrayList<>(collection.size());
			Class<?> elementType = componentType != null ? componentType : Object.class;
			for (Object element : collection) {
				converted.add(readCollectionElement(element, elementType));
			}
			return converted;
		}

		if (ClassUtils.isAssignableValue(targetType, value)) {
			return value;
		}

		if (conversionService.canConvert(value.getClass(), targetType)) {
			Object converted = conversionService.convert(value, targetType);
			if (converted != null) {
				return converted;
			}
		}

		if (value instanceof MeilisearchDocument) {
			MeilisearchDocument document = (MeilisearchDocument) value;
			return read(targetType, document);
		}

		if (value instanceof Map<?, ?>) {
			Map<?, ?> map = (Map<?, ?>) value;
			return read(targetType, toDocument(map));
		}

		return value;
	}

	@Nullable
	private Object readCollectionElement(@Nullable Object element, Class<?> componentType) {

		if (element == null || ClassUtils.isAssignableValue(componentType, element)) {
			return element;
		}

		if (conversions.hasCustomReadTarget(element.getClass(), componentType)) {
			Object converted = conversionService.convert(element, componentType);
			if (converted != null) {
				return converted;
			}
		}

		if (element instanceof MeilisearchDocument) {
			MeilisearchDocument document = (MeilisearchDocument) element;
			return read(componentType, document);
		}

		if (element instanceof Map<?, ?>) {
			Map<?, ?> map = (Map<?, ?>) element;
			return read(componentType, toDocument(map));
		}

		return element;
	}

	private boolean isSimpleType(Class<?> type) {
		return conversions.getSimpleTypeHolder().isSimpleType(type);
	}

	private void updateMappingContextSimpleTypeHolder() {

		if (mappingContext instanceof AbstractMappingContext<?, ?>) {
			setSimpleTypeHolder(mappingContext, conversions);
		}
	}

	@SuppressWarnings("rawtypes")
	private static void setSimpleTypeHolder(
			MappingContext<? extends MeilisearchPersistentEntity<?>, MeilisearchPersistentProperty> mappingContext,
			CustomConversions conversions) {

		AbstractMappingContext abstractMappingContext = (AbstractMappingContext) mappingContext;
		abstractMappingContext.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
	}

	private static MeilisearchDocument toDocument(Map<?, ?> source) {

		MeilisearchDocument document = MeilisearchDocument.create();
		source.forEach((key, value) -> document.put(String.valueOf(key), value));
		return document;
	}

	public void setConversions(CustomConversions conversions) {

		Assert.notNull(conversions, "CustomConversions must not be null");

		this.conversions = conversions;
		updateMappingContextSimpleTypeHolder();
	}

	@Override
	public void afterPropertiesSet() {
		updateMappingContextSimpleTypeHolder();
		conversions.registerConvertersIn(conversionService);
	}
}
