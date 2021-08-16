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

import com.sun.jdi.connect.Transport;
import com.sun.jdi.connect.TransportTimeoutException;

/**
 * A transport service for connections between a debugger and
 * a target VM.
 *
 * <p> A transport service is a concrete subclass of this class
 * that has a zero-argument constructor and implements the abstract
 * methods specified below. It is the underlying service
 * used by a {@link Transport} for connections between a debugger
 * and a target VM.
 *
 * <p> A transport service is used to establish a connection
 * between a debugger and a target VM, and to transport Java
 * Debug Wire Protocol (JDWP) packets over an underlying
 * communication protocol. In essence a transport service
 * implementation binds JDWP (as specified in the
 * <a href="{@docRoot}/../specs/jdwp/jdwp-spec.html">
 * JDWP specification</a>) to an underlying communication
 * protocol. A transport service implementation provides
 * a reliable JDWP packet transportation service. JDWP
 * packets are sent to and from the target VM without duplication
 * or data loss. A transport service implementation may be
 * based on an underlying communication protocol that is
 * reliable or unreliable. If the underlying communication
 * protocol is reliable then the transport service implementation
 * may be relatively simple and may only need to transport JDWP
 * packets as payloads of the underlying communication
 * protocol. In the case of an unreliable communication
 * protocol the transport service implementation may include
 * additional protocol support in order to ensure that packets
 * are not duplicated and that there is no data loss. The
 * details of such protocols are specific to the implementation
 * but may involve techniques such as the <i>positive
 * acknowledgment with retransmission</i> technique used in
 * protocols such as the Transmission Control Protocol (TCP)
 * (see <a href="http://www.ietf.org/rfc/rfc0793.txt"> RFC 793
 * </a>).
 *
 * <p> A transport service can be used to initiate a connection
 * to a target VM. This is done by invoking the {@link #attach}
 * method. Alternatively, a transport service can listen and
 * accept connections initiated by a target VM. This is done
 * by invoking the {@link #startListening(String)} method to
 * put the transport into listen mode. Then the {@link #accept}
 * method is used to accept a connection initiated by a
 * target VM.
 *
 * @since 1.5
 */
public abstract class TransportService {
    /**
     * Constructor for subclasses to call.
     */
    public TransportService() {}

    /**
     * Returns a name to identify the transport service.
     *
     * @return  The name of the transport service
     */
    public abstract String name();

    /**
     * Returns a description of the transport service.
     *
     * @return  The description of the transport service
     */
    public abstract String description();

    /**
     * The transport service capabilities.
     */
    public static abstract class Capabilities {
        /**
         * Constructor for subclasses to call.
         */
        public Capabilities() {}

        /**
         * Tells whether or not this transport service can support
         * multiple concurrent connections to a single address that
         * it is listening on.
         *
         * @return  {@code true} if, and only if, this transport
         *          service supports multiple connections.
         */
        public abstract boolean supportsMultipleConnections();

        /**
         * Tell whether or not this transport service supports a timeout
         * when attaching to a target VM.
         *
         * @return      {@code true} if, and only if, this transport
         *              service supports attaching with a timeout.
         *
         * @see #attach(String,long,long)
         */
        public abstract boolean supportsAttachTimeout();

        /**
         * Tell whether or not this transport service supports a
         * timeout while waiting for a target VM to connect.
         *
         * @return  {@code true} if, and only if, this transport
         *          service supports timeout while waiting for
         *          a target VM to connect.
         *
         * @see #accept(TransportService.ListenKey,long,long)
         */
        public abstract boolean supportsAcceptTimeout();

        /**
         * Tells whether or not this transport service supports a
         * timeout when handshaking with the target VM.
         *
         * @return  {@code true} if, and only if, this transport
         *          service supports a timeout while handshaking
         *          with the target VM.
         *
         * @see #attach(String,long,long)
         * @see #accept(TransportService.ListenKey,long,long)
         */
        public abstract boolean supportsHandshakeTimeout();
    }

