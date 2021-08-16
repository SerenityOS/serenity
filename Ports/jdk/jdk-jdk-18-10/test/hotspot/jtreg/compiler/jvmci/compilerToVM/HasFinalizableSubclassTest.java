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
 *                   compiler.jvmci.compilerToVM.HasFinalizableSubclassTest
 */

package compiler.jvmci.compilerToVM;

import compiler.jvmci.common.testcases.AbstractClass;
import compiler.jvmci.common.testcases.AbstractClassExtender;
import compiler.jvmci.common.testcases.DoNotImplementInterface;
import compiler.jvmci.common.testcases.MultipleImplementer1;
import compiler.jvmci.common.testcases.MultipleImplementersInterface;
import compiler.jvmci.common.testcases.SingleImplementerInterface;
import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;
import jdk.vm.ci.hotspot.CompilerToVMHelper;
import jdk.vm.ci.hotspot.HotSpotResolvedObjectType;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class HasFinalizableSubclassTest {
    public static void main(String args[]) {
        HasFinalizableSubclassTest test = new HasFinalizableSubclassTest();
        for (TestCase tcase : createTestCases()) {
            test.runTest(tcase);
        }
    }

    private static Set<TestCase> createTestCases() {
        Stream.of(
                    AbstractClassExtender.class,
                    SingleImplementerInterface.class,
                    MultipleImplementersInterface.class,
                    MultipleImplementer1.class,
                    DoNotImplementInterface.class)
                .forEach(Utils::ensureClassIsLoaded);
        Set<TestCase> result = new HashSet<>();
        // iface with finalize method
        result.add(new TestCase(SingleImplementerInterface.class, false));
        // iface with default finalize method
        result.add(new TestCase(MultipleImplementersInterface.class, false));
        // class which implements iface w/ default finalize method
        result.add(new TestCase(MultipleImplementer1.class, true));
        // abstract class with finalizeable subclass
        result.add(new TestCase(AbstractClass.class, true));
        // non-implemented iface
        result.add(new TestCase(DoNotImplementInterface.class, false));
        return result;
    }

    private void runTest(TestCase tcase) {
        System.out.println(tcase);
        HotSpotResolvedObjectType metaspaceKlass = CompilerToVMHelper
                .lookupTypeHelper(Utils.toJVMTypeSignature(tcase.aClass),
                        getClass(), /* resolve = */ true);
        Asserts.assertEQ(tcase.expected,
                CompilerToVMHelper.hasFinalizableSubclass(metaspaceKlass),
                        "Unexpected finalizableSubclass state for "
                                + tcase.aClass.getName());
    }

    private static class TestCase {
        public final Class<?> aClass;
        public final boolean expected;

        public TestCase(Class<?> clazz, boolean expected) {
            this.aClass = clazz;
            this.expected = expected;
        }
        @Override
        public String toString() {
            return "CASE: class= " + aClass.getName() + ", expected=" + expected;
        }
    }
}
