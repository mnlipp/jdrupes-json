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

import java.util.Optional;

public interface JsonRpc {
	
	/**
	 * The invoked method.
	 * 
	 * @return the method
	 */
	String method();

	/**
	 * The parameters.
	 * 
	 * @return the params
	 */
	JsonArray params();

	/**
	 * An optional request id.
	 * 
	 * @return the id
	 */
	Optional<Object> id();

	public static JsonRpc fromJsonObject(JsonObject object) {
		return new JsonRpcObject(object);
	}
}