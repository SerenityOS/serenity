/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8035968
 * @summary C2 support for MD5/SHA-1/SHA-224/SHA-256/SHA-384/SHA-512/SHA3
 *
 * @run main/othervm/timeout=600 -Xbatch
 *      -Dalgorithm=MD5
 *      compiler.intrinsics.sha.TestDigest
 * @run main/othervm/timeout=600 -Xbatch
 *      -Dalgorithm=SHA-1
 *      compiler.intrinsics.sha.TestDigest
 * @run main/othervm/timeout=600 -Xbatch
 *      -Dalgorithm=SHA-224
 *      compiler.intrinsics.sha.TestDigest
 * @run main/othervm/timeout=600 -Xbatch
 *      -Dalgorithm=SHA-256
 *      compiler.intrinsics.sha.TestDigest
 * @run main/othervm/timeout=600 -Xbatch
 *      -Dalgorithm=SHA-384
 *      compiler.intrinsics.sha.TestDigest
 * @run main/othervm/timeout=600 -Xbatch
 *      -Dalgorithm=SHA-512
 *      compiler.intrinsics.sha.TestDigest
 * @run main/othervm/timeout=600 -Xbatch
 *      -Dalgorithm=SHA3-224
 *      compiler.intrinsics.sha.TestDigest
 * @run main/othervm/timeout=600 -Xbatch
 *      -Dalgorithm=SHA3-256
 *      compiler.intrinsics.sha.TestDigest
 * @run main/othervm/timeout=600 -Xbatch
 *      -Dalgorithm=SHA3-384
 *      compiler.intrinsics.sha.TestDigest
 * @run main/othervm/timeout=600 -Xbatch
 *      -Dalgorithm=SHA3-512
 *      compiler.intrinsics.sha.TestDigest
 *
 * @run main/othervm/timeout=600 -Xbatch
 *      -Dalgorithm=MD5   -Doffset=1
 *      compiler.intrinsics.sha.TestDigest
 * @run main/othervm/timeout=600 -Xbatch
 *      -Dalgorithm=SHA-1   -Doffset=1
 *      compiler.intrinsics.sha.TestDigest
 * @run main/othervm/timeout=600 -Xbatch
 *      -Dalgorithm=SHA-224 -Doffset=1
 *      compiler.intrinsics.sha.TestDigest
 * @run main/othervm/timeout=600 -Xbatch
 *      -Dalgorithm=SHA-256 -Doffset=1
 *      compiler.intrinsics.sha.TestDigest
 * @run main/othervm/timeout=600 -Xbatch
 *      -Dalgorithm=SHA-384 -Doffset=1
 *      compiler.intrinsics.sha.TestDigest
 * @run main/othervm/timeout=600 -Xbatch
 *      -Dalgorithm=SHA-512 -Doffset=1
 *      compiler.intrinsics.sha.TestDigest
 * @run main/othervm/timeout=600 -Xbatch
 *      -Dalgorithm=SHA3-224 -Doffset=1
 *      compiler.intrinsics.sha.TestDigest
 * @run main/othervm/timeout=600 -Xbatch
 *      -Dalgorithm=SHA3-256 -Doffset=1
 *      compiler.intrinsics.sha.TestDigest
 * @run main/othervm/timeout=600 -Xbatch
 *      -Dalgorithm=SHA3-384 -Doffset=1
 *      compiler.intrinsics.sha.TestDigest
 * @run main/othervm/timeout=600 -Xbatch
 *      -Dalgorithm=SHA3-512 -Doffset=1
 *      compiler.intrinsics.sha.TestDigest
 *
 * @run main/othervm/timeout=600 -Xbatch
 *      -Dalgorithm=SHA-1   -Dalgorithm2=SHA-256
 *      compiler.intrinsics.sha.TestDigest
 * @run main/othervm/timeout=600 -Xbatch
 *      -Dalgorithm=SHA-1   -Dalgorithm2=SHA-512
 *      compiler.intrinsics.sha.TestDigest
 * @run main/othervm/timeout=600 -Xbatch
 *      -Dalgorithm=SHA-256 -Dalgorithm2=SHA-512
 *      compiler.intrinsics.sha.TestDigest
 *
 * @run main/othervm/timeout=600 -Xbatch
 *      -Dalgorithm=SHA-1   -Dalgorithm2=MD5
 *      compiler.intrinsics.sha.TestDigest
 * @run main/othervm/timeout=600 -Xbatch
 *      -Dalgorithm=MD5     -Dalgorithm2=SHA-1
 *      compiler.intrinsics.sha.TestDigest
 *
 * @run main/othervm/timeout=600 -Xbatch
 *      -Dalgorithm=SHA-1   -Dalgorithm2=SHA3-224
 *      compiler.intrinsics.sha.TestDigest
 * @run main/othervm/timeout=600 -Xbatch
 *      -Dalgorithm=SHA-1   -Dalgorithm2=SHA3-256
 *      compiler.intrinsics.sha.TestDigest
 * @run main/othervm/timeout=600 -Xbatch
 *      -Dalgorithm=SHA-1   -Dalgorithm2=SHA3-384
 *      compiler.intrinsics.sha.TestDigest
 * @run main/othervm/timeout=600 -Xbatch
 *      -Dalgorithm=SHA-1   -Dalgorithm2=SHA3-512
 *      compiler.intrinsics.sha.TestDigest
 * @run main/othervm/timeout=600 -Xbatch
 *      -Dalgorithm=SHA3-224 -Dalgorithm2=SHA-1
 *      compiler.intrinsics.sha.TestDigest
 * @run main/othervm/timeout=600 -Xbatch
 *      -Dalgorithm=SHA3-256 -Dalgorithm2=SHA-1
 *      compiler.intrinsics.sha.TestDigest
 * @run main/othervm/timeout=600 -Xbatch
 *      -Dalgorithm=SHA3-384 -Dalgorithm2=SHA-1
 *      compiler.intrinsics.sha.TestDigest
 * @run main/othervm/timeout=600 -Xbatch
 *      -Dalgorithm=SHA3-512 -Dalgorithm2=SHA-1
 *      compiler.intrinsics.sha.TestDigest
 */

