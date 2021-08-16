/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6293769 6294527 6474858
 * @summary test transmit() works
 * @author Andreas Sterbenz
 * @modules java.smartcardio/javax.smartcardio
 * @run main/manual TestTransmit
 */

// This test requires special hardware.

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

public class TestTransmit extends Utils {

    private final static String CMD_MARKER = "C-APDU: ";
    private final static String RES_MARKER = "R-APDU: ";

    public static void main(String[] args) throws Exception {
        CardTerminal terminal = getTerminal(args);
        if (terminal == null) {
            System.out.println("Skipping the test: " +
                    "no card terminals available");
            return;
        }

        Card card = terminal.connect("T=0");
        CardChannel channel = card.getBasicChannel();

        BufferedReader reader = new BufferedReader(new FileReader("apdu.log"));

        byte[] command = null;
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            if (line.startsWith(CMD_MARKER)) {
                System.out.println(line);
                line = line.substring(CMD_MARKER.length());
                command = parse(line);
            } else if (line.startsWith(RES_MARKER)) {
                System.out.println(line);
                line = line.substring(RES_MARKER.length());
                Bytes response = parseWildcard(line);
                CommandAPDU capdu = new CommandAPDU(command);
                ResponseAPDU rapdu = channel.transmit(capdu);
                byte[] received = rapdu.getBytes();
                if (received.length != response.bytes.length) {
                    throw new Exception("Length mismatch: " + toString(received));
                }
                for (int i = 0; i < received.length; i++) {
                    byte mask = response.mask[i];
                    if ((received[i] & response.mask[i]) != response.bytes[i]) {
                        throw new Exception("Mismatch: " + toString(received));
                    }
                }
            } // else ignore
        }

        // JDK-6474858 : CardChannel.transmit(CommandAPDU) throws
        //               unexpected ArrayIndexOutOfBoundsException
        {
            CommandAPDU capdu2 = new CommandAPDU(0x00, 0xA4, 0x00, 0x00);
            channel.transmit(capdu2);
        }

        // disconnect
        card.disconnect(true);

        System.out.println("OK.");
    }

    private static class Bytes {
        final byte[] bytes;
        final byte[] mask;
        Bytes(byte[] bytes, byte[] mask) {
            this.bytes = bytes;
            this.mask = mask;
        }
    }

    private static Bytes parseWildcard(String s) {
        try {
            int n = s.length();
            ByteArrayOutputStream out = new ByteArrayOutputStream(n >> 1);
            ByteArrayOutputStream mask = new ByteArrayOutputStream(n >> 1);
            StringReader r = new StringReader(s);
            while (true) {
                int b1 = nextNibble(r);
                if (b1 < 0) {
                    if (b1 == -1) {
                        break;
                    }
                    int b2 = nextNibble(r);
                    if (b2 != -2) {
                        throw new RuntimeException("Invalid wildcard: " + s);
                    }
                    out.write(0);
                    mask.write(0);
                    continue;
                }
                int b2 = nextNibble(r);
                if (b2 < 0) {
                    throw new RuntimeException("Invalid string " + s);
                }
                int b = (b1 << 4) | b2;
                out.write(b);
                mask.write(0xff);
            }
            byte[] b = out.toByteArray();
            byte[] m = mask.toByteArray();
            return new Bytes(b, m);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static int nextNibble(StringReader r) throws IOException {
        while (true) {
            int ch = r.read();
            if (ch == -1) {
                return -1;
            } else if ((ch >= '0') && (ch <= '9')) {
                return ch - '0';
            } else if ((ch >= 'a') && (ch <= 'f')) {
                return ch - 'a' + 10;
            } else if ((ch >= 'A') && (ch <= 'F')) {
                return ch - 'A' + 10;
            } else if (ch == 'X') {
                return -2;
            }
        }
    }

}
