/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7142509
 * @summary Cipher.doFinal(ByteBuffer,ByteBuffer) fails to
 *     process when in.remaining() == 0
 * @key randomness
 */

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/*
 * Simple test case to show that Cipher.doFinal(ByteBuffer, ByteBuffer) fails to
 * process the data internally buffered inBB the cipher when input.remaining()
 * == 0 and at least one buffer is a direct buffer.
 */
public class DirectBBRemaining {

    private static Random random = new SecureRandom();
    private static int testSizes = 40;
    private static int outputFrequency = 5;

    public static void main(String args[]) throws Exception {
        boolean failedOnce = false;
        Exception failedReason = null;

        byte[] keyBytes = new byte[8];
        random.nextBytes(keyBytes);
        SecretKey key = new SecretKeySpec(keyBytes, "DES");

        Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding", "SunJCE");
        cipher.init(Cipher.ENCRYPT_MODE, key);

        /*
         * Iterate through various sizes to make sure that the code does empty
         * blocks, single partial blocks, 1 full block, full + partial blocks,
         * multiple full blocks, etc. 5 blocks (using DES) is probably overkill
         * but will feel more confident the fix isn't breaking anything.
         */
        System.out.println("Output test results for every "
                + outputFrequency + " tests...");

        for (int size = 0; size <= testSizes; size++) {
            boolean output = (size % outputFrequency) == 0;
            if (output) {
                System.out.print("\nTesting buffer size: " + size + ":");
            }

            int outSize = cipher.getOutputSize(size);

            try {
                encrypt(cipher, size,
                        ByteBuffer.allocate(size),
                        ByteBuffer.allocate(outSize),
                        ByteBuffer.allocateDirect(size),
                        ByteBuffer.allocateDirect(outSize),
                        output);
            } catch (Exception e) {
                System.out.print("\n    Failed with size " + size);
                failedOnce = true;
                failedReason = e;

                // If we got an exception, let's be safe for future
                // testing and reset the cipher to a known good state.
                cipher.init(Cipher.ENCRYPT_MODE, key);
            }
        }
        if (failedOnce) {
            throw failedReason;
        }
        System.out.println("\nTest Passed...");
    }

    private enum TestVariant {

        HEAP_HEAP, HEAP_DIRECT, DIRECT_HEAP, DIRECT_DIRECT
    };

    private static void encrypt(Cipher cipher, int size,
            ByteBuffer heapIn, ByteBuffer heapOut,
            ByteBuffer directIn, ByteBuffer directOut,
            boolean output) throws Exception {

        ByteBuffer inBB = null;
        ByteBuffer outBB = null;

        // Set up data and encrypt to known/expected values.
        byte[] testdata = new byte[size];
        random.nextBytes(testdata);
        byte[] expected = cipher.doFinal(testdata);

        for (TestVariant tv : TestVariant.values()) {
            if (output) {
                System.out.print(" " + tv);
            }

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

            inBB.clear();
            outBB.clear();

            inBB.put(testdata);
            inBB.flip();

            // Process all data in one shot, but don't call doFinal() yet.
            // May store up to n-1 bytes (w/block size n) internally.
            cipher.update(inBB, outBB);
            if (inBB.hasRemaining()) {
                throw new Exception("buffer not empty");
            }

            // finish encryption and process all data buffered
            cipher.doFinal(inBB, outBB);
            outBB.flip();

            // validate output size
            if (outBB.remaining() != expected.length) {
                throw new Exception(
                        "incomplete encryption output, expected "
                        + expected.length + " bytes but was only "
                        + outBB.remaining() + " bytes");
            }

            // validate output data
            byte[] encrypted = new byte[outBB.remaining()];
            outBB.get(encrypted);
            if (!Arrays.equals(expected, encrypted)) {
                throw new Exception("bad encryption output");
            }

            if (!Arrays.equals(cipher.doFinal(), cipher.doFinal())) {
                throw new Exception("Internal buffers still held data!");
            }
        }
    }
}
