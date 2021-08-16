/*
 * Copyright (c) 1995, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MulticastChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Objects;
import java.util.Set;
import sun.net.NetProperties;
import sun.nio.ch.DefaultSelectorProvider;

/**
 * This class represents a socket for sending and receiving datagram packets.
 *
 * <p>A datagram socket is the sending or receiving point for a packet
 * delivery service. Each packet sent or received on a datagram socket
 * is individually addressed and routed. Multiple packets sent from
 * one machine to another may be routed differently, and may arrive in
 * any order.
 *
 * <p> Where possible, a newly constructed {@code DatagramSocket} has the
 * {@link StandardSocketOptions#SO_BROADCAST SO_BROADCAST} socket option enabled so as
 * to allow the transmission of broadcast datagrams. In order to receive
 * broadcast packets a DatagramSocket should be bound to the wildcard address.
 * In some implementations, broadcast packets may also be received when
 * a DatagramSocket is bound to a more specific address.
 * <p>
 * Example:
 * <pre>{@code
 *              DatagramSocket s = new DatagramSocket(null);
 *              s.bind(new InetSocketAddress(8888));
 * }</pre>
 * Which is equivalent to:
 * <pre>{@code
 *              DatagramSocket s = new DatagramSocket(8888);
 * }</pre>
 * Both cases will create a DatagramSocket able to receive broadcasts on
 * UDP port 8888.
 *
 * <p> The {@code DatagramSocket} class defines convenience
 * methods to set and get several socket options. This class also
 * defines the {@link #setOption(SocketOption,Object) setOption}
 * and {@link #getOption(SocketOption) getOption} methods to set
 * and query socket options.
 * A {@code DatagramSocket} supports the following socket options:
 * <blockquote>
 * <a id="SocketOptions"></a>
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
 *     <th scope="row"> {@link java.net.StandardSocketOptions#SO_SNDBUF SO_SNDBUF} </th>
 *     <td> The size of the socket send buffer in bytes </td>
 *   </tr>
 *   <tr>
 *     <th scope="row"> {@link java.net.StandardSocketOptions#SO_RCVBUF SO_RCVBUF} </th>
 *     <td> The size of the socket receive buffer in bytes </td>
 *   </tr>
 *   <tr>
 *     <th scope="row"> {@link java.net.StandardSocketOptions#SO_REUSEADDR SO_REUSEADDR} </th>
 *     <td> Re-use address </td>
 *   </tr>
 *   <tr>
 *     <th scope="row"> {@link java.net.StandardSocketOptions#SO_BROADCAST SO_BROADCAST} </th>
 *     <td> Allow transmission of broadcast datagrams </td>
 *   </tr>
 *   <tr>
 *     <th scope="row"> {@link java.net.StandardSocketOptions#IP_TOS IP_TOS} </th>
 *     <td> The Type of Service (ToS) octet in the Internet Protocol (IP) header </td>
 *   </tr>
 * </tbody>
 * </table>
 * </blockquote>
 * <p> In addition, the {@code DatagramSocket} class defines methods to {@linkplain
 * #joinGroup(SocketAddress, NetworkInterface) join} and {@linkplain
 * #leaveGroup(SocketAddress, NetworkInterface) leave} a multicast group, and
 * supports <a href="DatagramSocket.html#MulticastOptions">multicast options</a> which
 * are useful when {@linkplain #joinGroup(SocketAddress, NetworkInterface) joining},
 * {@linkplain #leaveGroup(SocketAddress, NetworkInterface) leaving}, or sending datagrams
 * to a multicast group.
 * The following multicast options are supported:
 * <blockquote>
 * <a id="MulticastOptions"></a>
 * <table class="striped">
 * <caption style="display:none">Multicast options</caption>
 * <thead>
 *   <tr>
 *     <th scope="col">Option Name</th>
 *     <th scope="col">Description</th>
 *   </tr>
 * </thead>
 * <tbody>
 *   <tr>
 *     <th scope="row"> {@link java.net.StandardSocketOptions#IP_MULTICAST_IF IP_MULTICAST_IF} </th>
 *     <td> The network interface for Internet Protocol (IP) multicast datagrams </td>
 *   </tr>
 *   <tr>
 *     <th scope="row"> {@link java.net.StandardSocketOptions#IP_MULTICAST_TTL
 *       IP_MULTICAST_TTL} </th>
 *     <td> The <em>time-to-live</em> for Internet Protocol (IP) multicast
 *       datagrams </td>
 *   </tr>
 *   <tr>
 *     <th scope="row"> {@link java.net.StandardSocketOptions#IP_MULTICAST_LOOP
 *       IP_MULTICAST_LOOP} </th>
 *     <td> Loopback for Internet Protocol (IP) multicast datagrams </td>
 *   </tr>
 * </tbody>
 * </table>
 * </blockquote>
 * An implementation may also support additional options.
 *
 * @apiNote  <a id="Multicasting"></a><b>Multicasting with DatagramSocket</b>
 *
 * <p> {@link DatagramChannel} implements the {@link MulticastChannel} interface
 * and provides an alternative API for sending and receiving multicast datagrams.
 * The {@link MulticastChannel} API supports both {@linkplain
 * MulticastChannel#join(InetAddress, NetworkInterface) any-source} and
 * {@linkplain MulticastChannel#join(InetAddress, NetworkInterface, InetAddress)
 * source-specific} multicast. Consider using {@code DatagramChannel} for
 * multicasting.
 *
 * <p> {@code DatagramSocket} can be used directly for multicasting. However,
 * contrarily to {@link MulticastSocket}, {@code DatagramSocket} doesn't call the
 * {@link DatagramSocket#setReuseAddress(boolean)} method to enable the SO_REUSEADDR
 * socket option by default. If creating a {@code DatagramSocket} intended to
 * later join a multicast group, the caller should consider explicitly enabling
 * the SO_REUSEADDR option.
 *
 * <p> An instance of {@code DatagramSocket} can be used to send or
 * receive multicast datagram packets. It is not necessary to join a multicast
 * group in order to send multicast datagrams. Before sending out multicast
 * datagram packets however, the default outgoing interface for sending
 * multicast datagram should first be configured using
 * {@link #setOption(SocketOption, Object) setOption} and
 * {@link StandardSocketOptions#IP_MULTICAST_IF}:
 *
 * <pre>{@code
 *    DatagramSocket sender = new DatagramSocket(new InetSocketAddress(0));
 *    NetworkInterface outgoingIf = NetworkInterface.getByName("en0");
 *    sender.setOption(StandardSocketOptions.IP_MULTICAST_IF, outgoingIf);
 *
 *    // optionally configure multicast TTL; the TTL defines the scope of a
 *    // multicast datagram, for example, confining it to host local (0) or
 *    // link local (1) etc...
 *    int ttl = ...; // a number betwen 0 and 255
 *    sender.setOption(StandardSocketOptions.IP_MULTICAST_TTL, ttl);
 *
 *    // send a packet to a multicast group
 *    byte[] msgBytes = ...;
 *    InetAddress mcastaddr = InetAddress.getByName("228.5.6.7");
 *    int port = 6789;
 *    InetSocketAddress dest = new InetSocketAddress(mcastaddr, port);
 *    DatagramPacket hi = new DatagramPacket(msgBytes, msgBytes.length, dest);
 *    sender.send(hi);
 * }</pre>
 *
 * <p> An instance of {@code DatagramSocket} can also be used to receive
 * multicast datagram packets. A {@code DatagramSocket} that is created
 * with the intent of receiving multicast datagrams should be created
 * <i>unbound</i>. Before binding the socket, {@link #setReuseAddress(boolean)
 * setReuseAddress(true)} should be configured:
 *
 * <pre>{@code
 *    DatagramSocket socket = new DatagramSocket(null); // unbound
 *    socket.setReuseAddress(true); // set reuse address before binding
 *    socket.bind(new InetSocketAddress(6789)); // bind
 *
 *    // joinGroup 228.5.6.7
 *    InetAddress mcastaddr = InetAddress.getByName("228.5.6.7");
 *    InetSocketAddress group = new InetSocketAddress(mcastaddr, 0);
 *    NetworkInterface netIf = NetworkInterface.getByName("en0");
 *    socket.joinGroup(group, netIf);
 *    byte[] msgBytes = new byte[1024]; // up to 1024 bytes
 *    DatagramPacket packet = new DatagramPacket(msgBytes, msgBytes.length);
 *    socket.receive(packet);
 *    ....
 *    // eventually leave group
 *    socket.leaveGroup(group, netIf);
 * }</pre>
 *
 * <p><a id="PlatformDependencies"></a><b>Platform dependencies</b>
 * <p>The multicast implementation is intended to map directly to the native
 * multicasting facility. Consequently, the following items should be considered
 * when developing an application that receives IP multicast datagrams:
 * <ol>
 *    <li> Contrarily to {@link DatagramChannel}, the constructors of {@code DatagramSocket}
 *        do not allow to specify the {@link ProtocolFamily} of the underlying socket.
 *        Consequently, the protocol family of the underlying socket may not
 *        correspond to the protocol family of the multicast groups that
 *        the {@code DatagramSocket} will attempt to join.
 *        <br>
 *        There is no guarantee that a {@code DatagramSocket} with an underlying
 *        socket created in one protocol family can join and receive multicast
 *        datagrams when the address of the multicast group corresponds to
 *        another protocol family. For example, it is implementation specific if a
 *        {@code DatagramSocket} to an IPv6 socket can join an IPv4 multicast group
 *        and receive multicast datagrams sent to the group.
 *    </li>
 *    <li> Before joining a multicast group, the {@code DatagramSocket} should be
 *        bound to the wildcard address.
 *        If the socket is bound to a specific address, rather than the wildcard address
 *        then it is implementation specific if multicast datagrams are received
 *        by the socket.
 *    </li>
 *    <li> The SO_REUSEADDR option should be enabled prior to binding the socket.
 *        This is required to allow multiple members of the group to bind to the same address.
 *    </li>
 * </ol>
 *
 * @author  Pavani Diwanji
 * @see     java.net.DatagramPacket
 * @see     java.nio.channels.DatagramChannel
 * @since 1.0
 */
