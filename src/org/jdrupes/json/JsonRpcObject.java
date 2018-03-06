/*
 * This file is part of the JDrupes JSON utilities project.
 * Copyright (C) 2018 Michael N. Lipp
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

import java.util.List;
import java.util.Optional;

public class JsonRpcObject extends JsonObject implements JsonRpc {

	private static final long serialVersionUID = -5874908112198729940L;

	public JsonRpcObject() {
		put("jsonrpc", "2.0");
	}
	
	public JsonRpcObject(JsonObject object) {
	}
	
	@Override
	public String method() {
		return (String)get("method");
	}

	public JsonRpcObject setMethod(String method) {
		put("method", method);
		return this;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public JsonArray params() {
		Object values = get("params");
		if (values instanceof JsonArray) {
			return (JsonArray) values;
		}
		if (values instanceof List) {
			return JsonArray.fromData((List<Object>)values);
		}
		throw new IllegalStateException(
				"Field \"params\" has wrong type " + values.getClass().getName());
	}

	public JsonRpcObject setParams(JsonArray params) {
		put("params", params);
		return this;
	}

	public JsonRpcObject addParam(Object param) {
		JsonArray params = (JsonArray)computeIfAbsent(
				"params", key -> new JsonArray());
		params.add(param);
		return this;
	}
	
	@Override
	public Optional<Object> id() {
		return Optional.ofNullable(get("id"));
	}
	
}