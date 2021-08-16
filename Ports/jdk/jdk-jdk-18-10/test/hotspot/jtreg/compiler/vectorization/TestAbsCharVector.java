/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, Arm Limited. All rights reserved.
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
 * @bug 8261022
 * @summary Test vectorization of Math.abs() with unsigned type
 * @run main/othervm compiler.vectorization.TestAbsCharVector
 */

package compiler.vectorization;

public class TestAbsCharVector {

    private static int SIZE = 60000;

    public static void main(String args[]) {
        char[] a = new char[SIZE];
        char[] b = new char[SIZE];

        for (int i = 0; i < SIZE; i++) {
            a[i] = b[i] = (char) i;
        }

        for (int i = 0; i < 20000; i++) {
            arrayAbs(a);
        }

        for (int i = 0; i < SIZE; i++) {
            if (a[i] != b[i]) {
                throw new RuntimeException("Broken!");
            }
        }
    }

    private static void arrayAbs(char[] arr) {
        for (int i = 0; i < SIZE; i++) {
            arr[i] = (char) Math.abs(arr[i]);
        }
    }
}

