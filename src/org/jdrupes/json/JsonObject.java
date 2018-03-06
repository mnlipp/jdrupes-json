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

/**
 * A "wrapper" class for `Map<String,Object>` that provides some utility methods
 * for accessing the data.
 * 
 * Instances of this class are used as default representations for JSON
 * objects.
 */
public class JsonObject extends HashMap<String, Object> {

	private static final long serialVersionUID = -9115687652764559620L;

	public JsonObject() {
	}

	private JsonObject(Map<String, Object> data) {
		super(data);
	}

	public static JsonObject fromData(Map<String, Object> data) {
		return new JsonObject(data);
	}
	
	public static JsonObject fromData(JsonObject data) {
		return data;
	}
	
	public String asString(String field) {
		return (String)get(field);
	}
	
	public int asInt(String field) {
		return (Integer)get(field);
	}
	
	public long asLong(String field) {
		return (Long)get(field);
	}
	
	public boolean asBoolean(String field) {
		return (Boolean)get(field);
	}
	
	public float asFloat(String field) {
		return (Float)get(field);
	}
	
	public double asDouble(String field) {
		return (Double)get(field);
	}
}
