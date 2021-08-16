/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8073480
 * @summary explicit range checks should be recognized by C2
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc:+open
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -ea -Xmixed -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
 *                   -XX:CompileCommand=compileonly,compiler.rangechecks.TestExplicitRangeChecks::test*
 *                   compiler.rangechecks.TestExplicitRangeChecks
 *
 */

package compiler.rangechecks;

import compiler.whitebox.CompilerWhiteBoxTest;
import jdk.internal.misc.Unsafe;
import jdk.test.lib.Platform;
import sun.hotspot.WhiteBox;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;

public class TestExplicitRangeChecks {
    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();
    private static final int TIERED_STOP_AT_LEVEL = WHITE_BOX.getIntxVMFlag("TieredStopAtLevel").intValue();
    private static int[] array = new int[10];
    private static boolean success = true;

    @Retention(RetentionPolicy.RUNTIME)
    @interface Args {
        int[] compile();
        int[] good();
        int[] bad();
        boolean deoptimize() default true;
    }

    // Should be compiled as a single unsigned comparison
    // 0 <= index < array.length
    @Args(compile = {5,}, good = {0, 9}, bad = {-1, 10})
    static boolean test1_1(int index, int[] array) {
        if (index < 0 || index >= array.length) {
            return false;
        }
        return true;
    }

    // same test but so we can compile with same optimization after trap in test1_1
    static boolean test1_2(int index, int[] array) {
        if (index < 0 || index >= array.length) {
            return false;
        }
        return true;
    }

    // Shouldn't matter whether first or second test is the one
    // against a constants
    // 0 <= index < array.length
    @Args(compile = {5,}, good = {0, 9}, bad = {-1, 10})
    static boolean test2_1(int index, int[] array) {
        if (index >= array.length || index < 0) {
            return false;
        }
        return true;
    }

    static boolean test2_2(int index, int[] array) {
        if (index >= array.length || index < 0) {
            return false;
        }
        return true;
    }

    // 0 <= index <= array.length
    @Args(compile = {5,}, good = {0, 10}, bad = {-1, 11})
    static boolean test3_1(int index, int[] array) {
        if (index < 0 || index > array.length) {
            return false;
        }
        return true;
    }

    static boolean test3_2(int index, int[] array) {
        if (index < 0 || index > array.length) {
            return false;
        }
        return true;
    }

    // 0 <= index <= array.length
    @Args(compile = {5,}, good = {0, 10}, bad = {-1, 11})
    static boolean test4_1(int index, int[] array) {
        if (index > array.length || index < 0 ) {
            return false;
        }
        return true;
    }

    static boolean test4_2(int index, int[] array) {
        if (index > array.length || index < 0) {
            return false;
        }
        return true;
    }

    static int[] test5_helper(int i) {
        return (i < 100) ? new int[10] : new int[5];
    }

    // 0 < index < array.length
    @Args(compile = {5,}, good = {1, 9}, bad = {0, 10})
    static boolean test5_1(int index, int[] array) {
        array = test5_helper(index); // array.length must be not constant greater than 1
        if (index <= 0 || index >= array.length) {
            return false;
        }
        return true;
    }

    static boolean test5_2(int index, int[] array) {
        array = test5_helper(index); // array.length must be not constant greater than 1
        if (index <= 0 || index >= array.length) {
            return false;
        }
        return true;
    }

    // 0 < index < array.length
    @Args(compile = {5,}, good = {1, 9}, bad = {0, 10})
    static boolean test6_1(int index, int[] array) {
        array = test5_helper(index); // array.length must be not constant greater than 1
        if (index >= array.length || index <= 0 ) {
            return false;
        }
        return true;
    }

    static boolean test6_2(int index, int[] array) {
        array = test5_helper(index); // array.length must be not constant greater than 1
        if (index >= array.length || index <= 0) {
            return false;
        }
        return true;
    }

    // 0 < index <= array.length
    @Args(compile = {5,}, good = {1, 10}, bad = {0, 11})
    static boolean test7_1(int index, int[] array) {
        if (index <= 0 || index > array.length) {
            return false;
        }
        return true;
    }

    static boolean test7_2(int index, int[] array) {
        if (index <= 0 || index > array.length) {
            return false;
        }
        return true;
    }

    // 0 < index <= array.length
    @Args(compile = {5,}, good = {1, 10}, bad = {0, 11})
    static boolean test8_1(int index, int[] array) {
        if (index > array.length || index <= 0 ) {
            return false;
        }
        return true;
    }

    static boolean test8_2(int index, int[] array) {
        if (index > array.length || index <= 0) {
            return false;
        }
        return true;
    }

    static int[] test9_helper1(int i) {
        return (i < 100) ? new int[1] : new int[2];
    }

    static int[] test9_helper2(int i) {
        return (i < 100) ? new int[10] : new int[11];
    }

    // array1.length <= index < array2.length
    @Args(compile = {5,}, good = {1, 9}, bad = {0, 10})
    static boolean test9_1(int index, int[] array) {
        int[] array1 = test9_helper1(index);
        int[] array2 = test9_helper2(index);
        if (index < array1.length || index >= array2.length) {
            return false;
        }
        return true;
    }

