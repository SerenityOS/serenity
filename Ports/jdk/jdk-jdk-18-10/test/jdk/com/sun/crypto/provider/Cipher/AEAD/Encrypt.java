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

import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;

/*
 * @test
 * @bug 8048596
 * @summary AEAD encryption/decryption test
 */

/*
 * The test does the following:
 *   - create an input text and additional data
 *   - generate a secret key
 *   - instantiate a cipher according to the GCM transformation
 *   - generate an outputText using a single-part encryption/decryption
 *     in AEAD mode
 *   - perform 16 different combinations of multiple-part encryption/decryption
 *     operation in AEAD mode (in encryption mode new Cipher object is created
 *     and initialized with the same secret key and parameters)
 *   - check that all 17 results are equal
 *
 * Combinations:
 *
 * combination #1
 *   updateAAD(byte[] src)
 *   update(byte[], int, int)
 *   doFinal(byte[], int, int)
 *
 * combination #2
 *   updateAAD(byte[] src)
 *   update(byte[], int, int)
 *   doFinal(byte[], int, int, byte[], int)
 *
 * combination #3
 *   updateAAD(byte[] src)
 *   update(byte[], int, int, byte[], int)
 *   doFinal(byte[], int, int)
 *
 * combination #4
 *   updateAAD(byte[] src)
 *   update(byte[], int, int, byte[], int)
 *   doFinal(byte[], int, int, byte[], int)
 *
 * combination #5 - #8 are similar to #1 -#4,
 * but with updateAAD(byte[] src, int offset, int len)
 *
 * combination #9 - #12 are similar to #1 - #4,
 * but with updateAAD(ByteBuffer src)
 *
 * combination #13 - #16 are similar to #9 - #12 but with directly allocated
 * ByteBuffer and update(ByteBuffer input, ByteBuffer output)
 *
 */
public class Encrypt {

    private static final String ALGORITHMS[] = { "AES" };
    private static final int KEY_STRENGTHS[] = { 128, 192, 256 };
    private static final int TEXT_LENGTHS[] = { 0, 256, 1024 };
    private static final int AAD_LENGTHS[] = { 0, 8, 128, 256, 1024 };
    private static final int ARRAY_OFFSET = 8;

    private final String transformation;
    private final Provider provider;
    private final SecretKey key;
    private final int textLength;
    private final int AADLength;

    /**
     * @param provider Security provider
     * @param algorithm Security algorithm to test
     * @param mode The mode (GCM is only expected)
     * @param padding Algorithm padding
     * @param keyStrength key length
     * @param textLength Plain text length
     * @param AADLength Additional data length
     */
    public Encrypt(Provider provider, String algorithm, String mode,
            String padding, int keyStrength, int textLength, int AADLength)
            throws Exception {

        // init a secret Key
        KeyGenerator kg = KeyGenerator.getInstance(algorithm, provider);
        kg.init(keyStrength);
        key = kg.generateKey();

        this.provider = provider;
        this.transformation = algorithm + "/" + mode + "/" + padding;
        this.textLength = textLength;
        this.AADLength = AADLength;
    }

    public static void main(String[] args) throws Exception {
        Provider p = Security.getProvider("SunJCE");
        for (String alg : ALGORITHMS) {
            for (int keyStrength : KEY_STRENGTHS) {
                if (keyStrength > Cipher.getMaxAllowedKeyLength(alg)) {
                    // skip this if this key length is larger than what's
                    // configured in the JCE jurisdiction policy files
                    continue;
                }
                for (int textLength : TEXT_LENGTHS) {
                    for (int AADLength : AAD_LENGTHS) {
                        Encrypt test = new Encrypt(p, alg,
                                "GCM", "NoPadding", keyStrength, textLength,
                                AADLength);
                        Cipher cipher = test.createCipher(Cipher.ENCRYPT_MODE,
                                null);
                        AlgorithmParameters params = cipher.getParameters();
                        test.doTest(params);
                        System.out.println("Test " + alg + ":"
                                + keyStrength + ":" + textLength + ":"
                                + AADLength + " passed");
                    }
                }
            }
        }
    }

