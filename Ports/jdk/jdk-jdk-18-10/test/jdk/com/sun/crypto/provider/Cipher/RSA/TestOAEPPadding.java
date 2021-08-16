/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8020081 8022669
 * @summary encryption/decryption test for using OAEPPadding with
 * OAEPParameterSpec specified and not specified during a Cipher.init().
 * @author Anthony Scarpino
 */

import java.util.Arrays;

import java.security.Security;
import java.security.Provider;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.MGF1ParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.PSource;


public class TestOAEPPadding {
    private static RSAPrivateKey privateKey;
    private static RSAPublicKey publicKey;
    static Provider cp;
    static boolean failed = false;

    public static void main(String args[]) throws Exception {
        cp = Security.getProvider("SunJCE");
        System.out.println("Testing provider " + cp.getName() + "...");
        Provider kfp = Security.getProvider("SunRsaSign");
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", kfp);
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        privateKey = (RSAPrivateKey)kp.getPrivate();
        publicKey = (RSAPublicKey)kp.getPublic();

        // Test using a spec with each digest algorithm case
        // MD5
        test(new OAEPParameterSpec("MD5", "MGF1",
                MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT));
        test(new OAEPParameterSpec("MD5", "MGF1",
                MGF1ParameterSpec.SHA224, PSource.PSpecified.DEFAULT));
        test(new OAEPParameterSpec("MD5", "MGF1",
                MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT));
        test(new OAEPParameterSpec("MD5", "MGF1",
                MGF1ParameterSpec.SHA384, PSource.PSpecified.DEFAULT));
        test(new OAEPParameterSpec("MD5", "MGF1",
                MGF1ParameterSpec.SHA512, PSource.PSpecified.DEFAULT));
        // SHA1
        test(new OAEPParameterSpec("SHA1", "MGF1",
                MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT));
        test(new OAEPParameterSpec("SHA1", "MGF1",
                MGF1ParameterSpec.SHA224, PSource.PSpecified.DEFAULT));
        test(new OAEPParameterSpec("SHA1", "MGF1",
                MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT));
        test(new OAEPParameterSpec("SHA1", "MGF1",
                MGF1ParameterSpec.SHA384, PSource.PSpecified.DEFAULT));
        test(new OAEPParameterSpec("SHA1", "MGF1",
                MGF1ParameterSpec.SHA512, PSource.PSpecified.DEFAULT));
        // For default OAEPParameterSpec case (SHA1)
        test(null);
        // SHA-224
        test(new OAEPParameterSpec("SHA-224", "MGF1",
                MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT));
        test(new OAEPParameterSpec("SHA-224", "MGF1",
                MGF1ParameterSpec.SHA224, PSource.PSpecified.DEFAULT));
        test(new OAEPParameterSpec("SHA-224", "MGF1",
                MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT));
        test(new OAEPParameterSpec("SHA-224", "MGF1",
                MGF1ParameterSpec.SHA384, PSource.PSpecified.DEFAULT));
        test(new OAEPParameterSpec("SHA-224", "MGF1",
                MGF1ParameterSpec.SHA512, PSource.PSpecified.DEFAULT));
        // SHA-256
        test(new OAEPParameterSpec("SHA-256", "MGF1",
                MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT));
        test(new OAEPParameterSpec("SHA-256", "MGF1",
                MGF1ParameterSpec.SHA224, PSource.PSpecified.DEFAULT));
        test(new OAEPParameterSpec("SHA-256", "MGF1",
                MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT));
        test(new OAEPParameterSpec("SHA-256", "MGF1",
                MGF1ParameterSpec.SHA384, PSource.PSpecified.DEFAULT));
        test(new OAEPParameterSpec("SHA-256", "MGF1",
                MGF1ParameterSpec.SHA512, PSource.PSpecified.DEFAULT));
        // SHA-384
        test(new OAEPParameterSpec("SHA-384", "MGF1",
                MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT));
        test(new OAEPParameterSpec("SHA-384", "MGF1",
                MGF1ParameterSpec.SHA224, PSource.PSpecified.DEFAULT));
        test(new OAEPParameterSpec("SHA-384", "MGF1",
                MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT));
        test(new OAEPParameterSpec("SHA-384", "MGF1",
                MGF1ParameterSpec.SHA384, PSource.PSpecified.DEFAULT));
        test(new OAEPParameterSpec("SHA-384", "MGF1",
                MGF1ParameterSpec.SHA512, PSource.PSpecified.DEFAULT));
        // SHA-512
        test(new OAEPParameterSpec("SHA-512", "MGF1",
                MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT));
        test(new OAEPParameterSpec("SHA-512", "MGF1",
                MGF1ParameterSpec.SHA224, PSource.PSpecified.DEFAULT));
        test(new OAEPParameterSpec("SHA-512", "MGF1",
                MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT));
        test(new OAEPParameterSpec("SHA-512", "MGF1",
                MGF1ParameterSpec.SHA384, PSource.PSpecified.DEFAULT));
        test(new OAEPParameterSpec("SHA-512", "MGF1",
                MGF1ParameterSpec.SHA512, PSource.PSpecified.DEFAULT));
        // SHA-512/224 and SHA-512/256
        test(new OAEPParameterSpec("SHA-512/224", "MGF1",
                MGF1ParameterSpec.SHA224, PSource.PSpecified.DEFAULT));
        test(new OAEPParameterSpec("SHA-512/224", "MGF1",
                MGF1ParameterSpec.SHA512_224, PSource.PSpecified.DEFAULT));
        test(new OAEPParameterSpec("SHA-512/256", "MGF1",
                MGF1ParameterSpec.SHA384, PSource.PSpecified.DEFAULT));
        test(new OAEPParameterSpec("SHA-512/256", "MGF1",
                MGF1ParameterSpec.SHA512, PSource.PSpecified.DEFAULT));

        if (failed) {
            throw new Exception("Test failed");
        }
    }

