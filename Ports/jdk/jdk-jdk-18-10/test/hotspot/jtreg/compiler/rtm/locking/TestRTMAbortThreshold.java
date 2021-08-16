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
 * @summary Verify that RTMAbortThreshold option affects
 *          amount of aborts after which abort ratio is calculated.
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @requires vm.rtm.cpu & vm.rtm.compiler
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm/native -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                          -XX:+WhiteBoxAPI
 *                          compiler.rtm.locking.TestRTMAbortThreshold
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
 * Test verifies that on RTMAbortThreshold option actually affects how soon
 * method will be deoptimized on high abort ratio.
 */
public class TestRTMAbortThreshold {

    protected void runTestCases() throws Throwable {
        verifyAbortThreshold(false, 1);
        verifyAbortThreshold(false, 10);
        verifyAbortThreshold(false, 1000);

        verifyAbortThreshold(true, 1);
        verifyAbortThreshold(true, 10);
        verifyAbortThreshold(true, 1000);
    }

    private void verifyAbortThreshold(boolean useStackLock,
            long abortThreshold) throws Throwable {
        AbortProvoker provoker = AbortType.XABORT.provoker();

        OutputAnalyzer outputAnalyzer = RTMTestBase.executeRTMTest(
                provoker,
                "-XX:+UseRTMDeopt",
                "-XX:RTMAbortRatio=0",
                CommandLineOptionTest.prepareNumericFlag("RTMAbortThreshold",
                        abortThreshold),
                CommandLineOptionTest.prepareBooleanFlag("UseRTMForStackLocks",
                        useStackLock),
                "-XX:RTMTotalCountIncrRate=1",
                "-XX:+PrintPreciseRTMLockingStatistics",
                AbortProvoker.class.getName(),
                AbortType.XABORT.toString(),
                Boolean.toString(!useStackLock));

        outputAnalyzer.shouldHaveExitValue(0);

        List<RTMLockingStatistics> statistics = RTMLockingStatistics.fromString(
                provoker.getMethodWithLockName(), outputAnalyzer.getOutput());

        Asserts.assertEQ(statistics.size(), 1, "VM output should contain "
                + "exactly one RTM locking statistics entry for method "
                + provoker.getMethodWithLockName());

        Asserts.assertEQ(statistics.get(0).getTotalLocks(), abortThreshold,
                String.format("Expected that method with rtm lock elision was"
                        + " deoptimized after %d lock attempts",
                        abortThreshold));
    }

    public static void main(String args[]) throws Throwable {
         new TestRTMAbortThreshold().runTestCases();
    }
}

