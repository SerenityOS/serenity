/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.security.AlgorithmParameters;
import java.security.Provider;
import java.security.Security;
import java.util.HexFormat;
import javax.crypto.SecretKey;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;
import jdk.test.lib.Convert;

/*
 * @test
 * @bug 8048596
 * @summary Check if AEAD operations work correctly when buffers used
 *          for storing plain text and cipher text are overlapped or the same
 * @library /test/lib
 */
public class SameBuffer {

    private static final String PROVIDER = "SunJCE";
    private static final String AES = "AES";
    private static final String GCM = "GCM";
    private static final String PADDING = "NoPadding";
    private static final int OFFSET = 2;
    private static final int OFFSETS = 4;
    private static final int KEY_LENGTHS[] = { 128, 192, 256 };
    private static final int TEXT_LENGTHS[] = { 0, 1024 };
    private static final int AAD_LENGTHS[] = { 0, 1024 };

    private final Provider provider;
    private final SecretKey key;
    private final String transformation;
    private final int textLength;
    private final int AADLength;

    /**
     * Constructor of the test
     *
     * @param provider security provider
     * @param keyStrength key length
     * @param textLength length of data
     * @param AADLength AAD length
     */
    public SameBuffer(Provider provider, String algorithm, String mode,
            String padding, int keyStrength, int textLength, int AADLength)
            throws Exception {

        // init a secret key
        KeyGenerator kg = KeyGenerator.getInstance(algorithm, provider);
        kg.init(keyStrength);
        key = kg.generateKey();

        this.transformation = algorithm + "/" + mode + "/" + padding;
        this.provider = provider;
        this.textLength = textLength;
        this.AADLength = AADLength;
    }

    public static void main(String[] args) throws Exception {
        Provider p = Security.getProvider(PROVIDER);
        for (int keyLength : KEY_LENGTHS) {
            for (int textLength : TEXT_LENGTHS) {
                for (int AADLength : AAD_LENGTHS) {
                    for (int i = 0; i < OFFSETS; i++) {
                        // try different offsets
                        int offset = i * OFFSET;
                        runTest(p, AES, GCM, PADDING, keyLength, textLength,
                                AADLength, offset);
                    }
                }
            }
        }
    }

    /*
     * Run single test case with given parameters
     */
    static void runTest(Provider p, String algo, String mode,
            String padding, int keyLength, int textLength, int AADLength,
            int offset) throws Exception {
        System.out.println("Testing " + keyLength + " key length; "
                + textLength + " text length; " + AADLength + " AAD length; "
                + offset + " offset");
        if (keyLength > Cipher.getMaxAllowedKeyLength(algo)) {
            // skip this if this key length is larger than what's
            // configured in the jce jurisdiction policy files
            return;
        }
        SameBuffer test = new SameBuffer(p, algo, mode,
                padding, keyLength, textLength, AADLength);

        /*
         * There are four test cases:
         *   1. AAD and text are placed in separated byte arrays
         *   2. AAD and text are placed in the same byte array
         *   3. AAD and text are placed in separated byte buffers
         *   4. AAD and text are placed in the same byte buffer
         */
        Cipher ci = test.createCipher(Cipher.ENCRYPT_MODE, null);
        AlgorithmParameters params = ci.getParameters();
        test.doTestWithSeparateArrays(offset, params);
        test.doTestWithSameArrays(offset, params);
        test.doTestWithSeparatedBuffer(offset, params);
        test.doTestWithSameBuffer(offset, params);
    }

    /*
     * Run the test in case when AAD and text are placed in separated byte
     * arrays.
     */
    private void doTestWithSeparateArrays(int offset,
            AlgorithmParameters params) throws Exception {
        // prepare buffers to test
        Cipher c = createCipher(Cipher.ENCRYPT_MODE, params);
        int outputLength = c.getOutputSize(textLength);
        int outputBufSize = outputLength + offset * 2;

        byte[] inputText = Helper.generateBytes(outputBufSize);
        byte[] AAD = Helper.generateBytes(AADLength);

        // do the test
        runGCMWithSeparateArray(Cipher.ENCRYPT_MODE, AAD, inputText, offset * 2,
                textLength, offset, params);
        int tagLength = c.getParameters()
                .getParameterSpec(GCMParameterSpec.class).getTLen() / 8;
        runGCMWithSeparateArray(Cipher.DECRYPT_MODE, AAD, inputText, offset,
                textLength + tagLength, offset, params);
    }

