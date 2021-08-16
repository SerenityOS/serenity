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
 *                   compiler.jvmci.compilerToVM.GetLineNumberTableTest
 */

package compiler.jvmci.compilerToVM;

import compiler.jvmci.common.CTVMUtilities;
import compiler.jvmci.common.testcases.TestCase;
import jdk.test.lib.Asserts;
import jdk.vm.ci.hotspot.CompilerToVMHelper;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod;

import java.lang.reflect.Executable;
import java.util.Arrays;
import java.util.Map;

public class GetLineNumberTableTest {
    public static void main(String[] args) {
        TestCase.getAllExecutables()
                .forEach(GetLineNumberTableTest::runSanityTest);
    }

    public static void runSanityTest(Executable aMethod) {
        HotSpotResolvedJavaMethod method = CTVMUtilities
                .getResolvedMethod(aMethod);
        long[] lineNumbers = CompilerToVMHelper.getLineNumberTable(method);
        long[] expectedLineNumbers = getExpectedLineNumbers(aMethod);

        Asserts.assertTrue(Arrays.equals(lineNumbers, expectedLineNumbers),
                String.format("%s : unequal table values : %n%s%n%s%n",
                        aMethod,
                        Arrays.toString(lineNumbers),
                        Arrays.toString(expectedLineNumbers)));
    }

    public static long[] getExpectedLineNumbers(Executable aMethod) {
        Map<Integer, Integer> bciToLine = CTVMUtilities
                .getBciToLineNumber(aMethod);
        long[] result = null;
        if (!bciToLine.isEmpty()) {
            result = new long[2 * bciToLine.size()];
            int i = 0;
            for (Integer key : bciToLine.keySet()) {
                result[i++] = key.longValue();
                result[i++] = bciToLine.get(key).longValue();
            }
        }
        // compilerToVM::getLineNumberTable returns null in case empty table
        return result;
    }

}
