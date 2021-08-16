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

import java.nio.*;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.smartcardio.*;

import static sun.security.smartcardio.PCSC.*;

/**
 * CardChannel implementation.
 *
 * @since   1.6
 * @author  Andreas Sterbenz
 */
final class ChannelImpl extends CardChannel {

    // the card this channel is associated with
    private final CardImpl card;

    // the channel number, 0 for the basic logical channel
    private final int channel;

    // whether this channel has been closed. only logical channels can be closed
    private volatile boolean isClosed;

    ChannelImpl(CardImpl card, int channel) {
        this.card = card;
        this.channel = channel;
    }

    void checkClosed() {
        card.checkState();
        if (isClosed) {
            throw new IllegalStateException("Logical channel has been closed");
        }
    }

    public Card getCard() {
        return card;
    }

    public int getChannelNumber() {
        checkClosed();
        return channel;
    }

    private static void checkManageChannel(byte[] b) {
        if (b.length < 4) {
            throw new IllegalArgumentException
                ("Command APDU must be at least 4 bytes long");
        }
        if ((b[0] >= 0) && (b[1] == 0x70)) {
            throw new IllegalArgumentException
                ("Manage channel command not allowed, use openLogicalChannel()");
        }
    }

    public ResponseAPDU transmit(CommandAPDU command) throws CardException {
        checkClosed();
        card.checkExclusive();
        byte[] commandBytes = command.getBytes();
        byte[] responseBytes = doTransmit(commandBytes);
        return new ResponseAPDU(responseBytes);
    }

    public int transmit(ByteBuffer command, ByteBuffer response) throws CardException {
        checkClosed();
        card.checkExclusive();
        if ((command == null) || (response == null)) {
            throw new NullPointerException();
        }
        if (response.isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        if (command == response) {
            throw new IllegalArgumentException
                    ("command and response must not be the same object");
        }
        if (response.remaining() < 258) {
            throw new IllegalArgumentException
                    ("Insufficient space in response buffer");
        }
        byte[] commandBytes = new byte[command.remaining()];
        command.get(commandBytes);
        byte[] responseBytes = doTransmit(commandBytes);
        response.put(responseBytes);
        return responseBytes.length;
    }

    private final static boolean t0GetResponse =
        getBooleanProperty("sun.security.smartcardio.t0GetResponse", true);

    private final static boolean t1GetResponse =
        getBooleanProperty("sun.security.smartcardio.t1GetResponse", true);

    private final static boolean t1StripLe =
        getBooleanProperty("sun.security.smartcardio.t1StripLe", false);

    private static boolean getBooleanProperty(String name, boolean def) {
        @SuppressWarnings("removal")
        String val = AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty(name));
        if (val == null) {
            return def;
        }
        if (val.equalsIgnoreCase("true")) {
            return true;
        } else if (val.equalsIgnoreCase("false")) {
            return false;
        } else {
            throw new IllegalArgumentException
                (name + " must be either 'true' or 'false'");
        }
    }

    private byte[] concat(byte[] b1, byte[] b2, int n2) {
        int n1 = b1.length;
        if ((n1 == 0) && (n2 == b2.length)) {
            return b2;
        }
        byte[] res = new byte[n1 + n2];
        System.arraycopy(b1, 0, res, 0, n1);
        System.arraycopy(b2, 0, res, n1, n2);
        return res;
    }

    private final static int RESPONSE_ITERATIONS = 256;
    private final static byte[] B0 = new byte[0];