public class DatagramSocket implements java.io.Closeable {

    // An instance of DatagramSocketAdaptor, NetMulticastSocket, or null
    private final DatagramSocket delegate;

    DatagramSocket delegate() {
        if (delegate == null) {
            throw new InternalError("Should not get here");
        }
        return delegate;
    }

    /**
     * All constructors eventually call this one.
     * @param delegate The wrapped DatagramSocket implementation, or null.
     */
    DatagramSocket(DatagramSocket delegate) {
        assert delegate == null
                || delegate instanceof NetMulticastSocket
                || delegate instanceof sun.nio.ch.DatagramSocketAdaptor;
        this.delegate = delegate;
    }

    /**
     * Constructs a datagram socket and binds it to any available port
     * on the local host machine.  The socket will be bound to the
     * {@link InetAddress#isAnyLocalAddress wildcard} address.
     *
     * <p>If there is a security manager,
     * its {@code checkListen} method is first called
     * with 0 as its argument to ensure the operation is allowed.
     * This could result in a SecurityException.
     *
     * @throws     SocketException  if the socket could not be opened,
     *              or the socket could not be bound.
     * @throws     SecurityException  if a security manager exists and its
     *             {@code checkListen} method doesn't allow the operation.
     *
     * @see SecurityManager#checkListen
     */
    public DatagramSocket() throws SocketException {
        this(new InetSocketAddress(0));
    }

    /**
     * Creates an unbound datagram socket with the specified
     * DatagramSocketImpl.
     *
     * @param impl an instance of a <B>DatagramSocketImpl</B>
     *        the subclass wishes to use on the DatagramSocket.
     * @since   1.4
     */
    protected DatagramSocket(DatagramSocketImpl impl) {
        this(new NetMulticastSocket(impl));
    }

    /**
     * Creates a datagram socket, bound to the specified local
     * socket address.
     * <p>
     * If the address is {@code null} an unbound socket will be created.
     *
     * <p>If there is a security manager,
     * its {@code checkListen} method is first called
     * with the port from the socket address
     * as its argument to ensure the operation is allowed.
     * This could result in a SecurityException.
     *
     * @param bindaddr local socket address to bind, or {@code null}
     *                 for an unbound socket.
     *
     * @throws     SocketException  if the socket could not be opened,
     *               or the socket could not bind to the specified local port.
     * @throws     SecurityException  if a security manager exists and its
     *             {@code checkListen} method doesn't allow the operation.
     * @throws     IllegalArgumentException  if bindaddr is a
     *              SocketAddress subclass not supported by this socket.
     *
     * @see SecurityManager#checkListen
     * @since   1.4
     */
    public DatagramSocket(SocketAddress bindaddr) throws SocketException {
        this(createDelegate(bindaddr, DatagramSocket.class));
    }

