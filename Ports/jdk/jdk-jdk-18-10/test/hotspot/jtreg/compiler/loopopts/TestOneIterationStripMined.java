/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8208275
 * @summary Test removal of strip mined loop with only one iteration.
 * @run main/othervm -XX:-TieredCompilation -Xcomp
 *                   -XX:CompileCommand=compileonly,compiler.loopopts.TestOneIterationStripMined::test*
 *                   compiler.loopopts.TestOneIterationStripMined
 */

package compiler.loopopts;

public class TestOneIterationStripMined {
    static int iField;
    static volatile Object oField;

    public static void test1(int val) {
        iField = 0; // Do something
        // This loop is strip mined and then removed because it has only one iteration.
        for (int i = 0; i < 1; ++i) {
            // Trigger split-if optimization
            if (val == 0) {
                break;
            }
            val = 0;
        }
    }

    // Same as test1 but with a volatile Object field
    // (will trigger a different failure mode)
    public static void test2(int val) {
        oField = null;
        for (int i = 0; i < 1; ++i) {
            if (val == 0) {
                break;
            }
            val = 0;
        }
    }

    // Same as test1 but without any code outside loop body
    // (will trigger a different failure mode)
    public static void test3(int val) {
        for (int i = 0; i < 1; ++i) {
            if (val == 0) {
                break;
            }
            val = 0;
        }
    }

    public static void main(String[] args) {
        test1(0);
        test2(0);
        test3(0);
    }
}
