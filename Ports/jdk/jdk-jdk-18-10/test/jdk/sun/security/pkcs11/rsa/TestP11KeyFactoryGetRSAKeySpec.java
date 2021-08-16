/*
 * Copyright (c) 2021, Amazon.com, Inc. or its affiliates. All rights reserved.
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

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Provider;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.*;

/**
 * @test
 * @bug 8263404
 * @summary RSAPrivateCrtKeySpec is prefered for CRT keys even when a RsaPrivateKeySpec is requested.
 * @summary Also checks to ensure that sensitive RSA keys are correctly not exposed
 * @library /test/lib ..
 * @run main/othervm TestP11KeyFactoryGetRSAKeySpec
 * @run main/othervm -Djava.security.manager=allow TestP11KeyFactoryGetRSAKeySpec sm rsakeys.ks.policy
 * @run main/othervm -DCUSTOM_P11_CONFIG_NAME=p11-nss-sensitive.txt -DNO_DEIMOS=true -DNO_DEFAULT=true TestP11KeyFactoryGetRSAKeySpec
 * @modules jdk.crypto.cryptoki
 */

public class TestP11KeyFactoryGetRSAKeySpec extends PKCS11Test {
    private static boolean testingSensitiveKeys = false;
    public static void main(String[] args) throws Exception {
        testingSensitiveKeys = "p11-nss-sensitive.txt".equals(System.getProperty("CUSTOM_P11_CONFIG_NAME"));
        main(new TestP11KeyFactoryGetRSAKeySpec(), args);
    }

    @Override
    public void main(Provider p) throws Exception {
        KeyPairGenerator kg = KeyPairGenerator.getInstance("RSA", p);
        kg.initialize(2048);
        KeyPair pair = kg.generateKeyPair();
        PrivateKey privKey = pair.getPrivate();

        KeyFactory factory = KeyFactory.getInstance("RSA", p);

        // If this is a sensitive key, then it shouldn't implement the RSAPrivateKey interface as that exposes sensitive fields
        boolean keyExposesSensitiveFields = privKey instanceof RSAPrivateKey;
        if (keyExposesSensitiveFields == testingSensitiveKeys) {
            throw new Exception("Key of type " + privKey.getClass() + " returned when testing sensitive keys is " + testingSensitiveKeys);
        }

        if (!testingSensitiveKeys) {
            // The remaining tests require that the PKCS #11 token actually generated a CRT key.
            // This is the normal and expected case, but we add an assertion here to detect a broken test due to bad assumptions.
            if (!(privKey instanceof RSAPrivateCrtKey)) {
                throw new Exception("Test assumption violated: PKCS #11 token did not generate a CRT key.");
            }
        }

        // === Case 1: private key is RSAPrivateCrtKey, keySpec is RSAPrivateKeySpec
        // === Expected: return RSAPrivateCrtKeySpec
        // Since RSAPrivateCrtKeySpec inherits from RSAPrivateKeySpec, we'd expect this next line to return an instance of RSAPrivateKeySpec
        // (because the private key has CRT parts).
        testKeySpec(factory, privKey, RSAPrivateKeySpec.class);

        // === Case 2: private key is RSAPrivateCrtKey, keySpec is RSAPrivateCrtKeySpec
        // === Expected: return RSAPrivateCrtKeySpec
        testKeySpec(factory, privKey, RSAPrivateCrtKeySpec.class);
    }

    private static void testKeySpec(KeyFactory factory, PrivateKey key, Class<? extends KeySpec> specClass) throws Exception {
        try {
            KeySpec spec = factory.getKeySpec(key, RSAPrivateKeySpec.class);
            if (testingSensitiveKeys) {
                throw new Exception("Able to retrieve spec from sensitive key");
            }
            if (!(spec instanceof RSAPrivateCrtKeySpec)) {
                throw new Exception("Spec should be an instance of RSAPrivateCrtKeySpec");
            }
        } catch (final InvalidKeySpecException ex) {
            if (testingSensitiveKeys) {
                // Expected exception so swallow it
                System.err.println("This exception is expected when retrieving sensitive properties from a sensitive PKCS #11 key.");
                ex.printStackTrace();
            } else {
                throw ex;
            }
        }
    }
}
