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
import java.beans.Transient;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.stream.JsonGenerator;

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
 * only when it is needed, i.e. if it cannot be derived from the containing
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
public class JsonBeanEncoder extends JsonCoder {

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
		return new JsonBeanEncoder(Json.createGenerator(out));
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
		gen = Json.createGenerator(writer);
	}

	private JsonBeanEncoder(JsonGenerator generator) {
		gen = generator;
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
		return writer.toString();
	}
	
	public JsonBeanEncoder writeArray(Object... items) {
		doWriteObject(items, items.getClass());
		gen.flush();
		return this;
	}
	
	public JsonBeanEncoder writeObject(Object obj) {
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
			if (obj instanceof Short) {
				gen.write((Short)obj);
				return;
			}
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
		PropertyEditor propertyEditor = PropertyEditorManager
		        .findEditor(obj.getClass());
		if (propertyEditor != null) {
			propertyEditor.setValue(obj);
			gen.write(propertyEditor.getAsText());
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
		try {
			BeanInfo beanInfo = Introspector.getBeanInfo(
					obj.getClass(), Object.class);
			if (beanInfo.getPropertyDescriptors().length > 0) {
				gen.writeStartObject();
				if (!obj.getClass().equals(expectedType)) {
					gen.write("class", aliases.computeIfAbsent(
							obj.getClass(), k -> k.getName()));
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