    /**
     * Run the test in case when AAD and text are placed in the same byte
     * array.
     */
    private void doTestWithSameArrays(int offset, AlgorithmParameters params)
            throws Exception {
        // prepare buffers to test
        Cipher c = createCipher(Cipher.ENCRYPT_MODE, params);
        int outputLength = c.getOutputSize(textLength);
        int outputBufSize = AADLength + outputLength + offset * 2;

        byte[] AAD_and_text = Helper.generateBytes(outputBufSize);

        // do the test
        runGCMWithSameArray(Cipher.ENCRYPT_MODE, AAD_and_text, AADLength + offset,
                textLength, params);
        int tagLength = c.getParameters()
                .getParameterSpec(GCMParameterSpec.class).getTLen() / 8;
        runGCMWithSameArray(Cipher.DECRYPT_MODE, AAD_and_text, AADLength + offset,
                textLength + tagLength, params);
    }

    /*
     * Run the test in case when AAD and text are placed in separated ByteBuffer
     */
    private void doTestWithSeparatedBuffer(int offset,
            AlgorithmParameters params) throws Exception {
        // prepare AAD byte buffers to test
        byte[] AAD = Helper.generateBytes(AADLength);
        ByteBuffer AAD_Buf = ByteBuffer.allocate(AADLength);
        AAD_Buf.put(AAD, 0, AAD.length);
        AAD_Buf.flip();

        // prepare text byte buffer to encrypt/decrypt
        Cipher c = createCipher(Cipher.ENCRYPT_MODE, params);
        int outputLength = c.getOutputSize(textLength);
        int outputBufSize = outputLength + offset;
        byte[] inputText = Helper.generateBytes(outputBufSize);
        ByteBuffer plainTextBB = ByteBuffer.allocateDirect(inputText.length);
        plainTextBB.put(inputText);
        plainTextBB.position(offset);
        plainTextBB.limit(offset + textLength);

        // do test
        runGCMWithSeparateBuffers(Cipher.ENCRYPT_MODE, AAD_Buf, plainTextBB, offset,
                textLength, params);
        int tagLength = c.getParameters()
                .getParameterSpec(GCMParameterSpec.class).getTLen() / 8;
        plainTextBB.position(offset);
        plainTextBB.limit(offset + textLength + tagLength);
        runGCMWithSeparateBuffers(Cipher.DECRYPT_MODE, AAD_Buf, plainTextBB, offset,
                textLength + tagLength, params);
    }

    /*
     * Run the test in case when AAD and text are placed in the same ByteBuffer
     */
    private void doTestWithSameBuffer(int offset, AlgorithmParameters params)
            throws Exception {
        // calculate output length
        Cipher c = createCipher(Cipher.ENCRYPT_MODE, params);
        int outputLength = c.getOutputSize(textLength);

        // prepare byte buffer contained AAD and plain text
        int bufSize = AADLength + offset + outputLength;
        byte[] AAD_and_Text = Helper.generateBytes(bufSize);
        ByteBuffer AAD_and_Text_Buf = ByteBuffer.allocate(bufSize);
        AAD_and_Text_Buf.put(AAD_and_Text, 0, AAD_and_Text.length);

        // do test
        runGCMWithSameBuffer(Cipher.ENCRYPT_MODE, AAD_and_Text_Buf, offset,
                textLength, params);
        int tagLength = c.getParameters()
                .getParameterSpec(GCMParameterSpec.class).getTLen() / 8;
        AAD_and_Text_Buf.limit(AADLength + offset + textLength + tagLength);
        runGCMWithSameBuffer(Cipher.DECRYPT_MODE, AAD_and_Text_Buf, offset,
                textLength + tagLength, params);

    }

