/*
 * Copyright (c) 2017, Red Hat, Inc. All rights reserved.
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
 * @bug 8191887
 * @summary loop cloning misses support for Opaque4 node
 * @modules java.base/jdk.internal.misc:+open
 *
 * @run main/othervm -XX:-BackgroundCompilation TestLoopUnswitching
 *
 */

import jdk.internal.misc.Unsafe;
import java.lang.reflect.Field;
import java.util.Arrays;

public class TestLoopUnswitching {

    static final jdk.internal.misc.Unsafe UNSAFE = Unsafe.getUnsafe();
    static final long F_OFFSET;

    static class A {
        int f;
        A(int f) {
            this.f = f;
        }
    }

    static {
        try {
            Field fField = A.class.getDeclaredField("f");
            F_OFFSET = UNSAFE.objectFieldOffset(fField);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static int test1(A[] arr, boolean flag1, boolean flag2) {
        int res = 0;
        for (int i = 0; i < 10; i++) {
            A a = arr[i];
            if (flag1) { // triggers unswitching
                res += UNSAFE.getInt(a, F_OFFSET);
            }
            if (flag2) {
                // Opaque4 node here is in the loop but If is out of the loop
                res += UNSAFE.getInt(a, F_OFFSET);
                break;
            }
            res += UNSAFE.getInt(a, F_OFFSET);
        }
        return res;
    }

    static int test2(A[] arr, boolean flag1, boolean flag2) {
        int res = 0;
        for (int i = 0; i < 10; i++) {
            A a = arr[i];
            if (flag1) { // triggers unswitching
                res += UNSAFE.getInt(a, F_OFFSET);
            }
            if (flag2) {
                // Opaque4 node here is out of the loop but Bool is in the the loop
                res += UNSAFE.getInt(a, F_OFFSET);
                break;
            }
            res += a.f;
        }
        return res;
    }

    static public void main(String[] args) {
        A[] arr = new A[1000];
        Arrays.fill(arr, new A(0x42));
        for (int i = 0; i < 20000; i++) {
            test1(arr, (i%2) == 0, (i%2) == 0);
            test2(arr, (i%2) == 0, (i%2) == 0);
        }
    }

}
