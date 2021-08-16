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

/*
 * @test IsMethodCompilableTest
 * @bug 8007270 8006683 8007288 8022832
 * @summary testing of WB::isMethodCompilable()
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 *
 * @requires vm.opt.DeoptimizeALot != true
 * @requires vm.flavor == "server" & (vm.opt.TieredStopAtLevel == null | vm.opt.TieredStopAtLevel == 4)
 * @requires !vm.emulatedClient
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm/timeout=2400 -XX:-TieredCompilation -Xmixed
 *      -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      -XX:PerMethodRecompilationCutoff=3 -XX:-UseCounterDecay
 *      -XX:CompileCommand=compileonly,compiler.whitebox.SimpleTestCaseHelper::*
 *      compiler.whitebox.IsMethodCompilableTest
 */

package compiler.whitebox;

import jdk.test.lib.Platform;

public class IsMethodCompilableTest extends CompilerWhiteBoxTest {
    /**
     * Value of {@code -XX:PerMethodRecompilationCutoff}
     */
    protected static final long PER_METHOD_RECOMPILATION_CUTOFF;

    static {
        long tmp = Long.parseLong(
                getVMOption("PerMethodRecompilationCutoff", "400"));
        if (tmp == -1) {
            PER_METHOD_RECOMPILATION_CUTOFF = -1 /* Inf */;
        } else {
            PER_METHOD_RECOMPILATION_CUTOFF = (0xFFFFFFFFL & tmp);
        }
    }

    public static void main(String[] args) throws Exception {
        CompilerWhiteBoxTest.main(IsMethodCompilableTest::new, args);
    }

    private IsMethodCompilableTest(TestCase testCase) {
        super(testCase);
        // to prevent inlining of #method
        WHITE_BOX.testSetDontInlineMethod(method, true);
    }

    /**
     * Tests {@code WB::isMethodCompilable()} by recompilation of tested method
     * 'PerMethodRecompilationCutoff' times and checks compilation status. Also
     * checks that WB::clearMethodState() clears no-compilable flags. Only
     * applicable to c2 compiled methods.
     *
     * @throws Exception if one of the checks fails.
     */
    @Override
    protected void test() throws Exception {

        // Only c2 compilations can be disabled through PerMethodRecompilationCutoff
        if (!Platform.isServer() || Platform.isEmulatedClient()) {
            throw new Error("TESTBUG: Not server mode");
        }

        if (skipXcompOSR()) {
            return;
        }
        if (!isCompilable(COMP_LEVEL_FULL_OPTIMIZATION)) {
            throw new RuntimeException(method + " must be compilable");
        }
        System.out.println("PerMethodRecompilationCutoff = "
                + PER_METHOD_RECOMPILATION_CUTOFF);
        if (PER_METHOD_RECOMPILATION_CUTOFF == -1) {
            System.err.println(
                    "Warning: test is not applicable if PerMethodRecompilationCutoff == Inf");
            return;
        }

        // deoptimize 'PerMethodRecompilationCutoff' times
        for (long attempts = 0, successes = 0;
               (successes < PER_METHOD_RECOMPILATION_CUTOFF)  &&
               (attempts < PER_METHOD_RECOMPILATION_CUTOFF*2) &&
               isCompilable(COMP_LEVEL_FULL_OPTIMIZATION); attempts++) {
            if (compileAndDeoptimize() == COMP_LEVEL_FULL_OPTIMIZATION) {
                successes++;
            }
        }

        if (!testCase.isOsr() && !isCompilable(COMP_LEVEL_FULL_OPTIMIZATION)) {
            // in osr test case count of deopt maybe more than iterations
            throw new RuntimeException(method + " is not compilable after "
                    + PER_METHOD_RECOMPILATION_CUTOFF + " iterations");
        }

        // Now compile once more
        compileAndDeoptimize();

        if (isCompilable(COMP_LEVEL_FULL_OPTIMIZATION)) {
            throw new RuntimeException(method + " is still compilable after "
                    + PER_METHOD_RECOMPILATION_CUTOFF + " iterations");
        }
        checkNotCompiled();
        compile();
        waitBackgroundCompilation();
        checkNotCompiled(COMP_LEVEL_FULL_OPTIMIZATION);

        // WB.clearMethodState() must reset no-compilable flags
        WHITE_BOX.clearMethodState(method);
        if (!isCompilable(COMP_LEVEL_FULL_OPTIMIZATION)) {
            throw new RuntimeException(method
                    + " is not compilable after clearMethodState()");
        }
        compile();
        checkCompiled();
    }

    private int compileAndDeoptimize() throws Exception {
        compile();
        waitBackgroundCompilation();
        int compLevel = getCompLevel();
        deoptimize();
        return compLevel;
    }
}
