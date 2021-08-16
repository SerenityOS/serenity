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

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Random;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.interfaces.PBEKey;
import javax.crypto.spec.PBEKeySpec;

/**
 * @test
 * @bug 8041781
 * @summary Verify if the SecretKeyFactory.translateKey() method works
 * @author Alexander Fomin
 * @run main PBKDF2Translate
 * @key randomness
 */
public class PBKDF2Translate {

    private static final String[] ALGO_TO_TEST = {
        "PBKDF2WithHmacSHA1",
        "PBKDF2WithHmacSHA224",
        "PBKDF2WithHmacSHA256",
        "PBKDF2WithHmacSHA384",
        "PBKDF2WithHmacSHA512"
    };

    private static final String PASS_PHRASE = "some hidden string";
    private static final int ITERATION_COUNT = 1000;
    private static final int KEY_SIZE = 128;

    private final String algoToTest;
    private final byte[] salt = new byte[8];

    public static void main(String[] args) throws Exception {

        boolean failed = false;

        for (String algo : ALGO_TO_TEST) {

            System.out.println("Testing " + algo + ":");
            PBKDF2Translate theTest = new PBKDF2Translate(algo);

            try {
                if (!theTest.testMyOwnSecretKey()
                        || !theTest.generateAndTranslateKey()
                        || !theTest.translateSpoiledKey()) {
                    // we don't want to set failed to false
                    failed = true;
                }
            } catch (InvalidKeyException | NoSuchAlgorithmException |
                    InvalidKeySpecException e) {
                e.printStackTrace(System.err);
                failed = true;
            }
        }

        if (failed) {
            throw new RuntimeException("One or more tests failed....");
        }
    }

    public PBKDF2Translate(String algoToTest) {
        this.algoToTest = algoToTest;
        new Random().nextBytes(this.salt);
    }

    /**
     * The test case scenario implemented in the method: - derive PBKDF2 key
     * using the given algorithm; - translate the key - check if the translated
     * and original keys have the same key value.
     *
     * @return true if the test case passed; false - otherwise.
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     * @throws InvalidKeyException
     */
    public boolean generateAndTranslateKey() throws NoSuchAlgorithmException,
            InvalidKeySpecException, InvalidKeyException {
        // derive PBKDF2 key
        SecretKey key1 = getSecretKeyForPBKDF2(algoToTest);

        // translate key
        SecretKeyFactory skf = SecretKeyFactory.getInstance(algoToTest);
        SecretKey key2 = skf.translateKey(key1);

        // check if it still the same after translation
        if (!Arrays.equals(key1.getEncoded(), key2.getEncoded())) {
            System.err.println("generateAndTranslateKey test case failed: the "
                    + "key1 and key2 values in its primary encoding format are "
                    + "not the same for " + algoToTest + "algorithm.");
            return false;
        }

        return true;
    }

    /**
     * The test case scenario implemented in the method: - derive Key1 for the
     * given PBKDF2 algorithm - create my own secret Key2 as an instance of a
     * class implements PBEKey - translate Key2 - check if the key value of the
     * translated key and Key1 are the same.
     *
     * @return true if the test case passed; false - otherwise.
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     * @throws InvalidKeyException
     */
    public boolean testMyOwnSecretKey()
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            InvalidKeyException {
        SecretKey key1 = getSecretKeyForPBKDF2(algoToTest);
        SecretKey key2 = getMyOwnSecretKey();

        // Is it actually the same?
        if (!Arrays.equals(key1.getEncoded(), key2.getEncoded())) {
            System.err.println("We shouldn't be here. The key1 and key2 values "
                    + "in its primary encoding format have to be the same!");
            return false;
        }

        // Translate key
        SecretKeyFactory skf = SecretKeyFactory.getInstance(algoToTest);
        SecretKey key3 = skf.translateKey(key2);

        // Check if it still the same after translation
        if (!Arrays.equals(key1.getEncoded(), key3.getEncoded())) {
            System.err.println("testMyOwnSecretKey test case failed: the key1 "
                    + "and key3 values in its primary encoding format are not "
                    + "the same for " + algoToTest + "algorithm.");
            return false;
        }

        return true;
    }

