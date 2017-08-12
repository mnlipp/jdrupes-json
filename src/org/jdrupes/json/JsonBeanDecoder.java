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
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

/**
 * Decoder for converting JSON to a Java object graph. The decoding
 * is based on the expected type passed to the decode method.
 * 
 * The convertion rules are as follows:
 *  * If the expected type is {@link Object} and the JSON input
 *    is an array, the JSON array is
 *    converted to an {@link ArrayList} with element type {@link Object}.
 *  * If the expected type implements {@link Collection} 
 *    a container of the expected type is created. Its element type
 *    is also {@link Object}. The JSON input must be an array.
 *  * If the expected type is an array type, an array of the expected 
 *    type with the given element type is created. The JSON input must
 *    be an array.
 *  * In all cases above, the element type is passed 
 *    as expected type when decoding the members of the JSON array.
 *  * If the expected type is an {@link Object} and the JSON input
 *    is a JSON object, the input is converted to a {@link HashMap}.
 *  * If the expected type is neither of the above, it is assumed
 *    to be a JavaBean and the JSON input must be a JSON object.
 *    The key/value pairs of the JSON input are interpreted as properties
 *    of the JavaBean and set if the values have successfully been parsed.
 *    The type of the properties are passed as expected types when
 *    parsing the values.
 *      
 *  A JSON object can have a "class" key. It must be the first key
 *  of the object. Its value is used to instantiate the Java object
 *  in which the information of the JSON object is stored. If
 *  provided, the class specified by this key/value pair overrides 
 *  the class passed as expected class. It is checked, however, that the
 *  specified class is assignable to the expected class.
 *  
 *  The value specified is first matched against the aliases that
 *  have been registered with the decoder 
 *  (see {@link #addAlias(Class, String)}). If no match is found,
 *  the converter set with {@link JsonBeanDecoder#setClassConverter(Function)}
 *  is used to convert the name to a class. The function defaults
 *  to {@link Class#forName(String)}. If the converter does not
 *  return a result, a {@link HashMap} is used as container for 
 *  the values provided by the JSON object.
 */
public class JsonBeanDecoder extends JsonCoder {

	private Map<String,Class<?>> aliases = new HashMap<>();
	private Function<String,Optional<Class<?>>> classConverter 
		= name -> {
			try {
				return Optional.ofNullable(Class.forName(name));
			} catch (ClassNotFoundException e) {
				return Optional.empty();
			}
		};
	private JsonParser parser;
	
	@Override
	public JsonBeanDecoder addAlias(Class<?> clazz, String alias) {
		aliases.put(alias, clazz);
		return this;
	}

	/**
	 * Sets the converter that maps a specified "class" to an actual Java
	 * {@link Class}. If it does not return a class, a {@link HashMap} is 
	 * used to store the data of the JSON object. 
	 * 
	 * @param converter the converter to use
	 * @return the conversion result
	 */
	public JsonBeanDecoder setClassConverter(
			Function<String,Optional<Class<?>>> converter) {
		this.classConverter = converter;
		return this;
	}
	
	/**
	 * Create a new decoder using a default {@link JsonParser}. 
	 * 
	 * @param in the source
	 * @return the decoder
	 */
	public static JsonBeanDecoder create(Reader in) {
		return new JsonBeanDecoder(Json.createParser(in));
	}

	/**
	 * Create a new decoder using a default parser to parse the
	 * given string. 
	 * 
	 * @param input the input
	 * @return the decoder
	 */
	public static JsonBeanDecoder create(String input) {
		StringReader reader = new StringReader(input);
		return new JsonBeanDecoder(Json.createParser(reader));
	}

	/**
	 * Create a new decoder using the given parser. 
	 * 
	 * @param parser the parser
	 * @return the decoder
	 */
	public static JsonBeanDecoder create(JsonParser parser) {
		return new JsonBeanDecoder(parser);
	}

	public JsonBeanDecoder(JsonParser parser) {
		this.parser = parser;
	}

	public Map<String,?> readObject() {
		Map <String,?> root = new HashMap<>();
		return root;
	}

	public <T> T readObject(Class<T> expected) throws JsonDecodeException {
		return readValue(expected);
	}
	
