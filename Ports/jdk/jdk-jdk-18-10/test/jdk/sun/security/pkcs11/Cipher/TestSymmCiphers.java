/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 4898461 6604496
 * @summary basic test for symmetric ciphers with padding
 * @author Valerie Peng
 * @library /test/lib ..
 * @key randomness
 * @modules jdk.crypto.cryptoki
 * @run main/othervm TestSymmCiphers
 * @run main/othervm -Djava.security.manager=allow TestSymmCiphers sm
 */

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.util.Random;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class TestSymmCiphers extends PKCS11Test {

    private static class CI { // class for holding Cipher Information

        String transformation;
        String keyAlgo;
        int dataSize;

        CI(String transformation, String keyAlgo, int dataSize) {
            this.transformation = transformation;
            this.keyAlgo = keyAlgo;
            this.dataSize = dataSize;
        }
    }
    private static final CI[] TEST_LIST = {
        new CI("ARCFOUR", "ARCFOUR", 400),
        new CI("RC4", "RC4", 401),
        new CI("DES/CBC/NoPadding", "DES", 400),
        new CI("DESede/CBC/NoPadding", "DESede", 160),
        new CI("AES/CBC/NoPadding", "AES", 4800),
        new CI("Blowfish/CBC/NoPadding", "Blowfish", 24),
        new CI("DES/cbc/PKCS5Padding", "DES", 6401),
        new CI("DESede/CBC/PKCS5Padding", "DESede", 402),
        new CI("AES/CBC/PKCS5Padding", "AES", 30),
        new CI("Blowfish/CBC/PKCS5Padding", "Blowfish", 19),
        new CI("DES/ECB/NoPadding", "DES", 400),
        new CI("DESede/ECB/NoPadding", "DESede", 160),
        new CI("AES/ECB/NoPadding", "AES", 4800),
        new CI("DES/ECB/PKCS5Padding", "DES", 32),
        new CI("DES/ECB/PKCS5Padding", "DES", 6400),
        new CI("DESede/ECB/PKCS5Padding", "DESede", 400),
        new CI("AES/ECB/PKCS5Padding", "AES", 64),

        new CI("DES", "DES", 6400),
        new CI("DESede", "DESede", 408),
        new CI("AES", "AES", 128),

        new CI("AES/CTR/NoPadding", "AES", 3200)

    };
    private static StringBuffer debugBuf = new StringBuffer();

    @Override
    public void main(Provider p) throws Exception {
        // NSS reports CKR_DEVICE_ERROR when the data passed to
        // its EncryptUpdate/DecryptUpdate is not multiple of blocks
        int firstBlkSize = 16;
        boolean status = true;
        Random random = new Random();
        try {
            for (int i = 0; i < TEST_LIST.length; i++) {
                CI currTest = TEST_LIST[i];
                System.out.println("===" + currTest.transformation + "===");
                try {
                    KeyGenerator kg =
                            KeyGenerator.getInstance(currTest.keyAlgo, p);
                    SecretKey key = kg.generateKey();
                    Cipher c1 = Cipher.getInstance(currTest.transformation, p);
                    Cipher c2 = Cipher.getInstance(currTest.transformation,
                            "SunJCE");

                    byte[] plainTxt = new byte[currTest.dataSize];
                    random.nextBytes(plainTxt);
                    System.out.println("Testing inLen = " + plainTxt.length);

                    c2.init(Cipher.ENCRYPT_MODE, key);
                    AlgorithmParameters params = c2.getParameters();
                    byte[] answer = c2.doFinal(plainTxt);
                    System.out.println("Encryption tests: START");
                    test(c1, Cipher.ENCRYPT_MODE, key, params, firstBlkSize,
                            plainTxt, answer);
                    System.out.println("Encryption tests: DONE");
                    c2.init(Cipher.DECRYPT_MODE, key, params);
                    byte[] answer2 = c2.doFinal(answer);
                    System.out.println("Decryption tests: START");
                    test(c1, Cipher.DECRYPT_MODE, key, params, firstBlkSize,
                            answer, answer2);
                    System.out.println("Decryption tests: DONE");
                } catch (NoSuchAlgorithmException nsae) {
                    System.out.println("Skipping unsupported algorithm: " +
                            nsae);
                }
            }
        } catch (Exception ex) {
            // print out debug info when exception is encountered
            if (debugBuf != null) {
                System.out.println(debugBuf.toString());
                debugBuf = new StringBuffer();
            }
            throw ex;
        }
    }

    private static void test(Cipher cipher, int mode, SecretKey key,
            AlgorithmParameters params, int firstBlkSize,
            byte[] in, byte[] answer) throws Exception {
        // test setup
        long startTime, endTime;
        cipher.init(mode, key, params);
        int outLen = cipher.getOutputSize(in.length);
        //debugOut("Estimated output size = " + outLen + "\n");

        // test data preparation
        ByteBuffer inBuf = ByteBuffer.allocate(in.length);
        inBuf.put(in);
        inBuf.position(0);
        ByteBuffer inDirectBuf = ByteBuffer.allocateDirect(in.length);
        inDirectBuf.put(in);
        inDirectBuf.position(0);
        ByteBuffer outBuf = ByteBuffer.allocate(outLen);
        ByteBuffer outDirectBuf = ByteBuffer.allocateDirect(outLen);

        // test#1: byte[] in + byte[] out
        //debugOut("Test#1:\n");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        startTime = System.nanoTime();
        byte[] temp = cipher.update(in, 0, firstBlkSize);
        if (temp != null && temp.length > 0) {
            baos.write(temp, 0, temp.length);
        }
        temp = cipher.doFinal(in, firstBlkSize, in.length - firstBlkSize);
        if (temp != null && temp.length > 0) {
            baos.write(temp, 0, temp.length);
        }
        byte[] testOut1 = baos.toByteArray();
        endTime = System.nanoTime();
        perfOut("stream InBuf + stream OutBuf: " +
                (endTime - startTime));
        match(testOut1, answer);

        // test#2: Non-direct Buffer in + non-direct Buffer out
        //debugOut("Test#2:\n");
        //debugOut("inputBuf: " + inBuf + "\n");
        //debugOut("outputBuf: " + outBuf + "\n");

        startTime = System.nanoTime();
        cipher.update(inBuf, outBuf);
        cipher.doFinal(inBuf, outBuf);
        endTime = System.nanoTime();
        perfOut("non-direct InBuf + non-direct OutBuf: " +
                (endTime - startTime));
        match(outBuf, answer);

        // test#3: Direct Buffer in + direc Buffer out
        //debugOut("Test#3:\n");
        //debugOut("(pre) inputBuf: " + inDirectBuf + "\n");
        //debugOut("(pre) outputBuf: " + outDirectBuf + "\n");

        startTime = System.nanoTime();
        cipher.update(inDirectBuf, outDirectBuf);
        cipher.doFinal(inDirectBuf, outDirectBuf);
        endTime = System.nanoTime();
        perfOut("direct InBuf + direct OutBuf: " +
                (endTime - startTime));

        //debugOut("(post) inputBuf: " + inDirectBuf + "\n");
        //debugOut("(post) outputBuf: " + outDirectBuf + "\n");
        match(outDirectBuf, answer);

        // test#4: Direct Buffer in + non-direct Buffer out
        //debugOut("Test#4:\n");
        inDirectBuf.position(0);
        outBuf.position(0);
        //debugOut("inputBuf: " + inDirectBuf + "\n");
        //debugOut("outputBuf: " + outBuf + "\n");

        startTime = System.nanoTime();
        cipher.update(inDirectBuf, outBuf);
        cipher.doFinal(inDirectBuf, outBuf);
        endTime = System.nanoTime();
        perfOut("direct InBuf + non-direct OutBuf: " +
                (endTime - startTime));
        match(outBuf, answer);

        // test#5: Non-direct Buffer in + direct Buffer out
        //debugOut("Test#5:\n");
        inBuf.position(0);
        outDirectBuf.position(0);

        //debugOut("(pre) inputBuf: " + inBuf + "\n");
        //debugOut("(pre) outputBuf: " + outDirectBuf + "\n");

        startTime = System.nanoTime();
        cipher.update(inBuf, outDirectBuf);
        cipher.doFinal(inBuf, outDirectBuf);
        endTime = System.nanoTime();
        perfOut("non-direct InBuf + direct OutBuf: " +
                (endTime - startTime));

        //debugOut("(post) inputBuf: " + inBuf + "\n");
        //debugOut("(post) outputBuf: " + outDirectBuf + "\n");
        match(outDirectBuf, answer);

        debugBuf = null;
    }

    private static void perfOut(String msg) {
        if (debugBuf != null) {
            debugBuf.append("PERF>" + msg);
        }
    }

    private static void debugOut(String msg) {
        if (debugBuf != null) {
            debugBuf.append(msg);
        }
    }

    private static void match(byte[] b1, byte[] b2) throws Exception {
        if (b1.length != b2.length) {
            debugOut("got len   : " + b1.length + "\n");
            debugOut("expect len: " + b2.length + "\n");
            throw new Exception("mismatch - different length! got: " + b1.length + ", expect: " + b2.length + "\n");
        } else {
            for (int i = 0; i < b1.length; i++) {
                if (b1[i] != b2[i]) {
                    debugOut("got   : " + toString(b1) + "\n");
                    debugOut("expect: " + toString(b2) + "\n");
                    throw new Exception("mismatch");
                }
            }
        }
    }

    private static void match(ByteBuffer bb, byte[] answer) throws Exception {
        byte[] bbTemp = new byte[bb.position()];
        bb.position(0);
        bb.get(bbTemp, 0, bbTemp.length);
        match(bbTemp, answer);
    }

    public static void main(String[] args) throws Exception {
        main(new TestSymmCiphers(), args);
    }
}