    /*
     * Execute GCM encryption/decryption of a text placed in a byte array.
     * AAD is placed in the separated byte array.
     * Data are processed twice:
     *   - in a separately allocated buffer
     *   - in the text buffer
     * Check if two results are equal
     */
    private void runGCMWithSeparateArray(int mode, byte[] AAD, byte[] text,
            int txtOffset, int length, int offset, AlgorithmParameters params)
            throws Exception {
        // first, generate the cipher text at an allocated buffer
        Cipher cipher = createCipher(mode, params);
        cipher.updateAAD(AAD);
        byte[] outputText = cipher.doFinal(text, txtOffset, length);

        // new cipher for encrypt operation
        Cipher anotherCipher = createCipher(mode, params);
        anotherCipher.updateAAD(AAD);

        // next, generate cipher text again at the same buffer of plain text
        int myoff = offset;
        int off = anotherCipher.update(text, txtOffset, length, text, myoff);
        anotherCipher.doFinal(text, myoff + off);

        // check if two resutls are equal
        if (!isEqual(text, myoff, outputText, 0, outputText.length)) {
            System.err.println(
                "\noutputText:   len = " + outputText.length + "  txtOffset = " + txtOffset + "\n" +
                HexFormat.of().withUpperCase().formatHex(outputText) + "\n" +
                "text:  len = " + text.length + "  myoff = " + myoff + "\n" +
                HexFormat.of().withUpperCase().formatHex(text) + "\n" +
                "length " + length);
            System.err.println("tlen = " + params.getParameterSpec(GCMParameterSpec.class).getTLen() / 8);
            throw new RuntimeException("Two results not equal, mode:" + mode);
        }
    }

    /*
     * Execute GCM encrption/decryption of a text. The AAD and text to process
     * are placed in the same byte array. Data are processed twice:
     *   - in a separetly allocated buffer
     *   - in a buffer that shares content of the AAD_and_Text_BA
     * Check if two results are equal
     */
    private void runGCMWithSameArray(int mode, byte[] array, int txtOffset,
            int length, AlgorithmParameters params) throws Exception {
        // first, generate cipher text at an allocated buffer
        Cipher cipher = createCipher(mode, params);
        cipher.updateAAD(array, 0, AADLength);
        byte[] outputText = cipher.doFinal(array, txtOffset, length);

        // new cipher for encrypt operation
        Cipher anotherCipher = createCipher(mode, params);
        anotherCipher.updateAAD(array, 0, AADLength);

        // next, generate cipher text again at the same buffer of plain text
        int off = anotherCipher.update(array, txtOffset, length,
                array, txtOffset);
        anotherCipher.doFinal(array, txtOffset + off);

        // check if two results are equal or not
        if (!isEqual(array, txtOffset, outputText, 0,
                outputText.length)) {
            throw new RuntimeException(
                    "Two results are not equal, mode:" + mode);
        }
    }

    /*
     * Execute GCM encryption/decryption of textBB. AAD and text to process are
     * placed in different byte buffers. Data are processed twice:
     *  - in a separately allocated buffer
     *  - in a buffer that shares content of the textBB
     * Check if results are equal
     */
    private void runGCMWithSeparateBuffers(int mode, ByteBuffer buffer,
            ByteBuffer textBB, int txtOffset, int dataLength,
            AlgorithmParameters params) throws Exception {
        // take offset into account
        textBB.position(txtOffset);
        textBB.mark();

        // first, generate the cipher text at an allocated buffer
        Cipher cipher = createCipher(mode, params);
        cipher.updateAAD(buffer);
        buffer.flip();
        ByteBuffer outBB = ByteBuffer.allocateDirect(
                cipher.getOutputSize(dataLength));

        cipher.doFinal(textBB, outBB);// get cipher text in outBB
        outBB.flip();

        // restore positions
        textBB.reset();

        // next, generate cipher text again in a buffer that shares content
        Cipher anotherCipher = createCipher(mode, params);
        anotherCipher.updateAAD(buffer);
        buffer.flip();
        ByteBuffer buf2 = textBB.duplicate(); // buf2 shares textBuf context
        buf2.limit(txtOffset + anotherCipher.getOutputSize(dataLength));
        int dataProcessed2 = anotherCipher.doFinal(textBB, buf2);
        buf2.position(txtOffset);
        buf2.limit(txtOffset + dataProcessed2);

        if (!buf2.equals(outBB)) {
            throw new RuntimeException(
                    "Two results are not equal, mode:" + mode);
        }
    }

