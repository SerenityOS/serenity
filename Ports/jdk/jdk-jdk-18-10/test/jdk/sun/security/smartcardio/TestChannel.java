/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/*
 * @test
 * @bug 6239117 8168851
 * @summary test logical channels work
 * @author Andreas Sterbenz
 * @modules java.smartcardio/javax.smartcardio
 * @run main/manual TestChannel
 * @run main/othervm/manual/java.security.policy==test.policy TestChannel
 */

// This test requires special hardware.

import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;

public class TestChannel extends Utils {

    static final byte[] c1 = parse("00 A4 04 00 07 A0 00 00 00 62 81 01 00");
    static final byte[] r1 = parse("07:a0:00:00:00:62:81:01:04:01:00:00:24:05:00:0b:04:b0:55:90:00");
//    static final byte[] r1 = parse("07 A0 00 00 00 62 81 01 04 01 00 00 24 05 00 0B 04 B0 25 90 00");

    static final byte[] openChannel = parse("00 70 00 00 01");
    static final byte[] closeChannel = new byte[] {0x01, 0x70, (byte)0x80, 0};

    public static void main(String[] args) throws Exception {
        CardTerminal terminal = getTerminal(args);
        if (terminal == null) {
            System.out.println("Skipping the test: " +
                    "no card terminals available");
            return;
        }

        // establish a connection with the card
        Card card = terminal.connect("T=0");
        System.out.println("card: " + card);

        CardChannel basicChannel = card.getBasicChannel();

        try {
            basicChannel.transmit(new CommandAPDU(openChannel));
        } catch (IllegalArgumentException e) {
            System.out.println("OK: " + e);
        }

        try {
            basicChannel.transmit(new CommandAPDU(closeChannel));
        } catch (IllegalArgumentException e) {
            System.out.println("OK: " + e);
        }

        byte[] atr = card.getATR().getBytes();
        System.out.println("atr: " + toString(atr));

        // semi-accurate test to see if the card appears to support logical channels
        boolean supportsChannels = false;
        for (int i = 0; i < atr.length; i++) {
            if (atr[i] == 0x73) {
                supportsChannels = true;
                break;
            }
        }

        if (supportsChannels == false) {
            System.out.println("Card does not support logical channels, skipping...");
        } else {
            CardChannel channel = card.openLogicalChannel();
            System.out.println("channel: " + channel);

/*
        // XXX bug in Oberthur card??
        System.out.println("Transmitting...");
        transmitTestCommand(channel);
        System.out.println("OK");
/**/

            channel.close();
        }

        // disconnect
        card.disconnect(true);

        System.out.println("OK.");
    }

}
