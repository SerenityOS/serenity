/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8178374
 * @summary Test GCM decryption with various types of input/output
 *     ByteBuffer objects
 * @key randomness
 */

import java.nio.ByteBuffer;
import java.security.*;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.AEADBadTagException;
import javax.crypto.spec.*;

public class TestGCMWithByteBuffer {

    private static Random random = new SecureRandom();
    private static int dataSize = 4096; // see javax.crypto.CipherSpi
    private static int multiples = 3;

    public static void main(String args[]) throws Exception {
        Provider[] provs = Security.getProviders();
        for (Provider p : provs) {
            try {
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", p);
                test(cipher);
            } catch (NoSuchAlgorithmException nsae) {
                // skip testing due to no support
                continue;
            }
        }
    }

    private static void test(Cipher cipher) throws Exception {
        System.out.println("Testing " + cipher.getProvider());

        boolean failedOnce = false;
        Exception failedReason = null;

        int tagLen = 96; // in bits
        byte[] keyBytes = new byte[16];
        random.nextBytes(keyBytes);
        byte[] dataChunk = new byte[dataSize];
        random.nextBytes(dataChunk);

        SecretKey key = new SecretKeySpec(keyBytes, "AES");
        // re-use key bytes as IV as the real test is buffer calculation
        GCMParameterSpec s = new GCMParameterSpec(tagLen, keyBytes);

        /*
         * Iterate through various sizes to make sure that the code works with
         * internal temp buffer size 4096.
         */
        for (int t = 1; t <= multiples; t++) {
            int size = t * dataSize;

            System.out.println("\nTesting data size: " + size);

            try {
                decrypt(cipher, key, s, dataChunk, t,
                        ByteBuffer.allocate(dataSize),
                        ByteBuffer.allocate(size),
                        ByteBuffer.allocateDirect(dataSize),
                        ByteBuffer.allocateDirect(size));
            } catch (Exception e) {
                System.out.println("\tFailed with data size " + size);
                failedOnce = true;
                failedReason = e;
            }
        }
        if (failedOnce) {
            throw failedReason;
        }
        System.out.println("\n=> Passed...");
    }

    private enum TestVariant {
        HEAP_HEAP, HEAP_DIRECT, DIRECT_HEAP, DIRECT_DIRECT
    };

    private static void decrypt(Cipher cipher, SecretKey key,
            GCMParameterSpec s, byte[] dataChunk, int multiples,
            ByteBuffer heapIn, ByteBuffer heapOut, ByteBuffer directIn,
            ByteBuffer directOut) throws Exception {

        ByteBuffer inBB = null;
        ByteBuffer outBB = null;

        // try various combinations of input/output
        for (TestVariant tv : TestVariant.values()) {
            System.out.println(" " + tv);

            switch (tv) {
            case HEAP_HEAP:
                inBB = heapIn;
                outBB = heapOut;
                break;
            case HEAP_DIRECT:
                inBB = heapIn;
                outBB = directOut;
                break;
            case DIRECT_HEAP:
                inBB = directIn;
                outBB = heapOut;
                break;
            case DIRECT_DIRECT:
                inBB = directIn;
                outBB = directOut;
                break;
            }

            // prepare input and output buffers
            inBB.clear();
            inBB.put(dataChunk);

            outBB.clear();

            try {
                // Always re-init the Cipher object so cipher is in
                // a good state for future testing
                cipher.init(Cipher.DECRYPT_MODE, key, s);

                for (int i = 0; i < multiples; i++) {
                    inBB.flip();
                    cipher.update(inBB, outBB);
                    if (inBB.hasRemaining()) {
                        throw new Exception("buffer not empty");
                    }
                }
                // finish decryption and process all data buffered
                cipher.doFinal(inBB, outBB);
                throw new RuntimeException("Error: doFinal completed without exception");
            } catch (AEADBadTagException ex) {
                System.out.println("Expected AEADBadTagException thrown");
                continue;
            }
        }
    }
}
