/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, Red Hat, Inc. All rights reserved.
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
 * @bug 8193518 8249608
 * @summary C2: Vector registers are sometimes corrupted at safepoint
 * @run main/othervm -XX:-BackgroundCompilation -XX:+IgnoreUnrecognizedVMOptions -XX:+UseCountedLoopSafepoints -XX:LoopStripMiningIter=2 -XX:-TieredCompilation TestVectorsNotSavedAtSafepoint test1
 * @run main/othervm -XX:-BackgroundCompilation TestVectorsNotSavedAtSafepoint test2
 */

import java.util.Arrays;

public class TestVectorsNotSavedAtSafepoint {

    static void test1(byte[] barray1, byte[] barray2, byte[] barray3, long[] larray, long v) {
        // Uses wide vectors, v in vector registers is live at the
        // safepoint of the outer strip mined loop
        for (int i = 0; i < larray.length; i++) {
            larray[i] = v;
        }
        // Runs for few iterations so limited unrolling and short
        // vectors
        for (int i = 0; i < barray3.length; i++) {
            barray3[i] = (byte)(barray1[i] + barray2[i]);
        }
    }

    public static void test2(int[] iArr, long[] lArr) {
        // Loop with wide and non-wide vectors
        for (int i = 0; i < lArr.length; i++) {
            iArr[i] = 1;
            lArr[i] = 1;
        }
    }

    static class GarbageProducerThread extends Thread {
        public void run() {
            for(;;) {
                // Produce some garbage and then let the GC do its work which will
                // corrupt vector registers if they are not saved at safepoints.
                Object[] arrays = new Object[1024];
                for (int i = 0; i < arrays.length; i++) {
                    arrays[i] = new int[1024];
                }
                System.gc();
            }
        }
    }

    public static void main(String[] args) {
        Thread garbage_producer = new GarbageProducerThread();
        garbage_producer.setDaemon(true);
        garbage_producer.start();

        if (args[0].equals("test1")) {
            byte[] bArr = new byte[10];
            long[] lArr = new long[1000];
            for (int i = 0; i < 10_000; ++i) {
                test1(bArr, bArr, bArr, lArr, -1);
                for (int j = 0; j < lArr.length; ++j) {
                    if (bArr[j % 10] != 0 || lArr[j] != -1) {
                        throw new RuntimeException("Test1 failed at iteration " + i + ": bArr[" + (j % 10) + "] = " + bArr[j % 10] + ", lArr[" + j + "] = " + lArr[j]);
                    }
                }
            }
        } else {
            int iArr[] = new int[100];
            long lArr[] = new long[100];
            for (int i = 0; i < 10_000; ++i) {
                test2(iArr, lArr);
                for (int j = 0; j < lArr.length; ++j) {
                    if (iArr[j] != 1 || lArr[j] != 1) {
                        throw new RuntimeException("Test2 failed at iteration " + i + ": iArr[" + j + "] = " + iArr[j] + ", lArr[" + j + "] = " + lArr[j]);
                    }
                }
            }
        }
    }
}

