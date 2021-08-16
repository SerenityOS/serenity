/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * An <code>RMISocketFactory</code> instance is used by the RMI runtime
 * in order to obtain client and server sockets for RMI calls.  An
 * application may use the <code>setSocketFactory</code> method to
 * request that the RMI runtime use its socket factory instance
 * instead of the default implementation.
 *
 * <p>The default socket factory implementation creates a direct
 * socket connection to the remote host.
 *
 * <p>The default socket factory implementation creates server sockets that
 * are bound to the wildcard address, which accepts requests from all network
 * interfaces.
 *
 * @implNote
 * <p>You can use the {@code RMISocketFactory} class to create a server socket that
 * is bound to a specific address, restricting the origin of requests. For example,
 * the following code implements a socket factory that binds server sockets to an IPv4
 * loopback address. This restricts RMI to processing requests only from the local host.
 *
 * <pre>{@code
 *     class LoopbackSocketFactory extends RMISocketFactory {
 *         public ServerSocket createServerSocket(int port) throws IOException {
 *             return new ServerSocket(port, 5, InetAddress.getByName("127.0.0.1"));
 *         }
 *
 *         public Socket createSocket(String host, int port) throws IOException {
 *             // just call the default client socket factory
 *             return RMISocketFactory.getDefaultSocketFactory()
 *                                    .createSocket(host, port);
 *         }
 *     }
 *
 *     // ...
 *
 *     RMISocketFactory.setSocketFactory(new LoopbackSocketFactory());
 * }</pre>
 *
 * Set the {@systemProperty java.rmi.server.hostname} system property
 * to {@code 127.0.0.1} to ensure that the generated stubs connect to the right
 * network interface.
 *
 * @author  Ann Wollrath
 * @author  Peter Jones
 * @since   1.1
 */
public abstract class RMISocketFactory
        implements RMIClientSocketFactory, RMIServerSocketFactory
{

    /** Client/server socket factory to be used by RMI runtime */
    private static RMISocketFactory factory = null;
    /** default socket factory used by this RMI implementation */
    private static RMISocketFactory defaultSocketFactory;
    /** Handler for socket creation failure */
    private static RMIFailureHandler handler = null;

    /**
     * Constructs an <code>RMISocketFactory</code>.
     * @since 1.1
     */
    public RMISocketFactory() {
        super();
    }

    /**
     * Creates a client socket connected to the specified host and port.
     * @param  host   the host name
     * @param  port   the port number
     * @return a socket connected to the specified host and port.
     * @throws IOException if an I/O error occurs during socket creation
     * @since 1.1
     */
    public abstract Socket createSocket(String host, int port)
        throws IOException;

    /**
     * Create a server socket on the specified port (port 0 indicates
     * an anonymous port).
     * @param  port the port number
     * @return the server socket on the specified port
     * @throws IOException if an I/O error occurs during server socket
     * creation
     * @since 1.1
     */
    public abstract ServerSocket createServerSocket(int port)
        throws IOException;

    /**
     * Set the global socket factory from which RMI gets sockets (if the
     * remote object is not associated with a specific client and/or server
     * socket factory). The RMI socket factory can only be set once. Note: The
     * RMISocketFactory may only be set if the current security manager allows
     * setting a socket factory; if disallowed, a SecurityException will be
     * thrown.
     * @param fac the socket factory
     * @throws IOException if the RMI socket factory is already set
     * @throws  SecurityException  if a security manager exists and its
     *          <code>checkSetFactory</code> method doesn't allow the operation.
     * @see #getSocketFactory
     * @see java.lang.SecurityManager#checkSetFactory()
     * @since 1.1
     */
    public synchronized static void setSocketFactory(RMISocketFactory fac)
        throws IOException
    {
        if (factory != null) {
            throw new SocketException("factory already defined");
        }
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkSetFactory();
        }
        factory = fac;
    }

    /**
     * Returns the socket factory set by the <code>setSocketFactory</code>
     * method. Returns <code>null</code> if no socket factory has been
     * set.
     * @return the socket factory
     * @see #setSocketFactory(RMISocketFactory)
     * @since 1.1
     */
    public synchronized static RMISocketFactory getSocketFactory()
    {
        return factory;
    }

    /**
     * Returns a reference to the default socket factory used
     * by this RMI implementation.  This will be the factory used
     * by the RMI runtime when <code>getSocketFactory</code>
     * returns <code>null</code>.
     * @return the default RMI socket factory
     * @since 1.1
     */
    public synchronized static RMISocketFactory getDefaultSocketFactory() {
        if (defaultSocketFactory == null) {
            defaultSocketFactory =
                new sun.rmi.transport.tcp.TCPDirectSocketFactory();
        }
        return defaultSocketFactory;
    }

    /**
     * Sets the failure handler to be called by the RMI runtime if server
     * socket creation fails.  By default, if no failure handler is installed
     * and server socket creation fails, the RMI runtime does attempt to
     * recreate the server socket.
     *
     * <p>If there is a security manager, this method first calls
     * the security manager's <code>checkSetFactory</code> method
     * to ensure the operation is allowed.
     * This could result in a <code>SecurityException</code>.
     *
     * @param fh the failure handler
     * @throws  SecurityException  if a security manager exists and its
     *          <code>checkSetFactory</code> method doesn't allow the
     *          operation.
     * @see #getFailureHandler
     * @see java.rmi.server.RMIFailureHandler#failure(Exception)
     * @since 1.1
     */
    public synchronized static void setFailureHandler(RMIFailureHandler fh)
    {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkSetFactory();
        }
        handler = fh;
    }

    /**
     * Returns the handler for socket creation failure set by the
     * <code>setFailureHandler</code> method.
     * @return the failure handler
     * @see #setFailureHandler(RMIFailureHandler)
     * @since 1.1
     */
    public synchronized static RMIFailureHandler getFailureHandler()
    {
        return handler;
    }
}