    /**
     * Constructs a datagram socket and binds it to the specified port
     * on the local host machine.  The socket will be bound to the
     * {@link InetAddress#isAnyLocalAddress wildcard} address.
     *
     * <p>If there is a security manager,
     * its {@code checkListen} method is first called
     * with the {@code port} argument
     * as its argument to ensure the operation is allowed.
     * This could result in a SecurityException.
     *
     * @param      port local port to use in the bind operation.
     * @throws     SocketException  if the socket could not be opened,
     *               or the socket could not bind to the specified local port.
     * @throws     SecurityException  if a security manager exists and its
     *             {@code checkListen} method doesn't allow the operation.
     * @throws     IllegalArgumentException  if port is <a href="#PortRange">
     *              out of range.</a>
     *
     * @see SecurityManager#checkListen
     */
    public DatagramSocket(int port) throws SocketException {
        this(port, null);
    }

    /**
     * Creates a datagram socket, bound to the specified local
     * address.
     * <p><a id="PortRange"></a>The local port must be between 0 and
     * 65535 inclusive. A port number of {@code zero} will let the system pick
     * up an ephemeral port in a {@code bind} operation.
     * <p>
     * If the IP address is a {@link InetAddress#isAnyLocalAddress wildcard}
     * address, or is {@code null}, the socket will be bound to the wildcard
     * address.
     *
     * <p>If there is a security manager,
     * its {@code checkListen} method is first called
     * with the {@code port} argument
     * as its argument to ensure the operation is allowed.
     * This could result in a SecurityException.
     *
     * @param port local port to use in the bind operation.
     * @param laddr local address to bind (can be {@code null})
     *
     * @throws     SocketException  if the socket could not be opened,
     *               or the socket could not bind to the specified local port.
     * @throws     SecurityException  if a security manager exists and its
     *             {@code checkListen} method doesn't allow the operation.
     * @throws     IllegalArgumentException  if port is <a href="#PortRange">
     *              out of range.</a>
     *
     * @see SecurityManager#checkListen
     * @since   1.1
     */
    public DatagramSocket(int port, InetAddress laddr) throws SocketException {
        this(new InetSocketAddress(laddr, port));
    }

    /**
     * Binds this DatagramSocket to a specific address and port.
     * <p>
     * If the address is {@code null}, then the system will pick up
     * an ephemeral port and a valid local address to bind the socket.
     *
     * @param   addr The address and port to bind to.
     * @throws  SocketException if any error happens during the bind, or if the
     *          socket is already bound.
     * @throws  SecurityException  if a security manager exists and its
     *             {@code checkListen} method doesn't allow the operation.
     * @throws IllegalArgumentException if addr is a SocketAddress subclass
     *         not supported by this socket.
     * @since 1.4
     */
    public void bind(SocketAddress addr) throws SocketException {
        delegate().bind(addr);
    }

    /**
     * Connects the socket to a remote address for this socket. When a
     * socket is connected to a remote address, packets may only be
     * sent to or received from that address. By default a datagram
     * socket is not connected. If the socket is already closed,
     * then this method has no effect.
     *
     * <p> If this socket is not bound then this method will first cause the
     * socket to be bound to an address that is assigned automatically,
     * as if invoking the {@link #bind bind} method with a parameter of
     * {@code null}. If the remote destination to which the socket is connected
     * does not exist, or is otherwise unreachable, and if an ICMP destination
     * unreachable packet has been received for that address, then a subsequent
     * call to send or receive may throw a PortUnreachableException. Note,
     * there is no guarantee that the exception will be thrown.
     *
     * <p> If a security manager has been installed then it is invoked to check
     * access to the remote address. Specifically, if the given {@code address}
     * is a {@link InetAddress#isMulticastAddress multicast address},
     * the security manager's {@link
     * java.lang.SecurityManager#checkMulticast(InetAddress)
     * checkMulticast} method is invoked with the given {@code address}.
     * Otherwise, the security manager's {@link
     * java.lang.SecurityManager#checkConnect(String,int) checkConnect}
     * and {@link java.lang.SecurityManager#checkAccept checkAccept} methods
     * are invoked, with the given {@code address} and {@code port}, to
     * verify that datagrams are permitted to be sent and received
     * respectively.
     *
     * <p> Care should be taken to ensure that a connected datagram socket
     * is not shared with untrusted code. When a socket is connected,
     * {@link #receive receive} and {@link #send send} <b>will not perform
     * any security checks</b> on incoming and outgoing packets, other than
     * matching the packet's and the socket's address and port. On a send
     * operation, if the packet's address is set and the packet's address
     * and the socket's address do not match, an {@code IllegalArgumentException}
     * will be thrown. A socket connected to a multicast address may only
     * be used to send packets. Datagrams in the socket's {@linkplain
     * java.net.StandardSocketOptions#SO_RCVBUF socket receive buffer}, which
     * have not been {@linkplain #receive(DatagramPacket) received} before invoking
     * this method, may be discarded.
     *
     * @param address the remote address for the socket
     *
     * @param port the remote port for the socket.
     *
     * @throws IllegalArgumentException
     *         if the address is null, or the port is <a href="#PortRange">
     *         out of range.</a>
     *
     * @throws SecurityException
     *         if a security manager has been installed and it does
     *         not permit access to the given remote address
     *
     * @throws UncheckedIOException
     *         may be thrown if connect fails, for example, if the
     *         destination address is non-routable
     *
     * @see #disconnect
     *
     * @since 1.2
     */
    public void connect(InetAddress address, int port) {
        delegate().connect(address, port);
    }

    /**
     * Connects this socket to a remote socket address (IP address + port number).
     *
     * <p> If given an {@link InetSocketAddress InetSocketAddress}, this method
     * behaves as if invoking {@link #connect(InetAddress,int) connect(InetAddress,int)}
     * with the given socket addresses IP address and port number, except that the
     * {@code SocketException} that may be raised is not wrapped in an
     * {@code UncheckedIOException}. Datagrams in the socket's {@linkplain
     * java.net.StandardSocketOptions#SO_RCVBUF socket receive buffer}, which
     * have not been {@linkplain #receive(DatagramPacket) received} before invoking
     * this method, may be discarded.
     *
     * @param   addr    The remote address.
     *
     * @throws  SocketException
     *          if the connect fails
     *
     * @throws IllegalArgumentException
     *         if {@code addr} is {@code null}, or {@code addr} is a SocketAddress
     *         subclass not supported by this socket
     *
     * @throws SecurityException
     *         if a security manager has been installed and it does
     *         not permit access to the given remote address
     *
     * @since 1.4
     */
    public void connect(SocketAddress addr) throws SocketException {
        delegate().connect(addr);
    }

