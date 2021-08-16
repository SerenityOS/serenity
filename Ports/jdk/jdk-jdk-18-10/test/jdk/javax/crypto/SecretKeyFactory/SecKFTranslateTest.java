/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.security.auth.DestroyFailedException;

/*
 * @test
 * @bug 8048820
 * @summary The test verifies SecretKey values should remain the same after
 *  translation with SecretKeyFactory.translateKey().
 */

public class SecKFTranslateTest {
    private static final String SUN_JCE = "SunJCE";

    public static void main(String[] args) throws Exception {

        SecKFTranslateTest test = new SecKFTranslateTest();
        test.run();
    }

    private void run() throws Exception {

        for (Algorithm algorithm : Algorithm.values()) {
            runTest(algorithm);
        }
    }

    private void runTest(Algorithm algo) throws NoSuchAlgorithmException,
            NoSuchProviderException, InvalidKeyException,
            InvalidKeySpecException, NoSuchPaddingException,
            InvalidAlgorithmParameterException, ShortBufferException,
            IllegalBlockSizeException, BadPaddingException {
        AlgorithmParameterSpec[] aps = new AlgorithmParameterSpec[1];
        byte[] plainText = new byte[800];

        SecretKey key1 = algo.intSecurityKey(aps);
        Random random = new Random();
        // Initialization
        SecretKeyFactory skf = SecretKeyFactory.getInstance(algo.toString(),
                SUN_JCE);

        random.nextBytes(plainText);
        Cipher ci = Cipher.getInstance(algo.toString(), SUN_JCE);
        // Encryption
        ci.init(Cipher.ENCRYPT_MODE, key1, aps[0]);
        byte[] cipherText = new byte[ci.getOutputSize(plainText.length)];
        int offset = ci.update(plainText, 0, plainText.length, cipherText, 0);
        ci.doFinal(cipherText, offset);
        // translate key
        SecretKey key2 = skf.translateKey(key1);

        // Decryption
        ci.init(Cipher.DECRYPT_MODE, key2, aps[0]);
        byte[] recoveredText = new byte[ci.getOutputSize(plainText.length)];
        ci.doFinal(cipherText, 0, cipherText.length, recoveredText);

        // Comparison
        if (!Arrays.equals(plainText, recoveredText)) {
            System.out.println("Key1:" + new String(key1.getEncoded())
                    + " Key2:" + new String(key2.getEncoded()));
            throw new RuntimeException("Testing translate key failed with "
                    + algo);
        }

    }
}

class MyOwnSecKey implements SecretKey {

    private static final String DEFAULT_ALGO = "PBEWithMD5AndDES";
    private final byte[] key;
    private final String algorithm;
    private final int keySize;

    public MyOwnSecKey(byte[] key1, int offset, String algo)
            throws InvalidKeyException {
        algorithm = algo;
        if (algo.equalsIgnoreCase("DES")) {
            keySize = 8;
        } else if (algo.equalsIgnoreCase("DESede")) {
            keySize = 24;
        } else {
            throw new InvalidKeyException(
                    "Inappropriate key format and algorithm");
        }

        if (key1 == null || key1.length - offset < keySize) {
            throw new InvalidKeyException("Wrong key size");
        }
        key = new byte[keySize];
        System.arraycopy(key, offset, key, 0, keySize);
    }

    public MyOwnSecKey(PBEKeySpec ks) throws InvalidKeySpecException {
        algorithm = DEFAULT_ALGO;
        key = new String(ks.getPassword()).getBytes();
        keySize = key.length;
    }

    @Override
    public String getAlgorithm() {
        return algorithm;
    }

    @Override
    public String getFormat() {
        return "RAW";
    }

    @Override
    public byte[] getEncoded() {
        byte[] copy = new byte[keySize];
        System.arraycopy(key, 0, copy, 0, keySize);
        return copy;
    }

    @Override
    public void destroy() throws DestroyFailedException {
    }

    @Override
    public boolean isDestroyed() {
        return false;
    }
}

enum Algorithm {
    DES {
        @Override
        SecretKey intSecurityKey(AlgorithmParameterSpec[] spec)
                throws InvalidKeyException {
            int keyLength = 8;
            byte[] keyVal = new byte[keyLength];
            new SecureRandom().nextBytes(keyVal);
            SecretKey key1 = new MyOwnSecKey(keyVal, 0, this.toString());
            return key1;
        }
    },
    DESEDE {
        @Override
        SecretKey intSecurityKey(AlgorithmParameterSpec[] spec)
                throws InvalidKeyException {
            int keyLength = 24;
            byte[] keyVal = new byte[keyLength];
            new SecureRandom().nextBytes(keyVal);
            SecretKey key1 = new MyOwnSecKey(keyVal, 0, this.toString());
            return key1;
        }
    },
    PBEWithMD5ANDdes {
        @Override
        SecretKey intSecurityKey(AlgorithmParameterSpec[] spec)
                throws InvalidKeySpecException {
            byte[] salt = new byte[8];
            int iterCnt = 6;
            new Random().nextBytes(salt);
            spec[0] = new PBEParameterSpec(salt, iterCnt);
            PBEKeySpec pbeKS = new PBEKeySpec(
                    new String("So far so good").toCharArray());
            SecretKey key1 = new MyOwnSecKey(pbeKS);
            return key1;
        }
    };
    abstract SecretKey intSecurityKey(AlgorithmParameterSpec[] spec)
            throws InvalidKeyException, InvalidKeySpecException;
}
