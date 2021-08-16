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
 * @summary Verify that on high abort ratio method will be recompiled
 *          without rtm locking.
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @requires vm.rtm.cpu & vm.rtm.compiler
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm/native -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                          -XX:+WhiteBoxAPI
 *                          compiler.rtm.locking.TestRTMDeoptOnHighAbortRatio
 */

package compiler.rtm.locking;

import compiler.testlibrary.rtm.AbortProvoker;
import compiler.testlibrary.rtm.AbortType;
import compiler.testlibrary.rtm.RTMLockingStatistics;
import compiler.testlibrary.rtm.RTMTestBase;
import jdk.test.lib.Asserts;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.cli.CommandLineOptionTest;

import java.util.List;

/**
 * Test verifies that on high abort ratio method wil be deoptimized with
 * <i>rtm_state_change</i> reason and after that RTM-based lock elision will not
 * be used for that method.
 * This test make asserts on total locks count done by compiled method,
 * so in order to avoid issue with retriable locks -XX:RTMRetryCount=0 is used.
 * For more details on that issue see {@link TestUseRTMAfterLockInflation}.
 */
public class TestRTMDeoptOnHighAbortRatio {
    private static final long ABORT_THRESHOLD
            = AbortProvoker.DEFAULT_ITERATIONS / 2L;

    protected void runTestCases() throws Throwable {
        verifyDeopt(false);
        verifyDeopt(true);
    }

    private void verifyDeopt(boolean useStackLock) throws Throwable {
        AbortProvoker provoker = AbortType.XABORT.provoker();
        String logFileName = String.format("rtm_deopt_%s_stack_lock.xml",
                (useStackLock ? "use" : "no"));

        OutputAnalyzer outputAnalyzer = RTMTestBase.executeRTMTest(
                logFileName,
                provoker,
                "-XX:+UseRTMDeopt",
                CommandLineOptionTest.prepareBooleanFlag("UseRTMForStackLocks",
                        useStackLock),
                "-XX:RTMRetryCount=0",
                CommandLineOptionTest.prepareNumericFlag("RTMAbortThreshold",
                        TestRTMDeoptOnHighAbortRatio.ABORT_THRESHOLD),
                "-XX:RTMAbortRatio=100",
                "-XX:CompileThreshold=1",
                "-XX:RTMTotalCountIncrRate=1",
                "-XX:+PrintPreciseRTMLockingStatistics",
                AbortProvoker.class.getName(),
                AbortType.XABORT.toString(),
                Boolean.toString(!useStackLock)
        );

        outputAnalyzer.shouldHaveExitValue(0);

        int firedTraps = RTMTestBase.firedRTMStateChangeTraps(logFileName);

        Asserts.assertEQ(firedTraps, 1, "Expected to get only one "
                + "deoptimization due to rtm state change");

        List<RTMLockingStatistics> statistics = RTMLockingStatistics.fromString(
                provoker.getMethodWithLockName(), outputAnalyzer.getOutput());

        Asserts.assertEQ(statistics.size(), 1, "VM output should contain "
                + "exactly one RTM locking statistics entry for method "
                + provoker.getMethodWithLockName());

        Asserts.assertEQ(statistics.get(0).getTotalLocks(),
                TestRTMDeoptOnHighAbortRatio.ABORT_THRESHOLD,
                "After AbortThreshold was reached, method should be"
                + " recompiled without rtm lock eliding.");
    }

    public static void main(String args[]) throws Throwable {
        new TestRTMDeoptOnHighAbortRatio().runTestCases();
    }
}

