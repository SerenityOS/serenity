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

/**
 * This class represents a datagram packet.
 * <p>
 * Datagram packets are used to implement a connectionless packet
 * delivery service. Each message is routed from one machine to
 * another based solely on information contained within that packet.
 * Multiple packets sent from one machine to another might be routed
 * differently, and might arrive in any order. Packet delivery is
 * not guaranteed.
 *
 * <p>
 * Unless otherwise specified, passing a {@code null} argument causes
 * a {@link NullPointerException NullPointerException} to be thrown.
 *
 * <p>
 * Methods and constructors of {@code DatagramPacket} accept parameters
 * of type {@link SocketAddress}. {@code DatagramPacket} supports
 * {@link InetSocketAddress}, and may support additional {@code SocketAddress}
 * sub-types.
 *
 * @author  Pavani Diwanji
 * @author  Benjamin Renaud
 * @since   1.0
 */
public final
class DatagramPacket {

    /*
     * The fields of this class are package-private since DatagramSocketImpl
     * classes needs to access them.
     */
    byte[] buf;
    int offset;
    int length;
    int bufLength;
    InetAddress address;
    int port;

    /**
     * Constructs a {@code DatagramPacket} for receiving packets of
     * length {@code length}, specifying an offset into the buffer.
     * <p>
     * The {@code length} argument must be less than or equal to
     * {@code buf.length}.
     *
     * @param   buf     buffer for holding the incoming datagram.
     * @param   offset  the offset for the buffer
     * @param   length  the number of bytes to read.
     *
     * @throws  IllegalArgumentException    if the length or offset
     *          is negative, or if the length plus the offset is
     *          greater than the length of the packet's given buffer.
     *
     * @since   1.2
     */
    public DatagramPacket(byte buf[], int offset, int length) {
        setData(buf, offset, length);
    }

    /**
     * Constructs a {@code DatagramPacket} for receiving packets of
     * length {@code length}.
     * <p>
     * The {@code length} argument must be less than or equal to
     * {@code buf.length}.
     *
     * @param   buf     buffer for holding the incoming datagram.
     * @param   length  the number of bytes to read.
     *
     * @throws  IllegalArgumentException    if the length is negative
     *          or if the length is greater than the length of the
     *          packet's given buffer.
     */
    public DatagramPacket(byte buf[], int length) {
        this (buf, 0, length);
    }

    /**
     * Constructs a datagram packet for sending packets of length
     * {@code length} with offset {@code offset} to the
     * specified port number on the specified host. The
     * {@code length} argument must be less than or equal to
     * {@code buf.length}.
     *
     * @param   buf     the packet data.
     * @param   offset  the packet data offset.
     * @param   length  the packet data length.
     * @param   address the destination address, or {@code null}.
     * @param   port    the destination port number.
     *
     * @throws  IllegalArgumentException    if the length or offset
     *          is negative, or if the length plus the offset is
     *          greater than the length of the packet's given buffer,
     *          or if the port is out of range.
     *
     * @see     java.net.InetAddress
     *
     * @since   1.2
     */
    public DatagramPacket(byte buf[], int offset, int length,
                          InetAddress address, int port) {
        setData(buf, offset, length);
        setAddress(address);
        setPort(port);
    }

    /**
     * Constructs a datagram packet for sending packets of length
     * {@code length} with offset {@code offset} to the
     * specified port number on the specified host. The
     * {@code length} argument must be less than or equal to
     * {@code buf.length}.
     *
     * @param   buf     the packet data.
     * @param   offset  the packet data offset.
     * @param   length  the packet data length.
     * @param   address the destination socket address.
     *
     * @throws  IllegalArgumentException    if address is null or its
     *          type is not supported, or if the length or offset is
     *          negative, or if the length plus the offset is greater
     *          than the length of the packet's given buffer.
     *
     * @see     java.net.InetAddress
     *
     * @since   1.4
     */
    public DatagramPacket(byte buf[], int offset, int length, SocketAddress address) {
        setData(buf, offset, length);
        setSocketAddress(address);
    }

    /**
     * Constructs a datagram packet for sending packets of length
     * {@code length} to the specified port number on the specified
     * host. The {@code length} argument must be less than or equal
     * to {@code buf.length}.
     *
     * @param   buf     the packet data.
     * @param   length  the packet length.
     * @param   address the destination address, or {@code null}.
     * @param   port    the destination port number.
     *
     * @throws  IllegalArgumentException    if the length is negative,
     *          or if the length is greater than the length of the
     *          packet's given buffer, or if the port is out of range.
     *
     * @see     java.net.InetAddress
     */
    public DatagramPacket(byte buf[], int length,
                          InetAddress address, int port) {
        this(buf, 0, length, address, port);
    }

    /**
     * Constructs a datagram packet for sending packets of length
     * {@code length} to the specified port number on the specified
     * host. The {@code length} argument must be less than or equal
     * to {@code buf.length}.
     *
     * @param   buf     the packet data.
     * @param   length  the packet length.
     * @param   address the destination address.
     *
     * @throws  IllegalArgumentException    if address is null or its type is
     *          not supported, or if the length is negative, or if the length
     *          is greater than the length of the packet's given buffer, or
     *          if the port is out of range.
     *
     * @see     java.net.InetAddress
     *
     * @since   1.4
     */
    public DatagramPacket(byte buf[], int length, SocketAddress address) {
        this(buf, 0, length, address);
    }

    /**
     * Returns the IP address of the machine to which this datagram is being
     * sent or from which the datagram was received, or {@code null} if not
     * set.
     *
     * @return  the IP address of the machine to which this datagram is being
     *          sent or from which the datagram was received.
     * @see     java.net.InetAddress
     * @see #setAddress(java.net.InetAddress)
     */
    public synchronized InetAddress getAddress() {
        return address;
    }

