/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8261147
 * @summary Cloned node in AddNode::Ideal is no longer a reduction but is still marked as such leading to wrong vectorization.
 * @run main/othervm -Xcomp -XX:CompileCommand=compileonly,compiler.loopopts.superword.TestWronglyMarkedReduction::*
 *                   compiler.loopopts.superword.TestWronglyMarkedReduction
 */
package compiler.loopopts.superword;

public class TestWronglyMarkedReduction {
    public static long b = 0;

    public static void main(String[] p) {
        TestWronglyMarkedReduction u = new TestWronglyMarkedReduction();
        for (int i = 0; i < 1000; i++) {
            b = 0;
            test();
        }
    }

    public static void test() {
        long r[] = new long[20];
        for (int q = 0; q < 12; ++q) {
            for (int i = 1; i < 6; ++i) {
                r[i + 1] += b;
            }
            b += 2;
        }
        check(r);
    }

    public static void check(long[] a) {
        for (int j = 0; j < 20; j++) {
            if (j >= 2 && j <= 6) {
                if (a[j] != 132) {
                    throw new RuntimeException("expected 132 at index " + j + " but got " + a[j]);
                }
            } else if (a[j] != 0) {
                throw new RuntimeException("expected 0 at index " + j + " but got " + a[j]);
            }
        }
    }
}

