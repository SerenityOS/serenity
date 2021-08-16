/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * Actual test code for the package private HKDF implementation
 */

package sun.security.ssl;

import java.util.Arrays;
import java.util.List;
import java.util.LinkedList;
import java.util.Objects;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class TestHkdf {
    public static class TestData {
        public TestData(String name, String algStr, String ikmStr,
                String saltStr, String infoStr, int oLen, String expPrkStr,
                String expOkmStr) {
            testName = Objects.requireNonNull(name);
            algName = Objects.requireNonNull(algStr);
            IKM = hex2bin(Objects.requireNonNull(ikmStr));
            if ((outLen = oLen) <= 0) {
                throw new IllegalArgumentException(
                        "Output length must be greater than 0");
            }
            expectedPRK = hex2bin(Objects.requireNonNull(expPrkStr));
            expectedOKM = hex2bin(Objects.requireNonNull(expOkmStr));

            // Non-mandatory fields - may be null
            salt = (saltStr != null) ? hex2bin(saltStr) : null;
            info = (infoStr != null) ? hex2bin(infoStr) : null;
        }

        public final String testName;
        public final String algName;
        public final byte[] IKM;
        public final byte[] salt;
        public final byte[] info;
        public final int outLen;
        public final byte[] expectedPRK;
        public final byte[] expectedOKM;
    }

    public static final List<TestData> testList = new LinkedList<TestData>() {{
        add(new TestData("RFC 5689 Test Case 1", "SHA-256",
            "0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b",
            "000102030405060708090a0b0c",
            "f0f1f2f3f4f5f6f7f8f9",
            42,
            "077709362c2e32df0ddc3f0dc47bba6390b6c73bb50f9c3122ec844ad7c2b3e5",
            "3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf" +
            "34007208d5b887185865"));
        add(new TestData("RFC 5689 Test Case 2", "SHA-256",
            "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f" +
            "202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f" +
            "404142434445464748494a4b4c4d4e4f",
            "606162636465666768696a6b6c6d6e6f707172737475767778797a7b7c7d7e7f" +
            "808182838485868788898a8b8c8d8e8f909192939495969798999a9b9c9d9e9f" +
            "a0a1a2a3a4a5a6a7a8a9aaabacadaeaf",
            "b0b1b2b3b4b5b6b7b8b9babbbcbdbebfc0c1c2c3c4c5c6c7c8c9cacbcccdcecf" +
            "d0d1d2d3d4d5d6d7d8d9dadbdcdddedfe0e1e2e3e4e5e6e7e8e9eaebecedeeef" +
            "f0f1f2f3f4f5f6f7f8f9fafbfcfdfeff",
            82,
            "06a6b88c5853361a06104c9ceb35b45cef760014904671014a193f40c15fc244",
            "b11e398dc80327a1c8e7f78c596a49344f012eda2d4efad8a050cc4c19afa97c" +
            "59045a99cac7827271cb41c65e590e09da3275600c2f09b8367793a9aca3db71" +
            "cc30c58179ec3e87c14c01d5c1f3434f1d87"));
        add(new TestData("RFC 5689 Test Case 3", "SHA-256",
            "0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b",
            null, null, 42,
            "19ef24a32c717b167f33a91d6f648bdf96596776afdb6377ac434c1c293ccb04",
            "8da4e775a563c18f715f802a063c5a31b8a11f5c5ee1879ec3454e5f3c738d2d" +
            "9d201395faa4b61a96c8"));
        add(new TestData("RFC 5689 Test Case 4", "SHA-1",
            "0b0b0b0b0b0b0b0b0b0b0b",
            "000102030405060708090a0b0c",
            "f0f1f2f3f4f5f6f7f8f9",
            42,
            "9b6c18c432a7bf8f0e71c8eb88f4b30baa2ba243",
            "085a01ea1b10f36933068b56efa5ad81a4f14b822f5b091568a9cdd4f155fda2" +
            "c22e422478d305f3f896"));
        add(new TestData("RFC 5689 Test Case 5", "SHA-1",
            "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f" +
            "202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f" +
            "404142434445464748494a4b4c4d4e4f",
            "606162636465666768696a6b6c6d6e6f707172737475767778797a7b7c7d7e7f" +
            "808182838485868788898a8b8c8d8e8f909192939495969798999a9b9c9d9e9f" +
            "a0a1a2a3a4a5a6a7a8a9aaabacadaeaf",
            "b0b1b2b3b4b5b6b7b8b9babbbcbdbebfc0c1c2c3c4c5c6c7c8c9cacbcccdcecf" +
            "d0d1d2d3d4d5d6d7d8d9dadbdcdddedfe0e1e2e3e4e5e6e7e8e9eaebecedeeef" +
            "f0f1f2f3f4f5f6f7f8f9fafbfcfdfeff",
            82,
            "8adae09a2a307059478d309b26c4115a224cfaf6",
            "0bd770a74d1160f7c9f12cd5912a06ebff6adcae899d92191fe4305673ba2ffe" +
            "8fa3f1a4e5ad79f3f334b3b202b2173c486ea37ce3d397ed034c7f9dfeb15c5e" +
            "927336d0441f4c4300e2cff0d0900b52d3b4"));
        add(new TestData("RFC 5689 Test Case 6", "SHA-1",
            "0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b",
            null, null, 42,
            "da8c8a73c7fa77288ec6f5e7c297786aa0d32d01",
            "0ac1af7002b3d761d1e55298da9d0506b9ae52057220a306e07b6b87e8df21d0" +
            "ea00033de03984d34918"));
        add(new TestData("RFC 5689 Test Case 7", "SHA-1",
            "0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c",
            null, null, 42,
            "2adccada18779e7c2077ad2eb19d3f3e731385dd",
            "2c91117204d745f3500d636a62f64f0ab3bae548aa53d423b0d1f27ebba6f5e5" +
            "673a081d70cce7acfc48"));
    }};

    public static void main(String args[]) throws Exception {
        int testsPassed = 0;

        int testNo = 0;
        for (TestData test : testList) {
            System.out.println("*** Test " + ++testNo + ": " +
                    test.testName);
            if (runVector(test)) {
                testsPassed++;
            }
        }

        System.out.println("Total tests: " + testList.size() +
                ", Passed: " + testsPassed + ", Failed: " +
                (testList.size() - testsPassed));
        if (testsPassed != testList.size()) {
            throw new RuntimeException("One or more tests failed.  " +
                    "Check output for details");
        }
    }

    private static boolean runVector(TestData testData)
            throws NoSuchAlgorithmException, InvalidKeyException {
        String kdfName, prfName;
        HKDF kdfHkdf;
        boolean result = true;
        SecretKey actualPRK;
        SecretKey actualOKM;
        byte[] deriveData;

        // Get an instance of the HKDF derivation engine
        kdfHkdf = new HKDF(testData.algName);

        // Set up the input keying material and the salt as a secret
        SecretKey ikmKey = new SecretKeySpec(testData.IKM, "HKDF-IKM");
        SecretKey saltKey = (testData.salt != null) ?
                new SecretKeySpec(testData.salt, "HKDF-Salt") : null;

        // *** HKDF-Extract-only testing
        System.out.println("* HKDF-Extract-Only:");
        actualPRK = kdfHkdf.extract(saltKey, ikmKey, "HKDF-PRK");
        result &= compareKeyAndData(actualPRK, testData.expectedPRK);

        // *** HKDF Expand-Only testing
        // For these tests, we'll use the actualPRK as the input key
        System.out.println("* HKDF-Expand-Only:");
        actualOKM = kdfHkdf.expand(actualPRK, testData.info, testData.outLen,
                "HKDF-OKM");
        result &= compareKeyAndData(actualOKM, testData.expectedOKM);

        // *** HKDF Extract-then-Expand testing
        // System.out.println("* HKDF-Extract-then-Expand:");
        // actualOKM = kdfHkdf.extractExpand(ikmKey, saltKey, testData.info,
        //         testData.outLen, "HKDF-OKM2");
        // result &= compareKeyAndData(actualOKM, testData.expectedOKM);

        return result;
    }

    /**
     * Compare actual key output from HKDF against an expected output value.
     *
     * @param outKey the KDF output in key form
     * @param expectedOut the expected value
     *
     * @return true if the underlying data for outKey, outData and
     * expectedOut are the same.
     */
    private static boolean compareKeyAndData(SecretKey outKey,
            byte[] expectedOut) {
        boolean result = false;

        if (Arrays.equals(outKey.getEncoded(), expectedOut)) {
            System.out.println("\t* Key output: Pass");
            result = true;
        } else {
            System.out.println("\t* Key output: FAIL");
            System.out.println("Expected:\n" +
                    dumpHexBytes(expectedOut, 16, "\n", " "));
            System.out.println("Actual:\n" +
                    dumpHexBytes(outKey.getEncoded(), 16, "\n", " "));
            System.out.println();
        }

        return result;
    }

    /**
     * Dump the hex bytes of a buffer into string form.
     *
     * @param data The array of bytes to dump to stdout.
     * @param itemsPerLine The number of bytes to display per line
     *      if the {@code lineDelim} character is blank then all bytes
     *      will be printed on a single line.
     * @param lineDelim The delimiter between lines
     * @param itemDelim The delimiter between bytes
     *
     * @return The hexdump of the byte array
     */
    private static String dumpHexBytes(byte[] data, int itemsPerLine,
            String lineDelim, String itemDelim) {
        StringBuilder sb = new StringBuilder();
        if (data != null) {
            for (int i = 0; i < data.length; i++) {
                if (i % itemsPerLine == 0 && i != 0) {
                    sb.append(lineDelim);
                }
                sb.append(String.format("%02X", data[i])).append(itemDelim);
            }
        }

        return sb.toString();
    }

    private static byte[] hex2bin(String hex) {
        int i;
        int len = hex.length();
        byte[] data = new byte [len / 2];
        for (i = 0; i < len; i += 2) {
            data[i / 2] = (byte)((Character.digit(hex.charAt(i), 16) << 4) +
                    Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
