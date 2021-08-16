/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8066103
 * @summary C2's range check smearing allows out of bound array accesses
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -ea -Xmixed -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
 *                   -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockExperimentalVMOptions -XX:+EagerJVMCI
 *                   compiler.rangechecks.TestRangeCheckSmearing
 *
 */

package compiler.rangechecks;

import compiler.whitebox.CompilerWhiteBoxTest;
import compiler.testlibrary.CompilerUtils;
import jdk.test.lib.Platform;
import sun.hotspot.WhiteBox;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;

public class TestRangeCheckSmearing {
    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();

    @Retention(RetentionPolicy.RUNTIME)
    @interface Args { int[] value(); }

    // first range check is i + max of all constants
    @Args({0, 8})
    static int m1(int[] array, int i, boolean allaccesses) {
        int res = 0;
        res += array[i+9];
        if (allaccesses) {
            res += array[i+8];
            res += array[i+7];
            res += array[i+6];
            res += array[i+5];
            res += array[i+4];
            res += array[i+3];
            res += array[i+2];
            res += array[i+1];
        }
        return res;
    }

    // first range check is i + min of all constants
    @Args({0, -9})
    static int m2(int[] array, int i, boolean allaccesses) {
        int res = 0;
        res += array[i+1];
        if (allaccesses) {
            res += array[i+2];
            res += array[i+3];
            res += array[i+4];
            res += array[i+5];
            res += array[i+6];
            res += array[i+7];
            res += array[i+8];
            res += array[i+9];
        }
        return res;
    }

    // first range check is not i + min/max of all constants
    @Args({0, 8})
    static int m3(int[] array, int i, boolean allaccesses) {
        int res = 0;
        res += array[i+3];
        if (allaccesses) {
            res += array[i+2];
            res += array[i+1];
            res += array[i+4];
            res += array[i+5];
            res += array[i+6];
            res += array[i+7];
            res += array[i+8];
            res += array[i+9];
        }
        return res;
    }

    @Args({0, -9})
    static int m4(int[] array, int i, boolean allaccesses) {
        int res = 0;
        res += array[i+3];
        if (allaccesses) {
            res += array[i+4];
            res += array[i+1];
            res += array[i+2];
            res += array[i+5];
            res += array[i+6];
            res += array[i+7];
            res += array[i+8];
            res += array[i+9];
        }
        return res;
    }

    @Args({0, -3})
    static int m5(int[] array, int i, boolean allaccesses) {
        int res = 0;
        res += array[i+3];
        res += array[i+2];
        if (allaccesses) {
            res += array[i+1];
            res += array[i+4];
            res += array[i+5];
            res += array[i+6];
            res += array[i+7];
            res += array[i+8];
            res += array[i+9];
        }
        return res;
    }

    @Args({0, 6})
    static int m6(int[] array, int i, boolean allaccesses) {
        int res = 0;
        res += array[i+3];
        res += array[i+4];
        if (allaccesses) {
            res += array[i+2];
            res += array[i+1];
            res += array[i+5];
            res += array[i+6];
            res += array[i+7];
            res += array[i+8];
            res += array[i+9];
        }
        return res;
    }

    @Args({0, 6})
    static int m7(int[] array, int i, boolean allaccesses) {
        int res = 0;
        res += array[i+3];
        res += array[i+2];
        res += array[i+4];
        if (allaccesses) {
            res += array[i+1];
            res += array[i+5];
            res += array[i+6];
            res += array[i+7];
            res += array[i+8];
            res += array[i+9];
        }
        return res;
    }

    @Args({0, -3})
    static int m8(int[] array, int i, boolean allaccesses) {
        int res = 0;
        res += array[i+3];
        res += array[i+4];
        res += array[i+2];
        if (allaccesses) {
            res += array[i+1];
            res += array[i+5];
            res += array[i+6];
            res += array[i+7];
            res += array[i+8];
            res += array[i+9];
        }
        return res;
    }

    @Args({6, 15})
    static int m9(int[] array, int i, boolean allaccesses) {
        int res = 0;
        res += array[i+3];
        if (allaccesses) {
            res += array[i-2];
            res += array[i-1];
            res += array[i-4];
            res += array[i-5];
            res += array[i-6];
        }
        return res;
    }

    @Args({3, 12})
    static int m10(int[] array, int i, boolean allaccesses) {
        int res = 0;
        res += array[i+3];
        if (allaccesses) {
            res += array[i-2];
            res += array[i-1];
            res += array[i-3];
            res += array[i+4];
            res += array[i+5];
            res += array[i+6];
        }
        return res;
    }

    @Args({3, -3})
    static int m11(int[] array, int i, boolean allaccesses) {
        int res = 0;
        res += array[i+3];
        res += array[i-2];
        if (allaccesses) {
            res += array[i+5];
            res += array[i+6];
        }
        return res;
    }

    @Args({3, 6})
    static int m12(int[] array, int i, boolean allaccesses) {
        int res = 0;
        res += array[i+3];
        res += array[i+6];
        if (allaccesses) {
            res += array[i-2];
            res += array[i-3];
        }
        return res;
    }

    // check that identical range check is replaced by dominating one
    // only when correct
    @Args({0})
    static int m13(int[] array, int i, boolean ignore) {
        int res = 0;
        res += array[i+3];
        res += array[i+3];
        return res;
    }

