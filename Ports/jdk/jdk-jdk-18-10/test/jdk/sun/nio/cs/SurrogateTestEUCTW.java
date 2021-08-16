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
   @bug 4847097
   @summary Check surrogate coverage of EUC_TW
 */

/*
 * Tests the full surrogate mapping roundtrip fidelity of the
 * EUC-TW charset coder updated to support the additional
 * planes 4,5,6,7,15
 *
 * byte->char mappings are contained in external files
 * using plane{x}.surrogate as the convention for the input filenames
 *
 */

import java.io.*;
public class SurrogateTestEUCTW {

    private static final String testRootDir
        = System.getProperty("test.src", ".");

    public static void main(String[] args) throws Exception {
        char[] surrogatePair = new char[2];
        int[] expectBytes = new int[4];

        // Iterate test over each supported CNS-11643 plane
        // containing supplementary character mappings

        String[] testPlane = { "3", "4", "5", "6" ,"7", "15" };

        for (int i = 0 ; i < testPlane.length; i++) {
            FileReader f = new FileReader(testRootDir +
                                          System.getProperty("file.separator")
                                          + "SurrogateTestEUCTW.plane"
                                          + testPlane[i]
                                          + ".surrogates");
            BufferedReader r = new BufferedReader(f);
            String line;

            while ((line = r.readLine()) != null) {
                int charValue = Integer.parseInt(line.substring(9,14), 16);
                surrogatePair[0] = (char) ((charValue - 0x10000) / 0x400
                                    + 0xd800);
                surrogatePair[1] = (char) ((charValue - 0x10000) % 0x400
                                    + 0xdc00);
                // Synthesize 4 byte expected byte values from CNS input values
                expectBytes[0] = 0x8E;
                expectBytes[1] = 0xA0 + Integer.parseInt(testPlane[i]);
                expectBytes[2] = 0x80 | Integer.parseInt(line.substring(2,4), 16);
                expectBytes[3] = 0x80 | Integer.parseInt(line.substring(4,6), 16);

                String testStr = new String(surrogatePair);
                byte[] encodedBytes = testStr.getBytes("EUC-TW");

                for (int x = 0 ; x < 4 ; x++) {
                    if (encodedBytes[x] != (byte)(expectBytes[x] & 0xff)) {
                        throw new Exception("EUC_TW Surrogate Encoder error");
                    }
                }

                // Next: test round-trip fidelity
                String decoded = new String(encodedBytes, "EUC-TW");

                if (!decoded.equals(testStr)) {
                    throw new Exception("EUCTW Decoder error");
                }
            }
            r.close();
            f.close();
        }
    }
}