    public void doTest(AlgorithmParameters params) throws Exception {
        System.out.println("Test transformation = " + transformation
                + ", textLength = " + textLength
                + ", AADLength = " + AADLength);
        byte[] input = Helper.generateBytes(textLength);
        byte[] AAD = Helper.generateBytes(AADLength);
        byte[] result = execute(Cipher.ENCRYPT_MODE, AAD, input, params);
        result = execute(Cipher.DECRYPT_MODE, AAD, result, params);
        if (!Arrays.equals(input, result)) {
            throw new RuntimeException("Test failed");
        }
        System.out.println("Test passed");
    }

    /**
     * Create a Cipher object for the requested encryption/decryption mode.
     *
     * @param mode encryption or decryption mode
     * @return Cipher object initiated to perform requested mode operation
     */
    private Cipher createCipher(int mode, AlgorithmParameters params)
            throws Exception {
        Cipher ci;
        if (Cipher.ENCRYPT_MODE == mode) {
            // create a new Cipher object for encryption
            ci = Cipher.getInstance(transformation, provider);

            // initiate it with the saved parameters
            if (params != null) {
                ci.init(Cipher.ENCRYPT_MODE, key, params);
            } else {
                // initiate the cipher without parameters
                ci.init(Cipher.ENCRYPT_MODE, key);
            }
        } else {
            // it is expected that parameters already generated
            // before decryption
            ci = Cipher.getInstance(transformation, provider);
            ci.init(Cipher.DECRYPT_MODE, key, params);
        }

        return ci;
    }

    /**
     * Test AEAD combinations
     *
     * @param mode decryption or encryption
     * @param AAD additional data for AEAD operations
     * @param inputText plain text to decrypt/encrypt
     * @return output text after encrypt/decrypt
     */
    public byte[] execute(int mode, byte[] AAD, byte[] inputText,
            AlgorithmParameters params) throws Exception {

        Cipher cipher = createCipher(mode, params);

        // results of each combination will be saved in the outputTexts
        List<byte[]> outputTexts = new ArrayList<>();

        // generate a standard outputText using a single-part en/de-cryption
        cipher.updateAAD(AAD);
        byte[] output = cipher.doFinal(inputText);

        // execute multiple-part encryption/decryption combinations
        combination_1(outputTexts, mode, AAD, inputText, params);
        combination_2(outputTexts, mode, AAD, inputText, params);
        combination_3(outputTexts, mode, AAD, inputText, params);
        combination_4(outputTexts, mode, AAD, inputText, params);
        combination_5(outputTexts, mode, AAD, inputText, params);
        combination_6(outputTexts, mode, AAD, inputText, params);
        combination_7(outputTexts, mode, AAD, inputText, params);
        combination_8(outputTexts, mode, AAD, inputText, params);
        combination_9(outputTexts, mode, AAD, inputText, params);
        combination_10(outputTexts, mode, AAD, inputText, params);
        combination_11(outputTexts, mode, AAD, inputText, params);
        combination_12(outputTexts, mode, AAD, inputText, params);
        combination_13(outputTexts, mode, AAD, inputText, params);
        combination_14(outputTexts, mode, AAD, inputText, params);
        combination_15(outputTexts, mode, AAD, inputText, params);
        combination_16(outputTexts, mode, AAD, inputText, params);

        for (int k = 0; k < outputTexts.size(); k++) {
            HexFormat hex = HexFormat.of().withUpperCase();
            if (!Arrays.equals(output, outputTexts.get(k))) {
                System.out.println("Combination #" + (k + 1) + "\nresult    " +
                    hex.formatHex(outputTexts.get(k)) +
                    "\nexpected: " + hex.formatHex(output));
                throw new RuntimeException("Combination #" + (k + 1) + " failed");
            }
        }
        return output;
    }

    /*
     * Execute multiple-part encryption/decryption combination #1:
     *   updateAAD(byte[] src)
     *   update(byte[], int, int)
     *   doFinal(byte[], int, int)
     */
    private void combination_1(List<byte[]> results, int mode, byte[] AAD,
            byte[] plainText, AlgorithmParameters params) throws Exception {
        Cipher c = createCipher(mode, params);
        c.updateAAD(AAD);
        byte[] part11 = c.update(plainText, 0, plainText.length);
        int part11_length = part11 == null ? 0 : part11.length;
        byte[] part12 = c.doFinal();
        byte[] outputText1 = new byte[part11_length + part12.length];
        if (part11 != null) {
            System.arraycopy(part11, 0, outputText1, 0, part11_length);
        }
        System.arraycopy(part12, 0, outputText1, part11_length, part12.length);
        results.add(outputText1);
    }

