/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test Level2RecompilationTest
 * @summary Test downgrading mechanism from level 3 to level 2 for those profiled methods.
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 *
 * @comment the test can't be run w/ TieredStopAtLevel < 4
 * @requires vm.flavor == "server" & (vm.opt.TieredStopAtLevel == null | vm.opt.TieredStopAtLevel == 4) & vm.compMode != "Xcomp"
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+TieredCompilation
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -XX:-UseCounterDecay
 *                   -XX:CompileCommand=compileonly,compiler.whitebox.SimpleTestCaseHelper::*
 *                   -XX:CompileCommand=print,compiler.whitebox.SimpleTestCaseHelper::*
 *                   compiler.tiered.Level2RecompilationTest
 */

package compiler.tiered;

import compiler.whitebox.CompilerWhiteBoxTest;
import jtreg.SkippedException;

public class Level2RecompilationTest extends CompLevelsTest {
    public static void main(String[] args) throws Throwable {
        if (CompilerWhiteBoxTest.skipOnTieredCompilation(false)) {
            throw new SkippedException("Test isn't applicable for non-tiered mode");
        }
        String[] testcases = {"METHOD_TEST", "OSR_STATIC_TEST"};
        CompilerWhiteBoxTest.main(Level2RecompilationTest::new, testcases);
    }

    protected Level2RecompilationTest(TestCase testCase) {
        super(testCase);
        // to prevent inlining of #method
        WHITE_BOX.testSetDontInlineMethod(method, true);
    }

    @Override
    protected void test() throws Exception {
        if (skipXcompOSR()) {
          return;
        }

        checkNotCompiled();
        int bci = WHITE_BOX.getMethodEntryBci(method);
        WHITE_BOX.markMethodProfiled(method);
        if (testCase.isOsr()) {
            // for OSR compilation, it must be the begin of a BB.
            // c1_GraphBulider.cpp:153  assert(method()->bci_block_start().at(cur_bci), ...
            bci = 0;
        }

        WHITE_BOX.enqueueMethodForCompilation(method, COMP_LEVEL_FULL_PROFILE, bci);
        checkCompiled();
        checkLevel(COMP_LEVEL_LIMITED_PROFILE, getCompLevel());

        for (int i=0; i<1_000; ++i) {
            WHITE_BOX.enqueueMethodForCompilation(method, COMP_LEVEL_FULL_PROFILE, bci);
            waitBackgroundCompilation();
            checkLevel(COMP_LEVEL_LIMITED_PROFILE, getCompLevel());
        }
    }

    @Override
    protected void checkLevel(int expected, int actual) {
        if (expected == COMP_LEVEL_FULL_PROFILE
                && actual == COMP_LEVEL_LIMITED_PROFILE) {
            // for simple method full_profile may be replaced by limited_profile
            if (IS_VERBOSE) {
                System.out.printf("Level check: full profiling was replaced "
                        + "by limited profiling. Expected: %d, actual:%d\n",
                        expected, actual);
            }
            return;
        }
        super.checkLevel(expected, actual);
    }
}

