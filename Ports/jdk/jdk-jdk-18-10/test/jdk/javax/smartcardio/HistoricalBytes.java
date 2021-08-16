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
 * @summary Verify that ATR.getHistoricalBytes() works
 * @author Andreas Sterbenz
 */

import java.util.Arrays;

import javax.smartcardio.*;

public class HistoricalBytes {

    public static String toString(byte[] b) {
        return Serialize.toString(b);
    }

    public static byte[] parse(String s) {
        return Serialize.parse(s);
    }

    // generated using ATR_analysis from pcsc-tools

    private final static String[] ATRS = {
        "3B 7F 18 00 00 00 31 C0 73 9E 01 0B 64 52 D9 04 00 82 90 00",
        "3B 65 00 00 9C 02 02 07 02",
        "3B 95 18 40 FF 62 01 02 01 04",
        "3B 86 40 20 68 01 01 02 04 AC",
        "3B 9F 96 80 1F C3 80 31 E0 73 FE 21 1B B3 E2 02 7E 83 0F 90 00 82",
        "3B FF 13 00 FF 81 31 FE 5D 80 25 A0 00 00 00 56 57 44 4B 33 32 30 05 00 3F",
        "3F 6D 00 00 80 31 80 65 B0 05 01 02 5E 83 00 90 00",
        "3F 65 35 64 02 04 6C 90 40",
        "3B 9F 96 80 1F C3 80 31 E0 73 FE 21 1B B3 E2 02 7E 83 0F 90 00 82 11",
        "3F 65 35 64 02 04 6C 90 40 55 55", // invalid
    };

    private final static String[] HIST = {
        "00 31 C0 73 9E 01 0B 64 52 D9 04 00 82 90 00",
        "9C 02 02 07 02",
        "62 01 02 01 04",
        "68 01 01 02 04 AC",
        "80 31 E0 73 FE 21 1B B3 E2 02 7E 83 0F 90 00",
        "80 25 A0 00 00 00 56 57 44 4B 33 32 30 05 00",
        "80 31 80 65 B0 05 01 02 5E 83 00 90 00",
        "02 04 6C 90 40",
        "",
        "",
    };

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < ATRS.length; i++) {
            ATR atr = new ATR(parse(ATRS[i]));
            byte[] hist = parse(HIST[i]);
            byte[] b = atr.getHistoricalBytes();
            if (!Arrays.equals(b, hist)) {
                throw new Exception("mismatch: " + toString(b) + " != " + toString(hist));
            }
        }
        System.out.println("OK");
    }

}
