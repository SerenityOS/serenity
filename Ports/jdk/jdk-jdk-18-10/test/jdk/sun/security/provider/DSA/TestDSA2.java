/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7044060 8042967
 * @run main/othervm/timeout=250 TestDSA2
 * @summary verify that DSA signature works using SHA and SHA-224 and
 *          SHA-256 digests.
 * @key randomness
 */


import java.security.*;
import java.security.spec.*;
import java.security.interfaces.*;

public class TestDSA2 {

    // NOTE: need to explictly specify provider since the more
    // preferred provider SunPKCS11 provider only supports up
    // 1024 bits.
    private static final String PROV = "SUN";

    private static final String[] SIG_ALGOS = {
        "NONEwithDSA",
        "SHA1withDSA",
        "SHA224withDSA",
        "SHA256withDSA",
        "NONEwithDSAinP1363Format",
        "SHA1withDSAinP1363Format",
        "SHA224withDSAinP1363Format",
        "SHA256withDSAinP1363Format"
    };

    private static final int[] KEYSIZES = {
        1024, 2048
    };

    public static void main(String[] args) throws Exception {
        boolean[] expectedToPass = { true, true, true, true,
                                     true, true, true, true };
        test(1024, expectedToPass);
        boolean[] expectedToPass2 = { true, false, true, true,
                                      true, false, true, true };
        test(2048, expectedToPass2);
    }

    private static void test(int keySize, boolean[] testStatus)
            throws Exception {
        // Raw DSA requires the data to be exactly 20 bytes long. Use a
        // 20-byte array for these tests so that the NONEwithDSA* algorithms
        // don't complain.
        byte[] data = "12345678901234567890".getBytes();
        System.out.println("Test against key size: " + keySize);

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA", PROV);
        keyGen.initialize(keySize, new SecureRandom());
        KeyPair pair = keyGen.generateKeyPair();

        if (testStatus.length != SIG_ALGOS.length) {
            throw new RuntimeException("TestError: incorrect status array!");
        }
        for (int i = 0; i < SIG_ALGOS.length; i++) {
            Signature dsa = Signature.getInstance(SIG_ALGOS[i], PROV);
            try {
                dsa.initSign(pair.getPrivate());
                dsa.update(data);
                byte[] sig = dsa.sign();
                dsa.initVerify(pair.getPublic());
                dsa.update(data);
                boolean verifies = dsa.verify(sig);
                if (verifies == testStatus[i]) {
                    System.out.println(SIG_ALGOS[i] + ": Passed");
                } else {
                    System.out.println(SIG_ALGOS[i] + ": should " +
                                       (testStatus[i]? "pass":"fail"));
                    throw new RuntimeException(SIG_ALGOS[i] + ": Unexpected Test result!");

                }
            } catch (Exception ex) {
                if (testStatus[i]) {
                    ex.printStackTrace();
                    throw new RuntimeException(SIG_ALGOS[i] + ": Unexpected exception " + ex);
                } else {
                    System.out.println(SIG_ALGOS[i] + ": Passed, expected " + ex);
                }
            }
        }
    }
}
