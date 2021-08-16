/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * This class creates sockets.  It may be subclassed by other factories,
 * which create particular subclasses of sockets and thus provide a general
 * framework for the addition of public socket-level functionality.
 *
 * <P> Socket factories are a simple way to capture a variety of policies
 * related to the sockets being constructed, producing such sockets in
 * a way which does not require special configuration of the code which
 * asks for the sockets:  <UL>
 *
 *      <LI> Due to polymorphism of both factories and sockets, different
 *      kinds of sockets can be used by the same application code just
 *      by passing it different kinds of factories.
 *
 *      <LI> Factories can themselves be customized with parameters used
 *      in socket construction.  So for example, factories could be
 *      customized to return sockets with different networking timeouts
 *      or security parameters already configured.
 *
 *      <LI> The sockets returned to the application can be subclasses
 *      of java.net.Socket, so that they can directly expose new APIs
 *      for features such as compression, security, record marking,
 *      statistics collection, or firewall tunneling.
 *
 *      </UL>
 *
 * <P> Factory classes are specified by environment-specific configuration
 * mechanisms.  For example, the <em>getDefault</em> method could return
 * a factory that was appropriate for a particular user or applet, and a
 * framework could use a factory customized to its own purposes.
 *
 * @since 1.4
 * @see ServerSocketFactory
 *
 * @author David Brownell
 */
public abstract class SocketFactory
{
    //
    // NOTE:  JDK 1.1 bug in class GC, this can get collected
    // even though it's always accessible via getDefault().
    //
    private static SocketFactory                theFactory;

    /**
     * Creates a <code>SocketFactory</code>.
     */
    protected SocketFactory() { /* NOTHING */ }


    /**
     * Returns a copy of the environment's default socket factory.
     *
     * @return the default <code>SocketFactory</code>
     */
    public static SocketFactory getDefault()
    {
        synchronized (SocketFactory.class) {
            if (theFactory == null) {
                //
                // Different implementations of this method SHOULD
                // work rather differently.  For example, driving
                // this from a system property, or using a different
                // implementation than JavaSoft's.
                //
                theFactory = new DefaultSocketFactory();
            }
        }

        return theFactory;
    }


    /**
     * Creates an unconnected socket.
     *
     * @return the unconnected socket
     * @throws IOException if the socket cannot be created
     * @see java.net.Socket#connect(java.net.SocketAddress)
     * @see java.net.Socket#connect(java.net.SocketAddress, int)
     * @see java.net.Socket#Socket()
     */
    public Socket createSocket() throws IOException {
        //
        // bug 6771432:
        // The Exception is used by HttpsClient to signal that
        // unconnected sockets have not been implemented.
        //
        UnsupportedOperationException uop = new
                UnsupportedOperationException();
        SocketException se =  new SocketException(
                "Unconnected sockets not implemented");
        se.initCause(uop);
        throw se;
    }


    /**
     * Creates a socket and connects it to the specified remote host
     * at the specified remote port.  This socket is configured using
     * the socket options established for this factory.
     * <p>
     * If there is a security manager, its <code>checkConnect</code>
     * method is called with the host address and <code>port</code>
     * as its arguments. This could result in a SecurityException.
     *
     * @param host the server host name with which to connect, or
     *        <code>null</code> for the loopback address.
     * @param port the server port
     * @return the <code>Socket</code>
     * @throws IOException if an I/O error occurs when creating the socket
     * @throws SecurityException if a security manager exists and its
     *         <code>checkConnect</code> method doesn't allow the operation.
     * @throws UnknownHostException if the host is not known
     * @throws IllegalArgumentException if the port parameter is outside the
     *         specified range of valid port values, which is between 0 and
     *         65535, inclusive.
     * @see SecurityManager#checkConnect
     * @see java.net.Socket#Socket(String, int)
     */
    public abstract Socket createSocket(String host, int port)
    throws IOException, UnknownHostException;


