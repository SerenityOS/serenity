/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * utility class
 */

public class TestUtilities {
    public static boolean equalsBlock(byte[] b1, byte[] b2, int len) {
        for (int i = 0; i < len; i++) {
            if (b1[i] != b2[i]) {
                System.err.println("b1[" + i + "] : " + b1[i]
                        + " b2[" + i + "] : " + b2[i]);
                return false;
            }
        }
        return true;
    }

    public static boolean equals(byte[] b1, byte[] b2) {
        if (b2.length != b1.length) {
            System.err.println("b1.length = " + b1.length
                    + " b2.length = " + b2.length );
            return false;
        }
        return equalsBlock(b1, b2, b1.length);
    }

    /**
     * Verify b1's partial part is same as b2. compares b1 and b2 by chopping up
     * b1 into blocks of b1BKSize and b2 into blocks of b2BKSize, and then
     * compare the first b2BKSize bytes of each block, return true if they equal
     * , otherwise return false.
     * @param b1 byte array to be compared.
     * @param b2 saved byte array.
     * @param b1BKSize b1's block size.
     * @param b2BKSize b2's block size.
     * @return true is same. false otherwise.
     */
    public static boolean equalsBlockPartial(byte[] b1, byte[] b2, int b1BKSize,
            int b2BKSize) {
        int numOfBlock = b1.length / b1BKSize;
        for (int b = 0; b < numOfBlock; b++) {
            for (int i = 0; i < b2BKSize; i++) {
                int j1 = b * b1BKSize + i;
                int j2 = b * b2BKSize + i;
                if (b1[j1] != b2[j2]) {
                    System.err.println("Compare failed at b1[" + j1 + "]:" +
                            b1[j1] + " b2[" + j2 + "]:" + b2[j2]);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Generate a byte block by given length. The content of byte block
     * is determined by the index.
     * @param length length of byte array
     * @return a byte array
     */
    public static byte[] generateBytes(int length) {
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) (i & 0xff);
        }
        return bytes;
    }
}
