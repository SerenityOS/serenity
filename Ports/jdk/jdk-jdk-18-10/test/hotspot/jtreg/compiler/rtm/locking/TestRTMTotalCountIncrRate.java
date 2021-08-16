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
 * @summary Verify that RTMTotalCountIncrRate option affects
 *          RTM locking statistics.
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @requires vm.rtm.cpu & vm.rtm.compiler
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm/native -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                          -XX:+WhiteBoxAPI
 *                          compiler.rtm.locking.TestRTMTotalCountIncrRate
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
 * Test verifies that with RTMTotalCountIncrRate=1 RTM locking statistics
 * contains precise information abort attempted locks and that with other values
 * statistics contains information abort non-zero locking attempts.
 * Since assert done for RTMTotalCountIncrRate=1 is pretty strict, test uses
 * -XX:RTMRetryCount=0 to avoid issue with retriable aborts. For more details on
 * that issue see {@link TestUseRTMAfterLockInflation}.
 */
public class TestRTMTotalCountIncrRate {
    protected void runTestCases() throws Throwable {
        verifyLocksCount(1, false);
        verifyLocksCount(64, false);
        verifyLocksCount(128, false);
        verifyLocksCount(1, true);
        verifyLocksCount(64, true);
        verifyLocksCount(128, true);
    }

    private void verifyLocksCount(int incrRate, boolean useStackLock)
            throws Throwable{
        CompilableTest test = new Test();

        OutputAnalyzer outputAnalyzer = RTMTestBase.executeRTMTest(
                test,
                CommandLineOptionTest.prepareBooleanFlag("UseRTMForStackLocks",
                        useStackLock),
                CommandLineOptionTest.prepareNumericFlag(
                        "RTMTotalCountIncrRate", incrRate),
                "-XX:RTMRetryCount=0",
                "-XX:+PrintPreciseRTMLockingStatistics",
                Test.class.getName(),
                Boolean.toString(!useStackLock)
        );

        outputAnalyzer.shouldHaveExitValue(0);

        List<RTMLockingStatistics> statistics = RTMLockingStatistics.fromString(
                test.getMethodWithLockName(), outputAnalyzer.getOutput());

        Asserts.assertEQ(statistics.size(), 1, "VM output should contain "
                + "exactly one RTM locking statistics entry for method "
                + test.getMethodWithLockName());

        RTMLockingStatistics lock = statistics.get(0);
        if (incrRate == 1) {
            Asserts.assertEQ(lock.getTotalLocks(), Test.TOTAL_ITERATIONS,
                    "Total locks should be exactly the same as amount of "
                    + "iterations.");
        }
    }

    public static class Test implements CompilableTest {
        private static final long TOTAL_ITERATIONS = 10000L;
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

        public void lock(boolean forceAbort) {
            synchronized(monitor) {
                if (forceAbort) {
                    // We're calling native method in order to force
                    // abort. It's done by explicit xabort call emitted
                    // in SharedRuntime::generate_native_wrapper.
                    // If an actual JNI call will be replaced by
                    // intrinsic - we'll be in trouble, since xabort
                    // will be no longer called and test may fail.
                    xabort.doAbort();
                }
                Test.field++;
            }
        }

        /**
         * Usage:
         * Test &lt;inflate monitor&gt;
         */
        public static void main(String args[]) throws Throwable {
            Asserts.assertGTE(args.length, 1, "One argument required.");
            Test test = new Test();
            boolean shouldBeInflated = Boolean.valueOf(args[0]);
            if (shouldBeInflated) {
                AbortProvoker.inflateMonitor(test.monitor);
            }
            for (long i = 0L; i < Test.TOTAL_ITERATIONS; i++) {
                AbortProvoker.verifyMonitorState(test.monitor,
                        shouldBeInflated);
                // Force abort on first iteration to avoid rare case when
                // there were no aborts and locks count was not incremented
                // with RTMTotalCountIncrRate > 1 (in such case JVM won't
                // print JVM locking statistics).
                test.lock(i == 0);
            }
        }
    }

    public static void main(String args[]) throws Throwable {
        new TestRTMTotalCountIncrRate().runTestCases();
    }
}
