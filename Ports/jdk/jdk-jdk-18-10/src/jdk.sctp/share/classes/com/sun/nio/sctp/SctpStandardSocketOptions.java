/*
 * Copyright (c) 2009, 2020, Oracle and/or its affiliates. All rights reserved.
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
import sun.nio.ch.sctp.SctpStdSocketOption;

/**
 * SCTP channels supports the socket options defined by this class
 * (as well as those listed in the particular channel class) and may support
 * additional Implementation specific socket options.
 *
 * @since 1.7
 */
public class SctpStandardSocketOptions {
    private SctpStandardSocketOptions() {}
    /**
     * Enables or disables message fragmentation.
     *
     * <P> The value of this socket option is a {@code Boolean} that represents
     * whether the option is enabled or disabled. If enabled no SCTP message
     * fragmentation will be performed. Instead if a message being sent
     * exceeds the current PMTU size, the message will NOT be sent and
     * an error will be indicated to the user.
     *
     * <P> It is implementation specific whether or not this option is
     * supported.
     */
    public static final SctpSocketOption<Boolean> SCTP_DISABLE_FRAGMENTS = new
        SctpStdSocketOption<Boolean>("SCTP_DISABLE_FRAGMENTS", Boolean.class,
        sun.nio.ch.sctp.SctpStdSocketOption.SCTP_DISABLE_FRAGMENTS);

    /**
     * Enables or disables explicit message completion.
     *
     * <p> The value of this socket option is a {@code Boolean} that represents
     * whether the option is enabled or disabled. When this option is enabled,
     * the {@code send} method may be invoked multiple times to a send message.
     * The {@code isComplete} parameter of the {@link MessageInfo} must only
     * be set to {@code true} for the final send to indicate that the message is
     * complete. If this option is disabled then each individual {@code send}
     * invocation is considered complete.
     *
     * <P> The default value of the option is {@code false} indicating that the
     * option is disabled. It is implementation specific whether or not this
     * option is supported.
     */
    public static final SctpSocketOption<Boolean> SCTP_EXPLICIT_COMPLETE = new
        SctpStdSocketOption<Boolean>("SCTP_EXPLICIT_COMPLETE", Boolean.class,
        sun.nio.ch.sctp.SctpStdSocketOption.SCTP_EXPLICIT_COMPLETE);

    /**
     * Fragmented interleave controls how the presentation of messages occur
     * for the message receiver. There are three levels of fragment interleave
     * defined. Two of the levels effect {@link SctpChannel}, while
     * {@link SctpMultiChannel} is effected by all three levels.
     *
     * <P> This option takes an {@code Integer} value. It can be set to a value
     * of {@code 0}, {@code 1} or {@code 2}.
     *
     * <P> Setting the three levels provides the following receiver
     * interactions:
     *
     * <P> {@code level 0} - Prevents the interleaving of any messages. This
     * means that when a partial delivery begins, no other messages will be
     * received except the message being partially delivered. If another message
     * arrives on a different stream (or association) that could be delivered,
     * it will be blocked waiting for the user to read all of the partially
     * delivered message.
     *
     * <P> {@code level 1} - Allows interleaving of messages that are from
     * different associations. For {@code SctpChannel}, level 0 and
     * level 1 have the same meaning since an {@code SctpChannel} always
     * receives messages from the same association. Note that setting an {@code
     * SctpMultiChannel} to this level may cause multiple partial
     * delivers from different associations but for any given association, only
     * one message will be delivered until all parts of a message have been
     * delivered. This means that one large message, being read with an
     * association identification of "X", will block other messages from
     * association "X" from being delivered.
     *
     * <P> {@code level 2} - Allows complete interleaving of messages. This
     * level requires that the sender carefully observe not only the peer
     * {@code Association} but also must pay careful attention to the stream
     * number. With this option enabled a partially delivered message may begin
     * being delivered for association "X" stream "Y" and the next subsequent
     * receive may return a message from association "X" stream "Z". Note that
     * no other messages would be delivered for association "X" stream "Y"
     * until all of stream "Y"'s partially delivered message was read.
     * Note that this option effects both channel types.  Also note that
     * for an {@code SctpMultiChannel} not only may another streams
     * message from the same association be delivered from the next receive,
     * some other associations message may be delivered upon the next receive.
     *
     * <P> It is implementation specific whether or not this option is
     * supported.
     */
    public static final SctpSocketOption<Integer> SCTP_FRAGMENT_INTERLEAVE =
            new SctpStdSocketOption<Integer>("SCTP_FRAGMENT_INTERLEAVE",
                  Integer.class,
                  sun.nio.ch.sctp.SctpStdSocketOption.SCTP_FRAGMENT_INTERLEAVE);

