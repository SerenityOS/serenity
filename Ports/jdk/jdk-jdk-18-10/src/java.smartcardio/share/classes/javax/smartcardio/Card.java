/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.ByteBuffer;

/**
 * A Smart Card with which a connection has been established. Card objects
 * are obtained by calling {@link CardTerminal#connect CardTerminal.connect()}.
 *
 * @see CardTerminal
 *
 * @since   1.6
 * @author  Andreas Sterbenz
 * @author  JSR 268 Expert Group
*/
public abstract class Card {

    /**
     * Constructs a new Card object.
     *
     * <p>This constructor is called by subclasses only. Application should
     * call the {@linkplain CardTerminal#connect CardTerminal.connect()}
     * method to obtain a Card
     * object.
     */
    protected Card() {
        // empty
    }

    /**
     * Returns the ATR of this card.
     *
     * @return the ATR of this card.
     */
    public abstract ATR getATR();

    /**
     * Returns the protocol in use for this card.
     *
     * @return the protocol in use for this card, for example "T=0" or "T=1"
     */
    public abstract String getProtocol();

    /**
     * Returns the CardChannel for the basic logical channel. The basic
     * logical channel has a channel number of 0.
     *
     * @return the CardChannel for the basic logical channel
     * @throws SecurityException if a SecurityManager exists and the
     *   caller does not have the required
     *   {@linkplain CardPermission permission}
     * @throws IllegalStateException if this card object has been disposed of
     *   via the {@linkplain #disconnect disconnect()} method
     */
    public abstract CardChannel getBasicChannel();

    /**
     * Opens a new logical channel to the card and returns it. The channel is
     * opened by issuing a <code>MANAGE CHANNEL</code> command that should use
     * the format <code>[00 70 00 00 01]</code>.
     *
     * @return the logical channel which has been opened
     * @throws SecurityException if a SecurityManager exists and the
     *   caller does not have the required
     *   {@linkplain CardPermission permission}
     * @throws CardException is a new logical channel could not be opened
     * @throws IllegalStateException if this card object has been disposed of
     *   via the {@linkplain #disconnect disconnect()} method
     */
    public abstract CardChannel openLogicalChannel() throws CardException;

    /**
     * Requests exclusive access to this card.
     *
     * <p>Once a thread has invoked <code>beginExclusive</code>, only this
     * thread is allowed to communicate with this card until it calls
     * <code>endExclusive</code>. Other threads attempting communication
     * will receive a CardException.
     *
     * <p>Applications have to ensure that exclusive access is correctly
     * released. This can be achieved by executing
     * the <code>beginExclusive()</code> and <code>endExclusive</code> calls
     * in a <code>try ... finally</code> block.
     *
     * @throws SecurityException if a SecurityManager exists and the
     *   caller does not have the required
     *   {@linkplain CardPermission permission}
     * @throws CardException if exclusive access has already been set
     *   or if exclusive access could not be established
     * @throws IllegalStateException if this card object has been disposed of
     *   via the {@linkplain #disconnect disconnect()} method
     */
    public abstract void beginExclusive() throws CardException;

    /**
     * Releases the exclusive access previously established using
     * <code>beginExclusive</code>.
     *
     * @throws SecurityException if a SecurityManager exists and the
     *   caller does not have the required
     *   {@linkplain CardPermission permission}
     * @throws IllegalStateException if the active Thread does not currently have
     *   exclusive access to this card or
     *   if this card object has been disposed of
     *   via the {@linkplain #disconnect disconnect()} method
     * @throws CardException if the operation failed
     */
    public abstract void endExclusive() throws CardException;

    /**
     * Transmits a control command to the terminal device.
     *
     * <p>This can be used to, for example, control terminal functions like
     * a built-in PIN pad or biometrics.
     *
     * @param controlCode the control code of the command
     * @param command the command data
     * @return the response from the terminal device
     *
     * @throws SecurityException if a SecurityManager exists and the
     *   caller does not have the required
     *   {@linkplain CardPermission permission}
     * @throws NullPointerException if command is null
     * @throws CardException if the card operation failed
     * @throws IllegalStateException if this card object has been disposed of
     *   via the {@linkplain #disconnect disconnect()} method
     */
    public abstract byte[] transmitControlCommand(int controlCode,
            byte[] command) throws CardException;

    /**
     * Disconnects the connection with this card. After this method returns,
     * calling methods on this object or in CardChannels associated with this
     * object that require interaction with the card will raise an
     * IllegalStateException.
     *
     * @param reset whether to reset the card after disconnecting.
     *
     * @throws CardException if the card operation failed
     * @throws SecurityException if a SecurityManager exists and the
     *   caller does not have the required
     *   {@linkplain CardPermission permission}
     */
    public abstract void disconnect(boolean reset) throws CardException;

}
