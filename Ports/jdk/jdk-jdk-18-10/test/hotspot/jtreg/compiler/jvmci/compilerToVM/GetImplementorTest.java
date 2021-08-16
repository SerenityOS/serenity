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
 * @library / /test/lib/
 * @library ../common/patches
 * @modules java.base/jdk.internal.misc
 * @modules jdk.internal.vm.ci/jdk.vm.ci.hotspot
 * @build jdk.internal.vm.ci/jdk.vm.ci.hotspot.CompilerToVMHelper
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI
 *                   -XX:-UseJVMCICompiler
 *                   compiler.jvmci.compilerToVM.GetImplementorTest
 */

package compiler.jvmci.compilerToVM;

import compiler.jvmci.common.testcases.AbstractClass;
import compiler.jvmci.common.testcases.AbstractClassExtender;
import compiler.jvmci.common.testcases.DoNotExtendClass;
import compiler.jvmci.common.testcases.DoNotImplementInterface;
import compiler.jvmci.common.testcases.MultipleImplementer1;
import compiler.jvmci.common.testcases.MultipleImplementer2;
import compiler.jvmci.common.testcases.MultipleImplementersInterface;
import compiler.jvmci.common.testcases.SingleImplementer;
import compiler.jvmci.common.testcases.SingleImplementerInterface;
import compiler.jvmci.common.testcases.SingleSubclass;
import compiler.jvmci.common.testcases.SingleSubclassedClass;
import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;
import jdk.vm.ci.hotspot.CompilerToVMHelper;
import jdk.vm.ci.hotspot.HotSpotResolvedObjectType;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class GetImplementorTest {
    public static void main(String args[]) {
        GetImplementorTest test = new GetImplementorTest();
        for (TestCase tcase : createTestCases()) {
            test.runTest(tcase);
        }
    }

    private static Set<TestCase> createTestCases() {
        Set<TestCase> result = new HashSet<>();
        Stream.of(
                    SingleSubclass.class,
                    AbstractClassExtender.class,
                    MultipleImplementer2.class,
                    MultipleImplementer1.class,
                    MultipleImplementersInterface.class,
                    DoNotImplementInterface.class,
                    DoNotExtendClass.class,
                    AbstractClass.class,
                    SingleSubclassedClass.class)
                .forEach(Utils::ensureClassIsLoaded);
        // an interface with single class implementing it
        result.add(new TestCase(SingleImplementerInterface.class,
                SingleImplementer.class));
        /* an interface with multiple implementers. According to getImplementor
           javadoc, an itself should be returned in case of more than one
           implementor
         */
        result.add(new TestCase(MultipleImplementersInterface.class,
                MultipleImplementersInterface.class));
        // an interface with no implementors
        result.add(new TestCase(DoNotImplementInterface.class, null));
        // an abstract class with extender class
        result.add(new TestCase(AbstractClass.class, null));
        // a simple class, which is not extended
        result.add(new TestCase(DoNotExtendClass.class, null));
        // a usual class, which is extended
        result.add(new TestCase(SingleSubclassedClass.class, null));
        return result;
    }

    private void runTest(TestCase tcase) {
        System.out.println(tcase);
        HotSpotResolvedObjectType resolvedIface = CompilerToVMHelper
                .lookupTypeHelper(Utils.toJVMTypeSignature(tcase.anInterface),
                        getClass(), /* resolve = */ true);
        if (!resolvedIface.isInterface()) {
            try {
                CompilerToVMHelper.getImplementor(resolvedIface);
                Asserts.fail("Expected " + IllegalArgumentException.class.getName());
            } catch (IllegalArgumentException e) {
            }
            return;
        }
        HotSpotResolvedObjectType resolvedImplementer = CompilerToVMHelper
                .getImplementor(resolvedIface);
        HotSpotResolvedObjectType resolvedExpected = null;
        if (tcase.expectedImplementer != null) {
            resolvedExpected = CompilerToVMHelper.lookupTypeHelper(Utils
                    .toJVMTypeSignature(tcase.expectedImplementer),
                    getClass(), /* resolve = */ true);
        }
        Asserts.assertEQ(resolvedImplementer, resolvedExpected,
                "Unexpected implementer for " + tcase.anInterface.getName());
    }

    private static class TestCase {
        public final Class<?> anInterface;
        public final Class<?> expectedImplementer;

        public TestCase(Class<?> iface, Class<?> expectedImplementer) {
            this.anInterface = iface;
            this.expectedImplementer = expectedImplementer;
        }

        @Override
        public String toString() {
            return String.format("CASE: interface=%s, expected=%s",
                    anInterface.getName(),
                    expectedImplementer == null
                            ? null
                            : expectedImplementer.getName());
        }
    }
}
