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
import java.net.InetAddress;
import java.io.IOException;
import java.util.Set;
import java.nio.ByteBuffer;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetBoundException;
import java.nio.channels.SelectionKey;

/**
 * A selectable channel for message-oriented SCTP sockets.
 *
 * <P> An SCTP multi channel supports many associations on a single socket.
 * An {@code SctpMultiChannel} is created by invoking the
 * {@link #open open} method of this class. A newly-created channel is open but
 * not yet bound. An attempt to invoke the {@link #receive receive} method of an
 * unbound channel will cause the {@link NotYetBoundException}
 * to be thrown. An attempt to invoke the {@link #send send} method of an
 * unbound channel will cause it to first invoke the {@link #bind bind} method.
 * The address(es) that the channel's socket is bound to can be retrieved by
 * calling {@link #getAllLocalAddresses getAllLocalAddresses}.
 *
 * <P> Messages may be sent and received without explicitly setting up an
 * association with the remote peer. The channel will implicitly setup
 * a new association whenever it sends or receives a message from a remote
 * peer if there is not already an association with that peer. Upon successful
 * association setup, an {@link AssociationChangeNotification
 * association changed} notification will be put to the SCTP stack with its
 * {@code event} parameter set to {@link
 * AssociationChangeNotification.AssocChangeEvent#COMM_UP
 * COMM_UP}. This notification can be received by invoking {@link #receive
 * receive}.
 *
 * <P> Socket options are configured using the
 * {@link #setOption(SctpSocketOption,Object,Association) setOption} method. An
 * {@code SctpMultiChannel} supports the following options:
 * <blockquote>
 * <table class="striped">
 *   <caption style="display:none">Socket options</caption>
 *   <thead>
 *   <tr>
 *     <th scope="col">Option Name</th>
 *     <th scope="col">Description</th>
 *   </tr>
 *   </thead>
 *   <tbody>
 *   <tr>
 *     <th scope="row"> {@link SctpStandardSocketOptions#SCTP_DISABLE_FRAGMENTS
 *                                          SCTP_DISABLE_FRAGMENTS} </th>
 *     <td> Enables or disables message fragmentation </td>
 *   </tr>
 *   <tr>
 *     <th scope="row"> {@link SctpStandardSocketOptions#SCTP_EXPLICIT_COMPLETE
 *                                          SCTP_EXPLICIT_COMPLETE} </th>
 *     <td> Enables or disables explicit message completion </td>
 *   </tr>
 *    <tr>
 *     <th scope="row"> {@link SctpStandardSocketOptions#SCTP_FRAGMENT_INTERLEAVE
 *                                          SCTP_FRAGMENT_INTERLEAVE} </th>
 *     <td> Controls how the presentation of messages occur for the message
 *          receiver </td>
 *   </tr>
 *   <tr>
 *     <th scope="row"> {@link SctpStandardSocketOptions#SCTP_INIT_MAXSTREAMS
 *                                          SCTP_INIT_MAXSTREAMS} </th>
 *     <td> The maximum number of streams requested by the local endpoint during
 *          association initialization </td>
 *   </tr>
 *   <tr>
 *     <th scope="row"> {@link SctpStandardSocketOptions#SCTP_NODELAY SCTP_NODELAY} </th>
 *     <td> Enables or disable a Nagle-like algorithm </td>
 *   </tr>
 *   <tr>
 *     <th scope="row"> {@link SctpStandardSocketOptions#SCTP_PRIMARY_ADDR
 *                                          SCTP_PRIMARY_ADDR} </th>
 *     <td> Requests that the local SCTP stack use the given peer address as the
 *          association primary </td>
 *   </tr>
 *   <tr>
 *     <th scope="row"> {@link SctpStandardSocketOptions#SCTP_SET_PEER_PRIMARY_ADDR
 *                                          SCTP_SET_PEER_PRIMARY_ADDR} </th>
 *     <td> Requests that the peer mark the enclosed address as the association
 *          primary </td>
 *   </tr>
 *   <tr>
 *     <th scope="row"> {@link SctpStandardSocketOptions#SO_SNDBUF
 *                                          SO_SNDBUF} </th>
 *     <td> The size of the socket send buffer </td>
 *   </tr>
 *   <tr>
 *     <th scope="row"> {@link SctpStandardSocketOptions#SO_RCVBUF
 *                                          SO_RCVBUF} </th>
 *     <td> The size of the socket receive buffer </td>
 *   </tr>
 *   <tr>
 *     <th scope="row"> {@link SctpStandardSocketOptions#SO_LINGER
 *                                          SO_LINGER} </th>
 *     <td> Linger on close if data is present (when configured in blocking mode
 *          only) </td>
 *   </tr>
 *   </tbody>
 * </table>
 * </blockquote>
 * Additional (implementation specific) options may also be supported. The list
 * of options supported is obtained by invoking the {@link #supportedOptions()
 * supportedOptions} method.
 *
 * <p> SCTP multi channels are safe for use by multiple concurrent threads.
 * They support concurrent sending and receiving, though at most one thread may be
 * sending and at most one thread may be receiving at any given time.
 *
 * @since 1.7
 */
