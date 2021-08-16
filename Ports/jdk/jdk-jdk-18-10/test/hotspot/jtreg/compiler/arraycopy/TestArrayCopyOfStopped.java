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
 * @bug 8074676
 * @summary after guards in Arrays.copyOf() intrinsic, control may become top
 *
 * @run main/othervm -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
 *                   compiler.arraycopy.TestArrayCopyOfStopped
 */

package compiler.arraycopy;

import java.util.Arrays;

public class TestArrayCopyOfStopped {
    static class A {
    }

    static class B {
    }

    static final B[] array_of_bs = new B[10];
    static final A[] array_of_as = new A[10];

    static Object[] m1_helper(Object[] array, boolean flag) {
        if (flag) {
            return Arrays.copyOf(array, 10, A[].class);
        }
        return null;
    }

    static Object[] m1(boolean flag) {
        return m1_helper(array_of_bs, flag);
    }

    public static void main(String[] args) {
        for (int i = 0; i < 20000; i++) {
            m1_helper(array_of_as, (i%2) == 0);
        }

        for (int i = 0; i < 20000; i++) {
            m1(false);
        }
    }
}
