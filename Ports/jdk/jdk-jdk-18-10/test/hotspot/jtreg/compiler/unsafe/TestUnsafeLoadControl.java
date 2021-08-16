/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8077504
 * @summary Unsafe load can loose control dependency and cause crash
 * @modules java.base/jdk.internal.misc:+open
 *
 * @run main/othervm -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
 *                   compiler.unsafe.TestUnsafeLoadControl
 */

package compiler.unsafe;

import jdk.internal.misc.Unsafe;

import java.lang.reflect.Field;

public class TestUnsafeLoadControl {

    private static final Unsafe UNSAFE;

    static {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            UNSAFE = (Unsafe) unsafeField.get(null);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    static int val;
    static void test1(int[] a, boolean[] flags, boolean flag, long j) {
        for (int i = 0; i < 10; i++) {
            if (flags[i]) {
                if (flag) {
                    long address = (j << 2) + UNSAFE.ARRAY_INT_BASE_OFFSET;
                    int v = UNSAFE.getInt(a, address);
                    val = v;
                }
            }
        }
    }

    static int test2(int[] a, boolean[] flags, boolean flag, long j) {
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            if (flags[i]) {
                if (flag) {
                    long address = (j << 2) + UNSAFE.ARRAY_INT_BASE_OFFSET;
                    int v = UNSAFE.getInt(a, address);
                    if (v == 0) {
                        sum++;
                    }
                }
            }
        }
        return sum;
    }

    static public void main(String[] args) {
        boolean[] flags = new boolean[10];
        for (int i = 0; i < flags.length; i++) {
            flags[i] = true;
        }
        int[] array = new int[10];
        for (int i = 0; i < 20000; i++) {
            test1(array, flags, true, 0);
        }
        for (int i = 0; i < flags.length; i++) {
            flags[i] = false;
        }
        test1(array, flags, true, Long.MAX_VALUE/4);

        for (int i = 0; i < flags.length; i++) {
            flags[i] = true;
        }
        for (int i = 0; i < 20000; i++) {
            test2(array, flags, true, 0);
        }
        for (int i = 0; i < flags.length; i++) {
            flags[i] = false;
        }
        test2(array, flags, true, Long.MAX_VALUE/4);
    }
}