    /*
     * Execute multiple-part encryption/decryption combination #2:
     *   updateAAD(byte[] src)
     *   update(byte[], int, int)
     *   doFinal(byte[], int, int, byte[], int)
     */
    private void combination_2(List<byte[]> results, int mode, byte[] AAD,
            byte[] plainText, AlgorithmParameters params) throws Exception {
        Cipher c = createCipher(mode, params);
        c.updateAAD(AAD);
        int t = 0;
        int offset = 0;
        if (plainText.length > ARRAY_OFFSET) {
            t = plainText.length - ARRAY_OFFSET;
            offset = ARRAY_OFFSET;
        }
        byte[] part21 = c.update(plainText, 0, t);
        byte[] part22 = new byte[c.getOutputSize(plainText.length)];
        int len2 = c.doFinal(plainText, t, offset, part22, 0);
        int part21Length = part21 != null ? part21.length : 0;
        byte[] outputText2 = new byte[part21Length + len2];
        if (part21 != null) {
            System.arraycopy(part21, 0, outputText2, 0, part21Length);
        }
        System.arraycopy(part22, 0, outputText2, part21Length, len2);
        results.add(outputText2);
    }

    /*
     * Execute multiple-part encryption/decryption combination #3
     *   updateAAD(byte[] src)
     *   update(byte[], int, int, byte[], int)
     *   doFinal(byte[], int, int)
     */
    private void combination_3(List<byte[]> results, int mode, byte[] AAD,
            byte[] plainText, AlgorithmParameters params) throws Exception {
        Cipher ci = createCipher(mode, params);
        ci.updateAAD(AAD);
        byte[] part31 = new byte[ci.getOutputSize(plainText.length)];
        int offset = plainText.length > ARRAY_OFFSET ? ARRAY_OFFSET : 0;
        int len = ci.update(plainText, 0, plainText.length - offset, part31, 0);
        byte[] part32 = ci.doFinal(plainText, plainText.length - offset,
                offset);
        byte[] outputText3 = new byte[len + part32.length];
        System.arraycopy(part31, 0, outputText3, 0, len);
        System.arraycopy(part32, 0, outputText3, len, part32.length);
        results.add(outputText3);
    }

    /*
     * Execute multiple-part encryption/decryption combination #4:
     *   updateAAD(byte[] src)
     *   update(byte[], int, int, byte[], int)
     *   doFinal(byte[], int, int, byte[], int)
     */
    private void combination_4(List<byte[]> results, int mode, byte[] AAD,
            byte[] plainText, AlgorithmParameters params) throws Exception {
        Cipher ci = createCipher(mode, params);
        ci.updateAAD(AAD);
        byte[] part41 = new byte[ci.getOutputSize(plainText.length)];
        int offset = plainText.length > ARRAY_OFFSET ? ARRAY_OFFSET : 0;
        int len = ci.update(plainText, 0, plainText.length - offset, part41, 0);
        int rest4 = ci.doFinal(plainText, plainText.length - offset, offset,
                part41, len);
        byte[] outputText4 = new byte[len + rest4];
        System.arraycopy(part41, 0, outputText4, 0, outputText4.length);
        results.add(outputText4);
    }

    /*
     * Execute multiple-part encryption/decryption combination #5:
     *   updateAAD(byte[] src, int offset, int len)
     *   update(byte[], int, int)
     *   doFinal(byte[], int, int)
     */
    private void combination_5(List<byte[]> results, int mode, byte[] AAD,
            byte[] plainText, AlgorithmParameters params) throws Exception {
        Cipher c = createCipher(mode, params);
        c.updateAAD(AAD, 0, AAD.length);
        byte[] part51 = c.update(plainText, 0, plainText.length);
        byte[] part52 = c.doFinal();
        int part51Length = part51 != null ? part51.length : 0;
        byte[] outputText5 = new byte[part51Length + part52.length];
        if (part51 != null) {
            System.arraycopy(part51, 0, outputText5, 0, part51Length);
        }
        System.arraycopy(part52, 0, outputText5, part51Length, part52.length);
        results.add(outputText5);
    }