    /**
     * Disconnects the socket. If the socket is closed or not connected,
     * then this method has no effect.
     *
     * @apiNote If this method throws an UncheckedIOException, the socket
     *          may be left in an unspecified state. It is strongly
     *          recommended that the socket be closed when disconnect
     *          fails.
     *
     * @throws  UncheckedIOException
     *          may be thrown if disconnect fails to dissolve the
     *          association and restore the socket to a consistent state.
     *
     * @see #connect
     *
     * @since 1.2
     */
    public void disconnect() {
        delegate().disconnect();
    }

    /**
     * Returns the binding state of the socket.
     * <p>
     * If the socket was bound prior to being {@link #close closed},
     * then this method will continue to return {@code true}
     * after the socket is closed.
     *
     * @return true if the socket successfully bound to an address
     * @since 1.4
     */
    public boolean isBound() {
        return delegate().isBound();
    }

    /**
     * Returns the connection state of the socket.
     * <p>
     * If the socket was connected prior to being {@link #close closed},
     * then this method will continue to return {@code true}
     * after the socket is closed.
     *
     * @return true if the socket successfully connected to a server
     * @since 1.4
     */
    public boolean isConnected() {
        return delegate().isConnected();
    }

    /**
     * Returns the address to which this socket is connected. Returns
     * {@code null} if the socket is not connected.
     * <p>
     * If the socket was connected prior to being {@link #close closed},
     * then this method will continue to return the connected address
     * after the socket is closed.
     *
     * @return the address to which this socket is connected.
     * @since 1.2
     */
    public InetAddress getInetAddress() {
        return delegate().getInetAddress();
    }

    /**
     * Returns the port number to which this socket is connected.
     * Returns {@code -1} if the socket is not connected.
     * <p>
     * If the socket was connected prior to being {@link #close closed},
     * then this method will continue to return the connected port number
     * after the socket is closed.
     *
     * @return the port number to which this socket is connected.
     * @since 1.2
     */
    public int getPort() {
        return delegate().getPort();
    }

    /**
     * Returns the address of the endpoint this socket is connected to, or
     * {@code null} if it is unconnected.
     * <p>
     * If the socket was connected prior to being {@link #close closed},
     * then this method will continue to return the connected address
     * after the socket is closed.
     *
     * @return a {@code SocketAddress} representing the remote
     *         endpoint of this socket, or {@code null} if it is
     *         not connected yet.
     * @see #getInetAddress()
     * @see #getPort()
     * @see #connect(SocketAddress)
     * @since 1.4
     */
    public SocketAddress getRemoteSocketAddress() {
        return delegate().getRemoteSocketAddress();
    }

    /**
     * Returns the address of the endpoint this socket is bound to.
     *
     * @return a {@code SocketAddress} representing the local endpoint of this
     *         socket, or {@code null} if it is closed or not bound yet.
     * @see #getLocalAddress()
     * @see #getLocalPort()
     * @see #bind(SocketAddress)
     * @since 1.4
     */
    public SocketAddress getLocalSocketAddress() {
        return delegate().getLocalSocketAddress();
    }

    /**
     * Sends a datagram packet from this socket. The
     * {@code DatagramPacket} includes information indicating the
     * data to be sent, its length, the IP address of the remote host,
     * and the port number on the remote host.
     *
     * <p>If there is a security manager, and the socket is not currently
     * connected to a remote address, this method first performs some
     * security checks. First, if {@code p.getAddress().isMulticastAddress()}
     * is true, this method calls the
     * security manager's {@code checkMulticast} method
     * with {@code p.getAddress()} as its argument.
     * If the evaluation of that expression is false,
     * this method instead calls the security manager's
     * {@code checkConnect} method with arguments
     * {@code p.getAddress().getHostAddress()} and
     * {@code p.getPort()}. Each call to a security manager method
     * could result in a SecurityException if the operation is not allowed.
     *
     * @param      p   the {@code DatagramPacket} to be sent.
     *
     * @throws     IOException  if an I/O error occurs.
     * @throws     SecurityException  if a security manager exists and its
     *             {@code checkMulticast} or {@code checkConnect}
     *             method doesn't allow the send.
     * @throws     PortUnreachableException may be thrown if the socket is connected
     *             to a currently unreachable destination. Note, there is no
     *             guarantee that the exception will be thrown.
     * @throws     java.nio.channels.IllegalBlockingModeException
     *             if this socket has an associated channel,
     *             and the channel is in non-blocking mode.
     * @throws     IllegalArgumentException if the socket is connected,
     *             and connected address and packet address differ, or
     *             if the socket is not connected and the packet address
     *             is not set or if its port is <a href="#PortRange">out of
     *             range.</a>
     *
     * @see        java.net.DatagramPacket
     * @see        SecurityManager#checkMulticast(InetAddress)
     * @see        SecurityManager#checkConnect
     * @revised 1.4
     */
    public void send(DatagramPacket p) throws IOException  {
        delegate().send(p);
    }

    /**
     * Receives a datagram packet from this socket. When this method
     * returns, the {@code DatagramPacket}'s buffer is filled with
     * the data received. The datagram packet also contains the sender's
     * IP address, and the port number on the sender's machine.
     * <p>
     * This method blocks until a datagram is received. The
     * {@code length} field of the datagram packet object contains
     * the length of the received message. If the message is longer than
     * the packet's length, the message is truncated.
     * <p>
     * If there is a security manager, and the socket is not currently
     * connected to a remote address, a packet cannot be received if the
     * security manager's {@code checkAccept} method does not allow it.
     * Datagrams that are not permitted by the security manager are silently
     * discarded.
     *
     * @param      p   the {@code DatagramPacket} into which to place
     *                 the incoming data.
     * @throws     IOException  if an I/O error occurs.
     * @throws     SocketTimeoutException  if setSoTimeout was previously called
     *                 and the timeout has expired.
     * @throws     PortUnreachableException may be thrown if the socket is connected
     *             to a currently unreachable destination. Note, there is no guarantee that the
     *             exception will be thrown.
     * @throws     java.nio.channels.IllegalBlockingModeException
     *             if this socket has an associated channel,
     *             and the channel is in non-blocking mode.
     * @see        java.net.DatagramPacket
     * @see        java.net.DatagramSocket
     * @revised 1.4
     */
    public void receive(DatagramPacket p) throws IOException {
        delegate().receive(p);
    }

