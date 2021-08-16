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
 * @summary Verify that rtm locking is used for stack locks.
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @requires vm.rtm.cpu & vm.rtm.compiler
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm/native -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                          -XX:+WhiteBoxAPI
 *                          compiler.rtm.locking.TestUseRTMForStackLocks
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
 * Test verifies that RTM-based lock elision could be used for stack locks
 * by calling compiled method that use RTM-based lock elision and using
 * stack lock.
 * Compiled method invoked {@code AbortProvoker.DEFAULT_ITERATIONS} times,
 * so total locks count should be the same.
 * This test could also be affected by retriable aborts, so -XX:RTMRetryCount=0
 * is used. For more information abort that issue see
 * {@link TestUseRTMAfterLockInflation}.
 */
public class TestUseRTMForStackLocks {
    private static final boolean INFLATE_MONITOR = false;

    protected void runTestCases() throws Throwable {
        AbortProvoker provoker = AbortType.XABORT.provoker();
        RTMLockingStatistics lock;

        OutputAnalyzer outputAnalyzer = RTMTestBase.executeRTMTest(
                provoker,
                "-XX:+UseRTMForStackLocks",
                "-XX:RTMTotalCountIncrRate=1",
                "-XX:RTMRetryCount=0",
                "-XX:+PrintPreciseRTMLockingStatistics",
                AbortProvoker.class.getName(),
                AbortType.XABORT.toString(),
                Boolean.toString(TestUseRTMForStackLocks.INFLATE_MONITOR));

        outputAnalyzer.shouldHaveExitValue(0);

        List<RTMLockingStatistics> statistics = RTMLockingStatistics.fromString(
                provoker.getMethodWithLockName(), outputAnalyzer.getOutput());

        Asserts.assertEQ(statistics.size(), 1,
                "VM output should contain exactly one rtm locking statistics "
                + "entry for method " + provoker.getMethodWithLockName());

        lock = statistics.get(0);
        Asserts.assertEQ(lock.getTotalLocks(), AbortProvoker.DEFAULT_ITERATIONS,
                "Total locks count should be greater or equal to "
                + AbortProvoker.DEFAULT_ITERATIONS);
    }

    public static void main(String args[]) throws Throwable {
        new TestUseRTMForStackLocks().runTestCases();
    }
}
