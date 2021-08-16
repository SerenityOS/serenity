/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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

package javax.smartcardio;

/**
 * Exception for errors that occur during communication with the
 * Smart Card stack or the card itself.
 *
 * @since   1.6
 * @author  Andreas Sterbenz
 * @author  JSR 268 Expert Group
 */
public class CardException extends Exception {

    private static final long serialVersionUID = 7787607144922050628L;

    /**
     * Constructs a new CardException with the specified detail message.
     *
     * @param message the detail message
     */
    public CardException(String message) {
        super(message);
    }

    /**
     * Constructs a new CardException with the specified cause and a detail message
     * of {@code (cause==null ? null : cause.toString())}.
     *
     * @param cause the cause of this exception or null
     */
    public CardException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new CardException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of this exception or null
     */
    public CardException(String message, Throwable cause) {
        super(message, cause);
    }
}
