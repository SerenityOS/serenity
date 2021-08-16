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

/**
 * @test
 * @bug 8078426
 * @summary split if finds predicates on several incoming paths when unswitched's loops are optimized out
 *
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:-UseOnStackReplacement
 *                   -XX:-BackgroundCompilation -XX:-UseCompressedOops
 *                   compiler.loopopts.TestSplitIfUnswitchedLoopsEliminated
 */

package compiler.loopopts;

public class TestSplitIfUnswitchedLoopsEliminated {

    static class A {
        int f;
    }

    static A aa = new A();
    static A aaa = new A();

    static int test_helper(int stop, boolean unswitch) {
        A a = null;
        for (int i = 3; i < 10; i++) {
            if (unswitch) {
                a = null;
            } else {
                a = aa;
                int v = a.f;
            }
        }
        if (stop != 4) {
            a = aaa;
        }
        if (a != null) {
            return a.f;
        }
        return 0;
    }

    static int test(boolean unswitch) {
        int stop = 1;
        for (; stop < 3; stop *= 4) {
        }
        return test_helper(stop, unswitch);
    }

    public static void main(String[] args) {
        for (int i = 0; i < 20000; i++) {
            test_helper(10, i%2 == 0);
            test(i%2 == 0);
        }
    }
}