    /**
     * Creates a socket and connects it to the specified remote host
     * on the specified remote port.
     * The socket will also be bound to the local address and port supplied.
     * This socket is configured using
     * the socket options established for this factory.
     * <p>
     * If there is a security manager, its <code>checkConnect</code>
     * method is called with the host address and <code>port</code>
     * as its arguments. This could result in a SecurityException.
     *
     * @param host the server host name with which to connect, or
     *        <code>null</code> for the loopback address.
     * @param port the server port
     * @param localHost the local address the socket is bound to
     * @param localPort the local port the socket is bound to
     * @return the <code>Socket</code>
     * @throws IOException if an I/O error occurs when creating the socket
     * @throws SecurityException if a security manager exists and its
     *         <code>checkConnect</code> method doesn't allow the operation.
     * @throws UnknownHostException if the host is not known
     * @throws IllegalArgumentException if the port parameter or localPort
     *         parameter is outside the specified range of valid port values,
     *         which is between 0 and 65535, inclusive.
     * @see SecurityManager#checkConnect
     * @see java.net.Socket#Socket(String, int, java.net.InetAddress, int)
     */
    public abstract Socket
    createSocket(String host, int port, InetAddress localHost, int localPort)
    throws IOException, UnknownHostException;


    /**
     * Creates a socket and connects it to the specified port number
     * at the specified address.  This socket is configured using
     * the socket options established for this factory.
     * <p>
     * If there is a security manager, its <code>checkConnect</code>
     * method is called with the host address and <code>port</code>
     * as its arguments. This could result in a SecurityException.
     *
     * @param host the server host
     * @param port the server port
     * @return the <code>Socket</code>
     * @throws IOException if an I/O error occurs when creating the socket
     * @throws SecurityException if a security manager exists and its
     *         <code>checkConnect</code> method doesn't allow the operation.
     * @throws IllegalArgumentException if the port parameter is outside the
     *         specified range of valid port values, which is between 0 and
     *         65535, inclusive.
     * @throws NullPointerException if <code>host</code> is null.
     * @see SecurityManager#checkConnect
     * @see java.net.Socket#Socket(java.net.InetAddress, int)
     */
    public abstract Socket createSocket(InetAddress host, int port)
    throws IOException;


    /**
     * Creates a socket and connect it to the specified remote address
     * on the specified remote port.  The socket will also be bound
     * to the local address and port suplied.  The socket is configured using
     * the socket options established for this factory.
     * <p>
     * If there is a security manager, its <code>checkConnect</code>
     * method is called with the host address and <code>port</code>
     * as its arguments. This could result in a SecurityException.
     *
     * @param address the server network address
     * @param port the server port
     * @param localAddress the client network address
     * @param localPort the client port
     * @return the <code>Socket</code>
     * @throws IOException if an I/O error occurs when creating the socket
     * @throws SecurityException if a security manager exists and its
     *         <code>checkConnect</code> method doesn't allow the operation.
     * @throws IllegalArgumentException if the port parameter or localPort
     *         parameter is outside the specified range of valid port values,
     *         which is between 0 and 65535, inclusive.
     * @throws NullPointerException if <code>address</code> is null.
     * @see SecurityManager#checkConnect
     * @see java.net.Socket#Socket(java.net.InetAddress, int,
     *     java.net.InetAddress, int)
     */
    public abstract Socket
    createSocket(InetAddress address, int port,
        InetAddress localAddress, int localPort)
    throws IOException;
}


//
// The default factory has NO intelligence about policies like tunneling
// out through firewalls (e.g. SOCKS V4 or V5) or in through them
// (e.g. using SSL), or that some ports are reserved for use with SSL.
//
// Note that at least JDK 1.1 has a low level "SocketImpl" that
// knows about SOCKS V4 tunneling, so this isn't a totally bogus default.
//
// ALSO:  we may want to expose this class somewhere so other folk
// can reuse it, particularly if we start to add highly useful features
// such as ability to set connect timeouts.
//
class DefaultSocketFactory extends SocketFactory {

    public Socket createSocket() {
        return new Socket();
    }

    public Socket createSocket(String host, int port)
    throws IOException, UnknownHostException
    {
        return new Socket(host, port);
    }

    public Socket createSocket(InetAddress address, int port)
    throws IOException
    {
        return new Socket(address, port);
    }

    public Socket createSocket(String host, int port,
        InetAddress clientAddress, int clientPort)
    throws IOException, UnknownHostException
    {
        return new Socket(host, port, clientAddress, clientPort);
    }

    public Socket createSocket(InetAddress address, int port,
        InetAddress clientAddress, int clientPort)
    throws IOException
    {
        return new Socket(address, port, clientAddress, clientPort);
    }
}