package compiler.intrinsics.sha;

import java.security.MessageDigest;
import java.util.Arrays;

public class TestDigest {
    private static final int HASH_LEN = 64; /* up to 512-bit */
    private static final int ALIGN = 8;     /* for different data alignments */

    public static void main(String[] args) throws Exception {
        String provider = System.getProperty("provider", "SUN");
        String algorithm = System.getProperty("algorithm", "SHA-1");
        String algorithm2 = System.getProperty("algorithm2", "");
        int msgSize = Integer.getInteger("msgSize", 1024);
        int offset = Integer.getInteger("offset", 0)  % ALIGN;
        int iters = (args.length > 0 ? Integer.valueOf(args[0]) : 100000);
        int warmupIters = (args.length > 1 ? Integer.valueOf(args[1]) : 20000);

        testDigest(provider, algorithm, msgSize, offset, iters, warmupIters);

        if (algorithm2.equals("") == false) {
            testDigest(provider, algorithm2, msgSize, offset, iters, warmupIters);
        }
    }

    public static void testDigest(String provider, String algorithm, int msgSize,
                        int offset, int iters, int warmupIters) throws Exception {
        System.out.println("provider = " + provider);
        System.out.println("algorithm = " + algorithm);
        System.out.println("msgSize = " + msgSize + " bytes");
        System.out.println("offset = " + offset);
        System.out.println("iters = " + iters);

        byte[] expectedHash = new byte[HASH_LEN];
        byte[] hash = new byte[HASH_LEN];
        byte[] data = new byte[msgSize + offset];
        for (int i = 0; i < (msgSize + offset); i++) {
            data[i] = (byte)(i & 0xff);
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm, provider);

            /* do once, which doesn't use intrinsics */
            digest.reset();
            digest.update(data, offset, msgSize);
            expectedHash = digest.digest();

            /* warm up */
            for (int i = 0; i < warmupIters; i++) {
                digest.reset();
                digest.update(data, offset, msgSize);
                hash = digest.digest();
            }

            /* check result */
            if (Arrays.equals(hash, expectedHash) == false) {
                System.out.println("TestDigest Error: ");
                showArray(expectedHash, "expectedHash");
                showArray(hash,         "computedHash");
                //System.exit(1);
                throw new Exception("TestDigest Error");
            } else {
                showArray(hash, "hash");
            }

            /* measure performance */
            long start = System.nanoTime();
            for (int i = 0; i < iters; i++) {
                digest.reset();
                digest.update(data, offset, msgSize);
                hash = digest.digest();
            }
            long end = System.nanoTime();
            double total = (double)(end - start)/1e9;         /* in seconds */
            double thruput = (double)msgSize*iters/1e6/total; /* in MB/s */
            System.out.println("TestDigest runtime = " + total + " seconds");
            System.out.println("TestDigest throughput = " + thruput + " MB/s");
            System.out.println();
        } catch (Exception e) {
            System.out.println("Exception: " + e);
            //System.exit(1);
            throw new Exception(e);
        }
    }

    static void showArray(byte b[], String name) {
        System.out.format("%s [%d]: ", name, b.length);
        for (int i = 0; i < Math.min(b.length, HASH_LEN); i++) {
            System.out.format("%02x ", b[i] & 0xff);
        }
        System.out.println();
    }
}