    @Args({2, 0})
    static int m14(int[] array, int i, boolean ignore) {
        int res = 0;

        res += array[i];
        res += array[i-2];
        res += array[i]; // If range check below were to be removed first this cannot be considered identical to first range check
        res += array[i-1]; // range check removed so i-1 array access depends on previous check

        return res;
    }

    static int[] m15_dummy = new int[10];
    @Args({2, 0})
    static int m15(int[] array, int i, boolean ignore) {
        int res = 0;
        res += array[i];

        // When the loop is optimized out we don't want the
        // array[i-1] access which is dependent on array[i]'s
        // range check to become dependent on the identical range
        // check above.

        int[] array2 = m15_dummy;
        int j = 0;
        for (; j < 10; j++);
        if (j == 10) {
            array2 = array;
        }

        res += array2[i-2];
        res += array2[i];
        res += array2[i-1]; // range check removed so i-1 array access depends on previous check

        return res;
    }

    @Args({2, 0})
    static int m16(int[] array, int i, boolean ignore) {
        int res = 0;

        res += array[i];
        res += array[i-1];
        res += array[i-1];
        res += array[i-2];

        return res;
    }

    @Args({2, 0})
    static int m17(int[] array, int i, boolean ignore) {
        int res = 0;

        res += array[i];
        res += array[i-2];
        res += array[i-2];
        res += array[i+2];
        res += array[i+2];
        res += array[i-1];
        res += array[i-1];

        return res;
    }

    static public void main(String[] args) {
        if (WHITE_BOX.getBooleanVMFlag("BackgroundCompilation")) {
            throw new AssertionError("Background compilation enabled");
        }
        new TestRangeCheckSmearing().doTests();
    }
    boolean success = true;
    boolean exception = false;
    final int[] array = new int[10];
    final HashMap<String,Method> tests = new HashMap<>();
    {
        final Class<?> TEST_PARAM_TYPES[] = { int[].class, int.class, boolean.class };
        for (Method m : this.getClass().getDeclaredMethods()) {
            if (m.getName().matches("m[0-9]+")) {
                assert(Modifier.isStatic(m.getModifiers())) : m;
                assert(m.getReturnType() == int.class) : m;
                assert(Arrays.equals(m.getParameterTypes(), TEST_PARAM_TYPES)) : m;
                tests.put(m.getName(), m);
            }
        }
    }

    void invokeTest(Method m, int[] array, int index, boolean z) {
        try {
            m.invoke(null, array, index, z);
        } catch (ReflectiveOperationException roe) {
            Throwable ex = roe.getCause();
            if (ex instanceof ArrayIndexOutOfBoundsException)
                throw (ArrayIndexOutOfBoundsException) ex;
            throw new AssertionError(roe);
        }
    }

    void doTest(String name) {
        Method m = tests.get(name);
        tests.remove(name);
        int[] args = m.getAnnotation(Args.class).value();
        int index0 = args[0], index1;
        boolean exceptionRequired = true;
        if (args.length == 2) {
            index1 = args[1];
        } else {
            // no negative test for this one
            assert(args.length == 1);
            assert(name.equals("m13"));
            exceptionRequired = false;
            index1 = index0;
        }
        // Get the method compiled.
        if (!WHITE_BOX.isMethodCompiled(m)) {
            // If not, try to compile it with C2
            if(!WHITE_BOX.enqueueMethodForCompilation(m, CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION)) {
                // C2 compiler not available, try to compile with C1
                WHITE_BOX.enqueueMethodForCompilation(m, CompilerWhiteBoxTest.COMP_LEVEL_SIMPLE);
            }
        }
        if (!WHITE_BOX.isMethodCompiled(m)) {
            throw new RuntimeException(m + " not compiled");
        }

        // valid access
        invokeTest(m, array, index0, true);

        if (!WHITE_BOX.isMethodCompiled(m)) {
            throw new RuntimeException(m + " deoptimized on valid array access");
        }

        exception = false;
        boolean test_success = true;
        try {
            invokeTest(m, array, index1, false);
        } catch(ArrayIndexOutOfBoundsException aioob) {
            exception = true;
            System.out.println("ArrayIndexOutOfBoundsException thrown in "+name);
        }
        if (!exception) {
            System.out.println("ArrayIndexOutOfBoundsException was not thrown in "+name);
        }

        if (CompilerUtils.getMaxCompilationLevel() == CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION) {
            if (exceptionRequired == WHITE_BOX.isMethodCompiled(m)) {
                System.out.println((exceptionRequired?"Didn't deoptimized":"deoptimized") + " in "+name);
                test_success = false;
            }
        }

        if (exception != exceptionRequired) {
            System.out.println((exceptionRequired?"exception required but not thrown":"not exception required but thrown") + " in "+name);
            test_success = false;
        }

        if (!test_success) {
            success = false;
            System.out.println("TEST FAILED: "+name);
        }

    }
    void doTests() {
        doTest("m1");
        doTest("m2");
        doTest("m3");
        doTest("m4");
        doTest("m5");
        doTest("m6");
        doTest("m7");
        doTest("m8");
        doTest("m9");
        doTest("m10");
        doTest("m11");
        doTest("m12");
        doTest("m13");
        doTest("m14");
        doTest("m15");
        doTest("m16");
        doTest("m17");
        if (!success) {
            throw new RuntimeException("Some tests failed");
        }
        assert(tests.isEmpty()) : tests;
    }
}
