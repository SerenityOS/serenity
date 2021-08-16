/*
 * Copyright (c) 2003, 2008, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.instrument;

/*
 * Copyright 2003 Wily Technology, Inc.
 */

/**
 * Thrown by an implementation of
 * {@link java.lang.instrument.ClassFileTransformer#transform ClassFileTransformer.transform}
 * when its input parameters are invalid.
 * This may occur either because the initial class file bytes were
 * invalid or a previously applied transform corrupted the bytes.
 *
 * @see     java.lang.instrument.ClassFileTransformer#transform
 * @since   1.5
 */
public class IllegalClassFormatException extends Exception {
    private static final long serialVersionUID = -3841736710924794009L;

    /**
     * Constructs an <code>IllegalClassFormatException</code> with no
     * detail message.
     */
    public
    IllegalClassFormatException() {
        super();
    }

    /**
     * Constructs an <code>IllegalClassFormatException</code> with the
     * specified detail message.
     *
     * @param   s   the detail message.
     */
    public
    IllegalClassFormatException(String s) {
        super(s);
    }
}
