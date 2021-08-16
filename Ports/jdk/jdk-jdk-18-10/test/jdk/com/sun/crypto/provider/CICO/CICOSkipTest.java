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

import static java.lang.System.out;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

/*
 * @test
 * @bug 8048604
 * @summary This test verifies the assertion "The skip feature of Filter
 *    streams should be supported." for feature
 *    CipherInputStream and CipherOutputStream
 */
public class CICOSkipTest {
    /**
     * Block length.
     */
    private static final int BLOCK = 50;

    /**
     * Saving bytes length.
     */
    private static final int SAVE = 45;

    /**
     * Plain text length.
     */
    private static final int PLAIN_TEXT_LENGTH = 800;

    /**
     * Skip reading byte size. This should be same to BLOCK - SAVE
     */
    private static final int DISCARD = BLOCK - SAVE;

    private static final String[] ALGOS = {"DES", "DESede", "Blowfish"};
    private static final String[] MODES = {"ECB", "CBC", "CFB", "CFB32",
        "OFB", "OFB64", "PCBC"};
    private static final String[] PADDINGS = {"NoPadding", "Pkcs5Padding"};
    private static final String[] PBE_ALGOS = {"PBEWithMD5AndDES",
        "PBEWithMD5AndDES/CBC/PKCS5Padding"};

    public static void main(String[] args) throws Exception {
        // how many kinds of padding mode such as PKCS5padding and NoPadding
        for (String algo : ALGOS) {
            for (String mode : MODES) {
                int padKinds = 1;
                if (mode.equalsIgnoreCase("ECB")
                        || mode.equalsIgnoreCase("PCBC")
                        || mode.equalsIgnoreCase("CBC")) {
                    padKinds = PADDINGS.length;
                }
                // PKCS5padding is meaningful only for ECB, CBC, PCBC
                for (int k = 0; k < padKinds; k++) {
                    String info = algo + "/" + mode + "/" + PADDINGS[k];
                    try {
                        CipherGenerator cg = new CipherGenerator(algo, mode,
                                PADDINGS[k]);
                        for (ReadMethod model : ReadMethod.values()) {
                            runTest(cg.getPair(), info, model);
                        }
                    } catch (LengthLimitException exp) {
                        // skip this if this key length is larger than what's
                        // configured in the jce jurisdiction policy files
                        out.println(exp.getMessage() + " is expected.");
                    }
                }
            }
        }
        for (String pbeAlgo : PBE_ALGOS) {
            for (ReadMethod model : ReadMethod.values()) {
                System.out.println("Testing Algorithm : " + pbeAlgo
                        + " ReadMethod : " + model);
                runTest(new CipherGenerator(pbeAlgo).getPair(), pbeAlgo, model);
            }
        }
    }

    private static void runTest(Cipher[] pair, String info, ReadMethod whichRead)
            throws IOException {
        byte[] plainText = TestUtilities.generateBytes(PLAIN_TEXT_LENGTH);
        out.println("Testing: " + info + "/" + whichRead);
        try (ByteArrayInputStream baInput = new ByteArrayInputStream(plainText);
                CipherInputStream ciInput1 = new CipherInputStream(baInput,
                        pair[0]);
                CipherInputStream ciInput2 = new CipherInputStream(ciInput1,
                        pair[1]);) {
            // Skip 5 bytes after read 45 bytes and repeat until finish
            // (Read from the input and write to the output using 2 types
            // of buffering : byte[] and int)
            // So output has size:
            // (OVERALL/BLOCK)* SAVE = (800 / 50) * 45 = 720 bytes
            int numOfBlocks = plainText.length / BLOCK;

            // Output buffer.
            byte[] outputText = new byte[numOfBlocks * SAVE];
            int index = 0;
            for (int i = 0; i < numOfBlocks; i++) {
                index = whichRead.readByte(ciInput2, outputText, SAVE, index);
                // If available is more than expected discard byte size. Skip
                // discard bytes, otherwise try to read discard bytes by read.
                if (ciInput2.available() >= DISCARD) {
                    ciInput2.skip(DISCARD);
                } else {
                    for (int k = 0; k < DISCARD; k++) {
                        ciInput2.read();
                    }
                }
            }
            // Verify output is same as input
            if (!TestUtilities
                    .equalsBlockPartial(plainText, outputText, BLOCK, SAVE)) {
                throw new RuntimeException("Test failed with compare fail");
            }
        }
    }
}