    /**
     * Gets the local address to which the socket is bound.
     *
     * <p>If there is a security manager, its
     * {@code checkConnect} method is first called
     * with the host address and {@code -1}
     * as its arguments to see if the operation is allowed.
     *
     * @see SecurityManager#checkConnect
     * @return  the local address to which the socket is bound,
     *          {@code null} if the socket is closed, or
     *          an {@code InetAddress} representing
     *          {@link InetAddress#isAnyLocalAddress wildcard}
     *          address if either the socket is not bound, or
     *          the security manager {@code checkConnect}
     *          method does not allow the operation
     * @since   1.1
     */
    public InetAddress getLocalAddress() {
        return delegate().getLocalAddress();
    }

    /**
     * Returns the port number on the local host to which this socket
     * is bound.
     *
     * @return  the port number on the local host to which this socket is bound,
     *          {@code -1} if the socket is closed, or
     *          {@code 0} if it is not bound yet.
     */
    public int getLocalPort() {
        return delegate().getLocalPort();
    }

    /**
     * Enable/disable SO_TIMEOUT with the specified timeout, in
     * milliseconds. With this option set to a positive timeout value,
     * a call to receive() for this DatagramSocket
     * will block for only this amount of time.  If the timeout expires,
     * a <B>java.net.SocketTimeoutException</B> is raised, though the
     * DatagramSocket is still valid. A timeout of zero is interpreted
     * as an infinite timeout.
     * The option <B>must</B> be enabled prior to entering the blocking
     * operation to have effect.
     *
     * @param timeout the specified timeout in milliseconds.
     * @throws SocketException if there is an error in the underlying protocol, such as an UDP error.
     * @throws IllegalArgumentException if {@code timeout} is negative
     * @since   1.1
     * @see #getSoTimeout()
     */
    public void setSoTimeout(int timeout) throws SocketException {
        delegate().setSoTimeout(timeout);
    }

    /**
     * Retrieve setting for SO_TIMEOUT.  0 returns implies that the
     * option is disabled (i.e., timeout of infinity).
     *
     * @return the setting for SO_TIMEOUT
     * @throws SocketException if there is an error in the underlying protocol, such as an UDP error.
     * @since   1.1
     * @see #setSoTimeout(int)
     */
    public int getSoTimeout() throws SocketException {
        return delegate().getSoTimeout();
    }

    /**
     * Sets the SO_SNDBUF option to the specified value for this
     * {@code DatagramSocket}. The SO_SNDBUF option is used by the
     * network implementation as a hint to size the underlying
     * network I/O buffers. The SO_SNDBUF setting may also be used
     * by the network implementation to determine the maximum size
     * of the packet that can be sent on this socket.
     * <p>
     * As SO_SNDBUF is a hint, applications that want to verify
     * what size the buffer is should call {@link #getSendBufferSize()}.
     * <p>
     * Increasing the buffer size may allow multiple outgoing packets
     * to be queued by the network implementation when the send rate
     * is high.
     * <p>
     * Note: If {@link #send(DatagramPacket)} is used to send a
     * {@code DatagramPacket} that is larger than the setting
     * of SO_SNDBUF then it is implementation specific if the
     * packet is sent or discarded.
     *
     * @apiNote
     * If {@code size > 0}, this method is equivalent to calling
     * {@link #setOption(SocketOption, Object)
     * setOption(StandardSocketOptions.SO_SNDBUF, size)}.
     *
     * @param size the size to which to set the send buffer
     * size, in bytes. This value must be greater than 0.
     *
     * @throws    SocketException if there is an error
     * in the underlying protocol, such as an UDP error.
     * @throws    IllegalArgumentException if the value is 0 or is
     * negative.
     * @see #getSendBufferSize()
     * @see StandardSocketOptions#SO_SNDBUF
     * @since 1.2
     */
    public void setSendBufferSize(int size) throws SocketException {
        delegate().setSendBufferSize(size);
    }

    /**
     * Get value of the SO_SNDBUF option for this {@code DatagramSocket}, that is the
     * buffer size, in bytes, used by the platform for output on this {@code DatagramSocket}.
     *
     * @apiNote
     * This method is equivalent to calling {@link #getOption(SocketOption)
     * getOption(StandardSocketOptions.SO_SNDBUF)}.
     *
     * @return the value of the SO_SNDBUF option for this {@code DatagramSocket}
     * @throws    SocketException if there is an error in
     * the underlying protocol, such as an UDP error.
     * @see #setSendBufferSize
     * @see StandardSocketOptions#SO_SNDBUF
     * @since 1.2
     */
    public int getSendBufferSize() throws SocketException {
        return delegate().getSendBufferSize();
    }

    /**
     * Sets the SO_RCVBUF option to the specified value for this
     * {@code DatagramSocket}. The SO_RCVBUF option is used by
     * the network implementation as a hint to size the underlying
     * network I/O buffers. The SO_RCVBUF setting may also be used
     * by the network implementation to determine the maximum size
     * of the packet that can be received on this socket.
     * <p>
     * Because SO_RCVBUF is a hint, applications that want to
     * verify what size the buffers were set to should call
     * {@link #getReceiveBufferSize()}.
     * <p>
     * Increasing SO_RCVBUF may allow the network implementation
     * to buffer multiple packets when packets arrive faster than
     * are being received using {@link #receive(DatagramPacket)}.
     * <p>
     * Note: It is implementation specific if a packet larger
     * than SO_RCVBUF can be received.
     *
     * @apiNote
     * If {@code size > 0}, this method is equivalent to calling
     * {@link #setOption(SocketOption, Object)
     * setOption(StandardSocketOptions.SO_RCVBUF, size)}.
     *
     * @param size the size to which to set the receive buffer
     * size, in bytes. This value must be greater than 0.
     *
     * @throws    SocketException if there is an error in
     * the underlying protocol, such as an UDP error.
     * @throws    IllegalArgumentException if the value is 0 or is
     * negative.
     * @see #getReceiveBufferSize()
     * @see StandardSocketOptions#SO_RCVBUF
     * @since 1.2
     */
    public void setReceiveBufferSize(int size) throws SocketException {
        delegate().setReceiveBufferSize(size);
    }

    /**
     * Get value of the SO_RCVBUF option for this {@code DatagramSocket}, that is the
     * buffer size, in bytes, used by the platform for input on this {@code DatagramSocket}.
     *
     * @apiNote
     * This method is equivalent to calling {@link #getOption(SocketOption)
     * getOption(StandardSocketOptions.SO_RCVBUF)}.
     *
     * @return the value of the SO_RCVBUF option for this {@code DatagramSocket}
     * @throws    SocketException if there is an error in the underlying protocol, such as an UDP error.
     * @see #setReceiveBufferSize(int)
     * @see StandardSocketOptions#SO_RCVBUF
     * @since 1.2
     */
    public int getReceiveBufferSize() throws SocketException {
        return delegate().getReceiveBufferSize();
    }

