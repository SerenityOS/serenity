/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.NamedParameterSpec;
import java.util.ArrayList;
import java.util.List;

/*
 * @test
 * @bug 8209632
 * @summary Test behaviour of Signature instance by re-using it multiple times
 *          in different way.
 * @run main EdDSAReuseTest
 */
public class EdDSAReuseTest {

    private static final String EDDSA = "EdDSA";
    private static final String ED25519 = "Ed25519";
    private static final String ED448 = "Ed448";
    private static final String PROVIDER = "SunEC";
    private static final String MSG = "TEST";
    private static final int REUSE = 20;
    private static final int ONCE = 1;
    private static final int TENTH = 10;
    private static final int FIFTH = 5;

    public static void main(String[] args) throws Exception {

        for (boolean initKey : new boolean[]{true, false}) {
            // Sign and Verify with data update once
            test(PROVIDER, EDDSA, null, initKey, ONCE, ONCE);
            test(PROVIDER, ED25519, ED25519, initKey, ONCE, ONCE);
            test(PROVIDER, ED448, ED448, initKey, ONCE, ONCE);

            // Sign and Verify with data update 10 times
            test(PROVIDER, EDDSA, null, initKey, TENTH, TENTH);
            test(PROVIDER, ED25519, ED25519, initKey, TENTH, TENTH);
            test(PROVIDER, ED448, ED448, initKey, TENTH, TENTH);

            // Sign and Verify with data update unmatched number of times
            test(PROVIDER, EDDSA, null, initKey, TENTH, FIFTH);
            test(PROVIDER, ED25519, ED25519, initKey, TENTH, FIFTH);
            test(PROVIDER, ED448, ED448, initKey, TENTH, FIFTH);
        }
    }

    private static void test(String provider, String name, Object param,
            boolean initKey, int signUpdate, int verifyUpdate)
            throws Exception {

        System.out.printf("Case for signature name: %s, param: %s,"
                + " initialize signature instance before each operation: %s%n",
                name, param, initKey);
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(name, provider);
        if (param != null) {
            kpg.initialize(new NamedParameterSpec((String) param));
        }
        KeyPair kp = kpg.generateKeyPair();
        Signature sig = Signature.getInstance(name, provider);
        testAPI(sig, kp, initKey, signUpdate, verifyUpdate);
        System.out.println("Passed.");
    }

    private static void testAPI(Signature sig, KeyPair kp, boolean initKey,
            int signUpdate, int verifyUpdate) throws Exception {

        sig.initSign(kp.getPrivate());
        List<byte[]> signatures = new ArrayList<>();
        // Re-use the signature instance 20 times
        for (int i = 0; i < REUSE; i++) {
            signatures.add(sign(sig, kp.getPrivate(), MSG, initKey, signUpdate));
        }
        System.out.printf("Generated signatures %s times%n", signatures.size());
        sig.initVerify(kp.getPublic());
        for (byte[] sign : signatures) {
            // Verification will pass when message update matches with
            // the same used for sign
            if (verify(sig, kp.getPublic(), MSG, sign, initKey, verifyUpdate)
                    != (signUpdate == verifyUpdate)) {
                throw new RuntimeException(
                        "Verification succed with unmatched message");
            }
        }
        System.out.printf("Verified signatures %s times%n", signatures.size());
    }

    private static byte[] sign(Signature sig, PrivateKey priKey, String msg,
            boolean initKey, int signUpdate) throws Exception {
        if (initKey) {
            sig.initSign(priKey);
        }
        for (int update = 0; update < signUpdate; update++) {
            sig.update(msg.getBytes());
        }
        return sig.sign();
    }

    private static boolean verify(Signature sig, PublicKey pubKey, String msg,
            byte[] sign, boolean initKey, int verifyUpdate) throws Exception {
        if (initKey) {
            sig.initVerify(pubKey);
        }
        for (int update = 0; update < verifyUpdate; update++) {
            sig.update(msg.getBytes());
        }
        return sig.verify(sign);
    }
}
