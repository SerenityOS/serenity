/*
 * Copyright (c) 2003, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jdi.connect.spi;

/**
 * This exception may be thrown as a result of an asynchronous
 * close of a {@link Connection} while an I/O operation is
 * in progress.
 *
 * <p> When a thread is blocked in {@link Connection#readPacket
 * readPacket} waiting for packet from a target VM the
 * {@link Connection} may be closed asynchronous by another
 * thread invokving the {@link Connection#close close} method.
 * When this arises the thread in readPacket will throw this
 * exception. Similiarly when a thread is blocked in
 * {@link Connection#writePacket} the Connection may be closed.
 * When this occurs the thread in writePacket will throw
 * this exception.
 *
 * @see Connection#readPacket
 * @see Connection#writePacket
 *
 * @since 1.5
 */
public class ClosedConnectionException extends java.io.IOException {

    private static final long serialVersionUID = 3877032124297204774L;

    /**
     * Constructs a {@code ClosedConnectionException} with no detail
     * message.
     */
    public ClosedConnectionException() {
    }

    /**
     * Constructs a {@code ClosedConnectionException} with the
     * specified detail message.
     *
     * @param message the detail message pertaining to this exception.
     */
    public ClosedConnectionException(String message) {
        super(message);
    }
}
