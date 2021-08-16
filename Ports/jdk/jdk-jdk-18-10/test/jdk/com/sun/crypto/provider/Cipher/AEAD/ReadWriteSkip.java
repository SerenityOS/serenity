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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/*
 * @test
 * @bug 8048596
 * @summary Test CICO AEAD read/write/skip operations
 */
public class ReadWriteSkip {

    static enum BufferType {
        BYTE_ARRAY_BUFFERING, INT_BYTE_BUFFERING
    }

    static final int KEY_LENGTHS[] = {128, 192, 256};
    static final int TXT_LENGTHS[] = {800, 0};
    static final int AAD_LENGTHS[] = {0, 100};
    static final int BLOCK = 50;
    static final int SAVE = 45;
    static final int DISCARD = BLOCK - SAVE;
    static final String PROVIDER = "SunJCE";
    static final String AES = "AES";
    static final String GCM = "GCM";
    static final String PADDING = "NoPadding";
    static final String TRANSFORM = AES + "/" + GCM + "/" + PADDING;

    final SecretKey key;
    final byte[] plaintext;
    final byte[] AAD;
    final int textLength;;
    final int keyLength;
    Cipher encryptCipher;
    Cipher decryptCipher;
    CipherInputStream ciInput;

    public static void main(String[] args) throws Exception {
        boolean success = true;
        for (int keyLength : KEY_LENGTHS) {
            if (keyLength > Cipher.getMaxAllowedKeyLength(TRANSFORM)) {
                // skip this if this key length is larger than what's
                // configured in the jce jurisdiction policy files
                continue;
            }
            for (int textLength : TXT_LENGTHS) {
                for (int AADLength : AAD_LENGTHS) {
                    System.out.println("Key length = " + keyLength
                            + ", text length = " + textLength
                            + ", AAD length = " + AADLength);
                    try {
                        run(keyLength, textLength, AADLength);
                        System.out.println("Test case passed");
                    } catch (Exception e) {
                        System.out.println("Test case failed: " + e);
                        success = false;
                    }
                }
            }
        }

        if (!success) {
            throw new RuntimeException("At least one test case failed");
        }

        System.out.println("Test passed");
    }

    ReadWriteSkip(int keyLength, int textLength, int AADLength)
            throws Exception {
        this.keyLength = keyLength;
        this.textLength = textLength;

        // init AAD
        this.AAD = Helper.generateBytes(AADLength);

        // init a secret Key
        KeyGenerator kg = KeyGenerator.getInstance(AES, PROVIDER);
        kg.init(this.keyLength);
        this.key = kg.generateKey();

        this.plaintext = Helper.generateBytes(textLength);
    }

    final void doTest(BufferType type) throws Exception {
        // init ciphers
        encryptCipher = createCipher(Cipher.ENCRYPT_MODE);
        decryptCipher = createCipher(Cipher.DECRYPT_MODE);

        // init cipher input stream
        ciInput = new CipherInputStream(new ByteArrayInputStream(plaintext),
                encryptCipher);

        runTest(type);
    }

    void runTest(BufferType type) throws Exception {}

