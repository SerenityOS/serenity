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
 * @summary Verify UseRTMDeopt option processing on CPUs with rtm support
 *          when rtm locking is supported by VM.
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @requires vm.flagless
 * @requires vm.rtm.cpu & vm.rtm.compiler
 * @run driver compiler.rtm.cli.TestUseRTMDeoptOptionOnSupportedConfig
 */

package compiler.rtm.cli;

import jdk.test.lib.process.ExitCode;
import jdk.test.lib.cli.CommandLineOptionTest;

public class TestUseRTMDeoptOptionOnSupportedConfig {
    private static final String DEFAULT_VALUE = "false";

    public void runTestCases() throws Throwable {
        String shouldPassMessage = " JVM should startup with option '"
                + "-XX:+UseRTMDeopt' without any warnings";
        // verify that option could be turned on
        CommandLineOptionTest.verifySameJVMStartup(
                null, null, shouldPassMessage, shouldPassMessage, ExitCode.OK,
                "-XX:+UseRTMDeopt");
        shouldPassMessage = " JVM should startup with option '"
                + "-XX:-UseRTMDeopt' without any warnings";
        // verify that option could be turned off
        CommandLineOptionTest.verifySameJVMStartup(
                null, null, shouldPassMessage, shouldPassMessage, ExitCode.OK,
                "-XX:-UseRTMDeopt");
        String defValMessage = String.format("UseRTMDeopt should have '%s'"
                                    + "default value",
                        TestUseRTMDeoptOptionOnSupportedConfig.DEFAULT_VALUE);
        // verify default value
        CommandLineOptionTest.verifyOptionValueForSameVM("UseRTMDeopt",
                TestUseRTMDeoptOptionOnSupportedConfig.DEFAULT_VALUE,
                defValMessage);
        // verify default value
        CommandLineOptionTest.verifyOptionValueForSameVM("UseRTMDeopt",
                TestUseRTMDeoptOptionOnSupportedConfig.DEFAULT_VALUE,
                defValMessage,
                CommandLineOptionTest.UNLOCK_EXPERIMENTAL_VM_OPTIONS,
                "-XX:+UseRTMLocking");
        // verify that option is off when UseRTMLocking is off
        CommandLineOptionTest.verifyOptionValueForSameVM("UseRTMDeopt",
                "false", "UseRTMDeopt should be off when UseRTMLocking is off",
                CommandLineOptionTest.UNLOCK_EXPERIMENTAL_VM_OPTIONS,
                "-XX:-UseRTMLocking", "-XX:+UseRTMDeopt");
        // verify that option could be turned on
        CommandLineOptionTest.verifyOptionValueForSameVM("UseRTMDeopt", "true",
                "UseRTMDeopt should be on when UseRTMLocking is on and "
                        + "'-XX:+UseRTMDeopt' flag set",
                CommandLineOptionTest.UNLOCK_EXPERIMENTAL_VM_OPTIONS,
                "-XX:+UseRTMLocking", "-XX:+UseRTMDeopt");
    }

    public static void main(String args[]) throws Throwable {
        new TestUseRTMDeoptOptionOnSupportedConfig().runTestCases();
    }
}
