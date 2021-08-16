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
 * @test MakeMethodNotCompilableTest
 * @bug 8012322 8006683 8007288 8022832
 * @summary testing of WB::makeMethodNotCompilable()
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 *
 * @requires vm.opt.DeoptimizeALot != true
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI -Xmixed -XX:-UseCounterDecay
 *                   compiler.whitebox.MakeMethodNotCompilableTest
 */

package compiler.whitebox;

public class MakeMethodNotCompilableTest extends CompilerWhiteBoxTest {
    private int bci;
    public static void main(String[] args) throws Exception {
        String directive =
                "[{ match:\"*SimpleTestCaseHelper.*\", BackgroundCompilation: false }, " +
                " { match:\"*.*\", inline:\"-*SimpleTestCaseHelper.*\"}]";
        if (WHITE_BOX.addCompilerDirective(directive) != 2) {
            throw new RuntimeException("Could not add directive");
        }
        try {
            CompilerWhiteBoxTest.main(MakeMethodNotCompilableTest::new, args);
        } finally {
            WHITE_BOX.removeCompilerDirective(2);
        }
    }

    private MakeMethodNotCompilableTest(TestCase testCase) {
        super(testCase);
        // to prevent inlining of #method
        WHITE_BOX.testSetDontInlineMethod(method, true);
    }

    /**
     * Tests {@code WB::makeMethodNotCompilable()} by calling it before
     * compilation and checking that method isn't compiled. Also
     * checks that WB::clearMethodState() clears no-compilable flags. For
     * tiered, additional checks for all available levels are conducted.
     *
     * @throws Exception if one of the checks fails.
     */
    @Override
    protected void test() throws Exception {
        if (skipXcompOSR()) {
            return;
        }
        checkNotCompiled();
        if (!isCompilable()) {
            throw new RuntimeException(method + " must be compilable");
        }

        bci = getBci();

        if (TIERED_COMPILATION) {
            final int tierLimit = TIERED_STOP_AT_LEVEL + 1;
            for (int testedTier = 1; testedTier < tierLimit; ++testedTier) {
                testTier(testedTier);
            }
            for (int testedTier = 1; testedTier < tierLimit; ++testedTier) {
                makeNotCompilable(testedTier);
                if (isCompilable(testedTier)) {
                    throw new RuntimeException(method
                            + " must be not compilable at level" + testedTier);
                }
                WHITE_BOX.enqueueMethodForCompilation(method, testedTier, bci);
                checkNotCompiled();

                if (!isCompilable()) {
                    System.out.println(method
                            + " is not compilable after level " + testedTier);
                }
            }
        } else {
            compile();
            checkCompiled();
            int compLevel = getCompLevel();
            deoptimize();
            makeNotCompilable(compLevel);
            if (isCompilable(COMP_LEVEL_ANY)) {
                throw new RuntimeException(method
                        + " must be not compilable at CompLevel::CompLevel_any,"
                        + " after it is not compilable at " + compLevel);
            }

            WHITE_BOX.clearMethodState(method);
            if (!isCompilable()) {
                throw new RuntimeException(method
                        + " is not compilable after clearMethodState()");
            }

            // nocompilable at opposite level must make no sense
            int oppositeLevel;
            if (isC1Compile(compLevel)) {
              oppositeLevel = COMP_LEVEL_FULL_OPTIMIZATION;
            } else {
              oppositeLevel = COMP_LEVEL_SIMPLE;
            }
            makeNotCompilable(oppositeLevel);

            if (!isCompilable(COMP_LEVEL_ANY)) {
                  throw new RuntimeException(method
                        + " must be compilable at CompLevel::CompLevel_any,"
                        + " even it is not compilable at opposite level ["
                        + compLevel + "]");
            }

            if (!isCompilable(compLevel)) {
                  throw new RuntimeException(method
                        + " must be compilable at level " + compLevel
                        + ", even it is not compilable at opposite level ["
                        + compLevel + "]");
            }
        }

        // clearing after tiered/non-tiered tests
        // WB.clearMethodState() must reset no-compilable flags
        WHITE_BOX.clearMethodState(method);
        if (!isCompilable()) {
            throw new RuntimeException(method
                    + " is not compilable after clearMethodState()");
        }
        // Make method not (OSR-)compilable (depending on testCase.isOsr())
        makeNotCompilable();
        if (isCompilable()) {
            throw new RuntimeException(method + " must be not compilable");
        }
        // Try to (OSR-)compile method
        compile();
        // Method should not be (OSR-)compiled
        checkNotCompiled(testCase.isOsr());
        if (isCompilable()) {
            throw new RuntimeException(method + " must be not compilable");
        }
        // WB.clearMethodState() must reset no-compilable flags
        WHITE_BOX.clearMethodState(method);
        if (!isCompilable()) {
            throw new RuntimeException(method
                    + " is not compilable after clearMethodState()");
        }
        compile();
        checkCompiled();
    }

    // separately tests each tier
    private void testTier(int testedTier) {
        if (!isCompilable(testedTier)) {
            throw new RuntimeException(method
                    + " is not compilable on start");
        }
        makeNotCompilable(testedTier);

        // tests for all other tiers
        for (int anotherTier = 1, tierLimit = TIERED_STOP_AT_LEVEL + 1;
                    anotherTier < tierLimit; ++anotherTier) {
            boolean isCompilable = isCompilable(anotherTier);
            if (sameCompile(testedTier, anotherTier)) {
                if (isCompilable) {
                    throw new RuntimeException(method
                            + " must be not compilable at level " + anotherTier
                            + ", if it is not compilable at " + testedTier);
                }
                WHITE_BOX.enqueueMethodForCompilation(method, anotherTier, bci);
                checkNotCompiled();
            } else {
                if (!isCompilable) {
                    throw new RuntimeException(method
                            + " must be compilable at level " + anotherTier
                            + ", even if it is not compilable at "
                            + testedTier);
                }
                WHITE_BOX.enqueueMethodForCompilation(method, anotherTier, bci);
                checkCompiled();
                deoptimize();
            }

            if (!isCompilable(COMP_LEVEL_ANY) && TIERED_STOP_AT_LEVEL == COMP_LEVEL_FULL_OPTIMIZATION) {
                throw new RuntimeException(method
                        + " must be compilable at 'CompLevel::CompLevel_any'"
                        + ", if it is not compilable only at " + testedTier);
            }
        }

        // clear state after test
        WHITE_BOX.clearMethodState(method);
        if (!isCompilable(testedTier)) {
            throw new RuntimeException(method
                    + " is not compilable after clearMethodState()");
        }
    }

    private boolean sameCompile(int level1, int level2) {
        if (level1 == level2) {
            return true;
        }
        if (isC1Compile(level1) && isC1Compile(level2)) {
            return true;
        }
        if (isC2Compile(level1) && isC2Compile(level2)) {
            return true;
        }
        return false;
    }

    private int getBci() throws Exception {
        compile();
        checkCompiled();
        int result = WHITE_BOX.getMethodEntryBci(method);
        deoptimize();
        WHITE_BOX.clearMethodState(method);
        return result;
    }
}
