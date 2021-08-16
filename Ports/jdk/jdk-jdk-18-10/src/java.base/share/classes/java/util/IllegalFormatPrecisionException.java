/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

package java.util;

/**
 * Unchecked exception thrown when the precision is a negative value other than
 * {@code -1}, the conversion does not support a precision, or the value is
 * otherwise unsupported. If the precision is not representable by an
 * {@code int} type, then the value {@code Integer.MIN_VALUE} will be used
 * in the exception.
 *
 * @since 1.5
 */
public class IllegalFormatPrecisionException extends IllegalFormatException {

    @java.io.Serial
    private static final long serialVersionUID = 18711008L;

    private int p;

    /**
     * Constructs an instance of this class with the specified precision.
     *
     * @param  p
     *         The precision
     */
    public IllegalFormatPrecisionException(int p) {
        this.p = p;
    }

    /**
     * Returns the precision. If the precision isn't representable by an
     * {@code int}, then will return {@code Integer.MIN_VALUE}.
     *
     * @return  The precision
     */
    public int getPrecision() {
        return p;
    }

    public String getMessage() {
        return Integer.toString(p);
    }
}
