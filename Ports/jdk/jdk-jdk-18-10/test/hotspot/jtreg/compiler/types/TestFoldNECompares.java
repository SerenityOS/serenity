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
 * @bug 8236721
 * @summary Test folding of != integer comparisons.
 * @run main/othervm -XX:CompileCommand=compileonly,compiler.c2.TestFoldNECompares::test*
 *                   -XX:CompileCommand=inline,compiler.c2.TestFoldNECompares::getNarrowInt*
 *                   -Xbatch -XX:-TieredCompilation
 *                   compiler.c2.TestFoldNECompares
 */

package compiler.c2;

public class TestFoldNECompares {

    public static int getNarrowInt(boolean b, int lo, int hi) {
        return b ? lo : hi;
    }

    public static void test1(boolean b) {
        int i = getNarrowInt(b, 42, 142);
        // i: 42..142
        if (i != 42) {
            // i: 43..142
            if (i <= 42) {
                throw new RuntimeException("Should not reach here");
            }
        }
    }

    public static void test2(boolean b) {
        int i = getNarrowInt(b, 42, 142);
        // i: 42..142
        if (i != 42) {
            // i: 43..142
            if (i > 42) {

            } else {
                throw new RuntimeException("Should not reach here");
            }
        }
    }

    public static void test3(boolean b) {
        int i = getNarrowInt(b, 42, 142);
        // i: 42..142
        if (i == 42) {

        } else {
            // i: 43..142
            if (i <= 42) {
                throw new RuntimeException("Should not reach here");
            }
        }
    }

    public static void test4(boolean b) {
        int i = getNarrowInt(b, 42, 142);
        // i: 42..142
        if (i == 42) {

        } else {
            // i: 43..142
            if (i > 42) {

            } else {
                throw new RuntimeException("Should not reach here");
            }
        }
    }

    public static void test5(boolean b) {
        int i = getNarrowInt(b, 42, 142);
        // i: 42..142
        if (i != 142) {
            // i: 42..141
            if (i >= 142) {
                throw new RuntimeException("Should not reach here");
            }
        }
    }

    public static void test6(boolean b) {
        int i = getNarrowInt(b, 42, 142);
        // i: 42..142
        if (i != 142) {
            // i: 42..141
            if (i < 142) {

            } else {
                throw new RuntimeException("Should not reach here");
            }
        }
    }

    public static void test7(boolean b) {
        int i = getNarrowInt(b, 42, 142);
        // i: 42..142
        if (i == 142) {

        } else {
            // i: 42..141
            if (i >= 142) {
                throw new RuntimeException("Should not reach here");
            }
        }
    }

    public static void test8(boolean b) {
        int i = getNarrowInt(b, 42, 142);
        // i: 42..142
        if (i == 142) {

        } else {
            // i: 42..141
            if (i < 142) {

            } else {
                throw new RuntimeException("Should not reach here");
            }
        }
    }

    public static void main(String[] args) {
        for (int i = 0; i < 100_000; ++i) {
            boolean b = ((i % 2) == 0);
            test1(b);
            test2(b);
            test3(b);
            test4(b);
            test5(b);
            test6(b);
            test7(b);
            test8(b);
        }
    }
}
