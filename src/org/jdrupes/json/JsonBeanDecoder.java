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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.beans.BeanInfo;
import java.beans.ConstructorProperties;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import org.jdrupes.json.JsonArray.DefaultJsonArray;
import org.jdrupes.json.JsonObject.DefaultJsonObject;

/**
 * Decoder for converting JSON to a Java object graph. The decoding
 * is based on the expected type passed to the decode method.
 * 
 * The convertion rules are as follows:
 *  * If the expected type is {@link Object} and the JSON input
 *    is an array, the JSON array is converted to a {@link DefaultJsonArray},
 *    which is an {@link ArrayList} with element type {@link Object} and
 *    some helpful accessor methods.
 *  * If the expected type implements {@link Collection} 
 *    a container of the expected type is created. Its element type
 *    is again {@link Object}. The JSON input must be an array.
 *  * If the expected type is an array type, an array of the expected 
 *    type with the given element type is created. The JSON input must
 *    be an array.
 *  * In all cases above, the element type is passed 
 *    as expected type when decoding the members of the JSON array.
 *  * If the expected type is an {@link Object} and the JSON input
 *    is a JSON object, the input is converted to a 
 *    {@link DefaultJsonObject}, which is a {@link HashMap
 *    HashMap&lt;String,Object&gt;} with some helpful accessor methods.
 *  * If the expected type is neither of the above, it is assumed
 *    to be a JavaBean and the JSON input must be a JSON object.
 *    The key/value pairs of the JSON input are interpreted as properties
 *    of the JavaBean and set if the values have been parsed successfully.
 *    The type of the properties are passed as expected types when
 *    parsing the values. 
 *    
 *    Constructors with {@link ConstructorProperties}
 *    are used if all required values are available. Else, if no setter is
 *    available for a key/value pair, an attempt
 *    is made to gain access to a private field with the name of the
 *    key and assign the value to that field. Note thatthis  will fail
 *    when using Java 9 modules unless you explicitly grant the decoder 
 *    access to private fields. So defining a constructor with
 *    a {@link ConstructorProperties} annotation and all immutable
 *    properties as parameters is strongly recommended.
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
 *  return a result, a {@link JsonObject} is used as container for 
 *  the values provided by the JSON object.
 */
public class JsonBeanDecoder extends JsonCodec {

