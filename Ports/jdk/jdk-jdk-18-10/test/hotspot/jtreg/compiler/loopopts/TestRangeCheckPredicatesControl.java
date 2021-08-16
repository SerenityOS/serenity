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

/*
 * @test
 * @key stress randomness
 * @requires vm.gc.Z
 * @bug 8237859
 * @summary A LoadP node has a wrong control input (too early) which results in an out-of-bounds read of an object array with ZGC.
 *
 * @run main/othervm -XX:+UseZGC compiler.loopopts.TestRangeCheckPredicatesControl
 * @run main/othervm -XX:+UseZGC -XX:+UnlockDiagnosticVMOptions -XX:+IgnoreUnrecognizedVMOptions -XX:+StressGCM compiler.loopopts.TestRangeCheckPredicatesControl
 */

package compiler.loopopts;

public class TestRangeCheckPredicatesControl {
    static Wrapper w1 = new Wrapper();
    static Wrapper w2 = new Wrapper();
    static Wrapper w3 = new Wrapper();

    public static void main(String[] args) {
        for (int x = 0; x < 10000000; x++) {
            test(x % 2 == 0);
            test2(x % 2 == 0, x % 3 == 0);
            test3(x % 2 == 0);
            test4(x % 2 == 0);
        }
    }

    private static class Wrapper {
        long longs;
        int a;
        public void maybeMaskBits(boolean b) {
            if (b) {
                longs &= 0x1F1F1F1F;
            }
        }

        public void maybeMaskBits2(boolean b, boolean c) {
            if (b) {
                longs &= 0x1F1F1F1F;
            }
            if (c) {
                a += 344;
            }
        }
    }

    private static void test(boolean flag) {
        Wrapper[] wrappers_array;
        if (flag) {
            wrappers_array = new Wrapper[] {w1, w2};
        } else {
            wrappers_array = new Wrapper[] {w1, w2, w3};
        }

        // This loop is first unswitched and then pre/main/post loops are created for both unswitched loops.
        // Both loops are unrolled once (two iterations in main loop, one in pre loop). As a result, the main
        // loop contains an access of wrappers_array[1] and wrappers_array[2]. If 'flag' is false, then the
        // main loop is not entered. But the load for wrappers_array[2] is wrongly scheduled before deciding
        // if the main loop is executed or not due to a wrong control input of the corresponding LoadP node.
        // The LoadP node still has a control input from a loop predicate of the original loop to be unswitched.
        // As a consequence, the access wrappers_array[2] is executed regardless of the value of 'flag' resulting
        // in a segfault. This fix addresses the problem of not updating the control inputs for data nodes
        // from a predicate before the pre loop to the main and post loop. The fix for JDK-8240227 updates the
        // control dependencies to predicates of the original loop to be unswitched to newly cloned predicates
        // of the unswitched loops.
        for (int i = 0; i < wrappers_array.length; i++) {
            wrappers_array[i].maybeMaskBits(flag);
        }
    }

    // This test unswitches two times
    private static void test2(boolean flag, boolean flag2) {
        Wrapper[] wrappers_array;
        Wrapper[] wrappers_array2;
        if (flag) {
            wrappers_array = new Wrapper[] {w1, w2};
            wrappers_array2 = new Wrapper[] {w1, w2};
        } else {
            wrappers_array = new Wrapper[] {w1, w2, w3};
            wrappers_array2 = new Wrapper[] {w1, w2, w3};
        }

        for (int i = 0; i < wrappers_array.length; i++) {
            wrappers_array[i].maybeMaskBits(flag);
            wrappers_array2[i].maybeMaskBits2(flag, flag2);
        }
    }

    // Test without unswitching but wrong control dependencies for data nodes to predicates before the pre loop.
    // There is no update for the data nodes belonging to main and post loop after pre/main/post loops are created.
    private static void test3(boolean flag) {
        Wrapper[] wrappers_array;
        if (flag) {
            wrappers_array = new Wrapper[] {w1, w2};
        } else {
            wrappers_array = new Wrapper[] {w1, w2, w3};
        }

        for (int i = 0; i < wrappers_array.length; i++) {
            wrappers_array[i].longs &= 0x1F1F1F1F;
        }
    }

    private static void test4(boolean flag) {
        Wrapper[] wrappers_array;
        Wrapper[] wrappers_array2;
        if (flag) {
            wrappers_array = new Wrapper[] {w1, w2};
            wrappers_array2 = new Wrapper[] {w1, w2};
        } else {
            wrappers_array = new Wrapper[] {w1, w2, w3};
            wrappers_array2 = new Wrapper[] {w1, w2, w3};
        }

        for (int i = 0; i < wrappers_array.length; i++) {
            wrappers_array[i].longs &= 0x1F1F1F1F;
            wrappers_array2[i].longs &= 0x1F1F1F1F;
        }
    }
}
