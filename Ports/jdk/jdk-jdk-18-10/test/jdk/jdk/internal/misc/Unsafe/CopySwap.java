/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.misc.Unsafe;

/*
 * @test
 * @summary Test Unsafe.copySwapMemory
 * @modules java.base/jdk.internal.misc
 */
public class CopySwap extends CopyCommon {
    private CopySwap() {
    }

    /**
     * Run positive tests
     *
     * @throws RuntimeException if an error is found
     */
    private void testPositive() {
        testSmallCopy(true);
    }

    /**
     * Run negative tests, testing corner cases and the various exceptions
     *
     * @throws RuntimeException if an error is found
     */
    private void testNegative() {
        long bufRaw = 0;

        try {
            bufRaw = UNSAFE.allocateMemory(1024);
            long buf = CopyCommon.alignUp(bufRaw, CopyCommon.BASE_ALIGNMENT);
            short[] arr = new short[16];

            // Check various illegal element sizes
            for (int elemSize = 2; elemSize <= 8; elemSize <<= 1) {
                long[] illegalSizes = { -1, 1, elemSize - 1, elemSize + 1, elemSize * 2 - 1 };
                for (long size : illegalSizes) {
                    try {
                        // Check that illegal elemSize throws an IAE
                        UNSAFE.copySwapMemory(null, buf, null, buf, size, elemSize);
                        throw new RuntimeException("copySwapMemory failed to throw IAE for size=" + size + " elemSize=" + elemSize);
                    } catch (IllegalArgumentException e) {
                        // good
                    }
                }
            }

            try {
                // Check that negative srcOffset throws an IAE
                UNSAFE.copySwapMemory(arr, -1, arr, UNSAFE.arrayBaseOffset(arr.getClass()), 16, 2);
                throw new RuntimeException("copySwapMemory failed to throw IAE for srcOffset=-1");
            } catch (IllegalArgumentException e) {
                // good
            }

            try {
                // Check that negative dstOffset throws an IAE
                UNSAFE.copySwapMemory(arr, UNSAFE.arrayBaseOffset(arr.getClass()), arr, -1, 16, 2);
                throw new RuntimeException("copySwapMemory failed to throw IAE for destOffset=-1");
            } catch (IllegalArgumentException e) {
                // good
            }

            long illegalElemSizes[] = { 0, 1, 3, 5, 6, 7, 9, 10, -1 };
            for (long elemSize : illegalElemSizes) {
                try {
                    // Check that elemSize 1 throws an IAE
                    UNSAFE.copySwapMemory(null, buf, null, buf, 16, elemSize);
                    throw new RuntimeException("copySwapMemory failed to throw NPE");
                } catch (IllegalArgumentException e) {
                    // good
                }
            }

            try {
                // Check that a reference array destination throws IAE
                UNSAFE.copySwapMemory(null, buf, new Object[16], UNSAFE.arrayBaseOffset(Object[].class), 16, 8);
                throw new RuntimeException("copySwapMemory failed to throw NPE");
            } catch (IllegalArgumentException e) {
                // good
            }

            // Check that invalid source & dest pointers throw IAEs (only relevant on 32-bit platforms)
            if (UNSAFE.addressSize() == 4) {
                long invalidPtr = (long)1 << 35; // Pick a random bit in upper 32 bits

                try {
                    // Check that an invalid (not 32-bit clean) source pointer throws IAE
                    UNSAFE.copySwapMemory(null, invalidPtr, null, buf, 16, 2);
                    throw new RuntimeException("copySwapMemory failed to throw IAE for srcOffset 0x" +
                                               Long.toHexString(invalidPtr));
                } catch (IllegalArgumentException e) {
                    // good
                }

                try {
                    // Check that an invalid (not 32-bit clean) source pointer throws IAE
                    UNSAFE.copySwapMemory(null, buf, null, invalidPtr, 16, 2);
                    throw new RuntimeException("copySwapMemory failed to throw IAE for destOffset 0x" +
                                               Long.toHexString(invalidPtr));
                } catch (IllegalArgumentException e) {
                    // good
                }
            }
        } finally {
            if (bufRaw != 0) {
                UNSAFE.freeMemory(bufRaw);
            }
        }
    }

    /**
     * Run all tests
     *
     * @throws RuntimeException if an error is found
     */
    private void test() {
        testPositive();
        testNegative();
    }

    public static void main(String[] args) {
        CopySwap cs = new CopySwap();
        cs.test();
    }
}
