/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.internal.dcmd;

import java.util.Formatter;

/**
 * Thrown to indicate that a diagnostic command could not be executed
 * successfully.
 */
final class DCmdException extends Exception {
    private static final long serialVersionUID = -3792411099340016465L;

    /**
     * Constructs a new exception with message derived from a format string.
     *
     * @param format format string as described in {@link Formatter} class.
     *
     * @param args arguments referenced by the format specifiers in the format
     *        string.
     *
     */
    public DCmdException(String format, Object... args) {
        super(format(format, args));
    }

    /**
     * Constructs a new exception with message derived from a format string.
     *
     * @param cause exception that stopped the diagnostic command to complete.
     *
     * @param format format string as described in {@link Formatter} class.
     *
     * @param args arguments referenced by the format specifiers in the format
     *        string.
     *
     */
    public DCmdException(Throwable cause, String format, Object... args) {
        super(format(format, args), cause);
    }

    private static String format(String message, Object... args) {
        try (Formatter formatter = new Formatter()) {
            return formatter.format(message, args).toString();
        }
    }
}
