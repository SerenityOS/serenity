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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.crypto.CipherOutputStream;

/**
 * CICO PBE Read/Write functional test.
 *
 * Verifies for the given PBE algorithm if the encrypt/decrypt mechanism is
 * performed correctly for CipherInputStream and CipherOutputStream.
 *
 * Test scenario:
 * 1. initializes plain text with random generated data.
 * 2. for the given PBE algorithm instantiates encrypt and decrypt Ciphers.
 * 3. instantiates CipherInputStream with the encrypt Cipher.
 * 4. instantiates CipherOutputStream with the decrypt Cipher.
 * 5. performs reading from the CipherInputStream (encryption data) and writing
 *    to the CipherOutputStream (decryption). As a result the output of the
 *    CipherOutputStream should be the same as an original plain text.
 * 6. compares if the original plain text is the same as the output of the
 *    CipherOutputStream.
 *
 * The test implements 2 test cases in accordance with buffering type:
 * 1. byte array buffering
 * 2. int buffering
 */
public class CICO_PBE_RW_Test extends CICO_PBE_Test {

    public CICO_PBE_RW_Test(PBEAlgorithm pbeAlgo)
            throws GeneralSecurityException {
        super(pbeAlgo);
    }

    /**
     * The CICO PBE RW test specific part of the super.doTest(). Implements the
     * scenario in accordance to the class description.
     * @param type byteArrayBuffering or intByteBuffering
     * @throws IOException  any I/O operation failed.
     * @throws GeneralSecurityException any security error.
     */
    @Override
    public void proceedTest(String type) throws IOException,
            GeneralSecurityException {
        ByteArrayOutputStream baOutput = new ByteArrayOutputStream();
        try (CipherOutputStream ciOutput = new CipherOutputStream(baOutput,
                getDecryptCipher())) {
            if (type.equals(CICO_PBE_Test.BYTE_ARR_BUFFER)) {
                proceedTestUsingByteArrayBuffer(ciOutput);
            } else {
                proceedTestUsingIntBuffer(ciOutput);
            }
            ciOutput.flush();
        }
        // Compare input and output
        if (!TestUtilities.equalsBlock(plainText, baOutput.toByteArray(), TEXT_SIZE)) {
            throw new RuntimeException("outputText not same with expectedText"
                    + " when test " + type);
        }
    }

    /**
     * Implements byte array buffering type test case of the CICO PBE RW test.
     * @param ciOutput  output stream for data written.
     * @throws java.io.IOException any I/O operation failed.
     */
    public void proceedTestUsingByteArrayBuffer(
            CipherOutputStream ciOutput) throws IOException {
        byte[] buffer = new byte[TEXT_SIZE];
        int len = getCiInput().read(buffer);
        while (len != -1) {
            ciOutput.write(buffer, 0, len);
            len = getCiInput().read(buffer);
        }
    }

    /**
     * Implements int buffering type test case.
     * @param ciOutput output stream for data written.
     * @throws java.io.IOException any I/O operation failed.
     */
    public void proceedTestUsingIntBuffer(CipherOutputStream ciOutput)
            throws IOException {
        int buffer = getCiInput().read();
        while (buffer != -1) {
            ciOutput.write(buffer);
            buffer = getCiInput().read();
        }
    }
}
