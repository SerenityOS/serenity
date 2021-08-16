/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8031320
 * @summary Verify that if we use RTMDeopt, then deoptimization
 *          caused by reason other then rtm_state_change will reset
 *          method's RTM state. And if we don't use RTMDeopt, then
 *          RTM state remain the same after such deoptimization.
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @requires vm.rtm.cpu & vm.rtm.compiler
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm/native -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                          -XX:+WhiteBoxAPI
 *                          compiler.rtm.locking.TestRTMAfterNonRTMDeopt
 */

package compiler.rtm.locking;

import compiler.testlibrary.rtm.AbortProvoker;
import compiler.testlibrary.rtm.XAbortProvoker;
import compiler.testlibrary.rtm.CompilableTest;
import compiler.testlibrary.rtm.RTMLockingStatistics;
import compiler.testlibrary.rtm.RTMTestBase;
import jdk.test.lib.Asserts;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.cli.CommandLineOptionTest;

import java.util.List;

/**
 * To verify that with +UseRTMDeopt method's RTM state will be
 * changed to ProfileRTM on deoptimization unrelated to
 * rtm_state_change following sequence of events is used:
 * <pre>
 *
 *     rtm state ^
 *               |
 *       UseRTM  |      ******|     ******
 *               |            |
 *   ProfileRTM  |******|     |*****|
 *               |      |     |     |
 *              0-------|-----|-----|---------------------&gt; time
 *                      |     |     \ force abort
 *                      |     |
 *                      |     \ force deoptimization
 *                      |
 *                      \ force xabort
 * </pre>
 * When xabort is forced by native method call method should
 * change it's state to UseRTM, because we use RTMAbortRatio=100
 * and low RTMLockingThreshold, so at this point actual abort
 * ratio will be below 100% and there should be enough lock
 * attempts to recompile method without RTM profiling.
 */
public class TestRTMAfterNonRTMDeopt {
    private static final int ABORT_THRESHOLD = 1000;
    private static final String RANGE_CHECK = "range_check";

    protected void runTestCases() throws Throwable {
        verifyRTMAfterDeopt(false, false);
        verifyRTMAfterDeopt(true, false);

        verifyRTMAfterDeopt(false, true);
        verifyRTMAfterDeopt(true, true);
    }

    private void verifyRTMAfterDeopt(boolean useStackLock,
            boolean useRTMDeopt) throws Throwable {
        CompilableTest test = new Test();
        String logFile = String.format("rtm_%s_stack_lock_%s_deopt.xml",
                (useStackLock ? "use" : "no"), (useRTMDeopt ? "use" : "no"));

        OutputAnalyzer outputAnalyzer = RTMTestBase.executeRTMTest(
                logFile,
                test,
                "-XX:CompileThreshold=1",
                CommandLineOptionTest.prepareBooleanFlag("UseRTMForStackLocks",
                        useStackLock),
                CommandLineOptionTest.prepareBooleanFlag("UseRTMDeopt",
                        useRTMDeopt),
                "-XX:RTMAbortRatio=100",
                CommandLineOptionTest.prepareNumericFlag("RTMAbortThreshold",
                        TestRTMAfterNonRTMDeopt.ABORT_THRESHOLD),
                CommandLineOptionTest.prepareNumericFlag("RTMLockingThreshold",
                        TestRTMAfterNonRTMDeopt.ABORT_THRESHOLD / 2L),
                "-XX:RTMTotalCountIncrRate=1",
                "-XX:+PrintPreciseRTMLockingStatistics",
                Test.class.getName(),
                Boolean.toString(!useStackLock)
        );

        outputAnalyzer.shouldHaveExitValue(0);

        int traps = RTMTestBase.firedRTMStateChangeTraps(logFile);

        if (useRTMDeopt) {
            Asserts.assertEQ(traps, 2, "Two uncommon traps with "
                    + "reason rtm_state_change should be fired.");
        } else {
            Asserts.assertEQ(traps, 0, "No uncommon traps with "
                    + "reason rtm_state_change should be fired.");
        }

        int rangeCheckTraps = RTMTestBase.firedUncommonTraps(logFile,
                TestRTMAfterNonRTMDeopt.RANGE_CHECK);

        Asserts.assertEQ(rangeCheckTraps, 1,
                "One range_check uncommon trap should be fired.");

        List<RTMLockingStatistics> statistics = RTMLockingStatistics.fromString(
                test.getMethodWithLockName(), outputAnalyzer.getOutput());

        int expectedStatEntries = (useRTMDeopt ? 4 : 2);

        Asserts.assertEQ(statistics.size(), expectedStatEntries,
                String.format("VM output should contain %d RTM locking "
                        + "statistics entries.", expectedStatEntries));
    }

    public static class Test implements CompilableTest {
        // Following field have to be static in order to avoid escape analysis.
        @SuppressWarnings("UnsuedDeclaration")
        private static int field = 0;
        private static final int ITERATIONS = 10000;
        private static final int RANGE_CHECK_AT = ITERATIONS / 2;
        private final XAbortProvoker xabort = new XAbortProvoker();
        private final Object monitor = new Object();

        @Override
        public String getMethodWithLockName() {
            return this.getClass().getName() + "::forceAbort";
        }

        @Override
        public String[] getMethodsToCompileNames() {
            return new String[] { getMethodWithLockName(),
                                  XAbortProvoker.class.getName() + "::doAbort()" };
        }

        public void forceAbort(int a[], boolean abort) {
            try {
                synchronized(monitor) {
                    a[0]++;
                    if (abort) {
                        Test.field = xabort.doAbort();
                    }
                }
            } catch (Throwable t) {
                // suppress any throwables
            }
        }

        /**
         * Usage:
         * Test &lt;inflate monitor&gt;
         */
        public static void main(String args[]) throws Throwable {
            Test t = new Test();

            boolean shouldBeInflated = Boolean.valueOf(args[0]);
            if (shouldBeInflated) {
                AbortProvoker.inflateMonitor(t.monitor);
            }

            int tmp[] = new int[1];

            for (int i = 0; i < Test.ITERATIONS; i++ ) {
                AbortProvoker.verifyMonitorState(t.monitor, shouldBeInflated);
                if (i == Test.RANGE_CHECK_AT) {
                    t.forceAbort(new int[0], false);
                } else {
                    boolean isThreshold
                            = (i == TestRTMAfterNonRTMDeopt.ABORT_THRESHOLD);
                    boolean isThresholdPlusRange
                            = (i == TestRTMAfterNonRTMDeopt.ABORT_THRESHOLD
                            + Test.RANGE_CHECK_AT);
                    t.forceAbort(tmp, isThreshold || isThresholdPlusRange);
                }
            }
        }
    }

    public static void main(String args[]) throws Throwable {
        new TestRTMAfterNonRTMDeopt().runTestCases();
    }
}