    private Map<String, Class<?>> aliases = new HashMap<>();
    private Function<String, Optional<Class<?>>> classConverter
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
            Function<String, Optional<Class<?>>> converter) {
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
        try {
            return new JsonBeanDecoder(defaultFactory().createParser(in));
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Create a new decoder using a default parser to parse the
     * given string. 
     * 
     * @param input the input
     * @return the decoder
     */
    public static JsonBeanDecoder create(String input) {
        try {
            return new JsonBeanDecoder(defaultFactory().createParser(input));
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
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
        if (parser == null) {
            throw new IllegalArgumentException("Parser may not be null.");
        }
        this.parser = parser;
    }

    /**
     * Read a JSON object description into a new {@link JsonObject}.
     * 
     * @return the object
     * @throws JsonDecodeException
     */
    public JsonObject readObject() throws JsonDecodeException {
        try {
            return readValue(DefaultJsonObject.class);
        } catch (IOException e) {
            throw new JsonDecodeException(e);
        }
    }

    /**
     * Read a JSON object description into a new object of the
     * expected type. The result may have a type derived from
     * the expected type if the JSON read has a `class` key.
     * 
     * @param expected the expected type
     * @return the result
     * @throws JsonDecodeException
     */
    public <T> T readObject(Class<T> expected) throws JsonDecodeException {
        try {
            return readValue(expected);
        } catch (IOException e) {
            throw new JsonDecodeException(e);
        }
    }

    /**
     * Read a JSON array description into a new array of the
     * expected type.
     *
     * @param <T> the generic type
     * @param expected the expected type
     * @return the result
     * @throws JsonDecodeException
     */
    public <T> T readArray(Class<T> expected) throws JsonDecodeException {
        try {
            return readValue(expected);
        } catch (IOException e) {
            throw new JsonDecodeException(e);
        }
    }

    private static final Object END_VALUE = new Object();

    @SuppressWarnings("unchecked")
    private <T> T readValue(Class<T> expected)
            throws JsonDecodeException, IOException {
        JsonToken token = parser.nextToken();
        if (token == null) {
            return null;
        }
        switch (token) {
        case END_ARRAY:
        case END_OBJECT:
            return (T) END_VALUE;
        case VALUE_NULL:
            return null;
        case VALUE_FALSE:
            return (T) Boolean.FALSE;
        case VALUE_TRUE:
            return (T) Boolean.TRUE;
        case VALUE_STRING:
            PropertyEditor propertyEditor = findPropertyEditor(expected);
            if (propertyEditor != null) {
                propertyEditor.setAsText(parser.getText());
                return (T) propertyEditor.getValue();
            }
            if (Enum.class.isAssignableFrom(expected)) {
                @SuppressWarnings("rawtypes")
                Class<Enum> enumClass = (Class<Enum>) expected;
                return (T) Enum.valueOf(enumClass, parser.getText());
            }
            // fall through
        case FIELD_NAME:
            return (T) parser.getText();
        case START_ARRAY:
            if (expected.isArray()
                || Collection.class.isAssignableFrom(expected)
                || expected.equals(Object.class)) {
                return (T) readArrayValue(expected);
            }
            throw new JsonDecodeException(parser.getCurrentLocation()
                + ": Encountered unexpected array.");
        case START_OBJECT:
            return readObjectValue(expected);
        default:
            if (token.isScalarValue()) {
                return readNumber(expected);
            }
            throw new JsonDecodeException(parser.getCurrentLocation()
                + ": Unexpected event.");
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T readNumber(Class<T> expected) throws IOException {
        if (expected.equals(Byte.class)
            || expected.equals(Byte.TYPE)) {
            return (T) Byte.valueOf((byte) parser.getValueAsInt());
        }
        if (expected.equals(Short.class)
            || expected.equals(Short.TYPE)) {
            return (T) Short.valueOf((short) parser.getValueAsInt());
        }
        if (expected.equals(Integer.class)
            || expected.equals(Integer.TYPE)) {
            return (T) Integer.valueOf(parser.getValueAsInt());
        }
        if (expected.equals(BigInteger.class)) {
            return (T) parser.getBigIntegerValue();
        }
        if (expected.equals(BigDecimal.class)) {
            return (T) parser.getDecimalValue();
        }
        if (expected.equals(Float.class)
            || expected.equals(Float.TYPE)) {
            return (T) Float.valueOf((float) parser.getValueAsDouble());
        }
        if (expected.equals(Long.class)
            || expected.equals(Long.TYPE)
            || parser.currentToken() == JsonToken.VALUE_NUMBER_INT) {
            return (T) Long.valueOf(parser.getValueAsLong());
        }
        return (T) Double.valueOf(parser.getValueAsDouble());
    }

    @SuppressWarnings("unchecked")
    private <T> Object readArrayValue(Class<T> arrayType)
            throws JsonDecodeException, IOException {
        Collection<T> items = createCollection(arrayType);
        Class<?> itemType = Object.class;
        if (arrayType.isArray()) {
            itemType = arrayType.getComponentType();
        }
        while (true) {
            T item = (T) readValue(itemType);
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
        for (Object o : items) {
            Array.set(result, index++, o);
        }
        return result;
    }

    private <T> Collection<T> createCollection(Class<T> arrayType)
            throws JsonDecodeException {
        if (!Collection.class.isAssignableFrom(arrayType)) {
            @SuppressWarnings("unchecked")
            Collection<T> result = (Collection<T>) JsonArray.create();
            return result;
        }
        if (arrayType.isInterface()) {
            // This is how things should be: interface type
            if (Set.class.isAssignableFrom(arrayType)) {
                return new HashSet<>();
            }
            @SuppressWarnings("unchecked")
            Collection<T> result = (Collection<T>) JsonArray.create();
            return result;
        }
        // Implementation type, we'll try our best
        try {
            @SuppressWarnings("unchecked")
            Collection<T> result = (Collection<T>) arrayType
                .getDeclaredConstructor().newInstance();
            return result;
        } catch (InstantiationException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            throw new JsonDecodeException(parser.getCurrentLocation()
                + ": Cannot create " + arrayType.getName(), e);
        }
    }

    private <T> T readObjectValue(Class<T> expected)
            throws JsonDecodeException, IOException {
        JsonToken prefetched = parser.nextToken();
        if (!prefetched.equals(JsonToken.FIELD_NAME)
            && !prefetched.equals(JsonToken.END_OBJECT)) {
            throw new JsonDecodeException(parser.getCurrentLocation()
                + ": Unexpected Json event " + prefetched);
        }
        Class<?> actualCls = expected;
        if (prefetched.equals(JsonToken.FIELD_NAME)) {
            String key = parser.getText();
            if ("class".equals(key)) {
                prefetched = null; // Now it's consumed
                parser.nextToken();
                String provided = parser.getText();
                if (aliases.containsKey(provided)) {
                    actualCls = aliases.get(provided);
                } else {
                    actualCls = classConverter.apply(provided)
                        .orElse(DefaultJsonObject.class);
                }
            }
        }
        if (!expected.isAssignableFrom(actualCls)) {
            throw new JsonDecodeException(parser.getCurrentLocation()
                + ": Expected " + expected.getName()
                + " found " + actualCls.getName());
        }
        if (actualCls.equals(Object.class)) {
            actualCls = DefaultJsonObject.class;
        }
        if (Map.class.isAssignableFrom(actualCls)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = createMapInstance(
                (Class<Map<String, Object>>) actualCls);
            objectIntoMap(map, prefetched);
            @SuppressWarnings("unchecked")
            T result = (T) map;
            return result;
        }
        @SuppressWarnings("unchecked")
        Class<T> beanCls = (Class<T>) actualCls;
        return objectToBean(beanCls, prefetched);
    }

    private <M extends Map<String, Object>> M createMapInstance(Class<M> mapCls)
            throws JsonDecodeException {
        try {
            return (M) mapCls.getDeclaredConstructor().newInstance();
        } catch (InstantiationException
                | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException
                | SecurityException e) {
            throw new JsonDecodeException(parser.getCurrentLocation()
                + ": Cannot create " + mapCls.getName(), e);
        }
    }

    private void objectIntoMap(Map<String, Object> result, JsonToken prefetched)
            throws JsonDecodeException, IOException {
        whileLoop: while (true) {
            JsonToken event
                = prefetched != null ? prefetched : parser.nextToken();
            prefetched = null; // Consumed.
            switch (event) {
            case END_OBJECT:
                break whileLoop;

            case FIELD_NAME:
                String key = parser.getText();
                Object value = readValue(Object.class);
                result.put(key, value);
                break;

            default:
                throw new JsonDecodeException(parser.getCurrentLocation()
                    + ": Unexpected Json event " + event);
            }
        }
    }

    private <T> T objectToBean(Class<T> beanCls, JsonToken prefetched)
            throws JsonDecodeException, IOException {
        Map<String, PropertyDescriptor> beanProps = new HashMap<>();
        BeanInfo beanInfo = findBeanInfo(beanCls);
        if (beanInfo == null) {
            throw new JsonDecodeException(parser.getCurrentLocation()
                + ": Cannot introspect " + beanCls);
        }
        for (PropertyDescriptor p : beanInfo.getPropertyDescriptors()) {
            beanProps.put(p.getName(), p);
        }

        // Get properties as map first.
        Map<String, Object> propsMap = parseProperties(beanProps, prefetched);

        // Prepare result, using constructor with parameters if available.
        T result = createBean(beanCls, propsMap);

        // Set (remaining) properties.
        for (Map.Entry<String, ?> e : propsMap.entrySet()) {
            PropertyDescriptor property = beanProps.get(e.getKey());
            if (property == null) {
                throw new JsonDecodeException(parser.getCurrentLocation()
                    + ": No bean property for key " + e.getKey());
            }
            setProperty(result, property, e.getValue());
        }
        return result;
    }

    private <T> T createBean(Class<T> beanCls, Map<String, Object> propsMap)
            throws JsonDecodeException {
        try {
            SortedMap<ConstructorProperties, Constructor<T>> cons
                = new TreeMap<>(Comparator.comparingInt(
                    (ConstructorProperties cp) -> cp.value().length)
                    .reversed());
            for (Constructor<?> c : beanCls.getConstructors()) {
                ConstructorProperties[] allCps = c.getAnnotationsByType(
                    ConstructorProperties.class);
                if (allCps.length > 0) {
                    @SuppressWarnings("unchecked")
                    Constructor<T> beanConstructor = (Constructor<T>) c;
                    cons.put(allCps[0], beanConstructor);
                }
            }
            for (Map.Entry<ConstructorProperties, Constructor<T>> e : cons
                .entrySet()) {
                String[] conProps = e.getKey().value();
                if (propsMap.keySet().containsAll(Arrays.asList(conProps))) {
                    Object[] args = new Object[conProps.length];
                    for (int i = 0; i < conProps.length; i++) {
                        args[i] = propsMap.remove(conProps[i]);
                    }
                    T result = e.getValue().newInstance(args);
                    return result;
                }
            }

            return beanCls.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            throw new JsonDecodeException(parser.getCurrentLocation()
                + ": Cannot create " + beanCls.getName(), e);
        }
    }

    private Map<String, Object> parseProperties(
            Map<String, PropertyDescriptor> beanProps, JsonToken prefetched)
            throws JsonDecodeException, IOException {
        Map<String, Object> map = new HashMap<>();
        whileLoop: while (true) {
            JsonToken event
                = prefetched != null ? prefetched : parser.nextToken();
            prefetched = null; // Consumed.
            switch (event) {
            case END_OBJECT:
                break whileLoop;

            case FIELD_NAME:
                String key = parser.getText();
                PropertyDescriptor property = beanProps.get(key);
                if (property == null) {
                    throw new JsonDecodeException(parser.getCurrentLocation()
                        + ": No bean property for key " + key);
                }
                Object value = readValue(property.getPropertyType());
                map.put(key, value);
                break;

            default:
                throw new JsonDecodeException(parser.getCurrentLocation()
                    + ": Unexpected Json event " + event);
            }
        }
        return map;
    }

    @SuppressWarnings("deprecation")
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
            throw new JsonDecodeException(parser.getCurrentLocation()
                + ": Cannot write property " + property.getName(), e);
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
