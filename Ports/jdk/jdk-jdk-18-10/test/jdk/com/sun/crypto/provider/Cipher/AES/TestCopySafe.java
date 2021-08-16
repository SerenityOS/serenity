/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8026943 8027575
 * @summary Verify that same buffer can be used as input and output when
 * using Cipher objects.
 * @author Valerie Peng
 */
import java.security.*;
import java.security.spec.*;

import java.util.Arrays;

import javax.crypto.*;
import javax.crypto.spec.*;

public class TestCopySafe {

    private static boolean DEBUG = false;
    private static int INPUT_LENGTH = 160; // must be multiple of block size
    private static byte[] PT = new byte[INPUT_LENGTH];
    private static SecretKey KEY = new SecretKeySpec(new byte[16], "AES");
    private static byte[] IV = new byte[16];

    private static int[] OFFSETS = { 1, 8, 9, 16, 17, 32, 33 };

    private static final String[] MODES = {
        "ECB", "CBC", "PCBC", "CTR", "CTS",
        "CFB", "CFB8", "CFB16", "CFB24", "CFB32", "CFB40",
        "CFB48", "CFB56", "CFB64",
        "OFB", "OFB8", "OFB16", "OFB24", "OFB32", "OFB40",
        "OFB48", "OFB56", "OFB64",
        "GCM"
    };

    public static void main(String[] argv) throws Exception {

        Provider p = Security.getProvider("SunJCE");

        AlgorithmParameterSpec params = null;
        boolean result = true;
        for (String mode : MODES) {
            String transformation = "AES/" + mode + "/NoPadding";
            boolean isGCM = (mode == "GCM");
            if (isGCM) {
                params = new GCMParameterSpec(128, IV);
            } else if (mode != "ECB") {
                params = new IvParameterSpec(IV);
            }
            Cipher c = Cipher.getInstance(transformation, p);
            System.out.println("Testing " + transformation + ":");
            for (int offset : OFFSETS) {
                System.out.print("=> offset " + offset + ": ");
                try {
                    test(c, params, offset, isGCM);
                    System.out.println("Passed");
                } catch(Exception ex) {
                    ex.printStackTrace();
                    result = false;
                    continue;
                }
            }
        }
        if (!result) {
            throw new Exception("One or more test failed");
        }
    }

    private static void test(Cipher c, AlgorithmParameterSpec params,
        int offset, boolean isGCM) throws Exception {

        // Test encryption first
        if (isGCM) {
            // re-init with only key value first to bypass the
            // Key+IV-uniqueness check for GCM encryption
            c.init(Cipher.ENCRYPT_MODE, KEY);
        }
        c.init(Cipher.ENCRYPT_MODE, KEY, params);
        byte[] answer = c.doFinal(PT);
        byte[] pt2 = Arrays.copyOf(PT, answer.length + offset);

        // #1: outOfs = inOfs = 0
        if (isGCM) {
            c.init(Cipher.ENCRYPT_MODE, KEY);
            c.init(Cipher.ENCRYPT_MODE, KEY, params);
        }
        c.doFinal(pt2, 0, PT.length, pt2, 0);
        if (!isTwoArraysEqual(pt2, 0, answer, 0, answer.length)) {
            throw new Exception("Enc#1 diff check failed!");
        } else if (DEBUG) {
            System.out.println("Enc#1 diff check passed");
        }

        // #2: inOfs = 0, outOfs = offset
        System.arraycopy(PT, 0, pt2, 0, PT.length);
        if (isGCM) {
            c.init(Cipher.ENCRYPT_MODE, KEY);
            c.init(Cipher.ENCRYPT_MODE, KEY, params);
        }
        c.doFinal(pt2, 0, PT.length, pt2, offset);
        if (!isTwoArraysEqual(pt2, offset, answer, 0, answer.length)) {
            throw new Exception("Enc#2 diff check failed");
        } else if (DEBUG) {
            System.out.println("Enc#2 diff check passed");
        }

        // #3: inOfs = offset, outOfs = 0
        System.arraycopy(PT, 0, pt2, offset, PT.length);
        if (isGCM) {
            c.init(Cipher.ENCRYPT_MODE, KEY);
            c.init(Cipher.ENCRYPT_MODE, KEY, params);
        }
        c.doFinal(pt2, offset, PT.length, pt2, 0);
        if (!isTwoArraysEqual(pt2, 0, answer, 0, answer.length)) {
            throw new Exception("Enc#3 diff check failed");
        } else if (DEBUG) {
            System.out.println("Enc#3 diff check passed");
        }

       // Test decryption now, we should get back PT as a result
        c.init(Cipher.DECRYPT_MODE, KEY, params);
        pt2 = Arrays.copyOf(answer, answer.length + offset);

        // #1: outOfs = inOfs = 0
        c.doFinal(pt2, 0, answer.length, pt2, 0);
        if (!isTwoArraysEqual(pt2, 0, PT, 0, PT.length)) {
            throw new Exception("Dec#1 diff check failed!");
        } else if (DEBUG) {
            System.out.println("Dec#1 diff check passed");
        }

        // #2: inOfs = 0, outOfs = offset
        System.arraycopy(answer, 0, pt2, 0, answer.length);
        c.doFinal(pt2, 0, answer.length, pt2, offset);
        if (!isTwoArraysEqual(pt2, offset, PT, 0, PT.length)) {
            throw new Exception("Dec#2 diff check failed");
        } else if (DEBUG) {
            System.out.println("Dec#2 diff check passed");
        }

        // #3: inOfs = offset, outOfs = 0
        System.arraycopy(answer, 0, pt2, offset, answer.length);
        c.doFinal(pt2, offset, answer.length, pt2, 0);
        if (!isTwoArraysEqual(pt2, 0, PT, 0, PT.length)) {
            throw new Exception("Dec#3 diff check failed");
        } else if (DEBUG) {
            System.out.println("Dec#3 diff check passed");
        }
    }

    private static boolean isTwoArraysEqual(byte[] a, int aOff, byte[] b, int bOff,
        int len) {
        for (int i = 0; i < len; i++) {
            if (a[aOff + i] != b[bOff + i]) {
                return false;
            }
        }
        return true;
    }
}

