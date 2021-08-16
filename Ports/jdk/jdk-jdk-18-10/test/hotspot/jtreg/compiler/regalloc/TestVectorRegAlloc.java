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
 * @bug 8131969
 * @summary assert in register allocation code when vector Phi for a loop is
 * processed because code assumes all inputs already processed
 *
 * @run main/othervm -Xbatch compiler.regalloc.TestVectorRegAlloc
 */

package compiler.regalloc;

public class TestVectorRegAlloc {

    static int test_helper_i;
    static boolean test_helper() {
        test_helper_i++;
        return (test_helper_i & 7) != 0;
    }

    static void test(double[] src, double[] dst, boolean flag) {
        double j = 0.0;
        while(test_helper()) {
            for (int i = 0; i < src.length; i++) {
                dst[i] = src[i] + j;
            }
            // Loop will be unswitched and ReplicateD of zero will be
            // split through the Phi of outer loop
            for (int i = 0; i < src.length; i++) {
                double k;
                if (flag) {
                    k = j;
                } else {
                    k = 0;
                }
                dst[i] = src[i] + k;
            }
            j++;
        }
    }

    static public void main(String[] args) {
        double[] src = new double[10];
        double[] dst = new double[10];
        for (int i = 0; i < 20000; i++) {
            test(src, dst, (i % 2) == 0);
        }
    }
}
