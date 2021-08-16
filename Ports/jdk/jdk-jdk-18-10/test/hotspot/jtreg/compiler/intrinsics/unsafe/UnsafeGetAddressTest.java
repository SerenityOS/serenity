/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6653795
 * @summary C2 intrinsic for Unsafe.getAddress performs pointer sign extension on 32-bit systems
 * @modules java.base/jdk.internal.misc:+open
 *
 * @run main compiler.intrinsics.unsafe.UnsafeGetAddressTest
 */

package compiler.intrinsics.unsafe;

import jdk.internal.misc.Unsafe;

import java.lang.reflect.Field;

public class UnsafeGetAddressTest {
    private static Unsafe unsafe;

    public static void main(String[] args) throws Exception {
        Class c = UnsafeGetAddressTest.class.getClassLoader().loadClass("jdk.internal.misc.Unsafe");
        Field f = c.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        unsafe = (Unsafe)f.get(c);

        long address = unsafe.allocateMemory(unsafe.addressSize());
        unsafe.putAddress(address, 0x0000000080000000L);
        // from jdk.internal.misc.Unsafe.getAddress' documentation:
        // "If the native pointer is less than 64 bits wide, it is
        // extended as an unsigned number to a Java long."
        result = unsafe.getAddress(address);
        System.out.printf("1: was 0x%x, expected 0x%x\n", result,
                0x0000000080000000L);
        for (int i = 0; i < 1000000; i++) {
            result = unsafe.getAddress(address);
        }

        // The code has got compiled, check the result now
        System.out.printf("2: was 0x%x, expected 0x%x\n", result,
                0x0000000080000000L);
        if (result != 0x0000000080000000L) {
            System.out.println("Test Failed");
            System.exit(97);
        } else {
            System.out.println("Test Passed");
        }
    }
    static volatile long result;
}

