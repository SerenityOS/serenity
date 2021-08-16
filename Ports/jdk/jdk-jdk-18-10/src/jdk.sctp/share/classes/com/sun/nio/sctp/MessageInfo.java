/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.nio.sctp;

import java.net.SocketAddress;

/**
 * The {@code MessageInfo} class provides additional ancillary information about
 * messages.
 *
 * <P> Received SCTP messages, returned by
 * {@link SctpChannel#receive SctpChannel.receive} and {@link
 * SctpMultiChannel#receive SctpMultiChannel.receive},
 * return a {@code MessageInfo} instance that can be queried to determine
 * ancillary information about the received message. Messages being sent should
 * use one of the {@link #createOutgoing(java.net.SocketAddress,int)
 * createOutgoing} methods to provide ancillary data for the message being
 * sent, and may use the appropriate setter methods to override the default
 * values provided for {@link #isUnordered() unordered}, {@link #timeToLive()
 * timeToLive}, {@link #isComplete() complete} and {@link #payloadProtocolID()
 * payloadProtocolID}, before sending the message.
 *
 * <P> For out going messages the {@code timeToLive} parameter is a time period
 * that the sending side SCTP stack may expire the message if it has not been
 * sent. This time period is an indication to the stack that the message is no
 * longer required to be sent after the time period expires. It is not a hard
 * timeout and may be influenced by whether the association supports the partial
 * reliability extension, <a href=http://www.ietf.org/rfc/rfc3758.txt>RFC 3758
 * </a>.
 *
 * <P> {@code MessageInfo} instances are not safe for use by multiple concurrent
 * threads. If a MessageInfo is to be used by more than one thread then access
 * to the MessageInfo should be controlled by appropriate synchronization.
 *
 * @since 1.7
 */
public abstract class MessageInfo {
    /**
     * Initializes a new instance of this class.
     */
    protected MessageInfo() {}

    /**
     * Creates a {@code MessageInfo} instance suitable for use when
     * sending a message.
     *
     * <P> The returned instance will have its {@link #isUnordered() unordered}
     * value set to {@code false}, its {@link #timeToLive() timeToLive} value
     * set to {@code 0}, its {@link #isComplete() complete} value set
     * to {@code true}, and its {@link #payloadProtocolID() payloadProtocolID}
     * value set to {@code 0}. These values, if required, can be set through
     * the appropriate setter method before sending the message.
     *
     * @param  address
     *         For a connected {@code SctpChannel} the address is the
     *         preferred peer address of the association to send the message
     *         to, or {@code null} to use the peer primary address. For an
     *         {@code SctpMultiChannel} the address is used to determine
     *         the association, or if no association exists with a peer of that
     *         address then one is setup.
     *
     * @param  streamNumber
     *         The stream number that the message will be sent on
     *
     * @return  The outgoing message info
     *
     * @throws  IllegalArgumentException
     *          If the streamNumber is negative or greater than {@code 65536}
     */
    public static MessageInfo createOutgoing(SocketAddress address,
                                             int streamNumber) {
        if (streamNumber < 0 || streamNumber > 65536)
            throw new IllegalArgumentException("Invalid stream number");

        return new sun.nio.ch.sctp.MessageInfoImpl(null, address, streamNumber);
    }
    /**
     * Creates a {@code MessageInfo} instance suitable for use when
     * sending a message to a given association. Typically used for
     * {@code SctpMultiChannel} when an association has already been setup.
     *
     * <P> The returned instance will have its {@link #isUnordered() unordered}
     * value set to {@code false}, its {@link #timeToLive() timeToLive} value
     * set to {@code 0}, its {@link #isComplete() complete} value set
     * to {@code true}, and its {@link #payloadProtocolID() payloadProtocolID}
     * value set to {@code 0}. These values, if required, can be set through
     * the appropriate setter method before sending the message.
     *
     * @param  association
     *         The association to send the message on
     *
     * @param  address
     *         The preferred peer address of the association to send the message
     *         to, or {@code null} to use the peer primary address
     *
     * @param  streamNumber
     *         The stream number that the message will be sent on.
     *
     * @return  The outgoing message info
     *
     * @throws  IllegalArgumentException
     *          If {@code association} is {@code null}, or the streamNumber is
     *          negative or greater than {@code 65536}
     */
    public static MessageInfo createOutgoing(Association association,
                                             SocketAddress address,
                                             int streamNumber) {
        if (association == null)
            throw new IllegalArgumentException("association cannot be null");

        if (streamNumber < 0 || streamNumber > 65536)
            throw new IllegalArgumentException("Invalid stream number");

        return new sun.nio.ch.sctp.MessageInfoImpl(association,
                                                   address, streamNumber);
    }

