/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

import static java.lang.System.out;

import java.lang.Integer;
import java.lang.String;
import java.lang.System;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.KeyPairGenerator;
import java.security.Provider;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

/*
 * @test
 * @bug 8048599 8248268
 * @summary  Tests for key wrap and unwrap operations
 */

public class TestCipherKeyWrapperTest {
    private static final String SUN_JCE = "SunJCE";
    // Blowfish Variable key length: 32 bits to 448 bits
    private static final int BLOWFISH_MIN_KEYSIZE = 32;
    private static final int BLOWFISH_MAX_KEYSIZE = 448;
    private static final int LINIMITED_KEYSIZE = 128;
    private static final String NOPADDING = "NoPaDDing";
    private static final String[] PBE_ALGORITHM_AR = { "pbeWithMD5ANDdes",
            "PBEWithMD5AndDES/CBC/PKCS5Padding", "PBEWithMD5AndTripleDES",
            "PBEWithMD5AndTripleDES/CBC/PKCS5Padding", "PBEwithSHA1AndDESede",
            "PBEwithSHA1AndDESede/CBC/PKCS5Padding", "PBEwithSHA1AndRC2_40",
            "PBEwithSHA1Andrc2_40/CBC/PKCS5Padding", "PBEWithSHA1AndRC2_128",
            "PBEWithSHA1andRC2_128/CBC/PKCS5Padding", "PBEWithSHA1AndRC4_40",
            "PBEWithsha1AndRC4_40/ECB/NoPadding", "PBEWithSHA1AndRC4_128",
            "pbeWithSHA1AndRC4_128/ECB/NoPadding", "PBEWithHmacSHA1AndAES_128",
            "PBEWithHmacSHA224AndAES_128", "PBEWithHmacSHA256AndAES_128",
            "PBEWithHmacSHA384AndAES_128", "PBEWithHmacSHA512AndAES_128",
            "PBEWithHmacSHA1AndAES_256", "PBEWithHmacSHA224AndAES_256",
            "PBEWithHmacSHA256AndAES_256", "PBEWithHmacSHA384AndAES_256",
            "PBEWithHmacSHA512AndAES_256" };
    private static final String[] MODEL_AR = { "ECb", "pCbC", "cbC", "cFB",
            "cFB24", "cFB40", "OfB48", "OFB64" };
    private static final String[] PADDING_AR = { NOPADDING, "PKCS5Padding" };

    private enum AlgorithmWrapper {
        AESWrap("AES", "AESWrap", -1),
        AESWrap_128("AES", "AESWrap_128", 128),
        AESWrap_192("AES", "AESWrap_192", 192),
        AESWrap_256("AES", "AESWrap_256", 256),
        AESWrapPad("AES", "AESWrapPad", -1),
        AESWrapPad_128("AES", "AESWrapPad_128", 128),
        AESWrapPad_192("AES", "AESWrapPad_192", 192),
        AESWrapPad_256("AES", "AESWrapPad_256", 256),
        DESedeWrap("desede", "DESedeWrap", -1),
        NegtiveWrap("AES", "DESedeWrap", -1);

        private final String algorithm;
        private final String wrapper;
        private final int keySize;

        private AlgorithmWrapper(String algorithm, String wrapper, int kSize) {
            this.algorithm = algorithm;
            this.wrapper = wrapper;
            this.keySize = kSize;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        public String getWrapper() {
            return wrapper;
        }

        public int getKeySize() {
            return keySize;
        }

    };

