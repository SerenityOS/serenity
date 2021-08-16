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
 *                   compiler.jvmci.compilerToVM.GetVtableIndexForInterfaceTest
 */

package compiler.jvmci.compilerToVM;

import compiler.jvmci.common.CTVMUtilities;
import compiler.jvmci.common.testcases.AbstractClass;
import compiler.jvmci.common.testcases.AnotherSingleImplementer;
import compiler.jvmci.common.testcases.AnotherSingleImplementerInterface;
import compiler.jvmci.common.testcases.DoNotExtendClass;
import compiler.jvmci.common.testcases.MultipleAbstractImplementer;
import compiler.jvmci.common.testcases.MultipleImplementersInterface;
import compiler.jvmci.common.testcases.MultipleImplementersInterfaceExtender;
import compiler.jvmci.common.testcases.SingleImplementer;
import compiler.jvmci.common.testcases.SingleImplementerInterface;
import compiler.jvmci.common.testcases.SingleSubclass;
import compiler.jvmci.common.testcases.SingleSubclassedClass;
import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;
import jdk.vm.ci.hotspot.CompilerToVMHelper;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod;
import jdk.vm.ci.hotspot.HotSpotResolvedObjectType;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class GetVtableIndexForInterfaceTest {
    private static final int INVALID_VTABLE_INDEX = -4; // see method.hpp: VtableIndexFlag

    public static void main(String args[]) {
        GetVtableIndexForInterfaceTest test
                = new GetVtableIndexForInterfaceTest();
        try {
            for (TestCase tcase : createTestCases()) {
                test.runTest(tcase);
            }
        } catch (NoSuchMethodException e) {
            throw new Error("TEST BUG: can't find requested method", e);
        }
    }

    private static Set<TestCase> createTestCases() {
        Set<TestCase> result = new HashSet<>();
        Stream.of(
                    AbstractClass.class,
                    SingleImplementer.class,
                    SingleImplementerInterface.class,
                    MultipleImplementersInterface.class,
                    MultipleImplementersInterfaceExtender.class,
                    SingleSubclass.class,
                    SingleSubclassedClass.class,
                    DoNotExtendClass.class,
                    MultipleAbstractImplementer.class
                )
                .forEach(Utils::ensureClassIsLoaded);
        // non iface method
        result.add(new TestCase(SingleImplementer.class,
                SingleImplementer.class, "nonInterfaceMethod",
                false, InternalError.class));
        // iface method w/o default implementation
        result.add(new TestCase(SingleImplementer.class,
                SingleImplementerInterface.class, "interfaceMethod", false));
        /* another iface which provides default implementation for the
           original iface*/
        result.add(new TestCase(MultipleImplementersInterfaceExtender.class,
                MultipleImplementersInterface.class, "testMethod", false,
                InternalError.class));
        // iface method w/ default implementation
        result.add(new TestCase(SingleImplementer.class,
                SingleImplementerInterface.class, "defaultMethod", true));
        // non iface class
        result.add(new TestCase(SingleSubclass.class,
                SingleSubclassedClass.class, "inheritedMethod", false,
                InternalError.class));
        // class not implementing iface
        result.add(new TestCase(DoNotExtendClass.class,
                SingleImplementerInterface.class, "defaultMethod", false,
                InternalError.class));
        // abstract class which doesn't implement iface
        result.add(new TestCase(AbstractClass.class,
                SingleImplementerInterface.class, "defaultMethod", false,
                InternalError.class));
        // abstract class which implements iface
        result.add(new TestCase(MultipleAbstractImplementer.class,
                MultipleImplementersInterface.class, "defaultMethod", true));
        // class not initialized
        result.add(new TestCase(AnotherSingleImplementer.class,
                AnotherSingleImplementerInterface.class, "defaultMethod", false,
                InternalError.class));
        return result;
    }

    private void runTest(TestCase tcase) throws NoSuchMethodException {
        System.out.println(tcase);
        Method method = tcase.holder.getDeclaredMethod(tcase.methodName);
        HotSpotResolvedObjectType metaspaceKlass = CompilerToVMHelper
                .lookupTypeHelper(Utils.toJVMTypeSignature(tcase.receiver),
                        getClass(), /* resolve = */ true);
        HotSpotResolvedJavaMethod metaspaceMethod = CTVMUtilities
                .getResolvedMethod(tcase.holder, method);
        int index = 0;
        try {
            index = CompilerToVMHelper
                    .getVtableIndexForInterfaceMethod(metaspaceKlass,
                            metaspaceMethod);
        } catch (Throwable t) {
            if (tcase.isPositive || tcase.expectedException == null) {
                throw new Error("Caught unexpected exception " + t);
            }
            if (!tcase.expectedException.equals(t.getClass())) {
                throw new Error(String.format("Caught %s while expected %s",
                        t.getClass().getName(),
                        tcase.expectedException.getName()));
            }
            return;
        }
        if (tcase.expectedException != null) {
            throw new AssertionError("Expected exception wasn't caught: "
                    + tcase.expectedException.getName());
        }
        if (tcase.isPositive) {
            Asserts.assertNE(index, INVALID_VTABLE_INDEX,
                    "Unexpected: got invalid index");
        } else {
            Asserts.assertEQ(index, INVALID_VTABLE_INDEX,
                    "Unexpected: got valid index ");
        }
    }

    private static class TestCase {
        public final Class<?> receiver;
        public final Class<?> holder;
        public final String methodName;
        public final boolean isPositive;
        public final Class<? extends Throwable> expectedException;

        public TestCase(Class<?> receiver, Class<?> holder, String methodName,
                boolean isPositive,
                Class<? extends Throwable> expectedException) {
            this.receiver = receiver;
            this.holder = holder;
            this.methodName = methodName;
            this.isPositive = isPositive;
            this.expectedException = expectedException;
        }

        public TestCase(Class<?> receiver, Class<?> holder, String methodName,
                boolean isPositive) {
            this(receiver, holder, methodName, isPositive, null);
        }

        @Override
        public String toString() {
            return String.format("CASE: receiver=%s, holder=%s, method=%s,"
                    + " isPositive=%s%n", receiver.getName(), holder.getName(),
                    methodName, isPositive);
        }
    }
}
