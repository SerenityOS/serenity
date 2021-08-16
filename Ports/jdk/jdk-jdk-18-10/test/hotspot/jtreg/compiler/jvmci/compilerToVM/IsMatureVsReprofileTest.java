/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @requires vm.jvmci & (vm.opt.TieredStopAtLevel == null | vm.opt.TieredStopAtLevel == 4)
 * @library / /test/lib
 *          ../common/patches
 * @modules java.base/jdk.internal.misc
 *          java.base/jdk.internal.org.objectweb.asm
 *          java.base/jdk.internal.org.objectweb.asm.tree
 *          jdk.internal.vm.ci/jdk.vm.ci.hotspot
 *          jdk.internal.vm.ci/jdk.vm.ci.code
 *          jdk.internal.vm.ci/jdk.vm.ci.meta
 *          jdk.internal.vm.ci/jdk.vm.ci.runtime
 *
 * @build jdk.internal.vm.ci/jdk.vm.ci.hotspot.CompilerToVMHelper
 *        sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *     -XX:+WhiteBoxAPI -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -Xbatch -XX:CompileThresholdScaling=1.0
 *     compiler.jvmci.compilerToVM.IsMatureVsReprofileTest
 */

package compiler.jvmci.compilerToVM;

import compiler.jvmci.common.CTVMUtilities;
import compiler.jvmci.common.testcases.SimpleClass;
import jdk.vm.ci.hotspot.CompilerToVMHelper;
import jdk.test.lib.Asserts;
import sun.hotspot.WhiteBox;
import compiler.whitebox.CompilerWhiteBoxTest;
import java.lang.reflect.Executable;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod;
import jdk.test.lib.Platform;

public class IsMatureVsReprofileTest {
    private static final WhiteBox WB = WhiteBox.getWhiteBox();
    private static final boolean TIERED = WB.getBooleanVMFlag("TieredCompilation");
    private static final boolean IS_XCOMP = Platform.isComp();

    public static void main(String[] args) throws Exception {
        new IsMatureVsReprofileTest().test();
    }

    public void test() throws Exception {
        SimpleClass sclass = new SimpleClass();
        Executable method = SimpleClass.class.getDeclaredMethod("testMethod");
        long metaspaceMethodData = WB.getMethodData(method);
        Asserts.assertEQ(metaspaceMethodData, 0L, "MDO should be null for a "
                 + "never invoked method");
        boolean isMature = CompilerToVMHelper.isMature(metaspaceMethodData);
        Asserts.assertFalse(isMature, "null MDO can't be mature");
        for (int i = 0; i < CompilerWhiteBoxTest.THRESHOLD; i++) {
            sclass.testMethod();
        }
        Asserts.assertTrue(WB.isMethodCompiled(method),
                "Method should be compiled");
        metaspaceMethodData = WB.getMethodData(method);
        Asserts.assertNE(metaspaceMethodData, 0L,
                "Multiple times invoked method should have MDO");
        isMature = CompilerToVMHelper.isMature(metaspaceMethodData);
        /* a method is not mature for -Xcomp and -Tiered,
           see NonTieredCompPolicy::is_mature */
        Asserts.assertEQ(!IS_XCOMP || TIERED, isMature,
                "Unexpected isMature state for compiled method");
        HotSpotResolvedJavaMethod resolvedMethod
                = CTVMUtilities.getResolvedMethod(method);
        CompilerToVMHelper.reprofile(resolvedMethod);
        Asserts.assertFalse(WB.isMethodCompiled(method),
                "Unexpected method compilation state after reprofile");
        metaspaceMethodData = WB.getMethodData(method);
        isMature = CompilerToVMHelper.isMature(metaspaceMethodData);
        Asserts.assertNE(metaspaceMethodData, 0L,
                "Got null MDO after reprofile");
        Asserts.assertEQ(TIERED && IS_XCOMP, isMature,
                "Got unexpected isMature state after reprofiling");
    }
}
