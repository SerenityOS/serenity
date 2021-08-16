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

package java.net;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;

/**
 * Abstract datagram and multicast socket implementation base class.
 *
 * @author Pavani Diwanji
 * @since  1.1
 */

public abstract class DatagramSocketImpl implements SocketOptions {

    /**
     * Constructor for subclasses to call.
     */
    public DatagramSocketImpl() {}

    /**
     * The local port number.
     */
    protected int localPort;

    /**
     * The file descriptor object.
     */
    protected FileDescriptor fd;

    int dataAvailable() {
        // default impl returns zero, which disables the calling
        // functionality
        return 0;
    }

    /**
     * Creates a datagram socket.
     * @throws    SocketException if there is an error in the
     * underlying protocol, such as a TCP error.
     */
    protected abstract void create() throws SocketException;

    /**
     * Binds a datagram socket to a local port and address.
     * @param     lport the local port
     * @param     laddr the local address
     * @throws    SocketException if there is an error in the
     *            underlying protocol, such as a TCP error.
     */
    protected abstract void bind(int lport, InetAddress laddr) throws SocketException;

    /**
     * Sends a datagram packet. The packet contains the data and the
     * destination address to send the packet to.
     * @param    p the packet to be sent.
     * @throws   IOException if an I/O exception occurs while sending the
     *           datagram packet.
     * @throws   PortUnreachableException may be thrown if the socket is connected
     *           to a currently unreachable destination. Note, there is no guarantee that
     *           the exception will be thrown.
     */
    protected abstract void send(DatagramPacket p) throws IOException;

    /**
     * Connects a datagram socket to a remote destination. This associates the remote
     * address with the local socket so that datagrams may only be sent to this destination
     * and received from this destination. This may be overridden to call a native
     * system connect.
     *
     * <p>If the remote destination to which the socket is connected does not
     * exist, or is otherwise unreachable, and if an ICMP destination unreachable
     * packet has been received for that address, then a subsequent call to
     * send or receive may throw a PortUnreachableException.
     * Note, there is no guarantee that the exception will be thrown.
     * @param   address the remote InetAddress to connect to
     * @param   port the remote port number
     * @throws  SocketException may be thrown if the socket cannot be
     *          connected to the remote destination
     * @since   1.4
     */
    protected void connect(InetAddress address, int port) throws SocketException {}

    /**
     * Disconnects a datagram socket from its remote destination.
     * @since 1.4
     */
    protected void disconnect() {}

    /**
     * Peek at the packet to see who it is from. Updates the specified {@code InetAddress}
     * to the address which the packet came from.
     * @param     i an InetAddress object
     * @return    the port number which the packet came from.
     * @throws    IOException if an I/O exception occurs
     * @throws    PortUnreachableException may be thrown if the socket is connected
     *            to a currently unreachable destination. Note, there is no guarantee that the
     *            exception will be thrown.
     */
    protected abstract int peek(InetAddress i) throws IOException;

    /**
     * Peek at the packet to see who it is from. The data is copied into the specified
     * {@code DatagramPacket}. The data is returned,
     * but not consumed, so that a subsequent peekData/receive operation
     * will see the same data.
     * @param     p the Packet Received.
     * @return    the port number which the packet came from.
     * @throws    IOException if an I/O exception occurs
     * @throws    PortUnreachableException may be thrown if the socket is connected
     *            to a currently unreachable destination. Note, there is no guarantee that the
     *            exception will be thrown.
     * @since 1.4
     */
    protected abstract int peekData(DatagramPacket p) throws IOException;
    /**
     * Receive the datagram packet.
     * @param     p the Packet Received.
     * @throws    IOException if an I/O exception occurs
     *            while receiving the datagram packet.
     * @throws    PortUnreachableException may be thrown if the socket is connected
     *            to a currently unreachable destination. Note, there is no guarantee that the
     *            exception will be thrown.
     */
    protected abstract void receive(DatagramPacket p) throws IOException;

    /**
     * Set the TTL (time-to-live) option.
     * @param ttl a byte specifying the TTL value
     *
     * @deprecated use setTimeToLive instead.
     * @throws    IOException if an I/O exception occurs while setting
     * the time-to-live option.
     * @see #getTTL()
     */
    @Deprecated
    protected abstract void setTTL(byte ttl) throws IOException;

    /**
     * Retrieve the TTL (time-to-live) option.
     *
     * @throws    IOException if an I/O exception occurs
     * while retrieving the time-to-live option
     * @deprecated use getTimeToLive instead.
     * @return a byte representing the TTL value
     * @see #setTTL(byte)
     */
    @Deprecated
    protected abstract byte getTTL() throws IOException;