    /*
     * Test with one byte, the max bytes, and the max + 1 bytes allowed by
     * the RSA key size and the digest algorithm
     */
    static void test(OAEPParameterSpec spec) throws Exception {
        int dlen = 0;
        String algo;

        // For default OAEPParameterSpec case (SHA1)
        if (spec == null) {
            dlen = 20;
            algo = "Default";
        } else {
            // Use the digest algorith provided in the spec
            algo = spec.getDigestAlgorithm();
            if (algo.equals("MD5")) {
                dlen = 16;
            } else if (algo.equals("SHA1")) {
                dlen = 20;
            } else if (algo.equals("SHA-224") || algo.equals("SHA-512/224")) {
                dlen = 28;
            } else if (algo.equals("SHA-256") || algo.equals("SHA-512/256")) {
                dlen = 32;
            } else if (algo.equals("SHA-384")) {
                dlen = 48;
            } else if (algo.equals("SHA-512")) {
                dlen = 64;
            }
        }

        // OAEP maximum length for a given digest algorith & RSA key length
        int max = ((publicKey.getModulus().bitLength() / 8) - (2 * dlen) - 2);

        // Test with data length of 1
        try {
            testEncryptDecrypt(spec, 1);
        } catch (Exception e) {
            System.out.println(algo + " failed with data length of 1");
            e.printStackTrace();
            failed = true;
        }

        // Test with data length of maximum allowed
        try {
            testEncryptDecrypt(spec, max);
        } catch (Exception e) {
            System.out.println(algo + " failed with data length of " + max);
            e.printStackTrace();
            failed = true;
        }

        // Test with data length of maximum allowed + 1
        try {
            testEncryptDecrypt(spec, max + 1);
            throw new Exception();
        } catch (IllegalBlockSizeException ie) {
                // expected to fail
        } catch (Exception e) {
            System.err.println(algo + " failed with data length of " +
                    (max + 1));
            e.printStackTrace();
            failed = true;

        }
    }

    private static void testEncryptDecrypt(OAEPParameterSpec spec,
            int dataLength) throws Exception {

        System.out.print("Testing OAEP with hash ");
        if (spec != null) {
            System.out.print(spec.getDigestAlgorithm() + " and MGF " +
                ((MGF1ParameterSpec)spec.getMGFParameters()).
                    getDigestAlgorithm());
        } else {
            System.out.print("Default");
        }
        System.out.println(", " + dataLength + " bytes");

        Cipher c = Cipher.getInstance("RSA/ECB/OAEPPadding", cp);
        if (spec != null) {
            c.init(Cipher.ENCRYPT_MODE, publicKey, spec);
        } else {
            c.init(Cipher.ENCRYPT_MODE, publicKey);
        }

        byte[] data = new byte[dataLength];
        byte[] enc = c.doFinal(data);
        if (spec != null) {
            c.init(Cipher.DECRYPT_MODE, privateKey, spec);
        } else {
            c.init(Cipher.DECRYPT_MODE, privateKey);
        }
        byte[] dec = c.doFinal(enc);
        if (Arrays.equals(data, dec) == false) {
            throw new Exception("Data does not match");
        }
    }
}
