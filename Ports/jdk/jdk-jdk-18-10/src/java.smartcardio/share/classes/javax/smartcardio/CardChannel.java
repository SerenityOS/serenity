/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javax.smartcardio;

import java.nio.*;

/**
 * A logical channel connection to a Smart Card. It is used to exchange APDUs
 * with a Smart Card.
 * A CardChannel object can be obtained by calling the method
 * {@linkplain Card#getBasicChannel} or {@linkplain Card#openLogicalChannel}.
 *
 * @see Card
 * @see CommandAPDU
 * @see ResponseAPDU
 *
 * @since   1.6
 * @author  Andreas Sterbenz
 * @author  JSR 268 Expert Group
 */
public abstract class CardChannel {

    /**
     * Constructs a new CardChannel object.
     *
     * <p>This constructor is called by subclasses only. Application should
     * call the {@linkplain Card#getBasicChannel} and
     * {@linkplain Card#openLogicalChannel} methods to obtain a CardChannel
     * object.
     */
    protected CardChannel() {
        // empty
    }

    /**
     * Returns the Card this channel is associated with.
     *
     * @return the Card this channel is associated with
     */
    public abstract Card getCard();

    /**
     * Returns the channel number of this CardChannel. A channel number of
     * 0 indicates the basic logical channel.
     *
     * @return the channel number of this CardChannel.
     *
     * @throws IllegalStateException if this channel has been
     *   {@linkplain #close closed} or if the corresponding Card has been
     *   {@linkplain Card#disconnect disconnected}.
     */
    public abstract int getChannelNumber();

    /**
     * Transmits the specified command APDU to the Smart Card and returns the
     * response APDU.
     *
     * <p>The CLA byte of the command APDU is automatically adjusted to
     * match the channel number of this CardChannel.
     *
     * <p>Note that this method cannot be used to transmit
     * <code>MANAGE CHANNEL</code> APDUs. Logical channels should be managed
     * using the {@linkplain Card#openLogicalChannel} and {@linkplain
     * CardChannel#close CardChannel.close()} methods.
     *
     * <p>Implementations should transparently handle artifacts
     * of the transmission protocol.
     * For example, when using the T=0 protocol, the following processing
     * should occur as described in ISO/IEC 7816-4:
     *
     * <ul>
     * <li><p>if the response APDU has an SW1 of <code>61</code>, the
     * implementation should issue a <code>GET RESPONSE</code> command
     * using <code>SW2</code> as the <code>Le</code>field.
     * This process is repeated as long as an SW1 of <code>61</code> is
     * received. The response body of these exchanges is concatenated
     * to form the final response body.
     *
     * <li><p>if the response APDU is <code>6C XX</code>, the implementation
     * should reissue the command using <code>XX</code> as the
     * <code>Le</code> field.
     * </ul>
     *
     * <p>The ResponseAPDU returned by this method is the result
     * after this processing has been performed.
     *
     * @param command the command APDU
     * @return the response APDU received from the card
     *
     * @throws IllegalStateException if this channel has been
     *   {@linkplain #close closed} or if the corresponding Card has been
     *   {@linkplain Card#disconnect disconnected}.
     * @throws IllegalArgumentException if the APDU encodes a
     *   <code>MANAGE CHANNEL</code> command
     * @throws NullPointerException if command is null
     * @throws CardException if the card operation failed
     */
    public abstract ResponseAPDU transmit(CommandAPDU command) throws CardException;

    /**
     * Transmits the command APDU stored in the command ByteBuffer and receives
     * the response APDU in the response ByteBuffer.
     *
     * <p>The command buffer must contain valid command APDU data starting
     * at <code>command.position()</code> and the APDU must be
     * <code>command.remaining()</code> bytes long.
     * Upon return, the command buffer's position will be equal
     * to its limit; its limit will not have changed. The output buffer
     * will have received the response APDU bytes. Its position will have
     * advanced by the number of bytes received, which is also the return
     * value of this method.
     *
     * <p>The CLA byte of the command APDU is automatically adjusted to
     * match the channel number of this CardChannel.
     *
     * <p>Note that this method cannot be used to transmit
     * <code>MANAGE CHANNEL</code> APDUs. Logical channels should be managed
     * using the {@linkplain Card#openLogicalChannel} and {@linkplain
     * CardChannel#close CardChannel.close()} methods.
     *
     * <p>See {@linkplain #transmit transmit()} for a discussion of the handling
     * of response APDUs with the SW1 values <code>61</code> or <code>6C</code>.
     *
     * @param command the buffer containing the command APDU
     * @param response the buffer that shall receive the response APDU from
     *   the card
     * @return the length of the received response APDU
     *
     * @throws IllegalStateException if this channel has been
     *   {@linkplain #close closed} or if the corresponding Card has been
     *   {@linkplain Card#disconnect disconnected}.
     * @throws NullPointerException if command or response is null
     * @throws ReadOnlyBufferException if the response buffer is read-only
     * @throws IllegalArgumentException if command and response are the
     *   same object, if <code>response</code> may not have
     *   sufficient space to receive the response APDU
     *   or if the APDU encodes a <code>MANAGE CHANNEL</code> command
     * @throws CardException if the card operation failed
     */
    public abstract int transmit(ByteBuffer command, ByteBuffer response)
        throws CardException;

    /**
     * Closes this CardChannel. The logical channel is closed by issuing
     * a <code>MANAGE CHANNEL</code> command that should use the format
     * <code>[xx 70 80 0n]</code> where <code>n</code> is the channel number
     * of this channel and <code>xx</code> is the <code>CLA</code>
     * byte that encodes this logical channel and has all other bits set to 0.
     * After this method returns, calling other
     * methods in this class will raise an IllegalStateException.
     *
     * <p>Note that the basic logical channel cannot be closed using this
     * method. It can be closed by calling {@link Card#disconnect}.
     *
     * @throws CardException if the card operation failed
     * @throws IllegalStateException if this CardChannel represents a
     *   connection the basic logical channel
     */
    public abstract void close() throws CardException;

}
