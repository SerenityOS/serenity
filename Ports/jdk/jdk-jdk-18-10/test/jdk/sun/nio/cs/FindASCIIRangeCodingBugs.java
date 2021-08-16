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

/* @test
 * @bug 6378295
 * @summary Roundtrip Encoding/Decoding of ASCII chars from 0x00-0x7f
 */

import java.util.*;
import java.nio.*;
import java.nio.charset.*;

public class FindASCIIRangeCodingBugs {
    private static int failures = 0;
    private static byte[] asciiBytes = new byte[0x80];
    private static char[] asciiChars = new char[0x80];
    private static String asciiString;

    private static void check(String csn) throws Exception {
        System.out.println(csn);
        if (! Arrays.equals(asciiString.getBytes(csn), asciiBytes)) {
            System.out.printf("%s -> bytes%n", csn);
            failures++;
        }
        if (! new String(asciiBytes, csn).equals(asciiString)) {
            System.out.printf("%s -> chars%n", csn);
            failures++;
        }
    }

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 0x80; i++) {
            asciiBytes[i] = (byte) i;
            asciiChars[i] = (char) i;
        }
        asciiString = new String(asciiChars);
        Charset ascii = Charset.forName("ASCII");
        for (Map.Entry<String,Charset> e
                 : Charset.availableCharsets().entrySet()) {
            String csn = e.getKey();
            Charset cs = e.getValue();
            if (!cs.contains(ascii) ||
                csn.matches(".*2022.*") ||             //iso2022 family
                csn.matches("x-windows-5022[0|1]") ||  //windows 2022jp
                csn.matches(".*UTF-[16|32].*"))        //multi-bytes
                continue;
            if (! cs.canEncode()) continue;
            try {
                check(csn);
            } catch (Throwable t) {
                t.printStackTrace();
                failures++;
            }
        }
        if (failures > 0)
            throw new Exception(failures + "tests failed");
    }
}
