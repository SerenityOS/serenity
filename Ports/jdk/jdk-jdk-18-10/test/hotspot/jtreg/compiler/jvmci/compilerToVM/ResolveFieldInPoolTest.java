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
 *          java.base/jdk.internal.misc
 *          java.base/jdk.internal.reflect
 *          java.base/jdk.internal.org.objectweb.asm
 *          java.base/jdk.internal.org.objectweb.asm.tree
 *          jdk.internal.vm.ci/jdk.vm.ci.hotspot
 *          jdk.internal.vm.ci/jdk.vm.ci.meta
 *          jdk.internal.vm.ci/jdk.vm.ci.runtime
 *
 * @build jdk.internal.vm.ci/jdk.vm.ci.hotspot.CompilerToVMHelper sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI
 *                   -XX:-UseJVMCICompiler
 *                   compiler.jvmci.compilerToVM.ResolveFieldInPoolTest
 */

package compiler.jvmci.compilerToVM;

import compiler.jvmci.compilerToVM.ConstantPoolTestCase.ConstantTypes;
import compiler.jvmci.compilerToVM.ConstantPoolTestCase.TestedCPEntry;
import compiler.jvmci.compilerToVM.ConstantPoolTestCase.Validator;
import compiler.jvmci.compilerToVM.ConstantPoolTestsHelper.DummyClasses;
import jdk.internal.misc.Unsafe;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.test.lib.Asserts;
import jdk.vm.ci.hotspot.CompilerToVMHelper;
import jdk.vm.ci.hotspot.HotSpotResolvedObjectType;
import jdk.vm.ci.meta.ConstantPool;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static compiler.jvmci.compilerToVM.ConstantPoolTestCase.ConstantTypes.CONSTANT_FIELDREF;

/**
 * Test for {@code jdk.vm.ci.hotspot.CompilerToVM.resolveFieldInPool} method
 */
public class ResolveFieldInPoolTest {

    private static final Unsafe UNSAFE = Unsafe.getUnsafe();

    public static void main(String[] args) throws Exception {
        Map<ConstantTypes, Validator> typeTests = new HashMap<>();
        typeTests.put(CONSTANT_FIELDREF, ResolveFieldInPoolTest::validate);
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
        for (int j = 0; j < entry.opcodes.length; j++) {
            int[] info = new int[3];
            HotSpotResolvedObjectType fieldToVerify
                    = CompilerToVMHelper.resolveFieldInPool(constantPoolCTVM,
                                                           index,
                                                           entry.methods == null ? null : entry.methods[j],
                                                           entry.opcodes[j],
                                                           info);
            String msg = String.format("Object returned by resolveFieldInPool method"
                                               + " for %sindex %d  should not be null",
                                       cached,
                                       index);
            Asserts.assertNotNull(fieldToVerify, msg);
            String classNameToRefer = entry.klass;
            String fieldToVerifyKlassToString = fieldToVerify.klass().toValueString();
            if (!fieldToVerifyKlassToString.contains(classNameToRefer)) {
                msg = String.format("String representation \"%s\" of the object"
                                            + " returned by resolveFieldInPool method"
                                            + " for index %d does not contain a field's class name,"
                                            + " should contain %s",
                                    fieldToVerifyKlassToString,
                                    index,
                                    classNameToRefer);
                throw new AssertionError(msg);
            }
            msg = String.format("Access flags returned by resolveFieldInPool"
                                        + " method are wrong for the field %s.%s"
                                        + " at %sindex %d",
                                entry.klass,
                                entry.name,
                                cached,
                                index);
            Asserts.assertEQ(info[0], entry.accFlags, msg);
            if (cpci == -1) {
                return;
            }
            Class classOfTheField = null;
            Field fieldToRefer = null;
            try {
                classOfTheField = Class.forName(classNameToRefer.replaceAll("/", "\\."));
                fieldToRefer = classOfTheField.getDeclaredField(entry.name);
                fieldToRefer.setAccessible(true);
            } catch (Exception ex) {
                throw new Error("Unexpected exception", ex);
            }
            int offsetToRefer;
            if ((entry.accFlags & Opcodes.ACC_STATIC) != 0) {
                offsetToRefer = (int) UNSAFE.staticFieldOffset(fieldToRefer);
            } else {
                offsetToRefer = (int) UNSAFE.objectFieldOffset(fieldToRefer);
            }
            msg = String.format("Field offset returned by resolveFieldInPool"
                                        + " method is wrong for the field %s.%s"
                                        + " at %sindex %d",
                                entry.klass,
                                entry.name,
                                cached,
                                index);
            Asserts.assertEQ(info[1], offsetToRefer, msg);
        }
    }
}
