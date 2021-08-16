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
import jdk.test.lib.cli.predicate.NotPredicate;
import jdk.test.lib.cli.predicate.OrPredicate;

/**
 * Generic test case for SHA-related options targeted to any CPU except
 * AArch64, PPC, S390x, and X86.
 */
public class GenericTestCaseForOtherCPU extends
        DigestOptionsBase.TestCase {

    final private boolean checkUseSHA;

    public GenericTestCaseForOtherCPU(String optionName) {
        this(optionName, true);
    }

    public GenericTestCaseForOtherCPU(String optionName, boolean checkUseSHA) {
        // Execute the test case on any CPU except AArch64, PPC, S390x, and X86.
        super(optionName, new NotPredicate(
                              new OrPredicate(Platform::isAArch64,
                              new OrPredicate(Platform::isS390x,
                              new OrPredicate(Platform::isPPC,
                              new OrPredicate(Platform::isX64,
                                              Platform::isX86))))));

        this.checkUseSHA = checkUseSHA;
    }

    @Override
    protected void verifyWarnings() throws Throwable {
        String shouldPassMessage = String.format("JVM should start with "
                + "option '%s' without any warnings", optionName);
        // Verify that on non-x86 and non-AArch64 CPU usage of SHA-related
        // options will not cause any warnings.
        CommandLineOptionTest.verifySameJVMStartup(null,
                new String[] { ".*" + optionName + ".*" }, shouldPassMessage,
                shouldPassMessage, ExitCode.OK,
                DigestOptionsBase.UNLOCK_DIAGNOSTIC_VM_OPTIONS,
                CommandLineOptionTest.prepareBooleanFlag(optionName, true));

        CommandLineOptionTest.verifySameJVMStartup(null,
                new String[] { ".*" + optionName + ".*" }, shouldPassMessage,
                shouldPassMessage, ExitCode.OK,
                DigestOptionsBase.UNLOCK_DIAGNOSTIC_VM_OPTIONS,
                CommandLineOptionTest.prepareBooleanFlag(optionName, false));
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
                        + "CPU even if set to true directly", optionName),
                DigestOptionsBase.UNLOCK_DIAGNOSTIC_VM_OPTIONS,
                CommandLineOptionTest.prepareBooleanFlag(optionName, true));

        if (checkUseSHA) {
            // Verify that option is disabled when it explicitly disabled
            // using CLI options.
            CommandLineOptionTest.verifyOptionValueForSameVM(optionName, "false",
                    String.format("Option '%s' should be off on unsupported CPU"
                            + " even if '%s' flag set to JVM", optionName,
                            CommandLineOptionTest.prepareBooleanFlag(
                            DigestOptionsBase.USE_SHA_OPTION, true)),
                    DigestOptionsBase.UNLOCK_DIAGNOSTIC_VM_OPTIONS,
                    CommandLineOptionTest.prepareBooleanFlag(optionName, false));
        }
    }
}
