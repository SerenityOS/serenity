/*
 * Copyright (c) 1997, 2007, Oracle and/or its affiliates. All rights reserved.
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

package javax.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;

/**
 * This class creates server sockets.  It may be subclassed by other
 * factories, which create particular types of server sockets.  This
 * provides a general framework for the addition of public socket-level
 * functionality.  It is the server side analogue of a socket factory,
 * and similarly provides a way to capture a variety of policies related
 * to the sockets being constructed.
 *
 * <P> Like socket factories, server Socket factory instances have
 * methods used to create sockets. There is also an environment
 * specific default server socket factory; frameworks will often use
 * their own customized factory.
 *
 * @since 1.4
 * @see SocketFactory
 *
 * @author David Brownell
 */
public abstract class ServerSocketFactory
{
    //
    // NOTE:  JDK 1.1 bug in class GC, this can get collected
    // even though it's always accessible via getDefault().
    //
    private static ServerSocketFactory          theFactory;


    /**
     * Creates a server socket factory.
     */
    protected ServerSocketFactory() { /* NOTHING */ }

    /**
     * Returns a copy of the environment's default socket factory.
     *
     * @return the <code>ServerSocketFactory</code>
     */
    public static ServerSocketFactory getDefault()
    {
        synchronized (ServerSocketFactory.class) {
            if (theFactory == null) {
                //
                // Different implementations of this method could
                // work rather differently.  For example, driving
                // this from a system property, or using a different
                // implementation than JavaSoft's.
                //
                theFactory = new DefaultServerSocketFactory();
            }
        }

        return theFactory;
    }


    /**
     * Returns an unbound server socket.  The socket is configured with
     * the socket options (such as accept timeout) given to this factory.
     *
     * @return the unbound socket
     * @throws IOException if the socket cannot be created
     * @see java.net.ServerSocket#bind(java.net.SocketAddress)
     * @see java.net.ServerSocket#bind(java.net.SocketAddress, int)
     * @see java.net.ServerSocket#ServerSocket()
     */
    public ServerSocket createServerSocket() throws IOException {
        throw new SocketException("Unbound server sockets not implemented");
    }

    /**
     * Returns a server socket bound to the specified port.
     * The socket is configured with the socket options
     * (such as accept timeout) given to this factory.
     * <P>
     * If there is a security manager, its <code>checkListen</code>
     * method is called with the <code>port</code> argument as its
     * argument to ensure the operation is allowed. This could result
     * in a SecurityException.
     *
     * @param port the port to listen to
     * @return the <code>ServerSocket</code>
     * @throws IOException for networking errors
     * @throws SecurityException if a security manager exists and its
     *         <code>checkListen</code> method doesn't allow the operation.
     * @throws IllegalArgumentException if the port parameter is outside the
     *         specified range of valid port values, which is between 0 and
     *         65535, inclusive.
     * @see    SecurityManager#checkListen
     * @see java.net.ServerSocket#ServerSocket(int)
     */
    public abstract ServerSocket createServerSocket(int port)
        throws IOException;


    /**
     * Returns a server socket bound to the specified port, and uses the
     * specified connection backlog.  The socket is configured with
     * the socket options (such as accept timeout) given to this factory.
     * <P>
     * The <code>backlog</code> argument must be a positive
     * value greater than 0. If the value passed if equal or less
     * than 0, then the default value will be assumed.
     * <P>
     * If there is a security manager, its <code>checkListen</code>
     * method is called with the <code>port</code> argument as its
     * argument to ensure the operation is allowed. This could result
     * in a SecurityException.
     *
     * @param port the port to listen to
     * @param backlog how many connections are queued
     * @return the <code>ServerSocket</code>
     * @throws IOException for networking errors
     * @throws SecurityException if a security manager exists and its
     *         <code>checkListen</code> method doesn't allow the operation.
     * @throws IllegalArgumentException if the port parameter is outside the
     *         specified range of valid port values, which is between 0 and
     *         65535, inclusive.
     * @see    SecurityManager#checkListen
     * @see java.net.ServerSocket#ServerSocket(int, int)
     */
    public abstract ServerSocket
    createServerSocket(int port, int backlog)
    throws IOException;


    /**
     * Returns a server socket bound to the specified port,
     * with a specified listen backlog and local IP.
     * <P>
     * The <code>ifAddress</code> argument can be used on a multi-homed
     * host for a <code>ServerSocket</code> that will only accept connect
     * requests to one of its addresses. If <code>ifAddress</code> is null,
     * it will accept connections on all local addresses. The socket is
     * configured with the socket options (such as accept timeout) given
     * to this factory.
     * <P>
     * The <code>backlog</code> argument must be a positive
     * value greater than 0. If the value passed if equal or less
     * than 0, then the default value will be assumed.
     * <P>
     * If there is a security manager, its <code>checkListen</code>
     * method is called with the <code>port</code> argument as its
     * argument to ensure the operation is allowed. This could result
     * in a SecurityException.
     *
     * @param port the port to listen to
     * @param backlog how many connections are queued
     * @param ifAddress the network interface address to use
     * @return the <code>ServerSocket</code>
     * @throws IOException for networking errors
     * @throws SecurityException if a security manager exists and its
     *         <code>checkListen</code> method doesn't allow the operation.
     * @throws IllegalArgumentException if the port parameter is outside the
     *         specified range of valid port values, which is between 0 and
     *         65535, inclusive.
     * @see    SecurityManager#checkListen
     * @see java.net.ServerSocket#ServerSocket(int, int, java.net.InetAddress)
     */
    public abstract ServerSocket
    createServerSocket(int port, int backlog, InetAddress ifAddress)
    throws IOException;
}


//
// The default factory has NO intelligence.  In fact it's not clear
// what sort of intelligence servers need; the onus is on clients,
// who have to know how to tunnel etc.
//
class DefaultServerSocketFactory extends ServerSocketFactory {

    DefaultServerSocketFactory()
    {
        /* NOTHING */
    }

    public ServerSocket createServerSocket()
    throws IOException
    {
        return new ServerSocket();
    }

    public ServerSocket createServerSocket(int port)
    throws IOException
    {
        return new ServerSocket(port);
    }

    public ServerSocket createServerSocket(int port, int backlog)
    throws IOException
    {
        return new ServerSocket(port, backlog);
    }

    public ServerSocket
    createServerSocket(int port, int backlog, InetAddress ifAddress)
    throws IOException
    {
        return new ServerSocket(port, backlog, ifAddress);
    }
}
