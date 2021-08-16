/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

package compiler.codegen.aes;

import jdk.test.lib.Utils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.AlgorithmParameters;
import java.util.Random;

/**
 * @author Tom Deneau
 */
public abstract class TestAESBase {
    int msgSize = Integer.getInteger("msgSize", 646);
    boolean checkOutput = Boolean.getBoolean("checkOutput");
    boolean noReinit = Boolean.getBoolean("noReinit");
    boolean testingMisalignment;
    private static final int ALIGN = 8;
    int encInputOffset = Integer.getInteger("encInputOffset", 0) % ALIGN;
    int encOutputOffset = Integer.getInteger("encOutputOffset", 0) % ALIGN;
    int decOutputOffset = Integer.getInteger("decOutputOffset", 0) % ALIGN;
    int lastChunkSize = Integer.getInteger("lastChunkSize", 32);
    int keySize = Integer.getInteger("keySize", 128);
    int inputLength;
    int encodeLength;
    int decodeLength;
    int decodeMsgSize;
    String algorithm = System.getProperty("algorithm", "AES");
    String mode = System.getProperty("mode", "CBC");
    String paddingStr = System.getProperty("paddingStr",
        (mode.equals("GCM") || mode.equals("CTR") || mode.equals("CTS")) ?
         "NoPadding" : "PKCS5Padding");
    byte[] input;
    byte[] encode;
    byte[] expectedEncode;
    byte[] decode;
    byte[] expectedDecode;
    final Random random = Utils.getRandomInstance();
    Cipher cipher;
    Cipher dCipher;
    AlgorithmParameters algParams = null;
    SecretKey key;
    GCMParameterSpec gcm_spec;
    byte[] aad = {0x11, 0x22, 0x33, 0x44, 0x55};
    int tlen = 12;
    byte[] iv = new byte[16];

    static int numThreads = 0;
    int threadId;

    static synchronized int getThreadId() {
        int id = numThreads;
        numThreads++;
        return id;
    }

    abstract public void run();

