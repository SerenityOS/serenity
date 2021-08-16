/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4923484 8146293
 * @summary encryption/decryption test for using OAEPParameterSpec.
 * @author Valerie Peng
 */

import java.util.*;

import java.security.*;
import java.security.spec.MGF1ParameterSpec;
import javax.crypto.*;
import javax.crypto.spec.PSource;
import javax.crypto.spec.OAEPParameterSpec;

public class TestOAEPWithParams {

    private static Provider cp;

    private static PrivateKey privateKey;

    private static PublicKey publicKey;

    private static Random random = new Random();

    private static String MD[] = {
        "MD5", "SHA1", "SHA-224", "SHA-256", "SHA-512/224", "SHA-512/256"
    };
    private static int DATA_LENGTH[] = {
        62, 54, 34, 30, 34, 30
    };
    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        cp = Security.getProvider("SunJCE");
        System.out.println("Testing provider " + cp.getName() + "...");
        Provider kfp = Security.getProvider("SunRsaSign");
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", kfp);
        kpg.initialize(768);
        KeyPair kp = kpg.generateKeyPair();
        privateKey = kp.getPrivate();
        publicKey = kp.getPublic();

        for (int i = 0; i < MD.length; i++) {
            // basic test using MD5
            testEncryptDecrypt(MD[i], DATA_LENGTH[i]);
        }

        long stop = System.currentTimeMillis();
        System.out.println("Done (" + (stop - start) + " ms).");
    }

    private static void testEncryptDecrypt(String hashAlg, int dataLength)
        throws Exception {
        System.out.println("Testing OAEP with hash " + hashAlg + ", " + dataLength + " bytes");
        Cipher c = Cipher.getInstance("RSA/ECB/OAEPwith" + hashAlg +
                                      "andMGF1Padding", cp);
        byte[] pSrc1 = { (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01,
                         (byte) 0x02, (byte) 0x02, (byte) 0x02, (byte) 0x02
        };
        byte[] pSrc2 = { (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01,
                         (byte) 0x02, (byte) 0x02, (byte) 0x03, (byte) 0x04
        };
        OAEPParameterSpec spec1 = new OAEPParameterSpec(hashAlg,
            "MGF1", MGF1ParameterSpec.SHA1, new PSource.PSpecified(pSrc1));
        OAEPParameterSpec spec2 = new OAEPParameterSpec(hashAlg,
            "MGF1", MGF1ParameterSpec.SHA1, new PSource.PSpecified(pSrc2));
        byte[] plainText = new byte[dataLength];
        byte[] cipherText, recovered;
        // do encryption with parameters#1
        System.out.println("Testing with user-supplied parameters...");
        c.init(Cipher.ENCRYPT_MODE, publicKey, spec1);
        cipherText = c.doFinal(plainText);

        // test#1: decrypt with parameters#1
        c.init(Cipher.DECRYPT_MODE, privateKey, spec1);
        recovered = c.doFinal(cipherText);
        if (Arrays.equals(plainText, recovered) == false) {
            throw new Exception("Decrypted data does not match");
        }

        // test#2: decrypt without parameters
        c.init(Cipher.DECRYPT_MODE, privateKey);
        try {
            recovered = c.doFinal(cipherText);
            throw new Exception("Should throw BadPaddingException");
        } catch (BadPaddingException bpe) {
            // expected
        }
        // test#3: decrypt with different parameters
        c.init(Cipher.DECRYPT_MODE, privateKey, spec2);
        try {
            recovered = c.doFinal(cipherText);
            throw new Exception("Should throw BadPaddingException");
        } catch (BadPaddingException bpe) {
            // expected
        }
        // do encryption without parameters
        System.out.println("Testing with cipher default parameters...");
        c.init(Cipher.ENCRYPT_MODE, publicKey);
        cipherText = c.doFinal(plainText);

        // test#1: decrypt with parameters got from cipher
        AlgorithmParameters params = c.getParameters();
        c.init(Cipher.DECRYPT_MODE, privateKey, params);
        recovered = c.doFinal(cipherText);
        if (Arrays.equals(plainText, recovered) == false) {
            throw new Exception("Decrypted data does not match");
        }

        // test#2: decrypt without parameters
        c.init(Cipher.DECRYPT_MODE, privateKey);
        recovered = c.doFinal(cipherText);
        if (Arrays.equals(plainText, recovered) == false) {
            throw new Exception("Decrypted data does not match");
        }

        // test#3: decrypt with different parameters
        c.init(Cipher.DECRYPT_MODE, privateKey, spec2);
        try {
            recovered = c.doFinal(cipherText);
            throw new Exception("Should throw BadPaddingException");
        } catch (BadPaddingException bpe) {
            // expected
        }
    }
}
