/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6931567
 * @summary JIT Error (on class file compiled with eclipse) on JVM x64 (but not on x32!).
 *
 * @run main compiler.c2.Test6931567
 */

package compiler.c2;

// Should be compiled with javac from JDK1.3 to get bytecode which shows the problem.
public class Test6931567 {

    public static void main(final String[] args) {
        booleanInvert(Integer.MAX_VALUE);
        booleanInvert(Integer.MAX_VALUE - 1);
    }

    private static void booleanInvert(final int max) {
        boolean test1 = false;
        boolean test2 = false;

        for (int i = 0; i < max; i++) {
            test1 = !test1;
        }

        for (int i = 0; i < max; i++) {
            test2 ^= true;
        }

        if (test1 != test2) {
            System.out.println("ERROR: Boolean invert\n\ttest1=" + test1
                    + "\n\ttest2=" + test2);
            System.exit(97);
        } else {
            System.out.println("Passed!");
        }
    }
}

