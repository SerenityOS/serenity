/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.security.*;
import java.security.spec.*;
import jdk.test.lib.RandomFactory;

/*
 * @test
 * @bug 8050374 8181048 8146293
 * @key randomness
 * @summary This test validates signature verification
 *          Signature.verify(byte[], int, int). The test uses RandomFactory to
 *          get random set of clear text data to sign. After the signature
 *          generation, the test tries to verify signature with the above API
 *          and passing in different signature offset (0, 33, 66, 99).
 * @library /test/lib
 * @build jdk.test.lib.RandomFactory
 * @run main Offsets SUN NONEwithDSA
 * @run main Offsets SUN SHA1withDSA
 * @run main Offsets SUN SHA224withDSA
 * @run main Offsets SUN SHA256withDSA
 * @run main Offsets SunRsaSign SHA224withRSA
 * @run main Offsets SunRsaSign SHA256withRSA
 * @run main Offsets SunRsaSign SHA384withRSA
 * @run main Offsets SunRsaSign SHA512withRSA
 * @run main Offsets SunRsaSign SHA512/224withRSA
 * @run main Offsets SunRsaSign SHA512/256withRSA
 */
public class Offsets {

    private final int size;
    private final byte[] cleartext;
    private final PublicKey pubkey;
    private final Signature signature;
    private final byte[] signed;

    private Offsets(Signature signature, PublicKey pubkey, PrivateKey privkey,
            int size, byte[] cleartext) throws InvalidKeyException,
                SignatureException {
        System.out.println("Testing signature " + signature.getAlgorithm());
        this.pubkey = pubkey;
        this.signature = signature;
        this.size = size;
        this.cleartext = cleartext;

        String sigAlg = signature.getAlgorithm();
        signature.initSign(privkey);
        signature.update(cleartext, 0, size);
        signed = signature.sign();
    }

    int getDataSize() {
        return size;
    }

    int getSignatureLength() {
        return signed.length;
    }

    byte[] shiftSignData(int offset) {
        byte[] testSignData = new byte[offset + signed.length];
        System.arraycopy(signed, 0, testSignData, offset,
                signed.length);
        return testSignData;
    }

    boolean verifySignature(byte[] sigData, int sigOffset, int sigLength,
            int updateOffset, int updateLength)
            throws InvalidKeyException, SignatureException {
        signature.initVerify(pubkey);
        signature.update(cleartext, updateOffset, updateLength);
        return signature.verify(sigData, sigOffset, sigLength);
    }

    static Offsets init(String provider, String algorithm)
            throws NoSuchAlgorithmException, NoSuchProviderException,
            InvalidKeyException, SignatureException {
        // fill the cleartext data with random bytes
        byte[] cleartext = new byte[100];
        RandomFactory.getRandom().nextBytes(cleartext);

        // NONEwith requires input to be of 20 bytes
        int size = algorithm.contains("NONEwith") ? 20 : 100;

        // create signature instance
        Signature signature = Signature.getInstance(algorithm, provider);

        String keyAlgo;
        int keySize = 2048;
        if (algorithm.contains("RSA")) {
            keyAlgo = "RSA";
        } else if (algorithm.contains("ECDSA")) {
            keyAlgo = "EC";
            keySize = 256;
        } else if (algorithm.contains("DSA")) {
            keyAlgo = "DSA";
            if (algorithm.startsWith("SHAwith") ||
                    algorithm.startsWith("SHA1with")) {
                keySize = 1024;
            }
        } else {
            throw new RuntimeException("Test doesn't support this signature "
                    + "algorithm: " + algorithm);
        }
        KeyPairGenerator kpg = null;
        // first try matching provider, fallback to most preferred if none available
        try {
            kpg = KeyPairGenerator.getInstance(keyAlgo, provider);
        } catch (NoSuchAlgorithmException nsae) {
            kpg = KeyPairGenerator.getInstance(keyAlgo);
        }
        kpg.initialize(keySize);
        KeyPair kp = kpg.generateKeyPair();
        PublicKey pubkey = kp.getPublic();
        PrivateKey privkey = kp.getPrivate();

        return new Offsets(signature, pubkey, privkey, size, cleartext);
    }

