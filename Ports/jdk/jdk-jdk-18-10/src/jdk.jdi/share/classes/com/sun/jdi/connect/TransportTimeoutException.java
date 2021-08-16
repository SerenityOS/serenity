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

package com.sun.jdi.connect;

import com.sun.jdi.connect.spi.TransportService;

/**
 * This exception may be thrown as a result of a timeout
 * when attaching to a target VM, or waiting to accept a
 * connection from a target VM.
 *
 * <p> When attaching to a target VM, using {@link
 * AttachingConnector#attach attach} this
 * exception may be thrown if the connector supports a timeout
 * {@link Connector.Argument connector argument}. Similiarly,
 * when waiting to accept a connection from a target VM,
 * using {@link ListeningConnector#accept accept} this
 * exception may be thrown if the connector supports a
 * timeout connector argument when accepting.
 *
 * <p> In addition, for developers creating {@link TransportService
 * TransportService} implementations this exception is thrown when
 * {@link TransportService#attach attach} times out when establishing a
 * connection to a target VM, or {@link TransportService#accept accept}
 * times out while waiting for a target VM to connect. </p>
 *
 * @see AttachingConnector#attach
 * @see ListeningConnector#accept
 * @see TransportService#attach
 * @see TransportService#accept
 *
 * @since 1.5
 */
public class TransportTimeoutException extends java.io.IOException {

    private static final long serialVersionUID = 4107035242623365074L;

    /**
     * Constructs a {@code TransportTimeoutException} with no detail
     * message.
     */
    public TransportTimeoutException() {
    }

    /**
     * Constructs a {@code TransportTimeoutException} with the
     * specified detail message.
     *
     * @param message the detail message pertaining to this exception.
     */
    public TransportTimeoutException(String message) {
        super(message);
    }
}