    /**
     * Returns the capabilities of the transport service.
     *
     * @return  the transport service capabilities
     */
    public abstract Capabilities capabilities();

    /**
     * Attaches to the specified address.
     *
     * <p> Attaches to the specified address and returns a connection
     * representing the bi-directional communication channel to the
     * target VM.
     *
     * <p> Attaching to the target VM involves two steps:
     * First, a connection is established to specified address. This
     * is followed by a handshake to ensure that the connection is
     * to a target VM. The handshake involves the exchange
     * of a string <i>JDWP-Handshake</i> as specified in the <a
     * href="{@docRoot}/../specs/jdwp/jdwp-spec.html">
     * Java Debug Wire Protocol</a> specification.
     *
     * @param   address
     *          The address of the target VM.
     *
     * @param   attachTimeout
     *          If this transport service supports an attach timeout,
     *          and if {@code attachTimeout} is positive, then it specifies
     *          the timeout, in milliseconds (more or less), to use
     *          when attaching to the target VM.  If the transport service
     *          does not support an attach timeout, or if {@code attachTimeout}
     *          is specified as zero then attach without any timeout.
     *
     * @param   handshakeTimeout
     *          If this transport service supports a handshake timeout,
     *          and if {@code handshakeTimeout} is positive, then it
     *          specifies the timeout, in milliseconds (more or less), to
     *          use when handshaking with the target VM. The exact
     *          usage of the timeout are specific to the transport service.
     *          A transport service may, for example, use the handshake
     *          timeout as the inter-character timeout while waiting for
     *          the <i>JDWP-Handshake</i> message from the target VM.
     *          Alternatively, a transport service may, for example,
     *          use the handshakeTimeout as a timeout for the duration of the
     *          handshake exchange.
     *          If the transport service does not support a handshake
     *          timeout, or if {@code handshakeTimeout} is specified
     *          as zero then the handshake does not timeout if there
     *          isn't a response from the target VM.
     *
     * @return  The Connection representing the bi-directional
     *          communication channel to the target VM.
     *
     * @throws  TransportTimeoutException
     *          If a timeout occurs while establishing the connection.
     *
     * @throws  IOException
     *          If an I/O error occurs (including a timeout when
     *          handshaking).
     *
     * @throws  IllegalArgumentException
     *          If the address is invalid or the value of the
     *          attach timeout or handshake timeout is negative.
     *
     * @see TransportService.Capabilities#supportsAttachTimeout()
     */
    public abstract Connection attach(String address, long attachTimeout,
        long handshakeTimeout) throws IOException;

    /**
     * A <i>listen key</i>.
     *
     * <p> A {@code TransportService} may listen on multiple, yet
     * different, addresses at the same time. To uniquely identify
     * each {@code listener} a listen key is created each time that
     * {@link #startListening startListening} is called. The listen
     * key is used in calls to the {@link #accept accept} method
     * to accept inbound connections to that listener. A listen
     * key is valid until it is used as an argument to {@link
     * #stopListening stopListening} to stop the transport
     * service from listening on an address.
     */
    public static abstract class ListenKey {
        /**
         * Constructor for subclasses to call.
         */
        public ListenKey() {}

        /**
         * Returns a string representation of the listen key.
         */
        public abstract String address();
    }

    /**
     * Listens on the specified address for inbound connections.
     *
     * <p> This method starts the transport service listening on
     * the specified address so that it can subsequently accept
     * an inbound connection. It does not wait until an inbound
     * connection is established.
     *
     * @param   address
     *          The address to start listening for connections,
     *          or {@code null} to listen on an address chosen
     *          by the transport service.
     *
     * @return  a listen key to be used in subsequent calls to be
     *          {@link #accept accept} or {@link #stopListening
     *          stopListening} methods.
     *
     * @throws  IOException
     *          If an I/O error occurs.
     *
     * @throws  IllegalArgumentException
     *          If the specific address is invalid
     */
    public abstract ListenKey startListening(String address) throws IOException;

