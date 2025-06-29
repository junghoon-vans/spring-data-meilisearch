/*
 * Copyright 2023-2025 the original author or authors.
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

import io.vanslog.spring.data.meilisearch.core.document.Document;
import io.vanslog.spring.data.meilisearch.core.mapping.MeilisearchPersistentEntity;
import io.vanslog.spring.data.meilisearch.core.mapping.MeilisearchPersistentProperty;

import java.util.Collections;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.EntityInstantiator;
import org.springframework.data.mapping.model.EntityInstantiators;
import org.springframework.data.mapping.model.PersistentEntityParameterValueProvider;
import org.springframework.data.mapping.model.PropertyValueProvider;
import org.springframework.data.util.TypeInformation;
import org.springframework.format.datetime.DateFormatterRegistrar;
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

	private static final String INVALID_TYPE_TO_READ = "Expected to read Document %s into type %s but didn't find a PersistentEntity for the latter!";

	private final MappingContext<? extends MeilisearchPersistentEntity<?>, MeilisearchPersistentProperty> mappingContext;
	private final GenericConversionService conversionService;
	private CustomConversions conversions = new MeilisearchCustomConversions(Collections.emptyList());
	private final EntityInstantiators instantiators = new EntityInstantiators();

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

	public void setConversions(CustomConversions conversions) {
		Assert.notNull(conversions, "CustomConversions must not be null");
		this.conversions = conversions;
	}

	@Override
	public void afterPropertiesSet() {
		DateFormatterRegistrar.addDateConverters(conversionService);
		conversions.registerConvertersIn(conversionService);
	}

	@Override
	public <R> R read(Class<R> type, Document source) {
		Assert.notNull(source, "Source must not be null!");
		return readInternal(type, source);
	}

	@SuppressWarnings("unchecked")
	private <R> R readInternal(Class<R> type, Document source) {
		TypeInformation<R> typeInfo = TypeInformation.of(type);
		Class<R> rawType = typeInfo.getType();

		if (conversions.hasCustomReadTarget(Document.class, rawType)) {
			return conversionService.convert(source, rawType);
		}

		if (Document.class.isAssignableFrom(rawType)) {
			return (R) source;
		}

		MeilisearchPersistentEntity<?> entity = mappingContext.getPersistentEntity(rawType);
		if (entity == null) {
			throw new MappingException(String.format(INVALID_TYPE_TO_READ, source, rawType));
		}

		return readEntity(entity, source, typeInfo);
	}

	@SuppressWarnings("unchecked")
	private <R> R readEntity(MeilisearchPersistentEntity<?> entity, Document source, TypeInformation<R> typeHint) {
		// Create an instance of the entity
		EntityInstantiator instantiator = instantiators.getInstantiatorFor(entity);
		Object instance = instantiator.createInstance(entity, 
			new PersistentEntityParameterValueProvider<>(entity, new DocumentPropertyValueProvider(source), null));

		// Get a property accessor for the instance
		PersistentPropertyAccessor<Object> accessor = entity.getPropertyAccessor(instance);

		// Set properties from the source document
		entity.doWithProperties((MeilisearchPersistentProperty property) -> {
			if (source.containsKey(property.getName())) {
				Object value = source.get(property.getName());
				if (value != null) {
					accessor.setProperty(property, readPropertyValue(property, value, typeHint.getProperty(property.getName())));
				}
			}
		});

		return (R) accessor.getBean();
	}

	@SuppressWarnings("unchecked")
	private Object readPropertyValue(MeilisearchPersistentProperty property, Object value,
			@Nullable TypeInformation<?> typeHint) {
		if (value == null) {
			return null;
		}

		Class<?> propertyType = property.getType();

		if (conversions.hasCustomReadTarget(value.getClass(), propertyType)) {
			return conversionService.convert(value, propertyType);
		}

		if (propertyType.isAssignableFrom(value.getClass())) {
			return value;
		}

		if (value instanceof Map) {
			Document document = Document.from((Map<String, Object>) value);
			return readInternal(propertyType, document);
		}

		return conversionService.convert(value, propertyType);
	}

	@Override
	public void write(Object source, Document sink) {
		Assert.notNull(source, "Source must not be null!");
		Assert.notNull(sink, "Sink must not be null!");

		Class<?> sourceType = ClassUtils.getUserClass(source.getClass());
		writeInternal(source, sink, sourceType);
	}

	@SuppressWarnings("unchecked")
	private void writeInternal(Object source, Document sink, Class<?> sourceType) {
		if (conversions.hasCustomWriteTarget(sourceType, Document.class)) {
			Document result = conversionService.convert(source, Document.class);
			sink.putAll(result);
			return;
		}

		if (source instanceof Document) {
			sink.putAll((Document) source);
			return;
		}

		if (source instanceof Map) {
			sink.putAll((Map<String, Object>) source);
			return;
		}

		MeilisearchPersistentEntity<?> entity = mappingContext.getPersistentEntity(sourceType);
		if (entity == null) {
			throw new MappingException("No mapping metadata found for " + sourceType);
		}

		writeEntity(entity, source, sink);
	}

	private void writeEntity(MeilisearchPersistentEntity<?> entity, Object source, Document sink) {
		PersistentPropertyAccessor<?> accessor = entity.getPropertyAccessor(source);

		entity.doWithProperties((MeilisearchPersistentProperty property) -> {
			Object value = accessor.getProperty(property);
			if (value != null) {
				if (conversions.hasCustomWriteTarget(value.getClass())) {
					value = conversionService.convert(value, getCustomWriteTarget(value.getClass()));
				}
				sink.put(property.getName(), value);
			}
		});
	}

	@SuppressWarnings("unchecked")
	private Class<?> getCustomWriteTarget(Class<?> sourceType) {
		return conversions.getCustomWriteTarget(sourceType).orElse(Document.class);
	}

	private class DocumentPropertyValueProvider implements PropertyValueProvider<MeilisearchPersistentProperty> {
		private final Document source;

		DocumentPropertyValueProvider(Document source) {
			this.source = source;
		}

		@Override
		public Object getPropertyValue(MeilisearchPersistentProperty property) {
			return source.get(property.getName());
		}
	}
}
