/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.channels;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.NetPermission;
import java.net.ProtocolFamily;
import java.net.ServerSocket;
import java.net.SocketOption;
import java.net.SocketAddress;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;
import static java.util.Objects.requireNonNull;

/**
 * A selectable channel for stream-oriented listening sockets.
 *
 * <p> A server-socket channel is created by invoking one of the {@code open}
 * methods of this class. The no-arg {@link #open() open} method opens a server-socket
 * channel for an <i>Internet protocol</i> socket. The {@link #open(ProtocolFamily)}
 * method is used to open a server-socket channel for a socket of a specified
 * protocol family. It is not possible to create a channel for an arbitrary,
 * pre-existing socket. A newly-created server-socket channel is open but not yet
 * bound. An attempt to invoke the {@link #accept() accept} method of an
 * unbound server-socket channel will cause a {@link NotYetBoundException}
 * to be thrown. A server-socket channel can be bound by invoking one of the
 * {@link #bind(java.net.SocketAddress, int) bind} methods defined by this class.
 *
 * <p> Socket options are configured using the {@link #setOption(SocketOption,Object)
 * setOption} method. Server-socket channels for <i>Internet protocol</i> sockets
 * support the following options:
 * <blockquote>
 * <table class="striped">
 * <caption style="display:none">Socket options</caption>
 * <thead>
 *   <tr>
 *     <th scope="col">Option Name</th>
 *     <th scope="col">Description</th>
 *   </tr>
 * </thead>
 * <tbody>
 *   <tr>
 *     <th scope="row"> {@link java.net.StandardSocketOptions#SO_RCVBUF SO_RCVBUF} </th>
 *     <td> The size of the socket receive buffer </td>
 *   </tr>
 *   <tr>
 *     <th scope="row"> {@link java.net.StandardSocketOptions#SO_REUSEADDR SO_REUSEADDR} </th>
 *     <td> Re-use address </td>
 *   </tr>
 * </tbody>
 * </table>
 * </blockquote>
 *
 * <p> Server-socket channels for <i>Unix domain</i> sockets support:
 * <blockquote>
 * <table class="striped">
 * <caption style="display:none">Socket options</caption>
 * <thead>
 *   <tr>
 *     <th scope="col">Option Name</th>
 *     <th scope="col">Description</th>
 *   </tr>
 * </thead>
 * <tbody>
 *   <tr>
 *     <th scope="row"> {@link java.net.StandardSocketOptions#SO_RCVBUF SO_RCVBUF} </th>
 *     <td> The size of the socket receive buffer </td>
 *   </tr>
 * </tbody>
 * </table>
 * </blockquote>
 *
 * <p> Additional (implementation specific) options may also be supported.
 *
 * <p> Server-socket channels are safe for use by multiple concurrent threads.
 * </p>
 *
 * @author Mark Reinhold
 * @author JSR-51 Expert Group
 * @since 1.4
 */

