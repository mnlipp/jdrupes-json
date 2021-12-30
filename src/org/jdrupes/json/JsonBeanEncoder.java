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
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.management.ObjectName;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularType;

/**
 * Encoder for converting a Java object graph to JSON. Objects may be JavaBeans
 * and arrays, collections, maps of primitive types, JavaBeans and
 * OpenTypes.
 * 
 * Arrays and collections are converted to JSON arrays. The
 * type information is lost. Maps, JavaBeans and MBeans are converted
 * to JSON objects.
 * 
 * The JSON description of objects can have an additional key/value pair with
 * key "class" and a class name. This class information is generated
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
 * Values of type {@link Date} are converted to ISO8601 compliant strings.
 * 
 * Values of type {@link ObjectName} are converted to their canonical 
 * string representation.
 * 
 * If The value is of a {@link CompositeType} or {@link TabularType}
 * as used for MBeans' attributes, the OpenType description is generated
 * as value for the class key.
 */
@SuppressWarnings({ "PMD.CyclomaticComplexity", "PMD.AvoidDuplicateLiterals",
    "PMD.TooManyMethods", "PMD.DataflowAnomalyAnalysis" })
public final class JsonBeanEncoder extends JsonCodec
        implements Flushable, Closeable {

    private static final Set<String> EXCLUDED_DEFAULT = new HashSet<>();

    static {
        // See https://issues.apache.org/jira/browse/GROOVY-8284
        EXCLUDED_DEFAULT.add("groovy.lang.MetaClass");
    }

    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private final Map<Class<?>, String> aliases = new HashMap<>();
    private Set<String> excluded = EXCLUDED_DEFAULT;
    private boolean omitClass;
    private JsonGenerator gen;
    private StringWriter writer;
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private final Map<String, Boolean> described = new HashMap<>();

    @Override
    public JsonBeanEncoder addAlias(Class<?> clazz, String alias) {
        aliases.put(clazz, alias);
        return this;
    }

    /**
     * Configure the encoder to not generate the `class` information
     * even when needed to properly restore the Object graph.
     * 
     * While this contradicts the initial objective to provide JSON
     * persistence for JavaBeans, this is a valid option if the generated
     * JSON is used for transferring information to an environment where
     * the information provided by `class` isn't useful.    
     * 
     * @return the encoder for easy chaining
     */
    public JsonBeanEncoder omitClass() {
        omitClass = true;
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
            throw new IllegalArgumentException(e);
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
            throw new IllegalArgumentException(e);
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

    /**
     * Write the given objects as JSON array.
     *
     * @param items the items
     * @return the JSON bean encoder
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public JsonBeanEncoder writeArray(Object... items) throws IOException {
        doWriteObject(items, items.getClass());
        return this;
    }

    /**
     * Write the given object as JSON.
     *
     * @param obj the obj
     * @return the json bean encoder
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public JsonBeanEncoder writeObject(Object obj) throws IOException {
        if (obj instanceof CompositeData || obj instanceof TabularData) {
            // Force class description for instances of top level OpenTypes
            doWriteObject(obj, null);
        } else {
            doWriteObject(obj, obj.getClass());
        }
        return this;
    }

    @SuppressWarnings({ "PMD.CognitiveComplexity", "PMD.NcssCount",
        "PMD.NPathComplexity", "PMD.ExcessiveMethodLength",
        "PMD.DataflowAnomalyAnalysis" })
    private void doWriteObject(Object obj, Class<?> expectedType)
            throws IOException {
        if (obj == null) {
            gen.writeNull();
            return;
        }
        if (obj instanceof Boolean) {
            gen.writeBoolean((Boolean) obj);
            return;
        }
        if (obj instanceof Byte) {
            gen.writeNumber(((Byte) obj).intValue());
            return;
        }
        if (obj instanceof Number) {
            if (obj instanceof Short) {
                gen.writeNumber((Short) obj);
                return;
            }
            if (obj instanceof Integer) {
                gen.writeNumber((Integer) obj);
                return;
            }
            if (obj instanceof Long) {
                gen.writeNumber((Long) obj);
                return;
            }
            if (obj instanceof BigInteger) {
                gen.writeNumber((BigInteger) obj);
                return;
            }
            if (obj instanceof BigDecimal) {
                gen.writeNumber((BigDecimal) obj);
                return;
            }
            gen.writeNumber((Double) obj);
            return;
        }
        PropertyEditor propertyEditor = findPropertyEditor(obj.getClass());
        if (propertyEditor != null) {
            propertyEditor.setValue(obj);
            gen.writeString(propertyEditor.getAsText());
            return;
        }
        if (obj instanceof Date) {
            gen.writeString(DateTimeFormatter.ISO_INSTANT.format(
                ((Date) obj).toInstant().truncatedTo(ChronoUnit.SECONDS)));
            return;
        }
        if (obj instanceof ObjectName) {
            gen.writeString(((ObjectName) obj).getCanonicalName());
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
            gen.writeEndArray();
            return;
        }
        if (obj instanceof Collection) {
            gen.writeStartArray();
            for (Object item : (Collection<?>) obj) {
                doWriteObject(item, null);
            }
            gen.writeEndArray();
            return;
        }
        if (obj instanceof CompositeData) {
            writeCompositeData((CompositeData) obj, expectedType);
            return;
        }
        // Must be tested before Map because TabularDataSupport implements Map
        if (obj instanceof TabularData) {
            writeTabularData((TabularData) obj, expectedType);
            return;
        }
        if (obj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;
            gen.writeStartObject();
            for (Map.Entry<String, Object> e : map.entrySet()) {
                gen.writeFieldName(e.getKey());
                doWriteObject(e.getValue(), null);
            }
            gen.writeEndObject();
            return;
        }
        BeanInfo beanInfo = findBeanInfo(obj.getClass());
        if (beanInfo != null && beanInfo.getPropertyDescriptors().length > 0) {
            writeJavaBean(obj, expectedType, beanInfo);
            return;
        }
        // Last resort
        gen.writeString(obj.toString());
    }

    private void writeCompositeData(CompositeData data, Class<?> expectedType)
            throws IOException {
        gen.writeStartObject();
        // We don't (necessarily) have the Java type mapped to this
        // CompositeData available. So we suppress writing if
        // CompositeData is passed as expected type.
        if (!omitClass && (expectedType == null
            || !CompositeData.class.isAssignableFrom(expectedType))) {
            gen.writeFieldName("class");
            writeOpenType(data.getCompositeType());
        }
        for (String name : data.getCompositeType().keySet()) {
            if (data.get(name) == null) {
                continue;
            }
            gen.writeFieldName(name);
            // Suppress class entry, type has been fully described already.
            doWriteObject(data.get(name), data.get(name).getClass());
        }
        gen.writeEndObject();
    }

    private void writeTabularData(TabularData data, Class<?> expectedType)
            throws IOException {
        gen.writeStartObject();
        // We don't (necessarily) have the Java type mapped to this
        // TabularData available. So we suppress writing if
        // CompositeData is passed as expected type.
        if (!omitClass && (expectedType == null
            || !TabularData.class.isAssignableFrom(expectedType))) {
            gen.writeFieldName("class");
            writeOpenType(data.getTabularType());
        }

        gen.writeArrayFieldStart("rows");
        @SuppressWarnings("unchecked")
        Collection<CompositeData> rowValues
            = (Collection<CompositeData>) data.values();
        CompositeType rowType = data.getTabularType().getRowType();
        for (CompositeData value : rowValues) {
            gen.writeStartArray();
            for (String column : rowType.keySet()) {
                // Suppress class entry, type has been fully described already.
                doWriteObject(value.get(column), value.get(column).getClass());
            }
            gen.writeEndArray();
        }
        gen.writeEndArray();
        gen.writeEndObject();
    }

    private boolean writeTypeNameReference(String typeName)
            throws IOException {
        if (described.getOrDefault(typeName, false)) {
            gen.writeString(typeName);
            return true;
        }
        // Will (!) happen.
        described.put(typeName, true);
        return false;
    }

    private void writeOpenType(OpenType<?> openType)
            throws IOException {
        if (openType instanceof CompositeType) {
            writeOpenType((CompositeType) openType);
            return;
        }
        if (openType instanceof TabularType) {
            writeOpenType((TabularType) openType);
            return;
        }
        if (openType instanceof ArrayType) {
            writeOpenType((ArrayType<?>) openType);
            return;
        }
        gen.writeString(openType.getTypeName());
    }

    private void writeOpenType(CompositeType type) throws IOException {
        if (writeTypeNameReference(type.getTypeName())) {
            return;
        }
        gen.writeStartObject();
        gen.writeStringField("type", type.getTypeName());
        if (!type.getDescription().equals(type.getTypeName())) {
            gen.writeStringField("description", type.getDescription());
        }
        gen.writeObjectFieldStart("keys");
        writeCompositeTypeFields(type);
        gen.writeEndObject();
        gen.writeEndObject();
    }

    private void writeOpenType(TabularType type) throws IOException {
        if (writeTypeNameReference(type.getTypeName())) {
            return;
        }
        gen.writeStartObject();
        gen.writeStringField("type", type.getTypeName());
        if (!type.getDescription().equals(type.getTypeName())) {
            gen.writeStringField("description", type.getDescription());
        }
        gen.writeObjectFieldStart("row");
        writeCompositeTypeFields(type.getRowType());
        gen.writeEndObject();
        gen.writeArrayFieldStart("indices");
        for (String index : type.getIndexNames()) {
            gen.writeString(index);
        }
        gen.writeEndArray();
        gen.writeEndObject();
    }

    private void writeOpenType(ArrayType<?> arrayType) throws IOException {
        String elementType = arrayType.getElementOpenType().getTypeName();
        if (arrayType.isPrimitiveArray()) {
            elementType = wrapperNameToPrimitive(elementType).getTypeName();
        }
        StringBuilder type = new StringBuilder(elementType);
        for (int i = 0; i < arrayType.getDimension(); i++) {
            type.append("[]");
        }
        if (writeTypeNameReference(type.toString())) {
            return;
        }
        gen.writeStartObject();
        gen.writeStringField("type", type.toString());
        gen.writeFieldName("elementType");
        if (arrayType.isPrimitiveArray()) {
            gen.writeString(elementType);
        } else {
            writeOpenType(arrayType.getElementOpenType());
        }
        gen.writeNumberField("dimension", arrayType.getDimension());
        gen.writeStringField("description", arrayType.getDescription());
        gen.writeEndObject();
    }

    private void writeCompositeTypeFields(CompositeType type)
            throws IOException {
        for (String key : type.keySet()) {
            gen.writeObjectFieldStart(key);
            gen.writeFieldName("type");
            writeOpenType(type.getType(key));
            if (!type.getDescription(key).equals(key)) {
                gen.writeStringField("description", type.getDescription(key));
            }
            gen.writeEndObject();
        }
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    private void writeJavaBean(Object obj, Class<?> expectedType,
            BeanInfo beanInfo) throws IOException {
        gen.writeStartObject();
        if (!obj.getClass().equals(expectedType) && !omitClass) {
            gen.writeStringField("class", aliases.computeIfAbsent(
                obj.getClass(), k -> k.getName()));
        }
        for (PropertyDescriptor propDesc : beanInfo
            .getPropertyDescriptors()) {
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
    }

}