    public static void main(String[] args) throws Exception {

        TestCipherKeyWrapperTest test = new TestCipherKeyWrapperTest();
        // AESWrap and DESedeWrap test
        for (AlgorithmWrapper algoWrapper : AlgorithmWrapper.values()) {
            String algo = algoWrapper.getAlgorithm();
            String wrapper = algoWrapper.getWrapper();
            try {
                int keySize = algoWrapper.getKeySize();
                // only run the tests on longer key lengths if unlimited
                // version of JCE jurisdiction policy files are installed
                if (!(Cipher.getMaxAllowedKeyLength(algo) == Integer.MAX_VALUE)
                        && keySize > LINIMITED_KEYSIZE) {
                    out.println(algo + " will not run if unlimited version of"
                            + " JCE jurisdiction policy files are installed");
                    continue;
                }
                test.wrapperAesDESedeKeyTest(algo, wrapper, keySize);
                if (algoWrapper == AlgorithmWrapper.NegtiveWrap) {
                    throw new RuntimeException("Expected not throw when algo"
                            + " and wrapAlgo are not match:" + algo);
                }
            } catch (InvalidKeyException e) {
                if (algoWrapper == AlgorithmWrapper.NegtiveWrap) {
                    out.println("Expepted exception when algo"
                            + " and wrapAlgo are not match:" + algo);
                } else {
                    throw e;
                }
            }
        }
        test.wrapperBlowfishKeyTest();
        // PBE and public wrapper test.
        String[] publicPrivateAlgos = new String[] { "DiffieHellman", "DSA",
                "RSA" };
        Provider provider = Security.getProvider(SUN_JCE);
        if (provider == null) {
            throw new RuntimeException("SUN_JCE provider not exist");
        }

        test.wrapperPBEKeyTest(provider);
        // Public and private key wrap test
        test.wrapperPublicPriviteKeyTest(provider, publicPrivateAlgos);
    }

    private void wrapperAesDESedeKeyTest(String algo, String wrapAlgo,
            int keySize) throws InvalidKeyException, NoSuchAlgorithmException,
            NoSuchPaddingException, IllegalBlockSizeException,
            InvalidAlgorithmParameterException {
        // Initialization
        KeyGenerator kg = KeyGenerator.getInstance(algo);
        if (keySize != -1) {
            kg.init(keySize);
        }
        SecretKey key = kg.generateKey();
        wrapTest(algo, wrapAlgo, key, key, Cipher.SECRET_KEY, false);
    }

    private void wrapperBlowfishKeyTest() throws InvalidKeyException,
            NoSuchAlgorithmException, NoSuchPaddingException,
            IllegalBlockSizeException, InvalidAlgorithmParameterException {
        // how many kinds of padding mode
        int padKinds;
        // Keysize should be multiple of 8 bytes.
        int KeyCutter = 8;
        int kSize = BLOWFISH_MIN_KEYSIZE;
        String algorithm = "Blowfish";
        int maxAllowKeyLength = Cipher.getMaxAllowedKeyLength(algorithm);
        boolean unLimitPolicy = maxAllowKeyLength == Integer.MAX_VALUE;
        SecretKey key = null;
        while (kSize <= BLOWFISH_MAX_KEYSIZE) {
            for (String mode : MODEL_AR) {
                // PKCS5padding is meaningful only for ECB, CBC, PCBC
                if (mode.equalsIgnoreCase(MODEL_AR[0])
                        || mode.equalsIgnoreCase(MODEL_AR[1])
                        || mode.equalsIgnoreCase(MODEL_AR[2])) {
                    padKinds = PADDING_AR.length;
                } else {
                    padKinds = 1;
                }
                // Initialization
                KeyGenerator kg = KeyGenerator.getInstance(algorithm);
                for (int k = 0; k < padKinds; k++) {
                    String transformation = algorithm + "/" + mode + "/"
                            + PADDING_AR[k];
                    if (NOPADDING.equals(PADDING_AR[k]) && kSize % 64 != 0) {
                        out.println(transformation
                                + " will not run if input length not multiple"
                                + " of 8 bytes when padding is " + NOPADDING);
                        continue;
                    }
                    kg.init(kSize);
                    key = kg.generateKey();
                    // only run the tests on longer key lengths if unlimited
                    // version of JCE jurisdiction policy files are installed
                    if (!unLimitPolicy && kSize > LINIMITED_KEYSIZE) {
                        out.println("keyStrength > 128 within " + algorithm
                                + " will not run under global policy");
                    } else {
                        wrapTest(transformation, transformation, key, key,
                                Cipher.SECRET_KEY, false);
                    }
                }
            }
            if (kSize <= LINIMITED_KEYSIZE) {
                KeyCutter = 8;
            } else {
                KeyCutter = 48;
            }
            kSize += KeyCutter;
        }
    }

