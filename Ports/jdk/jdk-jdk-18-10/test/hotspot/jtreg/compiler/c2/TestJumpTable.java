/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8229855 8238812
 * @summary Test jump table with key value that gets out of bounds after loop unrolling.
 * @requires vm.compiler2.enabled
 *
 * @run main/othervm -XX:CompileCommand=dontinline,compiler.c2.TestJumpTable::test*
 *                   -Xbatch -XX:+UnlockDiagnosticVMOptions -XX:-TieredCompilation -XX:-UseSwitchProfiling
 *                   compiler.c2.TestJumpTable
 * @run main/othervm -XX:CompileCommand=dontinline,compiler.c2.TestJumpTable::test*
 *                   -Xbatch -XX:-TieredCompilation -XX:-UseOnStackReplacement
 *                   compiler.c2.TestJumpTable
 * @run main/othervm -XX:CompileCommand=dontinline,compiler.c2.TestJumpTable::test*
 *                   -Xbatch -XX:+UnlockDiagnosticVMOptions -XX:-TieredCompilation -XX:+StressIGVN
 *                   compiler.c2.TestJumpTable
 */

package compiler.c2;

public class TestJumpTable {

    public static int test0() {
        int res = 0;
        for (int i = 10; i < 50; ++i) {
            switch (i * 5) {
                case 15:
                case 25:
                case 40:
                case 101:
                    return 42;
                case 45:
                case 51:
                case 60:
                    res++;
                    break;
            }
        }
        return res;
    }

    static int field;

    // Original (slightly simplified) fuzzer generated test
    public static void test1() {
        int i4, i5 = 99, i6, i9 = 89;
        for (i4 = 12; i4 < 365; i4++) {
            for (i6 = 5; i6 > 1; i6--) {
                switch ((i6 * 5) + 11) {
                case 13:
                case 19:
                case 26:
                case 31:
                case 35:
                case 41:
                case 43:
                case 61:
                case 71:
                case 83:
                case 314:
                    i9 = i5;
                    break;
                }
            }
        }
    }

    // This generates the following subgraph:
    /*
        // i: -10..4
        if ((i+min_jint) u<= max_jint) {    <- This is always true but not folded by C2
            ...
        } else {
            ...
            CastII(i-5, 0..45)              <- Replaced by TOP because i-5 range is -15..-1 but still considered reachable by C2 although it is dead code
            ...
        }
    */
    public static void test2() {
        for (int i = 5; i > -10; i--) {
            switch (i) {
            case 0:
            case 4:
            case 10:
            case 20:
            case 30:
            case 40:
            case 50:
            case 100:
                field = 42;
                break;
            }
        }
    }

    // This generates the following subgraph:
    /*
        // i: -20..0
        if (i != 0) {
            // i: -20..-1
            if (i < 0) {                    <- This is always true but not folded by C2
                // Fall through
            } else {
                ...
                CastII(i-1, 0..4)           <- Replaced by TOP because i-1 range is -21..-1 but still considered reachable by C2 although it is dead code
                ...
            }
        } else {
            StoreI                          <- Due to this additional store on, IfNode::has_shared_region returns false and the fold compares optimization does not kick in
        }
    */
    public static void test3() {
        for (int i = 5; i > -20; i -= 5) {
            switch (i) {
            case 0:
            case 10:
            case 20:
            case 30:
            case 40:
            case 50:
            case 60:
            case 100:
                field = 42;
                break;
            }
        }
    }

    // This generates the following subgraph:
    /*
        // i: -20..0
        if (i != 0) {
            // i: -20..-1
            if (i u< 101) {                 <- This is always false but not folded by C2 because CmpU is not handled
                CastII(i-1, 0..49)          <- Replaced by TOP because i-1 range is -21..-1 but still considered reachable by C2 although it is dead code
            } else {
                ...
            }
        } else {
            ...
        }
    */
    public static void test4() {
        int local = 0;
        for (int i = 5; i > -20; i -= 5) {
            switch (i) {
            case 0:
            case 10:
            case 20:
            case 30:
            case 40:
            case 50:
            case 100:
                local = 42;
                break;
            }
        }
    }

    // This generates the following subgraph:
    /*
        // i: 0..20
        if (i != 20) {
            // i: 0..19
            if ((i-20) u< 281) {            <- This is always false but not folded by C2 because the two ifs compare different values
                CastII(i-21, 0..49)         <- Replaced by TOP because i-21 range is -21..-1 but still considered reachable by C2 although it is dead code
            } else {
                ...
            }
        } else {
            ...
        }
    */
    public static void test5() {
        int local;
        for (int i = 25; i > 0; i -= 5) {
            switch (i) {
            case 20:
            case 30:
            case 40:
            case 50:
            case 60:
            case 70:
            case 300:
                local = 42;
                break;
            }
        }
    }

    // This generates the following subgraph:
    /*
        // i: 0..20
        if ((i+10) != 30) {
            // i: 0..19
            if ((i-20) u< 271) {            <- This is always false but not folded by C2 because the two ifs compare different values
                CastII(i-21, 0..4)          <- Replaced by TOP because i-21 range is -21..-1 but still considered reachable by C2 although it is dead code
            } else {
                ...
            }
        } else {
            ...
        }
    */
    public static void test6() {
        int local;
        for (int i = 25; i > 0; i -= 5) {
            switch (i + 10) {
            case 30:
            case 40:
            case 50:
            case 60:
            case 70:
            case 80:
            case 300:
                local = 42;
                break;
            }
        }
    }

    public static void main(String[] args) {
        for (int i = 0; i < 50_000; ++i) {
            test0();
            test1();
            test2();
            test3();
            test4();
            test5();
            test6();
        }
    }
}