    /*
     * Execute multiple-part encryption/decryption combination #6:
     *   updateAAD(byte[] src, int offset, int len)
     *   updateAAD(byte[] src, int offset, int len)
     *   update(byte[], int, int) doFinal(byte[], int, int, byte[], int)
     */
    private void combination_6(List<byte[]> results, int mode, byte[] AAD,
            byte[] plainText, AlgorithmParameters params) throws Exception {
        Cipher c = createCipher(mode, params);
        c.updateAAD(AAD, 0, AAD.length / 2);
        c.updateAAD(AAD, AAD.length / 2, AAD.length - AAD.length / 2);
        int t = 0;
        int offset = 0;
        if (plainText.length > ARRAY_OFFSET) {
            t = plainText.length - ARRAY_OFFSET;
            offset = ARRAY_OFFSET;
        }
        byte[] part61 = c.update(plainText, 0, t);
        byte[] part62 = new byte[c.getOutputSize(plainText.length)];
        int len = c.doFinal(plainText, t, offset, part62, 0);
        int part61Length = part61 != null ? part61.length : 0;
        byte[] outputText6 = new byte[part61Length + len];
        if (part61 != null) {
            System.arraycopy(part61, 0, outputText6, 0, part61Length);
        }
        System.arraycopy(part62, 0, outputText6, part61Length, len);
        results.add(outputText6);
    }

    /*
     * Execute multiple-part encryption/decryption combination #7
     *   updateAAD(byte[] src, int offset, int len)
     *   updateAAD(byte[] src, src.length, 0)
     *   update(byte[], int, int, byte[], int) doFinal(byte[],int, int)
     */
    private void combination_7(List<byte[]> results, int mode, byte[] AAD,
            byte[] plainText, AlgorithmParameters params) throws Exception {
        Cipher ci = createCipher(mode, params);
        ci.updateAAD(AAD, 0, AAD.length);
        ci.updateAAD(AAD, AAD.length, 0);
        byte[] part71 = new byte[ci.getOutputSize(plainText.length)];
        int offset = plainText.length > ARRAY_OFFSET ? ARRAY_OFFSET : 0;
        int len = ci.update(plainText, 0, plainText.length - offset, part71, 0);
        byte[] part72 = ci.doFinal(plainText, plainText.length - offset, offset);
        byte[] outputText7 = new byte[len + part72.length];
        System.arraycopy(part71, 0, outputText7, 0, len);
        System.arraycopy(part72, 0, outputText7, len, part72.length);
        results.add(outputText7);
    }

    /*
     * Execute multiple-part encryption/decryption combination #8:
     *   updateAAD(byte[] src, 0, 0)
     *   updateAAD(byte[] src, 0, src.length)
     *   update(byte[], int, int, byte[], int)
     *   doFinal(byte[], int, int, byte[], int)
     */
    private void combination_8(List<byte[]> results, int mode, byte[] AAD,
            byte[] plainText, AlgorithmParameters params) throws Exception {
        Cipher ci = createCipher(mode, params);
        ci.updateAAD(AAD, 0, 0);
        ci.updateAAD(AAD, 0, AAD.length);
        byte[] part81 = new byte[ci.getOutputSize(plainText.length)];
        int offset = plainText.length > ARRAY_OFFSET ? ARRAY_OFFSET : 0;
        int len = ci.update(plainText, 0, plainText.length - offset, part81, 0);
        int rest = ci.doFinal(plainText, plainText.length - offset, offset,
                part81, len);
        byte[] outputText8 = new byte[len + rest];
        System.arraycopy(part81, 0, outputText8, 0, outputText8.length);
        results.add(outputText8);
    }

