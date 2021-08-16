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
 * @summary Test Unsafe.copyMemory
 * @modules java.base/jdk.internal.misc
 */
public class CopyMemory extends CopyCommon {
    private CopyMemory() {
    }

    /**
     * Run positive tests
     *
     * @throws RuntimeException if an error is found
     */
    private void testPositive() {
        testSmallCopy(false);
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

            // Check illegal sizes
            System.out.println("Testing negative size");
            try {
                UNSAFE.copyMemory(null, buf, null, buf, -1);
                throw new RuntimeException("copyMemory failed to throw IAE for size=-1");
            } catch (IllegalArgumentException e) {
                // good
            }

            System.out.println("Testing negative srcOffset");
            try {
                // Check that negative srcOffset throws an IAE
                UNSAFE.copyMemory(arr, -1, arr, UNSAFE.arrayBaseOffset(arr.getClass()), 16);
                throw new RuntimeException("copyMemory failed to throw IAE for srcOffset=-1");
            } catch (IllegalArgumentException e) {
                // good
            }

            System.out.println("Testing negative destOffset");
            try {
                // Check that negative dstOffset throws an IAE
                UNSAFE.copyMemory(arr, UNSAFE.arrayBaseOffset(arr.getClass()), arr, -1, 16);
                throw new RuntimeException("copyMemory failed to throw IAE for destOffset=-1");
            } catch (IllegalArgumentException e) {
                // good
            }

            System.out.println("Testing reference array");
            try {
                // Check that a reference array destination throws IAE
                UNSAFE.copyMemory(null, buf, new Object[16], UNSAFE.arrayBaseOffset(Object[].class), 16);
                throw new RuntimeException("copyMemory failed to throw IAE");
            } catch (IllegalArgumentException e) {
                // good
            }

            // Check that invalid source & dest pointers throw IAEs (only relevant on 32-bit platforms)
            if (UNSAFE.addressSize() == 4) {
                long invalidPtr = (long)1 << 35; // Pick a random bit in upper 32 bits

                try {
                    // Check that an invalid (not 32-bit clean) source pointer throws IAE
                    UNSAFE.copyMemory(null, invalidPtr, null, buf, 16);
                    throw new RuntimeException("copyMemory failed to throw IAE for srcOffset 0x" +
                                               Long.toHexString(invalidPtr));
                } catch (IllegalArgumentException e) {
                    // good
                }

                try {
                    // Check that an invalid (not 32-bit clean) source pointer throws IAE
                    UNSAFE.copyMemory(null, buf, null, invalidPtr, 16);
                    throw new RuntimeException("copyMemory failed to throw IAE for destOffset 0x" +
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
        CopyMemory cs = new CopyMemory();
        cs.test();
    }
}
