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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A view on `Map<String,Object>` that provides some utility methods
 * for accessing the data.
 */
public interface JsonObject {

	/**
	 * Creates a new instabnce of the {@link DefaultJsonObject}.
	 *
	 * @return the json object
	 */
	public static JsonObject create() {
		return new DefaultJsonObject();
	}
	
	/**
	 * Creates a wrapper around an existing `Map<String,Object>`.
	 *
	 * @param backing the backing map
	 * @return the json object
	 */
	public static JsonObject from(Map<String, Object> backing) {
		return new JsonObjectWrapper(backing);
	}
	
	/**
	 * Overloaded to ensure that an existing {@link DefaultJsonObject}
	 * is not wrapped again.
	 * 
	 * @param backing the backing object
	 * @return the argument
	 */
	public static JsonObject from(JsonObject backing) {
		return backing;
	}

	Map<String,Object> backing();

	Set<String> fields();
	
	Object get(String field);
	
	JsonObject setField(String field, Object value);
	
	String asString(String field);
	
	int asInt(String field);
	
	long asLong(String field);
	
	boolean asBoolean(String field);
	
	float asFloat(String field);
	
	double asDouble(String field);

	/**
	 * Instances of this class are used as default representations for JSON
	 * objects.
	 */
	public static class DefaultJsonObject extends HashMap<String, Object>
		implements JsonObject {
		private static final long serialVersionUID = -9115687652764559620L;

		public DefaultJsonObject() {
		}

		@Override
		public Map<String,Object> backing() {
			return this;
		}

		/* (non-Javadoc)
		 * @see org.jdrupes.json.JsonObject#fields()
		 */
		@Override
		public Set<String> fields() {
			return keySet();
		}

		@Override
		public Object get(String field) {
			return super.get(field);
		}

		/* (non-Javadoc)
		 * @see org.jdrupes.json.JsonObject#setField(java.lang.String, java.lang.Object)
		 */
		@Override
		public JsonObject setField(String field, Object value) {
			put(field, value);
			return this;
		}

		@Override
		public String asString(String field) {
			return (String) get(field);
		}

		@Override
		public int asInt(String field) {
			return (Integer) get(field);
		}

		@Override
		public long asLong(String field) {
			return (Long) get(field);
		}

		@Override
		public boolean asBoolean(String field) {
			return (Boolean) get(field);
		}

		@Override
		public float asFloat(String field) {
			return (Float) get(field);
		}

		@Override
		public double asDouble(String field) {
			return (Double) get(field);
		}
	}
	
	public static class JsonObjectWrapper implements JsonObject {

		private Map<String, Object> backing;

		private JsonObjectWrapper(Map<String, Object> backing) {
			this.backing = backing;
		}

		@Override
		public Map<String, Object> backing() {
			return backing;
		}

		/* (non-Javadoc)
		 * @see org.jdrupes.json.JsonObject#fields()
		 */
		@Override
		public Set<String> fields() {
			return backing.keySet();
		}

		@Override
		public Object get(String field) {
			return backing.get(field);
		}

		/* (non-Javadoc)
		 * @see org.jdrupes.json.JsonObject#setField(java.lang.String, java.lang.Object)
		 */
		@Override
		public JsonObject setField(String field, Object value) {
			backing.put(field, value);
			return this;
		}

		@Override
		public String asString(String field) {
			return (String)backing.get(field);
		}

		@Override
		public int asInt(String field) {
			return (Integer)backing.get(field);
		}

		@Override
		public long asLong(String field) {
			return (Long)backing.get(field);
		}

		@Override
		public boolean asBoolean(String field) {
			return (Boolean)backing.get(field);
		}

		@Override
		public float asFloat(String field) {
			return (Float)backing.get(field);
		}

		@Override
		public double asDouble(String field) {
			return (Double)backing.get(field);
		}
	}
}
