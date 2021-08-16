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
import java.security.KeyPairGenerator;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;

/**
 * @test
 * @bug 8044199 4666485
 * @summary Equality checking for RSAPrivateKey by SunRsaSign provider.
 */
public class PrivateKeyEqualityTest {
    /**
     * ALGORITHM name, fixed as RSA.
     */
    private static final String KEYALG = "RSA";

    /**
     * JDK default RSA Provider.
     */
    private static final String PROVIDER_NAME = "SunRsaSign";

    public static void main(String[] args) throws NoSuchAlgorithmException,
            NoSuchProviderException, InvalidKeySpecException {
        // Generate the first key.
        KeyPairGenerator generator
                = KeyPairGenerator.getInstance(KEYALG, PROVIDER_NAME);
        KeyPair keyPair = generator.generateKeyPair();
        RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) keyPair.getPrivate();
        if (!(rsaPrivateKey instanceof RSAPrivateCrtKey)) {
            System.err.println("rsaPrivateKey class : " + rsaPrivateKey.getClass().getName());
            throw new RuntimeException("rsaPrivateKey is not a RSAPrivateCrtKey instance");
        }

        // Generate the second key.
        KeyFactory factory = KeyFactory.getInstance(KEYALG, PROVIDER_NAME);
        RSAPrivateKeySpec rsaPrivateKeySpec = new RSAPrivateKeySpec(
                rsaPrivateKey.getModulus(), rsaPrivateKey.getPrivateExponent());
        RSAPrivateKey rsaPrivateKey2 = (RSAPrivateKey) factory.generatePrivate(
                rsaPrivateKeySpec);

        // Generate the third key.
        PKCS8EncodedKeySpec encodedKeySpec = new PKCS8EncodedKeySpec(
                rsaPrivateKey.getEncoded());
        RSAPrivateKey rsaPrivateKey3 = (RSAPrivateKey) factory.generatePrivate(
                encodedKeySpec);

        // Check for equality.
        if (rsaPrivateKey.equals(rsaPrivateKey2)) {
            throw new RuntimeException("rsaPrivateKey should not equal to rsaPrivateKey2");
        }
        if (!rsaPrivateKey3.equals(rsaPrivateKey)) {
            throw new RuntimeException("rsaPrivateKey3 should equal to rsaPrivateKey");
        }
        if (rsaPrivateKey3.equals(rsaPrivateKey2)) {
            throw new RuntimeException("rsaPrivateKey3 should not equal to rsaPrivateKey2");
        }
        if (rsaPrivateKey2.equals(rsaPrivateKey3)) {
            throw new RuntimeException("rsaPrivateKey2 should not equal to rsaPrivateKey3");
        }

        // Generate the fourth key.
        RSAPrivateCrtKey rsaPrivateCrtKey =  (RSAPrivateCrtKey)rsaPrivateKey;
        RSAPrivateCrtKeySpec rsaPrivateCrtKeySpec = new RSAPrivateCrtKeySpec(
                rsaPrivateCrtKey.getModulus(),
                rsaPrivateCrtKey.getPublicExponent(),
                rsaPrivateCrtKey.getPrivateExponent(),
                rsaPrivateCrtKey.getPrimeP(),
                rsaPrivateCrtKey.getPrimeQ(),
                rsaPrivateCrtKey.getPrimeExponentP(),
                rsaPrivateCrtKey.getPrimeExponentQ(),
                rsaPrivateCrtKey.getCrtCoefficient()
            );
        RSAPrivateCrtKey rsaPrivateKey4 = (RSAPrivateCrtKey) factory.generatePrivate(
                rsaPrivateCrtKeySpec);
        if (!rsaPrivateKey.equals(rsaPrivateKey4)) {
            throw new RuntimeException("rsaPrivateKey should equal to rsaPrivateKey4");
        }
    }
}