public abstract class SctpMultiChannel
    extends AbstractSelectableChannel
{
    /**
     * Initializes a new instance of this class.
     *
     * @param  provider
     *         The selector provider for this channel
     */
    protected SctpMultiChannel(SelectorProvider provider) {
        super(provider);
    }

    /**
     * Opens an SCTP multi channel.
     *
     * <P> The new channel is unbound.
     *
     * @return  A new SCTP multi channel
     *
     * @throws UnsupportedOperationException
     *         If the SCTP protocol is not supported
     *
     * @throws  IOException
     *          If an I/O error occurs
     */
    public static SctpMultiChannel open() throws
        IOException {
        return new sun.nio.ch.sctp.SctpMultiChannelImpl((SelectorProvider)null);
    }

    /**
     * Returns the open associations on this channel's socket.
     *
     * <P> Only associations whose {@link AssociationChangeNotification.AssocChangeEvent#COMM_UP
     * COMM_UP} association change event has been received are included
     * in the returned set of associations. Associations for which a
     * {@link AssociationChangeNotification.AssocChangeEvent#COMM_LOST COMM_LOST} or {@link
     * AssociationChangeNotification.AssocChangeEvent#SHUTDOWN SHUTDOWN} association change
     * event have been receive are removed from the set of associations.
     *
     * <P> The returned set of associations is a snapshot of the open
     * associations at the time that this method is invoked.
     *
     * @return  A {@code Set} containing the open associations, or an empty
     *          {@code Set} if there are none.
     *
     * @throws  ClosedChannelException
     *          If this channel is closed
     *
     * @throws  IOException
     *          If some other I/O error occurs
     */
    public abstract Set<Association> associations()
        throws IOException;

    /**
     * Binds the channel's socket to a local address and configures the socket
     * to listen for connections.
     *
     * <P> This method is used to establish a relationship between the socket
     * and the local address. Once a relationship is established then
     * the socket remains bound until the channel is closed. This relationship
     * may not necesssarily be with the address {@code local} as it may be removed
     * by {@link #unbindAddress unbindAddress}, but there will always be at least one local
     * address bound to the channel's socket once an invocation of this method
     * successfully completes.
     *
     * <P> Once the channel's socket has been successfully bound to a specific
     * address, that is not automatically assigned, more addresses
     * may be bound to it using {@link #bindAddress bindAddress}, or removed
     * using {@link #unbindAddress unbindAddress}.
     *
     * <P> The backlog parameter is the maximum number of pending connections on
     * the socket. Its exact semantics are implementation specific. An implementation
     * may impose an implementation specific maximum length or may choose to ignore
     * the parameter. If the backlog parameter has the value {@code 0}, or a negative
     * value, then an implementation specific default is used.
     *
     * @param  local
     *         The local address to bind the socket, or {@code null} to
     *         bind the socket to an automatically assigned socket address
     *
     * @param  backlog
     *         The maximum number of pending connections
     *
     * @return  This channel
     *
     * @throws  ClosedChannelException
     *          If this channel is closed
     *
     * @throws  java.nio.channels.AlreadyBoundException
     *          If this channel is already bound
     *
     * @throws  java.nio.channels.UnsupportedAddressTypeException
     *          If the type of the given address is not supported
     *
     * @throws  SecurityException
     *          If a security manager has been installed and its {@link
     *          java.lang.SecurityManager#checkListen(int) checkListen} method
     *          denies the operation
     *
     * @throws  IOException
     *          If some other I/O error occurs
     */
    public abstract SctpMultiChannel bind(SocketAddress local,
                                          int backlog)
        throws IOException;

    /**
     * Binds the channel's socket to a local address and configures the socket
     * to listen for connections.
     *
     * <P> This method works as if invoking it were equivalent to evaluating the
     * expression:
     * <blockquote><pre>
     * bind(local, 0);
     * </pre></blockquote>
     *
     * @param  local
     *         The local address to bind the socket, or {@code null} to
     *         bind the socket to an automatically assigned socket address
     *
     * @return  This channel
     *
     * @throws  ClosedChannelException
     *          If this channel is closed
     *
     * @throws  java.nio.channels.AlreadyBoundException
     *          If this channel is already bound
     *
     * @throws  java.nio.channels.UnsupportedAddressTypeException
     *          If the type of the given address is not supported
     *
     * @throws  SecurityException
     *          If a security manager has been installed and its {@link
     *          java.lang.SecurityManager#checkListen(int) checkListen} method
     *          denies the operation
     *
     * @throws  IOException
     *          If some other I/O error occurs
     */
    public final SctpMultiChannel bind(SocketAddress local)
        throws IOException {
        return bind(local, 0);
    }

    /**
     * Adds the given address to the bound addresses for the channel's
     * socket.
     *
     * <P> The given address must not be the {@link
     * java.net.InetAddress#isAnyLocalAddress wildcard} address.
     * The channel must be first bound using {@link #bind bind} before
     * invoking this method, otherwise {@link NotYetBoundException} is thrown.
     * The {@link #bind bind} method takes a {@code SocketAddress} as its
     * argument which typically contains a port number as well as an address.
     * Addresses subquently bound using this method are simply addresses as the
     * SCTP port number remains the same for the lifetime of the channel.
     *
     * <P> New associations setup after this method successfully completes
     * will be associated with the given address. Adding addresses to existing
     * associations is optional functionality. If the endpoint supports
     * dynamic address reconfiguration then it may send the appropriate message
     * to the peer to change the peers address lists.
     *
     * @param  address
     *         The address to add to the bound addresses for the socket
     *
     * @return  This channel
     *
     * @throws  ClosedChannelException
     *          If this channel is closed
     *
     * @throws  NotYetBoundException
     *          If this channel is not yet bound
     *
     * @throws  java.nio.channels.AlreadyBoundException
     *          If this channel is already bound to the given address
     *
     * @throws  IllegalArgumentException
     *          If address is {@code null} or the {@link
     *          java.net.InetAddress#isAnyLocalAddress wildcard} address
     *
     * @throws  IOException
     *          If some other I/O error occurs
     */
    public abstract SctpMultiChannel bindAddress(InetAddress address)
         throws IOException;

    /**
     * Removes the given address from the bound addresses for the channel's
     * socket.
     *
     * <P> The given address must not be the {@link
     * java.net.InetAddress#isAnyLocalAddress wildcard} address.
     * The channel must be first bound using {@link #bind bind} before
     * invoking this method, otherwise {@link NotYetBoundException} is thrown.
     *
     * <P> If this method is invoked on a channel that does
     * not have {@code address} as one of its bound addresses, or that has only
     * one local address bound to it, then this method throws
     * {@link IllegalUnbindException}.
     *
     * <P> The initial address that the channel's socket is bound to using
     * {@link #bind bind} may be removed from the bound addresses for the
     * channel's socket.
     *
     * <P> New associations setup after this method successfully completes
     * will not be associated with the given address. Removing addresses from
     * existing associations is optional functionality. If the endpoint supports
     * dynamic address reconfiguration then it may send the appropriate message
     * to the peer to change the peers address lists.
     *
     * @param  address
     *         The address to remove from the bound addresses for the socket
     *
     * @return  This channel
     *
     * @throws  ClosedChannelException
     *          If this channel is closed
     *
     * @throws  NotYetBoundException
     *          If this channel is not yet bound
     *
     * @throws  IllegalUnbindException
     *          {@code address} is not bound to the channel's socket, or the
     *          channel has only one address  bound to it
     *
     * @throws  IllegalArgumentException
     *          If address is {@code null} or the {@link
     *          java.net.InetAddress#isAnyLocalAddress wildcard} address
     *
     * @throws  IOException
     *          If some other I/O error occurs
     */
    public abstract SctpMultiChannel unbindAddress(InetAddress address)
         throws IOException;

    /**
     * Returns all of the socket addresses to which this channel's socket is
     * bound.
     *
     * @return  All the socket addresses that this channel's socket is
     *          bound to, or an empty {@code Set} if the channel's socket is not
     *          bound
     *
     * @throws  ClosedChannelException
     *          If the channel is closed
     *
     * @throws  IOException
     *          If an I/O error occurs
     */
    public abstract Set<SocketAddress> getAllLocalAddresses()
        throws IOException;

    /**
     * Returns all of the remote addresses to which the given association on
     * this channel's socket is connected.
     *
     * @param  association
     *         The association
     *
     * @return  All of the remote addresses for the given association, or
     *          an empty {@code Set} if the association has been shutdown
     *
     * @throws  ClosedChannelException
     *          If the channel is closed
     *
     * @throws  IOException
     *          If an I/O error occurs
     */
    public abstract Set<SocketAddress> getRemoteAddresses(Association association)
        throws IOException;

    /**
     * Shutdown an association without closing the channel.
     *
     * @param  association
     *         The association to shutdown
     *
     * @return  This channel
     *
     * @throws  ClosedChannelException
     *          If this channel is closed
     *
     * @throws  IOException
     *          If some other I/O error occurs
     */
    public abstract SctpMultiChannel shutdown(Association association)
            throws IOException;

    /**
     * Returns the value of a socket option.
     *
     * <P> Note that some options are retrieved on the channel's socket,
     * therefore the {@code association} parameter is not applicable and will be
     * ignored if given. However, if the option is association specific then the
     * association must be given.
     *
     * @param  <T>
     *         The type of the socket option value
     *
     * @param  name
     *         The socket option
     *
     * @param  association
     *         The association whose option should be retrieved, or {@code null}
     *         if this option should be retrieved at the channel's socket level.
     *
     * @return  The value of the socket option. A value of {@code null} may be
     *          a valid value for some socket options.
     *
     * @throws  UnsupportedOperationException
     *          If the socket option is not supported by this channel
     *
     * @throws  ClosedChannelException
     *          If this channel is closed
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @see SctpStandardSocketOptions
     */
    public abstract <T> T getOption(SctpSocketOption<T> name,
                                    Association association)
        throws IOException;

    /**
     * Sets the value of a socket option.
     *
     * <P> Note that some options are retrieved on the channel's socket,
     * therefore the {@code association} parameter is not applicable and will be
     * ignored if given. However, if the option is association specific then the
     * association must be given.
     *
     * @param   <T>
     *          The type of the socket option value
     *
     * @param   name
     *          The socket option
     *
     * @param  association
     *         The association whose option should be set, or {@code null}
     *         if this option should be set at the channel's socket level.
     *
     * @param   value
     *          The value of the socket option. A value of {@code null} may be
     *          a valid value for some socket options.
     *
     * @return  This channel
     *
     * @throws  UnsupportedOperationException
     *          If the socket option is not supported by this channel
     *
     * @throws  IllegalArgumentException
     *          If the value is not a valid value for this socket option
     *
     * @throws  ClosedChannelException
     *          If this channel is closed
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @see SctpStandardSocketOptions
     */
    public abstract <T> SctpMultiChannel setOption(SctpSocketOption<T> name,
                                                   T value,
                                                   Association association)
         throws IOException;

    /**
     * Returns a set of the socket options supported by this channel.
     *
     * <P> This method will continue to return the set of options even after the
     * channel has been closed.
     *
     * @return  A set of the socket options supported by this channel
     */
    public abstract Set<SctpSocketOption<?>> supportedOptions();

    /**
     * Returns an operation set identifying this channel's supported operations.
     *
     * <P> SCTP multi channels support reading, and writing, so this
     * method returns
     * {@code (}{@link SelectionKey#OP_READ} {@code |}&nbsp;{@link
     * SelectionKey#OP_WRITE}{@code )}.  </p>
     *
     * @return  The valid-operation set
     */
    @Override
    public final int validOps() {
        return (SelectionKey.OP_READ |
                SelectionKey.OP_WRITE );
    }

    /**
     * Receives a message and/or handles a notification via this channel.
     *
     * <P> If a message or notification is immediately available, or if this
     * channel is in blocking mode and one eventually becomes available, then
     * the message or notification is returned or handled, respectively. If this
     * channel is in non-blocking mode and a message or notification is not
     * immediately available then this method immediately returns {@code null}.
     *
     * <P> If this method receives a message it is copied into the given byte
     * buffer and an {@link MessageInfo} is returned.
     * The message is transferred into the given byte buffer starting at its
     * current position and the buffers position is incremented by the number of
     * bytes read. If there are fewer bytes remaining in the buffer than are
     * required to hold the message, or the underlying input buffer does not
     * contain the complete message, then an invocation of {@link
     * MessageInfo#isComplete isComplete} on the returned {@code
     * MessageInfo} will return {@code false}, and more invocations of this
     * method will be necessary to completely consume the messgae. Only
     * one message at a time will be partially delivered in any stream. The
     * socket option {@link SctpStandardSocketOptions#SCTP_FRAGMENT_INTERLEAVE
     * SCTP_FRAGMENT_INTERLEAVE} controls various aspects of what interlacing of
     * messages occurs.
     *
     * <P> If this method receives a notification then the appropriate method of
     * the given handler, if there is one, is invoked. If the handler returns {@link
     * HandlerResult#CONTINUE CONTINUE} then this method will try to receive another
     * message/notification, otherwise, if {@link HandlerResult#RETURN RETURN} is returned
     * this method will return {@code null}. If an uncaught exception is thrown by the
     * handler it will be propagated up the stack through this method.
     *
     * <P> If a security manager has been installed then for each new association
     * setup this method verifies that the associations source address and port
     * number are permitted by the security manager's {@link
     * java.lang.SecurityManager#checkAccept(String,int) checkAccept} method.
     *
     * <P> This method may be invoked at any time. If another thread has
     * already initiated a receive operation upon this channel, then an
     * invocation of this method will block until the first operation is
     * complete. The given handler is invoked without holding any locks used
     * to enforce the above synchronization policy, that way handlers
     * will not stall other threads from receiving. A handler should not invoke
     * the {@code receive} method of this channel, if it does an
     * {@link IllegalReceiveException} will be thrown.
     *
     * @param  <T>
     *         The type of the attachment
     *
     * @param  buffer
     *         The buffer into which bytes are to be transferred
     *
     * @param  attachment
     *         The object to attach to the receive operation; can be
     *         {@code null}
     *
     * @param  handler
     *         A handler to handle notifications from the SCTP stack, or
     *         {@code null} to ignore any notifications.
     *
     * @return  The {@code MessageInfo}, {@code null} if this channel is in
     *          non-blocking mode and no messages are immediately available or
     *          the notification handler returns {@code RETURN} after handling
     *          a notification
     *
     * @throws  java.nio.channels.ClosedChannelException
     *          If this channel is closed
     *
     * @throws  java.nio.channels.AsynchronousCloseException
     *          If another thread closes this channel
     *          while the read operation is in progress
     *
     * @throws  java.nio.channels.ClosedByInterruptException
     *          If another thread interrupts the current thread
     *          while the read operation is in progress, thereby
     *          closing the channel and setting the current thread's
     *          interrupt status
     *
     * @throws  NotYetBoundException
     *          If this channel is not yet bound
     *
     * @throws  IllegalReceiveException
     *          If the given handler invokes the {@code receive} method of this
     *          channel
     *
     * @throws  SecurityException
     *          If a security manager has been installed and it does not permit
     *          new associations to be accepted from the message's sender
     *
     * @throws  IOException
     *          If some other I/O error occurs
     */
    public abstract <T> MessageInfo receive(ByteBuffer buffer,
                                            T attachment,
                                            NotificationHandler<T> handler)
        throws IOException;

    /**
     * Sends a message via this channel.
     *
     * <P> If this channel is unbound then this method will invoke {@link
     * #bind(SocketAddress, int) bind(null, 0)} before sending any data.
     *
     * <P> If there is no association existing between this channel's socket
     * and the intended receiver, identified by the address in the given messageInfo, then one
     * will be automatically setup to the intended receiver. This is considered
     * to be Implicit Association Setup. Upon successful association setup, an
     * {@link AssociationChangeNotification association changed}
     * notification will be put to the SCTP stack with its {@code event} parameter set
     * to {@link AssociationChangeNotification.AssocChangeEvent#COMM_UP COMM_UP}
     * . This notification can be received by invoking {@link #receive
     * receive}.
     *
     * <P> If this channel is in blocking mode, there is sufficient room in the
     * underlying output buffer, then the remaining bytes in the given byte
     * buffer are transmitted as a single message. Sending a message
     * is atomic unless explicit message completion {@link
     * SctpStandardSocketOptions#SCTP_EXPLICIT_COMPLETE SCTP_EXPLICIT_COMPLETE}
     * socket option is enabled on this channel's socket.
     *
     * <P> If this channel is in non-blocking mode, there is sufficient room
     * in the underlying output buffer, and an implicit association setup is
     * required, then the remaining bytes in the given byte buffer are
     * transmitted as a single message, subject to {@link
     * SctpStandardSocketOptions#SCTP_EXPLICIT_COMPLETE SCTP_EXPLICIT_COMPLETE}.
     * If for any reason the message cannot
     * be delivered an {@link AssociationChangeNotification association
     * changed} notification is put on the SCTP stack with its {@code event} parameter set
     * to {@link AssociationChangeNotification.AssocChangeEvent#CANT_START CANT_START}.
     *
     * <P> The message is transferred from the byte buffer as if by a regular
     * {@link java.nio.channels.WritableByteChannel#write(java.nio.ByteBuffer)
     * write} operation.
     *
     * <P> If a security manager has been installed then for each new association
     * setup this method verifies that the given remote peers address and port
     * number are permitted by the security manager's {@link
     * java.lang.SecurityManager#checkConnect(String,int) checkConnect} method.
     *
     * <P> This method may be invoked at any time. If another thread has already
     * initiated a send operation upon this channel, then an invocation of
     * this method will block until the first operation is complete.
     *
     * @param  buffer
     *         The buffer containing the message to be sent
     *
     * @param  messageInfo
     *         Ancillary data about the message to be sent
     *
     * @return  The number of bytes sent, which will be either the number of
     *          bytes that were remaining in the messages buffer when this method
     *          was invoked or, if this channel is non-blocking, may be zero if
     *          there was insufficient room for the message in the underlying
     *          output buffer
     *
     * @throws  InvalidStreamException
     *          If {@code streamNumber} is negative, or if an association already
     *          exists and {@code streamNumber} is greater than the maximum number
     *          of outgoing streams
     *
     * @throws  java.nio.channels.ClosedChannelException
     *          If this channel is closed
     *
     * @throws  java.nio.channels.AsynchronousCloseException
     *          If another thread closes this channel
     *          while the read operation is in progress
     *
     * @throws  java.nio.channels.ClosedByInterruptException
     *          If another thread interrupts the current thread
     *          while the read operation is in progress, thereby
     *          closing the channel and setting the current thread's
     *          interrupt status
     *
     * @throws  SecurityException
     *          If a security manager has been installed and it does not permit
     *          new associations to be setup with the messages's address
     *
     * @throws  IOException
     *          If some other I/O error occurs
     */
    public abstract int send(ByteBuffer buffer, MessageInfo messageInfo)
        throws IOException;

    /**
     * Branches off an association.
     *
     * <P> An application can invoke this method to branch off an association
     * into a separate channel. The new bound and connected {@link SctpChannel}
     * will be created for the association. The branched off association will no
     * longer be part of this channel.
     *
     * <P> This is particularly useful when, for instance, the application
     * wishes to have a number of sporadic message senders/receivers remain
     * under the original SCTP multi channel but branch off those
     * associations carrying high volume data traffic into their own
     * separate SCTP channels.
     *
     * @param  association
     *         The association to branch off
     *
     * @return  The {@code SctpChannel}
     *
     * @throws  java.nio.channels.ClosedChannelException
     *          If this channel is closed
     *
     * @throws  IOException
     *          If some other I/O error occurs
     */
    public abstract SctpChannel branch(Association association)
        throws IOException;
}
