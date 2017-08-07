/*
 * This file is part of the JDrupes JSON utilities project.
 * Copyright (C) 2017  Michael N. Lipp
 *
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public 
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jdrupes.json;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.stream.JsonGenerator;

/**
 * 
 */
public class JsonEncoder {

	private static final Set<String> EXCLUDED_DEFAULT = new HashSet<>();

	static {
		EXCLUDED_DEFAULT.add("groovy.lang.MetaClass");
	}

	private Set<String> excluded = EXCLUDED_DEFAULT;
	private JsonGenerator gen;
	private StringWriter writer = null;

	/**
	 * Create a new encoder using a default {@link JsonGenerator}. 
	 * 
	 * @param out the sink
	 * @return the encoder
	 */
	public static JsonEncoder create(Writer out) {
		return new JsonEncoder(Json.createGenerator(out));
	}

	/**
	 * Create a new encoder using a default {@link JsonGenerator}
	 * that writes to an internally created {@link StringWriter}. 
	 * The result can be obtained by invoking {@link #jsonString()}.
	 * 
	 * @return the encoder
	 */
	public static JsonEncoder create() {
		return new JsonEncoder();
	}

	/**
	 * Create a new encoder using the given {@link JsonGenerator}. 
	 * 
	 * @param generator the generator
	 * @return the encoder
	 */
	public static JsonEncoder create(JsonGenerator generator) {
		return new JsonEncoder(generator);
	}

	private JsonEncoder() {
		writer = new StringWriter();
		gen = Json.createGenerator(writer);
	}

	private JsonEncoder(JsonGenerator generator) {
		gen = generator;
	}

	/**
	 * Returns the text written to the output. Can only be used
	 * if the encoder has been created with {@link #JsonEncoder()}.
	 * 
	 * @return the result
	 */
	public String toJson() {
		if (writer == null) {
			throw new IllegalStateException(
					"JsonEncoder has been created without a known writer.");
		}
		return writer.toString();
	}
	
	public JsonEncoder addExcluded(String className) {
		if (excluded == EXCLUDED_DEFAULT) {
			excluded = new HashSet<>(EXCLUDED_DEFAULT);
		}
		excluded.add(className);
		return this;
	}

	public JsonEncoder writeArray(Object... items) {
		doWriteObject(items, items.getClass());
		gen.flush();
		return this;
	}
	
	public JsonEncoder writeObject(Object obj) {
		doWriteObject(obj, obj.getClass());
		gen.flush();
		return this;
	}
	
	private void doWriteObject(Object obj, Class<?> expectedType) {
		if (obj == null) {
			gen.writeNull();
			return;
		}
		if (obj instanceof Boolean) {
			gen.write((Boolean)obj);
			return;
		} 
		if (obj instanceof Byte) {
			gen.write(((Byte)obj).intValue());
			return;
		} 
		if (obj instanceof Number) {
			if (obj instanceof Integer) {
				gen.write((Integer)obj);
				return;
			}
			if (obj instanceof Long) {
				gen.write((Long)obj);
				return;
			}
			if (obj instanceof BigInteger) {
				gen.write((BigInteger)obj);
				return;
			}
			if (obj instanceof BigDecimal) {
				gen.write((BigDecimal)obj);
				return;
			}
			gen.write((Double)obj);
			return;
		}
		if (obj instanceof Object[]) {
			gen.writeStartArray();
			Class<?> compType = null;
			if (expectedType != null && expectedType.isArray()) {
				compType = expectedType.getComponentType();
			}
			for (Object item: (Object[])obj) {
				doWriteObject(item, compType);
			}
			gen.writeEnd();
			return;
		}
		if (obj instanceof Collection) {
			gen.writeStartArray();
			for (Object item: (Collection<?>)obj) {
				doWriteObject(item, null);
			}
			gen.writeEnd();
			return;
		}
		if (obj instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String,Object> map = (Map<String,Object>)obj;
			gen.writeStartObject();
			for (Map.Entry<String, Object> e: map.entrySet()) {
				gen.writeKey(e.getKey());
				doWriteObject(e.getValue(), null);
			}
			gen.writeEnd();
			return;
		}
		PropertyEditor propertyEditor 
			= PropertyEditorManager.findEditor(obj.getClass());
		if (propertyEditor != null) {
			propertyEditor.setValue(obj);
			gen.write(propertyEditor.getAsText());
			return;
		}
		try {
			BeanInfo beanInfo = Introspector.getBeanInfo(
					obj.getClass(), Object.class);
			if (beanInfo.getPropertyDescriptors().length > 0) {
				gen.writeStartObject();
				if (!obj.getClass().equals(expectedType)) {
					gen.write("class", obj.getClass().getName());
				}
				for (PropertyDescriptor propDesc: 
						beanInfo.getPropertyDescriptors()) {
					if (propDesc.getValue("transient") != null) {
						continue;
					}
					if (excluded.contains(propDesc.getPropertyType().getName())) {
						continue;
					}
					Method method = propDesc.getReadMethod();
					if (method == null) {
						continue;
					}
					try {
						Object value = method.invoke(obj);
						gen.writeKey(propDesc.getName());
						doWriteObject(value, propDesc.getPropertyType());
						continue;
					} catch (IllegalAccessException | IllegalArgumentException
					        | InvocationTargetException e) {
						// Bad luck
					}
				}
				gen.writeEnd();
				return;
			}
		} catch (IntrospectionException e) {
			// No luck
		}
		// Last resort
		gen.write(obj.toString());
	}
	
}
