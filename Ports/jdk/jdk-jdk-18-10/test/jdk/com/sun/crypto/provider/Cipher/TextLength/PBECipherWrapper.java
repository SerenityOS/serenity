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

import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * PBECipherWrapper is the abstract class for all concrete PBE Cipher wrappers.
 */
public abstract class PBECipherWrapper {

    public static final int ITERATION_COUNT = 1000;
    private final String algorithm;
    private final byte[] salt;
    protected SecretKey key;
    protected Cipher ci;
    protected String baseAlgo;
    protected byte[] resultText = null;
    protected AlgorithmParameterSpec aps = null;

    public PBECipherWrapper(String algorithm, int saltSize) {
        this.algorithm = algorithm;
        baseAlgo = algorithm.split("/")[0].toUpperCase();
        salt = generateSalt(saltSize);
    }

    protected abstract void initCipher(int mode) throws InvalidKeyException,
            InvalidAlgorithmParameterException, InvalidParameterSpecException;

    public void execute(int edMode, byte[] inputText)
            throws InvalidAlgorithmParameterException,
            InvalidParameterSpecException, IllegalBlockSizeException,
            BadPaddingException, ShortBufferException, InvalidKeyException {
        // Initialize
        initCipher(edMode);

        // Generate a resultText using a single-part enc/dec
        resultText = ci.doFinal(inputText);

        // Generate outputText for each multi-part en/de-cryption
        /* Combination #1:
        update(byte[], int, int)
        doFinal(byte[], int, int)
         */
        byte[] part11 = ci.update(inputText, 0, inputText.length);
        byte[] part12 = ci.doFinal();
        byte[] outputText1 = new byte[part11.length + part12.length];
        System.arraycopy(part11, 0, outputText1, 0, part11.length);
        System.arraycopy(part12, 0, outputText1, part11.length, part12.length);

        List<byte[]> outputTexts = new ArrayList<>(4);
        outputTexts.add(outputText1);

        /* Combination #2:
        update(byte[], int, int)
        doFinal(byte[], int, int, byte[], int)
         */
        byte[] part21 = ci.update(inputText, 0, inputText.length - 5);
        byte[] part22 = new byte[ci.getOutputSize(inputText.length)];
        int len2 = ci.doFinal(inputText, inputText.length - 5, 5, part22, 0);
        byte[] outputText2 = new byte[part21.length + len2];
        System.arraycopy(part21, 0, outputText2, 0, part21.length);
        System.arraycopy(part22, 0, outputText2, part21.length, len2);

        outputTexts.add(outputText2);

        /* Combination #3:
        update(byte[], int, int, byte[], int)
        doFinal(byte[], int, int)
         */
        byte[] part31 = new byte[ci.getOutputSize(inputText.length)];
        int len3 = ci.update(inputText, 0, inputText.length - 8, part31, 0);
        byte[] part32 = ci.doFinal(inputText, inputText.length - 8, 8);
        byte[] outputText3 = new byte[len3 + part32.length];
        System.arraycopy(part31, 0, outputText3, 0, len3);
        System.arraycopy(part32, 0, outputText3, len3, part32.length);

        outputTexts.add(outputText3);

        /* Combination #4:
        update(byte[], int, int, byte[], int)
        doFinal(byte[], int, int, byte[], int)
         */
        byte[] part41 = new byte[ci.getOutputSize(inputText.length)];
        int len4 = ci.update(inputText, 0, inputText.length - 8, part41, 0);
        int rest4 = ci
                .doFinal(inputText, inputText.length - 8, 8, part41, len4);
        byte[] outputText4 = new byte[len4 + rest4];
        System.arraycopy(part41, 0, outputText4, 0, outputText4.length);

        outputTexts.add(outputText4);

        // Compare results
        for (int k = 0; k < outputTexts.size(); k++) {
            if (!Arrays.equals(resultText, outputTexts.get(k))) {
                throw new RuntimeException(
                        "Compare value of resultText and combination " + k
                        + " are not same. Test failed.");
            }
        }

    }

