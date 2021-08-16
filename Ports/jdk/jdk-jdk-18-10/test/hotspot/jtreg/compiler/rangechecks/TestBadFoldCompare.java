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

/*
 * @test
 * @bug 8085832
 * @bug 8135069
 * @summary x <= 0 || x > 0 wrongly folded as (x-1) >u -1 and x < 0 || x > -1 wrongly folded as x >u -1
 *
 * @run main/othervm -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
 *                   compiler.rangechecks.TestBadFoldCompare
 */

package compiler.rangechecks;

public class TestBadFoldCompare {

    static boolean test1_taken;

    static void helper1(int i, int a, int b, boolean flag) {
        if (flag) {
            if (i <= a || i > b) {
                test1_taken = true;
            }
        }
    }

    static void test1(int i, boolean flag) {
        helper1(i, 0, 0, flag);
    }

    static boolean test2_taken;

    static void helper2(int i, int a, int b, boolean flag) {
        if (flag) {
            if (i > b || i <= a) {
                test2_taken = true;
            }
        }
    }

    static void test2(int i, boolean flag) {
        helper2(i, 0, 0, flag);
    }

    static boolean test3_taken;

    static void helper3(int i, int a, int b, boolean flag) {
        if (flag) {
            if (i < a || i > b - 1) {
                test3_taken = true;
            }
        }
    }

    static void test3(int i, boolean flag) {
        helper3(i, 0, 0, flag);
    }

    static boolean test4_taken;

    static void helper4(int i, int a, int b, boolean flag) {
        if (flag) {
            if (i > b - 1 || i < a) {
                test4_taken = true;
            }
        }
    }

    static void test4(int i, boolean flag) {
        helper4(i, 0, 0, flag);
    }

    static public void main(String[] args) {
        boolean success = true;

        for (int i = 0; i < 20000; i++) {
            helper1(5, 0,  10, (i%2)==0);
            helper1(-1, 0,  10, (i%2)==0);
            helper1(15, 0,  10, (i%2)==0);
            test1(0, false);
        }
        test1_taken = false;
        test1(0, true);
        if (!test1_taken) {
            System.out.println("Test1 failed");
            success = false;
        }

        for (int i = 0; i < 20000; i++) {
            helper2(5, 0,  10, (i%2)==0);
            helper2(-1, 0,  10, (i%2)==0);
            helper2(15, 0,  10, (i%2)==0);
            test2(0, false);
        }
        test2_taken = false;
        test2(0, true);

        if (!test2_taken) {
            System.out.println("Test2 failed");
            success = false;
        }

        for (int i = 0; i < 20000; i++) {
            helper3(5, 0,  10, (i%2)==0);
            helper3(-1, 0,  10, (i%2)==0);
            helper3(15, 0,  10, (i%2)==0);
            test3(0, false);
        }
        test3_taken = false;
        test3(0, true);

        if (!test3_taken) {
            System.out.println("Test3 failed");
            success = false;
        }

        for (int i = 0; i < 20000; i++) {
            helper4(5, 0,  10, (i%2)==0);
            helper4(-1, 0,  10, (i%2)==0);
            helper4(15, 0,  10, (i%2)==0);
            test4(0, false);
        }
        test4_taken = false;
        test4(0, true);

        if (!test4_taken) {
            System.out.println("Test4 failed");
            success = false;
        }

        if (!success) {
            throw new RuntimeException("Some tests failed");
        }
    }
}
