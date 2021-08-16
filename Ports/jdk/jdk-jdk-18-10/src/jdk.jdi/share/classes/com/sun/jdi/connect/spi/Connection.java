/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jdi.connect.spi;

import java.io.IOException;

/**
 * A connection between a debugger and a target VM which it debugs.
 *
 * <p> A Connection represents a bi-directional communication channel
 * between a debugger and a target VM. A Connection is created when
 * {@link TransportService TransportService} establishes a connection
 * and successfully handshakes with a target VM. A TransportService
 * implementation provides a reliable JDWP packet transportation service
 * and consequently a Connection provides a reliable flow of JDWP packets
 * between the debugger and the target VM. A Connection is stream oriented,
 * that is, the JDWP packets written to a connection are read by the target VM
 * in the order in which they were written. Similarly packets written
 * to a Connection by the target VM are read by the debugger in the
 * order in which they were written.
 *
 * <p> A connection is either open or closed. It is open upon creation,
 * and remains open until it is closed. Once closed, it remains closed,
 * and any attempt to invoke an I/O operation upon it will cause a
 * {@link ClosedConnectionException} to be thrown. A connection can
 * be tested by invoking the {@link #isOpen isOpen} method.
 *
 * <p> A Connection is safe for access by multiple concurrent threads,
 * although at most one thread may be reading and at most one thread may
 * be writing at any given time.
 *
 * @since 1.5
 */
public abstract class Connection {
    /**
     * Constructor for subclasses to call.
     */
    public Connection() {}

    /**
     * Reads a packet from the target VM.
     *
     * <p> Attempts to read a JDWP packet from the target VM.
     * A read operation may block indefinitely and only returns
     * when it reads all bytes of a packet, or in the case of a
     * transport service that is based on a stream-oriented
     * communication protocol, the end of stream is encountered.
     *
     * <p> Reading a packet does not do any integrity checking on
     * the packet aside from a check that the length of the packet
     * (as indicated by the value of the {@code length} field, the
     * first four bytes of the packet) is 11 or more bytes.
     * If the value of the {@code length} value is less then 11
     * then an {@code IOException} is thrown.
     *
     * <p> Returns a byte array of a length equal to the length
     * of the received packet, or a byte array of length 0 when an
     * end of stream is encountered. If end of stream is encountered
     * after some, but not all bytes of a packet, are read then it
     * is considered an I/O error and an {@code IOException} is
     * thrown. The first byte of the packet is stored in element
     * {@code 0} of the byte array, the second in element {@code 1},
     * and so on. The bytes in the byte array are laid out as per the
     * <a href="{@docRoot}/../specs/jdwp/jdwp-spec.html">
     * JDWP specification</a>. That is, all fields in the packet
     * are in big endian order as per the JDWP specification.
     *
     * <p> This method may be invoked at any time.  If another thread has
     * already initiated a {@link #readPacket readPacket} on this
     * connection then the invocation of this method will block until the
     * first operation is complete.
     *
     * @return  the packet read from the target VM
     *
     * @throws  ClosedConnectionException
     *          If the connection is closed, or another thread closes
     *          the connection while the readPacket is in progress.
     *
     * @throws  java.io.IOException
     *          If the length of the packet (as indictaed by the first
     *          4 bytes) is less than 11 bytes, or an I/O error occurs.
     *
     *
     */
    public abstract byte[] readPacket() throws IOException;

    /**
     * Writes a packet to the target VM.
     *
     * <p> Attempts to write, or send, a JDWP packet to the target VM.
     * A write operation only returns after writing the entire packet
     * to the target VM. Writing the entire packet does not mean
     * the entire packet has been transmitted to the target VM
     * but rather that all bytes have been written to the
     * transport service. A transport service based on a TCP/IP connection
     * may, for example, buffer some or all of the packet before
     * transmission on the network.
     *
     * <p> The byte array provided to this method should be laid out
     * as per the <a
     * href="{@docRoot}/../specs/jdwp/jdwp-spec.html">
     * JDWP specification</a>. That is, all fields in the packet
     * are in big endian order. The first byte, that is element
     * {@code pkt[0]}, is the first byte of the {@code length} field.
     * {@code pkt[1]} is the second byte of the {@code length} field,
     * and so on.
     *
     * <p> Writing a packet does not do any integrity checking on
     * the packet aside from checking the packet length. Checking
     * the packet length requires checking that the value of the
     * {@code length} field (as indicated by the first four bytes
     * of the packet) is 11 or greater. Consequently the length of
     * the byte array provided to this method, that is
     * {@code pkt.length}, must be 11 or more, and must be equal
     * or greater than the value of the {@code length} field. If the
     * length of the byte array is greater than the value of
     * the {@code length} field then all bytes from element
     * {@code pkt[length]} onwards are ignored. In other words,
     * any additional bytes that follow the packet in the byte
     * array are ignored and will not be transmitted to the target
     * VM.
     *
     * <p> A write operation may block or may complete immediately.
     * The exact circumstances when an operation blocks depends on
     * the transport service. In the case of a TCP/IP connection to
     * the target VM, the writePacket method may block if there is
     * network congestion or there is insufficient space to buffer
     * the packet in the underlying network system.
     *
     * <p> This method may be invoked at any time.  If another thread has
     * already initiated a write operation upon this Connection then
     * a subsequent invocation of this method will block until the first
     * operation is complete.
     *
     * @param   pkt
     *          The packet to write to the target VM.
     *
     * @throws  ClosedConnectionException
     *          If the connection is closed, or another thread closes
     *          the connection while the write operation is in progress.
     *
     * @throws  java.io.IOException
     *          If an I/O error occurs.
     *
     * @throws  IllegalArgumentException
     *          If the value of the {@code length} field is invalid,
     *          or the byte array is of insufficient length.
     */
    public abstract void writePacket(byte pkt[]) throws IOException;

    /**
     * Closes this connection.
     *
     * <p> If the connection is already closed then invoking this method
     * has no effect. After a connection is closed, any further attempt
     * calls to {@link #readPacket readPacket} or {@link #writePacket
     * writePacket} will throw a {@link ClosedConnectionException}.
     *
     * <p> Any thread currently blocked in an I/O operation ({@link
     * #readPacket readPacket} or {@link #writePacket writePacket})
     * will throw a {@link ClosedConnectionException}).
     *
     * <p> This method may be invoked at any time.  If some other thread has
     * already invoked it, however, then another invocation will block until
     * the first invocation is complete, after which it will return without
     * effect.
     *
     * @throws  java.io.IOException
     *          If an I/O error occurs
     */
    public abstract void close() throws IOException;

    /**
     * Tells whether or not this connection is open.
     *
     * @return {@code true} if and only if this connection is open
     */
    public abstract boolean isOpen();
}
