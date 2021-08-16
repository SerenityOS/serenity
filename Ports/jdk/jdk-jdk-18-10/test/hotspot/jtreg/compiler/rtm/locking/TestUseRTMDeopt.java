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
 * @summary Verify that UseRTMDeopt affects uncommon trap installation in
 *          compiled methods with synchronized block.
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @requires vm.rtm.cpu & vm.rtm.compiler
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm/native -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                          -XX:+WhiteBoxAPI
 *                          compiler.rtm.locking.TestUseRTMDeopt
 */

package compiler.rtm.locking;

import compiler.testlibrary.rtm.AbortProvoker;
import compiler.testlibrary.rtm.AbortType;
import compiler.testlibrary.rtm.RTMTestBase;
import jdk.test.lib.Asserts;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.cli.CommandLineOptionTest;

/**
 * Test verifies that usage of UseRTMDeopt option affects uncommon traps usage
 * for methods that use locking.
 */
public class TestUseRTMDeopt {

    protected void runTestCases() throws Throwable {
        verifyUseRTMDeopt(false);
        verifyUseRTMDeopt(true);
    }

    private void verifyUseRTMDeopt(boolean useRTMDeopt) throws Throwable {
        AbortProvoker provoker = AbortType.XABORT.provoker();
        String logFileName = String.format("rtm_%s_deopt.xml",
                useRTMDeopt ? "use" : "no");

        OutputAnalyzer outputAnalyzer = RTMTestBase.executeRTMTest(
                logFileName,
                provoker,
                CommandLineOptionTest.prepareBooleanFlag("UseRTMDeopt",
                        useRTMDeopt),
                AbortProvoker.class.getName(),
                AbortType.XABORT.toString()
        );

        outputAnalyzer.shouldHaveExitValue(0);

        int expectedUncommonTraps = useRTMDeopt ? 1 : 0;
        int installedUncommonTraps
                = RTMTestBase.installedRTMStateChangeTraps(logFileName);

        Asserts.assertEQ(expectedUncommonTraps, installedUncommonTraps,
                String.format("Expected to find %d uncommon traps "
                              + "installed with reason rtm_state_change.",
                        expectedUncommonTraps));
    }

    public static void main(String args[]) throws Throwable {
        new TestUseRTMDeopt().runTestCases();
    }
}
