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
 *                   compiler.jvmci.compilerToVM.ResolveMethodTest
 */

package compiler.jvmci.compilerToVM;

import compiler.jvmci.common.CTVMUtilities;
import compiler.jvmci.common.testcases.AbstractClass;
import compiler.jvmci.common.testcases.AbstractClassExtender;
import compiler.jvmci.common.testcases.MultipleImplementer1;
import compiler.jvmci.common.testcases.MultipleImplementer2;
import compiler.jvmci.common.testcases.MultipleImplementersInterface;
import compiler.jvmci.common.testcases.SingleImplementer;
import compiler.jvmci.common.testcases.SingleImplementerInterface;
import compiler.jvmci.common.testcases.SingleSubclass;
import compiler.jvmci.common.testcases.SingleSubclassedClass;
import jdk.internal.misc.Unsafe;
import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;
import jdk.vm.ci.hotspot.CompilerToVMHelper;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod;
import jdk.vm.ci.hotspot.HotSpotResolvedObjectType;

import java.util.HashSet;
import java.util.Set;

public class ResolveMethodTest {
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();

    public static void main(String args[]) {
        ResolveMethodTest test = new ResolveMethodTest();
        // positive cases
        try {
            for (TestCase tcase: createTestCases()) {
                test.runTest(tcase);
            }
        } catch (NoSuchMethodException e) {
            throw new Error("TEST BUG: can't find requested method", e);
        }
    }

    private static Set<TestCase> createTestCases() {
        Set<TestCase> result = new HashSet<>();
        // a usual class public method
        result.add(new TestCase(SingleSubclass.class, SingleSubclass.class,
                "usualMethod", ResolveMethodTest.class, true));
        // an array method
        result.add(new TestCase(int[].class, Object.class, "toString",
                ResolveMethodTest.class, true));
        // a method from base class, which was overriden in tested one
        result.add(new TestCase(SingleSubclass.class, SingleSubclass.class,
                "overridenMethod", ResolveMethodTest.class, true));
        // a method from base class, which was not overriden in tested one
        result.add(new TestCase(SingleSubclass.class,
                SingleSubclassedClass.class, "inheritedMethod",
                ResolveMethodTest.class, true));
        /* a method from base class, which was overriden in tested one with
           base class as holder */
        result.add(new TestCase(SingleSubclass.class,
                SingleSubclassedClass.class, "overridenMethod",
                ResolveMethodTest.class, true));
        // an interface method
        result.add(new TestCase(SingleImplementer.class,
                SingleImplementerInterface.class, "interfaceMethod",
                ResolveMethodTest.class, true));
        // an interface default method overriden in implementer
        result.add(new TestCase(MultipleImplementer1.class,
                MultipleImplementersInterface.class, "defaultMethod",
                ResolveMethodTest.class, true));
        // an interface default method not overriden in implementer
        result.add(new TestCase(MultipleImplementer2.class,
                MultipleImplementersInterface.class, "defaultMethod",
                ResolveMethodTest.class, true));
        // an abstract method
        result.add(new TestCase(AbstractClassExtender.class, AbstractClass.class,
                "abstractMethod", ResolveMethodTest.class, true));
        // private method with right accessor
        result.add(new TestCase(SingleSubclass.class, SingleSubclass.class,
                "privateMethod", SingleSubclass.class, true));
        // package-private method with right accessor
        result.add(new TestCase(SingleSubclass.class, SingleSubclass.class,
                "defaultAccessMethod", SingleSubclass.class, true));

        // negative cases

        // private method of another class
        result.add(new TestCase(SingleSubclass.class, SingleSubclass.class,
                "privateMethod", ResolveMethodTest.class, false));
        // package-private method from another package
        result.add(new TestCase(SingleSubclass.class, SingleSubclass.class,
                "defaultAccessMethod", ResolveMethodTest.class, false));
        return result;
    }

    private void runTest(TestCase tcase) throws NoSuchMethodException {
        System.out.println(tcase);
        HotSpotResolvedJavaMethod metaspaceMethod = CTVMUtilities
                .getResolvedMethod(tcase.holder,
                        tcase.holder.getDeclaredMethod(tcase.methodName));
        HotSpotResolvedObjectType holderMetaspace = CompilerToVMHelper
                .lookupTypeHelper(Utils.toJVMTypeSignature(tcase.holder),
                        getClass(), /* resolve = */ true);
        HotSpotResolvedObjectType callerMetaspace = CompilerToVMHelper
                .lookupTypeHelper(Utils.toJVMTypeSignature(tcase.caller),
                        getClass(), /* resolve = */ true);
        HotSpotResolvedObjectType receiverMetaspace = CompilerToVMHelper
                .lookupTypeHelper(Utils.toJVMTypeSignature(tcase.receiver),
                        getClass(), /* resolve = */ true);

        // Can only resolve methods on a linked class so force initialization
        receiverMetaspace.initialize();
        HotSpotResolvedJavaMethod resolvedMetaspaceMethod
                = CompilerToVMHelper.resolveMethod(receiverMetaspace,
                        metaspaceMethod, callerMetaspace);
        if (tcase.isPositive) {
            Asserts.assertNotNull(resolvedMetaspaceMethod,
                    "Unexpected null resolved method value for "
                            + tcase.methodName);
            Asserts.assertEQ(metaspaceMethod.getName(), tcase.methodName,
                    "Reflection and c2vm method names doesn't match");
        } else {
            Asserts.assertNull(resolvedMetaspaceMethod,
                    "Method unexpectedly resolved");
        }
    }

    private static class TestCase {
        public final Class<?> receiver;
        public final Class<?> holder;
        public final Class<?> caller;
        public final String methodName;
        public final boolean isPositive;

        public TestCase(Class<?> recv, Class<?> holder, String methodName,
                Class<?> caller, boolean isPositive) {
            this.receiver = recv;
            this.holder = holder;
            this.caller = caller;
            this.methodName = methodName;
            this.isPositive = isPositive;
        }

        @Override
        public String toString() {
            return String.format("CASE: receiver=%s, holder=%s, method=%s,"
                + "caller=%s, isPositive=%s%n", receiver.getName(),
                holder.getName(), methodName, caller.getName(), isPositive);
        }
    }
}
