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
 * @summary Verify that RTMLockingThreshold affects rtm state transition
 *          ProfileRTM => UseRTM.
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @requires vm.rtm.cpu & vm.rtm.compiler
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm/native -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                          -XX:+WhiteBoxAPI
 *                          compiler.rtm.locking.TestRTMLockingThreshold
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
 * Test verifies that RTMLockingThreshold option actually affects how soon
 * method will be deoptimized on low abort ratio.
 */
public class TestRTMLockingThreshold {

    /**
     * We use non-zero abort threshold to avoid abort related to
     * interrupts, VMM calls, etc. during first lock attempt.
     *
     */
    private static final int MIN_ABORT_THRESHOLD = 10;

    protected void runTestCases() throws Throwable {
        verifyLockingThreshold(0, false);
        verifyLockingThreshold(100, false);
        verifyLockingThreshold(1000, false);

        verifyLockingThreshold(0, true);
        verifyLockingThreshold(100, true);
        verifyLockingThreshold(1000, true);
    }

    private void verifyLockingThreshold(int lockingThreshold,
            boolean useStackLock) throws Throwable {
        CompilableTest test = new Test();

        int abortThreshold = Math.max(lockingThreshold / 2,
                TestRTMLockingThreshold.MIN_ABORT_THRESHOLD);

        OutputAnalyzer outputAnalyzer = RTMTestBase.executeRTMTest(
                test,
                "-XX:CompileThreshold=1",
                CommandLineOptionTest.prepareBooleanFlag("UseRTMForStackLocks",
                        useStackLock),
                "-XX:+UseRTMDeopt",
                "-XX:RTMTotalCountIncrRate=1",
                "-XX:RTMRetryCount=0",
                CommandLineOptionTest.prepareNumericFlag("RTMAbortThreshold",
                        abortThreshold),
                CommandLineOptionTest.prepareNumericFlag("RTMLockingThreshold",
                        lockingThreshold),
                "-XX:RTMAbortRatio=100",
                "-XX:+PrintPreciseRTMLockingStatistics",
                Test.class.getName(),
                Boolean.toString(!useStackLock),
                Integer.toString(lockingThreshold)
        );

        outputAnalyzer.shouldHaveExitValue(0);

        List<RTMLockingStatistics> statistics = RTMLockingStatistics.fromString(
                test.getMethodWithLockName(), outputAnalyzer.getOutput());

        Asserts.assertEQ(statistics.size(), 2, "VM output should contain two "
                + "RTM locking statistics entries.");

        /**
         * If RTMLockingThreshold==0, then we have to make at least 1 call.
         */
        long expectedValue = lockingThreshold;
        if (expectedValue == 0) {
            expectedValue++;
        }

        RTMLockingStatistics statBeforeDeopt = null;
        for (RTMLockingStatistics s : statistics) {
            if (s.getTotalLocks() == expectedValue) {
                Asserts.assertNull(statBeforeDeopt,
                        "Only one statistics entry should contain aborts");
                statBeforeDeopt = s;
            }
        }

        Asserts.assertNotNull(statBeforeDeopt, "There should be exactly one "
                + "statistics entry corresponding to ProfileRTM state.");
    }

    public static class Test implements CompilableTest {
        // Following field have to be static in order to avoid escape analysis.
        @SuppressWarnings("UnsuedDeclaration")
        private static int field = 0;
        private static final int TOTAL_ITERATIONS = 10000;
        private final XAbortProvoker xabort = new XAbortProvoker();
        private final Object monitor = new Object();

        @Override
        public String getMethodWithLockName() {
            return this.getClass().getName() + "::lock";
        }

        @Override
        public String[] getMethodsToCompileNames() {
            return new String[] { getMethodWithLockName(),
                                  XAbortProvoker.class.getName() + "::doAbort" };
        }

        public void lock(boolean abort) {
            synchronized(monitor) {
                if (abort) {
                    Test.field += xabort.doAbort();
                }
            }
        }

        /**
         * Usage:
         * Test &lt;inflate monitor&gt;
         */
        public static void main(String args[]) throws Throwable {
            Asserts.assertGTE(args.length, 2, "Two arguments required.");
            Test t = new Test();
            boolean shouldBeInflated = Boolean.valueOf(args[0]);
            int lockingThreshold = Integer.valueOf(args[1]);
            if (shouldBeInflated) {
                AbortProvoker.inflateMonitor(t.monitor);
            }
            for (int i = 0; i < Test.TOTAL_ITERATIONS; i++) {
                AbortProvoker.verifyMonitorState(t.monitor, shouldBeInflated);
                t.lock(i >= lockingThreshold / 2);
            }
        }
    }

    public static void main(String args[]) throws Throwable {
        new TestRTMLockingThreshold().runTestCases();
    }
}
