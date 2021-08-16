/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Defines the <em>standard</em> socket options.
 *
 * <p> The {@link SocketOption#name name} of each socket option defined by this
 * class is its field name.
 *
 * <p> In this release, the socket options defined here are used by {@link
 * java.nio.channels.NetworkChannel network} channels in the {@link
 * java.nio.channels channels} package.
 *
 * @since 1.7
 */

public final class StandardSocketOptions {
    private StandardSocketOptions() { }

    // -- SOL_SOCKET --

    /**
     * Allow transmission of broadcast datagrams.
     *
     * <p> The value of this socket option is a {@code Boolean} that represents
     * whether the option is enabled or disabled. The option is specific to
     * datagram-oriented sockets sending to {@link java.net.Inet4Address IPv4}
     * broadcast addresses. When the socket option is enabled then the socket
     * can be used to send <em>broadcast datagrams</em>.
     *
     * <p> The initial value of this socket option is {@code FALSE}. The socket
     * option may be enabled or disabled at any time. Some operating systems may
     * require that the Java virtual machine be started with implementation
     * specific privileges to enable this option or send broadcast datagrams.
     *
     * @see <a href="http://www.ietf.org/rfc/rfc919.txt">RFC&nbsp;929:
     * Broadcasting Internet Datagrams</a>
     * @see DatagramSocket#setBroadcast
     */
    public static final SocketOption<Boolean> SO_BROADCAST =
        new StdSocketOption<Boolean>("SO_BROADCAST", Boolean.class);

    /**
     * Keep connection alive.
     *
     * <p> The value of this socket option is a {@code Boolean} that represents
     * whether the option is enabled or disabled. When the {@code SO_KEEPALIVE}
     * option is enabled the operating system may use a <em>keep-alive</em>
     * mechanism to periodically probe the other end of a connection when the
     * connection is otherwise idle. The exact semantics of the keep alive
     * mechanism is system dependent and therefore unspecified.
     *
     * <p> The initial value of this socket option is {@code FALSE}. The socket
     * option may be enabled or disabled at any time.
     *
     * @see <a href="http://www.ietf.org/rfc/rfc1122.txt">RFC&nbsp;1122
     * Requirements for Internet Hosts -- Communication Layers</a>
     * @see Socket#setKeepAlive
     */
    public static final SocketOption<Boolean> SO_KEEPALIVE =
        new StdSocketOption<Boolean>("SO_KEEPALIVE", Boolean.class);

    /**
     * The size of the socket send buffer.
     *
     * <p> The value of this socket option is an {@code Integer} that is the
     * size of the socket send buffer in bytes. The socket send buffer is an
     * output buffer used by the networking implementation. It may need to be
     * increased for high-volume connections. The value of the socket option is
     * a <em>hint</em> to the implementation to size the buffer and the actual
     * size may differ. The socket option can be queried to retrieve the actual
     * size.
     *
     * <p> For datagram-oriented sockets, the size of the send buffer may limit
     * the size of the datagrams that may be sent by the socket. Whether
     * datagrams larger than the buffer size are sent or discarded is system
     * dependent.
     *
     * <p> The initial/default size of the socket send buffer and the range of
     * allowable values is system dependent although a negative size is not
     * allowed. An attempt to set the socket send buffer to larger than its
     * maximum size causes it to be set to its maximum size.
     *
     * <p> An implementation allows this socket option to be set before the
     * socket is bound or connected. Whether an implementation allows the
     * socket send buffer to be changed after the socket is bound is system
     * dependent.
     *
     * @see Socket#setSendBufferSize
     */
    public static final SocketOption<Integer> SO_SNDBUF =
        new StdSocketOption<Integer>("SO_SNDBUF", Integer.class);


    /**
     * The size of the socket receive buffer.
     *
     * <p> The value of this socket option is an {@code Integer} that is the
     * size of the socket receive buffer in bytes. The socket receive buffer is
     * an input buffer used by the networking implementation. It may need to be
     * increased for high-volume connections or decreased to limit the possible
     * backlog of incoming data. The value of the socket option is a
     * <em>hint</em> to the implementation to size the buffer and the actual
     * size may differ.
     *
     * <p> For datagram-oriented sockets, the size of the receive buffer may
     * limit the size of the datagrams that can be received. Whether datagrams
     * larger than the buffer size can be received is system dependent.
     * Increasing the socket receive buffer may be important for cases where
     * datagrams arrive in bursts faster than they can be processed.
     *
     * <p> In the case of stream-oriented sockets and the TCP/IP protocol, the
     * size of the socket receive buffer may be used when advertising the size
     * of the TCP receive window to the remote peer.
     *
     * <p> The initial/default size of the socket receive buffer and the range
     * of allowable values is system dependent although a negative size is not
     * allowed. An attempt to set the socket receive buffer to larger than its
     * maximum size causes it to be set to its maximum size.
     *
     * <p> An implementation allows this socket option to be set before the
     * socket is bound or connected. Whether an implementation allows the
     * socket receive buffer to be changed after the socket is bound is system
     * dependent.
     *
     * @see <a href="http://www.ietf.org/rfc/rfc1323.txt">RFC&nbsp;1323: TCP
     * Extensions for High Performance</a>
     * @see Socket#setReceiveBufferSize
     * @see ServerSocket#setReceiveBufferSize
     */
    public static final SocketOption<Integer> SO_RCVBUF =
        new StdSocketOption<Integer>("SO_RCVBUF", Integer.class);

    /**
     * Re-use address.
     *
     * <p> The value of this socket option is a {@code Boolean} that represents
     * whether the option is enabled or disabled. The exact semantics of this
     * socket option are socket type and system dependent.
     *
     * <p> In the case of stream-oriented sockets, this socket option will
     * usually determine whether the socket can be bound to a socket address
     * when a previous connection involving that socket address is in the
     * <em>TIME_WAIT</em> state. On implementations where the semantics differ,
     * and the socket option is not required to be enabled in order to bind the
     * socket when a previous connection is in this state, then the
     * implementation may choose to ignore this option.
     *
     * <p> For datagram-oriented sockets the socket option is used to allow
     * multiple programs bind to the same address. This option should be enabled
     * when the socket is to be used for Internet Protocol (IP) multicasting.
     *
     * <p> An implementation allows this socket option to be set before the
     * socket is bound or connected. Changing the value of this socket option
     * after the socket is bound has no effect. The default value of this
     * socket option is system dependent.
     *
     * @see <a href="http://www.ietf.org/rfc/rfc793.txt">RFC&nbsp;793: Transmission
     * Control Protocol</a>
     * @see ServerSocket#setReuseAddress
     */
    public static final SocketOption<Boolean> SO_REUSEADDR =
        new StdSocketOption<Boolean>("SO_REUSEADDR", Boolean.class);

    /**
     * Re-use port.
     *
     * <p> The value of this socket option is a {@code Boolean} that represents
     * whether the option is enabled or disabled. The exact semantics of this
     * socket option are socket type and system dependent.
     *
     * <p> In the case of stream-oriented sockets, this socket option usually allows
     * multiple listening sockets to be bound to both same address
     * and same port.
     *
     * <p> For datagram-oriented sockets the socket option usually allows
     * multiple UDP sockets to be bound to the same address and port.
     *
     * <p> An implementation allows this socket option to be set before the
     * socket is bound or connected. Changing the value of this socket option
     * after the socket is bound has no effect.
     *
     * @since 9
     */
    public static final SocketOption<Boolean> SO_REUSEPORT =
        new StdSocketOption<Boolean>("SO_REUSEPORT", Boolean.class);

    /**
     * Linger on close if data is present.
     *
     * <p> The value of this socket option is an {@code Integer} that controls
     * the action taken when unsent data is queued on the socket and a method
     * to close the socket is invoked. If the value of the socket option is zero
     * or greater, then it represents a timeout value, in seconds, known as the
     * <em>linger interval</em>. The linger interval is the timeout for the
     * {@code close} method to block while the operating system attempts to
     * transmit the unsent data or it decides that it is unable to transmit the
     * data. If the value of the socket option is less than zero then the option
     * is disabled. In that case the {@code close} method does not wait until
     * unsent data is transmitted; if possible the operating system will transmit
     * any unsent data before the connection is closed.
     *
     * <p> This socket option is intended for use with sockets that are configured
     * in {@link java.nio.channels.SelectableChannel#isBlocking() blocking} mode
     * only. The behavior of the {@code close} method when this option is
     * enabled on a non-blocking socket is not defined.
     *
     * <p> The initial value of this socket option is a negative value, meaning
     * that the option is disabled. The option may be enabled, or the linger
     * interval changed, at any time. The maximum value of the linger interval
     * is system dependent. Setting the linger interval to a value that is
     * greater than its maximum value causes the linger interval to be set to
     * its maximum value.
     *
     * @see Socket#setSoLinger
     */
    public static final SocketOption<Integer> SO_LINGER =
        new StdSocketOption<Integer>("SO_LINGER", Integer.class);


    // -- IPPROTO_IP --

    /**
     * The Type of Service (ToS) octet in the Internet Protocol (IP) header.
     *
     * <p> The value of this socket option is an {@code Integer} representing
     * the value of the ToS octet in IP packets sent by sockets to an {@link
     * StandardProtocolFamily#INET IPv4} socket. The interpretation of the ToS
     * octet is network specific and is not defined by this class. Further
     * information on the ToS octet can be found in <a
     * href="http://www.ietf.org/rfc/rfc1349.txt">RFC&nbsp;1349</a> and <a
     * href="http://www.ietf.org/rfc/rfc2474.txt">RFC&nbsp;2474</a>. The value
     * of the socket option is a <em>hint</em>. An implementation may ignore the
     * value, or ignore specific values.
     *
     * <p> The initial/default value of the TOS field in the ToS octet is
     * implementation specific but will typically be {@code 0}. For
     * datagram-oriented sockets the option may be configured at any time after
     * the socket has been bound. The new value of the octet is used when sending
     * subsequent datagrams. It is system dependent whether this option can be
     * queried or changed prior to binding the socket.
     *
     * <p> The behavior of this socket option on a stream-oriented socket, or an
     * {@link StandardProtocolFamily#INET6 IPv6} socket, is not defined in this
     * release.
     *
     * @see DatagramSocket#setTrafficClass
     */
    public static final SocketOption<Integer> IP_TOS =
        new StdSocketOption<Integer>("IP_TOS", Integer.class);

    /**
     * The network interface for Internet Protocol (IP) multicast datagrams.
     *
     * <p> The value of this socket option is a {@link NetworkInterface} that
     * represents the outgoing interface for multicast datagrams sent by the
     * datagram-oriented socket. For {@link StandardProtocolFamily#INET6 IPv6}
     * sockets then it is system dependent whether setting this option also
     * sets the outgoing interface for multicast datagrams sent to IPv4
     * addresses.
     *
     * <p> The initial/default value of this socket option may be {@code null}
     * to indicate that outgoing interface will be selected by the operating
     * system, typically based on the network routing tables. An implementation
     * allows this socket option to be set after the socket is bound. Whether
     * the socket option can be queried or changed prior to binding the socket
     * is system dependent.
     *
     * @see java.nio.channels.MulticastChannel
     */
    public static final SocketOption<NetworkInterface> IP_MULTICAST_IF =
        new StdSocketOption<NetworkInterface>("IP_MULTICAST_IF", NetworkInterface.class);

    /**
     * The <em>time-to-live</em> for Internet Protocol (IP) multicast datagrams.
     *
     * <p> The value of this socket option is an {@code Integer} in the range
     * {@code 0 <= value <= 255}. It is used to control the scope of multicast
     * datagrams sent by the datagram-oriented socket.
     * In the case of an {@link StandardProtocolFamily#INET IPv4} socket
     * the option is the time-to-live (TTL) on multicast datagrams sent by the
     * socket. Datagrams with a TTL of zero are not transmitted on the network
     * but may be delivered locally. In the case of an {@link
     * StandardProtocolFamily#INET6 IPv6} socket the option is the
     * <em>hop limit</em> which is number of <em>hops</em> that the datagram can
     * pass through before expiring on the network. For IPv6 sockets it is
     * system dependent whether the option also sets the <em>time-to-live</em>
     * on multicast datagrams sent to IPv4 addresses.
     *
     * <p> The initial/default value of the time-to-live setting is typically
     * {@code 1}. An implementation allows this socket option to be set after
     * the socket is bound. Whether the socket option can be queried or changed
     * prior to binding the socket is system dependent.
     *
     * @see java.nio.channels.MulticastChannel
     * @see MulticastSocket#setTimeToLive
     */
    public static final SocketOption<Integer> IP_MULTICAST_TTL =
        new StdSocketOption<Integer>("IP_MULTICAST_TTL", Integer.class);

    /**
     * Loopback for Internet Protocol (IP) multicast datagrams.
     *
     * <p> The value of this socket option is a {@code Boolean} that controls
     * the <em>loopback</em> of multicast datagrams. The value of the socket
     * option represents if the option is enabled or disabled.
     *
     * <p> The exact semantics of this socket options are system dependent.
     * In particular, it is system dependent whether the loopback applies to
     * multicast datagrams sent from the socket or received by the socket.
     * For {@link StandardProtocolFamily#INET6 IPv6} sockets then it is
     * system dependent whether the option also applies to multicast datagrams
     * sent to IPv4 addresses.
     *
     * <p> The initial/default value of this socket option is {@code TRUE}. An
     * implementation allows this socket option to be set after the socket is
     * bound. Whether the socket option can be queried or changed prior to
     * binding the socket is system dependent.
     *
     * @see java.nio.channels.MulticastChannel
     */
    public static final SocketOption<Boolean> IP_MULTICAST_LOOP =
        new StdSocketOption<Boolean>("IP_MULTICAST_LOOP", Boolean.class);


    // -- IPPROTO_TCP --

    /**
     * Disable the Nagle algorithm.
     *
     * <p> The value of this socket option is a {@code Boolean} that represents
     * whether the option is enabled or disabled. The socket option is specific to
     * stream-oriented sockets using the TCP/IP protocol. TCP/IP uses an algorithm
     * known as <em>The Nagle Algorithm</em> to coalesce short segments and
     * improve network efficiency.
     *
     * <p> The default value of this socket option is {@code FALSE}. The
     * socket option should only be enabled in cases where it is known that the
     * coalescing impacts performance. The socket option may be enabled at any
     * time. In other words, the Nagle Algorithm can be disabled. Once the option
     * is enabled, it is system dependent whether it can be subsequently
     * disabled. If it cannot, then invoking the {@code setOption} method to
     * disable the option has no effect.
     *
     * @see <a href="http://www.ietf.org/rfc/rfc1122.txt">RFC&nbsp;1122:
     * Requirements for Internet Hosts -- Communication Layers</a>
     * @see Socket#setTcpNoDelay
     */
    public static final SocketOption<Boolean> TCP_NODELAY =
        new StdSocketOption<Boolean>("TCP_NODELAY", Boolean.class);


    private static class StdSocketOption<T> implements SocketOption<T> {
        private final String name;
        private final Class<T> type;
        StdSocketOption(String name, Class<T> type) {
            this.name = name;
            this.type = type;
        }
        @Override public String name() { return name; }
        @Override public Class<T> type() { return type; }
        @Override public String toString() { return name; }
    }
}
