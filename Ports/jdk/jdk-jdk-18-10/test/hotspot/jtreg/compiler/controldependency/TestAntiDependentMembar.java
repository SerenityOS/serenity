/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8228772
 * @summary Test correct insertion of anti-dependencies if load is already control dependent on membar.
 * @run main/othervm -Xbatch -XX:-TieredCompilation
 *                   -XX:CompileCommand=inline,compiler.controldependency.TestAntiDependentMembar::hitSearchLimit
 *                   compiler.controldependency.TestAntiDependentMembar
 * @run main/othervm -Xbatch -XX:-TieredCompilation
 *                   -XX:CompileCommand=inline,compiler.controldependency.TestAntiDependentMembar::hitSearchLimit
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+IgnoreUnrecognizedVMOptions -XX:DominatorSearchLimit=0
 *                   compiler.controldependency.TestAntiDependentMembar
 */

package compiler.controldependency;

public class TestAntiDependentMembar {

    static volatile int step1 = 0;
    static volatile int step2 = 0;

    public static int test1(int count, int b1, int b2) {
        int[] result = {0};

        // Complex control flow to generate Region with 4 paths and therefore bail out of Node::dominates
        if (b1 == 0) {
            count += 1;
        } else if (b1 == 1) {
            if (b2 == 1) {
                count += 2;
            }
        }

        for (int i = 0; i < count; ++i) {
            // Volatile field write adds a membar
            step1 = i;
            // Load that is dependent on the membar
            result[0] += count;
        }
        return result[0];
    }

    // Same as test1 but bailing out of Node::dominates due to hitting DominatorSearchLimit
    public static int test2(int count) {
        int[] result = {0};

        // Large method with regions to hit the DominatorSearchLimit
        hitSearchLimit();

        for (int i = 0; i < count; ++i) {
            step1 = i;
            result[0] += count;
        }
        return result[0];
    }

    // Same as test2 but with multiple membars before the load
    public static int test3(int count) {
        int[] result = {0};

        hitSearchLimit();

        for (int i = 0; i < count; ++i) {
            step1 = i;
            step2 = i;
            step1 = i;
            step2 = i;
            result[0] += count;
        }
        return result[0];
    }

    public static void main(String[] args) {
        for (int i = 0; i < 50_000; ++i) {
          test1(10, 0, 0);
          test1(10, 1, 1);
          test1(10, 1, 0);
          test1(10, 0, 1);
          test2(10);
          test3(10);
        }
    }

    public static void hitSearchLimit() {
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
        step1++;
        step2++;
    }
}