class CipherGenerator {
    /**
     * Initialization vector  length.
     */
    private static final int IV_LENGTH = 8;

    private static final String PASSWD = "Sesame!(@#$%^&*)";

    private final Cipher[] pair = new Cipher[2];

    // For DES/DESede ciphers
    CipherGenerator(String algo, String mo, String pad)
            throws NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException,
            NoSuchPaddingException, SecurityException, LengthLimitException {
        // Do initialization
        KeyGenerator kg = KeyGenerator.getInstance(algo);
        SecretKey key = kg.generateKey();
        if (key.getEncoded().length * 8 > Cipher.getMaxAllowedKeyLength(algo)) {
            // skip this if this key length is larger than what's
            // configured in the jce jurisdiction policy files
            throw new LengthLimitException(
                    "Skip this test if key length is larger than what's"
                    + "configured in the jce jurisdiction policy files");
        }
        AlgorithmParameterSpec aps = null;
        if (!mo.equalsIgnoreCase("ECB")) {
            byte[] iv = TestUtilities.generateBytes(IV_LENGTH);
            aps = new IvParameterSpec(iv);
        }
        initCiphers(algo + "/" + mo + "/" + pad, key, aps);
    }

    // For PBE ciphers
    CipherGenerator(String algo) throws NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException,
            NoSuchPaddingException, InvalidKeySpecException {
        // Do initialization
        byte[] salt = TestUtilities.generateBytes(IV_LENGTH);
        int iterCnt = 6;
        SecretKeyFactory skf = SecretKeyFactory.getInstance(algo.split("/")[0]);
        SecretKey key = skf
                .generateSecret(new PBEKeySpec(PASSWD.toCharArray()));
        AlgorithmParameterSpec aps = new PBEParameterSpec(salt, iterCnt);
        initCiphers(algo, key, aps);
    }

    private void initCiphers(String algo, SecretKey key,
            AlgorithmParameterSpec aps) throws NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException {
        Provider provider = Security.getProvider("SunJCE");
        if (provider == null) {
            throw new RuntimeException("SunJCE provider does not exist.");
        }
        Cipher ci1 = Cipher.getInstance(algo, provider);
        ci1.init(Cipher.ENCRYPT_MODE, key, aps);
        pair[0] = ci1;
        Cipher ci2 = Cipher.getInstance(algo, provider);
        ci2.init(Cipher.DECRYPT_MODE, key, aps);
        pair[1] = ci2;
    }

    Cipher[] getPair() {
        return pair;
    }
}

enum ReadMethod {
    // read one byte at a time for save times
    READ_ONE_BYTE {
        @Override
        int readByte(CipherInputStream ciIn2, byte[] outputText, int save,
                int index) throws IOException {
            for (int j = 0; j < save; j++, index++) {
                int buffer0 = ciIn2.read();
                if (buffer0 != -1) {
                    outputText[index] = (byte) buffer0;
                } else {
                    break;
                }
            }
            return index;
        }
    },
    // read a chunk of save bytes if possible
    READ_BLOCK {
        @Override
        int readByte(CipherInputStream ciIn2, byte[] outputText, int save,
                int index) throws IOException {
            int len1 = ciIn2.read(outputText, index, save);
            out.println("Init: index=" + index + ",len=" + len1);
            // read more until save bytes
            index += len1;
            int len2 = 0;
            while (len1 != save && len2 != -1) {
                len2 = ciIn2.read(outputText, index, save - len1);
                out.println("Cont: index=" + index + ",len=" + len2);
                len1 += len2;
                index += len2;
            }
            return index;
        }
    };

    abstract int readByte(CipherInputStream ciIn2, byte[] outputText, int save,
            int index) throws IOException;
};

class LengthLimitException extends Exception {

    public LengthLimitException(String string) {
        super(string);
    }
}
