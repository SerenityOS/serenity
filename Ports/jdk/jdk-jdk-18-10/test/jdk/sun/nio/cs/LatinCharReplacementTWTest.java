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
   @bug 4658679 4879644
   @summary Checks replacement logic within EUC-TW decoder
*/

/*
 * Tests goodness of fix for bugID 4658679: EUC-TW decoder should
 * perform replacement when it encounters latin chars outside the
 * normal US-ASCII range. For example: Isolated occurrences of
 * French accented chars. See bugID: 4658679.
 */
import java.io.*;
public class LatinCharReplacementTWTest {
    public static void  main(String[] args) throws Exception {
        final String bugID = "4658679";
        // Attempt to decode
        byte[] input = { (byte)0xa1,
                         (byte)0xf0,
                         (byte)'r',
                         (byte)'e',
                         (byte)'s',
                         (byte)0xe9,  // illegal within EUC-TW
                         (byte)'r',
                         (byte)'v',
                         (byte)0xe9,  // illegal within EUC-TW
                         (byte)'s',
                         (byte)0xa2,
                         (byte)0xf8
                       };

        char[] expected = { (char) 0xa7,
                         (char) 'r',
                         (char) 'e',
                         (char) 's',
                         (char) 0xFFFD,  // replacement for accented lowercase e
                         (char) 'r',
                         (char) 'v',
                         (char) 0xFFFD,  // replacement for accented lowercase e
                         (char) 's',
                         (char) 0xb0 };

        ByteArrayInputStream bais = new ByteArrayInputStream(input);
        InputStreamReader isr = new InputStreamReader(bais, "x-EUC-TW");

        char[] decoded = new char[128];
        int numChars = isr.read(decoded);

        if (numChars != expected.length) {
            throw new Exception("failure of test for bug " + bugID);
        }

        for (int i = 0 ; i < numChars; i++) {
           if (decoded[i] != expected[i])
                throw new Exception("failure of test for bug " + bugID);
        }
    }
}