    /**
     * The maximum number of streams requested by the local endpoint during
     * association initialization.
     *
     * <P> The value of this socket option is an {@link
     * SctpStandardSocketOptions.InitMaxStreams InitMaxStreams}, that represents
     * the maximum number of inbound and outbound streams that an association
     * on the channel is prepared to support.
     *
     * <P> For an {@link SctpChannel} this option may only be used to
     * change the number of inbound/outbound streams prior to connecting.
     *
     * <P> For an {@link SctpMultiChannel} this option determines
     * the maximum number of inbound/outbound streams new associations setup
     * on the channel will be prepared to support.
     *
     * <P> For an {@link SctpServerChannel} this option determines the
     * maximum number of inbound/outbound streams accepted sockets will
     * negotiate with their connecting peer.
     *
     * <P> In all cases the value set by this option is used in the negotiation
     * of new associations setup on the channel's socket and the actual
     * maximum number of inbound/outbound streams that have been negotiated
     * with the peer can be retrieved from the appropriate {@link
     * Association}. The {@code Association} can be retrieved from the
     * {@link AssociationChangeNotification.AssocChangeEvent#COMM_UP COMM_UP}
     * {@link AssociationChangeNotification} belonging to that association.
     *
     * <p> This value is bounded by the actual implementation. In other
     * words the user may be able to support more streams than the Operating
     * System. In such a case, the Operating System limit may override the
     * value requested by the user. The default value of 0 indicates to use
     * the endpoints default value.
     */
    public static final SctpSocketOption
        <SctpStandardSocketOptions.InitMaxStreams> SCTP_INIT_MAXSTREAMS =
        new SctpStdSocketOption<SctpStandardSocketOptions.InitMaxStreams>(
        "SCTP_INIT_MAXSTREAMS", SctpStandardSocketOptions.InitMaxStreams.class);

    /**
     * Enables or disables a Nagle-like algorithm.
     *
     * <P> The value of this socket option is a {@code Boolean} that represents
     * whether the option is enabled or disabled. SCTP uses an algorithm like
     * <em>The Nagle Algorithm</em> to coalesce short segments and
     * improve network efficiency.
     */
    public static final SctpSocketOption<Boolean> SCTP_NODELAY =
        new SctpStdSocketOption<Boolean>("SCTP_NODELAY", Boolean.class,
        sun.nio.ch.sctp.SctpStdSocketOption.SCTP_NODELAY);

    /**
     * Requests that the local SCTP stack use the given peer address as
     * the association primary.
     *
     * <P> The value of this socket option is a {@code SocketAddress}
     * that represents the peer address that the local SCTP stack should use as
     * the association primary. The address must be one of the association
     * peer's addresses.
     *
     * <P> An {@code SctpMultiChannel} can control more than one
     * association, the association parameter must be given when setting or
     * retrieving this option.
     *
     * <P> Since {@code SctpChannel} only controls one association,
     * the association parameter is not required and this option can be
     * set or queried directly.
     */
    public static final SctpSocketOption<SocketAddress> SCTP_PRIMARY_ADDR =
            new SctpStdSocketOption<SocketAddress>
            ("SCTP_PRIMARY_ADDR", SocketAddress.class);

    /**
     * Requests that the peer mark the enclosed address as the association
     * primary.
     *
     * <P> The value of this socket option is a {@code SocketAddress}
     * that represents the local address that the peer should use as its
     * primary address. The given address must be one of the association's
     * locally bound addresses.
     *
     * <P> An {@code SctpMultiChannel} can control more than one
     * association, the association parameter must be given when setting or
     * retrieving this option.
     *
     * <P> Since {@code SctpChannel} only controls one association,
     * the association parameter is not required and this option can be
     * queried directly.
     *
     * <P> Note, this is a set only option and cannot be retrieved by {@code
     * getOption}. It is implementation specific whether or not this
     * option is supported.
     */
    public static final SctpSocketOption<SocketAddress> SCTP_SET_PEER_PRIMARY_ADDR =
            new SctpStdSocketOption<SocketAddress>
            ("SCTP_SET_PEER_PRIMARY_ADDR", SocketAddress.class);

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
     * <p> For {@code SctpChannel}, this controls the amount of data
     * the SCTP stack may have waiting in internal buffers to be sent. This
     * option therefore bounds the maximum size of data that can be sent in a
     * single send call.
     *
     * <P> For {@code SctpMultiChannel}, the effect is the same as for {@code
     * SctpChannel}, except that it applies to all associations. The option
     * applies to each association's window size separately.
     *
     * <p> An implementation allows this socket option to be set before the
     * socket is bound or connected. Whether an implementation allows the
     * socket send buffer to be changed after the socket is bound is system
     * dependent.
     */
    public static final SctpSocketOption<Integer> SO_SNDBUF =
        new SctpStdSocketOption<Integer>("SO_SNDBUF", Integer.class,
        sun.nio.ch.sctp.SctpStdSocketOption.SO_SNDBUF);