    private byte[] doTransmit(byte[] command) throws CardException {
        // note that we modify the 'command' array in some cases, so it must
        // be a copy of the application provided data.
        try {
            checkManageChannel(command);
            setChannel(command);
            int n = command.length;
            boolean t0 = card.protocol == SCARD_PROTOCOL_T0;
            boolean t1 = card.protocol == SCARD_PROTOCOL_T1;
            if (t0 && (n >= 7) && (command[4] == 0)) {
                throw new CardException
                        ("Extended length forms not supported for T=0");
            }
            if ((t0 || (t1 && t1StripLe)) && (n >= 7)) {
                int lc = command[4] & 0xff;
                if (lc != 0) {
                    if (n == lc + 6) {
                        n--;
                    }
                } else {
                    lc = ((command[5] & 0xff) << 8) | (command[6] & 0xff);
                    if (n == lc + 9) {
                        n -= 2;
                    }
                }
            }
            boolean getresponse = (t0 && t0GetResponse) || (t1 && t1GetResponse);
            int k = 0;
            byte[] result = B0;
            while (true) {
                if (++k > RESPONSE_ITERATIONS) {
                    throw new CardException("Number of response iterations" +
                            " exceeded maximum " + RESPONSE_ITERATIONS);
                }
                byte[] response = SCardTransmit
                    (card.cardId, card.protocol, command, 0, n);
                int rn = response.length;
                if (getresponse && (rn >= 2) && (n >= 1)) {
                    // see ISO 7816/2005, 5.1.3
                    if ((rn == 2) && (response[0] == 0x6c)) {
                        // Resend command using SW2 as short Le field
                        command[n - 1] = response[1];
                        continue;
                    }
                    if (response[rn - 2] == 0x61) {
                        // Issue a GET RESPONSE command with the same CLA
                        // using SW2 as short Le field
                        if (rn > 2) {
                            result = concat(result, response, rn - 2);
                        }
                        if (command.length < 5) {
                            byte cla = command[0];
                            command = new byte[5];
                            command[0] = cla;
                        }
                        command[1] = (byte)0xC0;
                        command[2] = 0;
                        command[3] = 0;
                        command[4] = response[rn - 1];
                        n = 5;
                        continue;
                    }
                }
                result = concat(result, response, rn);
                break;
            }
            return result;
        } catch (PCSCException e) {
            card.handleError(e);
            throw new CardException(e);
        }
    }

    private static int getSW(byte[] res) throws CardException {
        if (res.length < 2) {
            throw new CardException("Invalid response length: " + res.length);
        }
        int sw1 = res[res.length - 2] & 0xff;
        int sw2 = res[res.length - 1] & 0xff;
        return (sw1 << 8) | sw2;
    }

    private static boolean isOK(byte[] res) throws CardException {
        return (res.length == 2) && (getSW(res) == 0x9000);
    }

    private void setChannel(byte[] com) {
        int cla = com[0];
        if (cla < 0) {
            // proprietary class format, cannot set or check logical channel
            // for now, just return
            return;
        }
        // classes 001x xxxx is reserved for future use in ISO, ignore
        if ((cla & 0xe0) == 0x20) {
            return;
        }
        // see ISO 7816/2005, table 2 and 3
        if (channel <= 3) {
            // mask of bits 7, 1, 0 (channel number)
            // 0xbc == 1011 1100
            com[0] &= 0xbc;
            com[0] |= channel;
        } else if (channel <= 19) {
            // mask of bits 7, 3, 2, 1, 0 (channel number)
            // 0xbc == 1011 0000
            com[0] &= 0xb0;
            com[0] |= 0x40;
            com[0] |= (channel - 4);
        } else {
            throw new RuntimeException("Unsupported channel number: " + channel);
        }
    }

    public void close() throws CardException {
        if (getChannelNumber() == 0) {
            throw new IllegalStateException("Cannot close basic logical channel");
        }
        if (isClosed) {
            return;
        }
        card.checkExclusive();
        try {
            byte[] com = new byte[] {0x00, 0x70, (byte)0x80, 0};
            com[3] = (byte)getChannelNumber();
            setChannel(com);
            byte[] res = SCardTransmit(card.cardId, card.protocol, com, 0, com.length);
            if (isOK(res) == false) {
                throw new CardException("close() failed: " + PCSC.toString(res));
            }
        } catch (PCSCException e) {
            card.handleError(e);
            throw new CardException("Could not close channel", e);
        } finally {
            isClosed = true;
        }
    }

    public String toString() {
        return "PC/SC channel " + channel;
    }

}
