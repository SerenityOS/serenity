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

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;
import javax.crypto.SecretKey;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;

/*
 * @test
 * @bug 8048596
 * @summary Check if GCMParameterSpec works as expected
 */
public class GCMParameterSpecTest {

    private static final int[] IV_LENGTHS = { 96, 8, 1024 };
    private static final int[] KEY_LENGTHS = { 128, 192, 256 };
    private static final int[] DATA_LENGTHS = { 0, 128, 1024 };
    private static final int[] AAD_LENGTHS = { 0, 128, 1024 };
    private static final int[] TAG_LENGTHS = { 128, 120, 112, 104, 96 };
    private static final int[] OFFSETS = { 0, 2, 5, 99 };
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String TEMPLATE = "Test:\n  tag = %d\n"
            + "  IV length = %d\n  data length = %d\n"
            + "  AAD length = %d\n  offset = %d\n  keylength = %d\n";

    private final byte[] IV;
    private final byte[] IVO;
    private final byte[] data;
    private final byte[] AAD;
    private final SecretKey key;
    private final int tagLength;
    private final int IVlength;
    private final int offset;

    /**
     * Initialize IV, IV with offset, plain text, AAD and SecretKey
     *
     * @param keyLength length of a secret key
     * @param tagLength tag length
     * @param IVlength IV length
     * @param offset offset in a buffer for IV
     * @param textLength plain text length
     * @param AADLength AAD length
     */
    public GCMParameterSpecTest(int keyLength, int tagLength, int IVlength,
            int offset, int textLength, int AADLength)
            throws NoSuchAlgorithmException, NoSuchProviderException {
        this.tagLength = tagLength; // save tag length
        this.IVlength = IVlength; // save IV length
        this.offset = offset; // save IV offset

        // prepare IV
        IV = Helper.generateBytes(IVlength);

        // prepare IV with offset
        IVO = new byte[this.IVlength + this.offset];
        System.arraycopy(IV, 0, IVO, offset, this.IVlength);

        // prepare data
        data = Helper.generateBytes(textLength);

        // prepare AAD
        AAD = Helper.generateBytes(AADLength);

        // init a secret key
        KeyGenerator kg = KeyGenerator.getInstance("AES", "SunJCE");
        kg.init(keyLength);
        key = kg.generateKey();
    }

    /*
     * Run the test for each key length, tag length, IV length, plain text
     * length, AAD length and offset.
     */
    public static void main(String[] args) throws Exception {
        boolean success = true;
        for (int k : KEY_LENGTHS) {
            if (k > Cipher.getMaxAllowedKeyLength(TRANSFORMATION)) {
                // skip this if this key length is larger than what's
                // allowed in the jce jurisdiction policy files
                continue;
            }
            for (int t : TAG_LENGTHS) {
                for (int n : IV_LENGTHS) {
                    for (int p : DATA_LENGTHS) {
                        for (int a : AAD_LENGTHS) {
                            for (int o : OFFSETS) {
                                System.out.printf(TEMPLATE, t, n, p, a, o, k);
                                success &= new GCMParameterSpecTest(
                                        k, t, n, o, p, a).doTest();
                            }
                        }
                    }
                }
            }
        }

        if (!success) {
            throw new RuntimeException("At least one test case failed");
        }
    }

    /*
     * Run the test:
     *   - check if result of encryption of plain text is the same
     *     when parameters constructed with different GCMParameterSpec
     *     constructors are used
     *   - check if GCMParameterSpec.getTLen() is equal to actual tag length
     *   - check if ciphertext has the same length as plaintext
     */
    private boolean doTest() throws Exception {
        GCMParameterSpec spec1 = new GCMParameterSpec(tagLength, IV);
        GCMParameterSpec spec2 = new GCMParameterSpec(tagLength, IVO, offset,
                IVlength);
        byte[] cipherText1 = getCipherTextBySpec(spec1);
        if (cipherText1 == null) {
            return false;
        }
        byte[] cipherText2 = getCipherTextBySpec(spec2);
        if (cipherText2 == null) {
            return false;
        }
        if (!Arrays.equals(cipherText1, cipherText2)) {
            System.out.println("Cipher texts are different");
            return false;
        }
        if (spec1.getTLen() != spec2.getTLen()) {
            System.out.println("Tag lengths are not equal");
            return false;
        }
        byte[] recoveredText1 = recoverCipherText(cipherText1, spec2);
        if (recoveredText1 == null) {
            return false;
        }
        byte[] recoveredText2 = recoverCipherText(cipherText2, spec1);
        if (recoveredText2 == null) {
            return false;
        }
        if (!Arrays.equals(recoveredText1, recoveredText2)) {
            System.out.println("Recovered texts are different");
            return false;
        }
        if (!Arrays.equals(recoveredText1, data)) {
            System.out.println("Recovered and original texts are not equal");
            return false;
        }

        return true;
    }

    /*
     * Encrypt a plain text, and check if GCMParameterSpec.getIV()
     * is equal to Cipher.getIV()
     */
    private byte[] getCipherTextBySpec(GCMParameterSpec spec) throws Exception {
        // init a cipher
        Cipher cipher = createCipher(Cipher.ENCRYPT_MODE, spec);
        cipher.updateAAD(AAD);
        byte[] cipherText = cipher.doFinal(data);

        // check IVs
        if (!Arrays.equals(cipher.getIV(), spec.getIV())) {
            System.out.println("IV in parameters is incorrect");
            return null;
        }
        if (spec.getTLen() != (cipherText.length - data.length) * 8) {
            System.out.println("Tag length is incorrect");
            return null;
        }
        return cipherText;
    }

    private byte[] recoverCipherText(byte[] cipherText, GCMParameterSpec spec)
            throws Exception {
        // init a cipher
        Cipher cipher = createCipher(Cipher.DECRYPT_MODE, spec);

        // check IVs
        if (!Arrays.equals(cipher.getIV(), spec.getIV())) {
            System.out.println("IV in parameters is incorrect");
            return null;
        }

        cipher.updateAAD(AAD);
        return cipher.doFinal(cipherText);
    }

    private Cipher createCipher(int mode, GCMParameterSpec spec)
            throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION, "SunJCE");
        cipher.init(mode, key, spec);
        return cipher;
    }
}