	public <T> T readArray(Class<T> expected) throws JsonDecodeException {
		return readValue(expected);
	}

	private static final Object END_VALUE = new Object();
	
	@SuppressWarnings("unchecked")
	private <T> T readValue(Class<T> expected) 
			throws JsonDecodeException {
		switch(parser.next()) {
		case END_ARRAY:
		case END_OBJECT:
			return (T)END_VALUE;
		case VALUE_NULL:
			return null;
		case VALUE_FALSE:
			return (T)Boolean.FALSE;
		case VALUE_TRUE:
			return (T)Boolean.TRUE;
		case VALUE_STRING:
			PropertyEditor propertyEditor 
				= PropertyEditorManager.findEditor(expected);
			if (propertyEditor != null) {
				propertyEditor.setAsText(parser.getString());
				return (T)propertyEditor.getValue();
			}
			if (Enum.class.isAssignableFrom(expected)) {
				@SuppressWarnings("rawtypes")
				Class<Enum> enumClass = (Class<Enum>)expected;
				return (T)Enum.valueOf(enumClass, parser.getString());
			}
			// fall through
		case KEY_NAME:
			return (T)parser.getString();
		case VALUE_NUMBER:
			if (expected.equals(Byte.class) 
					|| expected.equals(Byte.TYPE)) {
				return (T)Byte.valueOf((byte)parser.getInt());
			}
			if (expected.equals(Short.class) 
					|| expected.equals(Short.TYPE)) {
				return (T)Short.valueOf((short)parser.getInt());
			}
			if (expected.equals(Integer.class) 
					|| expected.equals(Integer.TYPE)) {
				return (T)Integer.valueOf(parser.getInt());
			}
			if (expected.equals(BigInteger.class)) {
				return (T)parser.getBigDecimal().toBigInteger();
			}
			if (expected.equals(BigDecimal.class)) {
				return (T)parser.getBigDecimal();
			}
			if (expected.equals(Float.class)
					|| expected.equals(Float.TYPE)) {
				return (T)Float.valueOf(parser.getBigDecimal().floatValue());
			}
			if (expected.equals(Long.class) 
					|| expected.equals(Long.TYPE)
					|| parser.isIntegralNumber()) {
				return (T)Long.valueOf(parser.getLong());
			}
			return (T)Double.valueOf(parser.getBigDecimal().doubleValue());
		case START_ARRAY:
			if (expected.isArray() 
					|| Collection.class.isAssignableFrom(expected)
					|| expected.equals(Object.class)) {
				return (T)readArrayValue(expected);
			}
			throw new JsonDecodeException(parser.getLocation()
					+ ": Encountered unexpected array.");
		case START_OBJECT:
			return readObjectValue(expected);
		default:
			throw new JsonDecodeException(parser.getLocation()
					+ ": Unexpected event.");
		}
	}
	
	@SuppressWarnings("unchecked")
	private <T> Object readArrayValue(Class<T> arrayType) 
			throws JsonDecodeException {
		Collection<T> items;
		if (Collections.class.isAssignableFrom(arrayType)
				&& !arrayType.isInterface()) {
			try {
				items = (Collection<T>)arrayType.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new JsonDecodeException(parser.getLocation()
						+ ": Cannot create " + arrayType.getName(), e);
			}
		} else {
			items = new ArrayList<>();
		}
		Class<?> itemType = Object.class;
		if (arrayType.isArray()) {
			itemType = arrayType.getComponentType();
		}
		while (true) {
			T item = (T)readValue(itemType);
			if (item == END_VALUE) {
				break;
			}
			items.add(item);
		}
		if (!arrayType.isArray()) {
			return items;
		}
		Object result = Array.newInstance(itemType, items.size());
		int index = 0;
		for (Object o: items) {
			Array.set(result, index++, o);
		}
		return result;
	}
	
