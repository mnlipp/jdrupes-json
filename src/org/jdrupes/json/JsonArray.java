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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * A view on a `List<Object>` that provides some utility methods
 * for accessing the data.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public interface JsonArray {

    JsonArray EMPTY_ARRAY = from(Collections.emptyList());

    /**
     * Creates a new instance of the {@link DefaultJsonArray}.
     * 
     * @return the json array
     */
    static JsonArray create() {
        return new DefaultJsonArray();
    }

    /**
     * Creates a wrapper around an existing `List<Object>`.
     *
     * @param backing the backing list
     * @return the json array
     */
    static JsonArray from(List<Object> backing) {
        return new JsonArrayWrapper(backing);
    }

    /**
     * Overloaded to ensure that an existing {@link DefaultJsonArray}
     * is not wrapped again.
     * 
     * @param backing the backing list
     * @return the argument
     */
    static JsonArray from(DefaultJsonArray backing) {
        return backing;
    }

    /**
     * Returns the list of objects that this array is backing.
     * 
     * @return the objects in the array
     */
    List<Object> backing();

    /**
     * Returns the size of the array.
     * 
     * @return the size
     */
    int size();

    /**
     * Streams the elements in the array.
     *
     * @return the stream
     */
    Stream<Object> stream();

    /**
     * Streams the elements in the array after casting them to 
     * {@link JsonArray}s. Useful for processing arrays of arrays.
     *
     * @return the stream
     */
    Stream<JsonArray> arrayStream();

    /**
     * Appends the given value.
     *
     * @param value the value
     * @return the json array
     */
    JsonArray append(Object value);

    /**
     * Returns the value at the given index.
     *
     * @param index the index
     * @return the object
     */
    Object get(int index);

    /**
     * Returns the value at the given index as string.
     *
     * @param index the index
     * @return the string
     */
    String asString(int index);

    /**
     * Returns the value at the given index as int.
     *
     * @param index the index
     * @return the int
     */
    int asInt(int index);

    /**
     * Returns the value at the given index as long.
     *
     * @param index the index
     * @return the long
     */
    long asLong(int index);

    /**
     * Returns the value at the given index as boolean.
     *
     * @param index the index
     * @return true, if successful
     */
    boolean asBoolean(int index);

    /**
     * Returns the value at the given index as float.
     *
     * @param index the index
     * @return the float
     */
    float asFloat(int index);

    /**
     * Returns the value at the given index as double.
     *
     * @param index the index
     * @return the double
     */
    double asDouble(int index);

    /**
     * Returns the value at the given index as a JSON array.
     *
     * @param index the index
     * @return the json array
     */
    JsonArray asArray(int index);

    /**
     * Instances of this class are used as default representations for JSON
     * arrays.
     */
    class DefaultJsonArray extends ArrayList<Object> implements JsonArray {

        private static final long serialVersionUID = 2997178412748739135L;

        @Override
        public List<Object> backing() {
            return this;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.jdrupes.json.JsonArray#stream()
         */
        @Override
        public Stream<Object> stream() {
            return super.stream();
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.jdrupes.json.JsonArrayItf#arrayStream()
         */
        @Override
        @SuppressWarnings("unchecked")
        public Stream<JsonArray> arrayStream() {
            return stream().map(
                obj -> JsonArray.from((List<Object>) obj));
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.jdrupes.json.JsonArrayItf#append(java.lang.Object)
         */
        @Override
        public JsonArray append(Object value) {
            add(value);
            return this;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.jdrupes.json.JsonArrayItf#asString(int)
         */
        @Override
        public String asString(int index) {
            return (String) get(index);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.jdrupes.json.JsonArrayItf#asInt(int)
         */
        @Override
        public int asInt(int index) {
            return ((Number) get(index)).intValue();
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.jdrupes.json.JsonArrayItf#asLong(int)
         */
        @Override
        public long asLong(int index) {
            return ((Number) get(index)).longValue();
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.jdrupes.json.JsonArrayItf#asBoolean(int)
         */
        @Override
        public boolean asBoolean(int index) {
            return (Boolean) get(index);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.jdrupes.json.JsonArrayItf#asFloat(int)
         */
        @Override
        public float asFloat(int index) {
            return ((Number) get(index)).floatValue();
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.jdrupes.json.JsonArrayItf#asDouble(int)
         */
        @Override
        public double asDouble(int index) {
            return ((Number) get(index)).doubleValue();
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.jdrupes.json.JsonArrayItf#asArray(int)
         */
        @Override
        @SuppressWarnings("unchecked")
        public JsonArray asArray(int index) {
            Object value = get(index);
            if (value instanceof JsonArray) {
                return (JsonArray) value;
            }
            if (value instanceof List) {
                return JsonArray.from((List<Object>) value);
            }
            throw new IllegalStateException("Not an array.");
        }

    }

    /**
     * Instances of this class are used as default representations for JSON
     * arrays.
     */
    class JsonArrayWrapper implements JsonArray {

        private final List<Object> backing;

        private JsonArrayWrapper(List<Object> backing) {
            this.backing = backing;
        }

        @Override
        public List<Object> backing() {
            return backing;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.jdrupes.json.JsonArray#size()
         */
        @Override
        public int size() {
            return backing.size();
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.jdrupes.json.JsonArray#stream()
         */
        @Override
        public Stream<Object> stream() {
            return backing.stream();
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.jdrupes.json.JsonArrayItf#arrayStream()
         */
        @Override
        @SuppressWarnings("unchecked")
        public Stream<JsonArray> arrayStream() {
            return backing.stream().map(
                obj -> JsonArray.from((List<Object>) obj));
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.jdrupes.json.JsonArrayItf#append(java.lang.Object)
         */
        @Override
        public JsonArray append(Object value) {
            backing.add(value);
            return this;
        }

        @Override
        public Object get(int index) {
            return backing.get(index);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.jdrupes.json.JsonArrayItf#asString(int)
         */
        @Override
        public String asString(int index) {
            return (String) backing.get(index);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.jdrupes.json.JsonArrayItf#asInt(int)
         */
        @Override
        public int asInt(int index) {
            return ((Number) backing.get(index)).intValue();
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.jdrupes.json.JsonArrayItf#asLong(int)
         */
        @Override
        public long asLong(int index) {
            return ((Number) backing.get(index)).longValue();
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.jdrupes.json.JsonArrayItf#asBoolean(int)
         */
        @Override
        public boolean asBoolean(int index) {
            return (Boolean) backing.get(index);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.jdrupes.json.JsonArrayItf#asFloat(int)
         */
        @Override
        public float asFloat(int index) {
            return ((Number) backing.get(index)).floatValue();
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.jdrupes.json.JsonArrayItf#asDouble(int)
         */
        @Override
        public double asDouble(int index) {
            return ((Number) backing.get(index)).doubleValue();
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.jdrupes.json.JsonArrayItf#asArray(int)
         */
        @Override
        @SuppressWarnings("unchecked")
        public JsonArray asArray(int index) {
            Object value = backing.get(index);
            if (value instanceof JsonArray) {
                return (JsonArray) value;
            }
            if (value instanceof List) {
                return JsonArray.from((List<Object>) value);
            }
            throw new IllegalStateException("Not an array.");
        }

    }
}