public abstract class ServerSocketChannel
    extends AbstractSelectableChannel
    implements NetworkChannel
{

    /**
     * Initializes a new instance of this class.
     *
     * @param  provider
     *         The provider that created this channel
     */
    protected ServerSocketChannel(SelectorProvider provider) {
        super(provider);
    }

    /**
     * Opens a server-socket channel for an <i>Internet protocol</i> socket.
     *
     * <p> The new channel is created by invoking the {@link
     * java.nio.channels.spi.SelectorProvider#openServerSocketChannel
     * openServerSocketChannel} method of the system-wide default {@link
     * java.nio.channels.spi.SelectorProvider} object.
     *
     * <p> The new channel's socket is initially unbound; it must be bound to a
     * specific address via one of its socket's {@link
     * java.net.ServerSocket#bind(SocketAddress) bind} methods before
     * connections can be accepted.  </p>
     *
     * @return  A new socket channel
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @see     <a href="../../net/doc-files/net-properties.html#Ipv4IPv6">
     *          java.net.preferIPv4Stack</a> system property
     */
    public static ServerSocketChannel open() throws IOException {
        return SelectorProvider.provider().openServerSocketChannel();
    }

    /**
     * Opens a server-socket channel. The {@code family} parameter specifies the
     * {@link ProtocolFamily protocol family} of the channel's socket.
     *
     * <p> The new channel is created by invoking the {@link
     * java.nio.channels.spi.SelectorProvider#openServerSocketChannel(ProtocolFamily)
     * openServerSocketChannel(ProtocolFamily)} method of the system-wide default {@link
     * java.nio.channels.spi.SelectorProvider} object. </p>
     *
     * @param   family
     *          The protocol family
     *
     * @return  A new socket channel
     *
     * @throws  UnsupportedOperationException
     *          If the specified protocol family is not supported. For example,
     *          suppose the parameter is specified as {@link
     *          java.net.StandardProtocolFamily#INET6 StandardProtocolFamily.INET6}
     *          but IPv6 is not enabled on the platform.
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @see     <a href="../../net/doc-files/net-properties.html#Ipv4IPv6">
     *          java.net.preferIPv4Stack</a> system property
     *
     * @since 15
     */
    public static ServerSocketChannel open(ProtocolFamily family) throws IOException {
        return SelectorProvider.provider().openServerSocketChannel(requireNonNull(family));
    }

    /**
     * Returns an operation set identifying this channel's supported
     * operations.
     *
     * <p> Server-socket channels only support the accepting of new
     * connections, so this method returns {@link SelectionKey#OP_ACCEPT}.
     * </p>
     *
     * @return  The valid-operation set
     */
    public final int validOps() {
        return SelectionKey.OP_ACCEPT;
    }


    // -- ServerSocket-specific operations --

    /**
     * Binds the channel's socket to a local address and configures the socket
     * to listen for connections.
     *
     * <p> An invocation of this method is equivalent to the following:
     * <blockquote><pre>
     * bind(local, 0);
     * </pre></blockquote>
     *
     * @param   local
     *          The local address to bind the socket, or {@code null} to bind
     *          to an automatically assigned socket address
     *
     * @return  This channel
     *
     * @throws  AlreadyBoundException               {@inheritDoc}
     * @throws  UnsupportedAddressTypeException     {@inheritDoc}
     * @throws  ClosedChannelException              {@inheritDoc}
     * @throws  IOException                         {@inheritDoc}
     * @throws  SecurityException
     *          If a security manager has been installed and it denies the
     *          operation
     *
     * @since 1.7
     */
    public final ServerSocketChannel bind(SocketAddress local)
        throws IOException
    {
        return bind(local, 0);
    }

    /**
     * Binds the channel's socket to a local address and configures the socket to
     * listen for connections.
     *
     * <p> This method is used to establish an association between the socket and
     * a local address. For <i>Internet protocol</i> sockets, once an association
     * is established then the socket remains bound until the channel is closed.
     *
     * <p> The {@code backlog} parameter is the maximum number of pending
     * connections on the socket. Its exact semantics are implementation specific.
     * In particular, an implementation may impose a maximum length or may choose
     * to ignore the parameter altogether. If the {@code backlog} parameter has
     * the value {@code 0}, or a negative value, then an implementation specific
     * default is used.
     *
     * @apiNote
     * Binding a server socket channel for a <i>Unix Domain</i> socket, creates a
     * file corresponding to the file path in the {@link UnixDomainSocketAddress}.
     * This file persists after the channel is closed, and must be removed before
     * another socket can bind to the same name. Binding to a {@code null} address
     * causes the socket to be <i>automatically</i> bound to some unique file
     * in a system temporary location. The associated socket file also persists
     * after the channel is closed. Its name can be obtained from the channel's
     * local socket address.
     *
     * @implNote
     * Each platform enforces an implementation specific, maximum length for the
     * name of a <i>Unix Domain</i> socket. This limitation is enforced when a
     * channel is bound. The maximum length is typically close to and generally
     * not less than 100 bytes. This limitation also applies to <i>automatically</i>
     * bound server socket channels. See the <i>Unix domain</i>
     * <a href="../../net/doc-files/net-properties.html#Unixdomain">networking
     * properties</a> that can be used to select the temporary directory where
     * these sockets are created.
     *
     * @param   local
     *          The address to bind the socket, or {@code null} to bind to
     *          an automatically assigned socket address
     * @param   backlog
     *          The maximum number of pending connections
     *
     * @return  This channel
     *
     * @throws  AlreadyBoundException
     *          If the socket is already bound
     * @throws  UnsupportedAddressTypeException
     *          If the type of the given address is not supported
     * @throws  ClosedChannelException
     *          If this channel is closed
     * @throws  IOException
     *          If some other I/O error occurs
     * @throws  SecurityException
     *          If a security manager has been installed and its {@link
     *          SecurityManager#checkListen checkListen} method denies
     *          the operation for an <i>Internet protocol</i> socket address,
     *          or for a <i>Unix domain</i> socket address if it denies
     *          {@link NetPermission}{@code("accessUnixDomainSocket")}.
     *
     * @since 1.7
     */
    public abstract ServerSocketChannel bind(SocketAddress local, int backlog)
        throws IOException;

    /**
     * @throws  UnsupportedOperationException           {@inheritDoc}
     * @throws  IllegalArgumentException                {@inheritDoc}
     * @throws  ClosedChannelException                  {@inheritDoc}
     * @throws  IOException                             {@inheritDoc}
     *
     * @since 1.7
     */
    public abstract <T> ServerSocketChannel setOption(SocketOption<T> name, T value)
        throws IOException;

    /**
     * Retrieves a server socket associated with this channel.
     *
     * <p> The returned object will not declare any public methods that are not
     * declared in the {@link java.net.ServerSocket} class.  </p>
     *
     * @return  A server socket associated with this channel
     *
     * @throws  UnsupportedOperationException
     *          If the channel's socket is not an <i>Internet protocol</i> socket
     */
    public abstract ServerSocket socket();

    /**
     * Accepts a connection made to this channel's socket.
     *
     * <p> If this channel is in non-blocking mode then this method will
     * immediately return {@code null} if there are no pending connections.
     * Otherwise it will block indefinitely until a new connection is available
     * or an I/O error occurs.
     *
     * <p> The socket channel returned by this method, if any, will be in
     * blocking mode regardless of the blocking mode of this channel.
     *
     * <p> If bound to an <i>Internet protocol</i> socket address, this method
     * performs exactly the same security checks as the {@link
     * java.net.ServerSocket#accept accept} method of the {@link java.net.ServerSocket}
     * class.  That is, if a security manager has been installed then for each
     * new connection this method verifies that the address and port number
     * of the connection's remote endpoint are permitted by the security
     * manager's {@link java.lang.SecurityManager#checkAccept checkAccept}
     * method. If bound to a <i>Unix Domain</i> socket address, this method checks
     * {@link NetPermission}{@code ("accessUnixDomainSocket")}.
     *
     * @return  The socket channel for the new connection,
     *          or {@code null} if this channel is in non-blocking mode
     *          and no connection is available to be accepted
     *
     * @throws  ClosedChannelException
     *          If this channel is closed
     *
     * @throws  AsynchronousCloseException
     *          If another thread closes this channel
     *          while the accept operation is in progress
     *
     * @throws  ClosedByInterruptException
     *          If another thread interrupts the current thread
     *          while the accept operation is in progress, thereby
     *          closing the channel and setting the current thread's
     *          interrupt status
     *
     * @throws  NotYetBoundException
     *          If this channel's socket has not yet been bound
     *
     * @throws  SecurityException
     *          If a security manager has been installed and this
     *          channel is bound to an {@link InetSocketAddress}
     *          and the security manager denies access to the remote endpoint
     *          of the new connection, or if this channel is bound to a
     *          {@link UnixDomainSocketAddress} and the security manager
     *          denies {@link NetPermission}{@code ("accessUnixDomainSocket")}
     *
     * @throws  IOException
     *          If some other I/O error occurs
     */
    public abstract SocketChannel accept() throws IOException;

    /**
     * {@inheritDoc}
     *
     * If there is a security manager set, its {@code checkConnect} method is
     * called with the local address and {@code -1} as its arguments to see
     * if the operation is allowed. If the operation is not allowed,
     * a {@code SocketAddress} representing the
     * {@link java.net.InetAddress#getLoopbackAddress loopback} address and the
     * local port of the channel's socket is returned.
     *
     * <p> Where the channel is bound to a <i>Unix Domain</i> socket address, the socket
     * address is a {@link UnixDomainSocketAddress}. If there is a security manager
     * set, its {@link SecurityManager#checkPermission(java.security.Permission)
     * checkPermission} method is called with {@link NetPermission}{@code
     * ("accessUnixDomainSocket")}. If the operation is not allowed an unnamed
     * {@link UnixDomainSocketAddress} is returned.
     *
     * @return  The {@code SocketAddress} that the socket is bound to, or the
     *          {@code SocketAddress} representing the loopback address or empty
     *          path if denied by the security manager, or {@code null} if the
     *          channel's socket is not bound
     *
     * @throws  ClosedChannelException     {@inheritDoc}
     * @throws  IOException                {@inheritDoc}
     */
    @Override
    public abstract SocketAddress getLocalAddress() throws IOException;
}