	private <T> T readObjectValue(Class<T> expected) 
			throws JsonDecodeException {
		Event event = parser.next();
		if (!event.equals(Event.KEY_NAME)) {
			throw new JsonDecodeException(parser.getLocation()
					+ ": Unexpected Json event " + event);
		}
		String key = parser.getString();		
		Class<?> cls = expected;
		if ("class".equals(key)) {
			parser.next();
			String provided = parser.getString();
			if (aliases.containsKey(provided)) {
				cls = aliases.get(provided);
			} else {
				cls = classConverter.apply(provided).orElse(HashMap.class);
			}
		}
		if (!expected.isAssignableFrom(cls)) {
			throw new JsonDecodeException(parser.getLocation()
					+ ": Expected " + expected.getName()
					+ " found " + cls.getName());
		}
		if (cls.equals(Object.class)) {
			cls = HashMap.class;
		}
		T result = null;
		try {
			@SuppressWarnings("unchecked")
			T res = (T)cls.newInstance();
			result = res;
		} catch (InstantiationException
				| IllegalAccessException e) {
			throw new JsonDecodeException(parser.getLocation()
					+ ": Cannot create " + cls.getName(), e);
		}
		if (result instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String,?> map = (Map<String,?>)result;
			@SuppressWarnings("unchecked")
			T res = (T)objectToMap(map, !"class".equals(key));
			return res;
		}
		return objectToBean(result, !"class".equals(key));
	}

	private Map<String,?> objectToMap(Map<String,?> result, boolean inKeyState)
			throws JsonDecodeException {
		whileLoop:
		while (true) {
			Event event = inKeyState ? Event.KEY_NAME : parser.next();
			switch(event) {
			case END_OBJECT:
				break whileLoop;
				
			case KEY_NAME:
				String key = parser.getString();
				inKeyState = false;
				Object value = readValue(Object.class);
				@SuppressWarnings("unchecked")
				Map<String,Object> map = (Map<String,Object>)result;
				map.put(key, value);
				break;
				
			default:
				throw new JsonDecodeException(parser.getLocation()
						+ ": Unexpected Json event " + event);
			}
		}
		return result;
		
	}
	
	private <T> T objectToBean(T result, boolean inKeyState)
			throws JsonDecodeException {
		Map<String,PropertyDescriptor> beanProps = new HashMap<>();
		try {
			BeanInfo beanInfo = Introspector.getBeanInfo(
					result.getClass(), Object.class);
			for (PropertyDescriptor 
					p: beanInfo.getPropertyDescriptors()) {
				beanProps.put(p.getName(), p);
			}
		} catch (IntrospectionException e) {
			throw new JsonDecodeException(parser.getLocation()
					+ ": Cannot introspect " + result.getClass());
		}
		
		whileLoop:
		while (true) {
			Event event = inKeyState ? Event.KEY_NAME : parser.next();
			switch(event) {
			case END_OBJECT:
				break whileLoop;
				
			case KEY_NAME:
				String key = parser.getString();
				inKeyState = false;
				PropertyDescriptor property = beanProps.get(key);
				if (property == null) {
					throw new JsonDecodeException(parser.getLocation()
							+ ": No bean property for key " + key);
				}
				Object value = readValue(property.getPropertyType());
				setProperty(result, property, value);
				break;
				
			default:
				throw new JsonDecodeException(parser.getLocation()
						+ ": Unexpected Json event " + event);
			}
		}
		return result;
		
	}

	private <T> void setProperty(T obj, PropertyDescriptor property,
	        Object value) throws JsonDecodeException {
		try {
			Method writeMethod = property.getWriteMethod();
			if (writeMethod != null) {
				writeMethod.invoke(obj, value);
				return;
			}
			Field propField = findField(obj.getClass(), property.getName());
			if (!propField.isAccessible()) {
				propField.setAccessible(true);
			}
			propField.set(obj, value);
		} catch (IllegalAccessException | IllegalArgumentException
		        | InvocationTargetException | NoSuchFieldException e) {
			throw new JsonDecodeException(parser.getLocation()
					+ ": Cannot write property " + property.getName());
		}
	}

	private Field findField(Class<?> cls, String fieldName) 
			throws NoSuchFieldException {
		if (cls.equals(Object.class)) {
			throw new NoSuchFieldException();
		}
		try {
			return cls.getDeclaredField(fieldName);
		} catch (NoSuchFieldException e) {
			return findField(cls.getSuperclass(), fieldName);
		}
	}
}