    /**
     * The size of the socket receive buffer.
     *
     * <P> The value of this socket option is an {@code Integer} that is the
     * size of the socket receive buffer in bytes. The socket receive buffer is
     * an input buffer used by the networking implementation. It may need to be
     * increased for high-volume connections or decreased to limit the possible
     * backlog of incoming data. The value of the socket option is a
     * <em>hint</em> to the implementation to size the buffer and the actual
     * size may differ.
     *
     * <P> For {@code SctpChannel}, this controls the receiver window size.
     *
     * <P> For {@code SctpMultiChannel}, the meaning is implementation
     * dependent. It might control the receive buffer for each association bound
     * to the socket descriptor or it might control the receive buffer for the
     * whole socket.
     *
     * <p> An implementation allows this socket option to be set before the
     * socket is bound or connected. Whether an implementation allows the
     * socket receive buffer to be changed after the socket is bound is system
     * dependent.
     */
    public static final SctpSocketOption<Integer> SO_RCVBUF =
        new SctpStdSocketOption<Integer>("SO_RCVBUF", Integer.class,
        sun.nio.ch.sctp.SctpStdSocketOption.SO_RCVBUF);

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
     */
    public static final SctpSocketOption<Integer> SO_LINGER =
        new SctpStdSocketOption<Integer>("SO_LINGER", Integer.class,
        sun.nio.ch.sctp.SctpStdSocketOption.SO_LINGER);

    /**
     * This class is used to set the maximum number of inbound/outbound streams
     * used by the local endpoint during association initialization. An
     * instance of this class is used to set the {@link
     * SctpStandardSocketOptions#SCTP_INIT_MAXSTREAMS SCTP_INIT_MAXSTREAMS}
     * socket option.
     *
     * @since 1.7
     */
    public static class InitMaxStreams {
        private int maxInStreams;
        private int maxOutStreams;

        private InitMaxStreams(int maxInStreams, int maxOutStreams) {
           this.maxInStreams = maxInStreams;
           this.maxOutStreams = maxOutStreams;
        }

        /**
         * Creates an InitMaxStreams instance.
         *
         * @param  maxInStreams
         *         The maximum number of inbound streams, where
         *         {@code 0 <= maxInStreams <= 65536}
         *
         * @param  maxOutStreams
         *         The maximum number of outbound streams, where
         *         {@code 0 <= maxOutStreams <= 65536}
         *
         * @return  An {@code InitMaxStreams} instance
         *
         * @throws  IllegalArgumentException
         *          If an argument is outside of specified bounds
         */
        public static InitMaxStreams create
              (int maxInStreams, int maxOutStreams) {
            if (maxOutStreams < 0 || maxOutStreams > 65535)
                throw new IllegalArgumentException(
                      "Invalid maxOutStreams value");
            if (maxInStreams < 0 || maxInStreams > 65535)
                throw new IllegalArgumentException(
                      "Invalid maxInStreams value");

            return new InitMaxStreams(maxInStreams, maxOutStreams);
        }

        /**
         * Returns the maximum number of inbound streams.
         *
         * @return  Maximum inbound streams
         */
        public int maxInStreams() {
            return maxInStreams;
        }

        /**
         * Returns the maximum number of outbound streams.
         *
         * @return  Maximum outbound streams
         */
        public int maxOutStreams() {
            return maxOutStreams;
        }

        /**
         * Returns a string representation of this init max streams, including
         * the maximum in and out bound streams.
         *
         * @return  A string representation of this init max streams
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(super.toString()).append(" [");
            sb.append("maxInStreams:").append(maxInStreams);
            sb.append("maxOutStreams:").append(maxOutStreams).append("]");
            return sb.toString();
        }

        /**
         * Returns true if the specified object is another {@code InitMaxStreams}
         * instance with the same number of in and out bound streams.
         *
         * @param  obj
         *         The object to be compared with this init max streams
         *
         * @return  true if the specified object is another
         *          {@code InitMaxStreams} instance with the same number of in
         *          and out bound streams
         */
        @Override
        public boolean equals(Object obj) {
            if (obj != null && obj instanceof InitMaxStreams) {
                InitMaxStreams that = (InitMaxStreams) obj;
                if (this.maxInStreams == that.maxInStreams &&
                    this.maxOutStreams == that.maxOutStreams)
                    return true;
            }
            return false;
        }

        /**
         * Returns a hash code value for this init max streams.
         */
        @Override
        public int hashCode() {
            int hash = 7 ^ maxInStreams ^ maxOutStreams;
            return hash;
        }
    }
}
