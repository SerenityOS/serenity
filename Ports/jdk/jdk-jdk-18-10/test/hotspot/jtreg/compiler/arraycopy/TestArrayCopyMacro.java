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
 * @bug 7173584
 * @summary arraycopy as macro node
 *
 * @run main/othervm -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
 *                   compiler.arraycopy.TestArrayCopyMacro
 */

package compiler.arraycopy;

public class TestArrayCopyMacro {
    static class A {
    }

    // In its own method so profiling reports both branches taken
    static Object m2(Object o1, Object o2, int i) {
        if (i == 4) {
            return o1;
        }
        return o2;
    }

    static Object m1(A[] src, Object dest) {
        int i = 1;

        // won't be optimized out until after parsing
        for (; i < 3; i *= 4) {
        }
        dest = m2(new A[10], dest, i);

        // dest is new array here but C2 picks the "disjoint" stub
        // only if stub to call is decided after parsing
        System.arraycopy(src, 0, dest, 0, 10);
        return dest;
    }

    public static void main(String[] args) {
        A[] array_src = new A[10];

        for (int i = 0; i < array_src.length; i++) {
            array_src[i] = new A();
        }

        for (int i = 0; i < 20000; i++) {
            m2(null, null, 0);
        }

        for (int i = 0; i < 20000; i++) {
            Object[] array_dest = (Object[])m1(array_src, null);

            for (int j = 0; j < array_src.length; j++) {
                if (array_dest[j] != array_src[j]) {
                    throw new RuntimeException("copy failed at index " + j + " src = " + array_src[j] + " dest = " + array_dest[j]);
                }
            }
        }
    }
}
