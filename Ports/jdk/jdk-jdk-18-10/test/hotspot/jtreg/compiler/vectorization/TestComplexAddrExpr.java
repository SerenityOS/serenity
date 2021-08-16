/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8249749
 * @summary Test vectorization for complex address expressions
 * @requires vm.compMode != "Xint"
 * @run main compiler.vectorization.TestComplexAddrExpr test1
 * @run main compiler.vectorization.TestComplexAddrExpr test2
 * @run main compiler.vectorization.TestComplexAddrExpr test3
 * @run main compiler.vectorization.TestComplexAddrExpr test4
 * @run main compiler.vectorization.TestComplexAddrExpr test5
 * @run main compiler.vectorization.TestComplexAddrExpr test6
 */

package compiler.vectorization;

import java.util.stream.IntStream;

public class TestComplexAddrExpr {

    static final int n = 1024;

    static void test1(int[] data) {
        IntStream.range(0, n).forEach(j -> {
            for (int i = 0; i < n; i++) {
                data[j * n + i]++;
            }
        });
    }

    static void test2(int[] data) {
        IntStream.range(0, n).forEach(j -> {
            for (int i = n - 1; i >= 0; i--) {
                data[j * n + i]--;
            }
        });
    }

    static void test3(int[] data) {
        IntStream.range(0, n).forEach(j -> {
            for (int i = 1 - n; i < 1; i++) {
                data[j * n - i]++;
            }
        });
    }

    static void test4(int[] data) {
        IntStream.range(0, n).forEach(j -> {
            for (int i = 0; i >= 1 - n; i--) {
                data[j * n - i]--;
            }
        });
    }

    static void test5(int[] data) {
        IntStream.range(0, n).forEach(j -> {
            for (int i = 0; i < n/2; i++) {
                data[j * n + (i << 1)]++;
            }
            for (int i = 0; i < n/2; i++) {
                data[j * n + i * 2 + 1]++;
            }
        });
    }

    static void test6(int[] data) {
        IntStream.range(0, n).forEach(j -> {
            for (int i = (n - 1)/2; i >= 0; i--) {
                data[j * n + (i << 1)]--;
            }
            for (int i = (n - 1)/2; i >= 0; i--) {
                data[j * n + i * 2 + 1]--;
            }
        });
    }

    static void verify(int[] data, int k) {
        for (int i = 0; i < n * n; i++) {
            if (data[i] != k) {
                throw new RuntimeException(" Invalid result: data[" + i + "]: " + data[i] + " != " + k);
            }
        }
    }

    public static void main(String[] args) {
        // int n = 1024;
        int[] data = new int[n * n];

        if (args.length == 0) {
            throw new RuntimeException(" Missing test name: test1, test2, test3, test4, test5, test6");
        }

        if (args[0].equals("test1")) {
            System.out.println(" Run test1 ...");
            for (int k = 0; k < n; k++) {
                test1(data);
            }
            verify(data, n);
            System.out.println(" Finished test1.");
        }

        if (args[0].equals("test2")) {
            System.out.println(" Run test2 ...");
            for (int k = 0; k < n; k++) {
                test2(data);
            }
            verify(data, -n);
            System.out.println(" Finished test2.");
        }

        if (args[0].equals("test3")) {
            System.out.println(" Run test3 ...");
            for (int k = 0; k < n; k++) {
                test3(data);
            }
            verify(data, n);
            System.out.println(" Finished test3.");
        }

        if (args[0].equals("test4")) {
            System.out.println(" Run test4 ...");
            for (int k = 0; k < n; k++) {
                test4(data);
            }
            verify(data, -n);
            System.out.println(" Finished test4.");
        }

        if (args[0].equals("test5")) {
            System.out.println(" Run test5 ...");
            for (int k = 0; k < n; k++) {
                test5(data);
            }
            verify(data, n);
            System.out.println(" Finished test5.");
        }

        if (args[0].equals("test6")) {
            System.out.println(" Run test6 ...");
            for (int k = 0; k < n; k++) {
                test6(data);
            }
            verify(data, -n);
            System.out.println(" Finished test6.");
        }
    }
}
