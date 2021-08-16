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
 * @summary Verify that RTMAbortRatio affects amount of aborts before
 *          deoptimization.
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @requires vm.rtm.cpu & vm.rtm.compiler
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm/native -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                          -XX:+WhiteBoxAPI
 *                          compiler.rtm.locking.TestRTMAbortRatio
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
 * Test verifies that method will be deoptimized on high abort ratio
 * as soon as abort ratio reaches RTMAbortRatio's value.
 */
public class TestRTMAbortRatio {

    protected void runTestCases() throws Throwable {
        verifyAbortRatio(0, false);
        verifyAbortRatio(10, false);
        verifyAbortRatio(50, false);
        verifyAbortRatio(100, false);

        verifyAbortRatio(0, true);
        verifyAbortRatio(10, true);
        verifyAbortRatio(50, true);
        verifyAbortRatio(100, true);
    }

    private void verifyAbortRatio(int abortRatio, boolean useStackLock)
            throws Throwable {
        CompilableTest test = new Test();

        OutputAnalyzer outputAnalyzer = RTMTestBase.executeRTMTest(
                test,
                CommandLineOptionTest.prepareBooleanFlag("UseRTMForStackLocks",
                        useStackLock),
                "-XX:+UseRTMDeopt",
                "-XX:RTMTotalCountIncrRate=1",
                "-XX:RTMAbortThreshold=0",
                CommandLineOptionTest.prepareNumericFlag("RTMLockingThreshold",
                        10 * Test.TOTAL_ITERATIONS),
                CommandLineOptionTest.prepareNumericFlag("RTMAbortRatio",
                        abortRatio),
                "-XX:+PrintPreciseRTMLockingStatistics",
                test.getClass().getName(),
                Boolean.toString(!useStackLock));

        outputAnalyzer.shouldHaveExitValue(0);

        List<RTMLockingStatistics> statistics = RTMLockingStatistics.fromString(
                test.getMethodWithLockName(), outputAnalyzer.getOutput());

        Asserts.assertEQ(statistics.size(), 1, "VM output should contain "
                + "exactly one RTM locking statistics entry.");

        RTMLockingStatistics lock = statistics.get(0);
        int actualRatio;

        if (lock.getTotalAborts() == 1L) {
            actualRatio = 0;
        } else {
            actualRatio = (int) (lock.getTotalLocks()
                    / (lock.getTotalAborts() - 1L));
        }

        Asserts.assertLTE(actualRatio, abortRatio, String.format(
                "Actual abort ratio (%d) should lower or equal to "
                + "specified (%d).", actualRatio, abortRatio));
    }

    /**
     * Force abort after {@code Test.WARMUP_ITERATIONS} is done.
     */
    public static class Test implements CompilableTest {
        private static final int TOTAL_ITERATIONS = 10000;
        private static final int WARMUP_ITERATIONS = 1000;
        private final XAbortProvoker xabort = new XAbortProvoker();
        private final Object monitor = new Object();
        // Following field have to be static in order to avoid escape analysis.
        @SuppressWarnings("UnsuedDeclaration")
        private static int field = 0;

        @Override
        public String getMethodWithLockName() {
             return this.getClass().getName() + "::lock";
         }

        @Override
        public String[] getMethodsToCompileNames() {
            return new String[] { getMethodWithLockName(), "*.doAbort" };
        }

        public void lock(boolean abort) {
            synchronized(monitor) {
                if (abort) {
                    xabort.doAbort();
                }
            }
        }

        /**
         * Usage:
         * Test &lt;inflate monitor&gt;
         */
        public static void main(String args[]) throws Throwable {
            Asserts.assertGTE(args.length, 1, "One argument required.");
            Test t = new Test();
            boolean shouldBeInflated = Boolean.valueOf(args[0]);
            if (shouldBeInflated) {
                AbortProvoker.inflateMonitor(t.monitor);
            }
            for (int i = 0; i < Test.TOTAL_ITERATIONS; i++) {
                AbortProvoker.verifyMonitorState(t.monitor, shouldBeInflated);
                t.lock(i >= Test.WARMUP_ITERATIONS);
            }
        }
    }

    public static void main(String args[]) throws Throwable {
        new TestRTMAbortRatio().runTestCases();
    }
}

