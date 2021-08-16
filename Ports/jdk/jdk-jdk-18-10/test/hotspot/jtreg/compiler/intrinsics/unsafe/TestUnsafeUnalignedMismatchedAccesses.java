/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8136473
 * @summary Mismatched stores on same slice possible with Unsafe.Put*Unaligned methods
 * @modules java.base/jdk.internal.misc:+open
 *
 * @run main/othervm -XX:-UseOnStackReplacement -XX:-BackgroundCompilation
 *      compiler.intrinsics.unsafe.TestUnsafeUnalignedMismatchedAccesses
 * @run main/othervm -XX:-UseOnStackReplacement -XX:-BackgroundCompilation
 *      -XX:+UnlockDiagnosticVMOptions -XX:-UseUnalignedAccesses
 *      compiler.intrinsics.unsafe.TestUnsafeUnalignedMismatchedAccesses
 */

package compiler.intrinsics.unsafe;

import jdk.internal.misc.Unsafe;

import java.lang.reflect.Field;

public class TestUnsafeUnalignedMismatchedAccesses {

    private static final Unsafe UNSAFE;

    static {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            UNSAFE = (Unsafe) unsafeField.get(null);
        }
        catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    static void test1(byte[] array) {
        array[0] = 0;
        UNSAFE.putIntUnaligned(array, UNSAFE.ARRAY_BYTE_BASE_OFFSET, 0);
        array[0] = 0;
    }

    static void test2(byte[] array) {
        array[0] = 0;
        UNSAFE.putIntUnaligned(array, UNSAFE.ARRAY_BYTE_BASE_OFFSET+1, 0);
        array[0] = 0;
    }

    static void test3(byte[] array) {
        array[0] = 0;
        UNSAFE.putIntUnaligned(array, UNSAFE.ARRAY_BYTE_BASE_OFFSET+2, 0);
        array[0] = 0;
    }

    static void test4(byte[] array) {
        array[0] = 0;
        UNSAFE.putIntUnaligned(array, UNSAFE.ARRAY_BYTE_BASE_OFFSET+3, 0);
        array[0] = 0;
    }

    static void test5(byte[] array) {
        array[0] = 0;
        UNSAFE.putInt(array, UNSAFE.ARRAY_BYTE_BASE_OFFSET, 0);
        array[0] = 0;
    }

    // unaligned access and non escaping allocation
    static void test6() {
        byte[] array = new byte[10];
        UNSAFE.putIntUnaligned(array, UNSAFE.ARRAY_BYTE_BASE_OFFSET+1, -1);
        array[0] = 0;
    }

    // unaligned access and non escaping allocation
    static int test7() {
        byte[] array = new byte[10];
        UNSAFE.putIntUnaligned(array, UNSAFE.ARRAY_BYTE_BASE_OFFSET+1, -1);
        array[0] = 0;
        array[2] = 0;
        return array[0] + array[1] + array[2] + array[3] + array[4];
    }

    // unaligned access with vectorization
    static void test8(int[] src1, int[] src2, int[] dst) {
        for (int i = 0; i < dst.length-1; i++) {
            int res = src1[i] + src2[i];
            UNSAFE.putIntUnaligned(dst, UNSAFE.ARRAY_INT_BASE_OFFSET + i*4+1, res);
        }
    }

    static public void main(String[] args) throws Exception {
        byte[] byte_array = new byte[100];
        int[] int_array = new int[100];
        Object[] obj_array = new Object[100];
        TestUnsafeUnalignedMismatchedAccesses test = new TestUnsafeUnalignedMismatchedAccesses();
        for (int i = 0; i < 20000; i++) {
            test1(byte_array);
            test2(byte_array);
            test3(byte_array);
            test4(byte_array);
            test5(byte_array);
            test6();
            test7();
            test8(int_array, int_array, int_array);
        }
    }
}