    /**
     * Enable/disable the SO_REUSEADDR socket option.
     * <p>
     * For UDP sockets it may be necessary to bind more than one
     * socket to the same socket address. This is typically for the
     * purpose of receiving multicast packets
     * (See {@link java.net.MulticastSocket}). The
     * {@code SO_REUSEADDR} socket option allows multiple
     * sockets to be bound to the same socket address if the
     * {@code SO_REUSEADDR} socket option is enabled prior
     * to binding the socket using {@link #bind(SocketAddress)}.
     * <p>
     * Note: This functionality is not supported by all existing platforms,
     * so it is implementation specific whether this option will be ignored
     * or not. However, if it is not supported then
     * {@link #getReuseAddress()} will always return {@code false}.
     * <p>
     * When a {@code DatagramSocket} is created the initial setting
     * of {@code SO_REUSEADDR} is disabled.
     * <p>
     * The behaviour when {@code SO_REUSEADDR} is enabled or
     * disabled after a socket is bound (See {@link #isBound()})
     * is not defined.
     *
     * @apiNote
     * This method is equivalent to calling {@link #setOption(SocketOption, Object)
     * setOption(StandardSocketOptions.SO_REUSEADDR, on)}.
     *
     * @param on  whether to enable or disable the
     * @throws    SocketException if an error occurs enabling or
     *            disabling the {@code SO_REUSEADDR} socket option,
     *            or the socket is closed.
     * @since 1.4
     * @see #getReuseAddress()
     * @see #bind(SocketAddress)
     * @see #isBound()
     * @see #isClosed()
     * @see StandardSocketOptions#SO_REUSEADDR
     */
    public void setReuseAddress(boolean on) throws SocketException {
        delegate().setReuseAddress(on);
    }

    /**
     * Tests if SO_REUSEADDR is enabled.
     *
     * @apiNote
     * This method is equivalent to calling {@link #getOption(SocketOption)
     * getOption(StandardSocketOptions.SO_REUSEADDR)}.
     *
     * @return a {@code boolean} indicating whether or not SO_REUSEADDR is enabled.
     * @throws    SocketException if there is an error
     * in the underlying protocol, such as an UDP error.
     * @since   1.4
     * @see #setReuseAddress(boolean)
     * @see StandardSocketOptions#SO_REUSEADDR
     */
    public boolean getReuseAddress() throws SocketException {
        return delegate().getReuseAddress();
    }

    /**
     * Enable/disable SO_BROADCAST.
     *
     * <p> Some operating systems may require that the Java virtual machine be
     * started with implementation specific privileges to enable this option or
     * send broadcast datagrams.
     *
     * @apiNote
     * This method is equivalent to calling {@link #setOption(SocketOption, Object)
     * setOption(StandardSocketOptions.SO_BROADCAST, on)}.
     *
     * @param  on
     *         whether or not to have broadcast turned on.
     *
     * @throws  SocketException
     *          if there is an error in the underlying protocol, such as an UDP
     *          error.
     *
     * @since 1.4
     * @see #getBroadcast()
     * @see StandardSocketOptions#SO_BROADCAST
     */
    public void setBroadcast(boolean on) throws SocketException {
        delegate().setBroadcast(on);
    }

    /**
     * Tests if SO_BROADCAST is enabled.
     *
     * @apiNote
     * This method is equivalent to calling {@link #getOption(SocketOption)
     * getOption(StandardSocketOptions.SO_BROADCAST)}.
     *
     * @return a {@code boolean} indicating whether or not SO_BROADCAST is enabled.
     * @throws    SocketException if there is an error
     * in the underlying protocol, such as an UDP error.
     * @since 1.4
     * @see #setBroadcast(boolean)
     * @see StandardSocketOptions#SO_BROADCAST
     */
    public boolean getBroadcast() throws SocketException {
        return delegate().getBroadcast();
    }

    /**
     * Sets traffic class or type-of-service octet in the IP
     * datagram header for datagrams sent from this DatagramSocket.
     * As the underlying network implementation may ignore this
     * value applications should consider it a hint.
     *
     * <P> The tc <B>must</B> be in the range {@code 0 <= tc <=
     * 255} or an IllegalArgumentException will be thrown.
     * <p>Notes:
     * <p>For Internet Protocol v4 the value consists of an
     * {@code integer}, the least significant 8 bits of which
     * represent the value of the TOS octet in IP packets sent by
     * the socket.
     * RFC 1349 defines the TOS values as follows:
     *
     * <UL>
     * <LI><CODE>IPTOS_LOWCOST (0x02)</CODE></LI>
     * <LI><CODE>IPTOS_RELIABILITY (0x04)</CODE></LI>
     * <LI><CODE>IPTOS_THROUGHPUT (0x08)</CODE></LI>
     * <LI><CODE>IPTOS_LOWDELAY (0x10)</CODE></LI>
     * </UL>
     * The last low order bit is always ignored as this
     * corresponds to the MBZ (must be zero) bit.
     * <p>
     * Setting bits in the precedence field may result in a
     * SocketException indicating that the operation is not
     * permitted.
     * <p>
     * for Internet Protocol v6 {@code tc} is the value that
     * would be placed into the sin6_flowinfo field of the IP header.
     *
     * @apiNote
     * This method is equivalent to calling {@link #setOption(SocketOption, Object)
     * setOption(StandardSocketOptions.IP_TOS, tc)}.
     *
     * @param tc        an {@code int} value for the bitset.
     * @throws SocketException if there is an error setting the
     * traffic class or type-of-service
     * @since 1.4
     * @see #getTrafficClass
     * @see StandardSocketOptions#IP_TOS
     */
    public void setTrafficClass(int tc) throws SocketException {
        delegate().setTrafficClass(tc);
    }

    /**
     * Gets traffic class or type-of-service in the IP datagram
     * header for packets sent from this DatagramSocket.
     * <p>
     * As the underlying network implementation may ignore the
     * traffic class or type-of-service set using {@link #setTrafficClass(int)}
     * this method may return a different value than was previously
     * set using the {@link #setTrafficClass(int)} method on this
     * DatagramSocket.
     *
     * @apiNote
     * This method is equivalent to calling {@link #getOption(SocketOption)
     * getOption(StandardSocketOptions.IP_TOS)}.
     *
     * @return the traffic class or type-of-service already set
     * @throws SocketException if there is an error obtaining the
     * traffic class or type-of-service value.
     * @since 1.4
     * @see #setTrafficClass(int)
     * @see StandardSocketOptions#IP_TOS
     */
    public int getTrafficClass() throws SocketException {
        return delegate().getTrafficClass();
    }