    private void wrapperPBEKeyTest(Provider p) throws InvalidKeySpecException,
            InvalidKeyException, NoSuchPaddingException,
            IllegalBlockSizeException, InvalidAlgorithmParameterException,
            NoSuchAlgorithmException {
        for (String alg : PBE_ALGORITHM_AR) {
            String baseAlgo = alg.split("/")[0].toUpperCase();
            // only run the tests on longer key lengths if unlimited version
            // of JCE jurisdiction policy files are installed

            if (Cipher.getMaxAllowedKeyLength(alg) < Integer.MAX_VALUE
                    && (baseAlgo.endsWith("TRIPLEDES") || alg
                            .endsWith("AES_256"))) {
                out.println("keyStrength > 128 within " + alg
                        + " will not run under global policy");
                continue;
            }
            SecretKeyFactory skf = SecretKeyFactory.getInstance(baseAlgo, p);
            SecretKey key = skf.generateSecret(new PBEKeySpec("Secret Lover"
                    .toCharArray()));
            wrapTest(alg, alg, key, key, Cipher.SECRET_KEY, true);
        }
    }

    private void wrapperPublicPriviteKeyTest(Provider p, String[] algorithms)
            throws NoSuchAlgorithmException, InvalidKeyException,
            NoSuchPaddingException, IllegalBlockSizeException,
            InvalidAlgorithmParameterException {
        for (String algo : algorithms) {
            // Key pair generated
            System.out.println("Generate key pair (algorithm: " + algo
                    + ", provider: " + p.getName() + ")");
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(algo);
            kpg.initialize(512);
            KeyPair kp = kpg.genKeyPair();
            // key generated
            String algoWrap = "DES";
            KeyGenerator kg = KeyGenerator.getInstance(algoWrap, p);
            Key key = kg.generateKey();
            wrapTest(algo, algoWrap, key, kp.getPrivate(), Cipher.PRIVATE_KEY,
                    false);
            wrapTest(algo, algoWrap, key, kp.getPublic(), Cipher.PUBLIC_KEY,
                    false);
        }
    }

    private void wrapTest(String transformation, String wrapAlgo, Key initKey,
            Key wrapKey, int keyType, boolean isPBE)
            throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException,
            InvalidAlgorithmParameterException {
        String algo = transformation.split("/")[0];
        boolean isAESBlowfish = algo.indexOf("AES") != -1
                || algo.indexOf("Blowfish") != -1;
        AlgorithmParameters aps = null;
        AlgorithmParameterSpec pbeParams = null;
        if (isPBE) {
            byte[] salt = new byte[8];
            int iterCnt = 1000;
            new Random().nextBytes(salt);
            pbeParams = new PBEParameterSpec(salt, iterCnt);
        }

        out.println("Testing " + wrapAlgo + " cipher wrap/unwrap");
        // Wrap & UnWrap operation
        Cipher wrapCI = Cipher.getInstance(wrapAlgo);
        if (isPBE && !isAESBlowfish) {
            wrapCI.init(Cipher.WRAP_MODE, initKey, pbeParams);
        } else if (isAESBlowfish) {
            wrapCI.init(Cipher.WRAP_MODE, initKey);
            aps = wrapCI.getParameters();
        } else {
            wrapCI.init(Cipher.WRAP_MODE, initKey);
        }
        byte[] keyWrapper = wrapCI.wrap(wrapKey);
        if (isPBE && !isAESBlowfish) {
            wrapCI.init(Cipher.UNWRAP_MODE, initKey, pbeParams);
        } else if (isAESBlowfish) {
            wrapCI.init(Cipher.UNWRAP_MODE, initKey, aps);
        } else {
            wrapCI.init(Cipher.UNWRAP_MODE, initKey);
        }
        Key unwrappedKey = wrapCI.unwrap(keyWrapper, algo, keyType);
        // Comparison
        if (!Arrays.equals(wrapKey.getEncoded(), unwrappedKey.getEncoded())) {
            out.println("keysize : " + wrapKey.getEncoded().length);
            throw new RuntimeException("Comparation failed testing "
                    + transformation + ":" + wrapAlgo + ":" + keyType);
        }
    }
}
