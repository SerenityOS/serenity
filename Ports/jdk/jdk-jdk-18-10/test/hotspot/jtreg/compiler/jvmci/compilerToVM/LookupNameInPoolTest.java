/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8138708
 * @requires vm.jvmci
 * @library /test/lib /
 * @library ../common/patches
 * @modules java.base/jdk.internal.access
 *          java.base/jdk.internal.reflect
 *          java.base/jdk.internal.org.objectweb.asm
 *          java.base/jdk.internal.org.objectweb.asm.tree
 *          jdk.internal.vm.ci/jdk.vm.ci.hotspot
 *          jdk.internal.vm.ci/jdk.vm.ci.runtime
 *          jdk.internal.vm.ci/jdk.vm.ci.meta
 *
 * @build jdk.internal.vm.ci/jdk.vm.ci.hotspot.CompilerToVMHelper
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI
 *                   -XX:-UseJVMCICompiler
 *                   compiler.jvmci.compilerToVM.LookupNameInPoolTest
 */

package compiler.jvmci.compilerToVM;

import compiler.jvmci.compilerToVM.ConstantPoolTestCase.ConstantTypes;
import compiler.jvmci.compilerToVM.ConstantPoolTestCase.TestedCPEntry;
import compiler.jvmci.compilerToVM.ConstantPoolTestCase.Validator;
import compiler.jvmci.compilerToVM.ConstantPoolTestsHelper.DummyClasses;
import jdk.test.lib.Asserts;
import jdk.vm.ci.hotspot.CompilerToVMHelper;
import jdk.vm.ci.meta.ConstantPool;

import java.util.HashMap;
import java.util.Map;

import static compiler.jvmci.compilerToVM.ConstantPoolTestCase.ConstantTypes.CONSTANT_FIELDREF;
import static compiler.jvmci.compilerToVM.ConstantPoolTestCase.ConstantTypes.CONSTANT_INTERFACEMETHODREF;
import static compiler.jvmci.compilerToVM.ConstantPoolTestCase.ConstantTypes.CONSTANT_INVOKEDYNAMIC;
import static compiler.jvmci.compilerToVM.ConstantPoolTestCase.ConstantTypes.CONSTANT_METHODREF;

/**
 * Test for {@code jdk.vm.ci.hotspot.CompilerToVM.lookupNameInPool} method
 */
public class LookupNameInPoolTest {

    public static void main(String[] args) throws Exception {
        Map<ConstantTypes, Validator> typeTests = new HashMap<>();
        typeTests.put(CONSTANT_METHODREF, LookupNameInPoolTest::validate);
        typeTests.put(CONSTANT_INTERFACEMETHODREF, LookupNameInPoolTest::validate);
        typeTests.put(CONSTANT_FIELDREF, LookupNameInPoolTest::validate);
        typeTests.put(CONSTANT_INVOKEDYNAMIC, LookupNameInPoolTest::validate);
        ConstantPoolTestCase testCase = new ConstantPoolTestCase(typeTests);
        testCase.test();
        // The next "Class.forName" and repeating "testCase.test()"
        // are here for the following reason.
        // The first test run is without dummy class initialization,
        // which means no constant pool cache exists.
        // The second run is with initialized class (with constant pool cache available).
        // Some CompilerToVM methods require different input
        // depending on whether CP cache exists or not.
        for (DummyClasses dummy : DummyClasses.values()) {
            Class.forName(dummy.klass.getName());
        }
        testCase.test();
    }

    private static void validate(ConstantPool constantPoolCTVM,
                                 ConstantTypes cpType,
                                 DummyClasses dummyClass,
                                 int cpi) {
        TestedCPEntry entry = cpType.getTestedCPEntry(dummyClass, cpi);
        if (entry == null) {
            return;
        }
        int index = cpi;
        String cached = "";
        int cpci = dummyClass.getCPCacheIndex(cpi);
        if (cpci != ConstantPoolTestsHelper.NO_CP_CACHE_PRESENT) {
            index = cpci;
            cached = "cached ";
        }
        String nameToVerify = CompilerToVMHelper.lookupNameInPool(constantPoolCTVM, index);
        String nameToRefer = entry.name;
        String msg = String.format("Wrong name accessed by %sconstant pool index %d", cached, index);
        Asserts.assertEQ(nameToVerify, nameToRefer, msg);
    }
}