    public void prepare() {
        try {
            System.out.println("\nalgorithm=" + algorithm + ", mode=" + mode + ", paddingStr=" + paddingStr +
                    ", msgSize=" + msgSize + ", keySize=" + keySize + ", noReinit=" + noReinit +
                    ", checkOutput=" + checkOutput + ", encInputOffset=" + encInputOffset + ", encOutputOffset=" +
                    encOutputOffset + ", decOutputOffset=" + decOutputOffset + ", lastChunkSize=" + lastChunkSize);

            if (encInputOffset % ALIGN != 0 || encOutputOffset % ALIGN != 0 || decOutputOffset % ALIGN != 0)
                testingMisalignment = true;

            int keyLenBytes = (keySize == 0 ? 16 : keySize / 8);
            byte keyBytes[] = new byte[keyLenBytes];
            if (keySize == 128)
                keyBytes = new byte[]{-8, -7, -6, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7};
            else
                random.nextBytes(keyBytes);

            key = new SecretKeySpec(keyBytes, algorithm);
            if (threadId == 0) {
                System.out.println("Algorithm: " + key.getAlgorithm() + "("
                        + key.getEncoded().length * 8 + "bit)");
            }

            cipher = Cipher.getInstance(algorithm + "/" + mode + "/" + paddingStr, "SunJCE");
            dCipher = Cipher.getInstance(algorithm + "/" + mode + "/" + paddingStr, "SunJCE");

            // CBC or CTR init
            if (mode.equals("CBC") || mode.equals("CTR")) {
                IvParameterSpec initVector = new IvParameterSpec(iv);
                cipher.init(Cipher.ENCRYPT_MODE, key, initVector);
                algParams = cipher.getParameters();
                dCipher.init(Cipher.DECRYPT_MODE, key, initVector);

                // GCM init
            } else if (mode.equals("GCM")) {
                gcm_init(true);
                gcm_init(false);

                // ECB init
            } else {
                cipher.init(Cipher.ENCRYPT_MODE, key, algParams);
                dCipher.init(Cipher.DECRYPT_MODE, key, algParams);
            }

            if (threadId == 0) {
                childShowCipher();
            }

            inputLength = msgSize + encInputOffset;
            if (testingMisalignment) {
                encodeLength = cipher.getOutputSize(msgSize - lastChunkSize) + encOutputOffset;
                encodeLength += cipher.getOutputSize(lastChunkSize);
                decodeLength = dCipher.getOutputSize(encodeLength - lastChunkSize) + decOutputOffset;
                decodeLength += dCipher.getOutputSize(lastChunkSize);
            } else {
                encodeLength = cipher.getOutputSize(msgSize) + encOutputOffset;
                decodeLength = dCipher.getOutputSize(encodeLength) + decOutputOffset;
            }

            input = new byte[inputLength];
            for (int i = encInputOffset, j = 0; i < inputLength; i++, j++) {
                input[i] = (byte) (j & 0xff);
            }

            // do one encode and decode in preparation
            encode = new byte[encodeLength];
            decode = new byte[decodeLength];
            if (testingMisalignment) {
                decodeMsgSize = cipher.update(input, encInputOffset, (msgSize - lastChunkSize), encode, encOutputOffset);
                decodeMsgSize += cipher.doFinal(input, (encInputOffset + msgSize - lastChunkSize), lastChunkSize, encode, (encOutputOffset + decodeMsgSize));

                int tempSize = dCipher.update(encode, encOutputOffset, (decodeMsgSize - lastChunkSize), decode, decOutputOffset);
                dCipher.doFinal(encode, (encOutputOffset + decodeMsgSize - lastChunkSize), lastChunkSize, decode, (decOutputOffset + tempSize));
            } else {
                decodeMsgSize = cipher.doFinal(input, encInputOffset, msgSize, encode, encOutputOffset);
                dCipher.doFinal(encode, encOutputOffset, decodeMsgSize, decode, decOutputOffset);
            }
            if (checkOutput) {
                expectedEncode = (byte[]) encode.clone();
                expectedDecode = (byte[]) decode.clone();
                showArray(key.getEncoded(), "key:    ");
                showArray(input, "input:  ");
                showArray(encode, "encode: ");
                showArray(decode, "decode: ");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    void showArray(byte b[], String name) {
        System.out.format("%s [%d]: ", name, b.length);
        for (int i = 0; i < Math.min(b.length, 32); i++) {
            System.out.format("%02x ", b[i] & 0xff);
        }
        System.out.println();
    }

    void compareArrays(byte b[], byte exp[]) {
        if (b.length != exp.length) {
            System.out.format("different lengths for actual and expected output arrays\n");
            showArray(b, "test: ");
            showArray(exp, "exp : ");
            System.exit(1);
        }
        for (int i = 0; i < exp.length; i++) {
            if (b[i] != exp[i]) {
                System.out.format("output error at index %d: got %02x, expected %02x\n", i, b[i] & 0xff, exp[i] & 0xff);
                showArray(b, "test: ");
                showArray(exp, "exp : ");
                System.exit(1);
            }
        }
    }

    void showCipher(Cipher c, String kind) {
        System.out.println(kind + " cipher provider: " + cipher.getProvider());
        System.out.println(kind + " cipher algorithm: " + cipher.getAlgorithm());
    }

    abstract void childShowCipher();

    void gcm_init(boolean encrypt) throws Exception {
        gcm_spec = new GCMParameterSpec(tlen * 8, iv);
        if (encrypt) {
            // Get a new instance everytime because of reuse IV restrictions
            cipher = Cipher.getInstance(algorithm + "/" + mode + "/" + paddingStr, "SunJCE");
            cipher.init(Cipher.ENCRYPT_MODE, key, gcm_spec);
            cipher.updateAAD(aad);
        } else {
            dCipher.init(Cipher.DECRYPT_MODE, key, gcm_spec);
            dCipher.updateAAD(aad);


        }
    }
}