    /**
     * Set the TTL (time-to-live) option.
     * @param ttl an {@code int} specifying the time-to-live value
     * @throws    IOException if an I/O exception occurs
     * while setting the time-to-live option.
     * @see #getTimeToLive()
     */
    protected abstract void setTimeToLive(int ttl) throws IOException;

    /**
     * Retrieve the TTL (time-to-live) option.
     * @throws    IOException if an I/O exception occurs
     * while retrieving the time-to-live option
     * @return an {@code int} representing the time-to-live value
     * @see #setTimeToLive(int)
     */
    protected abstract int getTimeToLive() throws IOException;

    /**
     * Join the multicast group.
     * @param inetaddr multicast address to join.
     * @throws    IOException if an I/O exception occurs
     * while joining the multicast group.
     */
    protected abstract void join(InetAddress inetaddr) throws IOException;

    /**
     * Leave the multicast group.
     * @param inetaddr multicast address to leave.
     * @throws    IOException if an I/O exception occurs
     * while leaving the multicast group.
     */
    protected abstract void leave(InetAddress inetaddr) throws IOException;

    /**
     * Join the multicast group.
     * @param mcastaddr address to join.
     * @param netIf specifies the local interface to receive multicast
     *        datagram packets
     * @throws IOException if an I/O exception occurs while joining
     * the multicast group
     * @since 1.4
     */
    protected abstract void joinGroup(SocketAddress mcastaddr,
                                      NetworkInterface netIf)
        throws IOException;

    /**
     * Leave the multicast group.
     * @param mcastaddr address to leave.
     * @param netIf specified the local interface to leave the group at
     * @throws IOException if an I/O exception occurs while leaving
     * the multicast group
     * @since 1.4
     */
    protected abstract void leaveGroup(SocketAddress mcastaddr,
                                       NetworkInterface netIf)
        throws IOException;

    /**
     * Close the socket.
     */
    protected abstract void close();

    /**
     * Gets the local port.
     * @return an {@code int} representing the local port value
     */
    protected int getLocalPort() {
        return localPort;
    }

    /**
     * Gets the datagram socket file descriptor.
     * @return a {@code FileDescriptor} object representing the datagram socket
     * file descriptor
     */
    protected FileDescriptor getFileDescriptor() {
        return fd;
    }

    /**
     * Called to set a socket option.
     *
     * @implSpec
     * The default implementation of this method first checks that the given
     * socket option {@code name} is not null, then throws {@code
     * UnsupportedOperationException}. Subclasses should override this method
     * with an appropriate implementation.
     *
     * @param  <T> The type of the socket option value
     * @param  name The socket option
     * @param  value The value of the socket option. A value of {@code null}
     *              may be valid for some options.
     *
     * @throws UnsupportedOperationException if the DatagramSocketImpl does not
     *         support the option
     * @throws IllegalArgumentException if the value is not valid for
     *         the option
     * @throws IOException if an I/O error occurs, or if the socket is closed
     * @throws NullPointerException if name is {@code null}
     *
     * @since 9
     */
    protected <T> void setOption(SocketOption<T> name, T value) throws IOException {
        Objects.requireNonNull(name);
        throw new UnsupportedOperationException("'" + name + "' not supported");
    }

    /**
     * Called to get a socket option.
     *
     * @implSpec
     * The default implementation of this method first checks that the given
     * socket option {@code name} is not null, then throws {@code
     * UnsupportedOperationException}. Subclasses should override this method
     * with an appropriate implementation.
     *
     * @param  <T> The type of the socket option value
     * @param  name The socket option
     * @return the socket option
     *
     * @throws UnsupportedOperationException if the DatagramSocketImpl does not
     *         support the option
     * @throws IOException if an I/O error occurs, or if the socket is closed
     * @throws NullPointerException if name is {@code null}
     *
     * @since 9
     */
    protected <T> T getOption(SocketOption<T> name) throws IOException {
        Objects.requireNonNull(name);
        throw new UnsupportedOperationException("'" + name + "' not supported");
    }

    /**
     * Returns a set of SocketOptions supported by this impl
     * and by this impl's socket (DatagramSocket or MulticastSocket)
     *
     * @implSpec
     * The default implementation of this method returns an empty set.
     * Subclasses should override this method with an appropriate implementation.
     *
     * @return a Set of SocketOptions
     *
     * @since 9
     */
    protected Set<SocketOption<?>> supportedOptions() {
        return Set.of();
    }
}
