/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.io.IOException;
import java.security.GeneralSecurityException;
import javax.crypto.CipherInputStream;

/**
 * CICO PBE SKIP functional test.
 *
 * Verifies for the given PBE algorithm if the encrypt/decrypt mechanism is
 * performed correctly for CipherInputStream when skip() method is used.
 *
 * Test scenario:
 * 1. initializes plain text with random generated data with length TEXT_SIZE.
 * 2. for the given PBE algorithm instantiates encrypt and decrypt Ciphers.
 * 3. instantiates CipherInputStream 1 with the encrypt Cipher.
 * 4. instantiates CipherInputStream 2 with the CipherInputStream 1 and decrypt
 *    Cipher.
 * 5. the plain text is divided on TEXT_SIZE/BLOCK blocks. Reading from
 *    CipherInputStream 2 one block at time. The last BLOCK - SAVE bytes are
 *    skipping for each block. Therefor the plain text data go through
 *    CipherInputStream 1 (encrypting) and CipherInputStream 2 (decrypting).
 *    As a result the output should equal to the original text except DISCARD
 *    byte for each block are skipped.
 * 6. get the standard output.
 * 7. compares the expected standard output with the output of the
 *    CipherInputStream 2. If it is the same the test passed. Otherwise it
 *    failed. Any uncaught exceptions should be considered as an error.
 * The test implements 2 test cases in accordance with a buffering type:
 * 1. byte array buffering
 * 2. int buffering
 */
public class CICO_PBE_SKIP_Test extends CICO_PBE_Test {
    /**
     * Block size.
     */
    private static final int BLOCK = 50;

    /**
     * Valid reading byte size.
     */
    private static final int SAVE = 45;

    /**
     * Skip reading byte size. This should be same to BLOCK - SAVE
     */
    private static final int DISCARD = BLOCK - SAVE;

    /**
     * Number of blocks.
     */
    private static final int NUMBER_OF_BLOCKS = TEXT_SIZE / BLOCK;

    private final byte[] outputText;
    /**
     * CICO PBE Skip test constructor
     *
     * @param pbeAlgo the PBE algorithm to test.
     * @throws java.security.GeneralSecurityException
     */
    public CICO_PBE_SKIP_Test(PBEAlgorithm pbeAlgo)
            throws GeneralSecurityException {
        super(pbeAlgo);
        outputText = new byte[NUMBER_OF_BLOCKS * SAVE];
    }

    /**
     * Implements byte array buffering type test case of the CICO SKIP test.
     *
     * @param blockNum block number to read.
     */
    private void proceedSkipTestUsingByteArrayBufferingType(
            CipherInputStream ciIn2, int blockNum) throws IOException {
        int index = blockNum * SAVE;
        int len1 = ciIn2.read(outputText, index, SAVE);
        // read more until SAVE bytes
        index += len1;
        int len2 = 0;
        int totalRead = len1;
        while (len1 != SAVE && len2 != -1) {
            len2 = ciIn2.read(outputText, index, SAVE - len1);
            len1 += len2;
            index += len2;
            totalRead += len2;
        }
        if (totalRead != SAVE) {
            throw new RuntimeException("Read bytes number " + totalRead
                    + " does not equal to given number " + SAVE);
        }
    }

    /**
     * Implements int buffering type test case of the CICO SKIP test.
     *
     * @param blockNum block number to read.
     */
    private void proceedSkipTestUsingIntBufferingType(CipherInputStream ciIn2,
            int blockNum) throws IOException {
        int index = blockNum * SAVE;
        int totalRead = 0;
        for (int j = 0; j < SAVE; j++, index++) {
            int buffer0 = ciIn2.read();
            if (buffer0 != -1) {
                outputText[index] = (byte) buffer0;
                totalRead++;
            } else {
                break;
            }
        }
        if (totalRead != SAVE) {
            throw new RuntimeException("Read bytes number " + totalRead
                    + " does not equal to given number " + SAVE);
        }
    }

    /**
     * The CICO PBE SKIP test specific part of the super.doTest(). Implements
     * the scenario in accordance to the class description.
     * @throws java.io.IOException any I/O failed.
     */
    @Override
    public void proceedTest(String type) throws IOException {
        System.out.println("Test type: " + type);
        // init second input stream with decrypt Cipher
        try (CipherInputStream ciIn2 = new CipherInputStream(getCiInput(),
                getDecryptCipher())) {
            for (int i = 0; i < NUMBER_OF_BLOCKS; i++) {
                if (type.equals(CICO_PBE_Test.BYTE_ARR_BUFFER)) {
                    proceedSkipTestUsingByteArrayBufferingType(ciIn2, i);
                } else {
                    proceedSkipTestUsingIntBufferingType(ciIn2, i);
                }
                if (ciIn2.available() >= DISCARD) {
                    ciIn2.skip(DISCARD);
                } else {
                    for (int k = 0; k < DISCARD; k++) {
                        ciIn2.read();
                    }
                }
            }
        }
        if (!TestUtilities.equalsBlockPartial(plainText, outputText, BLOCK, SAVE)) {
            throw new RuntimeException("outputText not same with expectedText"
                    + " when test " + type);
        }
    }

}
