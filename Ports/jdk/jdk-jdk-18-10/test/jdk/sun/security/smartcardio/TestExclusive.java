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
 * @bug 6239117
 * @summary verify that beginExclusive()/endExclusive() works
 * @author Andreas Sterbenz
 * @modules java.smartcardio/javax.smartcardio
 * @run main/manual TestExclusive
 */

// This test requires special hardware.

import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;

public class TestExclusive extends Utils {

    static volatile boolean exclusive;

    static volatile boolean otherOK;

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

        Thread thread = new Thread(new OtherThread(card));
        thread.setDaemon(true);
        thread.start();

        card.beginExclusive();
        exclusive = true;

        Thread.sleep(1000);
        System.out.println("=1=resuming...");

        CardChannel channel = card.getBasicChannel();

        System.out.println("=1=Transmitting...");
        transmitTestCommand(channel);
        System.out.println("=1=OK");

        try {
            card.beginExclusive();
        } catch (CardException e) {
            System.out.println("=1=OK: " + e);
        }

        card.endExclusive();

        try {
            card.endExclusive();
        } catch (IllegalStateException e) {
            System.out.println("=1=OK: " + e);
        }

        exclusive = false;

        Thread.sleep(1000);

        // disconnect
        card.disconnect(true);

        if (! otherOK) {
            throw new Exception("Secondary thread failed");
        }

        System.out.println("=1=OK.");
    }

    private static class OtherThread implements Runnable {
        private final Card card;
        OtherThread(Card card) {
            this.card = card;
        }

        public void run() {
            try {
                while (exclusive == false) {
                    Thread.sleep(100);
                }

                System.out.println("=2=trying endexclusive...");
                try {
                    card.endExclusive();
                } catch (IllegalStateException e) {
                    System.out.println("=2=OK: " + e);
                }

                System.out.println("=2=trying beginexclusive...");
                try {
                    card.beginExclusive();
                } catch (CardException e) {
                    System.out.println("=2=OK: " + e);
                }

                System.out.println("=2=trying to transmit...");
                CardChannel channel = card.getBasicChannel();
                try {
                    channel.transmit(new CommandAPDU(C1));
                } catch (CardException e) {
                    System.out.println("=2=OK: " + e);
                }

                while (exclusive) {
                    Thread.sleep(100);
                }

                System.out.println("=2=transmitting...");
                transmitTestCommand(channel);
                System.out.println("=2=OK...");

                System.out.println("=2=setting ok");
                otherOK = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
