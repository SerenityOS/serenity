/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 0000000
 * @library ../UTIL
 * @build TestUtil
 * @run main BlowfishTestVector
 * @summary Known Answer Test for Blowfish cipher with ECB mode
 * @author Jan Luehe
 */

import java.security.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public class BlowfishTestVector {

    // test vector #1 (checking for the "signed" bug)
    // (ECB mode)
    private static final byte[] TEST_KEY_1 = new byte[] {
        (byte)0x1c, (byte)0x58, (byte)0x7f, (byte)0x1c,
        (byte)0x13, (byte)0x92, (byte)0x4f, (byte)0xef
    };
    private static final byte[] TV_P1 = new byte[] {
        (byte)0x30, (byte)0x55, (byte)0x32, (byte)0x28,
        (byte)0x6d, (byte)0x6f, (byte)0x29, (byte)0x5a
    };
    private static final byte[] TV_C1 = new byte[] {
        (byte)0x55, (byte)0xcb, (byte)0x37, (byte)0x74,
        (byte)0xd1, (byte)0x3e, (byte)0xf2, (byte)0x01
    };

    // test vector #2 (offical vector by Bruce Schneier)
    // (ECB mode)
    private static final String S_TEST_KEY_2 = "Who is John Galt?";

    private static final byte[] TV_P2 = new byte[] {
        (byte)0xfe, (byte)0xdc, (byte)0xba, (byte)0x98,
        (byte)0x76, (byte)0x54, (byte)0x32, (byte)0x10
    };
    private static final byte[] TV_C2 = new byte[] {
        (byte)0xcc, (byte)0x91, (byte)0x73, (byte)0x2b,
        (byte)0x80, (byte)0x22, (byte)0xf6, (byte)0x84
    };

    public static void main(String[] argv) throws Exception {

        String transformation = "Blowfish/ECB/NoPadding";
        Cipher cipher = Cipher.getInstance(transformation, "SunJCE");
        int MAX_KEY_SIZE = Cipher.getMaxAllowedKeyLength(transformation);
        //
        // test 1
        //
        if (TEST_KEY_1.length*8 <= MAX_KEY_SIZE) {
            SecretKey sKey = new SecretKeySpec(TEST_KEY_1, "Blowfish");
            try {
                cipher.init(Cipher.ENCRYPT_MODE, sKey);
                byte[] c1 = cipher.doFinal(TV_P1);
                if (!Arrays.equals(c1, TV_C1))
                    throw new Exception("Encryption (Test vector 1) failed");

                cipher.init(Cipher.DECRYPT_MODE, sKey);
                byte[] p1 = cipher.doFinal(c1);
                if (!Arrays.equals(p1, TV_P1))
                    throw new Exception("Decryption (Test vector 1) failed");
            } catch (SecurityException se) {
                TestUtil.handleSE(se);
            }
        }
        //
        // test 2
        //
        byte[] testKey2 = S_TEST_KEY_2.getBytes();
        if (testKey2.length*8 <= MAX_KEY_SIZE) {
            SecretKey sKey = new SecretKeySpec(testKey2, "Blowfish");
            try {
                cipher.init(Cipher.ENCRYPT_MODE, sKey);
                byte[] c2 = cipher.doFinal(TV_P2);
                if (!Arrays.equals(c2, TV_C2))
                    throw new Exception("Encryption (Test vector 2) failed");

                cipher.init(Cipher.DECRYPT_MODE, sKey);
                byte[] p2 = cipher.doFinal(c2);
                if (!Arrays.equals(p2, TV_P2))
                    throw new Exception("Decryption (Test vector 2) failed");
            } catch (SecurityException se) {
                TestUtil.handleSE(se);
            }
        }
        System.out.println("Test passed");
    }
}
