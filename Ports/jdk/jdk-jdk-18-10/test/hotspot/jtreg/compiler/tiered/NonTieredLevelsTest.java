/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test NonTieredLevelsTest
 * @summary Verify that only one level can be used
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @requires vm.opt.TieredStopAtLevel==null
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:-TieredCompilation
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -XX:-UseCounterDecay
 *                   -XX:CompileCommand=compileonly,compiler.whitebox.SimpleTestCaseHelper::*
 *                   compiler.tiered.NonTieredLevelsTest
 */

package compiler.tiered;

import java.util.function.IntPredicate;
import compiler.whitebox.CompilerWhiteBoxTest;
import jdk.test.lib.Platform;
import jtreg.SkippedException;

public class NonTieredLevelsTest extends CompLevelsTest {
    private static final int AVAILABLE_COMP_LEVEL;
    private static final IntPredicate IS_AVAILABLE_COMPLEVEL;
    static {
        if (Platform.isServer() && !Platform.isEmulatedClient()) {
            AVAILABLE_COMP_LEVEL = COMP_LEVEL_FULL_OPTIMIZATION;
            IS_AVAILABLE_COMPLEVEL = x -> x == COMP_LEVEL_FULL_OPTIMIZATION;
        } else if (Platform.isClient() || Platform.isMinimal() || Platform.isEmulatedClient()) {
            AVAILABLE_COMP_LEVEL = COMP_LEVEL_SIMPLE;
            IS_AVAILABLE_COMPLEVEL = x -> x == COMP_LEVEL_SIMPLE;
        } else {
            throw new Error("TESTBUG: unknown VM: " + Platform.vmName);
        }

    }
    public static void main(String[] args) throws Exception {
        if (CompilerWhiteBoxTest.skipOnTieredCompilation(true)) {
            throw new SkippedException("Test isn't applicable for tiered mode");
        }
        CompilerWhiteBoxTest.main(NonTieredLevelsTest::new, args);
    }

    private NonTieredLevelsTest(TestCase testCase) {
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
        compile();
        checkCompiled();

        int compLevel = getCompLevel();
        checkLevel(AVAILABLE_COMP_LEVEL, compLevel);
        int bci = WHITE_BOX.getMethodEntryBci(method);
        deoptimize();
        if (!testCase.isOsr()) {
            for (int level = 1; level <= COMP_LEVEL_MAX; ++level) {
                if (IS_AVAILABLE_COMPLEVEL.test(level)) {
                    testAvailableLevel(level, bci);
                } else {
                    testUnavailableLevel(level, bci);
                }
            }
        } else {
            System.out.println("skip other levels testing in OSR");
            testAvailableLevel(AVAILABLE_COMP_LEVEL, bci);
        }
    }
}