    static boolean test9_2(int index, int[] array) {
        int[] array1 = test9_helper1(index);
        int[] array2 = test9_helper2(index);
        if (index < array1.length || index >= array2.length) {
            return false;
        }
        return true;
    }

    // Previously supported pattern
    @Args(compile = {-5,5,15}, good = {0, 9}, bad = {-1, 10}, deoptimize=false)
    static boolean test10_1(int index, int[] array) {
        if (index < 0 || index >= 10) {
            return false;
        }
        return true;
    }

    static int[] array11 = new int[10];
    @Args(compile = {5,}, good = {0, 9}, bad = {-1,})
    static boolean test11_1(int index, int[] array) {
        if (index < 0) {
            return false;
        }
        int unused = array11[index];
        // If this one is folded with the first test then we allow
        // array access above to proceed even for out of bound array
        // index and the method throws an
        // ArrayIndexOutOfBoundsException.
        if (index >= array.length) {
            return false;
        }
        return true;
    }

    static int[] array12 = {10, 10, 10, 10, 10, 10, 10, 10, 10, 10};
    @Args(compile = {5,}, good = {0, 9}, bad = {-1,})
    static boolean test12_1(int index, int[] array) {
        // Cannot be folded otherwise would cause incorrect array
        // access if the array12 range check is executed before the
        // folded test.
        if (index < 0 || index >= array12[index]) {
            return false;
        }
        return true;
    }

    // Same as test1_1 but pass null array when index < 0: shouldn't
    // cause NPE.
    @Args(compile = {5,}, good = {0, 9}, bad = {})
    static boolean test13_1(int index, int[] array) {
        if (index < 0 || index >= array.length) {
            return false;
        }
        return true;
    }

    // Same as test10 but with uncommon traps
    @Args(compile = {5}, good = {0, 9}, bad = {-1, 10})
    static boolean test14_1(int index, int[] array) {
        if (index < 0 || index >= 10) {
            return false;
        }
        return true;
    }

    static boolean test14_2(int index, int[] array) {
        if (index < 0 || index >= 10) {
            return false;
        }
        return true;
    }

    // Same as test13_1 but pass null array: null trap should be reported on first if
    @Args(compile = {5,}, good = {0, 9}, bad = {})
    static boolean test15_1(int index, int[] array) {
        if (index < 0 || index >= array.length) {
            return false;
        }
        return true;
    }

    // Same as test1 but with no null check between the integer comparisons
    @Args(compile = {5,}, good = {0, 9}, bad = {-1, 10})
    static boolean test16_1(int index, int[] array) {
        int l = array.length;
        if (index < 0 || index >= l) {
            return false;
        }
        return true;
    }

    static boolean test16_2(int index, int[] array) {
        int l = array.length;
        if (index < 0 || index >= l) {
            return false;
        }
        return true;
    }

    // Same as test1 but bound check on array access should optimize
    // out.
    @Args(compile = {5,}, good = {0, 9}, bad = {-1, 10})
    static boolean test17_1(int index, int[] array) {
        if (index < 0 || index >= array.length) {
            return false;
        }
        array[index] = 0;
        return true;
    }

    static boolean test17_2(int index, int[] array) {
        if (index < 0 || index >= array.length) {
            return false;
        }
        array[index] = 0;
        return true;
    }

    // Same as test1 but range check smearing should optimize
    // 3rd range check out.
    @Args(compile = {5,}, good = {}, bad = {})
    static boolean test18_1(int index, int[] array) {
        if (index < 0 || index >= array.length) {
            return false;
        }
        array[index+2] = 0;
        array[index+1] = 0;
        return true;
    }

    static boolean test19_helper1(int index) {
        if (index < 12) {
            return false;
        }
        return true;
    }

    static boolean test19_helper2(int index) {
        if (index > 8) {
            return false;
        }
        return true;
    }

    // Second test should be optimized out
    static boolean test19(int index, int[] array) {
        test19_helper1(index);
        test19_helper2(index);
        return true;
    }

    final HashMap<String,Method> tests = new HashMap<>();
    {
        for (Method m : this.getClass().getDeclaredMethods()) {
            if (m.getName().matches("test[0-9]+(_[0-9])?")) {
                assert(Modifier.isStatic(m.getModifiers())) : m;
                tests.put(m.getName(), m);
            }
        }
    }

