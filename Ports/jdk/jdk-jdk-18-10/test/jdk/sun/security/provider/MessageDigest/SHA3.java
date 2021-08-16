/*
 * Copyright (c) 2020, Huawei Technologies Co., Ltd. All rights reserved.
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
 * @bug 8252204
 * @library /test/lib
 * @summary testing SHA3-224/256/384/512.
 */
public class SHA3 {

    static byte[] msg1600bits;
    static {
        msg1600bits = new byte[200];
        for (int i = 0; i < 200; i++)
            msg1600bits[i] = (byte) 0xa3;
    }

    public static void main(String[] args) throws Exception {

        MessageDigest md;

        // Test vectors obtained from
        // https://csrc.nist.gov/CSRC/media/Projects/Cryptographic-Standards-and-Guidelines/documents/examples/SHA3-224_Msg0.pdf
        md = MessageDigest.getInstance("SHA3-224");
        Asserts.assertTrue(Arrays.equals(md.digest("".getBytes()),
                                         xeh("6B4E0342 3667DBB7 3B6E1545 4F0EB1AB D4597F9A 1B078E3F 5B5A6BC7")));
        // Test vectors obtained from
        // https://csrc.nist.gov/CSRC/media/Projects/Cryptographic-Standards-and-Guidelines/documents/examples/SHA3-224_1600.pdf
        Asserts.assertTrue(Arrays.equals(md.digest(msg1600bits),
                                         xeh("9376816A BA503F72 F96CE7EB 65AC095D EEE3BE4B F9BBC2A1 CB7E11E0")));

        // Test vectors obtained from
        // https://csrc.nist.gov/CSRC/media/Projects/Cryptographic-Standards-and-Guidelines/documents/examples/SHA3-256_Msg0.pdf
        md = MessageDigest.getInstance("SHA3-256");
        Asserts.assertTrue(Arrays.equals(md.digest("".getBytes()),
                                         xeh("A7FFC6F8 BF1ED766 51C14756 A061D662 F580FF4D E43B49FA 82D80A4B 80F8434A")));
        // Test vectors obtained from
        // https://csrc.nist.gov/CSRC/media/Projects/Cryptographic-Standards-and-Guidelines/documents/examples/SHA3-256_1600.pdf
        Asserts.assertTrue(Arrays.equals(md.digest(msg1600bits),
                                         xeh("79F38ADE C5C20307 A98EF76E 8324AFBF D46CFD81 B22E3973 C65FA1BD 9DE31787")));

        // Test vectors obtained from
        // https://csrc.nist.gov/CSRC/media/Projects/Cryptographic-Standards-and-Guidelines/documents/examples/SHA3-384_Msg0.pdf
        md = MessageDigest.getInstance("SHA3-384");
        Asserts.assertTrue(Arrays.equals(md.digest("".getBytes()),
                                         xeh("0C63A75B 845E4F7D 01107D85 2E4C2485 C51A50AA AA94FC61 995E71BB EE983A2A" +
                                             "C3713831 264ADB47 FB6BD1E0 58D5F004")));
        // Test vectors obtained from
        // https://csrc.nist.gov/CSRC/media/Projects/Cryptographic-Standards-and-Guidelines/documents/examples/SHA3-384_1600.pdf
        Asserts.assertTrue(Arrays.equals(md.digest(msg1600bits),
                                         xeh("1881DE2C A7E41EF9 5DC4732B 8F5F002B 189CC1E4 2B74168E D1732649 CE1DBCDD" +
                                             "76197A31 FD55EE98 9F2D7050 DD473E8F")));

        // Test vectors obtained from
        // https://csrc.nist.gov/CSRC/media/Projects/Cryptographic-Standards-and-Guidelines/documents/examples/SHA3-512_Msg0.pdf
        md = MessageDigest.getInstance("SHA3-512");
        Asserts.assertTrue(Arrays.equals(md.digest("".getBytes()),
                                         xeh("A69F73CC A23A9AC5 C8B567DC 185A756E 97C98216 4FE25859 E0D1DCC1 475C80A6" +
                                             "15B2123A F1F5F94C 11E3E940 2C3AC558 F500199D 95B6D3E3 01758586 281DCD26")));
        // Test vectors obtaned from
        // https://csrc.nist.gov/CSRC/media/Projects/Cryptographic-Standards-and-Guidelines/documents/examples/SHA3-512_1600.pdf
        Asserts.assertTrue(Arrays.equals(md.digest(msg1600bits),
                                         xeh("E76DFAD2 2084A8B1 467FCF2F FA58361B EC7628ED F5F3FDC0 E4805DC4 8CAEECA8" +
                                             "1B7C13C3 0ADF52A3 65958473 9A2DF46B E589C51C A1A4A841 6DF6545A 1CE8BA00")));
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
