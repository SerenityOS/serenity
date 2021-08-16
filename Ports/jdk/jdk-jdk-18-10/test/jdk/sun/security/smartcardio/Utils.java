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


// common utility functions for the PC/SC tests

import java.io.StringReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardChannel;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.TerminalFactory;

public class Utils {

    static void setLibrary(String[] args) {
        if ((args.length > 0) && args[0].equalsIgnoreCase("MUSCLE")) {
            System.setProperty("sun.security.smartcardio.library", "/usr/local/$LIBISA/libpcsclite.so");
        }
    }

    static TerminalFactory getTerminalFactory(String provName) throws Exception {
        try {
            TerminalFactory factory = (provName == null)
                    ? TerminalFactory.getInstance("PC/SC", null)
                    : TerminalFactory.getInstance("PC/SC", null, provName);
            System.out.println(factory);
            return factory;
        } catch (NoSuchAlgorithmException e) {
            Throwable cause = e.getCause();
            if (cause != null && cause.getMessage().startsWith("PC/SC not available")) {
                return null;
            }
            throw e;
        }
    }

    static CardTerminal getTerminal(String[] args) throws Exception {
        return getTerminal(args, null);
    }

    static CardTerminal getTerminal(String[] args, String provider) throws Exception {
        setLibrary(args);

        try {
            TerminalFactory factory = (provider == null)
                    ? TerminalFactory.getInstance("PC/SC", null)
                    : TerminalFactory.getInstance("PC/SC", null, provider);
            System.out.println(factory);

            List<CardTerminal> terminals = factory.terminals().list();
            System.out.println("Terminals: " + terminals);
            if (terminals.isEmpty()) {
                return null;
            }
            CardTerminal terminal = terminals.get(0);

            if (terminal.isCardPresent() == false) {
                System.out.println("*** Insert card");
                if (terminal.waitForCardPresent(20 * 1000) == false) {
                    throw new Exception("no card available");
                }
            }
            System.out.println("card present: " + terminal.isCardPresent());

            return terminal;

        } catch (NoSuchAlgorithmException e) {
            Throwable cause = e.getCause();
            if (cause != null && cause.getMessage().startsWith("PC/SC not available")) {
                return null;
            }
            throw e;
        }
    }

    static final byte[] C1 = parse("00 A4 04 00 07 A0 00 00 00 62 81 01 00");
    static final byte[] R1a = parse("07 A0 00 00 00 62 81 01 04 01 00 00 24 05 00 0B 04 B0 25 90 00");
    static final byte[] R1b = parse("07 A0 00 00 00 62 81 01 04 01 00 00 24 05 00 0B 04 B0 55 90 00");

    static void transmitTestCommand(CardChannel channel) throws Exception {
        ResponseAPDU r = channel.transmit(new CommandAPDU(C1));
        byte[] rb = r.getBytes();
        if ((Arrays.equals(rb, R1a) == false) && (Arrays.equals(rb, R1b) == false)) {
            System.out.println("expected: " + toString(R1a));
            System.out.println("received: " + toString(rb));
            throw new Exception("Response does not match");
        }
    }

    private final static char[] hexDigits = "0123456789abcdef".toCharArray();

    public static String toString(byte[] b) {
        StringBuffer sb = new StringBuffer(b.length * 3);
        for (int i = 0; i < b.length; i++) {
            int k = b[i] & 0xff;
            if (i != 0) {
                sb.append(':');
            }
            sb.append(hexDigits[k >>> 4]);
            sb.append(hexDigits[k & 0xf]);
        }
        return sb.toString();
    }

    public static byte[] parse(String s) {
        try {
            int n = s.length();
            ByteArrayOutputStream out = new ByteArrayOutputStream(n >> 1);
            StringReader r = new StringReader(s);
            while (true) {
                int b1 = nextNibble(r);
                if (b1 < 0) {
                    break;
                }
                int b2 = nextNibble(r);
                if (b2 < 0) {
                    throw new RuntimeException("Invalid string " + s);
                }
                int b = (b1 << 4) | b2;
                out.write(b);
            }
            return out.toByteArray();
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
            }
        }
    }

}
