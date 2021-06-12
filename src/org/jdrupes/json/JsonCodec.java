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
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

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

}
