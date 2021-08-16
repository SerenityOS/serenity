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
 * @bug 8134321
 * @summary Code that capture field values of eliminated allocation at a safepoint when there's an arraycopy behind a Phi is broken
 *
 * @run main/othervm -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
 *                   compiler.arraycopy.TestEliminatedArrayCopyPhi
 */

package compiler.arraycopy;

public class TestEliminatedArrayCopyPhi {

    static int[] escaped;

    static void test(int[] src, boolean flag1, boolean flag2) {
        int[] array = new int[10];
        if (flag1) {
            System.arraycopy(src, 0, array, 0, src.length);
        } else {
        }

        if (flag2) {
            // never taken
            escaped = array;
        }
    }

    public static void main(String[] args) {
        int[] src = new int[10];
        for (int i = 0; i < 20000; i++) {
            test(src, (i % 2) == 0, false);
        }
    }
}