    /**
     * Closes this datagram socket.
     * <p>
     * Any thread currently blocked in {@link #receive} upon this socket
     * will throw a {@link SocketException}.
     *
     * <p> If this socket has an associated channel then the channel is closed
     * as well.
     *
     * @revised 1.4
     */
    public void close() {
        delegate().close();
    }

    /**
     * Returns whether the socket is closed or not.
     *
     * @return true if the socket has been closed
     * @since 1.4
     */
    public boolean isClosed() {
        return delegate().isClosed();
    }

    /**
     * Returns the unique {@link java.nio.channels.DatagramChannel} object
     * associated with this datagram socket, if any.
     *
     * <p> A datagram socket will have a channel if, and only if, the channel
     * itself was created via the {@link java.nio.channels.DatagramChannel#open
     * DatagramChannel.open} method.
     *
     * @return  the datagram channel associated with this datagram socket,
     *          or {@code null} if this socket was not created for a channel
     *
     * @since 1.4
     */
    public DatagramChannel getChannel() {
        return null;
    }

    /**
     * User defined factory for all datagram sockets.
     */
    private static volatile DatagramSocketImplFactory factory;

    /**
     * Sets the datagram socket implementation factory for the
     * application. The factory can be specified only once.
     * <p>
     * When an application creates a new datagram socket, the socket
     * implementation factory's {@code createDatagramSocketImpl} method is
     * called to create the actual datagram socket implementation.
     * <p>
     * Passing {@code null} to the method is a no-op unless the factory
     * was already set.
     *
     * <p>If there is a security manager, this method first calls
     * the security manager's {@code checkSetFactory} method
     * to ensure the operation is allowed.
     * This could result in a SecurityException.
     *
     * @param      fac   the desired factory.
     * @throws     IOException  if an I/O error occurs when setting the
     *              datagram socket factory.
     * @throws     SocketException  if the factory is already defined.
     * @throws     SecurityException  if a security manager exists and its
     *             {@code checkSetFactory} method doesn't allow the operation.
     * @see       java.net.DatagramSocketImplFactory#createDatagramSocketImpl()
     * @see       SecurityManager#checkSetFactory
     * @since 1.3
     *
     * @deprecated Use {@link DatagramChannel}, or subclass {@code DatagramSocket}
     *    directly.
     *    <br> This method provided a way in early JDK releases to replace the
     *    system wide implementation of {@code DatagramSocket}. It has been mostly
     *    obsolete since Java 1.4. If required, a {@code DatagramSocket} can be
     *    created to use a custom implementation by extending {@code DatagramSocket}
     *    and using the {@linkplain #DatagramSocket(DatagramSocketImpl) protected
     *    constructor} that takes an {@linkplain DatagramSocketImpl implementation}
     *    as a parameter.
     */
    @Deprecated(since = "17")
    public static synchronized void
    setDatagramSocketImplFactory(DatagramSocketImplFactory fac)
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
     * Sets the value of a socket option.
     *
     * @param <T> The type of the socket option value
     * @param name The socket option
     * @param value The value of the socket option. A value of {@code null}
     *              may be valid for some options.
     *
     * @return this DatagramSocket
     *
     * @throws UnsupportedOperationException if the datagram socket
     *         does not support the option.
     *
     * @throws IllegalArgumentException if the value is not valid for
     *         the option.
     *
     * @throws IOException if an I/O error occurs, or if the socket is closed.
     *
     * @throws SecurityException if a security manager is set and if the socket
     *         option requires a security permission and if the caller does
     *         not have the required permission.
     *         {@link java.net.StandardSocketOptions StandardSocketOptions}
     *         do not require any security permission.
     *
     * @throws NullPointerException if name is {@code null}
     *
     * @since 9
     */
    public <T> DatagramSocket setOption(SocketOption<T> name, T value)
        throws IOException
    {
        delegate().setOption(name, value);
        return this;
    }

    /**
     * Returns the value of a socket option.
     *
     * @param <T> The type of the socket option value
     * @param name The socket option
     *
     * @return The value of the socket option.
     *
     * @throws UnsupportedOperationException if the datagram socket
     *         does not support the option.
     *
     * @throws IOException if an I/O error occurs, or if the socket is closed.
     *
     * @throws NullPointerException if name is {@code null}
     *
     * @throws SecurityException if a security manager is set and if the socket
     *         option requires a security permission and if the caller does
     *         not have the required permission.
     *         {@link java.net.StandardSocketOptions StandardSocketOptions}
     *         do not require any security permission.
     *
     * @since 9
     */
    public <T> T getOption(SocketOption<T> name) throws IOException {
        return delegate().getOption(name);
    }

    /**
     * Returns a set of the socket options supported by this socket.
     *
     * This method will continue to return the set of options even after
     * the socket has been closed.
     *
     * @return A set of the socket options supported by this socket. This set
     *        may be empty if the socket's DatagramSocketImpl cannot be created.
     *
     * @since 9
     */
    public Set<SocketOption<?>> supportedOptions() {
        return delegate().supportedOptions();
    }

    /**
     * Joins a multicast group.
     *
     * <p> In order to join a multicast group, the caller should specify
     * the IP address of the multicast group to join, and the local
     * {@linkplain NetworkInterface network interface} to receive multicast
     * packets from.
     * <ul>
     *  <li> The {@code mcastaddr} argument indicates the IP address
     *   of the multicast group to join. For historical reasons this is
     *   specified as a {@code SocketAddress}.
     *   The default implementation only supports {@link InetSocketAddress} and
     *   the {@link InetSocketAddress#getPort() port} information is ignored.
     *  </li>
     *  <li> The {@code netIf} argument specifies the local interface to receive
     *       multicast datagram packets, or {@code null} to defer to the interface
     *       set for outgoing multicast datagrams.
     *       If {@code null}, and no interface has been set, the behaviour is
     *       unspecified: any interface may be selected or the operation may fail
     *       with a {@code SocketException}.
     *  </li>
     * </ul>
     *
     * <p> It is possible to call this method several times to join
     * several different multicast groups, or join the same group
     * in several different networks. However, if the socket is already a
     * member of the group, an {@link IOException} will be thrown.
     *
     * <p>If there is a security manager, this method first
     * calls its {@code checkMulticast} method with the {@code mcastaddr}
     * argument as its argument.
     *
     * @apiNote The default interface for sending outgoing multicast datagrams
     * can be configured with {@link #setOption(SocketOption, Object)}
     * with {@link StandardSocketOptions#IP_MULTICAST_IF}.
     *
     * @param  mcastaddr indicates the multicast address to join.
     * @param  netIf specifies the local interface to receive multicast
     *         datagram packets, or {@code null}.
     * @throws IOException if there is an error joining, or when the address
     *         is not a multicast address, or the platform does not support
     *         multicasting
     * @throws SecurityException if a security manager exists and its
     *         {@code checkMulticast} method doesn't allow the join.
     * @throws IllegalArgumentException if mcastaddr is {@code null} or is a
     *         SocketAddress subclass not supported by this socket
     * @see    SecurityManager#checkMulticast(InetAddress)
     * @see    DatagramChannel#join(InetAddress, NetworkInterface)
     * @see    StandardSocketOptions#IP_MULTICAST_IF
     * @since  17
     */
    public void joinGroup(SocketAddress mcastaddr, NetworkInterface netIf)
            throws IOException {
        delegate().joinGroup(mcastaddr, netIf);
    }

