/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.nio.sctp;

/**
 * Unchecked exception thrown when an attempt is made to remove an
 * address that is not bound to the channel, or remove an address from a
 * channel that has only one address bound to it.
 *
 * @since 1.7
 */
public class IllegalUnbindException extends IllegalStateException {
    private static final long serialVersionUID = -310540883995532224L;

    /**
     * Constructs an instance of this class.
     */
    public IllegalUnbindException() { }

    /**
     * Constructs an instance of this class with the specified detailed message.
     *
     * @param  msg
     *         The String that contains a detailed message
     */
    public IllegalUnbindException(String msg) {
        super(msg);
    }
}

