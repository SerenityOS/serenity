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
 * @summary Verify UseRTMLocking option processing on CPU and OS with rtm support and
 *          on VM with rtm locking support.
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @requires vm.flagless
 * @requires vm.rtm.cpu & vm.rtm.compiler
 * @run driver compiler.rtm.cli.TestUseRTMLockingOptionOnSupportedConfig
 */

package compiler.rtm.cli;

import jdk.test.lib.process.ExitCode;
import jdk.test.lib.cli.CommandLineOptionTest;

public class TestUseRTMLockingOptionOnSupportedConfig {
    private static final String DEFAULT_VALUE = "false";

    public void runTestCases() throws Throwable {
        String unrecongnizedOption
                =  CommandLineOptionTest.getUnrecognizedOptionErrorMessage(
                "UseRTMLocking");
        String shouldPassMessage = "VM option 'UseRTMLocking' is experimental"
                + "%nJVM startup should pass with "
                + "-XX:+UnlockExperimentalVMOptions flag";
        // verify that there are no warning or error in VM output
        CommandLineOptionTest.verifySameJVMStartup(null,
                new String[]{
                        RTMGenericCommandLineOptionTest.RTM_INSTR_ERROR,
                        unrecongnizedOption
                }, shouldPassMessage, "There should not be any warning when use"
                        + "with -XX:+UnlockExperimentalVMOptions", ExitCode.OK,
                CommandLineOptionTest.UNLOCK_EXPERIMENTAL_VM_OPTIONS,
                "-XX:+UseRTMLocking"
        );

        CommandLineOptionTest.verifySameJVMStartup(null,
                new String[]{
                        RTMGenericCommandLineOptionTest.RTM_INSTR_ERROR,
                        unrecongnizedOption
                }, shouldPassMessage, "There should not be any warning when use"
                        + "with -XX:+UnlockExperimentalVMOptions", ExitCode.OK,
                CommandLineOptionTest.UNLOCK_EXPERIMENTAL_VM_OPTIONS,
                "-XX:-UseRTMLocking"
        );
        // verify that UseRTMLocking is of by default
        CommandLineOptionTest.verifyOptionValueForSameVM("UseRTMLocking",
                TestUseRTMLockingOptionOnSupportedConfig.DEFAULT_VALUE,
                String.format("Default value of option 'UseRTMLocking' should "
                    + "be '%s'", DEFAULT_VALUE),
                CommandLineOptionTest.UNLOCK_EXPERIMENTAL_VM_OPTIONS);
        // verify that we can change UseRTMLocking value
        CommandLineOptionTest.verifyOptionValueForSameVM("UseRTMLocking",
                TestUseRTMLockingOptionOnSupportedConfig.DEFAULT_VALUE,
                String.format("Default value of option 'UseRTMLocking' should "
                    + "be '%s'", DEFAULT_VALUE),
                CommandLineOptionTest.UNLOCK_EXPERIMENTAL_VM_OPTIONS,
                "-XX:-UseRTMLocking");
        CommandLineOptionTest.verifyOptionValueForSameVM("UseRTMLocking",
                "true", "Value of 'UseRTMLocking' should be set "
                        + "to 'true' if -XX:+UseRTMLocking flag set",
                CommandLineOptionTest.UNLOCK_EXPERIMENTAL_VM_OPTIONS,
                "-XX:+UseRTMLocking");
    }

    public static void main(String args[]) throws Throwable {
        new TestUseRTMLockingOptionOnSupportedConfig().runTestCases();
    }
}
