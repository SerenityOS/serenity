/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.smartcardio;

import java.nio.ByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import javax.smartcardio.*;
import static sun.security.smartcardio.PCSC.*;

/**
 * Card implementation.
 *
 * @since   1.6
 * @author  Andreas Sterbenz
 */
final class CardImpl extends Card {

    private static enum State { OK, REMOVED, DISCONNECTED };

    // the terminal that created this card
    private final TerminalImpl terminal;

    // the native SCARDHANDLE
    final long cardId;

    // atr of this card
    private final ATR atr;

    // protocol in use, one of SCARD_PROTOCOL_T0 and SCARD_PROTOCOL_T1
    final int protocol;

    // the basic logical channel (channel 0)
    private final ChannelImpl basicChannel;

    // state of this card connection
    private volatile State state;

    // thread holding exclusive access to the card, or null
    private volatile Thread exclusiveThread;

    // used for platform specific logic
    private static final boolean isWindows;

    static {
        @SuppressWarnings("removal")
        final String osName = AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty("os.name"));
        isWindows = osName.startsWith("Windows");
    }

    CardImpl(TerminalImpl terminal, String protocol) throws PCSCException {
        this.terminal = terminal;
        int sharingMode = SCARD_SHARE_SHARED;
        int connectProtocol;
        if (protocol.equals("*")) {
            connectProtocol = SCARD_PROTOCOL_T0 | SCARD_PROTOCOL_T1;
        } else if (protocol.equalsIgnoreCase("T=0")) {
            connectProtocol = SCARD_PROTOCOL_T0;
        } else if (protocol.equalsIgnoreCase("T=1")) {
            connectProtocol = SCARD_PROTOCOL_T1;
        } else if (protocol.equalsIgnoreCase("direct")) {
            // testing

            // MSDN states that the preferred protocol can be zero, but doesn't
            //     specify whether other values are allowed.
            // pcsc-lite implementation expects the preferred protocol to be non zero.
            connectProtocol = isWindows ? 0 : SCARD_PROTOCOL_RAW;

            sharingMode = SCARD_SHARE_DIRECT;
        } else {
            throw new IllegalArgumentException("Unsupported protocol " + protocol);
        }
        cardId = SCardConnect(terminal.contextId, terminal.name,
                    sharingMode, connectProtocol);
        byte[] status = new byte[2];
        byte[] atrBytes = SCardStatus(cardId, status);
        atr = new ATR(atrBytes);
        this.protocol = status[1] & 0xff;
        basicChannel = new ChannelImpl(this, 0);
        state = State.OK;
    }

    void checkState()  {
        State s = state;
        if (s == State.DISCONNECTED) {
            throw new IllegalStateException("Card has been disconnected");
        } else if (s == State.REMOVED) {
            throw new IllegalStateException("Card has been removed");
        }
    }

    boolean isValid() {
        if (state != State.OK) {
            return false;
        }
        // ping card via SCardStatus
        try {
            SCardStatus(cardId, new byte[2]);
            return true;
        } catch (PCSCException e) {
            state = State.REMOVED;
            return false;
        }
    }

    private void checkSecurity(String action) {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new CardPermission(terminal.name, action));
        }
    }

    void handleError(PCSCException e) {
        if (e.code == SCARD_W_REMOVED_CARD) {
            state = State.REMOVED;
        }
    }

    public ATR getATR() {
        return atr;
    }

    public String getProtocol() {
        switch (protocol) {
        case SCARD_PROTOCOL_T0:
            return "T=0";
        case SCARD_PROTOCOL_T1:
            return "T=1";
        default:
            // should never occur
            return "Unknown protocol " + protocol;
        }
    }

    public CardChannel getBasicChannel() {
        checkSecurity("getBasicChannel");
        checkState();
        return basicChannel;
    }

    private static int getSW(byte[] b) {
        if (b.length < 2) {
            return -1;
        }
        int sw1 = b[b.length - 2] & 0xff;
        int sw2 = b[b.length - 1] & 0xff;
        return (sw1 << 8) | sw2;
    }

    private static byte[] commandOpenChannel = new byte[] {0, 0x70, 0, 0, 1};

    public CardChannel openLogicalChannel() throws CardException {
        checkSecurity("openLogicalChannel");
        checkState();
        checkExclusive();
        try {
            byte[] response = SCardTransmit
                (cardId, protocol, commandOpenChannel, 0, commandOpenChannel.length);
            if ((response.length != 3) || (getSW(response) != 0x9000)) {
                throw new CardException
                        ("openLogicalChannel() failed, card response: "
                        + PCSC.toString(response));
            }
            return new ChannelImpl(this, response[0]);
        } catch (PCSCException e) {
            handleError(e);
            throw new CardException("openLogicalChannel() failed", e);
        }
    }

    void checkExclusive() throws CardException {
        Thread t = exclusiveThread;
        if (t == null) {
            return;
        }
        if (t != Thread.currentThread()) {
            throw new CardException("Exclusive access established by another Thread");
        }
    }

    public synchronized void beginExclusive() throws CardException {
        checkSecurity("exclusive");
        checkState();
        if (exclusiveThread != null) {
            throw new CardException
                    ("Exclusive access has already been assigned to Thread "
                    + exclusiveThread.getName());
        }
        try {
            SCardBeginTransaction(cardId);
        } catch (PCSCException e) {
            handleError(e);
            throw new CardException("beginExclusive() failed", e);
        }
        exclusiveThread = Thread.currentThread();
    }

    public synchronized void endExclusive() throws CardException {
        checkState();
        if (exclusiveThread != Thread.currentThread()) {
            throw new IllegalStateException
                    ("Exclusive access not assigned to current Thread");
        }
        try {
            SCardEndTransaction(cardId, SCARD_LEAVE_CARD);
        } catch (PCSCException e) {
            handleError(e);
            throw new CardException("endExclusive() failed", e);
        } finally {
            exclusiveThread = null;
        }
    }

    public byte[] transmitControlCommand(int controlCode, byte[] command)
            throws CardException {
        checkSecurity("transmitControl");
        checkState();
        checkExclusive();
        if (command == null) {
            throw new NullPointerException();
        }
        try {
            byte[] r = SCardControl(cardId, controlCode, command);
            return r;
        } catch (PCSCException e) {
            handleError(e);
            throw new CardException("transmitControlCommand() failed", e);
        }
    }

    public void disconnect(boolean reset) throws CardException {
        if (reset) {
            checkSecurity("reset");
        }
        if (state != State.OK) {
            return;
        }
        checkExclusive();
        try {
            SCardDisconnect(cardId, (reset ? SCARD_RESET_CARD : SCARD_LEAVE_CARD));
        } catch (PCSCException e) {
            throw new CardException("disconnect() failed", e);
        } finally {
            state = State.DISCONNECTED;
            exclusiveThread = null;
        }
    }

    public String toString() {
        return "PC/SC card in " + terminal.name
            + ", protocol " + getProtocol() + ", state " + state;
    }

    @SuppressWarnings("deprecation")
    protected void finalize() throws Throwable {
        try {
            if (state == State.OK) {
                state = State.DISCONNECTED;
                SCardDisconnect(cardId, SCARD_LEAVE_CARD);
            }
        } finally {
            super.finalize();
        }
    }

}
