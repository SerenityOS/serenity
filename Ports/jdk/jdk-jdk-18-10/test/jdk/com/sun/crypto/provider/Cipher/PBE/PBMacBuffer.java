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

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Random;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * @test
 * @bug 8041787
 * @summary verify that Mac.update works with different size ByteBuffer
 * @author Alexander Fomin
 * @run main PBMacBuffer
 */
public class PBMacBuffer {

    private final int LARGE_SIZE = 500000;

    public static void main(String[] args) {
        String[] PBMAC1Algorithms = {
            "HmacPBESHA1",
            "PBEWithHmacSHA1",
            "PBEWithHmacSHA224",
            "PBEWithHmacSHA256",
            "PBEWithHmacSHA384",
            "PBEWithHmacSHA512"
        };

        String[] PBKDF2Algorithms = {
            "PBKDF2WithHmacSHA1",
            "PBKDF2WithHmacSHA224",
            "PBKDF2WithHmacSHA256",
            "PBKDF2WithHmacSHA384",
            "PBKDF2WithHmacSHA512"
        };

        PBMacBuffer testRunner = new PBMacBuffer();
        boolean failed = false;

        for (String thePBMacAlgo : PBMAC1Algorithms) {

            for (String thePBKDF2Algo : PBKDF2Algorithms) {

                System.out.println("Running test with " + thePBMacAlgo
                        + " and " + thePBKDF2Algo + ":");
                try {
                    if (!testRunner.doTest(thePBMacAlgo, thePBKDF2Algo)) {
                        failed = true;
                    }
                } catch (NoSuchAlgorithmException | InvalidKeyException |
                        InvalidKeySpecException e) {
                    failed = true;
                    e.printStackTrace(System.out);
                    System.out.println("Test FAILED.");
                }
            }
        }

        if (failed) {
            throw new RuntimeException("One or more tests failed....");
        }
    }

    /**
     * Tests Mac.update(ByteBuffer input) method. Three test cases are
     * performed: - large ByteBuffer test case to test if the update() method
     * process a large ByteBuffer correctly; - empty ByteBuffer test case to
     * test if the update() method process an empty ByteBuffer correctly; - NULL
     * ByteBuffer test case to test if the update() method throws expected
     * IllegalArgumentException exception.
     *
     * @param theMacAlgo PBMAC algorithm to test
     * @param thePBKDF2Algo PBKDF2 algorithm to test
     * @return true - test passed; false - otherwise.
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws InvalidKeySpecException
     * @see javax.crypto.Mac
     */
    protected boolean doTest(String theMacAlgo, String thePBKDF2Algo)
            throws NoSuchAlgorithmException, InvalidKeyException,
            InvalidKeySpecException {
        // obtain a SecretKey using PBKDF2
        SecretKey key = getSecretKey(thePBKDF2Algo);

        // Instantiate Mac object and init it with a SecretKey
        Mac theMac = Mac.getInstance(theMacAlgo);
        theMac.init(key);

        // Do large ByteBuffer test case
        if (!largeByteBufferTest(theMac)) {
            System.out.println("Large ByteBuffer test case failed.");
            return false;
        }

        // Do empty ByteBuffer test case
        if (!emptyByteBufferTest(theMac)) {
            System.out.println("Empty ByteBuffer test case failed.");
            return false;
        }

        // Do null ByteBuffer test case
        if (!nullByteBufferTest(theMac)) {
            System.out.println("NULL ByteBuffer test case failed.");
            return false;
        }

        return true;
    }

    /**
     * Large ByteBuffer test case. Generate random ByteBuffer of LARGE_SIZE
     * size. Performs MAC operation with the given Mac object (theMac
     * parameter).Verifies the assertion "Upon return, the buffer's position
     * will be equal to its limit; its limit will not have changed".
     *
     * @param theMac MAC object to test.
     * @return true - test case passed; false - otherwise;
     */
    protected boolean largeByteBufferTest(Mac theMac) {
        ByteBuffer buf = generateRandomByteBuffer(LARGE_SIZE);
        int limitBefore = buf.limit();

        theMac.update(buf);
        theMac.doFinal();

        int limitAfter = buf.limit();
        int positonAfter = buf.position();

        if (limitAfter != limitBefore) {
            System.out.println("FAIL: Buffer's limit has been chenged.");
            return false;
        }

        if (positonAfter != limitAfter) {
            System.out.println("FAIL: "
                    + "Buffer's position isn't equal to its limit");
            return false;
        }

        return true;
    }

    /**
     * Empty ByteBuffer test case. Generates an empty ByteBuffer. Perform MAC
     * operation. No exceptions are expected.
     *
     * @param theMac
     * @return true - test case pass; exception otherwise
     */
    protected boolean emptyByteBufferTest(Mac theMac) {
        ByteBuffer buf = generateRandomByteBuffer(0);
        theMac.update(buf);
        theMac.doFinal();
        return true;
    }

    /**
     * NULL ByteBuffer test case. Pass NULL ByteBuffer to Mac.update(ByteBuffer
     * buffer) method. An IllegalArgumentException expected.
     *
     * @param theMac Mac object to test.
     * @return true - test case pass; false - otherwise.
     */
    protected boolean nullByteBufferTest(Mac theMac) {
        try {
            ByteBuffer buf = null;
            theMac.update(buf);
            theMac.doFinal();
        } catch (IllegalArgumentException e) {
            // expected exception has been thrown
            return true;
        }

        System.out.println("FAIL: "
                + "IllegalArgumentException hasn't been thrown as expected");

        return false;
    }

    /**
     * Get SecretKey for the given PBKDF2 algorithm.
     *
     * @param thePBKDF2Algorithm - PBKDF2 algorithm
     * @return SecretKey according to thePBKDF2Algorithm
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    protected SecretKey getSecretKey(String thePBKDF2Algorithm)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        // Prepare salt
        byte[] salt = new byte[64]; // PKCS #5 v2.1 recommendation
        new SecureRandom().nextBytes(salt);

        // Generate secret key
        PBEKeySpec pbeKeySpec = new PBEKeySpec(
                "A #pwd# implied to be hidden!".toCharArray(),
                salt, 1000, 128);
        SecretKeyFactory keyFactory
                = SecretKeyFactory.getInstance(thePBKDF2Algorithm);
        return keyFactory.generateSecret(pbeKeySpec);
    }

    /**
     * An utility method to generate a random ByteBuffer of the requested size.
     *
     * @param size size of the ByteBuffer.
     * @return ByteBuffer populated random data;
     */
    private ByteBuffer generateRandomByteBuffer(int size) {
        // generate randome byte array
        byte[] data = new byte[size];
        new Random().nextBytes(data);

        // create ByteBuffer
        ByteBuffer bb = ByteBuffer.wrap(data);

        return bb;
    }

}
