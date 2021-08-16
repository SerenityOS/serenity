/*
 * Copyright (c) 2006, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 6445367
 * @summary make sure serialization works
 * @author Andreas Sterbenz
 */

import java.io.*;

import javax.smartcardio.*;

public class Serialize {

    public static void main(String[] args) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(bout);

        CommandAPDU c1 = new CommandAPDU(parse("00 A4 04 00 07 A0 00 00 00 62 81 01 00"));
        ResponseAPDU r1 = new ResponseAPDU(parse("07 A0 00 00 00 62 81 01 04 01 00 00 24 05 00 0B 04 B0 25 90 00"));
        ATR a1 = new ATR(parse("3B 7F 18 00 00 00 31 C0 73 9E 01 0B 64 52 D9 04 00 82 90 00"));

        oout.writeObject(c1);
        oout.writeObject(r1);
        oout.writeObject(a1);
        oout.close();

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        ObjectInputStream oin = new ObjectInputStream(bin);

        CommandAPDU c2 = (CommandAPDU)oin.readObject();
        ResponseAPDU r2 = (ResponseAPDU)oin.readObject();
        ATR a2 = (ATR)oin.readObject();

        if (!c2.equals(c1)) {
            throw new Exception("CommandAPDU not equal");
        }
        if (c2.getNc() != 7) {
            throw new Exception("Nc mismatch: " + c2.getNc());
        }
        if (c2.getNe() != 256) {
            throw new Exception("Ne mismatch: " + c2.getNe());
        }
        if (c2.getINS() != 0xA4) {
            throw new Exception("INS mismatch: " + c2.getINS());
        }
        if (!r2.equals(r1)) {
            throw new Exception("ResponseAPDU not equal");
        }
        if (r2.getSW1() != 0x90) {
            throw new Exception("SW1 mismatch: " + r2.getSW1());
        }
        if (!a2.equals(a1)) {
            throw new Exception("ATR not equal");
        }
        if (!java.util.Arrays.equals(a2.getHistoricalBytes(), a1.getHistoricalBytes())) {
            throw new Exception("Historical bytes mismatch");
        }
        System.out.println("OK");
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
