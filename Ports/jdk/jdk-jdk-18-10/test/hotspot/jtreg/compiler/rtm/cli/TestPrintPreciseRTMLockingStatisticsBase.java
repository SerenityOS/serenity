/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

package compiler.rtm.cli;

import jdk.test.lib.process.ExitCode;
import jdk.test.lib.Platform;
import jdk.test.lib.cli.CommandLineOptionTest;

public abstract class TestPrintPreciseRTMLockingStatisticsBase
        extends RTMGenericCommandLineOptionTest {
    protected static final String DEFAULT_VALUE = "false";

    protected TestPrintPreciseRTMLockingStatisticsBase() {
        super("PrintPreciseRTMLockingStatistics", true, false,
                TestPrintPreciseRTMLockingStatisticsBase.DEFAULT_VALUE);
    }

    @Override
    protected void runNonX86TestCases() throws Throwable {
        verifyJVMStartup();
        verifyOptionValues();
    }

    @Override
    protected void verifyJVMStartup() throws Throwable {
        if (Platform.isServer()) {
            if (!Platform.isDebugBuild()) {
                String shouldFailMessage = String.format("VM option '%s' is "
                        + "diagnostic%nJVM startup should fail without "
                        + "-XX:\\+UnlockDiagnosticVMOptions flag", optionName);
                String shouldPassMessage = String.format("VM option '%s' is "
                        + "diagnostic%nJVM startup should pass with "
                        + "-XX:\\+UnlockDiagnosticVMOptions in debug build",
                        optionName);
                String errorMessage = CommandLineOptionTest.
                        getDiagnosticOptionErrorMessage(optionName);
                // verify that option is actually diagnostic
                CommandLineOptionTest.verifySameJVMStartup(
                        new String[] { errorMessage }, null, shouldFailMessage,
                        shouldFailMessage, ExitCode.FAIL,
                        prepareOptionValue("true"));

                CommandLineOptionTest.verifySameJVMStartup(null,
                        new String[] { errorMessage }, shouldPassMessage,
                        shouldPassMessage + "without any warnings", ExitCode.OK,
                        CommandLineOptionTest.UNLOCK_DIAGNOSTIC_VM_OPTIONS,
                        prepareOptionValue("true"));
            } else {
                String shouldPassMessage = String.format("JVM startup should "
                                + "pass with '%s' option in debug build",
                                optionName);
                CommandLineOptionTest.verifySameJVMStartup(null, null,
                        shouldPassMessage, shouldPassMessage,
                        ExitCode.OK, prepareOptionValue("true"));
            }
        } else {
            String errorMessage = CommandLineOptionTest.
                    getUnrecognizedOptionErrorMessage(optionName);
            String shouldFailMessage =  String.format("JVM startup should fail"
                    + " with '%s' option in not debug build", optionName);
            CommandLineOptionTest.verifySameJVMStartup(
                    new String[]{errorMessage}, null, shouldFailMessage,
                    shouldFailMessage, ExitCode.FAIL,
                    CommandLineOptionTest.UNLOCK_DIAGNOSTIC_VM_OPTIONS,
                    prepareOptionValue("true"));
        }
    }

    @Override
    protected void verifyOptionValues() throws Throwable {
        if (Platform.isServer()) {
            // Verify default value
            CommandLineOptionTest.verifyOptionValueForSameVM(optionName,
                    TestPrintPreciseRTMLockingStatisticsBase.DEFAULT_VALUE,
                    String.format("Option '%s' should have '%s' default value",
                            optionName,
                       TestPrintPreciseRTMLockingStatisticsBase.DEFAULT_VALUE),
                    CommandLineOptionTest.UNLOCK_DIAGNOSTIC_VM_OPTIONS);
        }
    }
}
