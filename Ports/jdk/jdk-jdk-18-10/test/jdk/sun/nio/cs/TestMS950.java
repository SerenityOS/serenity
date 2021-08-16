/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8232161
 * @summary Those test data confirm the preferred b2c irreversible mappings defined in MS950.nr file.
 */

import java.nio.charset.Charset;
import java.nio.CharBuffer;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class TestMS950 {
    // Data is listed from make/data/charsetmapping/MS950.map
    private final static String[] MS950B2C = new String[] {
        "0xF9FA  0x256D",
        "0xF9FB  0x256E",
        "0xF9FC  0x2570",
        "0xF9FD  0x256F",
        "0xA2CC  0x5341",
        "0xA2CE  0x5345",
        "0xF9F9  0x2550",
        "0xF9E9  0x255E",
        "0xF9EA  0x256A",
        "0xF9EB  0x2561",
        "0xA27E  0x256D",
        "0xA2A1  0x256E",
        "0xA2A2  0x2570",
        "0xA2A3  0x256F",
        "0xA451  0x5341",
        "0xA4CA  0x5345",
        "0xA2A4  0x2550",
        "0xA2A5  0x255E",
        "0xA2A6  0x256A",
        "0xA2A7  0x2561",
    };

    // Data is listed from MS950.map
    // Col1 should be in MS950.nr
    // (Only check col2 and col3)
    private final static String[] MS950C2B= new String[] {
        "0xF9FA -> u256D -> 0xA27E",
        "0xF9FB -> u256E -> 0xA2A1",
        "0xF9FC -> u2570 -> 0xA2A2",
        "0xF9FD -> u256F -> 0xA2A3",
        "0xA2CC -> u5341 -> 0xA451",
        "0xA2CE -> u5345 -> 0xA4CA",
        "0xA2A4 -> u2550 -> 0xF9F9",
        "0xA2A5 -> u255E -> 0xF9E9",
        "0xA2A6 -> u256A -> 0xF9EA",
        "0xA2A7 -> u2561 -> 0xF9EB",
    };

    // Convert Hex string to byte array
    private static byte[] hex2ba(String s) {
        byte[] ba;
        if (s.startsWith("0x")) {
            s = s.substring(2);
        }
        try {
            ByteBuffer bb = ByteBuffer.allocate((int)(s.length()/2));
            StringBuilder sb = new StringBuilder(s.substring(0, bb.limit() * 2));
            while (sb.length() > 0) {
                bb.put((byte)Integer.parseInt(sb.substring(0, 2), 16));
                sb.delete(0, 2);
            }
            ba = bb.array();
        } catch (NumberFormatException nfe) {
            ba = new byte[0];
        }
        return ba;
    }

    // Convert Hex string to string
    private static String hex2s(String s) {
        char[] ca;
        if (s.startsWith("0x")) {
            s = s.substring(2);
        } else if (s.startsWith("u")) {
            s = s.substring(1);
        }
        try {
            CharBuffer cb = CharBuffer.allocate((int)(s.length()/4));
            StringBuilder sb = new StringBuilder(s.substring(0, cb.limit() * 4));
            while (sb.length() > 0) {
                cb.put((char)Integer.parseInt(sb.substring(0, 4), 16));
                sb.delete(0,4);
            }
            ca = cb.array();
        } catch (NumberFormatException nfe) {
            ca = new char[0];
        }
        return new String(ca);
    }

    public static void main(String[] args) throws Exception {
        Charset cs = Charset.forName("MS950");
        int diffs = 0;
        // Check b2c side: Duplicated entries
        for(int i = 0; i < MS950B2C.length; ++i) {
            String[] sa = MS950B2C[i].split("\\s+");
            String s = new String(hex2ba(sa[0]), cs);
            if (!s.equals(hex2s(sa[1]))) {
                ++diffs;
                System.out.printf("b2c: %s, expected:%s, result:0x", sa[0], sa[1]);
                for (char ch : s.toCharArray()) {
                    System.out.printf("%04X", (int)ch);
                }
                System.out.println();
            }
        }
        // Check c2b side: round-trip entries
        for(int i = 0; i < MS950C2B.length; ++i) {
            String[] sa = MS950C2B[i].split("\\s+->\\s+");
            byte[] ba = hex2s(sa[1]).getBytes(cs);
            if (!Arrays.equals(ba, hex2ba(sa[2]))) {
                ++diffs;
                System.out.printf("c2b: %s, expected:%s, result:0x", sa[1], sa[2]);
                for (byte b : ba) {
                    System.out.printf("%02X", (int)b & 0xFF);
                }
                System.out.println();
            }
        }
        if (diffs > 0) {
            throw new Exception("Failed");
        }
    }
}
