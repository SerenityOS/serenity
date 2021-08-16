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
 * @bug 6239117 6470320 8168851
 * @summary test if transmitControlCommand() works
 * @author Andreas Sterbenz
 * @modules java.smartcardio/javax.smartcardio
 * @run main/manual TestControl
 * @run main/othervm/manual/java.security.policy==test.policy TestControl
 */

// This test requires special hardware.

import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;

public class TestControl extends Utils {

    private static final int IOCTL_SMARTCARD_VENDOR_IFD_EXCHANGE = 0x42000000 + 1;

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

        byte[] data = new byte[] {2};
//      byte[] data = new byte[] {6, 0, 10, 1, 1, 16, 0};

        try {
            byte[] resp = card.transmitControlCommand(IOCTL_SMARTCARD_VENDOR_IFD_EXCHANGE, data);
            System.out.println("Firmware: " + toString(resp));
            throw new Exception();
        } catch (CardException e) {
            // we currently don't know of any control commands that work with
            // our readers. call the function just to make sure we don't crash
            // or throw the wrong exception
            System.out.println("OK: " + e);
            e.printStackTrace(System.out);
        }
        try {
            card.transmitControlCommand(IOCTL_SMARTCARD_VENDOR_IFD_EXCHANGE, null);
        } catch (NullPointerException e) {
            System.out.println("OK: " + e);
        }

        // disconnect
        card.disconnect(true);

        System.out.println("OK.");
    }

}
