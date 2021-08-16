/*
 * Copyright (c) 1998, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.security.SecureRandom;
import javax.crypto.spec.SecretKeySpec;

/**
 * Helper class.
 */
class Utils {

    static final int KEY_SIZE = 70;

    static final String[] MAC_ALGOS = {"HmacMD5", "HmacSHA1", "HmacSHA224",
        "HmacSHA256", "HmacSHA384", "HmacSHA512"};

    /**
     * Get SecretKeySpec.
     */
    static SecretKeySpec getSecretKeySpec() {
        SecureRandom srdm = new SecureRandom();
        byte[] keyVal = new byte[KEY_SIZE];
        srdm.nextBytes(keyVal);
        return new SecretKeySpec(keyVal, "HMAC");
    }

    static void runTests(MacTest... tests) {
        boolean success = true;
        for (MacTest test : tests) {
            success &= runTest(test);
        }

        if (success) {
            System.out.println("Test passed");
        } else {
            throw new RuntimeException("Test failed");
        }
    }

    private static boolean runTest(MacTest test) {
        boolean success = true;
        for (String alg : MAC_ALGOS) {
            try {
                System.out.println("Test " + alg);
                test.doTest(alg);
            } catch (Exception e) {
                System.out.println("Unexpected exception:");
                e.printStackTrace();
                success = false;
            }
        }

        return success;
    }
}

interface MacTest {
    void doTest(String alg) throws Exception;
}
