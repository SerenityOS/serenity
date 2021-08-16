/*
 * Copyright (c) 1998, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.util.Map;

import com.sun.jdi.VirtualMachine;

/**
 * A connector which listens for a connection initiated by a target VM.
 *
 * @author Gordon Hirsch
 * @since  1.3
 */
public interface ListeningConnector extends Connector {

    /**
     * Indicates whether this listening connector supports multiple
     * connections for a single argument map. If so, a call to
     * {@link #startListening} may allow
     * multiple target VM to become connected.
     *
     * @return {@code true} if multiple connections are supported;
     * {@code false} otherwise.
     */
    boolean supportsMultipleConnections();

    /**
     * Listens for one or more connections initiated by target VMs.
     * The connector uses the given argument map
     * in determining the address at which to listen or else it generates
     * an appropriate listen address. In either case, an address string
     * is returned from this method which can be used in starting target VMs
     * to identify this connector. The format of the address string
     * is connector, transport, and, possibly, platform dependent.
     * <p>
     * The argument map associates argument name strings to instances
     * of {@link Connector.Argument}. The default argument map for a
     * connector can be obtained through {@link Connector#defaultArguments}.
     * Argument map values can be changed, but map entries should not be
     * added or deleted.
     * <p>
     * This method does not return a {@link VirtualMachine}, and, normally,
     * returns before any target VM initiates
     * a connection. The connected target is obtained through
     * {@link #accept} (using the same argument map as is passed to this
     * method).
     * <p>
     * If {@code arguments} contains addressing information and
     * only one connection will be accepted, the {@link #accept accept} method
     * can be called immediately without calling this method.
     *
     * @return the address at which the connector is listening
     * for a connection.
     * @throws java.io.IOException when unable to start listening.
     * Specific exceptions are dependent on the Connector implementation
     * in use.
     * @throws IllegalConnectorArgumentsException when one of the
     * connector arguments is invalid.
     */
    String startListening(Map<String,? extends Connector.Argument> arguments)
        throws IOException, IllegalConnectorArgumentsException;

    /**
     * Cancels listening for connections. The given argument map should match
     * the argument map given for a previous {@link #startListening} invocation.
     *
     * @throws java.io.IOException when unable to stop listening.
     * Specific exceptions are dependent on the Connector implementation
     * in use.
     * @throws IllegalConnectorArgumentsException when one of the
     * connector arguments is invalid.
     */
    void stopListening(Map<String,? extends Connector.Argument> arguments)
        throws IOException, IllegalConnectorArgumentsException;

    /**
     * Waits for a target VM to attach to this connector.
     *
     * @throws TransportTimeoutException when the Connector encapsulates
     * a transport that supports a timeout when accepting, a
     * {@link Connector.Argument} representing a timeout has been set
     * in the argument map, and a timeout occurs whilst waiting for
     * the target VM to connect.
     * @throws java.io.IOException when unable to accept.
     * Specific exceptions are dependent on the Connector implementation
     * in use.
     * @throws IllegalConnectorArgumentsException when one of the
     * connector arguments is invalid.
     */
    VirtualMachine accept(Map<String,? extends Connector.Argument> arguments)
        throws IOException, IllegalConnectorArgumentsException;
}
