/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.rmi.server;

import java.io.*;
import java.net.*;

/**
 * An <code>RMIServerSocketFactory</code> instance is used by the RMI runtime
 * in order to obtain server sockets for RMI calls.  A remote object can be
 * associated with an <code>RMIServerSocketFactory</code> when it is
 * created/exported via the constructors or <code>exportObject</code> methods
 * of <code>java.rmi.server.UnicastRemoteObject</code>.
 *
 * <p>An <code>RMIServerSocketFactory</code> instance associated with a remote
 * object is used to obtain the <code>ServerSocket</code> used to accept
 * incoming calls from clients.
 *
 * <p>An <code>RMIServerSocketFactory</code> instance can also be associated
 * with a remote object registry so that clients can use custom socket
 * communication with a remote object registry.
 *
 * <p>An implementation of this interface
 * should implement {@link Object#equals} to return <code>true</code> when
 * passed an instance that represents the same (functionally equivalent)
 * server socket factory, and <code>false</code> otherwise (and it should also
 * implement {@link Object#hashCode} consistently with its
 * <code>Object.equals</code> implementation).
 *
 * @author  Ann Wollrath
 * @author  Peter Jones
 * @since   1.2
 * @see     java.rmi.server.UnicastRemoteObject
 * @see     java.rmi.registry.LocateRegistry
 */
public interface RMIServerSocketFactory {

    /**
     * Create a server socket on the specified port (port 0 indicates
     * an anonymous port).
     * @param  port the port number
     * @return the server socket on the specified port
     * @throws IOException if an I/O error occurs during server socket
     * creation
     * @since 1.2
     */
    public ServerSocket createServerSocket(int port)
        throws IOException;
}
