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
 * @bug 8080289 8189067
 * @summary Move stores out of loops if possible
 *
 * @run main/othervm -XX:-UseOnStackReplacement -XX:-BackgroundCompilation
 *      -XX:CompileCommand=dontinline,compiler.loopopts.TestMoveStoresOutOfLoops::test*
 *      compiler.loopopts.TestMoveStoresOutOfLoops
 */

package compiler.loopopts;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.function.Function;

public class TestMoveStoresOutOfLoops {

    private static long[] array = new long[10];
    private static long[] array2 = new long[10];
    private static boolean[] array3 = new boolean[1000];
    private static int[] array4 = new int[1000];
    private static byte[] byte_array = new byte[10];

    // Array store should be moved out of the loop, value stored
    // should be 999, the loop should be eliminated
    static void test_after_1(int idx) {
        for (int i = 0; i < 1000; i++) {
            array[idx] = i;
        }
    }

    // Array store can't be moved out of loop because of following
    // non loop invariant array access
    static void test_after_2(int idx) {
        for (int i = 0; i < 1000; i++) {
            array[idx] = i;
            array2[i%10] = i;
        }
    }

    // Array store can't be moved out of loop because of following
    // use
    static void test_after_3(int idx) {
        for (int i = 0; i < 1000; i++) {
            array[idx] = i;
            if (array[0] == -1) {
                break;
            }
        }
    }

    // Array store can't be moved out of loop because of preceding
    // use
    static void test_after_4(int idx) {
        for (int i = 0; i < 1000; i++) {
            if (array[0] == -2) {
                break;
            }
            array[idx] = i;
        }
    }

    // All array stores should be moved out of the loop, one after
    // the other
    static void test_after_5(int idx) {
        for (int i = 0; i < 1000; i++) {
            array[idx] = i;
            array[idx+1] = i;
            array[idx+2] = i;
            array[idx+3] = i;
            array[idx+4] = i;
            array[idx+5] = i;
        }
    }

    // Array store can be moved after the loop but needs to be
    // cloned on both exit paths
    static void test_after_6(int idx) {
        for (int i = 0; i < 1000; i++) {
            array[idx] = i;
            if (array3[i]) {
                return;
            }
        }
    }

    // Array store can be moved out of the inner loop
    static void test_after_7(int idx) {
        for (int i = 0; i < 1000; i++) {
            for (int j = 0; j <= 42; j++) {
                array4[i] = j;
            }
        }
    }

    // Optimize out redundant stores
    static void test_stores_1(int ignored) {
        array[0] = 0;
        array[1] = 1;
        array[2] = 2;
        array[0] = 0;
        array[1] = 1;
        array[2] = 2;
    }

    static void test_stores_2(int idx) {
        array[idx+0] = 0;
        array[idx+1] = 1;
        array[idx+2] = 2;
        array[idx+0] = 0;
        array[idx+1] = 1;
        array[idx+2] = 2;
    }

    static void test_stores_3(int idx) {
        byte_array[idx+0] = 0;
        byte_array[idx+1] = 1;
        byte_array[idx+2] = 2;
        byte_array[idx+0] = 0;
        byte_array[idx+1] = 1;
        byte_array[idx+2] = 2;
    }

    // Array store can be moved out of the loop before the loop header
    static void test_before_1(int idx) {
        for (int i = 0; i < 1000; i++) {
            array[idx] = 999;
        }
    }

    // Array store can't be moved out of the loop before the loop
    // header because there's more than one store on this slice
    static void test_before_2(int idx) {
        for (int i = 0; i < 1000; i++) {
            array[idx] = 999;
            array[i%2] = 0;
        }
    }

    // Array store can't be moved out of the loop before the loop
    // header because of use before store
    static int test_before_3(int idx) {
        int res = 0;
        for (int i = 0; i < 1000; i++) {
            res += array[i%10];
            array[idx] = 999;
        }
        return res;
    }

    // Array store can't be moved out of the loop before the loop
    // header because of possible early exit
    static void test_before_4(int idx) {
        for (int i = 0; i < 1000; i++) {
            if (idx / (i+1) > 0) {
                return;
            }
            array[idx] = 999;
        }
    }

    // Array store can't be moved out of the loop before the loop
    // header because it doesn't postdominate the loop head
    static void test_before_5(int idx) {
        for (int i = 0; i < 1000; i++) {
            if (i % 2 == 0) {
                array[idx] = 999;
            }
        }
    }

    // Array store can be moved out of the loop before the loop header
    static int test_before_6(int idx) {
        int res = 0;
        for (int i = 0; i < 1000; i++) {
            if (i%2 == 1) {
                res *= 2;
            } else {
                res++;
            }
            array[idx] = 999;
        }
        return res;
    }

