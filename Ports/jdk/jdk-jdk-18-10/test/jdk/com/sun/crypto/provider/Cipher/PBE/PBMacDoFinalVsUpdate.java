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
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * @test
 * @bug 8041787
 * @summary Check if doFinal and update operation result in same PBMac
 * @author Alexander Fomin
 * @run main PBMacDoFinalVsUpdate
 */
public class PBMacDoFinalVsUpdate {

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

        PBMacDoFinalVsUpdate testRunner = new PBMacDoFinalVsUpdate();
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
     * Uses a random generator to initialize a message, instantiate a Mac object
     * according to the given PBMAC1 algorithm, initialize the object with a
     * SecretKey derived using PBKDF2 algorithm (see PKCS #5 v21, chapter 7.1),
     * feed the message into the Mac object all at once and get the output MAC
     * as result1. Reset the Mac object, chop the message into three pieces,
     * feed into the Mac object sequentially, and get the output MAC as result2.
     * Finally, compare result1 and result2 and see if they are the same.
     *
     * @param theMacAlgo PBMAC algorithm to test
     * @param thePBKDF2Algo PBKDF2 algorithm to test
     * @return true - the test is passed; false - otherwise.
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws InvalidKeySpecException
     */
    protected boolean doTest(String theMacAlgo, String thePBKDF2Algo)
            throws NoSuchAlgorithmException, InvalidKeyException,
            InvalidKeySpecException {
        int OFFSET = 5;

        // Some message for which a MAC result will be calculated
        byte[] plain = new byte[25];
        new SecureRandom().nextBytes(plain);

        // Form tail - is one of the three pieces
        byte[] tail = new byte[plain.length - OFFSET];
        System.arraycopy(plain, OFFSET, tail, 0, tail.length);

        // Obtain a SecretKey using PBKDF2
        SecretKey key = getSecretKey(thePBKDF2Algo);

        // Instantiate Mac object and init it with a SecretKey and calc result1
        Mac theMac = Mac.getInstance(theMacAlgo);
        theMac.init(key);
        byte[] result1 = theMac.doFinal(plain);

        if (!isMacLengthExpected(theMacAlgo, result1.length)) {
            return false;
        }

        // Reset Mac and calculate result2
        theMac.reset();
        theMac.update(plain[0]);
        theMac.update(plain, 1, OFFSET - 1);
        byte[] result2 = theMac.doFinal(tail);

        // Return result
        if (!java.util.Arrays.equals(result1, result2)) {
            System.out.println("result1 and result2 are not the same:");
            System.out.println("result1: " + dumpByteArray(result1));
            System.out.println("result2: " + dumpByteArray(result2));
            return false;
        } else {
            System.out.println("Resulted MAC with update and doFinal is same");
        }

        return true;
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
     * Check if the lengthToCheck is expected length for the given MACAlgo.
     *
     * @param MACAlgo PBMAC algorithm
     * @param lengthToCheck the length of MAC need to check
     * @return true - lengthToCheck is expected length for the MACAlgo; false -
     * otherwise.
     */
    protected boolean isMacLengthExpected(String MACAlgo, int lengthToCheck) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d+)",
                java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher m = p.matcher(MACAlgo);
        int val = 0;

        if (m.find()) {
            val = Integer.parseInt(m.group(1));
        }

        // HmacPBESHA1 should return MAC 20 byte length
        if ((val == 1) && (lengthToCheck == 20)) {
            return true;
        }

        return (val / 8) == lengthToCheck;
    }

    /**
     * An utility method to dump a byte array for debug output.
     *
     * @param theByteArray the byte array to dump
     * @return string representation of the theByteArray in Hex.
     */
    protected String dumpByteArray(byte[] theByteArray) {
        StringBuilder buf = new StringBuilder();

        for (byte b : theByteArray) {
            buf.append(Integer.toHexString(b));
        }

        return buf.toString();
    }

}
