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
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -XX:-UseJVMCICompiler
 *                   compiler.jvmci.compilerToVM.AsResolvedJavaMethodTest
 */

package compiler.jvmci.compilerToVM;

import jdk.test.lib.Asserts;
import jdk.vm.ci.hotspot.CompilerToVMHelper;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod;

import java.lang.reflect.Executable;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class AsResolvedJavaMethodTest {

    private static class A {
        {
            System.out.println("Dummy");
        }
        public void f1() {}
        public int f2() { return 0; }
        public String f3() { return ""; }
    }


    private static class S {
        static {
            System.out.println("Dummy static");
        }
        public S() {}
        public void f1() {}
        public int f2() { return 0; }
        public String f3() { return ""; }
    }

    private class B extends A {
        public void f4() {}
    }

    private interface I {
        void f1();
        int f2();
        String f3();
    }

    public static void main(String[] args) {
        List<Class<?>> testCases = getTestCases();
        testCases.forEach(AsResolvedJavaMethodTest::test);
    }

    private static List<Class<?>> getTestCases() {
        List<Class<?>> testCases = new ArrayList<>();
        testCases.add(A.class);
        testCases.add(S.class);
        testCases.add(I.class);
        testCases.add(B.class);
        return testCases;
    }

    private static void test(Class<?> aClass) {
        testCorrectMethods(aClass);
    }

    private static void testCorrectMethods(Class<?> holder) {
        List<Executable> executables = new ArrayList<>();
        executables.addAll(Arrays.asList(holder.getDeclaredMethods()));
        executables.addAll(Arrays.asList(holder.getDeclaredConstructors()));
        for (Executable executable : executables) {
            HotSpotResolvedJavaMethod method = CompilerToVMHelper
                    .asResolvedJavaMethod(executable);
            Asserts.assertNotNull(method, "could not convert " + method);
        }
    }
}
