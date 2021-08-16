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

package compiler.arguments;

import jdk.test.lib.process.ExitCode;
import jdk.test.lib.Platform;
import jdk.test.lib.cli.CommandLineOptionTest;

/**
 * Test on bit manipulation related command line options,
 * that should be executed on CPU that does not support
 * required features.
 *
 * Note that this test intended to verify that VM could be launched with
 * specific options and that values of these options processed correctly.
 * In order to do that test launch a new VM with tested options, the same
 * flavor-specific flag as one that was used for parent VM (-client, -server,
 * -minimal, -graal) and '-version'.
 */
public class BMIUnsupportedCPUTest extends BMICommandLineOptionTestBase {

    /**
     * Construct new test on {@code optionName} option.
     *
     * @param optionName Name of the option to be tested
     *                   without -XX:[+-] prefix.
     * @param warningMessage Message that can occur in VM output
     *                       if CPU on test box does not support
     *                       features required by the option.
     * @param cpuFeatures CPU features requires by the option.
     */
    public BMIUnsupportedCPUTest(String optionName,
                                 String warningMessage,
                                 String... cpuFeatures) {
        super(optionName, warningMessage, null, cpuFeatures);
    }

    @Override
    public void runTestCases() throws Throwable {
        if (Platform.isX86() || Platform.isX64()) {
            unsupportedX86CPUTestCases();
        } else {
            unsupportedNonX86CPUTestCases();
        }
    }

    /**
     * Run test cases common for all bit manipulation related VM options
     * targeted to X86 CPU that does not support required features.
     *
     * @throws Throwable if test failed.
     */
    public void unsupportedX86CPUTestCases() throws Throwable {

        /*
          Verify that VM will successfully start up, but output will contain a
          warning. VM will be launched with following options:
          -XX:+<tested option> -version
        */
        String errorString = String.format("JVM should start with '-XX:+%s' "
                + "flag, but output should contain warning.", optionName);
        CommandLineOptionTest.verifySameJVMStartup(
                new String[] { warningMessage }, new String[] { errorMessage },
                errorString, String.format("Option '%s' is unsupported.%n"
                + "Warning expected to be shown.", optionName), ExitCode.OK,
                CommandLineOptionTest.prepareBooleanFlag(
                        optionName, true));

        /*
          Verify that VM will successfully startup without any warnings.
          VM will be launched with following options:
          -XX:-<tested option> -version
        */
        errorString = String.format("JVM should start with '-XX:-%s' flag "
                        + "without any warnings", optionName);
        CommandLineOptionTest.verifySameJVMStartup(null,
                new String[] { warningMessage, errorMessage },
                errorString, errorString, ExitCode.OK,
                CommandLineOptionTest.prepareBooleanFlag(optionName, false));

        /*
         * Verify that on unsupported CPUs option is off by default. VM will be
         * launched with following options: -version
         */
        CommandLineOptionTest.verifyOptionValueForSameVM(optionName, "false",
                String.format("Option '%s' is expected to have default value "
                        + "'false' since feature required is not supported "
                        + "on CPU", optionName));

        /*
          Verify that on unsupported CPUs option will be off even if
          it was explicitly turned on by user. VM will be launched with
          following options: -XX:+<tested option> -version
        */
        CommandLineOptionTest.verifyOptionValueForSameVM(optionName, "false",
                String.format("Option '%s' is expected to have default value"
                        + " 'false' since feature required is not supported on"
                        + " CPU even if user set another value.", optionName),
                CommandLineOptionTest.prepareBooleanFlag(optionName, true));

    }

    /**
     * Run test cases common for all bit manipulation related VM options
     * targeted to non-X86 CPU that does not support required features.
     *
     * @throws Throwable if test failed.
     */
    public void unsupportedNonX86CPUTestCases() throws Throwable {

        /*
          Verify that VM known nothing about tested option. VM will be launched
          with following options: -XX:[+-]<tested option> -version
        */
        CommandLineOptionTest.verifySameJVMStartup(
                new String[] { errorMessage }, null,
                String.format("JVM startup should fail with '-XX:+%s' flag."
                        + "%nOption should be unknown (non-X86CPU).",
                        optionName), "", ExitCode.FAIL,
                CommandLineOptionTest.prepareBooleanFlag(optionName, true));

        CommandLineOptionTest.verifySameJVMStartup(
                new String[] { errorMessage }, null,
                String.format("JVM startup should fail with '-XX:-%s' flag."
                        + "%nOption should be unknown (non-X86CPU)",
                        optionName), "", ExitCode.FAIL,
                CommandLineOptionTest.prepareBooleanFlag(optionName, false));
    }
}