    public final byte[] generateSalt(int numberOfBytes) {
        byte[] aSalt = new byte[numberOfBytes];
        for (int i = 0; i < numberOfBytes; i++) {
            aSalt[i] = (byte) (i & 0xff);
        }
        return aSalt;
    }

    public byte[] getResult() {
        return resultText;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public byte[] getSalt() {
        return salt;
    }

    /**
     * Wrapper class to test a given SecretKeyFactory.PBKDF2 algorithm.
     */
    public static class PBKDF2 extends PBECipherWrapper {

        private static final int PBKDF2_SALT_SIZE = 64;
        private static final int CIPHER_KEY_SIZE = 128;
        private static final String CIPHER_TRANSFORMATION = "AES/CBC/PKCS5Padding";
        private static final String KEY_ALGORITHM = "AES";
        private byte[] iv = null;

        public PBKDF2(String algo, String passwd)
                throws InvalidKeySpecException, NoSuchAlgorithmException,
                NoSuchPaddingException {
            super(algo, PBKDF2_SALT_SIZE);

            ci = Cipher.getInstance(CIPHER_TRANSFORMATION);

            PBEKeySpec pbeKeySpec = new PBEKeySpec(passwd.toCharArray(), getSalt(),
                    ITERATION_COUNT, CIPHER_KEY_SIZE);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(algo);
            key = keyFactory.generateSecret(pbeKeySpec);
        }

        @Override
        protected void initCipher(int mode) throws InvalidKeyException,
                InvalidAlgorithmParameterException, InvalidParameterSpecException {
            if (Cipher.ENCRYPT_MODE == mode) {
                ci.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key.getEncoded(),
                        KEY_ALGORITHM));
                iv = ci.getParameters().getParameterSpec(IvParameterSpec.class)
                        .getIV();
            } else {
                ci.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key.getEncoded(),
                        KEY_ALGORITHM), new IvParameterSpec(iv));
            }
        }
    }

    /**
     * Wrapper class to test a given AES-based PBE algorithm.
     */
    public static class AES extends PBECipherWrapper {

        private AlgorithmParameters pbeParams;

        public AES(String algo, String passwd)
                throws NoSuchAlgorithmException, NoSuchPaddingException,
                InvalidKeySpecException {
            super(algo, 0);

            ci = Cipher.getInstance(algo);

            SecretKeyFactory skf = SecretKeyFactory.getInstance(algo);
            key = skf.generateSecret(new PBEKeySpec(passwd.toCharArray()));
        }

        @Override
        protected void initCipher(int mode) throws InvalidKeyException,
                InvalidAlgorithmParameterException, InvalidParameterSpecException {
            if (Cipher.ENCRYPT_MODE == mode) {
                ci.init(Cipher.ENCRYPT_MODE, key);
                pbeParams = ci.getParameters();
            } else {
                ci.init(Cipher.DECRYPT_MODE, key, pbeParams);
            }
        }
    }

    /**
     * Wrapper class to test a given PBE algorithm.
     */
    public static class Legacy extends PBECipherWrapper {

        private static final int PBE_SALT_SIZE = 8;

        public Legacy(String algo, String passwd)
                throws NoSuchAlgorithmException, NoSuchPaddingException,
                InvalidKeySpecException {
            super(algo, PBE_SALT_SIZE);

            SecretKeyFactory skf = SecretKeyFactory.getInstance(algo.split("/")[0]);
            key = skf.generateSecret(new PBEKeySpec(passwd.toCharArray()));

            aps = new PBEParameterSpec(getSalt(), ITERATION_COUNT);

            ci = Cipher.getInstance(algo);
        }

        @Override
        protected void initCipher(int mode) throws InvalidKeyException,
                InvalidAlgorithmParameterException, InvalidParameterSpecException {
            ci.init(mode, key, aps);
        }
    }
}
