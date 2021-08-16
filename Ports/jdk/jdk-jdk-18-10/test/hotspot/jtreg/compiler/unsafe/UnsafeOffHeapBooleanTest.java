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

/*
 * @test
 * @bug 8161720
 * @modules java.base/jdk.internal.misc
 * @run main/othervm -Xint UnsafeOffHeapBooleanTest 1
 * @run main/othervm -XX:+TieredCompilation -XX:TieredStopAtLevel=3 -Xbatch  UnsafeOffHeapBooleanTest 20000
 * @run main/othervm -XX:-TieredCompilation -Xbatch UnsafeOffHeapBooleanTest 20000
 */


import java.lang.reflect.Field;
import jdk.internal.misc.Unsafe;

public class UnsafeOffHeapBooleanTest {
    static boolean bool0 = false, bool1 = false, result = false;
    static Unsafe UNSAFE = Unsafe.getUnsafe();
    static long offHeapMemory;

    public static void test() {
        // Write two bytes to the off-heap memory location, both
        // bytes correspond to the boolean value 'true'.
        UNSAFE.putShort(null, offHeapMemory, (short)0x0204);

        // Read two bytes from the storage allocated above (as booleans).
        bool0 = UNSAFE.getBoolean(null, offHeapMemory + 0);
        bool1 = UNSAFE.getBoolean(null, offHeapMemory + 1);
        result = bool0 & bool1;
    }

    public static void main(String args[]) {
        System.out.println("### Test started");

        if (args.length != 1) {
            throw new RuntimeException("### Test failure: test called with incorrect number of arguments");
        }

        // Allocate two bytes of storage.
        offHeapMemory = UNSAFE.allocateMemory(2);

        try {
            for (int i = 0; i < Integer.parseInt(args[0]); i++) {
                test();
            }

            // Check if the two 'true' boolean values were normalized
            // (i.e., reduced from the range 1...255 to 1).
            if (!bool0 || !bool1 || !result) {
                System.out.println("Some of the results below are wrong");
                System.out.println("bool0 is: " + bool0);
                System.out.println("bool1 is: " + bool1);
                System.out.println("bool0 & bool1 is: " + result);
                System.out.println("===================================");
                throw new RuntimeException("### Test failed");
            } else {
                System.out.println("Test generated correct results");
                System.out.println("bool0 is: " + bool0);
                System.out.println("bool1 is: " + bool1);
                System.out.println("bool0 & bool1 is: " + result);
                System.out.println("===================================");
            }
        } catch (NumberFormatException e) {
            throw new RuntimeException("### Test failure: test called with incorrectly formatted parameter");
        }

        UNSAFE.freeMemory(offHeapMemory);

        System.out.println("### Test passed");
    }
}
