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
import java.sql.Date;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
import org.jdrupes.json.JsonArray.DefaultJsonArray;
import org.jdrupes.json.JsonObject.DefaultJsonObject;

// TODO: Auto-generated Javadoc
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
 *    key and assign the value to that field. Note that this  will fail
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
@SuppressWarnings({ "PMD.CouplingBetweenObjects", "PMD.CyclomaticComplexity",
    "PMD.TooManyMethods", "PMD.AvoidDuplicateLiterals",
    "PMD.DataflowAnomalyAnalysis" })
public class JsonBeanDecoder extends JsonCodec {

    private static final Object END_VALUE = new Object();
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private final Map<String, Class<?>> aliases = new HashMap<>();
    private boolean skipUnknown;
    private Function<String, Optional<Class<
            ?>>> classConverter = name -> {
                try {
                    return Optional.ofNullable(Class.forName(name));
                } catch (ClassNotFoundException e) {
                    return Optional.empty();
                }
            };
    private final JsonParser parser;
    private final Map<String, OpenType<?>> openTypes;

    /*
     * (non-Javadoc)
     * 
     * @see org.jdrupes.json.JsonCodec#addAlias(java.lang.Class,
     * java.lang.String)
     */
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
    @SuppressWarnings("PMD.LinguisticNaming")
    public JsonBeanDecoder setClassConverter(
            Function<String, Optional<Class<?>>> converter) {
        this.classConverter = converter;
        return this;
    }

    /**
     * Cause this decoder to silently skip information from the JSON source
     * that cannot be mapped to a property of the bean being created.
     * This is useful if e.g. a REST invocation returns data that you
     * are not interested in and therefore don't want to model in your 
     * JavaBean. 
     * 
     * @return the decoder for chaining
     */
    public JsonBeanDecoder skipUnknown() {
        skipUnknown = true;
        return this;
    }