    /**
     * Returns the port number on the remote host to which this datagram is
     * being sent or from which the datagram was received, or 0 if not set.
     *
     * @return  the port number on the remote host to which this datagram is
     *          being sent or from which the datagram was received.
     * @see #setPort(int)
     */
    public synchronized int getPort() {
        return port;
    }

    /**
     * Returns the data buffer. The data received or the data to be sent
     * starts from the {@code offset} in the buffer,
     * and runs for {@code length} long.
     *
     * @return  the buffer used to receive or  send data
     * @see #setData(byte[], int, int)
     */
    public synchronized byte[] getData() {
        return buf;
    }

    /**
     * Returns the offset of the data to be sent or the offset of the
     * data received.
     *
     * @return  the offset of the data to be sent or the offset of the
     *          data received.
     *
     * @since 1.2
     */
    public synchronized int getOffset() {
        return offset;
    }

    /**
     * Returns the length of the data to be sent or the length of the
     * data received.
     *
     * @return  the length of the data to be sent or the length of the
     *          data received.
     * @see #setLength(int)
     */
    public synchronized int getLength() {
        return length;
    }

    /**
     * Set the data buffer for this packet. This sets the
     * data, length and offset of the packet.
     *
     * @param   buf     the buffer to set for this packet
     * @param   offset  the offset into the data
     * @param   length  the length of the data
     *          and/or the length of the buffer used to receive data
     *
     * @throws  IllegalArgumentException    if the length or offset
     *          is negative, or if the length plus the offset is
     *          greater than the length of the packet's given buffer.
     *
     * @see     #getData
     * @see     #getOffset
     * @see     #getLength
     *
     * @since   1.2
     */
    public synchronized void setData(byte[] buf, int offset, int length) {
        /* this will check to see if buf is null */
        if (length < 0 || offset < 0 ||
            (length + offset) < 0 ||
            ((length + offset) > buf.length)) {
            throw new IllegalArgumentException("illegal length or offset");
        }
        this.buf = buf;
        this.length = length;
        this.bufLength = length;
        this.offset = offset;
    }

    /**
     * Sets the IP address of the machine to which this datagram
     * is being sent.
     *
     * @param   iaddr   the {@code InetAddress}, or {@code null}.
     *
     * @see     #getAddress()
     *
     * @since   1.1
     */
    public synchronized void setAddress(InetAddress iaddr) {
        address = iaddr;
    }

    /**
     * Sets the port number on the remote host to which this datagram
     * is being sent.
     *
     * @param   iport   the port number
     *
     * @throws  IllegalArgumentException    if the port is out of range
     *
     * @see     #getPort()
     *
     * @since   1.1
     */
    public synchronized void setPort(int iport) {
        if (iport < 0 || iport > 0xFFFF) {
            throw new IllegalArgumentException("Port out of range:"+ iport);
        }
        port = iport;
    }

    /**
     * Sets the SocketAddress (usually IP address + port number) of the remote
     * host to which this datagram is being sent.
     *
     * @param   address the {@code SocketAddress}
     *
     * @throws  IllegalArgumentException    if address is null or is a
     *          SocketAddress subclass not supported.
     *
     * @see     #getSocketAddress
     *
     * @since   1.4
     */
    public synchronized void setSocketAddress(SocketAddress address) {
        if (!(address instanceof InetSocketAddress addr))
            throw new IllegalArgumentException("unsupported address type");
        if (addr.isUnresolved())
            throw new IllegalArgumentException("unresolved address");
        setAddress(addr.getAddress());
        setPort(addr.getPort());
    }

    /**
     * Returns the {@link InetSocketAddress#InetSocketAddress(InetAddress, int)
     * SocketAddress} (usually {@linkplain #getAddress() IP address} +
     * {@linkplain #getPort() port number}) of the remote host that this packet
     * is being sent to or is coming from.
     *
     * @return  the {@code SocketAddress}
     *
     * @see     #setSocketAddress
     *
     * @since   1.4
     */
    public synchronized SocketAddress getSocketAddress() {
        return new InetSocketAddress(getAddress(), getPort());
    }

    /**
     * Set the data buffer for this packet. With the offset of
     * this DatagramPacket set to 0, and the length set to
     * the length of {@code buf}.
     *
     * @param   buf     the buffer to set for this packet.
     *
     * @see     #getLength
     * @see     #getData
     *
     * @since   1.1
     */
    public synchronized void setData(byte[] buf) {
        if (buf == null) {
            throw new NullPointerException("null packet buffer");
        }
        this.buf = buf;
        this.offset = 0;
        this.length = buf.length;
        this.bufLength = buf.length;
    }

    /**
     * Set the length for this packet. The length of the packet is
     * the number of bytes from the packet's data buffer that will be
     * sent, or the number of bytes of the packet's data buffer that
     * will be used for receiving data. The length must be lesser or
     * equal to the offset plus the length of the packet's buffer.
     *
     * @param   length      the length to set for this packet.
     *
     * @throws  IllegalArgumentException    if the length is negative,
     *          or if the length plus the offset is greater than the
     *          length of the packet's data buffer.
     *
     * @see     #getLength
     * @see     #setData
     *
     * @since   1.1
     */
    public synchronized void setLength(int length) {
        if ((length + offset) > buf.length || length < 0 ||
            (length + offset) < 0) {
            throw new IllegalArgumentException("illegal length");
        }
        this.length = length;
        this.bufLength = this.length;
    }
}
