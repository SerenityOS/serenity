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
 * @build jdk.internal.vm.ci/jdk.vm.ci.hotspot.CompilerToVMHelper
 *        sun.hotspot.WhiteBox jdk.test.whitebox.parser.DiagnosticCommand
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *                                jdk.test.whitebox.parser.DiagnosticCommand
 * @run main/othervm -Xbootclasspath/a:.
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI
 *                   -XX:-BackgroundCompilation
 *                   compiler.jvmci.compilerToVM.AllocateCompileIdTest
 */

package compiler.jvmci.compilerToVM;

import compiler.jvmci.common.CTVMUtilities;
import jdk.test.lib.Asserts;
import jdk.test.lib.util.Pair;
import jdk.test.lib.Utils;
import jdk.vm.ci.hotspot.CompilerToVMHelper;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod;
import sun.hotspot.code.NMethod;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AllocateCompileIdTest {

    private static final int SOME_REPEAT_VALUE = 5;
    private final HashSet<Integer> ids = new HashSet<>();

    public static void main(String[] args) {
        AllocateCompileIdTest test = new AllocateCompileIdTest();
        createTestCasesCorrectBci().forEach(test::runSanityCorrectTest);
        createTestCasesIncorrectBci().forEach(test::runSanityIncorrectTest);
    }

    private static List<CompileCodeTestCase> createTestCasesCorrectBci() {
        List<CompileCodeTestCase> result = new ArrayList<>();
        try {
            Class<?> aClass = DummyClass.class;
            Method method = aClass.getMethod("withLoop");
            Object receiver = new DummyClass();
            result.add(new CompileCodeTestCase(receiver, method, 17));
            result.add(new CompileCodeTestCase(receiver, method, -1));
        } catch (NoSuchMethodException e) {
            throw new Error("TEST BUG : " + e, e);
        }
        return result;
    }

    private static List<Pair<CompileCodeTestCase, Class<? extends Throwable>>>
            createTestCasesIncorrectBci() {
        List<Pair<CompileCodeTestCase, Class<? extends Throwable>>> result
                = new ArrayList<>();
        try {
            Class<?> aClass = DummyClass.class;
            Object receiver = new DummyClass();
            Method method = aClass.getMethod("dummyInstanceFunction");
            // greater than bytecode.length
            byte[] bytecode = CompilerToVMHelper.getBytecode(CTVMUtilities
                    .getResolvedMethod(method));
            Stream.of(
                    // greater than bytecode.length
                    bytecode.length + 4,
                    bytecode.length + 50,
                    bytecode.length + 200,
                    // negative cases
                    -4, -50, -200)
                    .map(bci -> new Pair<CompileCodeTestCase,
                            Class<? extends Throwable>>(
                            new CompileCodeTestCase(receiver, method, bci),
                            IllegalArgumentException.class))
                    .collect(Collectors.toList());
        } catch (NoSuchMethodException e) {
            throw new Error("TEST BUG : " + e.getMessage(), e);
        }
        return result;
    }

    private void runSanityCorrectTest(CompileCodeTestCase testCase) {
        System.out.println(testCase);
        Executable aMethod = testCase.executable;
        // to generate ciTypeFlow
        testCase.invoke(Utils.getNullValues(aMethod.getParameterTypes()));
        int bci = testCase.bci;
        HotSpotResolvedJavaMethod method = CTVMUtilities
                .getResolvedMethod(aMethod);
        for (int i = 0; i < SOME_REPEAT_VALUE; ++i) {
            int wbCompileID = getWBCompileID(testCase);
            int id = CompilerToVMHelper.allocateCompileId(method, bci);
            Asserts.assertNE(id, 0, testCase + " : zero compile id");
            Asserts.assertGT(id, wbCompileID, testCase
                    + " : allocated 'compile id' not  greater than existed");
            Asserts.assertTrue(ids.add(wbCompileID), testCase
                    + " : vm compilation allocated existing id " + id);
            Asserts.assertTrue(ids.add(id), testCase
                    + " : allocateCompileId returned existing id " + id);
        }
    }

    private void runSanityIncorrectTest(
            Pair<CompileCodeTestCase, Class<? extends Throwable>> testCase) {
        System.out.println(testCase);
        Class<? extends Throwable> exception = testCase.second;
        Executable aMethod = testCase.first.executable;
        int bci = testCase.first.bci;
        HotSpotResolvedJavaMethod method = CTVMUtilities
                .getResolvedMethod(aMethod);
        Utils.runAndCheckException(
                () -> CompilerToVMHelper.allocateCompileId(method, bci),
                exception);
    }

    private int getWBCompileID(CompileCodeTestCase testCase) {
        NMethod nm = testCase.deoptimizeAndCompile();
        if (nm == null || nm.compile_id <= 0) {
            throw new Error("TEST BUG : cannot compile method " + testCase);
        }
        return nm.compile_id;
    }
}