    /**
     * Listens on an address chosen by the transport service.
     *
     * <p> This convenience method works as if by invoking
     * {@link #startListening(String) startListening(null)}.
     *
     * @return  a listen key to be used in subsequent calls to be
     *          {@link #accept accept} or {@link #stopListening
     *          stopListening} methods.
     *
     * @throws  IOException
     *          If an I/O error occurs.
     */
    public abstract ListenKey startListening() throws IOException;

    /**
     * Stop listening for inbound connections.
     *
     * <p> Invoking this method while another thread is blocked
     * in {@link #accept accept}, with the same listen key,
     * waiting to accept a connection will cause that thread to
     * throw an IOException. If the thread blocked in accept
     * has already accepted a connection from a target VM and
     * is in the process of handshaking with the target VM then
     * invoking this method will not cause the thread to throw
     * an exception.
     *
     * @param   listenKey
     *          The listen key obtained from a previous call to {@link
     *          #startListening(String)} or {@link #startListening()}.
     *
     * @throws  IllegalArgumentException
     *          If the listen key is invalid
     *
     * @throws  IOException
     *          If an I/O error occurs.
     */
    public abstract void stopListening(ListenKey listenKey) throws IOException;

    /**
     * Accept a connection from a target VM.
     *
     * <p> Waits (indefinitely or with timeout) to accept a connection
     * from a target VM. Returns a connection representing the
     * bi-directional communication channel to the target VM.
     *
     * <p> Accepting a connection from a target VM involves two
     * steps. First, the transport service waits to accept
     * the connection from the target VM. Once the connection is
     * established a handshake is performed to ensure that the
     * connection is indeed to a target VM. The handshake involves
     * the exchange of a string <i>JDWP-Handshake</i> as specified
     * in the <a
     * href="{@docRoot}/../specs/jdwp/jdwp-spec.html">
     * Java Debug Wire Protocol</a> specification.
     *
     * @param   listenKey
     *          A listen key obtained from a previous call to {@link
     *          #startListening(String)} or {@link #startListening()}.
     *
     * @param   acceptTimeout
     *          if this transport service supports an accept timeout, and
     *          if {@code acceptTimeout} is positive then block for up to
     *          {@code acceptTimeout} milliseconds, more or less, while waiting
     *          for the target VM to connect.
     *          If the transport service does not support an accept timeout
     *          or if {@code acceptTimeout} is zero then block indefinitely
     *          for a target VM to connect.
     *
     * @param   handshakeTimeout
     *          If this transport service supports a handshake timeout,
     *          and if {@code handshakeTimeout} is positive, then it
     *          specifies the timeout, in milliseconds (more or less), to
     *          use when handshaking with the target VM. The exact
     *          usage of the timeout is specific to the transport service.
     *          A transport service may, for example, use the handshake
     *          timeout as the inter-character timeout while waiting for
     *          the <i>JDWP-Handshake</i> message from the target VM.
     *          Alternatively, a transport service may, for example,
     *          use the timeout as a timeout for the duration of the
     *          handshake exchange.
     *          If the transport service does not support a handshake
     *          timeout, of if {@code handshakeTimeout} is specified
     *          as zero then the handshake does not timeout if there
     *          isn't a response from the target VM.
     *
     * @return  The Connection representing the bi-directional
     *          communication channel to the target VM.
     *
     * @throws  TransportTimeoutException
     *          If a timeout occurs while waiting for a target VM
     *          to connect.
     *
     * @throws  IOException
     *          If an I/O error occurs (including a timeout when
     *          handshaking).
     *
     * @throws  IllegalArgumentException
     *          If the value of the acceptTimeout argument, or
     *          handshakeTimeout is negative, or an invalid listen key
     *          is provided.
     *
     * @throws  IllegalStateException
     *          If {@link #stopListening stopListening} has already been
     *          called with this listen key and the transport service
     *          is no longer listening for inbound connections.
     *
     * @see TransportService.Capabilities#supportsAcceptTimeout()
     */
    public abstract Connection accept(ListenKey listenKey, long acceptTimeout,
        long handshakeTimeout) throws IOException;
}
