/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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

package compiler.intrinsics.sha.cli.testcases;

import compiler.intrinsics.sha.cli.DigestOptionsBase;
import jdk.test.lib.process.ExitCode;
import jdk.test.lib.Platform;
import jdk.test.lib.cli.CommandLineOptionTest;
import jdk.test.lib.cli.predicate.AndPredicate;
import jdk.test.lib.cli.predicate.NotPredicate;

/**
 * Generic test case for SHA-related options targeted to AArch64 CPUs
 * which don't support instruction required by the tested option.
 */
public class GenericTestCaseForUnsupportedAArch64CPU extends
        DigestOptionsBase.TestCase {

    final private boolean checkUseSHA;

    public GenericTestCaseForUnsupportedAArch64CPU(String optionName) {
        this(optionName, true);
    }

    public GenericTestCaseForUnsupportedAArch64CPU(String optionName, boolean checkUseSHA) {
        super(optionName, new AndPredicate(Platform::isAArch64,
                new NotPredicate(DigestOptionsBase.getPredicateForOption(
                        optionName))));

        this.checkUseSHA = checkUseSHA;
    }

    @Override
    protected void verifyWarnings() throws Throwable {
        String shouldPassMessage = String.format("JVM startup should pass with"
                + "option '-XX:-%s' without any warnings", optionName);
        //Verify that option could be disabled without any warnings.
        CommandLineOptionTest.verifySameJVMStartup(null, new String[] {
                        DigestOptionsBase.getWarningForUnsupportedCPU(optionName)
                }, shouldPassMessage, shouldPassMessage, ExitCode.OK,
                DigestOptionsBase.UNLOCK_DIAGNOSTIC_VM_OPTIONS,
                CommandLineOptionTest.prepareBooleanFlag(optionName, false));

        if (checkUseSHA) {
            shouldPassMessage = String.format("If JVM is started with '-XX:-"
                    + "%s' '-XX:+%s', output should contain warning.",
                    DigestOptionsBase.USE_SHA_OPTION, optionName);

            // Verify that when the tested option is enabled, then
            // a warning will occur in VM output if UseSHA is disabled.
            if (!optionName.equals(DigestOptionsBase.USE_SHA_OPTION)) {
                CommandLineOptionTest.verifySameJVMStartup(
                        new String[] { DigestOptionsBase.getWarningForUnsupportedCPU(optionName) },
                        null,
                        shouldPassMessage,
                        shouldPassMessage,
                        ExitCode.OK,
                        DigestOptionsBase.UNLOCK_DIAGNOSTIC_VM_OPTIONS,
                        CommandLineOptionTest.prepareBooleanFlag(DigestOptionsBase.USE_SHA_OPTION, false),
                        CommandLineOptionTest.prepareBooleanFlag(optionName, true));
            }
        }
    }

    @Override
    protected void verifyOptionValues() throws Throwable {
        // Verify that option is disabled by default.
        CommandLineOptionTest.verifyOptionValueForSameVM(optionName, "false",
                String.format("Option '%s' should be disabled by default",
                        optionName),
                DigestOptionsBase.UNLOCK_DIAGNOSTIC_VM_OPTIONS);

        // Verify that option is disabled even if it was explicitly enabled
        // using CLI options.
        CommandLineOptionTest.verifyOptionValueForSameVM(optionName, "false",
                String.format("Option '%s' should be off on unsupported "
                        + "AArch64CPU even if set to true directly", optionName),
                DigestOptionsBase.UNLOCK_DIAGNOSTIC_VM_OPTIONS,
                CommandLineOptionTest.prepareBooleanFlag(optionName, true));

        if (checkUseSHA) {
            // Verify that option is disabled when +UseSHA was passed to JVM.
            CommandLineOptionTest.verifyOptionValueForSameVM(optionName, "false",
                    String.format("Option '%s' should be off on unsupported "
                            + "AArch64CPU even if %s flag set to JVM",
                            optionName, CommandLineOptionTest.prepareBooleanFlag(
                                DigestOptionsBase.USE_SHA_OPTION, true)),
                    DigestOptionsBase.UNLOCK_DIAGNOSTIC_VM_OPTIONS,
                    CommandLineOptionTest.prepareBooleanFlag(
                            DigestOptionsBase.USE_SHA_OPTION, true));
        }
    }
}