    /**
     * Leave a multicast group on a specified local interface.
     *
     * <p>If there is a security manager, this method first
     * calls its {@code checkMulticast} method with the
     * {@code mcastaddr} argument as its argument.
     *
     * @apiNote
     * The {@code mcastaddr} and {@code netIf} arguments should identify
     * a multicast group that was previously {@linkplain
     * #joinGroup(SocketAddress, NetworkInterface) joined} by
     * this {@code DatagramSocket}.
     * <p> It is possible to call this method several times to leave
     * multiple different multicast groups previously joined, or leave
     * the same group previously joined in multiple different networks.
     * However, if the socket is not a member of the specified group
     * in the specified network, an {@link IOException} will be
     * thrown.
     *
     * @param  mcastaddr is the multicast address to leave. This should
     *         contain the same IP address than that used for {@linkplain
     *         #joinGroup(SocketAddress, NetworkInterface) joining}
     *         the group.
     * @param  netIf specifies the local interface or {@code null} to defer
     *         to the interface set for outgoing multicast datagrams.
     *         If {@code null}, and no interface has been set, the behaviour
     *         is unspecified: any interface may be selected or the operation
     *         may fail with a {@code SocketException}.
     * @throws IOException if there is an error leaving or when the address
     *         is not a multicast address.
     * @throws SecurityException if a security manager exists and its
     *         {@code checkMulticast} method doesn't allow the operation.
     * @throws IllegalArgumentException if mcastaddr is {@code null} or is a
     *         SocketAddress subclass not supported by this socket.
     * @see    SecurityManager#checkMulticast(InetAddress)
     * @see    #joinGroup(SocketAddress, NetworkInterface)
     * @see    StandardSocketOptions#IP_MULTICAST_IF
     * @since  17
     */
    public void leaveGroup(SocketAddress mcastaddr, NetworkInterface netIf)
            throws IOException {
        delegate().leaveGroup(mcastaddr, netIf);
    }

    // Temporary solution until JDK-8237352 is addressed
    private static final SocketAddress NO_DELEGATE = new SocketAddress() {};

    /**
     * Best effort to convert an {@link IOException}
     * into a {@link SocketException}.
     *
     * @param e an instance of {@link IOException}
     * @return an instance of {@link SocketException}
     */
    private static SocketException toSocketException(IOException e) {
        if (e instanceof SocketException)
            return (SocketException) e;
        Throwable cause = e.getCause();
        if (cause instanceof SocketException)
            return (SocketException) cause;
        SocketException se = new SocketException(e.getMessage());
        se.initCause(e);
        return se;
    }

    /**
     * Creates a delegate for the specific requested {@code type}. This method should
     * only be called by {@code DatagramSocket} and {@code MulticastSocket}
     * public constructors.
     *
     * @param bindaddr An address to bind to, or {@code null} if creating an unbound
     *                 socket.
     * @param type     This is either {@code MulticastSocket.class}, if the delegate needs
     *                 to support joining multicast groups, or {@code DatagramSocket.class},
     *                 if it doesn't. Typically, this will be {@code DatagramSocket.class}
     *                 when creating a delegate for {@code DatagramSocket}, and
     *                 {@code MulticastSocket.class} when creating a delegate for
     *                 {@code MulticastSocket}.
     * @param <T>      The target type for which the delegate is created.
     *                 This is either {@code java.net.DatagramSocket} or
     *                 {@code java.net.MulticastSocket}.
     * @return {@code null} if {@code bindaddr == NO_DELEGATE}, otherwise returns a
     * delegate for the requested {@code type}.
     * @throws SocketException if an exception occurs while creating or binding the
     *                         the delegate.
     */
    static <T extends DatagramSocket> T createDelegate(SocketAddress bindaddr, Class<T> type)
            throws SocketException {

        // Temporary solution until JDK-8237352 is addressed
        if (bindaddr == NO_DELEGATE) return null;

        assert type == DatagramSocket.class || type == MulticastSocket.class;
        boolean multicast = (type == MulticastSocket.class);
        DatagramSocket delegate = null;
        boolean initialized = false;
        try {
            DatagramSocketImplFactory factory = DatagramSocket.factory;
            if (factory != null) {
                // create legacy DatagramSocket delegate
                DatagramSocketImpl impl = factory.createDatagramSocketImpl();
                Objects.requireNonNull(impl,
                        "Implementation returned by installed DatagramSocketImplFactory is null");
                delegate = new NetMulticastSocket(impl);
                ((NetMulticastSocket) delegate).getImpl(); // ensure impl.create() is called.
            } else {
                // create NIO adaptor
                delegate = DefaultSelectorProvider.get()
                        .openUninterruptibleDatagramChannel()
                        .socket();
            }

            if (multicast) {
                // set reuseaddress if multicasting
                // (must be set before binding)
                delegate.setReuseAddress(true);
            }

            if (bindaddr != null) {
                // bind if needed
                delegate.bind(bindaddr);
            }

            // enable broadcast if possible
            try {
                delegate.setBroadcast(true);
            } catch (IOException ioe) {
            }

            initialized = true;
        } catch (IOException ioe) {
            throw toSocketException(ioe);
        } finally {
            // make sure the delegate is closed if anything
            // went wrong
            if (!initialized && delegate != null) {
                delegate.close();
            }
        }
        @SuppressWarnings("unchecked")
        T result = (T) delegate;
        return result;
    }

}
