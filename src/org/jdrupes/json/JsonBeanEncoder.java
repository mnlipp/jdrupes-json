/*
 * This file is part of the JDrupes JSON utilities project.
 * Copyright (C) 2017, 2018  Michael N. Lipp
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

import com.fasterxml.jackson.core.JsonGenerator;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.beans.Transient;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Encoder for converting a Java object graph to JSON. Objects may be arrays,
 * collections, maps and JavaBeans.
 * 
 * Arrays and collections are converted to JSON arrays. The
 * type information is lost. Maps and JavaBeans are converted
 * to JSON objects.
 * 
 * The generated JSON objects can have an additional key/value pair with
 * key "class" and a class name. The class information is generated
 * only if it is needed, i.e. if it cannot be derived from the containing
 * object.
 * 
 * Given the following classes:
 *
 * ```java
 * public static class Person {
 *
 *     private String name;
 *     private int age;
 *     private PhoneNumber[] numbers;
 *
 *     public String getName() {
 *         return name;
 *     }
 *     
 *     public void setName(String name) {
 *         this.name = name;
 *     }
 *     
 *     public int getAge() {
 *         return age;
 *     }
 *     
 *     public void setAge(int age) {
 *         this.age = age;
 *     }
 *     
 *     public PhoneNumber[] getNumbers() {
 *         return numbers;
 *     }
 *     
 *     public void setNumbers(PhoneNumber[] numbers) {
 *         this.numbers = numbers;
 *     }
 * }
 *
 * public static class PhoneNumber {
 *     private String name;
 *     private String number;
 *
 *     public PhoneNumber() {
 *     }
 *     
 *     public String getName() {
 *         return name;
 *     }
 *     
 *     public void setName(String name) {
 *         this.name = name;
 *     }
 *     
 *     public String getNumber() {
 *         return number;
 *     }
 *     
 *     public void setNumber(String number) {
 *         this.number = number;
 *     }
 * }
 *
 * public static class SpecialNumber extends PhoneNumber {
 * }
 * ```
 * 
 * A serialization result may look like this:
 * 
 * ```json
 * {
 *     "age": 42,
 *     "name": "Simon Sample",
 *     "numbers": [
 *         {
 *             "name": "Home",
 *             "number": "06751 51 56 57"
 *         },
 *         {
 *             "class": "test.json.SpecialNumber",
 *             "name": "Work",
 *             "number": "030 77 35 44"
 *         }
 *     ]
 * } 
 * ```
 * 
 * 
 */
