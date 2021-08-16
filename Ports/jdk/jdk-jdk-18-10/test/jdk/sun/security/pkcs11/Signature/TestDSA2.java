/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8080462 8242332
 * @library /test/lib ..
 * @modules jdk.crypto.cryptoki
 * @run main/othervm/timeout=250 TestDSA2
 * @summary verify that DSA signature works using SHA-2 digests.
 * @key randomness
 */


import java.security.*;
import java.security.spec.*;
import java.security.interfaces.*;

public class TestDSA2 extends PKCS11Test {

    private static final String[] SIG_ALGOS = {
        "SHA224withDSA",
        "SHA256withDSA",
        "SHA3-224withDSA",
        "SHA3-256withDSA",
        "SHA384withDSA",
        "SHA512withDSA",
        "SHA3-384withDSA",
        "SHA3-512withDSA",
    };

    private static final int KEYSIZE = 2048;

    public static void main(String[] args) throws Exception {
        main(new TestDSA2(), args);
    }

    @Override
    public void main(Provider p) throws Exception {
        KeyPair kp;
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("DSA", p);
            kpg.initialize(KEYSIZE);
            kp = kpg.generateKeyPair();
        } catch (Exception ex) {
            System.out.println("Skip due to no 2048-bit DSA support: " + ex);
            return;
        }

        boolean allPass = true;
        for (String sigAlg : SIG_ALGOS) {
            System.out.println("Testing " + sigAlg);
            try {
                Signature sig = Signature.getInstance(sigAlg, p);
                test(sig, kp, p);
            } catch (NoSuchAlgorithmException nsae) {
                System.out.println("=>Skip due to no support");
            } catch (Exception ex) {
                System.out.println("Unexpected exception when testing " +
                    sigAlg);
                ex.printStackTrace();
                allPass = false;
            }
        }
        if (allPass) {
            System.out.println("Tests Passed");
        } else {
            throw new RuntimeException("One or more tests failed");
        }
    }

    private static void test(Signature sig, KeyPair kp, Provider p)
            throws Exception {

        byte[] data = "anything will do".getBytes();

        sig.initSign(kp.getPrivate());
        sig.update(data);
        byte[] signature = sig.sign();

        Signature sigV = Signature.getInstance(sig.getAlgorithm() , p);
        sigV.initVerify(kp.getPublic());
        sigV.update(data);
        boolean verifies = sigV.verify(signature);
        System.out.println("=> Passed");
    }
}