    public static void main(String[] args) throws NoSuchAlgorithmException,
            InvalidKeyException, SignatureException {
        if (args.length < 2) {
            throw new RuntimeException("Wrong parameters");
        }

        boolean result = true;
        try {
            Offsets test = init(args[0], args[1]);

            // We are trying 3 different offsets, data size has nothing to do
            // with signature length
            for (int chunk = 3; chunk > 0; chunk--) {
                int signOffset = test.getDataSize() / chunk;

                System.out.println("Running test with offset " + signOffset);
                byte[] signData = test.shiftSignData(signOffset);

                boolean success = test.verifySignature(signData, signOffset,
                        test.getSignatureLength(), 0, test.getDataSize());

                if (success) {
                    System.out.println("Successfully verified with offset "
                            + signOffset);
                } else {
                    System.out.println("Verification failed with offset "
                            + signOffset);
                    result = false;
                }
            }

            // save signature to offset 0
            byte[] signData = test.shiftSignData(0);

            // Negative tests

            // Test signature offset 0.
            // Wrong test data will be passed to update,
            // so signature verification should fail.
            for (int chunk = 3; chunk > 0; chunk--) {
                int dataOffset = (test.getDataSize() - 1) / chunk;
                boolean success;
                try {
                    success = test.verifySignature(signData, 0,
                            test.getSignatureLength(), dataOffset,
                            (test.getDataSize() - dataOffset));
                } catch (SignatureException e) {
                    // Since we are trying different data size, it can throw
                    // SignatureException
                    success = false;
                }

                if (!success) {
                    System.out.println("Signature verification failed "
                            + "as expected, with data offset " + dataOffset
                            + " and length "
                            + (test.getDataSize() - dataOffset));
                } else {
                    System.out.println("Signature verification "
                            + "should not succeed, with data offset "
                            + dataOffset + " and length "
                            + (test.getDataSize() - dataOffset));
                    result = false;
                }
            }

            // Tests with manipulating offset and length
            result &= Offsets.checkFailure(test, signData, -1,
                    test.getSignatureLength());

            result &= Offsets.checkFailure(test, signData, 0,
                    test.getSignatureLength() - 1);

            result &= Offsets.checkFailure(test, signData,
                    test.getSignatureLength() + 1, test.getSignatureLength());

            result &= Offsets.checkFailure(test, signData, 0,
                    test.getSignatureLength() + 1);

            result &= Offsets.checkFailure(test, signData, 0, 0);

            result &= Offsets.checkFailure(test, signData, 0, -1);

            result &= Offsets.checkFailure(test, signData,
                    2147483646, test.getSignatureLength());

            result &= Offsets.checkFailure(test, null, 0,
                    test.getSignatureLength());
        } catch (NoSuchProviderException nspe) {
            System.out.println("No such provider: " + nspe);
        }

        if (!result) {
            throw new RuntimeException("Some test cases failed");
        }
    }

    static boolean checkFailure(Offsets test, byte[] signData, int offset,
            int length) {
        boolean success;
        try {
            success = test.verifySignature(signData, offset, length, 0,
                    test.getDataSize());
        } catch (IllegalArgumentException | SignatureException e) {
            System.out.println("Expected exception: " + e);
            success = false;
        } catch (InvalidKeyException e) {
            System.out.println("Unexpected exception: " + e);
            return false;
        }

        if (!success) {
            System.out.println("Signature verification failed as expected, "
                    + "with signature offset " + offset + " and length "
                    + length);
            return true;
        } else {
            System.out.println("Signature verification should not succeed, "
                    + "with signature offset " + offset + " and length "
                    + length);
            return false;
        }
    }

}
