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

/**
 * 
 */
@SuppressWarnings("serial")
public class JsonDecodeException extends Exception {

    /**
     * Instantiates a new JSON decode exception.
     */
    public JsonDecodeException() {
        super();
    }

    /**
     * Instantiates a new JSON decode exception.
     *
     * @param message the message
     * @param cause the cause
     * @param enableSuppression the enable suppression
     * @param writableStackTrace the writable stack trace
     */
    public JsonDecodeException(String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    /**
     * Instantiates a new JSON decode exception.
     *
     * @param message the message
     * @param cause the cause
     */
    public JsonDecodeException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Instantiates a new JSON decode exception.
     *
     * @param message the message
     */
    public JsonDecodeException(String message) {
        super(message);
    }

    /**
     * Instantiates a new JSON decode exception.
     *
     * @param cause the cause
     */
    public JsonDecodeException(Throwable cause) {
        super(cause);
    }

}
