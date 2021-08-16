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
 * @summary Verify that rtm locking is used for stack locks before
 *          inflation and after it used for inflated locks.
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @requires vm.rtm.cpu & vm.rtm.compiler
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm/native -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                          -XX:+WhiteBoxAPI
 *                          compiler.rtm.locking.TestUseRTMAfterLockInflation
 */

package compiler.rtm.locking;

import compiler.testlibrary.rtm.AbortProvoker;
import compiler.testlibrary.rtm.AbortType;
import compiler.testlibrary.rtm.RTMLockingStatistics;
import compiler.testlibrary.rtm.RTMTestBase;
import jdk.test.lib.Asserts;
import jdk.test.lib.process.OutputAnalyzer;

import java.util.List;

/**
 * Test verifies that RTM is used after lock inflation by executing compiled
 * method with RTM-based lock elision using stack lock first, then that lock
 * is inflated and the same compiled method invoked again.
 *
 * Compiled method invoked {@code AbortProvoker.DEFAULT_ITERATIONS} times before
 * lock inflation and the same amount of times after inflation.
 * As a result total locks count should be equal to
 * {@code 2 * AbortProvoker.DEFAULT_ITERATIONS}.
 * It is a pretty strict assertion which could fail if some retriable abort
 * happened: it could be {@code AbortType.RETRIABLE} or
 * {@code AbortType.MEM_CONFLICT}, but unfortunately abort can has both these
 * reasons simultaneously. In order to avoid false negative failures related
 * to incorrect aborts counting, -XX:RTMRetryCount=0 is used.
 */
public class TestUseRTMAfterLockInflation {
    private static final long EXPECTED_LOCKS
            = 2L * AbortProvoker.DEFAULT_ITERATIONS;

    protected void runTestCases() throws Throwable {
        AbortProvoker provoker = AbortType.XABORT.provoker();
        long totalLocksCount = 0;

        OutputAnalyzer outputAnalyzer = RTMTestBase.executeRTMTest(
                provoker,
                "-XX:+UseRTMForStackLocks",
                "-XX:RTMTotalCountIncrRate=1",
                "-XX:RTMRetryCount=0",
                "-XX:+PrintPreciseRTMLockingStatistics",
                Test.class.getName(),
                AbortType.XABORT.toString());

        outputAnalyzer.shouldHaveExitValue(0);

        List<RTMLockingStatistics> statistics = RTMLockingStatistics.fromString(
                provoker.getMethodWithLockName(), outputAnalyzer.getOutput());

        Asserts.assertEQ(statistics.size(), 2,
                "VM output should contain two rtm locking statistics entries "
                + "for method " + provoker.getMethodWithLockName());

        for (RTMLockingStatistics s : statistics) {
            totalLocksCount += s.getTotalLocks();
        }

        Asserts.assertEQ(totalLocksCount,
                TestUseRTMAfterLockInflation.EXPECTED_LOCKS,
                "Total lock count should be greater or equal to "
                + TestUseRTMAfterLockInflation.EXPECTED_LOCKS);
    }

    public static class Test {
        /**
         * Usage:
         * Test &lt;provoker type&gt;
         */
        public static void main(String args[]) throws Throwable {
            Asserts.assertGT(args.length, 0,
                    "AbortType name is expected as first argument.");

            AbortProvoker provoker
                    = AbortType.lookup(Integer.valueOf(args[0])).provoker();
            for (int i = 0; i < AbortProvoker.DEFAULT_ITERATIONS; i++) {
                AbortProvoker.verifyMonitorState(provoker, false /*deflated*/);
                provoker.forceAbort();
            }
            provoker.inflateMonitor();
            for (int i = 0; i < AbortProvoker.DEFAULT_ITERATIONS; i++) {
                AbortProvoker.verifyMonitorState(provoker, true /*inflated*/);
                provoker.forceAbort();
            }
        }
    }

    public static void main(String args[]) throws Throwable {
        new TestUseRTMAfterLockInflation().runTestCases();
    }
}
