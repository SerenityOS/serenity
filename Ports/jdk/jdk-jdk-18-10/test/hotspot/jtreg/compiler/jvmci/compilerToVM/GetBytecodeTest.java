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
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI
 *                   -XX:-UseJVMCICompiler
 *                   compiler.jvmci.compilerToVM.GetBytecodeTest
 */

package compiler.jvmci.compilerToVM;

import compiler.jvmci.common.CTVMUtilities;
import compiler.jvmci.common.testcases.TestCase;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.test.lib.Asserts;
import jdk.vm.ci.hotspot.CompilerToVMHelper;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod;

import java.lang.reflect.Executable;
import java.lang.reflect.Modifier;

public class GetBytecodeTest {

    public static void main(String[] args) {
        TestCase.getAllExecutables()
                .forEach(GetBytecodeTest::runSanityTest);
    }

    private static void runSanityTest(Executable aMethod) {
        HotSpotResolvedJavaMethod method = CTVMUtilities
                .getResolvedMethod(aMethod);
        byte[] bytecode = CompilerToVMHelper.getBytecode(method);

        int mods = aMethod.getModifiers();
        boolean shouldHasZeroLength = Modifier.isAbstract(mods)
                || Modifier.isNative(mods);
        boolean correctLength = (bytecode.length == 0 && shouldHasZeroLength)
                || (bytecode.length > 0 && !shouldHasZeroLength);

        Asserts.assertTrue(correctLength, "Bytecode of '" + aMethod + "' has "
                + bytecode.length + " length");

        if (!shouldHasZeroLength) {
            Asserts.assertTrue(containsReturn(bytecode), "Bytecode of '"
                    + aMethod + "' doesn't have any return statement");
        }
    }

    private static boolean containsReturn(byte[] bytecode) {
        for (byte b : bytecode) {
            //  cast unsigned byte to int
            int value = (int) b & 0x000000FF;
            switch (value) {
                case Opcodes.RET:
                case Opcodes.ARETURN:
                case Opcodes.IRETURN:
                case Opcodes.LRETURN:
                case Opcodes.FRETURN:
                case Opcodes.DRETURN:
                case Opcodes.RETURN:
                    return true;
            }
        }
        return false;
    }
}