    void doTest(String name) throws Exception {
        Method m = tests.get(name + "_1");

        Args anno =  m.getAnnotation(Args.class);
        int[] compile = anno.compile();
        int[] good = anno.good();
        int[] bad = anno.bad();
        boolean deoptimize = anno.deoptimize();

        // Get compiled
        for (int i = 0; i < 20000;) {
            for (int j = 0; j < compile.length; j++) {
                m.invoke(null, compile[j], array);
                i++;
            }
        }

        if (!WHITE_BOX.isMethodCompiled(m)) {
            System.out.println(name + "_1 not compiled");
            success = false;
        }

        // check that good values don't trigger exception or
        // deoptimization
        for (int i = 0; i < good.length; i++) {
            boolean res = (boolean)m.invoke(null, good[i], array);

            if (!res) {
                System.out.println(name + " bad result for good input " + good[i]);
                success = false;
            }
            if (!WHITE_BOX.isMethodCompiled(m)) {
                System.out.println(name + " deoptimized on valid access");
                success = false;
            }
        }

        // check that bad values trigger exception and deoptimization
        for (int i = 0; i < bad.length; i++) {
            if (i > 0 && deoptimize) {
                m = tests.get(name + "_" + (i+1));
                for (int k = 0; k < 20000;) {
                    for (int j = 0; j < compile.length; j++) {
                        m.invoke(null, compile[j], array);
                        k++;
                    }
                }
                if (!WHITE_BOX.isMethodCompiled(m)) {
                    System.out.println(name + ("_" + (i+1)) + " not compiled");
                    success = false;
                }
            }

            boolean res = (boolean)m.invoke(null, bad[i], array);

            if (res) {
                System.out.println(name + " bad result for bad input " + bad[i]);
                success = false;
            }
            // Only perform these additional checks if C2 is available
            if (Platform.isServer() && !Platform.isEmulatedClient() &&
                TIERED_STOP_AT_LEVEL == CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION) {
                if (deoptimize && WHITE_BOX.isMethodCompiled(m)) {
                    System.out.println(name + " not deoptimized on invalid access");
                    success = false;
                } else if (!deoptimize && !WHITE_BOX.isMethodCompiled(m)) {
                    System.out.println(name + " deoptimized on invalid access");
                    success = false;
                }
            }
        }

    }

    private static final Unsafe UNSAFE;

    static {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            UNSAFE = (Unsafe) unsafeField.get(null);
        }
        catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    // On x64, int to long conversion should optimize away in address computation
    static int test20(int[] a) {
        int sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += test20_helper(a, i);
        }
        return sum;
    }

    static int test20_helper(int[] a, int i) {
        if (i < 0 || i >= a.length)
            throw new ArrayIndexOutOfBoundsException();

        long address = (((long) i) << 2) + UNSAFE.ARRAY_INT_BASE_OFFSET;
        return UNSAFE.getInt(a, address);
    }

    static int test21(int[] a) {
        int sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += test20_helper(a, i);
        }
        return sum;
    }

    static int test21_helper(int[] a, int i) {
        if (i < 0 || i >= a.length)
            throw new ArrayIndexOutOfBoundsException();

        long address = (((long) i) << 2) + UNSAFE.ARRAY_INT_BASE_OFFSET;
        return UNSAFE.getIntVolatile(a, address);
    }

    static public void main(String[] args) throws Exception {

        if (WHITE_BOX.getBooleanVMFlag("BackgroundCompilation")) {
            throw new AssertionError("Background compilation enabled");
        }

        TestExplicitRangeChecks test = new TestExplicitRangeChecks();

        test.doTest("test1");
        test.doTest("test2");
        test.doTest("test3");
        test.doTest("test4");

        // pollute branch profile
        for (int i = 0; i < 10000; i++) {
            test5_helper((i%2 == 0) ? 0 : 1000);
        }

        test.doTest("test5");
        test.doTest("test6");
        test.doTest("test7");
        test.doTest("test8");

        // pollute branch profile
        for (int i = 0; i < 10000; i++) {
            test9_helper1((i%2 == 0) ? 0 : 1000);
            test9_helper2((i%2 == 0) ? 0 : 1000);
        }

        test.doTest("test9");
        test.doTest("test10");
        test.doTest("test11");
        test.doTest("test12");

        test.doTest("test13");
        {
            Method m = test.tests.get("test13_1");
            for (int i = 0; i < 1; i++) {
                test13_1(-1, null);
                if (!WHITE_BOX.isMethodCompiled(m)) {
                    break;
                }
            }
        }
        test.doTest("test13");
        {
            Method m = test.tests.get("test13_1");
            for (int i = 0; i < 10; i++) {
                test13_1(-1, null);
                if (!WHITE_BOX.isMethodCompiled(m)) {
                    break;
                }
            }
        }

        test.doTest("test14");

        test.doTest("test15");
        {
            Method m = test.tests.get("test15_1");
            for (int i = 0; i < 10; i++) {
                try {
                    test15_1(5, null);
                } catch(NullPointerException npe) {}
                if (!WHITE_BOX.isMethodCompiled(m)) {
                    break;
                }
            }
        }
        test.doTest("test15");
        test.doTest("test16");
        test.doTest("test17");
        test.doTest("test18");

        for (int i = 0; i < 20000; i++) {
            test19_helper1(20);
            test19_helper2(5);
        }

        {
            Method m = test.tests.get("test19");
            WHITE_BOX.enqueueMethodForCompilation(m, CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION);
        }

        for (int i = 0; i < 20000; i++) {
            test20(array);
        }

        for (int i = 0; i < 20000; i++) {
            test21(array);
        }

        if (!success) {
            throw new RuntimeException("some tests failed");
        }
    }
}
