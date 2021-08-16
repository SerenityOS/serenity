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
 * @summary Verify that UseRTMXendForLockBusy option affects
 *          method behaviour if lock is busy.
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @requires vm.rtm.cpu & vm.rtm.compiler
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI
 *                   compiler.rtm.locking.TestUseRTMXendForLockBusy
 */

package compiler.rtm.locking;

import compiler.testlibrary.rtm.AbortType;
import compiler.testlibrary.rtm.BusyLock;
import compiler.testlibrary.rtm.CompilableTest;
import compiler.testlibrary.rtm.RTMLockingStatistics;
import compiler.testlibrary.rtm.RTMTestBase;
import jdk.test.lib.Asserts;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.cli.CommandLineOptionTest;

import java.util.List;

/**
 * Test verifies that with +UseRTMXendForLockBusy there will be no aborts
 * forced by the test.
 */
public class TestUseRTMXendForLockBusy {
    private final static int LOCKING_TIME = 5000;

    protected void runTestCases() throws Throwable {
        // inflated lock, xabort on lock busy
        verifyXendForLockBusy(true, false);
        // inflated lock, xend on lock busy
        verifyXendForLockBusy(true, true);
        // stack lock, xabort on lock busy
        verifyXendForLockBusy(false, false);
        // stack lock, xend on lock busy
        verifyXendForLockBusy(false, true);
    }

    private void verifyXendForLockBusy(boolean inflateMonitor,
            boolean useXend) throws Throwable {
        CompilableTest test = new BusyLock();

        OutputAnalyzer outputAnalyzer = RTMTestBase.executeRTMTest(
                test,
                CommandLineOptionTest.prepareBooleanFlag("UseRTMForStackLocks",
                        !inflateMonitor),
                CommandLineOptionTest.prepareBooleanFlag(
                        "UseRTMXendForLockBusy",
                        useXend),
                "-XX:RTMRetryCount=0",
                "-XX:RTMTotalCountIncrRate=1",
                "-XX:+PrintPreciseRTMLockingStatistics",
                BusyLock.class.getName(),
                Boolean.toString(inflateMonitor),
                Integer.toString(TestUseRTMXendForLockBusy.LOCKING_TIME)
        );

        outputAnalyzer.shouldHaveExitValue(0);

        List<RTMLockingStatistics> statistics = RTMLockingStatistics.fromString(
                test.getMethodWithLockName(), outputAnalyzer.getOutput());

        Asserts.assertEQ(statistics.size(), 1, "VM output should contain "
                + "exactly one rtm locking statistics entry for method "
                + test.getMethodWithLockName());

        long aborts = statistics.get(0).getAborts(AbortType.XABORT);

        if (useXend) {
            Asserts.assertEQ(aborts, 0L,
                    "Expected to get no aborts on busy lock");
        } else {
            Asserts.assertGT(aborts, 0L,
                    "Expected to get at least one abort on busy lock");
        }
    }

    public static void main(String args[]) throws Throwable {
        new TestUseRTMXendForLockBusy().runTestCases();
    }
}
