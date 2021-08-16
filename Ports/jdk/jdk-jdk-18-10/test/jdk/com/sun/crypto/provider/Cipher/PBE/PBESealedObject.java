/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.io.PrintStream;
import java.security.AlgorithmParameters;
import java.security.InvalidKeyException;
import java.security.Provider;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.Random;
import java.util.StringTokenizer;
import javax.crypto.Cipher;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

/**
 * @test
 * @bug 8041781
 * @summary test if seal/unseal works correctly with PBE algorithms
 * @author Yun Ke
 * @author Bill Situ
 * @author Alexander Fomin
 * @run main PBESealedObject
 * @key randomness
 */
public class PBESealedObject {

    private static final String[] PBEAlgorithms = {
        "pbeWithMD5ANDdes",
        "PBEWithMD5AndDES/CBC/PKCS5Padding",
        "PBEWithMD5AndTripleDES",
        "PBEWithMD5AndTripleDES/CBC/PKCS5Padding",
        "PBEwithSHA1AndDESede",
        "PBEwithSHA1AndDESede/CBC/PKCS5Padding",
        "PBEwithSHA1AndRC2_40",
        "PBEwithSHA1Andrc2_40/CBC/PKCS5Padding",
        "PBEWithSHA1AndRC2_128",
        "PBEWithSHA1andRC2_128/CBC/PKCS5Padding",
        "PBEWithSHA1AndRC4_40",
        "PBEWithsha1AndRC4_40/ECB/NoPadding",
        "PBEWithSHA1AndRC4_128",
        "pbeWithSHA1AndRC4_128/ECB/NoPadding",
        "PBEWithHmacSHA1AndAES_128",
        "PBEWithHmacSHA224AndAES_128",
        "PBEWithHmacSHA256AndAES_128",
        "PBEWithHmacSHA384AndAES_128",
        "PBEWithHmacSHA512AndAES_128",
        "PBEWithHmacSHA1AndAES_256",
        "PBEWithHmacSHA224AndAES_256",
        "PBEWithHmacSHA256AndAES_256",
        "PBEWithHmacSHA384AndAES_256",
        "PBEWithHmacSHA512AndAES_256"
    };

    public static void main(String[] args) {
        PBESealedObject test = new PBESealedObject();
        Provider sunjce = Security.getProvider("SunJCE");

        if (!test.runAll(sunjce, System.out)) {
            throw new RuntimeException("One or more tests have failed....");
        }
    }

    public boolean runAll(Provider p, PrintStream out) {
        boolean finalResult = true;

        for (String algorithm : PBEAlgorithms) {
            out.println("Running test with " + algorithm + ":");
            try {
                if (!runTest(p, algorithm, out)) {
                    finalResult = false;
                    out.println("STATUS: Failed");
                } else {
                    out.println("STATUS: Passed");
                }
            } catch (Exception ex) {
                finalResult = false;
                ex.printStackTrace(out);
                out.println("STATUS:Failed");
            }
        }

        return finalResult;
    }

    // Have a generic throws Exception as it can throw many different exceptions
    public boolean runTest(Provider p, String algo, PrintStream out)
            throws Exception {

        byte[] salt = new byte[8];
        int ITERATION_COUNT = 1000;
        AlgorithmParameters pbeParams = null;

        String baseAlgo
                = new StringTokenizer(algo, "/").nextToken().toUpperCase();
        boolean isAES = baseAlgo.contains("AES");

        try {
            // Initialization
            Cipher ci = Cipher.getInstance(algo, p);
            new Random().nextBytes(salt);
            AlgorithmParameterSpec aps = new PBEParameterSpec(salt,
                    ITERATION_COUNT);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(baseAlgo, p);
            SecretKey key = skf.generateSecret(
                    new PBEKeySpec("Secret Lover".toCharArray()));

            // Seal
            if (isAES) {
                ci.init(Cipher.ENCRYPT_MODE, key);
                pbeParams = ci.getParameters();
            } else {
                ci.init(Cipher.ENCRYPT_MODE, key, aps);
            }

            SealedObject so = new SealedObject(key, ci);

            // Unseal and compare
            if (isAES) {
                ci.init(Cipher.DECRYPT_MODE, key, pbeParams);
            } else {
                ci.init(Cipher.DECRYPT_MODE, key, aps);
            }

            SecretKey unsealedKey;

            unsealedKey = (SecretKey) so.getObject(ci);
            if (!Arrays.equals(unsealedKey.getEncoded(), key.getEncoded())) {
                return false;
            }

            unsealedKey = (SecretKey) so.getObject(key);
            if (!Arrays.equals(unsealedKey.getEncoded(), key.getEncoded())) {
                return false;
            }

            unsealedKey = (SecretKey) so.getObject(key, "SunJCE");
            return Arrays.equals(unsealedKey.getEncoded(), key.getEncoded());
        } catch (InvalidKeyException ex) {
            if (baseAlgo.endsWith("TRIPLEDES") || baseAlgo.endsWith("AES_256")) {
                out.println(
                        "Expected exception , keyStrength > 128 within" + algo);
                return true;
            }

            throw ex;
        }
    }

}
