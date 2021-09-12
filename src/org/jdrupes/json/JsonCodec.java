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

import com.fasterxml.jackson.core.JsonFactory;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import javax.management.ObjectName;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

/**
 * The base class for the {@link JsonBeanEncoder} and {@link JsonBeanDecoder}.
 */
public abstract class JsonCodec {

    private static JsonFactory defaultFactory = new JsonFactory();

    protected static JsonFactory defaultFactory() {
        return defaultFactory;
    }

    private static final Map<Class<?>, PropertyEditor> propertyEditorCache
        = Collections.synchronizedMap(new WeakHashMap<>());

    protected static PropertyEditor findPropertyEditor(Class<?> cls) {
        PropertyEditor propertyEditor = propertyEditorCache.get(cls);
        if (propertyEditor == null && !propertyEditorCache.containsKey(cls)) {
            // Never looked for before.
            propertyEditor = PropertyEditorManager.findEditor(cls);
            propertyEditorCache.put(cls, propertyEditor);
        }
        return propertyEditor;
    }

    private static final Map<Class<?>, BeanInfo> beanInfoCache
        = Collections.synchronizedMap(new WeakHashMap<>());

    protected static BeanInfo findBeanInfo(Class<?> cls) {
        BeanInfo beanInfo = beanInfoCache.get(cls);
        if (beanInfo == null && !beanInfoCache.containsKey(cls)) {
            try {
                beanInfo = Introspector.getBeanInfo(cls, Object.class);
            } catch (IntrospectionException e) {
                // Bad luck
            }
            beanInfoCache.put(cls, beanInfo);
        }
        return beanInfo;
    }

    /**
     * The encoder and decoder make use of the information from
     * {@link PropertyEditorManager#findEditor(Class)} and
     * {@link Introspector#getBeanInfo(Class, Class)}. You'd
     * expect these methods to provide some caching to speed
     * up requests for the same infomration, but they don't.
     * 
     * The results are therefore kept in an internal cache.
     * This cache may, however, become outdated of additional
     * classes are loaded into the VM dynamically. This method
     * can be used to clear the caches if this is required. 
     */
    public static void clearCaches() {
        propertyEditorCache.clear();
        beanInfoCache.clear();
    }

    /**
     * Add an alias for the given class. If defined, the alias
     * will be used instead of the class name by the encoder.
     * 
     * @param clazz the class
     * @param alias the alias
     * @return the object for easy chaining
     */
    public abstract JsonCodec addAlias(Class<?> clazz, String alias);

    /**
     * Maps a wrapper class to the primitive type.
     */
    protected static final Map<Class<?>, Type> wrapperToPrimitive
        = new HashMap<>();

    static {
        wrapperToPrimitive.put(Boolean.class, Boolean.TYPE);
        wrapperToPrimitive.put(Byte.class, Byte.TYPE);
        wrapperToPrimitive.put(Character.class, Character.TYPE);
        wrapperToPrimitive.put(Double.class, Double.TYPE);
        wrapperToPrimitive.put(Float.class, Float.TYPE);
        wrapperToPrimitive.put(Integer.class, Integer.TYPE);
        wrapperToPrimitive.put(Long.class, Long.TYPE);
        wrapperToPrimitive.put(Short.class, Short.TYPE);
    }

    protected Type wrapperToPrimitive(Class<?> wrapper) {
        return wrapperToPrimitive.get(wrapper);
    }

    /**
     * Maps the name of a wrapper class to the primitive type.
     */
    protected static final Map<String, Type> wrapperNameToPrimitive
        = new HashMap<>();

    static {
        for (Map.Entry<Class<?>, Type> w2p : wrapperToPrimitive.entrySet()) {
            wrapperNameToPrimitive.put(w2p.getKey().getName(), w2p.getValue());
        }
    }

    /**
     * Maps the name of a primitive type to the wrapper type.
     */
    private static final Map<String, Class<?>> primitiveNameToWrapper
        = new HashMap<>();