    /**
     * Create a new decoder using a default {@link JsonParser}. 
     * 
     * @param input the source
     * @return the decoder
     */
    public static JsonBeanDecoder create(Reader input) {
        try {
            return new JsonBeanDecoder(defaultFactory().createParser(input));
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

    /**
     * Create a new decoder using the given parser. 
     *
     * @param parser the parser
     */
    public JsonBeanDecoder(JsonParser parser) {
        if (parser == null) {
            throw new IllegalArgumentException("Parser may not be null.");
        }
        this.parser = parser;
        openTypes = new HashMap<>(simpleOpenTypesMap());
    }

    /**
     * Read a JSON object description into a new {@link JsonObject}.
     *
     * @return the object
     * @throws JsonDecodeException the json decode exception
     */
    public JsonObject readObject() throws JsonDecodeException {
        try {
            return readValue(DefaultJsonObject.class, null);
        } catch (IOException e) {
            throw new JsonDecodeException(e);
        }
    }

    /**
     * Read a JSON object description into a new object of the
     * expected type. The result may have a type derived from
     * the expected type if the JSON read has a `class` key.
     *
     * @param <T> the generic type
     * @param expected the expected type
     * @return the result
     * @throws JsonDecodeException the json decode exception
     */
    public <T> T readObject(Class<T> expected) throws JsonDecodeException {
        try {
            return readValue(expected, null);
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
     * @throws JsonDecodeException the json decode exception
     */
    public <T> T readArray(Class<T> expected) throws JsonDecodeException {
        try {
            return readValue(expected, null);
        } catch (IOException e) {
            throw new JsonDecodeException(e);
        }
    }

    @SuppressWarnings({ "unchecked", "PMD.CognitiveComplexity",
        "PMD.NcssCount" })
    private <T> T readValue(Class<T> expected, OpenType<? extends T> openType)
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
            return (T) maybeParse(expected, parser.getText());
        case FIELD_NAME:
            return (T) parser.getText();
        case START_ARRAY:
            if (openType instanceof ArrayType) {
                return (T) readArrayValues((ArrayType<?>) openType);
            }
            if (expected.isArray()
                || Collection.class.isAssignableFrom(expected)
                || expected.equals(Object.class)) {
                return (T) readArrayValues(expected);
            }
            throw new JsonDecodeException(parser.getCurrentLocation()
                + ": Encountered unexpected array.");
        case START_OBJECT:
            return readObjectValue(expected);
        default:
            if (token.isScalarValue()) {
                if (openType instanceof SimpleType) {
                    return readNumber(simpleToJavaType(openType));
                }
                return readNumber(expected);
            }
            throw new JsonDecodeException(parser.getCurrentLocation()
                + ": Unexpected event.");
        }
    }

    private <T> T maybeParse(Class<T> expected, String text) {
        if (expected.equals(Object.class)
            || expected.isAssignableFrom(String.class)) {
            @SuppressWarnings("unchecked")
            T result = (T) text;
            return result;
        }
        if (Enum.class.isAssignableFrom(expected)) {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            Class<Enum> enumClass = (Class<Enum>) expected;
            @SuppressWarnings("unchecked")
            T result = (T) Enum.valueOf(enumClass, text);
            return result;
        }
        if ((expected.isAssignableFrom(Character.class)
            || expected.equals(Character.TYPE))
            && ((String) text).length() == 1) {
            @SuppressWarnings("unchecked")
            T result = (T) Character.valueOf(((String) text).charAt(0));
            return result;
        }
        if (expected.isAssignableFrom(Date.class)) {
            TemporalAccessor parsed
                = DateTimeFormatter.ISO_INSTANT.parse((String) text);
            if (parsed != null) {
                @SuppressWarnings("unchecked")
                T result = (T) Date.from(Instant.from(parsed));
                return result;
            }
        }
        if (expected.isAssignableFrom(ObjectName.class)) {
            try {
                @SuppressWarnings("unchecked")
                T result = (T) new ObjectName((String) text);
                return result;
            } catch (MalformedObjectNameException e) {
                throw new IllegalArgumentException(e);
            }
        }
        throw new IllegalArgumentException();
    }

    @SuppressWarnings({ "unchecked", "PMD.NPathComplexity" })
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

    private <T> T readArrayValues(Class<T> arrayType)
            throws JsonDecodeException, IOException {
        Collection<?> items = createCollection(arrayType);
        Class<?> elementType = Object.class;
        if (arrayType.isArray()) {
            elementType = arrayType.getComponentType();
        }
        while (true) {
            @SuppressWarnings("unchecked")
            Object item = readValue((Class<Object>) elementType, null);
            if (item == END_VALUE) {
                break;
            }
            @SuppressWarnings("unchecked")
            Collection<Object> anyItems = (Collection<Object>) items;
            anyItems.add(item);
        }
        if (!arrayType.isArray()) {
            @SuppressWarnings("unchecked")
            T typedItems = (T) items;
            return typedItems;
        }
        @SuppressWarnings("unchecked")
        T result = (T) Array.newInstance(elementType, items.size());
        int index = 0;
        for (Object o : items) {
            Array.set(result, index++, o);
        }
        return result;
    }

    private <T> T readArrayValues(ArrayType<T> arrayOpenType)
            throws JsonDecodeException, IOException {
        Collection<?> items = new ArrayList<>();
        OpenType<?> elementType = arrayOpenType.getElementOpenType();
        while (true) {
            Object item = readValue(Object.class, elementType);
            if (item == END_VALUE) {
                break;
            }
            @SuppressWarnings("unchecked")
            Collection<Object> anyItems = (Collection<Object>) items;
            anyItems.add(item);
        }
        T result = null;
        if (arrayOpenType.isPrimitiveArray()) {
            @SuppressWarnings("unchecked")
            T res = (T) createPrimitiveArray(
                simpleToJavaType(arrayOpenType.getElementOpenType()),
                items.size());
            result = res;
        }
        if (result == null) {
            @SuppressWarnings("unchecked")
            T res = (T) Array.newInstance(
                simpleToJavaType(arrayOpenType.getElementOpenType()),
                items.size());
            result = res;
        }
        int index = 0;
        for (Object o : items) {
            Array.set(result, index++, o);
        }
        return result;
    }

    private <T> Collection<?> createCollection(Class<T> arrayType)
            throws JsonDecodeException {
        if (!Collection.class.isAssignableFrom(arrayType)) {
            return (Collection<?>) JsonArray.create();
        }
        if (arrayType.isInterface()) {
            // This is how things should be: interface type
            if (Set.class.isAssignableFrom(arrayType)) {
                return new HashSet<>();
            }
            return (Collection<?>) JsonArray.create();
        }
        // Implementation type, we'll try our best
        try {
            return (Collection<?>) arrayType.getDeclaredConstructor()
                .newInstance();
        } catch (InstantiationException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            throw new JsonDecodeException(parser.getCurrentLocation()
                + ": Cannot create " + arrayType.getName(), e);
        }
    }

    @SuppressWarnings("PMD.CognitiveComplexity")
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
                OpenType<?> openType;
                String provided = null;
                if (parser.nextToken() == JsonToken.START_OBJECT) {
                    openType = readOpenType();
                } else {
                    provided = parser.getText();
                    openType = openTypes.get(provided);
                }
                if (openType != null) {
                    @SuppressWarnings("unchecked")
                    OpenType<T> narrowed = (OpenType<T>) openType;
                    return readOpenTypeValues(narrowed);
                }
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
            @SuppressWarnings("PMD.ConfusingTernary")
            JsonToken event
                = (prefetched != null) ? prefetched : parser.nextToken();
            prefetched = null; // Consumed.
            switch (event) {
            case END_OBJECT:
                break whileLoop;

            case FIELD_NAME:
                String key = parser.getText();
                Object value = readValue(Object.class, null);
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
        BeanInfo beanInfo = findBeanInfo(beanCls);
        if (beanInfo == null) {
            throw new JsonDecodeException(parser.getCurrentLocation()
                + ": Cannot introspect " + beanCls);
        }
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<String, PropertyDescriptor> beanProps = new HashMap<>();
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
                if (skipUnknown) {
                    continue;
                }
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
                    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
                    Object[] args = new Object[conProps.length];
                    for (int i = 0; i < conProps.length; i++) {
                        args[i] = propsMap.remove(conProps[i]);
                    }
                    return e.getValue().newInstance(args);
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
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<String, Object> map = new HashMap<>();
        whileLoop: while (true) {
            @SuppressWarnings("PMD.ConfusingTernary")
            JsonToken event
                = (prefetched != null) ? prefetched : parser.nextToken();
            prefetched = null; // Consumed.
            switch (event) {
            case END_OBJECT:
                break whileLoop;

            case FIELD_NAME:
                String key = parser.getText();
                PropertyDescriptor property = beanProps.get(key);
                Object value;
                if (property == null) {
                    value = readValue(Object.class, null);
                } else {
                    value = readValue(property.getPropertyType(), null);
                }
                map.put(key, value);
                break;

            default:
                throw new JsonDecodeException(parser.getCurrentLocation()
                    + ": Unexpected Json event " + event);
            }
        }
        return map;
    }

    @SuppressWarnings({ "PMD.AvoidAccessibilityAlteration" })
    private <T> void setProperty(T obj, PropertyDescriptor property,
            Object value) throws JsonDecodeException {
        try {
            Method writeMethod = property.getWriteMethod();
            if (writeMethod != null) {
                writeMethod.invoke(obj, value);
                return;
            }
            Field propField = findField(obj.getClass(), property.getName());
            if (!propField.canAccess(obj)) {
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

    @SuppressWarnings({ "PMD.CognitiveComplexity", "PMD.NcssCount",
        "PMD.NPathComplexity", "PMD.SwitchDensity" })
    private OpenType<?> readOpenType()
            throws IOException, JsonDecodeException {
        if (parser.currentToken() == JsonToken.VALUE_STRING) {
            OpenType<?> result = openTypes.get(parser.getText());
            if (result != null) {
                return result;
            }
            throw new JsonDecodeException(parser.getCurrentLocation()
                + ": reference to unknown type: " + parser.getText() + ".");
        }
        if (parser.currentToken() != JsonToken.START_OBJECT) {
            throw new JsonDecodeException(parser.getCurrentLocation()
                + ": value of type must be definition or valid reference.");
        }
        String type = null;
        String description = null;
        List<CompositeItem> items = null;
        String elementType = null;
        int dimension = 0;
        CompositeType rowType = null;
        String[] indices = null;
        while (true) {
            JsonToken token = parser.nextToken();
            if (token == null) {
                throw new JsonDecodeException(parser.getCurrentLocation()
                    + ": Unexpected end of input.");
            }
            switch (token) {
            case END_OBJECT:
                if (items != null) {
                    return createCompositeDefinition(type, items, description);
                }
                if (rowType != null) {
                    try {
                        return new TabularType(type, description, rowType,
                            indices);
                    } catch (OpenDataException e) {
                        throw new JsonDecodeException(
                            parser.getCurrentLocation()
                                + ": Cannot create OpenType.",
                            e);
                    }
                }
                if (elementType != null) {
                    return createArrayDefintion(type, elementType, dimension);
                }
                OpenType<?> result = openTypes.get(type);
                if (result == null) {
                    throw new JsonDecodeException(parser.getCurrentLocation()
                        + ": reference to unknown type: " + type + ".");
                }
                return result;
            case FIELD_NAME:
                switch (parser.getText()) {
                case "type":
                    if (parser.nextToken() == JsonToken.VALUE_STRING) {
                        type = parser.getText();
                    } else {
                        type = readOpenType().getTypeName();
                    }
                    if (description == null) {
                        description = type;
                    }
                    break;
                case "description":
                    description = parser.nextTextValue();
                    break;
                case "keys":
                    items = readCompositeTypeItems();
                    break;
                case "elementType":
                    elementType = parser.nextTextValue();
                    break;
                case "dimension":
                    dimension = parser.nextIntValue(1);
                    break;
                case "row":
                    rowType = createCompositeType("Row",
                        readCompositeTypeItems(), "Tabular data row");
                    break;
                case "indices":
                    indices = readArray(String[].class);
                    break;
                default:
                    throw new JsonDecodeException(parser.getCurrentLocation()
                        + ": Invalid OpenType description.");
                }
                break;
            default:
                throw new JsonDecodeException(parser.getCurrentLocation()
                    + ": Unexpected event.");
            }
        }
    }

    private OpenType<?> createCompositeDefinition(String type,
            List<CompositeItem> items, String description)
            throws JsonDecodeException {
        OpenType<?> openType = createCompositeType(type, items, description);
        openTypes.put(type, openType);
        return openType;
    }

    private CompositeType createCompositeType(String type,
            List<CompositeItem> items, String description)
            throws JsonDecodeException {
        try {
            String[] itemNames = new String[items.size()];
            String[] itemDescriptions = new String[items.size()];
            OpenType<?>[] itemTypes = new OpenType<?>[items.size()];

            int index = 0;
            for (CompositeItem item : items) {
                itemNames[index] = item.name;
                itemTypes[index] = item.type;
                itemDescriptions[index] = item.description;
                index += 1;
            }
            return new CompositeType(type, description,
                itemNames, itemDescriptions, itemTypes);
        } catch (OpenDataException e) {
            throw new JsonDecodeException(parser.getCurrentLocation()
                + ": Cannot create OpenType.", e);
        }
    }

    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    private OpenType<?> createArrayDefintion(String type, String elementType,
            int dimension) throws JsonDecodeException {
        try {
            ArrayType<?> openType;
            Class<?> wrapper = primitiveNameToWrapper(elementType);
            if (wrapper == null) {
                openType
                    = new ArrayType<>(dimension, openTypes.get(elementType));
            } else {
                // Primitive type
                openType = new ArrayType<>(
                    simpleOpenTypeByName(wrapper.getName()), true);
                if (dimension > 1) {
                    openType = new ArrayType<>(dimension - 1, openType);
                }
            }
            openTypes.put(type, openType);
            return openType;
        } catch (OpenDataException e) {
            throw new JsonDecodeException(parser.getCurrentLocation()
                + ": Cannot create OpenType.", e);
        }
    }

    /**
     * Represents an item from a composite type.
     */
    private static class CompositeItem {
        public String name;
        public String description;
        public OpenType<?> type;
    }

    private List<CompositeItem> readCompositeTypeItems()
            throws IOException, JsonDecodeException {
        JsonToken token = parser.nextToken();
        if (!token.equals(JsonToken.START_OBJECT)) {
            throw new JsonDecodeException(parser.getCurrentLocation()
                + ": Expect start of object.");
        }
        List<CompositeItem> result = new ArrayList<>();
        while (true) {
            token = parser.nextToken();
            if (token == null) {
                throw new JsonDecodeException(parser.getCurrentLocation()
                    + ": Unexpected end of input.");
            }
            switch (token) {
            case END_OBJECT:
                return result;
            case FIELD_NAME:
                @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
                CompositeItem item = new CompositeItem();
                item.name = parser.getText();
                item.description = item.name; // Fallback
                if (parser.nextToken() != JsonToken.START_OBJECT) {
                    throw new JsonDecodeException(parser.getCurrentLocation()
                        + ": Expecting composite type item description.");
                }
                readCompositeTypeItem(item);
                result.add(item);
                break;
            default:
                throw new JsonDecodeException(parser.getCurrentLocation()
                    + ": Unexpected event.");
            }
        }
    }

    private void readCompositeTypeItem(CompositeItem item)
            throws IOException, JsonDecodeException {
        while (true) {
            JsonToken token = parser.nextToken();
            if (token == null) {
                throw new JsonDecodeException(parser.getCurrentLocation()
                    + ": Unexpected end of input.");
            }
            switch (token) {
            case END_OBJECT:
                return;
            case FIELD_NAME:
                switch (parser.getText()) {
                case "type":
                    parser.nextToken();
                    item.type = readOpenType();
                    break;
                case "description":
                    item.description = parser.nextTextValue();
                    break;
                default:
                    throw new JsonDecodeException(parser.getCurrentLocation()
                        + ": unexpected key in composite type item description.");
                }
                break;
            default:
                throw new JsonDecodeException(parser.getCurrentLocation()
                    + ": Unexpected event.");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T readOpenTypeValues(OpenType<T> openType)
            throws JsonDecodeException, IOException {
        if (openType instanceof CompositeType) {
            return (T) readCompositeData((CompositeType) openType);
        }
        if (openType instanceof TabularType) {
            return (T) readTabularData((TabularType) openType);
        }
        return null;
    }

    private CompositeData readCompositeData(CompositeType type)
            throws JsonDecodeException, IOException {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<String, Object> asMap = new HashMap<>();
        while (true) {
            JsonToken event = parser.nextToken();
            switch (event) {
            case END_OBJECT:
                try {
                    return new CompositeDataSupport(type, asMap);
                } catch (OpenDataException e) {
                    throw new JsonDecodeException(parser.getCurrentLocation()
                        + ": Cannot create OpenType.", e);
                }

            case FIELD_NAME:
                String key = parser.getText();
                OpenType<?> itemType = type.getType(key);
                Object value = readValue(Object.class, itemType);
                asMap.put(key, value);
                break;

            default:
                throw new JsonDecodeException(parser.getCurrentLocation()
                    + ": Unexpected Json event " + event);
            }
        }
    }

    @SuppressWarnings("PMD.LiteralsFirstInComparisons")
    private TabularData readTabularData(TabularType openType)
            throws IOException, JsonDecodeException {
        JsonToken event = parser.nextToken();
        if (event != JsonToken.FIELD_NAME || !parser.getText().equals("rows")) {
            throw new JsonDecodeException(parser.getCurrentLocation()
                + ": Unexpected Json event " + event);
        }
        event = parser.nextToken();
        if (event != JsonToken.START_ARRAY) {
            throw new JsonDecodeException(parser.getCurrentLocation()
                + ": Unexpected Json event " + event);
        }
        TabularDataSupport data = new TabularDataSupport(openType);
        while (true) {
            CompositeData row = readTabularDataRow(openType);
            if (row == null) {
                break;
            }
            data.put(row);
        }
        event = parser.nextToken();
        if (event != JsonToken.END_OBJECT) {
            throw new JsonDecodeException(parser.getCurrentLocation()
                + ": Unexpected Json event " + event);
        }
        return data;
    }

    private CompositeData readTabularDataRow(TabularType tabularType)
            throws JsonDecodeException, IOException {
        JsonToken event = parser.nextToken();
        if (event == JsonToken.END_ARRAY) {
            return null;
        }
        if (event != JsonToken.START_ARRAY) {
            throw new JsonDecodeException(parser.getCurrentLocation()
                + ": Unexpected Json event " + event);
        }
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<String, Object> items = new HashMap<>();
        Iterator<String> fields = tabularType.getRowType().keySet().iterator();
        while (fields.hasNext()) {
            String field = fields.next();
            items.put(field, readValue(Object.class,
                tabularType.getRowType().getType(field)));
        }
        event = parser.nextToken();
        if (event != JsonToken.END_ARRAY) {
            throw new JsonDecodeException(parser.getCurrentLocation()
                + ": Unexpected Json event " + event);
        }
        try {
            return new CompositeDataSupport(tabularType.getRowType(), items);
        } catch (OpenDataException e) {
            throw new JsonDecodeException(parser.getCurrentLocation()
                + ": Cannot create OpenType.", e);
        }
    }

}