    private Cipher createCipher(int mode) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(TRANSFORM, PROVIDER);
        if (mode == Cipher.ENCRYPT_MODE) {
            cipher.init(Cipher.ENCRYPT_MODE, key);
        } else {
            if (encryptCipher != null) {
                cipher.init(Cipher.DECRYPT_MODE, key,
                        encryptCipher.getParameters());
            } else {
                throw new RuntimeException("Can't create a cipher");
            }
        }
        cipher.updateAAD(AAD);
        return cipher;
    }

    /*
     * Run test cases
     */
    static void run(int keyLength, int textLength, int AADLength)
            throws Exception {
        new ReadWriteTest(keyLength, textLength, AADLength)
                .doTest(BufferType.BYTE_ARRAY_BUFFERING);
        new ReadWriteTest(keyLength, textLength, AADLength)
                .doTest(BufferType.INT_BYTE_BUFFERING);
        new SkipTest(keyLength, textLength, AADLength)
                .doTest(BufferType.BYTE_ARRAY_BUFFERING);
        new SkipTest(keyLength, textLength, AADLength)
                .doTest(BufferType.INT_BYTE_BUFFERING);
    }

    static void check(byte[] first, byte[] second) {
        if (!Arrays.equals(first, second)) {
            throw new RuntimeException("Arrays are not equal");
        }
    }

    /*
     * CICO AEAD read/write functional test.
     *
     * Check if encrypt/decrypt operations work correctly.
     *
     * Test scenario:
     *   - initializes plain text
     *   - for given AEAD algorithm instantiates encrypt and decrypt Ciphers
     *   - instantiates CipherInputStream with the encrypt Cipher
     *   - instantiates CipherOutputStream with the decrypt Cipher
     *   - performs reading from the CipherInputStream (encryption data)
     *     and writing to the CipherOutputStream (decryption). As a result,
     *     output of the CipherOutputStream should be equal
     *     with original plain text
     *   - check if the original plain text is equal to output
     *     of the CipherOutputStream
     *   - if it is equal the test passes, otherwise it fails
     */
    static class ReadWriteTest extends ReadWriteSkip {

        public ReadWriteTest(int keyLength, int textLength, int AADLength)
                throws Exception {
            super(keyLength, textLength, AADLength);
        }

        @Override
        public void runTest(BufferType bufType) throws IOException,
                GeneralSecurityException {

            ByteArrayOutputStream baOutput = new ByteArrayOutputStream();
            try (CipherOutputStream ciOutput = new CipherOutputStream(baOutput,
                    decryptCipher)) {
                if (bufType == BufferType.BYTE_ARRAY_BUFFERING) {
                    doByteTest(ciOutput);
                } else {
                    doIntTest(ciOutput);
                }
            }

            check(plaintext, baOutput.toByteArray());
        }

        /*
         * Implements byte array buffering type test case
         */
        public void doByteTest(CipherOutputStream out) throws IOException {
            byte[] buffer = Helper.generateBytes(textLength + 1);
            int len = ciInput.read(buffer);
            while (len != -1) {
                out.write(buffer, 0, len);
                len = ciInput.read(buffer);
            }
        }

        /*
         * Implements integer buffering type test case
         */
        public void doIntTest(CipherOutputStream out) throws IOException {
            int buffer = ciInput.read();
            while (buffer != -1) {
                out.write(buffer);
                buffer = ciInput.read();
            }
        }
    }

    /*
     * CICO AEAD SKIP functional test.
     *
     * Checks if the encrypt/decrypt operations work correctly
     * when skip() method is used.
     *
     * Test scenario:
     *   - initializes a plain text
     *   - initializes ciphers
     *   - initializes cipher streams
     *   - split plain text to TEXT_SIZE/BLOCK blocks
     *   - read from CipherInputStream2 one block at time
     *   - the last DISCARD = BLOCK - SAVE bytes are skipping for each block
     *   - therefore, plain text data go through CipherInputStream1 (encrypting)
     *     and CipherInputStream2 (decrypting)
     *   - as a result, output should equal to the original text
     *     except DISCART byte for each block
     *   - check result buffers
     */
    static class SkipTest extends ReadWriteSkip {

        private final int numberOfBlocks;
        private final byte[] outputText;

        public SkipTest(int keyLength, int textLength, int AADLength)
                throws Exception {
            super(keyLength, textLength, AADLength);
            numberOfBlocks = this.textLength / BLOCK;
            outputText = new byte[numberOfBlocks * SAVE];
        }

        private void doByteTest(int blockNum, CipherInputStream cis)
                throws IOException {
            int index = blockNum * SAVE;
            int len = cis.read(outputText, index, SAVE);
            index += len;
            int read = 0;
            while (len != SAVE && read != -1) {
                read = cis.read(outputText, index, SAVE - len);
                len += read;
                index += read;
            }
        }

        private void doIntTest(int blockNum, CipherInputStream cis)
                throws IOException {
            int i = blockNum * SAVE;
            for (int j = 0; j < SAVE && i < outputText.length; j++, i++) {
                int b = cis.read();
                if (b != -1) {
                    outputText[i] = (byte) b;
                }
            }
        }

        @Override
        public void runTest(BufferType type) throws IOException,
                NoSuchAlgorithmException {
            try (CipherInputStream cis = new CipherInputStream(ciInput,
                    decryptCipher)) {
                for (int i = 0; i < numberOfBlocks; i++) {
                    if (type == BufferType.BYTE_ARRAY_BUFFERING) {
                        doByteTest(i, cis);
                    } else {
                        doIntTest(i, cis);
                    }
                    if (cis.available() >= DISCARD) {
                        cis.skip(DISCARD);
                    } else {
                        for (int k = 0; k < DISCARD; k++) {
                            cis.read();
                        }
                    }
                }
            }

            byte[] expectedText = new byte[numberOfBlocks * SAVE];
            for (int m = 0; m < numberOfBlocks; m++) {
                for (int n = 0; n < SAVE; n++) {
                    expectedText[m * SAVE + n] = plaintext[m * BLOCK + n];
                }
            }
            check(expectedText, outputText);
        }
    }
}
