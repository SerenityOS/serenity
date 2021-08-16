/*
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

/*
 * @test
 * @bug 8245714
 * @requires vm.compiler2.enabled
 * @summary "Bad graph detected in build_loop_late" when loads are pinned on loop limit check uncommon branch
 *
 * @run main/othervm -XX:-BackgroundCompilation -XX:ArrayCopyLoadStoreMaxElem=0 TestBadControlLoopLimitCheck
 */

public class TestBadControlLoopLimitCheck {
    public static void main(String[] args) {
        int[] int_array = {0, 0};
        A[] obj_array = {new A(), new A()};
        for (int i = 0; i < 20_000; i++) {
            test1(int_array, 0, 10, false);
            test_helper(42);
            test2(obj_array, 0, 10, false);
        }
    }

    private static int test1(int[] a, int start, int stop, boolean flag) {
        int[] b = new int[2]; // non escaping allocation
        System.arraycopy(a, 0, b, 0, 2); // optimized out
        int v = 1;
        int j = 0;
        for (; j < 10; j++);
        int inc = test_helper(j); // delay transformation to counted loop
        // loop limit check here has loads pinned on unc branch
        for (int i = start; i < stop; i += inc) {
            v *= 2;
        }
        if (flag) {
            v += b[0] + b[1];
        }
        return v;
    }

    private static int test2(A[] a, int start, int stop, boolean flag) {
        A[] b = new A[2]; // non escaping allocation
        System.arraycopy(a, 0, b, 0, 2); // optimized out
        int v = 1;
        int j = 0;
        for (; j < 10; j++);
        int inc = test_helper(j); // delay transformation to counted loop
        // loop limit check here has loads pinned on unc branch
        for (int i = start; i < stop; i += inc) {
            v *= 2;
        }
        if (flag) {
            v += b[0].f + b[1].f;
        }
        return v;
    }

    static class A {
        int f;
    }

    static int test_helper(int j) {
        return j == 10 ? 10 : 1;
    }
}
