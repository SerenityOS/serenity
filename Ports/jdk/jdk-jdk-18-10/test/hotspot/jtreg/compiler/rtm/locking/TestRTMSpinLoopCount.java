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
 * @summary Verify that RTMSpinLoopCount affects time spent
 *          between locking attempts.
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @requires vm.rtm.cpu & vm.rtm.compiler
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI
 *                   compiler.rtm.locking.TestRTMSpinLoopCount
 */

package compiler.rtm.locking;

import compiler.testlibrary.rtm.BusyLock;
import compiler.testlibrary.rtm.CompilableTest;
import compiler.testlibrary.rtm.RTMLockingStatistics;
import compiler.testlibrary.rtm.RTMTestBase;
import jdk.test.lib.Asserts;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.cli.CommandLineOptionTest;
import jdk.test.lib.Platform;

import java.util.List;

/**
 * Test verifies that RTMSpinLoopCount increase time spent between retries
 * by comparing amount of retries done with different RTMSpinLoopCount's values.
 */
public class TestRTMSpinLoopCount {
    private static final int LOCKING_TIME = 1000;
    private static final int RTM_RETRY_COUNT = 1000;
    private static final boolean INFLATE_MONITOR = true;
    private static final long MAX_ABORTS = RTM_RETRY_COUNT + 1L;
    private static int[] SPIN_LOOP_COUNTS;

    protected void runTestCases() throws Throwable {

        if (Platform.isPPC()) {
            SPIN_LOOP_COUNTS = new int[] { 0, 10, 100, 1_000, 10_000 };
        } else {
            SPIN_LOOP_COUNTS = new int[] { 0, 100, 1_000, 10_000, 100_000 };
        }

        long[] aborts = new long[TestRTMSpinLoopCount.SPIN_LOOP_COUNTS.length];

        for (int i = 0; i < TestRTMSpinLoopCount.SPIN_LOOP_COUNTS.length; i++) {
            aborts[i] = getAbortsCountOnLockBusy(
                    TestRTMSpinLoopCount.SPIN_LOOP_COUNTS[i]);
        }

        for (int i = 1; i < aborts.length; i++) {
            Asserts.assertLTE(aborts[i], aborts[i - 1], "Increased spin loop "
                    + "count should not increase retries count.");
        }
    }

    private long getAbortsCountOnLockBusy(int spinLoopCount) throws Throwable {
        CompilableTest test = new BusyLock();

        OutputAnalyzer outputAnalyzer = RTMTestBase.executeRTMTest(
                test,
                CommandLineOptionTest.prepareNumericFlag("RTMRetryCount",
                        TestRTMSpinLoopCount.RTM_RETRY_COUNT),
                CommandLineOptionTest.prepareNumericFlag("RTMSpinLoopCount",
                        spinLoopCount),
                "-XX:-UseRTMXendForLockBusy",
                "-XX:RTMTotalCountIncrRate=1",
                "-XX:+PrintPreciseRTMLockingStatistics",
                BusyLock.class.getName(),
                Boolean.toString(TestRTMSpinLoopCount.INFLATE_MONITOR),
                Integer.toString(TestRTMSpinLoopCount.LOCKING_TIME)
        );

        outputAnalyzer.shouldHaveExitValue(0);

        List<RTMLockingStatistics> statistics = RTMLockingStatistics.fromString(
                test.getMethodWithLockName(), outputAnalyzer.getOutput());

        Asserts.assertEQ(statistics.size(), 1,
                "VM output should contain exactly one entry for method "
                 + test.getMethodWithLockName());

        RTMLockingStatistics lock = statistics.get(0);

        Asserts.assertLTE(lock.getTotalAborts(),
                TestRTMSpinLoopCount.MAX_ABORTS, String.format("Total aborts "
                        + "count (%d) should be less or equal to %d",
                        lock.getTotalAborts(),
                        TestRTMSpinLoopCount.MAX_ABORTS));

        return lock.getTotalAborts();
    }

    public static void main(String args[]) throws Throwable {
        new TestRTMSpinLoopCount().runTestCases();
    }
}
