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
 * @summary Verify UseRTMLocking option processing on CPUs without
 *          rtm support.
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @requires vm.flagless
 * @requires !vm.rtm.cpu & vm.rtm.compiler
 * @run driver compiler.rtm.cli.TestUseRTMLockingOptionOnUnsupportedCPU
 */

package compiler.rtm.cli;

import jdk.test.lib.process.ExitCode;
import jdk.test.lib.Platform;
import jdk.test.lib.cli.CommandLineOptionTest;

public class TestUseRTMLockingOptionOnUnsupportedCPU {
    private static final String DEFAULT_VALUE = "false";

    public void runTestCases() throws Throwable {
        String unrecognizedOption
                = CommandLineOptionTest.getUnrecognizedOptionErrorMessage(
                "UseRTMLocking");
        String errorMessage = RTMGenericCommandLineOptionTest.RTM_INSTR_ERROR;

        if (Platform.isX86() || Platform.isX64() || Platform.isPPC()) {
            String shouldFailMessage = "JVM startup should fail with option " +
                                       "-XX:+UseRTMLocking on unsupported CPU";

            try {
                // verify that we get an error when use +UseRTMLocking
                // on unsupported CPU
                CommandLineOptionTest.verifySameJVMStartup(
                        new String[] { errorMessage },
                        new String[] { unrecognizedOption }, shouldFailMessage,
                        shouldFailMessage + ". Error message should be shown.",
                        ExitCode.FAIL, "-XX:+UseRTMLocking");
            } catch (Throwable e) {
                // verify that we get an error when use +UseRTMLocking
                // on unsupported OS. It might be the case that although CPU
                // supports RTM the OS version does not support RTM
                if (Platform.isPPC()) {
                    String errorMessage2 = RTMGenericCommandLineOptionTest.RTM_OS_ERROR;
                    String shouldFailMessage2 = "JVM startup should fail with option " +
                                                "-XX:+UseRTMLocking on unsupported CPU or " +
                                                "OS version";

                    CommandLineOptionTest.verifySameJVMStartup(
                            new String[] { errorMessage2 },
                            new String[] { unrecognizedOption}, shouldFailMessage2,
                            shouldFailMessage2 + ". Error message should be shown.",
                            ExitCode.FAIL, "-XX:+UseRTMLocking");
                } else {
                    throw e; // checking unsupported OS error is not necessary
                }
            }

            String shouldPassMessage = "JVM startup should pass with option "
                    + "-XX:-UseRTMLocking even on unsupported CPU";
            // verify that we can pass -UseRTMLocking without
            // getting any error messages
            CommandLineOptionTest.verifySameJVMStartup(null, new String[] {
                    errorMessage, unrecognizedOption }, shouldPassMessage,
                    shouldPassMessage + " without any warnings", ExitCode.OK,
                    "-XX:-UseRTMLocking");

            // verify that UseRTMLocking is false by default
            CommandLineOptionTest.verifyOptionValueForSameVM("UseRTMLocking",
                    TestUseRTMLockingOptionOnUnsupportedCPU.DEFAULT_VALUE,
                    String.format("Default value of option 'UseRTMLocking' "
                        +"should be '%s'", DEFAULT_VALUE));
        } else {
            String shouldFailMessage = "RTMLocking should be unrecognized"
                    + " on non-x86 CPUs. JVM startup should fail."
                    + "Error message should be shown";
            // verify that on non-x86 CPUs RTMLocking could not be used
            CommandLineOptionTest.verifySameJVMStartup(
                    new String[] { unrecognizedOption },
                    null, shouldFailMessage, shouldFailMessage,
                    ExitCode.FAIL, "-XX:+UseRTMLocking");

            CommandLineOptionTest.verifySameJVMStartup(
                    new String[] { unrecognizedOption },
                    null, shouldFailMessage, shouldFailMessage,
                    ExitCode.FAIL, "-XX:-UseRTMLocking");
        }
    }

    public static void main(String args[]) throws Throwable {
        new TestUseRTMLockingOptionOnUnsupportedCPU().runTestCases();
    }
}
