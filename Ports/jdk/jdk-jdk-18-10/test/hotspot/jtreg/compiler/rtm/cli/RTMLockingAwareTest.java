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
import jdk.test.lib.cli.CommandLineOptionTest;

import java.util.LinkedList;
import java.util.List;

/**
 * Base for all RTM-related CLI tests on options whose processing depends
 * on UseRTMLocking value.
 *
 * Since UseRTMLocking option could be used when both CPU and VM supports RTM
 * locking, this test will be skipped on all unsupported configurations.
 */
public abstract class RTMLockingAwareTest
        extends RTMGenericCommandLineOptionTest {
    protected final String warningMessage;
    protected final String[] correctValues;
    protected final String[] incorrectValues;
    /**
     * Constructs new test for option {@code optionName} that should be executed
     * only on CPU with RTM support.
     * Test will be executed using set of correct values from
     * {@code correctValues} and set of incorrect values from
     * {@code incorrectValues}.
     *
     * @param optionName name of option to be tested
     * @param isBoolean {@code true} if tested option is binary
     * @param isExperimental {@code true} if tested option is experimental
     * @param defaultValue default value of tested option
     * @param correctValues array with correct values, that should not emit
     *                      {@code warningMessage} to VM output
     * @param incorrectValues array with incorrect values, that should emit
     *                        {@code waningMessage} to VM output
     * @param warningMessage warning message associated with tested option
     */
    protected RTMLockingAwareTest(String optionName, boolean isBoolean,
            boolean isExperimental, String defaultValue,
            String[] correctValues, String[] incorrectValues,
            String warningMessage) {
        super(optionName, isBoolean, isExperimental, defaultValue);
        this.correctValues = correctValues;
        this.incorrectValues = incorrectValues;
        this.warningMessage = warningMessage;
    }

    @Override
    protected void verifyJVMStartup() throws Throwable {
        // Run generic sanity checks
        super.verifyJVMStartup();
        // Verify how option values will be processed depending on
        // UseRTMLocking value.
        if (correctValues != null) {
            for (String correctValue : correctValues) {
                // For correct values it is expected to see no warnings
                // regardless to UseRTMLocking
                verifyStartupWarning(correctValue, true, false);
                verifyStartupWarning(correctValue, false, false);
            }
        }

        if (incorrectValues != null) {
            for (String incorrectValue : incorrectValues) {
                // For incorrect values it is expected to see warning
                // only with -XX:+UseRTMLocking
                verifyStartupWarning(incorrectValue, true, true);
                verifyStartupWarning(incorrectValue, false, false);
            }
        }
    }

    @Override
    protected void verifyOptionValues() throws Throwable {
        super.verifyOptionValues();
        // Verify how option values will be setup after processing
        // depending on UseRTMLocking value
        if (correctValues != null) {
            for (String correctValue : correctValues) {
                // Correct value could be set up regardless to UseRTMLocking
                verifyOptionValues(correctValue, false, correctValue);
                verifyOptionValues(correctValue, true, correctValue);
            }
        }

        if (incorrectValues != null) {
            for (String incorrectValue : incorrectValues) {
                // With -XX:+UseRTMLocking, incorrect value will be changed to
                // default value.
                verifyOptionValues(incorrectValue, false, incorrectValue);
                verifyOptionValues(incorrectValue, true, defaultValue);
            }
        }
    }

    private void verifyStartupWarning(String value, boolean useRTMLocking,
            boolean isWarningExpected) throws Throwable {
        String warnings[] = new String[] { warningMessage };
        List<String> options = new LinkedList<>();
        options.add(CommandLineOptionTest.prepareBooleanFlag("UseRTMLocking",
                useRTMLocking));

        if (isExperimental) {
            options.add(CommandLineOptionTest.UNLOCK_EXPERIMENTAL_VM_OPTIONS);
        }
        options.add(prepareOptionValue(value));

        String errorString =  String.format("JVM should start with option '%s'"
                + "'%nWarnings should be shown: %s", optionName,
                isWarningExpected);
        CommandLineOptionTest.verifySameJVMStartup(
                (isWarningExpected ? warnings : null),
                (isWarningExpected ? null : warnings),
                errorString, errorString, ExitCode.OK,
                options.toArray(new String[options.size()]));
    }

    private void verifyOptionValues(String value, boolean useRTMLocking,
            String expectedValue) throws Throwable {
        List<String> options = new LinkedList<>();
        options.add(CommandLineOptionTest.prepareBooleanFlag("UseRTMLocking",
                useRTMLocking));

        if (isExperimental) {
            options.add(CommandLineOptionTest.UNLOCK_EXPERIMENTAL_VM_OPTIONS);
        }
        options.add(prepareOptionValue(value));

        CommandLineOptionTest.verifyOptionValueForSameVM(optionName,
                expectedValue, String.format("Option '%s' should have '%s' "
                        + "value if '%s' flag set",
                        optionName, expectedValue, prepareOptionValue(value)),
                options.toArray(new String[options.size()]));
    }
}
