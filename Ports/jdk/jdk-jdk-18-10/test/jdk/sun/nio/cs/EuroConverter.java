/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      4114080 8186803
 * @summary  Make sure the euro converters, which are derived from
 * existing converters, only differ from their parents at the expected
 * code point.
 * @modules jdk.charsets
 */

import java.text.*;
import java.util.*;
import java.io.*;

/* Author: Alan Liu
 * 7/14/98
 */
public class EuroConverter {
    public static void main(String args[]) throws Exception {
        boolean pass = true;
        char[] map = new char[256]; // map for the encoding
        byte[] bytes = new byte[1]; // scratch
        char[] chars = new char[1]; // scratch
        for (int i=0; i<DATA.length; ) {
            String euroEnc = DATA[i++];
            String parentEnc = DATA[i++];
            System.out.println("Checking encoder " + euroEnc + " against " + parentEnc);
            String currentEnc = parentEnc;

            try {
                // Fill map with parent values
                for (int j=-128; j<128; ++j) {
                    bytes[0] = (byte)j;
                    char parentValue = new String(bytes, parentEnc).charAt(0);
                    // NOTE: 0x25 doesn't round trip on the EBCDIC code pages,
                    // so we don't check that code point in the sanity check.
                    if (j != 0x0025) {
                        chars[0] = parentValue;
                        int parentRoundTrip = new String(chars).getBytes(parentEnc)[0];
                        // This is a sanity check -- we aren't really testing the parent
                        // encoder here.
                        if (parentRoundTrip != j) {
                            pass = false;
                            System.out.println("Error: Encoder " + parentEnc +
                                           " fails round-trip: " + j +
                                           " -> \\u" + Integer.toHexString(parentValue) +
                                           " -> " + parentRoundTrip);
                        }
                    }
                    map[(j+0x100)&0xFF] = parentValue;
                }

                // Modify map with new expected values.  Each pair has code point, parent value, euro value.
                // Terminated by null.
                while (DATA[i] != null) {
                    int codePoint = Integer.valueOf(DATA[i++], 16).intValue();
                    char expectedParentValue = DATA[i++].charAt(0);
                    char expectedEuroValue = DATA[i++].charAt(0);
                    // This is a sanity check -- we aren't really testing the parent
                    // encoder here.
                    if (map[codePoint] != expectedParentValue) {
                        pass = false;
                        System.out.println("Error: Encoder " + parentEnc +
                                           " " + Integer.toHexString(codePoint) + " -> \\u" +
                                           Integer.toHexString(map[codePoint]) + ", expected \\u" +
                                           Integer.toHexString(expectedParentValue));
                    }
                    // Fill in new expected value
                    map[codePoint] = expectedEuroValue;
                }
                ++i; // Skip over null at end of set

                // Now verify the euro encoder
                currentEnc = euroEnc;
                for (int j=-128; j<128; ++j) {
                    bytes[0] = (byte)j;
                    char euroValue = new String(bytes, euroEnc).charAt(0);
                    chars[0] = euroValue;
                    // NOTE: 0x25 doesn't round trip on the EBCDIC code pages,
                    // so we don't check that code point in the sanity check.
                    if (j != 0x0025) {
                        int euroRoundTrip = new String(chars).getBytes(euroEnc)[0];
                        if (euroRoundTrip != j) {
                            pass = false;
                            System.out.println("Error: Encoder " + euroEnc +
                                           " fails round-trip at " + j);
                        }
                    }
                    // Compare against the map
                    if (euroValue != map[(j+0x100)&0xFF]) {
                        pass = false;
                        System.out.println("Error: Encoder " + euroEnc +
                                           " " + Integer.toHexString((j+0x100)&0xFF) + " -> \\u" +
                                           Integer.toHexString(euroValue) + ", expected \\u" +
                                           Integer.toHexString(map[(j+0x100)&0xFF]));
                    }
                }
            } catch (UnsupportedEncodingException e) {
                System.out.println("Unsupported encoding " + currentEnc);
                pass = false;
                while (i < DATA.length && DATA[i] != null) ++i;
                ++i; // Skip over null
            }
        }
        if (!pass) {
            throw new RuntimeException("Bug 4114080 - Euro encoder test failed");
        }
    }
    static String[] DATA = {
        // New converter, parent converter, [ code point that changed, parent code point value,
        // euro code point value ], null
        // Any number of changed code points may be specified, including zero.
        "ISO8859_15_FDIS", "ISO8859_1",
            "A4", "\u00A4", "\u20AC",
            "A6", "\u00A6", "\u0160",
            "A8", "\u00A8", "\u0161",
            "B4", "\u00B4", "\u017D",
            "B8", "\u00B8", "\u017E",
            "BC", "\u00BC", "\u0152",
            "BD", "\u00BD", "\u0153",
            "BE", "\u00BE", "\u0178",
            null,
        // 923 is IBM's name for ISO 8859-15; make sure they're identical
        "Cp923", "ISO8859_15_FDIS", null,
        "Cp858", "Cp850", "D5", "\u0131", "\u20AC", null,
        "Cp1140", "Cp037", "9F", "\u00A4", "\u20AC", null,
        "Cp1141", "Cp273", "9F", "\u00A4", "\u20AC", null,
        "Cp1142", "Cp277", "5A", "\u00A4", "\u20AC", null,
        "Cp1143", "Cp278", "5A", "\u00A4", "\u20AC", null,
        "Cp1144", "Cp280", "9F", "\u00A4", "\u20AC", null,
        "Cp1145", "Cp284", "9F", "\u00A4", "\u20AC", null,
        "Cp1146", "Cp285", "9F", "\u00A4", "\u20AC", null,
        "Cp1147", "Cp297", "9F", "\u00A4", "\u20AC", null,
        "Cp1148", "Cp500", "9F", "\u00A4", "\u20AC", null,
        "Cp1149", "Cp871", "9F", "\u00A4", "\u20AC", null,
    };
}
