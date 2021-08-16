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

/**
 * @test
 * @bug 8216012
 * @summary Tests the RSA public key exponent for KeyPairGenerator
 * @run main/timeout=60 TestKeyPairGeneratorExponent
 */

import java.math.BigInteger;

import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;

public class TestKeyPairGeneratorExponent {
    private static int keyLen = 512;

    private static BigInteger[] validExponents = new BigInteger[] {
        RSAKeyGenParameterSpec.F0,
        RSAKeyGenParameterSpec.F4,
        BigInteger.ONE.shiftLeft(keyLen - 1).subtract(BigInteger.ONE)
    };

    private static BigInteger[] invalidExponents = new BigInteger[] {
        BigInteger.valueOf(-1),
        BigInteger.ZERO,
        BigInteger.ONE,
        // without this fix, an even value causes an infinite loop
        BigInteger.valueOf(4)
    };

    public static void testValidExponents(KeyPairGenerator kpg,
            BigInteger exponent) {
        System.out.println("Testing exponent = " + exponent.toString(16));
        try {
            kpg.initialize(new RSAKeyGenParameterSpec(keyLen, exponent));
            kpg.generateKeyPair();
            System.out.println("OK, key pair generated");
        } catch(InvalidAlgorithmParameterException iape){
            throw new RuntimeException("Error: Unexpected Exception: " + iape);
        }
    }

    public static void testInvalidExponents(KeyPairGenerator kpg,
            BigInteger exponent) {
        System.out.println("Testing exponent = " + exponent.toString(16));
        try {
            kpg.initialize(new RSAKeyGenParameterSpec(keyLen, exponent));
            kpg.generateKeyPair();
            throw new RuntimeException("Error: Expected IAPE not thrown.");
        } catch(InvalidAlgorithmParameterException iape){
            // Expected InvalidAlgorithmParameterException was thrown
            System.out.println("OK, expected IAPE thrown");
        } catch(Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error: unexpected exception " + e);
        }
    }

    public static void main(String[] args) throws Exception {
        KeyPairGenerator kpg =
                KeyPairGenerator.getInstance("RSA", "SunRsaSign");

        for(BigInteger validExponent : validExponents) {
            testValidExponents(kpg, validExponent);
        }

        for(BigInteger invalidExponent : invalidExponents) {
            testInvalidExponents(kpg, invalidExponent);
        }
    }
}