    /**
     * Returns the source socket address if the message has been received,
     * otherwise the preferred destination of the message to be sent.
     *
     * @return  The socket address, or {@code null} if this instance is to be
     *          used for sending a message and has been construced without
     *          specifying a preferred destination address
     *
     */
    public abstract SocketAddress address();

    /**
     * Returns the association that the message was received on, if the message
     * has been received, otherwise the association that the message is to be
     * sent on.
     *
     * @return The association, or {@code null} if this instance is to be
     *         used for sending a message and has been construced using the
     *         the {@link #createOutgoing(SocketAddress,int)
     *         createOutgoing(SocketAddress,int)} static factory method
     */
    public abstract Association association();

    /**
     * Returns the number of bytes read for the received message.
     *
     * <P> This method is only appicable for received messages, it has no
     * meaning for messages being sent.
     *
     * @return  The number of bytes read, {@code -1} if the channel is an {@link
     *          SctpChannel} that has reached end-of-stream, otherwise
     *          {@code 0}
     */
    public abstract int bytes();

    /**
     * Tells whether or not the message is complete.
     *
     * <P> For received messages {@code true} indicates that the message was
     * completely received. For messages being sent {@code true} indicates that
     * the message is complete, {@code false} indicates that the message is not
     * complete. How the send channel interprets this value depends on the value
     * of its {@link SctpStandardSocketOptions#SCTP_EXPLICIT_COMPLETE
     * SCTP_EXPLICIT_COMPLETE} socket option.
     *
     * @return  {@code true} if, and only if, the message is complete
     */
    public abstract boolean isComplete();

    /**
     * Sets whether or not the message is complete.
     *
     * <P> For messages being sent {@code true} indicates that
     * the message is complete, {@code false} indicates that the message is not
     * complete. How the send channel interprets this value depends on the value
     * of its {@link SctpStandardSocketOptions#SCTP_EXPLICIT_COMPLETE
     * SCTP_EXPLICIT_COMPLETE} socket option.
     *
     * @param  complete
     *         {@code true} if, and only if, the message is complete
     *
     * @return  This MessageInfo
     *
     * @see  MessageInfo#isComplete()
     */
    public abstract MessageInfo complete(boolean complete);

    /**
     * Tells whether or not the message is unordered. For received messages
     * {@code true} indicates that the message was sent non-ordered. For
     * messages being sent {@code true} requests the un-ordered delivery of the
     * message, {@code false} indicates that the message is ordered.
     *
     * @return  {@code true} if the message is unordered, otherwise
     *          {@code false}
     */
    public abstract boolean isUnordered();

    /**
     * Sets whether or not the message is unordered.
     *
     * @param  unordered
     *         {@code true} requests the un-ordered delivery of the message,
     *         {@code false} indicates that the message is ordered.
     *
     * @return  This MessageInfo
     *
     * @see  MessageInfo#isUnordered()
     */
    public abstract MessageInfo unordered(boolean unordered);

    /**
     * Returns the payload protocol Identifier.
     *
     * <P> A value indicating the type of payload protocol data being
     * transmitted/received. This value is passed as opaque data by SCTP.
     * {@code 0} indicates an unspecified payload protocol identifier.
     *
     * @return  The Payload Protocol Identifier
     */
    public abstract int payloadProtocolID();

    /**
     * Sets the payload protocol Identifier.
     *
     * <P> A value indicating the type of payload protocol data being
     * transmitted. This value is passed as opaque data by SCTP.
     *
     * @param  ppid
     *         The Payload Protocol Identifier, or {@code 0} indicate an
     *         unspecified payload protocol identifier.
     *
     * @return  This MessageInfo
     *
     * @see  MessageInfo#payloadProtocolID()
     */
    public abstract MessageInfo payloadProtocolID(int ppid);

    /**
     * Returns the stream number that the message was received on, if the
     * message has been received, otherwise the stream number that the message
     * is to be sent on.
     *
     * @return  The stream number
     */
    public abstract int streamNumber();

    /**
     * Sets the stream number that the message is to be sent on.
     *
     * @param  streamNumber
     *         The stream number
     *
     * @throws  IllegalArgumentException
     *          If the streamNumber is negative or greater than {@code 65536}
     *
     * @return  This MessageInfo
     */
    public abstract MessageInfo streamNumber(int streamNumber);

    /**
     * The time period that the sending side may expire the message if it has
     * not been sent, or {@code 0} to indicate that no timeout should occur. This
     * value is only applicable for messages being sent, it has no meaning for
     * received messages.
     *
     * @return  The time period in milliseconds, or {@code 0}
     */
    public abstract long timeToLive();

    /**
     * Sets the time period that the sending side may expire the message if it
     * has not been sent.
     *
     * @param  millis
     *         The time period in milliseconds, or {@code 0} to indicate that no
     *         timeout should occur
     *
     * @return  This MessageInfo
     *
     * @see MessageInfo#timeToLive()
     */
    public abstract MessageInfo timeToLive(long millis);
}
