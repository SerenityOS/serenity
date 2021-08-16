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

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Random;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.interfaces.PBEKey;
import javax.crypto.spec.PBEKeySpec;
import javax.security.auth.DestroyFailedException;

import static java.lang.System.out;

/*
 * @test
 * @bug 8048820
 * @summary The test verifies if the SecretKeyFactory.translateKey() method
 *  works as expected for the PBKDF2 algorithms.
 */

public class PBKDF2TranslateTest {

    private static final String PASS_PHRASE = "some hidden string";
    private static final int ITERATION_COUNT = 1000;
    private static final int KEY_SIZE = 128;
    private static final String[] TEST_ALGOS = { "PBKDF2WithHmacSHA1",
            "PBKDF2WithHmacSHA224", "PBKDF2WithHmacSHA256",
            "PBKDF2WithHmacSHA384", "PBKDF2WithHmacSHA512" };
    private final String algoForTest;

    public static void main(String[] args) throws Exception {
        for (String algo : TEST_ALGOS) {
            PBKDF2TranslateTest theTest = new PBKDF2TranslateTest(algo);
            byte[] salt = new byte[8];
            new Random().nextBytes(salt);
            theTest.testMyOwnSecretKey(salt);
            theTest.generateAndTranslateKey(salt);
            theTest.translateSpoiledKey(salt);
        }
    }

    public PBKDF2TranslateTest(String algo) {
        algoForTest = algo;
    }

    /**
     * The test case scenario implemented in the method: - derive PBKDF2 key
     * using the given algorithm; - translate the key - check if the translated
     * and original keys have the same key value.
     *
     */
    public void generateAndTranslateKey(byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            InvalidKeyException {
        // derive PBKDF2 key
        SecretKey key1 = getSecretKeyForPBKDF2(algoForTest, salt);

        // translate key
        SecretKeyFactory skf = SecretKeyFactory.getInstance(algoForTest);
        SecretKey key2 = skf.translateKey(key1);

        // Check if it still the same after translation
        if (!Arrays.equals(key1.getEncoded(), key2.getEncoded())) {
            System.out.println("Key1=" + new String(key1.getEncoded())
                    + " key2=" + new String(key2.getEncoded()) + " salt="
                    + new String(salt));
            throw new RuntimeException(
                    "generateAndTranslateKey test case failed: the  key1 and"
                            + " key2 values in its primary encoding format are"
                            + " not the same for " + algoForTest
                            + " algorithm.");
        }
    }

    /**
     * The test case scenario implemented in the method: - derive Key1 for the
     * given PBKDF2 algorithm - create my own secret Key2 as an instance of a
     * class implements PBEKey - translate Key2 - check if the key value of the
     * translated key and Key1 are the same.
     */
    private void testMyOwnSecretKey(byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            InvalidKeyException {
        SecretKey key1 = getSecretKeyForPBKDF2(algoForTest, salt);
        SecretKey key2 = getMyOwnSecretKey(salt);

        // Is it actually the same?
        if (!Arrays.equals(key1.getEncoded(), key2.getEncoded())) {
            throw new RuntimeException(
                    "We shouldn't be here. The key1 and key2 values in its"
                            + " primary encoding format have to be the same!");
        }

        // translate key
        SecretKeyFactory skf = SecretKeyFactory.getInstance(algoForTest);
        SecretKey key3 = skf.translateKey(key2);

        // Check if it still the same after translation
        if (!Arrays.equals(key1.getEncoded(), key3.getEncoded())) {
            System.out.println("Key1=" + new String(key1.getEncoded())
                    + " key3=" + new String(key3.getEncoded()) + " salt="
                    + new String(salt));
            throw new RuntimeException(
                    "testMyOwnSecretKey test case failed: the key1  and key3"
                            + " values in its primary encoding format are not"
                            + " the same for " + algoForTest + " algorithm.");
        }

    }

    /**
     * The test case scenario implemented in the method: - create my own secret
     * Key2 as an instance of a class implements PBEKey - spoil the key (set
     * iteration count to 0, for example) - try to translate key -
     * InvalidKeyException is expected.
     */
    public void translateSpoiledKey(byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        // derive the key
        SecretKey key1 = getMyOwnSecretKey(salt);

        // spoil the key
        ((MyPBKDF2SecretKey) key1).spoil();

        // translate key
        SecretKeyFactory skf = SecretKeyFactory.getInstance(algoForTest);
        try {
            skf.translateKey(key1);
            throw new RuntimeException(
                    "translateSpoiledKey test case failed, should throw"
                            + " InvalidKeyException when spoil the key");
        } catch (InvalidKeyException ike) {
            out.println("Expected exception when spoil the key");
        }

    }

    /**
     * Generate a PBKDF2 secret key using given algorithm.
     */
    private SecretKey getSecretKeyForPBKDF2(String algoDeriveKey, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        SecretKeyFactory skf = SecretKeyFactory.getInstance(algoDeriveKey);
        PBEKeySpec spec = new PBEKeySpec(PASS_PHRASE.toCharArray(), salt,
                ITERATION_COUNT, KEY_SIZE);

        return skf.generateSecret(spec);
    }

    /**
     * Generate a secrete key as an instance of a class implements PBEKey.
     */
    private SecretKey getMyOwnSecretKey(byte[] salt)
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        return new MyPBKDF2SecretKey(PASS_PHRASE, algoForTest, salt,
                ITERATION_COUNT, KEY_SIZE);
    }

    /**
     * An utility class to check the SecretKeyFactory.translateKey() method.
     */
    class MyPBKDF2SecretKey implements PBEKey {
        private final byte[] key;
        private final byte[] salt;
        private final String algorithm;
        private final int keyLength;
        private final String pass;
        private int itereationCount;

        /**
         * The key is generating by SecretKeyFactory and its value just copying
         * in the key field of MySecretKey class. So, this is real key derived
         * using the given algo.
         */
        public MyPBKDF2SecretKey(String passPhrase, String algo, byte[] salt1,
                int iterationCount, int keySize)
                throws InvalidKeySpecException, NoSuchAlgorithmException {
            algorithm = algo;
            salt = salt1;
            itereationCount = iterationCount;
            pass = passPhrase;

            PBEKeySpec spec = new PBEKeySpec(passPhrase.toCharArray(), salt,
                    iterationCount, keySize);

            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(algo);

            SecretKey realKey = keyFactory.generateSecret(spec);

            keyLength = realKey.getEncoded().length;

            key = new byte[keyLength];
            System.arraycopy(realKey.getEncoded(), 0, key, 0, keyLength);
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
            byte[] copy = new byte[keyLength];
            System.arraycopy(key, 0, copy, 0, keyLength);
            return copy;
        }

        @Override
        public int getIterationCount() {
            return itereationCount;
        }

        @Override
        public byte[] getSalt() {
            return salt;
        }

        @Override
        public char[] getPassword() {
            return pass.toCharArray();
        }

        /**
         * Spoil the generated key (before translation) to cause an
         * InvalidKeyException
         */
        public void spoil() {
            itereationCount = -1;
        }

        @Override
        public void destroy() throws DestroyFailedException {
        }

        @Override
        public boolean isDestroyed() {
            return false;
        }

    }
}