    /*
     * Execute multiple-part encryption/decryption combination #9:
     *   updateAAD(ByteBuffer src)
     *   update(byte[], int, int) doFinal(byte[], int, int)
     */
    private void combination_9(List<byte[]> results, int mode, byte[] AAD,
            byte[] plainText, AlgorithmParameters params) throws Exception {

        // prepare ByteBuffer to test
        ByteBuffer buf = ByteBuffer.allocate(AAD.length);
        buf.put(AAD);
        buf.position(0);
        buf.limit(AAD.length);

        // Get Cipher object and do the combination
        Cipher c = createCipher(mode, params);
        c.updateAAD(buf);
        byte[] part91 = c.update(plainText, 0, plainText.length);
        int part91_length = part91 == null ? 0 : part91.length;
        byte[] part92 = c.doFinal();
        byte[] outputText9 = new byte[part91_length + part92.length];

        // form result of the combination
        if (part91 != null) {
            System.arraycopy(part91, 0, outputText9, 0, part91_length);
        }
        System.arraycopy(part92, 0, outputText9, part91_length, part92.length);
        results.add(outputText9);
    }

    /*
     * Execute multiple-part encryption/decryption combination #10:
     *   updateAAD(ByteBuffer src)
     *   updateAAD(ByteBuffer src) update(byte[], int, int)
     *   doFinal(byte[], int, int, byte[], int)
     */
    private void combination_10(List<byte[]> results, int mode, byte[] AAD,
            byte[] plainText, AlgorithmParameters params) throws Exception {

        // prepare ByteBuffer to test
        ByteBuffer buf = ByteBuffer.allocate(AAD.length);
        buf.put(AAD);
        buf.position(0);
        buf.limit(AAD.length / 2);

        // get a Cipher object and do the combination
        Cipher c = createCipher(mode, params);

        // process the first half of AAD data
        c.updateAAD(buf);

        // process the rest of AAD data
        buf.limit(AAD.length);
        c.updateAAD(buf);

        // prapare variables for the combination
        int t = 0;
        int offset = 0;
        if (plainText.length > ARRAY_OFFSET) {
            t = plainText.length - ARRAY_OFFSET;
            offset = ARRAY_OFFSET;
        }

        // encrypt the text
        byte[] part10_1 = c.update(plainText, 0, t);
        int part10_1_Length = part10_1 != null ? part10_1.length : 0;
        byte[] part10_2 = new byte[c.getOutputSize(plainText.length)];
        int len2 = c.doFinal(plainText, t, offset, part10_2, 0);

        // form the combination's result
        byte[] outputText10 = new byte[part10_1_Length + len2];
        if (part10_1 != null) {
            System.arraycopy(part10_1, 0, outputText10, 0, part10_1_Length);
        }
        System.arraycopy(part10_2, 0, outputText10, part10_1_Length, len2);
        results.add(outputText10);
    }

    /*
     * Execute multiple-part encryption/decryption combination #11
     *   updateAAD(ByteBuffer src1)
     *   updateAAD(ByteBuffer src2)
     *   update(byte[],int, int, byte[], int)
     *   doFinal(byte[], int, int)
     */
    private void combination_11(List<byte[]> results, int mode, byte[] AAD,
            byte[] plainText, AlgorithmParameters params) throws Exception {

        // prepare ByteBuffer1 to test
        ByteBuffer buf1 = ByteBuffer.allocate(AAD.length / 2);
        buf1.put(AAD, 0, AAD.length / 2);
        buf1.position(0);
        buf1.limit(AAD.length / 2);

        // get a Cipher object and do combination
        Cipher ci = createCipher(mode, params);

        // process the first half of AAD data
        ci.updateAAD(buf1);

        // prepare ByteBuffer2 to test
        ByteBuffer buf2 = ByteBuffer.allocate(AAD.length - AAD.length / 2);
        buf2.put(AAD, AAD.length / 2, AAD.length - AAD.length / 2);
        buf2.position(0);
        buf2.limit(AAD.length - AAD.length / 2);

        // process the rest of AAD data
        ci.updateAAD(buf2);

        // encrypt plain text
        byte[] part11_1 = new byte[ci.getOutputSize(plainText.length)];
        int offset = plainText.length > ARRAY_OFFSET ? ARRAY_OFFSET : 0;
        int len_11 = ci.update(plainText, 0, plainText.length - offset,
                part11_1, 0);
        byte[] part11_2 = ci.doFinal(plainText, plainText.length - offset,
                offset);
        byte[] outputText11 = new byte[len_11 + part11_2.length];
        System.arraycopy(part11_1, 0, outputText11, 0, len_11);
        System.arraycopy(part11_2, 0, outputText11, len_11, part11_2.length);
        results.add(outputText11);
    }

