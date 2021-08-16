/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

/**
 * @test
 * @bug 8044199
 * @summary test if the private and public key size are the same as what is set
 * through KeyPairGenerator.
 * @run main KeySizeTest 512 10
 * @run main KeySizeTest 768 10
 * @run main KeySizeTest 1024 10
 * @run main KeySizeTest 2048 5
 * @run main KeySizeTest 4096 1
 */
public class KeySizeTest {

    /**
     * ALGORITHM name, fixed as RSA.
     */
    private static final String KEYALG = "RSA";

    /**
     * JDK default RSA Provider.
     */
    private static final String PROVIDER_NAME = "SunRsaSign";

    public static void main(String[] args) throws Exception {
        int iKeyPairSize = Integer.parseInt(args[0]);
        int maxLoopCnt = Integer.parseInt(args[1]);

        int failCount = 0;
        KeyPairGenerator keyPairGen
                = KeyPairGenerator.getInstance(KEYALG, PROVIDER_NAME);
        keyPairGen.initialize(iKeyPairSize);
        // Generate RSA keypair
        KeyPair keyPair = keyPairGen.generateKeyPair();

        // Get priavte and public keys
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        try {
            if (!sizeTest(keyPair)) {
                failCount++;
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            failCount++;
        }

        for (int iCnt = 0; iCnt < maxLoopCnt; iCnt++) {

            // Get keysize (modulus) of keys
            KeyFactory keyFact = KeyFactory.getInstance(KEYALG, PROVIDER_NAME);

            // Comparing binary length.
            RSAPrivateKeySpec privateKeySpec
                    = (RSAPrivateKeySpec) keyFact.getKeySpec(privateKey,
                            RSAPrivateKeySpec.class);
            int iPrivateKeySize = privateKeySpec.getModulus().bitLength();

            RSAPublicKeySpec publicKeySpec
                    = (RSAPublicKeySpec) keyFact.getKeySpec(publicKey,
                            RSAPublicKeySpec.class);
            int iPublicKeySize = publicKeySpec.getModulus().bitLength();

            if ((iKeyPairSize != iPublicKeySize) || (iKeyPairSize != iPrivateKeySize)) {
                System.err.println("iKeyPairSize : " + iKeyPairSize);
                System.err.println("Generated a " + iPrivateKeySize
                        + " bit RSA private key");
                System.err.println("Generated a " + iPublicKeySize
                        + " bit RSA public key");
                failCount++;
            }
        }

        if (failCount > 0) {
            throw new RuntimeException("There are " + failCount + " tests failed.");
        }
    }

    /**
     * @param kpair test key pair.
     * @return true if test passed. false if test failed.
     */
    private static boolean sizeTest(KeyPair kpair) {
        RSAPrivateKey priv = (RSAPrivateKey) kpair.getPrivate();
        RSAPublicKey pub = (RSAPublicKey) kpair.getPublic();

        // test the getModulus method
        if ((priv instanceof RSAKey) && (pub instanceof RSAKey)) {
            if (!priv.getModulus().equals(pub.getModulus())) {
                System.err.println("priv.getModulus() = " + priv.getModulus());
                System.err.println("pub.getModulus() = " + pub.getModulus());
                return false;
            }
        }
        return true;
    }
}
