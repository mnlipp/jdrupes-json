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

import java.util.List;
import java.util.Optional;

public interface JsonRpc extends JsonObject {

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

    public JsonRpc setMethod(String method);

    public JsonRpc setParams(JsonArray params);

    public JsonRpc addParam(Object param);

    /**
     * Creates an instance of {@link JsonRpc}.
     * 
     * @return the result
     */
    public static JsonRpc create() {
        return new DefaultJsonRpc();
    }

    public class DefaultJsonRpc extends DefaultJsonObject implements JsonRpc {

        private static final long serialVersionUID = -5874908112198729940L;

        public DefaultJsonRpc() {
            put("jsonrpc", "2.0");
        }

        @Override
        public String method() {
            return (String) get("method");
        }

        @Override
        public DefaultJsonRpc setMethod(String method) {
            put("method", method);
            return this;
        }

        @SuppressWarnings("unchecked")
        @Override
        public JsonArray params() {
            Object values
                = computeIfAbsent("params", key -> JsonArray.create());
            if (values instanceof JsonArray) {
                return (JsonArray) values;
            }
            if (values instanceof List) {
                return JsonArray.from((List<Object>) values);
            }
            throw new IllegalStateException(
                "Field \"params\" has wrong type "
                    + values.getClass().getName());
        }

        @Override
        public DefaultJsonRpc setParams(JsonArray params) {
            put("params", params);
            return this;
        }

        @Override
        public DefaultJsonRpc addParam(Object param) {
            JsonArray params = (JsonArray) computeIfAbsent(
                "params", key -> JsonArray.create());
            params.append(param);
            return this;
        }

        @Override
        public Optional<Object> id() {
            return Optional.ofNullable(get("id"));
        }

    }
}