    /*
     * Execute multiple-part encryption/decryption combination #12:
     *   updateAAD(ByteBuffer src)
     *   updateAAD(ByteBuffer emptyByteBuffer)
     *   update(byte[], int, int, byte[], int)
     *   doFinal(byte[], int, int, byte[], int)
     */
    private void combination_12(List<byte[]> results, int mode, byte[] AAD,
            byte[] plainText, AlgorithmParameters params) throws Exception {

        // prepare ByteBuffer to test
        ByteBuffer buf = ByteBuffer.allocate(AAD.length);
        buf.put(AAD);
        buf.position(0);
        buf.limit(AAD.length);
        Cipher ci = createCipher(mode, params);
        ci.updateAAD(buf);

        // prepare an empty ByteBuffer
        ByteBuffer emptyBuf = ByteBuffer.allocate(0);
        emptyBuf.put(new byte[0]);
        ci.updateAAD(emptyBuf);
        byte[] part12_1 = new byte[ci.getOutputSize(plainText.length)];
        int offset = plainText.length > ARRAY_OFFSET ? ARRAY_OFFSET : 0;
        int len12 = ci.update(plainText, 0, plainText.length - offset,
                part12_1, 0);
        int rest12 = ci.doFinal(plainText, plainText.length - offset, offset,
                part12_1, len12);
        byte[] outputText12 = new byte[len12 + rest12];
        System.arraycopy(part12_1, 0, outputText12, 0, outputText12.length);
        results.add(outputText12);
    }

    /*
     * Execute multiple-part encryption/decryption combination #13:
     *   updateAAD(ByteBuffer src), where src is directly allocated
     *   update(ByteBuffer input, ByteBuffer out)
     *   doFinal(ByteBuffer input, ByteBuffer out)
     */
    private void combination_13(List<byte[]> results, int mode, byte[] AAD,
            byte[] plainText, AlgorithmParameters params) throws Exception {
        Cipher c = createCipher(mode, params);

        // prepare ByteBuffer to test
        ByteBuffer buf = ByteBuffer.allocateDirect(AAD.length);
        buf.put(AAD);
        buf.position(0);
        buf.limit(AAD.length);
        c.updateAAD(buf);

        // prepare buffers to encrypt/decrypt
        ByteBuffer in = ByteBuffer.allocateDirect(plainText.length);
        in.put(plainText);
        in.position(0);
        in.limit(plainText.length);
        ByteBuffer output = ByteBuffer.allocateDirect(
                c.getOutputSize(in.limit()));
        output.position(0);
        output.limit(c.getOutputSize(in.limit()));

        // process input text
        c.update(in, output);
        c.doFinal(in, output);
        int resultSize = output.position();
        byte[] result13 = new byte[resultSize];
        output.position(0);
        output.limit(resultSize);
        output.get(result13, 0, resultSize);
        results.add(result13);
    }

    /*
     * Execute multiple-part encryption/decryption combination #14:
     *   updateAAD(ByteBuffer src) updateAAD(ByteBuffer src),
     *       where src is directly allocated
     *   update(ByteBuffer input, ByteBuffer out)
     *   doFinal(ByteBuffer input, ByteBuffer out)
     */
    private void combination_14(List<byte[]> results, int mode, byte[] AAD,
            byte[] plainText, AlgorithmParameters params) throws Exception {
        Cipher c = createCipher(mode, params);
        // prepare ByteBuffer to test
        ByteBuffer buf = ByteBuffer.allocateDirect(AAD.length);
        buf.put(AAD);

        // process the first half of AAD data
        buf.position(0);
        buf.limit(AAD.length / 2);
        c.updateAAD(buf);

        // process the rest of AAD data
        buf.limit(AAD.length);
        c.updateAAD(buf);

        // prepare buffers to encrypt/decrypt
        ByteBuffer in = ByteBuffer.allocate(plainText.length);
        in.put(plainText);
        in.position(0);
        in.limit(plainText.length);
        ByteBuffer out = ByteBuffer.allocate(c.getOutputSize(in.limit()));
        out.position(0);
        out.limit(c.getOutputSize(in.limit()));

        // process input text
        c.update(in, out);
        c.doFinal(in, out);
        int resultSize = out.position();
        byte[] result14 = new byte[resultSize];
        out.position(0);
        out.limit(resultSize);
        out.get(result14, 0, resultSize);
        results.add(result14);
    }