    /*
     * Execute GCM encryption/decryption of text. AAD and a text to process are
     * placed in the same buffer. Data is processed twice:
     *   - in a separately allocated buffer
     *   - in a buffer that shares content of the AAD_and_Text_BB
     */
    private void runGCMWithSameBuffer(int mode, ByteBuffer buffer,
            int txtOffset, int length, AlgorithmParameters params)
            throws Exception {

        // allocate a separate buffer
        Cipher cipher = createCipher(mode, params);
        ByteBuffer outBB = ByteBuffer.allocateDirect(
                cipher.getOutputSize(length));

        // first, generate the cipher text at an allocated buffer
        buffer.flip();
        buffer.limit(AADLength);
        cipher.updateAAD(buffer);
        buffer.limit(AADLength + txtOffset + length);
        buffer.position(AADLength + txtOffset);
        cipher.doFinal(buffer, outBB);
        outBB.flip(); // cipher text in outBB

        // next, generate cipherText again in the same buffer
        Cipher anotherCipher = createCipher(mode, params);
        buffer.flip();
        buffer.limit(AADLength);
        anotherCipher.updateAAD(buffer);
        buffer.limit(AADLength + txtOffset + length);
        buffer.position(AADLength + txtOffset);

        // share textBuf context
        ByteBuffer buf2 = buffer.duplicate();
        buf2.limit(AADLength + txtOffset + anotherCipher.getOutputSize(length));
        int dataProcessed2 = anotherCipher.doFinal(buffer, buf2);
        buf2.position(AADLength + txtOffset);
        buf2.limit(AADLength + txtOffset + dataProcessed2);

        if (!buf2.equals(outBB)) {
            throw new RuntimeException(
                    "Two results are not equal, mode:" + mode);
        }
    }

    private boolean isEqual(byte[] A, int offsetA, byte[] B, int offsetB,
            int bytesToCompare) {
        System.out.println("offsetA: " + offsetA + " offsetB: " + offsetA
                + " bytesToCompare: " + bytesToCompare);
        for (int i = 0; i < bytesToCompare; i++) {
            int setA = i + offsetA;
            int setB = i + offsetB;
            if (setA > A.length - 1 || setB > B.length - 1
                    || A[setA] != B[setB]) {
                System.err.println("i = " + i + "   A[setA] = " + A[setA] +
                    "   B[setB] = " + B[setB]);
                return false;
            }
        }

        return true;
    }

    /*
     * Creates a Cipher object for testing: for encryption it creates new Cipher
     * based on previously saved parameters (it is prohibited to use the same
     * Cipher twice for encription during GCM mode), or returns initiated
     * existing Cipher.
     */
    private Cipher createCipher(int mode, AlgorithmParameters params)
            throws Exception {
        Cipher cipher = Cipher.getInstance(transformation, provider);
        if (Cipher.ENCRYPT_MODE == mode) {
            // initiate it with the saved parameters
            if (params != null) {
                cipher.init(Cipher.ENCRYPT_MODE, key, params);
            } else {
                // intiate the cipher and save parameters
                cipher.init(Cipher.ENCRYPT_MODE, key);
            }
        } else if (cipher != null) {
            cipher.init(Cipher.DECRYPT_MODE, key, params);
        } else {
            throw new RuntimeException("Can't create cipher");
        }

        return cipher;
    }

}