    static {
        for (Map.Entry<Class<?>, Type> w2p : wrapperToPrimitive.entrySet()) {
            primitiveNameToWrapper.put(w2p.getValue().getTypeName(),
                w2p.getKey());
        }
    }

    protected Class<?> primitiveNameToWrapper(String name) {
        return primitiveNameToWrapper.get(name);
    }

    protected Object createPrimitiveArray(Class<?> type, int size) {
        switch (type.getName()) {
        case "java.lang.Boolean":
            return new boolean[size];
        case "java.lang.Byte":
            return new byte[size];
        case "java.lang.Character":
            return new char[size];
        case "java.lang.Double":
            return new double[size];
        case "java.lang.Float":
            return new float[size];
        case "java.lang.Integer":
            return new int[size];
        case "java.lang.Long":
            return new long[size];
        case "java.lang.Short":
            return new short[size];
        }
        throw new IllegalArgumentException();
    }

    protected static Map<String, SimpleType<?>> simpleOpenTypes
        = new HashMap<>();

    static {
        simpleOpenTypes.put(SimpleType.BIGDECIMAL.getTypeName(),
            SimpleType.BIGDECIMAL);
        simpleOpenTypes.put(SimpleType.BIGINTEGER.getTypeName(),
            SimpleType.BIGINTEGER);
        simpleOpenTypes.put(SimpleType.BOOLEAN.getTypeName(),
            SimpleType.BOOLEAN);
        simpleOpenTypes.put(SimpleType.BYTE.getTypeName(), SimpleType.BYTE);
        simpleOpenTypes.put(SimpleType.CHARACTER.getTypeName(),
            SimpleType.CHARACTER);
        simpleOpenTypes.put(SimpleType.DATE.getTypeName(), SimpleType.DATE);
        simpleOpenTypes.put(SimpleType.DOUBLE.getTypeName(), SimpleType.DOUBLE);
        simpleOpenTypes.put(SimpleType.FLOAT.getTypeName(), SimpleType.FLOAT);
        simpleOpenTypes.put(SimpleType.INTEGER.getTypeName(),
            SimpleType.INTEGER);
        simpleOpenTypes.put(SimpleType.LONG.getTypeName(), SimpleType.LONG);
        simpleOpenTypes.put(SimpleType.OBJECTNAME.getTypeName(),
            SimpleType.OBJECTNAME);
        simpleOpenTypes.put(SimpleType.SHORT.getTypeName(), SimpleType.SHORT);
        simpleOpenTypes.put(SimpleType.STRING.getTypeName(), SimpleType.STRING);
        simpleOpenTypes.put(SimpleType.VOID.getTypeName(), SimpleType.VOID);
    }

    private static Map<SimpleType<?>, Class<?>> simpleToJavaType
        = new HashMap<>();

    static {
        simpleToJavaType.put(SimpleType.BIGDECIMAL, BigDecimal.class);
        simpleToJavaType.put(SimpleType.BIGINTEGER, BigInteger.class);
        simpleToJavaType.put(SimpleType.BOOLEAN, Boolean.class);
        simpleToJavaType.put(SimpleType.BYTE, Byte.class);
        simpleToJavaType.put(SimpleType.CHARACTER, Character.class);
        simpleToJavaType.put(SimpleType.DATE, Date.class);
        simpleToJavaType.put(SimpleType.DOUBLE, Double.class);
        simpleToJavaType.put(SimpleType.FLOAT, Float.class);
        simpleToJavaType.put(SimpleType.INTEGER, Integer.class);
        simpleToJavaType.put(SimpleType.LONG, Long.class);
        simpleToJavaType.put(SimpleType.OBJECTNAME, ObjectName.class);
        simpleToJavaType.put(SimpleType.SHORT, Short.class);
        simpleToJavaType.put(SimpleType.STRING, String.class);
        simpleToJavaType.put(SimpleType.VOID, Void.class);
    }

    @SuppressWarnings("unchecked")
    protected <T> Class<T> simpleToJavaType(OpenType<T> openType) {
        return (Class<T>) simpleToJavaType.get(openType);
    }

}
