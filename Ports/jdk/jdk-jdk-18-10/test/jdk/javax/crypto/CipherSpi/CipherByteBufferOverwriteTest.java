/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8181386
 * @summary CipherSpi ByteBuffer to byte array conversion fails for
 *          certain data overlap conditions
 * @run main CipherByteBufferOverwriteTest AES/CBC/PKCS5Padding 0 false
 * @run main CipherByteBufferOverwriteTest AES/CBC/PKCS5Padding 0 true
 * @run main CipherByteBufferOverwriteTest AES/CBC/PKCS5Padding 4 false
 * @run main CipherByteBufferOverwriteTest AES/CBC/PKCS5Padding 4 true
 * @run main CipherByteBufferOverwriteTest AES/GCM/NoPadding 0 false
 * @run main CipherByteBufferOverwriteTest AES/GCM/NoPadding 0 true
 * @run main CipherByteBufferOverwriteTest AES/GCM/NoPadding 4 false
 * @run main CipherByteBufferOverwriteTest AES/GCM/NoPadding 4 true
 */

import java.math.BigInteger;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class CipherByteBufferOverwriteTest {

    private static final boolean DEBUG = false;

    private static String transformation;

    // must be larger than the temp array size, i.e. 4096, hardcoded in
    // javax.crypto.CipherSpi class
    private static final int PLAINTEXT_SIZE = 8192;
    // leave room for padding
    private static final int CIPHERTEXT_BUFFER_SIZE = PLAINTEXT_SIZE + 32;

    private static final SecretKey KEY = new SecretKeySpec(new byte[16], "AES");
    private static AlgorithmParameterSpec params;

    private static ByteBuffer inBuf;
    private static ByteBuffer outBuf;

    private enum BufferType {
        ALLOCATE, DIRECT, WRAP;
    }

    public static void main(String[] args) throws Exception {

        transformation = args[0];
        int offset = Integer.parseInt(args[1]);
        boolean useRO = Boolean.parseBoolean(args[2]);

        if (transformation.equalsIgnoreCase("AES/GCM/NoPadding")) {
            params = new GCMParameterSpec(16 * 8, new byte[16]);
        } else {
            params = new IvParameterSpec(new byte[16]);
        }
        // an all-zeros plaintext is the easiest way to demonstrate the issue,
        // but it fails with any plaintext, of course
        byte[] expectedPT = new byte[PLAINTEXT_SIZE];
        byte[] buf = new byte[offset + CIPHERTEXT_BUFFER_SIZE];
        System.arraycopy(expectedPT, 0, buf, 0, PLAINTEXT_SIZE);

        // generate expected cipher text using byte[] methods
        Cipher c = Cipher.getInstance(transformation);
        c.init(Cipher.ENCRYPT_MODE, KEY, params);
        byte[] expectedCT = c.doFinal(expectedPT);

        // Test#1: against ByteBuffer generated with allocate(int) call
        prepareBuffers(BufferType.ALLOCATE, useRO, buf.length,
                buf, 0, PLAINTEXT_SIZE, offset);

        runTest(offset, expectedPT, expectedCT);
        System.out.println("\tALLOCATE: passed");

        // Test#2: against direct ByteBuffer
        prepareBuffers(BufferType.DIRECT, useRO, buf.length,
                buf, 0, PLAINTEXT_SIZE, offset);

        runTest(offset, expectedPT, expectedCT);
        System.out.println("\tDIRECT: passed");

        // Test#3: against ByteBuffer wrapping existing array
        prepareBuffers(BufferType.WRAP, useRO, buf.length,
                buf, 0, PLAINTEXT_SIZE, offset);

        runTest(offset, expectedPT, expectedCT);
        System.out.println("\tWRAP: passed");

        System.out.println("All Tests Passed");
    }

    private static void prepareBuffers(BufferType type,
            boolean useRO, int bufSz, byte[] in, int inOfs, int inLen,
            int outOfs) {
        switch (type) {
            case ALLOCATE:
                outBuf = ByteBuffer.allocate(bufSz);
                inBuf = outBuf.slice();
                inBuf.put(in, inOfs, inLen);
                inBuf.rewind();
                inBuf.limit(inLen);
                outBuf.position(outOfs);
                break;
            case DIRECT:
                outBuf = ByteBuffer.allocateDirect(bufSz);
                inBuf = outBuf.slice();
                inBuf.put(in, inOfs, inLen);
                inBuf.rewind();
                inBuf.limit(inLen);
                outBuf.position(outOfs);
                break;
            case WRAP:
                if (in.length < bufSz) {
                    throw new RuntimeException("ERROR: Input buffer too small");
                }
                outBuf = ByteBuffer.wrap(in);
                inBuf = ByteBuffer.wrap(in, inOfs, inLen);
                outBuf.position(outOfs);
                break;
        }
        if (useRO) {
            inBuf = inBuf.asReadOnlyBuffer();
        }
        if (DEBUG) {
            System.out.println("inBuf, pos = " + inBuf.position() +
                ", capacity = " + inBuf.capacity() +
                ", limit = " + inBuf.limit() +
                ", remaining = " + inBuf.remaining());
            System.out.println("outBuf, pos = " + outBuf.position() +
                ", capacity = " + outBuf.capacity() +
                ", limit = " + outBuf.limit() +
                ", remaining = " + outBuf.remaining());
        }
    }

    private static void runTest(int ofs, byte[] expectedPT, byte[] expectedCT)
            throws Exception {

        Cipher c = Cipher.getInstance(transformation);
        c.init(Cipher.ENCRYPT_MODE, KEY, params);
        int ciphertextSize = c.doFinal(inBuf, outBuf);

        // read out the encrypted result
        outBuf.position(ofs);
        byte[] finalCT = new byte[ciphertextSize];
        if (DEBUG) {
            System.out.println("runTest, ciphertextSize = " + ciphertextSize);
            System.out.println("runTest, ofs = " + ofs +
                ", remaining = " + finalCT.length +
                ", limit = " + outBuf.limit());
        }
        outBuf.get(finalCT);

        if (!Arrays.equals(finalCT, expectedCT)) {
            System.err.println("Ciphertext mismatch:" +
                "\nresult   (len=" + finalCT.length + "):\n" +
                String.format("%0" + (finalCT.length << 1) + "x",
                    new BigInteger(1, finalCT)) +
                "\nexpected (len=" + expectedCT.length + "):\n" +
                String.format("%0" + (expectedCT.length << 1) + "x",
                    new BigInteger(1, expectedCT)));
            throw new Exception("ERROR: Ciphertext does not match");
        }

        // now do decryption
        outBuf.position(ofs);
        outBuf.limit(ofs + ciphertextSize);
        c.init(Cipher.DECRYPT_MODE, KEY, params);
        ByteBuffer finalPTBuf = ByteBuffer.allocate(
                c.getOutputSize(outBuf.remaining()));
        c.doFinal(outBuf, finalPTBuf);

        // read out the decrypted result
        finalPTBuf.flip();
        byte[] finalPT = new byte[finalPTBuf.remaining()];
        finalPTBuf.get(finalPT);

        if (!Arrays.equals(finalPT, expectedPT)) {
            System.err.println("Ciphertext mismatch " +
                "):\nresult   (len=" + finalCT.length + "):\n" +
                String.format("%0" + (finalCT.length << 1) + "x",
                    new BigInteger(1, finalCT)) +
                "\nexpected (len=" + expectedCT.length + "):\n" +
                String.format("%0" + (expectedCT.length << 1) + "x",
                    new BigInteger(1, expectedCT)));
            throw new Exception("ERROR: Plaintext does not match");
        }
    }
}

