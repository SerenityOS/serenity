/*
 * Copyright (c) 2010, 2019, Oracle and/or its affiliates. All rights reserved.
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

/*
 *******************************************************************************
 * Copyright (C) 2009-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package java.util;

/**
 * Thrown by methods in {@link Locale} and {@link Locale.Builder} to
 * indicate that an argument is not a well-formed BCP 47 tag.
 *
 * @see Locale
 * @since 1.7
 */
public class IllformedLocaleException extends RuntimeException {

    @java.io.Serial
    private static final long serialVersionUID = -5245986824925681401L;

    private int _errIdx = -1;

    /**
     * Constructs a new {@code IllformedLocaleException} with no
     * detail message and -1 as the error index.
     */
    public IllformedLocaleException() {
        super();
    }

    /**
     * Constructs a new {@code IllformedLocaleException} with the
     * given message and -1 as the error index.
     *
     * @param message the message
     */
    public IllformedLocaleException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code IllformedLocaleException} with the
     * given message and the error index.  The error index is the approximate
     * offset from the start of the ill-formed value to the point where the
     * parse first detected an error.  A negative error index value indicates
     * either the error index is not applicable or unknown.
     *
     * @param message the message
     * @param errorIndex the index
     */
    public IllformedLocaleException(String message, int errorIndex) {
        super(message + ((errorIndex < 0) ? "" : " [at index " + errorIndex + "]"));
        _errIdx = errorIndex;
    }

    /**
     * Returns the index where the error was found. A negative value indicates
     * either the error index is not applicable or unknown.
     *
     * @return the error index
     */
    public int getErrorIndex() {
        return _errIdx;
    }
}
