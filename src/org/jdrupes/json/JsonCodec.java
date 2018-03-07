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

/**
 * The base class for the {@link JsonBeanEncoder} and {@link JsonBeanDecoder}.
 */
public abstract class JsonCodec {

	private static JsonFactory defaultFactory = new JsonFactory();
	
	protected static JsonFactory defaultFactory() {
		return defaultFactory;
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
