/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package javax.xml.validation;

/**
 * Thrown when a problem with configuration with the Schema Factories
 * exists. This error will typically be thrown when the class of a
 * schema factory specified in the system properties cannot be found
 * or instantiated.
 * @since 1.8
 */
public final class SchemaFactoryConfigurationError extends Error {

    static final long serialVersionUID = 3531438703147750126L;

    /**
     * Create a new <code>SchemaFactoryConfigurationError</code> with no
     * detail message.
     */
    public SchemaFactoryConfigurationError() {
    }


    /**
     * Create a new <code>SchemaFactoryConfigurationError</code> with
     * the <code>String</code> specified as an error message.
     *
     * @param message The error message for the exception.
     */
    public SchemaFactoryConfigurationError(String message) {
        super(message);
    }

    /**
     * Create a new <code>SchemaFactoryConfigurationError</code> with the
     * given <code>Throwable</code> base cause.
     *
     * @param cause The exception or error to be encapsulated in a
     * SchemaFactoryConfigurationError.
     */
    public SchemaFactoryConfigurationError(Throwable cause) {
        super(cause);
    }

    /**
     * Create a new <code>SchemaFactoryConfigurationError</code> with the
     * given <code>Throwable</code> base cause and detail message.
     *
     * @param cause The exception or error to be encapsulated in a
     * SchemaFactoryConfigurationError.
     * @param message The detail message.
     */
    public SchemaFactoryConfigurationError(String message, Throwable cause) {
        super(message, cause);
    }

}
