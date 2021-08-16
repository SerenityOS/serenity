/*
 * Copyright (c) 1995, 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

/**
 * Thrown to indicate that the {@code clone} method in class
 * {@code Object} has been called to clone an object, but that
 * the object's class does not implement the {@code Cloneable}
 * interface.
 * <p>
 * Applications that override the {@code clone} method can also
 * throw this exception to indicate that an object could not or
 * should not be cloned.
 *
 * @see     java.lang.Cloneable
 * @see     java.lang.Object#clone()
 * @since   1.0
 */

public class CloneNotSupportedException extends Exception {
    @java.io.Serial
    private static final long serialVersionUID = 5195511250079656443L;

    /**
     * Constructs a {@code CloneNotSupportedException} with no
     * detail message.
     */
    public CloneNotSupportedException() {
        super();
    }

    /**
     * Constructs a {@code CloneNotSupportedException} with the
     * specified detail message.
     *
     * @param   s   the detail message.
     */
    public CloneNotSupportedException(String s) {
        super(s);
    }
}
