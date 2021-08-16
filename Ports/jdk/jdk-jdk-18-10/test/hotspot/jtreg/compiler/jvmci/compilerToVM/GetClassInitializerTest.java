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

/*
 * @test
 * @bug 8136421
 * @requires vm.jvmci
 * @library / /test/lib
 * @library ../common/patches
 * @modules java.base/jdk.internal.misc
 * @modules jdk.internal.vm.ci/jdk.vm.ci.hotspot
 * @build jdk.internal.vm.ci/jdk.vm.ci.hotspot.CompilerToVMHelper
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI
 *                   -XX:-UseJVMCICompiler
 *                   compiler.jvmci.compilerToVM.GetClassInitializerTest
 */

package compiler.jvmci.compilerToVM;

import compiler.jvmci.common.testcases.AbstractClass;
import compiler.jvmci.common.testcases.AbstractClassExtender;
import compiler.jvmci.common.testcases.DoNotExtendClass;
import compiler.jvmci.common.testcases.MultipleImplementersInterfaceExtender;
import compiler.jvmci.common.testcases.SingleImplementer;
import compiler.jvmci.common.testcases.SingleImplementerInterface;
import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;
import jdk.vm.ci.hotspot.CompilerToVMHelper;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod;
import jdk.vm.ci.hotspot.HotSpotResolvedObjectType;

import java.util.HashSet;
import java.util.Set;

public class GetClassInitializerTest {

    public static void main(String args[]) {
        GetClassInitializerTest test = new GetClassInitializerTest();
        for (TestCase tcase : createTestCases()) {
            test.runTest(tcase);
        }
    }

    private static Set<TestCase> createTestCases() {
        Set<TestCase> result = new HashSet<>();
        // a simple class with initializer
        result.add(new TestCase(SingleImplementer.class, true));
        // an interface with initializer
        result.add(new TestCase(SingleImplementerInterface.class, true));
        // an abstract class with initializer
        result.add(new TestCase(AbstractClass.class, true));
        // a class without initializer, extending class with initializer
        result.add(new TestCase(AbstractClassExtender.class, false));
        // an interface without initializer
        result.add(new TestCase(MultipleImplementersInterfaceExtender.class, false));
        // a class without initializer
        result.add(new TestCase(DoNotExtendClass.class, false));
        return result;
    }

    private void runTest(TestCase tcase) {
        System.out.println(tcase);
        String className = tcase.holder.getName();
        HotSpotResolvedObjectType resolvedClazz = CompilerToVMHelper
                .lookupTypeHelper(Utils.toJVMTypeSignature(tcase.holder),
                        getClass(), /* resolve = */ true);
        HotSpotResolvedJavaMethod initializer = CompilerToVMHelper
                .getClassInitializer(resolvedClazz);
        if (tcase.isPositive) {
            Asserts.assertNotNull(initializer, "Couldn't get initializer for "
                    + className);
            Asserts.assertEQ(initializer.getName(), "<clinit>",
                    "Unexpected initializer name for " + className);
        } else {
            Asserts.assertNull(initializer, "Unexpected: found initializer for "
                    + className);
        }
    }

    private static class TestCase {
        public final Class<?> holder;
        public final boolean isPositive;

        public TestCase(Class<?> clazz, boolean isPositive) {
            this.holder = clazz;
            this.isPositive = isPositive;
        }

        @Override
        public String toString() {
            return "CASE: clazz=" + holder.getName()
                    + ", isPositive=" + isPositive;
        }
    }
}
