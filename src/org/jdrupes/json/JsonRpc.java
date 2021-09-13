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

/**
 * An interface that defines a JSON RPC.
 */
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
    @SuppressWarnings("PMD.ShortMethodName")
    Optional<Object> id();

    /**
     * Sets the method.
     *
     * @param method the method
     * @return the json rpc
     */
    @SuppressWarnings("PMD.LinguisticNaming")
    JsonRpc setMethod(String method);

    /**
     * Sets the params.
     *
     * @param params the params
     * @return the json rpc
     */
    @SuppressWarnings("PMD.LinguisticNaming")
    JsonRpc setParams(JsonArray params);

    /**
     * Adds the param.
     *
     * @param param the param
     * @return the json rpc
     */
    JsonRpc addParam(Object param);

    /**
     * Creates an instance of {@link JsonRpc}.
     * 
     * @return the result
     */
    static JsonRpc create() {
        return new DefaultJsonRpc();
    }

    /**
     * The Class DefaultJsonRpc.
     */
    class DefaultJsonRpc extends DefaultJsonObject implements JsonRpc {

        private static final long serialVersionUID = -5874908112198729940L;

        /**
         * Instantiates a new default json rpc.
         */
        public DefaultJsonRpc() {
            put("jsonrpc", "2.0");
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.jdrupes.json.JsonRpc#method()
         */
        @Override
        public String method() {
            return (String) get("method");
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.jdrupes.json.JsonRpc#setMethod(java.lang.String)
         */
        @Override
        public DefaultJsonRpc setMethod(String method) {
            put("method", method);
            return this;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.jdrupes.json.JsonRpc#params()
         */
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

        /*
         * (non-Javadoc)
         * 
         * @see org.jdrupes.json.JsonRpc#setParams(org.jdrupes.json.JsonArray)
         */
        @Override
        public DefaultJsonRpc setParams(JsonArray params) {
            put("params", params);
            return this;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.jdrupes.json.JsonRpc#addParam(java.lang.Object)
         */
        @Override
        public DefaultJsonRpc addParam(Object param) {
            JsonArray params = (JsonArray) computeIfAbsent(
                "params", key -> JsonArray.create());
            params.append(param);
            return this;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.jdrupes.json.JsonRpc#id()
         */
        @Override
        @SuppressWarnings("PMD.ShortMethodName")
        public Optional<Object> id() {
            return Optional.ofNullable(get("id"));
        }

    }
}