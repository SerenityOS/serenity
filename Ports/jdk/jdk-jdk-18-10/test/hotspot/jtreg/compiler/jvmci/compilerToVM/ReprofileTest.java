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
 * @requires vm.jvmci & (vm.opt.TieredStopAtLevel == null | vm.opt.TieredStopAtLevel == 4)
 * @library /test/lib /
 * @library ../common/patches
 * @modules java.base/jdk.internal.misc
 * @modules java.base/jdk.internal.org.objectweb.asm
 *          java.base/jdk.internal.org.objectweb.asm.tree
 *          jdk.internal.vm.ci/jdk.vm.ci.hotspot
 *          jdk.internal.vm.ci/jdk.vm.ci.code
 *          jdk.internal.vm.ci/jdk.vm.ci.meta
 *
 * @build jdk.internal.vm.ci/jdk.vm.ci.hotspot.CompilerToVMHelper sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:.
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI
 *                   -Xmixed -Xbatch
 *                   compiler.jvmci.compilerToVM.ReprofileTest
 */

package compiler.jvmci.compilerToVM;

import compiler.jvmci.common.CTVMUtilities;
import compiler.whitebox.CompilerWhiteBoxTest;
import jdk.test.lib.Asserts;
import jdk.vm.ci.hotspot.CompilerToVMHelper;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod;
import jdk.vm.ci.meta.ProfilingInfo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ReprofileTest {

    public static void main(String[] args) {
        List<Method> testCases = createTestCases();
        testCases.forEach(ReprofileTest::runSanityTest);
    }

    private static List<Method> createTestCases() {
        List<Method> testCases = new ArrayList<>();
        try {

            Class<?> aClass = DummyClass.class;
            testCases.add(aClass.getMethod("dummyInstanceFunction"));

            aClass = DummyClass.class;
            testCases.add(aClass.getMethod("dummyFunction"));
        } catch (NoSuchMethodException e) {
            throw new Error("TEST BUG " + e.getMessage(), e);
        }
        return testCases;
    }

    private static void runSanityTest(Method aMethod) {
        System.out.println(aMethod);
        HotSpotResolvedJavaMethod method = CTVMUtilities
                .getResolvedMethod(aMethod);
        ProfilingInfo startProfile = method.getProfilingInfo();
        Asserts.assertFalse(startProfile.isMature(), aMethod
                + " : profiling info is mature in the beginning");

        // make interpreter to profile this method
        try {
            Object obj = aMethod.getDeclaringClass().newInstance();
            for (long i = 0; i < CompilerWhiteBoxTest.THRESHOLD; i++) {
                aMethod.invoke(obj);
            }
        } catch (ReflectiveOperationException e) {
            throw new Error("TEST BUG : " + e.getMessage(), e);
        }
        ProfilingInfo compProfile = method.getProfilingInfo();

        Asserts.assertNE(startProfile.toString(), compProfile.toString(),
                String.format("%s : profiling info wasn't changed after "
                                + "%d invocations",
                        aMethod, CompilerWhiteBoxTest.THRESHOLD));
        Asserts.assertTrue(compProfile.isMature(),
                String.format("%s is not mature after %d invocations",
                        aMethod, CompilerWhiteBoxTest.THRESHOLD));

        CompilerToVMHelper.reprofile(method);
        ProfilingInfo reprofiledProfile = method.getProfilingInfo();

        Asserts.assertNE(startProfile.toString(), reprofiledProfile.toString(),
                aMethod + " : profiling info wasn't changed after reprofiling");
        Asserts.assertNE(compProfile.toString(), reprofiledProfile.toString(),
                aMethod + " : profiling info didn't change after reprofile");
        Asserts.assertFalse(reprofiledProfile.isMature(), aMethod
                + " : profiling info is mature after reprofiling");
    }
}
