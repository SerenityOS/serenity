/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @key randomness
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
 * @build jdk.internal.vm.ci/jdk.vm.ci.hotspot.CompilerToVMHelper sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:.
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI
 *                   -XX:-BackgroundCompilation
 *                   compiler.jvmci.compilerToVM.HasCompiledCodeForOSRTest
 */

package compiler.jvmci.compilerToVM;

import compiler.jvmci.common.CTVMUtilities;
import compiler.testlibrary.CompilerUtils;
import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;
import jdk.vm.ci.hotspot.CompilerToVMHelper;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod;
import sun.hotspot.code.NMethod;

import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.List;

public class HasCompiledCodeForOSRTest {
    public static void main(String[] args) {
        List<CompileCodeTestCase> testCases = createTestCases();
        testCases.forEach(HasCompiledCodeForOSRTest::runSanityTest);
    }

    public static List<CompileCodeTestCase> createTestCases() {
        List<CompileCodeTestCase> testCases = new ArrayList<>();

        try {
            Class<?> aClass = DummyClass.class;
            Object receiver = new DummyClass();
            testCases.add(new CompileCodeTestCase(receiver,
                    aClass.getMethod("withLoop"), 17));
        } catch (NoSuchMethodException e) {
            throw new Error("TEST BUG : " + e.getMessage(), e);
        }
        return testCases;
    }

    private static void runSanityTest(CompileCodeTestCase testCase) {
        System.out.println(testCase);
        Executable aMethod = testCase.executable;
        HotSpotResolvedJavaMethod method = CTVMUtilities
                .getResolvedMethod(aMethod);
        testCase.invoke(Utils.getNullValues(aMethod.getParameterTypes()));
        testCase.deoptimize();
        int[] levels = CompilerUtils.getAvailableCompilationLevels();
        // not compiled
        for (int level : levels) {
            boolean isCompiled = CompilerToVMHelper.hasCompiledCodeForOSR(
                    method, testCase.bci, level);
            Asserts.assertFalse(isCompiled, String.format(
                    "%s : unexpected return value for non-compiled method at "
                            + "level %d", testCase, level));
        }
        NMethod nm = testCase.compile();
        if (nm == null) {
            throw new Error(String.format(
                    "TEST BUG : %s : cannot compile method", testCase));
        }

        boolean isCompiled;
        int level = nm.comp_level;
        int[] someLevels = new int[] {-4, 0, 1, 2, 3, 4, 5, 45};
        // check levels
        for (int i : someLevels) {
            isCompiled = CompilerToVMHelper.hasCompiledCodeForOSR(
                    method, testCase.bci, i);
            Asserts.assertEQ(isCompiled, level == i, String.format(
                    "%s : unexpected return value for compiled method at "
                            + "level %d", testCase, i));
        }

        // check bci
        byte[] bytecode = CompilerToVMHelper.getBytecode(CTVMUtilities
                .getResolvedMethod(testCase.executable));
        int[] incorrectBci = new int[] {
                testCase.bci + 1,
                testCase.bci - 1,
                -200,
                -10,
                bytecode.length,
                bytecode.length + 1,
                bytecode.length + 4,
                bytecode.length + 200
        };
        for (int bci : incorrectBci) {
            isCompiled = CompilerToVMHelper.hasCompiledCodeForOSR(
                    method, bci, level);
            Asserts.assertFalse(isCompiled, String.format(
                    "%s : unexpected return value for compiled method at "
                            + "level %d with bci = %d ",
                    testCase, level, bci));
        }
    }
}