public class JsonBeanEncoder extends JsonCodec 
	implements Flushable, Closeable {

	private static final Set<String> EXCLUDED_DEFAULT = new HashSet<>();

	static {
		// See https://issues.apache.org/jira/browse/GROOVY-8284
		EXCLUDED_DEFAULT.add("groovy.lang.MetaClass");
	}

	private Map<Class<?>, String> aliases = new HashMap<>();
	private Set<String> excluded = EXCLUDED_DEFAULT;
	private JsonGenerator gen;
	private StringWriter writer = null;

	@Override
	public JsonBeanEncoder addAlias(Class<?> clazz, String alias) {
		aliases.put(clazz, alias);
		return this;
	}

	/**
	 * Add a type to excude from encoding, usually because it cannot
	 * be converted to JSON. Properties of such types should be
	 * marked as {@link Transient}. However, sometimes base types
	 * don't follow the rules.
	 * 
	 * @param className
	 * @return the encoder for easy chaining
	 */
	public JsonBeanEncoder addExcluded(String className) {
		if (excluded == EXCLUDED_DEFAULT) {
			excluded = new HashSet<>(EXCLUDED_DEFAULT);
		}
		excluded.add(className);
		return this;
	}

	/**
	 * Create a new encoder using a default {@link JsonGenerator}. 
	 * 
	 * @param out the sink
	 * @return the encoder
	 */
	public static JsonBeanEncoder create(Writer out) {
		try {
			return new JsonBeanEncoder(defaultFactory().createGenerator(out));
		} catch (IOException e) {
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Create a new encoder using a default {@link JsonGenerator}
	 * that writes to an internally created {@link StringWriter}. 
	 * The result can be obtained by invoking {@link #toJson()}.
	 * 
	 * @return the encoder
	 */
	public static JsonBeanEncoder create() {
		return new JsonBeanEncoder();
	}

	/**
	 * Create a new encoder using the given {@link JsonGenerator}. 
	 * 
	 * @param generator the generator
	 * @return the encoder
	 */
	public static JsonBeanEncoder create(JsonGenerator generator) {
		return new JsonBeanEncoder(generator);
	}

	private JsonBeanEncoder() {
		writer = new StringWriter();
		try {
			gen = defaultFactory().createGenerator(writer);
		} catch (IOException e) {
			throw new IllegalArgumentException();
		}
	}

	private JsonBeanEncoder(JsonGenerator generator) {
		gen = generator;
	}

	@Override
	public void flush() throws IOException {
		gen.flush();
	}

	@Override
	public void close() throws IOException {
		gen.close();
	}

	/**
	 * Returns the text written to the output. Can only be used
	 * if the encoder has been created with {@link #JsonBeanEncoder()}.
	 * 
	 * @return the result
	 */
	public String toJson() {
		if (writer == null) {
			throw new IllegalStateException(
					"JsonBeanEncoder has been created without a known writer.");
		}
		try {
			gen.flush();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		return writer.toString();
	}
	
	public JsonBeanEncoder writeArray(Object... items) throws IOException {
		doWriteObject(items, items.getClass());
		return this;
	}
	
	public JsonBeanEncoder writeObject(Object obj) throws IOException {
		doWriteObject(obj, obj.getClass());
		return this;
	}
	
	private void doWriteObject(Object obj, Class<?> expectedType)
		throws IOException {
		if (obj == null) {
			gen.writeNull();
			return;
		}
		if (obj instanceof Boolean) {
			gen.writeBoolean((Boolean)obj);
			return;
		} 
		if (obj instanceof Byte) {
			gen.writeNumber(((Byte)obj).intValue());
			return;
		} 
		if (obj instanceof Number) {
			if (obj instanceof Short) {
				gen.writeNumber((Short)obj);
				return;
			}
			if (obj instanceof Integer) {
				gen.writeNumber((Integer)obj);
				return;
			}
			if (obj instanceof Long) {
				gen.writeNumber((Long)obj);
				return;
			}
			if (obj instanceof BigInteger) {
				gen.writeNumber((BigInteger)obj);
				return;
			}
			if (obj instanceof BigDecimal) {
				gen.writeNumber((BigDecimal)obj);
				return;
			}
			gen.writeNumber((Double)obj);
			return;
		}
		PropertyEditor propertyEditor = findPropertyEditor(obj.getClass());
		if (propertyEditor != null) {
			propertyEditor.setValue(obj);
			gen.writeString(propertyEditor.getAsText());
			return;
		}
		if (obj.getClass().isArray()) {
			gen.writeStartArray();
			Class<?> compType = null;
			if (expectedType != null && expectedType.isArray()) {
				compType = expectedType.getComponentType();
			}
			for (int i = 0; i < Array.getLength(obj); i++) {
				doWriteObject(Array.get(obj, i), compType);
			}
			gen.writeEndArray();;
			return;
		}
		if (obj instanceof Collection) {
			gen.writeStartArray();
			for (Object item: (Collection<?>)obj) {
				doWriteObject(item, null);
			}
			gen.writeEndArray();
			return;
		}
		if (obj instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String,Object> map = (Map<String,Object>)obj;
			gen.writeStartObject();
			for (Map.Entry<String, Object> e: map.entrySet()) {
				gen.writeFieldName(e.getKey());
				doWriteObject(e.getValue(), null);
			}
			gen.writeEndObject();
			return;
		}
		BeanInfo beanInfo = findBeanInfo(obj.getClass());
		if (beanInfo != null && beanInfo.getPropertyDescriptors().length > 0) {
			gen.writeStartObject();
			if (!obj.getClass().equals(expectedType)) {
				gen.writeStringField("class", aliases.computeIfAbsent(
						obj.getClass(), k -> k.getName()));
			}
			for (PropertyDescriptor propDesc : beanInfo.getPropertyDescriptors()) {
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
					gen.writeFieldName(propDesc.getName());
					doWriteObject(value, propDesc.getPropertyType());
					continue;
				} catch (IllegalAccessException | IllegalArgumentException 
						| InvocationTargetException e) {
					// Bad luck
				}
			}
			gen.writeEndObject();
			return;
		}
		// Last resort
		gen.writeString(obj.toString());
	}
	
}
