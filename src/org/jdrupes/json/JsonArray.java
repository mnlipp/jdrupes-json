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
 * A "wrapper" class for `List<Object>` that provides some utility methods
 * for accessing the data.
 * 
 * Instances of this class are used as default representations for JSON
 * arrays.
 */
public class JsonArray extends ArrayList<Object> {
	
	private static final long serialVersionUID = 2997178412748739135L;
	
	public static final JsonArray EMPTY_ARRAY 
		= fromData(Collections.emptyList());
	
	public JsonArray() {
	}

	private JsonArray(List<Object>data) {
		super(data != null ? data : Collections.emptyList());
	}

	public static JsonArray fromData(List<Object> data) {
		return new JsonArray(data);
	}
	
	public static JsonArray fromData(JsonArray data) {
		return data;
	}
	
	@SuppressWarnings("unchecked")
	public Stream<JsonArray> arrayStream() {
		return stream().map(
				obj -> new JsonArray((List<Object>)obj));
	}
	
	public JsonArray append(Object value) {
		add(value);
		return this;
	}
	
	public String asString(int index) {
		return (String)get(index);
	}
	
	public int asInt(int index) {
		return ((Number)get(index)).intValue();
	}
	
	public long asLong(int index) {
		return ((Number)get(index)).longValue();
	}
	
	public boolean asBoolean(int index) {
		return (Boolean)get(index);
	}
	
	public float asFloat(int index) {
		return ((Number)get(index)).floatValue();
	}
	
	public double asDouble(int index) {
		return ((Number)get(index)).doubleValue();
	}
	
	@SuppressWarnings("unchecked")
	public JsonArray asArray(int index) {
		Object value = get(index);
		if (value instanceof JsonArray) {
			return (JsonArray) value;
		}
		if (value instanceof List) {
			return fromData((List<Object>) value);			
		}
		throw new IllegalStateException("Not an array.");
	}
	
}