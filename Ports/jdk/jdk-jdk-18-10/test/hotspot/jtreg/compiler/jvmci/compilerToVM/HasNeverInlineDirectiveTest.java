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
 *                   -XX:-UseJVMCICompiler
 *                   compiler.jvmci.compilerToVM.HasNeverInlineDirectiveTest
 */

package compiler.jvmci.compilerToVM;

import compiler.jvmci.common.CTVMUtilities;
import jdk.test.lib.Asserts;
import jdk.vm.ci.hotspot.CompilerToVMHelper;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod;
import sun.hotspot.WhiteBox;

import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HasNeverInlineDirectiveTest {

    private static final WhiteBox WB = WhiteBox.getWhiteBox();

    public static void main(String[] args) {
        List<Executable> testCases = createTestCases();
        testCases.forEach(HasNeverInlineDirectiveTest::runSanityTest);
    }

    private static void runSanityTest(Executable aMethod) {
        HotSpotResolvedJavaMethod method = CTVMUtilities
                .getResolvedMethod(aMethod);
        boolean hasNeverInlineDirective = CompilerToVMHelper.hasNeverInlineDirective(method);
        boolean expected = WB.testSetDontInlineMethod(aMethod, true);
        Asserts.assertEQ(hasNeverInlineDirective, expected, "Unexpected initial " +
                "value of property 'hasNeverInlineDirective'");

        hasNeverInlineDirective = CompilerToVMHelper.hasNeverInlineDirective(method);
        Asserts.assertTrue(hasNeverInlineDirective, aMethod + "Unexpected value of " +
                "property 'hasNeverInlineDirective' after setting 'do not inline' to true");
        WB.testSetDontInlineMethod(aMethod, false);
        hasNeverInlineDirective = CompilerToVMHelper.hasNeverInlineDirective(method);
        Asserts.assertFalse(hasNeverInlineDirective, "Unexpected value of " +
                "property 'hasNeverInlineDirective' after setting 'do not inline' to false");
    }

    private static List<Executable> createTestCases() {
        List<Executable> testCases = new ArrayList<>();

        Class<?> aClass = DummyClass.class;
        testCases.addAll(Arrays.asList(aClass.getDeclaredMethods()));
        testCases.addAll(Arrays.asList(aClass.getDeclaredConstructors()));
        return testCases;
    }
}