    /*
     * Execute multiple-part encryption/decryption combination #15
     *   updateAAD(ByteBuffer src1), where src1 is directly allocated
     *   updateAAD(ByteBuffer src2), where src2 is directly allocated
     *   doFinal(ByteBuffer input, ByteBuffer out)
     */
    private void combination_15(List<byte[]> results, int mode, byte[] AAD,
            byte[] plainText, AlgorithmParameters params) throws Exception {
        Cipher c = createCipher(mode, params);

        // prepare ByteBuffer1 to test
        ByteBuffer buf1 = ByteBuffer.allocateDirect(AAD.length / 2);
        buf1.put(AAD, 0, AAD.length / 2);
        buf1.position(0);
        buf1.limit(AAD.length / 2);

        // process the first half of AAD data
        c.updateAAD(buf1);

        // prepare ByteBuffer2 to test
        ByteBuffer buf2 = ByteBuffer.allocateDirect(
                AAD.length - AAD.length / 2);
        buf2.put(AAD, AAD.length / 2, AAD.length - AAD.length / 2);
        buf2.position(0);
        buf2.limit(AAD.length - AAD.length / 2);

        // process the rest of AAD data
        c.updateAAD(buf2);

        // prepare buffers to encrypt/decrypt
        ByteBuffer in = ByteBuffer.allocateDirect(plainText.length);
        in.put(plainText);
        in.position(0);
        in.limit(plainText.length);
        ByteBuffer output = ByteBuffer.allocateDirect(
                c.getOutputSize(in.limit()));
        output.position(0);
        output.limit(c.getOutputSize(in.limit()));

        // process input text
        c.doFinal(in, output);
        int resultSize = output.position();
        byte[] result15 = new byte[resultSize];
        output.position(0);
        output.limit(resultSize);
        output.get(result15, 0, resultSize);
        results.add(result15);
    }

    /*
     * Execute multiple-part encryption/decryption combination #16:
     *   updateAAD(ByteBuffer src)
     *   updateAAD(ByteBuffer emptyByteBuffer)
     *   update(ByteBuffer input, ByteBuffer out)
     *   doFinal(EmptyByteBuffer, ByteBuffer out)
     */
    private void combination_16(List<byte[]> results, int mode, byte[] AAD,
            byte[] plainText, AlgorithmParameters params) throws Exception {
        Cipher c = createCipher(mode, params);

        // prepare ByteBuffer to test
        ByteBuffer buf = ByteBuffer.allocateDirect(AAD.length);
        buf.put(AAD);
        buf.position(0);
        buf.limit(AAD.length);
        c.updateAAD(buf);

        // prepare empty ByteBuffer
        ByteBuffer emptyBuf = ByteBuffer.allocateDirect(0);
        emptyBuf.put(new byte[0]);
        c.updateAAD(emptyBuf);

        // prepare buffers to encrypt/decrypt
        ByteBuffer in = ByteBuffer.allocateDirect(plainText.length);
        in.put(plainText);
        in.position(0);
        in.limit(plainText.length);
        ByteBuffer output = ByteBuffer.allocateDirect(
                c.getOutputSize(in.limit()));
        output.position(0);
        output.limit(c.getOutputSize(in.limit()));

        // process input text with an empty buffer
        c.update(in, output);
        ByteBuffer emptyBuf2 = ByteBuffer.allocate(0);
        emptyBuf2.put(new byte[0]);
        c.doFinal(emptyBuf2, output);
        int resultSize = output.position();
        byte[] result16 = new byte[resultSize];
        output.position(0);
        output.limit(resultSize);
        output.get(result16, 0, resultSize);
        results.add(result16);
    }
}
