/*
 * Copyright (c) 2006, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6429510
 * @summary Verify that our digests work correctly irrespective of input alignment
 * @author Andreas Sterbenz
 */

import java.util.*;

import java.security.*;

public class Offsets {

    private static void outOfBounds(MessageDigest md, int arrayLen, int ofs, int len) throws Exception {
        try {
            md.reset();
            md.update(new byte[arrayLen], ofs, len);
            throw new Exception("invalid call succeeded");
        } catch (RuntimeException e) {
            // ignore
            //System.out.println(e);
        }
    }

    private static void test(String algorithm, int minOfs, int maxOfs, int minLen, int maxLen) throws Exception {
        Random random = new Random();
        MessageDigest md = MessageDigest.getInstance(algorithm, "SUN");
        System.out.println("Testing " + algorithm + "...");
        outOfBounds(md, 16, 0, 32);
        outOfBounds(md, 16, -8, 16);
        outOfBounds(md, 16, 8, -8);
        outOfBounds(md, 16, Integer.MAX_VALUE, 8);
        for (int n = minLen; n <= maxLen; n++) {
            System.out.print(n + " ");
            byte[] data = new byte[n];
            random.nextBytes(data);
            byte[] digest = null;
            for (int ofs = minOfs; ofs <= maxOfs; ofs++) {
                byte[] ofsData = new byte[n + maxOfs];
                random.nextBytes(ofsData);
                System.arraycopy(data, 0, ofsData, ofs, n);
                md.update(ofsData, ofs, n);
                byte[] ofsDigest = md.digest();
                if (digest == null) {
                    digest = ofsDigest;
                } else {
                    if (Arrays.equals(digest, ofsDigest) == false) {
                        throw new Exception("Digest mismatch " + algorithm + ", ofs: " + ofs + ", len: " + n);
                    }
                }
            }
        }
        System.out.println();
    }

    public static void main(String[] args) throws Exception {
        test("MD2", 0, 64, 0, 128);
        test("MD5", 0, 64, 0, 128);
        test("SHA1", 0, 64, 0, 128);
        test("SHA-224", 0, 64, 0, 128);
        test("SHA-256", 0, 64, 0, 128);
        test("SHA-384", 0, 128, 0, 256);
        test("SHA-512", 0, 128, 0, 256);
        test("SHA3-224", 0, 64, 0, 128);
        test("SHA3-256", 0, 64, 0, 128);
        test("SHA3-384", 0, 128, 0, 256);
        test("SHA3-512", 0, 128, 0, 256);
    }

}