    final HashMap<String,Method> tests = new HashMap<>();
    {
        for (Method m : this.getClass().getDeclaredMethods()) {
            if (m.getName().matches("test_(before|after|stores)_[0-9]+")) {
                assert(Modifier.isStatic(m.getModifiers())) : m;
                tests.put(m.getName(), m);
            }
        }
    }

    boolean success = true;
    void doTest(String name, Runnable init, Function<String, Boolean> check) throws Exception {
        Method m = tests.get(name);
        for (int i = 0; i < 20000; i++) {
            init.run();
            m.invoke(null, 0);
            success = success && check.apply(name);
            if (!success) {
                break;
            }
        }
    }

    static void array_init() {
        array[0] = -1;
    }

    static boolean array_check(String name) {
        boolean success = true;
        if (array[0] != 999) {
            success = false;
            System.out.println(name + " failed: array[0] = " + array[0]);
        }
        return success;
    }

    static void array_init2() {
        for (int i = 0; i < 6; i++) {
            array[i] = -1;
        }
    }

    static boolean array_check2(String name) {
        boolean success = true;
        for (int i = 0; i < 6; i++) {
            if (array[i] != 999) {
                success = false;
                System.out.println(name + " failed: array[" + i + "] = " + array[i]);
            }
        }
        return success;
    }

    static void array_init3() {
        for (int i = 0; i < 3; i++) {
            array[i] = -1;
        }
    }

    static boolean array_check3(String name) {
        boolean success = true;
        for (int i = 0; i < 3; i++) {
            if (array[i] != i) {
                success = false;
                System.out.println(name + " failed: array[" + i + "] = " + array[i]);
            }
        }
        return success;
    }

    static void array_init4() {
        for (int i = 0; i < 3; i++) {
            byte_array[i] = -1;
        }
    }

    static boolean array_check4(String name) {
        boolean success = true;
        for (int i = 0; i < 3; i++) {
            if (byte_array[i] != i) {
                success = false;
                System.out.println(name + " failed: byte_array[" + i + "] = " + byte_array[i]);
            }
        }
        return success;
    }

    static boolean array_check5(String name) {
        boolean success = true;
        for (int i = 0; i < 1000; i++) {
            if (array4[i] != 42) {
                success = false;
                System.out.println(name + " failed: array[" + i + "] = " + array4[i]);
            }
        }
        return success;
    }

    static public void main(String[] args) throws Exception {
        TestMoveStoresOutOfLoops test = new TestMoveStoresOutOfLoops();
        test.doTest("test_after_1", TestMoveStoresOutOfLoops::array_init, TestMoveStoresOutOfLoops::array_check);
        test.doTest("test_after_2", TestMoveStoresOutOfLoops::array_init, TestMoveStoresOutOfLoops::array_check);
        test.doTest("test_after_3", TestMoveStoresOutOfLoops::array_init, TestMoveStoresOutOfLoops::array_check);
        test.doTest("test_after_4", TestMoveStoresOutOfLoops::array_init, TestMoveStoresOutOfLoops::array_check);
        test.doTest("test_after_5", TestMoveStoresOutOfLoops::array_init2, TestMoveStoresOutOfLoops::array_check2);
        test.doTest("test_after_6", TestMoveStoresOutOfLoops::array_init, TestMoveStoresOutOfLoops::array_check);
        array3[999] = true;
        test.doTest("test_after_6", TestMoveStoresOutOfLoops::array_init, TestMoveStoresOutOfLoops::array_check);
        test.doTest("test_after_7", TestMoveStoresOutOfLoops::array_init, TestMoveStoresOutOfLoops::array_check5);

        test.doTest("test_stores_1", TestMoveStoresOutOfLoops::array_init3, TestMoveStoresOutOfLoops::array_check3);
        test.doTest("test_stores_2", TestMoveStoresOutOfLoops::array_init3, TestMoveStoresOutOfLoops::array_check3);
        test.doTest("test_stores_3", TestMoveStoresOutOfLoops::array_init4, TestMoveStoresOutOfLoops::array_check4);

        test.doTest("test_before_1", TestMoveStoresOutOfLoops::array_init, TestMoveStoresOutOfLoops::array_check);
        test.doTest("test_before_2", TestMoveStoresOutOfLoops::array_init, TestMoveStoresOutOfLoops::array_check);
        test.doTest("test_before_3", TestMoveStoresOutOfLoops::array_init, TestMoveStoresOutOfLoops::array_check);
        test.doTest("test_before_4", TestMoveStoresOutOfLoops::array_init, TestMoveStoresOutOfLoops::array_check);
        test.doTest("test_before_5", TestMoveStoresOutOfLoops::array_init, TestMoveStoresOutOfLoops::array_check);
        test.doTest("test_before_6", TestMoveStoresOutOfLoops::array_init, TestMoveStoresOutOfLoops::array_check);

        if (!test.success) {
            throw new RuntimeException("Some tests failed");
        }
    }
}
