/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.reflect;

/**
 * Thrown when {@link java.lang.reflect.Executable#getParameters the
 * java.lang.reflect package} attempts to read method parameters from
 * a class file and determines that one or more parameters are
 * malformed.
 *
 * <p>The following is a list of conditions under which this exception
 * can be thrown:
 * <ul>
 * <li> The number of parameters (parameter_count) is wrong for the method
 * <li> A constant pool index is out of bounds.
 * <li> A constant pool index does not refer to a UTF-8 entry
 * <li> A parameter's name is "", or contains an illegal character
 * <li> The flags field contains an illegal flag (something other than
 *     FINAL, SYNTHETIC, or MANDATED)
 * </ul>
 *
 * See {@link java.lang.reflect.Executable#getParameters} for more
 * information.
 *
 * @see java.lang.reflect.Executable#getParameters
 * @since 1.8
 */
public class MalformedParametersException extends RuntimeException {

    /**
     * Version for serialization.
     */
    @java.io.Serial
    private static final long serialVersionUID = 20130919L;

    /**
     * Create a {@code MalformedParametersException} with an empty
     * reason.
     */
    public MalformedParametersException() {}

    /**
     * Create a {@code MalformedParametersException}.
     *
     * @param reason The reason for the exception.
     */
    public MalformedParametersException(String reason) {
        super(reason);
    }
}