    /**
     * The test case scenario implemented in the method: - create my own secret
     * Key2 as an instance of a class implements PBEKey - spoil the key (set
     * iteration count to 0, for example) - try to translate key -
     * InvalidKeyException is expected.
     *
     * @return true if InvalidKeyException occurred; false - otherwise.
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    public boolean translateSpoiledKey() throws NoSuchAlgorithmException,
            InvalidKeySpecException {
        // derive the key
        SecretKey key1 = getMyOwnSecretKey();

        // spoil the key
        ((MyPBKDF2SecretKey) key1).spoil();

        // translate key
        SecretKeyFactory skf = SecretKeyFactory.getInstance(algoToTest);
        try {
            SecretKey key2 = skf.translateKey(key1);
        } catch (InvalidKeyException ike) {
            // this is expected
            return true;
        }

        return false;
    }

    /**
     * Generate a PBKDF2 secret key using given algorithm.
     *
     * @param algoToDeriveKey PBKDF2 algorithm
     * @return PBKDF2 secret key
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    private SecretKey getSecretKeyForPBKDF2(String algoToDeriveKey)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory skf = SecretKeyFactory.getInstance(algoToDeriveKey);

        PBEKeySpec spec = new PBEKeySpec(PASS_PHRASE.toCharArray(),
                this.salt, ITERATION_COUNT, KEY_SIZE);

        return skf.generateSecret(spec);
    }

    /**
     * Generate a secrete key as an instance of a class implements PBEKey.
     *
     * @return secrete key
     * @throws InvalidKeySpecException
     * @throws NoSuchAlgorithmException
     */
    private SecretKey getMyOwnSecretKey() throws InvalidKeySpecException,
            NoSuchAlgorithmException {
        return new MyPBKDF2SecretKey(PASS_PHRASE, this.algoToTest, this.salt,
                ITERATION_COUNT, KEY_SIZE);
    }
}

/**
 * An utility class to check the SecretKeyFactory.translateKey() method.
 */
class MyPBKDF2SecretKey implements PBEKey {

    private final byte[] key;
    private final byte[] salt;
    private final String algorithm;
    private final int keySize, keyLength;
    private int itereationCount;
    private final String pass;

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
        System.arraycopy(this.key, 0, copy, 0, keyLength);
        return copy;
    }

    /**
     * The key is generating by SecretKeyFactory and its value just copying in
     * the key field of MySecretKey class. So, this is real key derived using
     * the given algorithm.
     *
     * @param passPhrase some string intended to be a password
     * @param algo PBKDF2 algorithm
     * @param salt slat for PBKDF2
     * @param iterationCount iteration count
     * @param keySize key size in bits
     * @throws InvalidKeySpecException
     * @throws NoSuchAlgorithmException
     */
    public MyPBKDF2SecretKey(String passPhrase, String algo, byte[] salt,
            int iterationCount, int keySize)
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        this.algorithm = algo;
        this.salt = salt;
        this.itereationCount = iterationCount;
        this.keySize = keySize;
        this.pass = passPhrase;

        PBEKeySpec spec = new PBEKeySpec(passPhrase.toCharArray(),
                this.salt, iterationCount, this.keySize);

        SecretKeyFactory keyFactory
                = SecretKeyFactory.getInstance(algo);

        SecretKey realKey = keyFactory.generateSecret(spec);

        this.keyLength = realKey.getEncoded().length;

        this.key = new byte[this.keyLength];
        System.arraycopy(realKey.getEncoded(), 0, this.key, 0,
                this.keyLength);
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
        return this.pass.toCharArray();
    }

    /**
     * Spoil the generated key (before translation) to cause an
     * InvalidKeyException
     */
    public void spoil() {
        this.itereationCount = -1;
    }

}
