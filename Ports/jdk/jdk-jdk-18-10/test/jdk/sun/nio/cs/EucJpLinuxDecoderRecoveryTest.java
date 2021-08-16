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
   @bug 5016049
   @summary ensure euc-jp-linux charset decoder recovery for unmappable input
 */

import java.io.*;

public class EucJpLinuxDecoderRecoveryTest {
    public static void main(String[] args) throws Exception {
        byte[] encoded = {
                // EUC_JP_LINUX mappable JIS X 0208 range
                (byte)0xa6, (byte)0xc5,
                // EUC_JP_LINUX Unmappable (JIS X 0212 range)
                (byte)0x8f, (byte)0xa2, (byte)0xb7,
                // EUC_JP_LINUX mappable JIS X 0208 range
                (byte)0xa6, (byte)0xc7 };

        char[] decodedChars = new char[3];
        char[] expectedChars =
                        {
                        '\u03B5',  // mapped
                        '\ufffd',  // unmapped
                        '\u03B7'   // mapped
                        };

        ByteArrayInputStream bais = new ByteArrayInputStream(encoded);
        InputStreamReader isr = new InputStreamReader(bais, "EUC_JP_LINUX");
        int n = 0;   // number of chars decoded

        try {
            n = isr.read(decodedChars);
        } catch (Exception ex) {
            throw new Error("euc-jp-linux decoding broken");
        }

        // check number of decoded chars is what is expected
        if (n != expectedChars.length)
            throw new Error("Unexpected number of chars decoded");

        // Compare actual decoded with expected

        for (int i = 0; i < n; i++) {
            if (expectedChars[i] != decodedChars[i])
                throw new Error("euc-jp-linux decoding incorrect");
        }
    }
}
