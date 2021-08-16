/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.Asserts;

import java.security.MessageDigest;
import java.util.Arrays;

/**
 * @test
 * @bug 8051408
 * @library /test/lib
 * @summary testing SHA-512/224 and SHA-512/256.
 */
public class SHA512 {
    public static void main(String[] args) throws Exception {

        MessageDigest md;

        // Test vectors obtained from
        // http://csrc.nist.gov/groups/ST/toolkit/documents/Examples/SHA512_224.pdf
        md = MessageDigest.getInstance("SHA-512/224");
        Asserts.assertTrue(Arrays.equals(md.digest("abc".getBytes()),
            xeh("4634270F 707B6A54 DAAE7530 460842E2 0E37ED26 5CEEE9A4 3E8924AA")));
        Asserts.assertTrue(Arrays.equals(md.digest((
                "abcdefghbcdefghicdefghijdefghijkefghijklfghijklmghijklmn" +
                "hijklmnoijklmnopjklmnopqklmnopqrlmnopqrsmnopqrstnopqrstu").getBytes()),
            xeh("23FEC5BB 94D60B23 30819264 0B0C4533 35D66473 4FE40E72 68674AF9")));

        // Test vectors obtained from
        // http://csrc.nist.gov/groups/ST/toolkit/documents/Examples/SHA512_256.pdf
        md = MessageDigest.getInstance("SHA-512/256");
        Asserts.assertTrue(Arrays.equals(md.digest("abc".getBytes()),
            xeh("53048E26 81941EF9 9B2E29B7 6B4C7DAB E4C2D0C6 34FC6D46 E0E2F131 07E7AF23")));
        Asserts.assertTrue(Arrays.equals(md.digest((
                "abcdefghbcdefghicdefghijdefghijkefghijklfghijklmghijklmn" +
                "hijklmnoijklmnopjklmnopqklmnopqrlmnopqrsmnopqrstnopqrstu").getBytes()),
            xeh("3928E184 FB8690F8 40DA3988 121D31BE 65CB9D3E F83EE614 6FEAC861 E19B563A")));
    }

    static byte[] xeh(String in) {
        in = in.replaceAll(" ", "");
        int len = in.length() / 2;
        byte[] out = new byte[len];
        for (int i = 0; i < len; i++) {
            out[i] = (byte)Integer.parseInt(in.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }
}
