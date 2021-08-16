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
 * @bug 8251458
 * @summary Test int range overflow of MultiBranchData counter.
 * @run main/othervm -XX:CompileCommand=dontinline,compiler.profiling.TestMultiBranchDataOverflow::test
 *                   -Xbatch -XX:Tier4BackEdgeThreshold=2147483647
 *                   compiler.profiling.TestMultiBranchDataOverflow
 */

package compiler.profiling;

public class TestMultiBranchDataOverflow {

    public static int test(int val, long max) {
        int res = 0;
        for (long l = 0; l < max; ++l) {
            switch (val) {
            case 0:
                return 0;
            case 42:
                res++;
                break;
            }
        }
        return res;
    }

    public static void main(String[] args) {
        // Warmup to generate profile information that has a MultiBranchData
        // counter > Integer.MAX_VALUE for the i == 42 lookupswitch branch.
        long max = Integer.MAX_VALUE + 100_000L;
        test(42, max);

        // Trigger C2 compilation
        for (int i = 0; i < 10_000; ++i) {
            test(42, 1);
        }
    }
}
