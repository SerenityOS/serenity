/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8136421
 * @requires vm.jvmci
 * @library /test/lib /
 * @library ../common/patches
 * @modules java.base/jdk.internal.misc
 * @modules java.base/jdk.internal.org.objectweb.asm
 *          java.base/jdk.internal.org.objectweb.asm.tree
 *          jdk.internal.vm.ci/jdk.vm.ci.hotspot
 *          jdk.internal.vm.ci/jdk.vm.ci.code
 *          jdk.internal.vm.ci/jdk.vm.ci.meta
 *          jdk.internal.vm.ci/jdk.vm.ci.runtime
 *
 * @build jdk.internal.vm.ci/jdk.vm.ci.hotspot.CompilerToVMHelper
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI
 *                   -XX:-UseJVMCICompiler
 *                   compiler.jvmci.compilerToVM.GetStackTraceElementTest
 */

package compiler.jvmci.compilerToVM;

import compiler.jvmci.common.CTVMUtilities;
import compiler.jvmci.common.testcases.TestCase;
import jdk.test.lib.Asserts;
import jdk.vm.ci.hotspot.CompilerToVMHelper;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class GetStackTraceElementTest {

    public static void main(String[] args) {
        Map<Executable, int[]> testCases = createTestCases();
        testCases.forEach(GetStackTraceElementTest::runSanityTest);
    }

    private static void runSanityTest(Executable aMethod, int[] bcis) {
        HotSpotResolvedJavaMethod method = CTVMUtilities
                .getResolvedMethod(aMethod);
        String className = aMethod.getDeclaringClass().getName();
        String methodName = aMethod.getName().equals(className)
                ? "<init>"
                : aMethod.getName();
        String fileName = getFileName(className);
        Map<Integer, Integer> bciWithLineNumber = CTVMUtilities
                .getBciToLineNumber(aMethod);
        boolean isNative = Modifier.isNative(aMethod.getModifiers());
        int lineNumber = -1;
        for (int bci : bcis) {
            StackTraceElement ste = CompilerToVMHelper
                    .getStackTraceElement(method, bci);
            Asserts.assertNotNull(ste, aMethod + " : got null StackTraceElement"
                    + " at bci " + bci);
            Asserts.assertEQ(className, ste.getClassName(), aMethod
                    + " : unexpected class name");
            Asserts.assertEQ(fileName, ste.getFileName(), aMethod
                    + " : unexpected filename");
            Asserts.assertEQ(methodName, ste.getMethodName(), aMethod
                    + " : unexpected method name");
            Asserts.assertEQ(isNative, ste.isNativeMethod(), aMethod
                    + " : unexpected 'isNative' value");
            if (bciWithLineNumber.size() > 0) {
                if (bciWithLineNumber.containsKey(bci)) {
                    lineNumber = bciWithLineNumber.get(bci);
                }
                Asserts.assertEQ(lineNumber, ste.getLineNumber(), aMethod
                        + " : unexpected line number");
            } else {
                // native and abstract function
                Asserts.assertGT(0, ste.getLineNumber(),
                        aMethod + " : unexpected line number for abstract "
                                + "or native method");
            }
        }

    }

    private static String getFileName(String className) {
        int lastDot = className.lastIndexOf('.');
        int firstDol = className.contains("$")
                ? className.indexOf('$')
                : className.length();
        return className.substring(lastDot + 1, firstDol) + ".java";
    }

    private static Map<Executable, int[]> createTestCases() {
        Map<Executable, int[]> testCases = new HashMap<>();

        try {
            Class<?> aClass = DummyClass.class;
            Method aMethod = aClass.getDeclaredMethod("dummyInstanceFunction");
            int[] bci = new int[] {0, 2, 3, 6, 7, 8, 11, 13, 15, 16, 17, 18};
            testCases.put(aMethod, bci);

            aMethod = aClass.getDeclaredMethod("dummyEmptyFunction");
            bci = new int[] {0};
            testCases.put(aMethod, bci);

            aMethod = aClass.getDeclaredMethod("nativeFunction");
            bci = new int[] {0};
            testCases.put(aMethod, bci);

            TestCase.getAllExecutables()
                    .forEach(c -> testCases.put(c, new int[] {0}));
        } catch (NoSuchMethodException e) {
            throw new Error("TEST BUG : test method not found", e);
        }
        return testCases;
    }

    private class DummyClass {
        public int dummyInstanceFunction() {
            String str1 = "123123123";
            double x = 3.14;
            int y = Integer.parseInt(str1);

            return y / (int)x;
        }

        public void dummyEmptyFunction() {}

        public native void nativeFunction();
    }
}
