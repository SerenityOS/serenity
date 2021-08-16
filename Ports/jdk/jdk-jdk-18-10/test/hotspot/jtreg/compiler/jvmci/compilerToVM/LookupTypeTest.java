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
 * @modules java.base/jdk.internal.access
 * @modules jdk.internal.vm.ci/jdk.vm.ci.hotspot
 * @build jdk.internal.vm.ci/jdk.vm.ci.hotspot.CompilerToVMHelper
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI
 *                   -XX:-UseJVMCICompiler
 *                   compiler.jvmci.compilerToVM.LookupTypeTest
 */

package compiler.jvmci.compilerToVM;

import compiler.jvmci.common.testcases.DoNotExtendClass;
import compiler.jvmci.common.testcases.MultiSubclassedClass;
import compiler.jvmci.common.testcases.SingleSubclass;
import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;
import jdk.vm.ci.hotspot.CompilerToVMHelper;
import jdk.vm.ci.hotspot.HotSpotResolvedObjectType;

import java.util.HashSet;
import java.util.Set;

public class LookupTypeTest {
    public static void main(String args[]) {
        LookupTypeTest test = new LookupTypeTest();
        for (TestCase tcase : createTestCases()) {
            test.runTest(tcase);
        }
    }

    private static Set<TestCase> createTestCases() {
        Set<TestCase> result = new HashSet<>();
        // a primitive class
        result.add(new TestCase(Utils.toJVMTypeSignature(int.class),
                LookupTypeTest.class, true, false, InternalError.class));
        // lookup not existing class
        result.add(new TestCase("Lsome_not_existing;", LookupTypeTest.class,
                true, false, ClassNotFoundException.class));
        // lookup invalid classname
        result.add(new TestCase("L!@#$%^&**()[]{}?;", LookupTypeTest.class,
                true, false, ClassNotFoundException.class));
        // lookup package private class
        result.add(new TestCase(
                "Lcompiler/jvmci/compilerToVM/testcases/PackagePrivateClass;",
                LookupTypeTest.class, true, false,
                ClassNotFoundException.class));
        // lookup usual class with resolve=true
        result.add(new TestCase(Utils.toJVMTypeSignature(SingleSubclass.class),
                LookupTypeTest.class, true, true));
        // lookup usual class with resolve=false
        result.add(new TestCase(
                Utils.toJVMTypeSignature(DoNotExtendClass.class),
                LookupTypeTest.class, false, true));
        // lookup usual class with null accessor
        result.add(new TestCase(
                Utils.toJVMTypeSignature(MultiSubclassedClass.class), null,
                false, false, NullPointerException.class));
        return result;
    }

    private void runTest(TestCase tcase) {
        System.out.println(tcase);
        HotSpotResolvedObjectType metaspaceKlass;
        try {
            metaspaceKlass = CompilerToVMHelper.lookupType(tcase.className,
                    tcase.accessing, tcase.resolve);
        } catch (Throwable t) {
            Asserts.assertNotNull(tcase.expectedException,
                    "Assumed no exception, but got " + t);
            Asserts.assertFalse(tcase.isPositive,
                    "Got unexpected exception " + t);
            Asserts.assertEQ(t.getClass(), tcase.expectedException,
                    "Unexpected exception");
            // passed
            return;
        }
        if (tcase.expectedException != null) {
            throw new AssertionError("Expected exception was not thrown: "
                    + tcase.expectedException.getName());
        }
        if (tcase.isPositive) {
            Asserts.assertNotNull(metaspaceKlass,
                    "Unexpected null metaspace klass");
            Asserts.assertEQ(metaspaceKlass.getName(), tcase.className,
                    "Got unexpected resolved class name");
        } else {
            Asserts.assertNull(metaspaceKlass, "Unexpected metaspace klass");
        }
    }

    private static class TestCase {
        public final String className;
        public final Class<?> accessing;
        public final boolean resolve;
        public final boolean isPositive;
        public final Class<? extends Throwable> expectedException;

        public TestCase(String className, Class<?> accessing, boolean resolve,
                boolean isPositive,
                Class<? extends Throwable> expectedException) {
            this.className = className;
            this.accessing = accessing;
            this.resolve = resolve;
            this.isPositive = isPositive;
            this.expectedException = expectedException;
        }

        public TestCase(String className, Class<?> accessing, boolean resolve,
                boolean isPositive) {
            this.className = className;
            this.accessing = accessing;
            this.resolve = resolve;
            this.isPositive = isPositive;
            this.expectedException = null;
        }

        @Override
        public String toString() {
            return String.format("CASE: class=%s, accessing=%s,"
                + " resolve=%s, positive=%s, expectedException=%s", className,
                accessing, resolve, isPositive, expectedException);
        }
    }
}
