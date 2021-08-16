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
import compiler.testlibrary.sha.predicate.IntrinsicPredicates;
import jdk.test.lib.Asserts;
import jdk.test.lib.process.ExitCode;
import jdk.test.lib.Platform;
import jdk.test.lib.cli.CommandLineOptionTest;
import jdk.test.lib.cli.predicate.AndPredicate;
import jdk.test.lib.cli.predicate.OrPredicate;

/**
 * UseSHA specific test case targeted to CPUs which support any sha* instruction.
 */
public class UseSHASpecificTestCaseForSupportedCPU
        extends DigestOptionsBase.TestCase {
    public UseSHASpecificTestCaseForSupportedCPU(String optionName) {
        super(DigestOptionsBase.USE_SHA_OPTION,
              IntrinsicPredicates.ANY_SHA_INSTRUCTION_AVAILABLE);

        Asserts.assertEQ(optionName, DigestOptionsBase.USE_SHA_OPTION,
                String.format("Test case should be used for '%s' option only.",
                        DigestOptionsBase.USE_SHA_OPTION));
    }

    @Override
    protected void verifyWarnings() throws Throwable {
        String shouldPassMessage = String.format("JVM startup should pass when"
                        + " %s was passed and all UseSHA*Intrinsics options "
                        + "were disabled",
                        CommandLineOptionTest.prepareBooleanFlag(
                            DigestOptionsBase.USE_SHA_OPTION, true));
        // Verify that there will be no warnings when +UseSHA was passed and
        // all UseSHA*Intrinsics options were disabled.
        CommandLineOptionTest.verifySameJVMStartup(
                null, new String[] { ".*UseSHA.*" }, shouldPassMessage,
                shouldPassMessage, ExitCode.OK,
                DigestOptionsBase.UNLOCK_DIAGNOSTIC_VM_OPTIONS,
                CommandLineOptionTest.prepareBooleanFlag(
                        DigestOptionsBase.USE_SHA_OPTION, true),
                CommandLineOptionTest.prepareBooleanFlag(
                        DigestOptionsBase.USE_SHA1_INTRINSICS_OPTION, false),
                CommandLineOptionTest.prepareBooleanFlag(
                        DigestOptionsBase.USE_SHA256_INTRINSICS_OPTION, false),
                CommandLineOptionTest.prepareBooleanFlag(
                        DigestOptionsBase.USE_SHA512_INTRINSICS_OPTION, false),
                CommandLineOptionTest.prepareBooleanFlag(
                        DigestOptionsBase.USE_SHA3_INTRINSICS_OPTION, false));
    }

    @Override
    protected void verifyOptionValues() throws Throwable {
        // Verify that UseSHA is disabled when all UseSHA*Intrinsics are
        // disabled.
        CommandLineOptionTest.verifyOptionValueForSameVM(
                DigestOptionsBase.USE_SHA_OPTION, "false", String.format(
                "'%s' option should be disabled when all UseSHA*Intrinsics are"
                        + " disabled", DigestOptionsBase.USE_SHA_OPTION),
                DigestOptionsBase.UNLOCK_DIAGNOSTIC_VM_OPTIONS,
                CommandLineOptionTest.prepareBooleanFlag(
                        DigestOptionsBase.USE_SHA1_INTRINSICS_OPTION, false),
                CommandLineOptionTest.prepareBooleanFlag(
                        DigestOptionsBase.USE_SHA256_INTRINSICS_OPTION, false),
                CommandLineOptionTest.prepareBooleanFlag(
                        DigestOptionsBase.USE_SHA512_INTRINSICS_OPTION, false),
                CommandLineOptionTest.prepareBooleanFlag(
                        DigestOptionsBase.USE_SHA3_INTRINSICS_OPTION, false));

        CommandLineOptionTest.verifyOptionValueForSameVM(
                // Verify that UseSHA is disabled when all UseSHA*Intrinsics are
                // disabled even if it was explicitly enabled.
                DigestOptionsBase.USE_SHA_OPTION, "false",
                String.format("'%s' option should be disabled when all "
                        + "UseSHA*Intrinsics are disabled even if %s flag set "
                        + "to JVM", DigestOptionsBase.USE_SHA_OPTION,
                        CommandLineOptionTest.prepareBooleanFlag(
                             DigestOptionsBase.USE_SHA_OPTION, true)),
                DigestOptionsBase.UNLOCK_DIAGNOSTIC_VM_OPTIONS,
                CommandLineOptionTest.prepareBooleanFlag(
                        DigestOptionsBase.USE_SHA_OPTION, true),
                CommandLineOptionTest.prepareBooleanFlag(
                        DigestOptionsBase.USE_SHA1_INTRINSICS_OPTION, false),
                CommandLineOptionTest.prepareBooleanFlag(
                        DigestOptionsBase.USE_SHA256_INTRINSICS_OPTION, false),
                CommandLineOptionTest.prepareBooleanFlag(
                        DigestOptionsBase.USE_SHA512_INTRINSICS_OPTION, false),
                CommandLineOptionTest.prepareBooleanFlag(
                        DigestOptionsBase.USE_SHA3_INTRINSICS_OPTION, false));

        // Verify that explicitly disabled UseSHA option remains disabled even
        // if all UseSHA*Intrinsics options were enabled.
        CommandLineOptionTest.verifyOptionValueForSameVM(
                DigestOptionsBase.USE_SHA_OPTION, "false",
                String.format("'%s' option should be disabled if %s flag "
                        + "set even if all UseSHA*Intrinsics were enabled",
                        DigestOptionsBase.USE_SHA_OPTION,
                        CommandLineOptionTest.prepareBooleanFlag(
                            DigestOptionsBase.USE_SHA_OPTION, false)),
                DigestOptionsBase.UNLOCK_DIAGNOSTIC_VM_OPTIONS,
                CommandLineOptionTest.prepareBooleanFlag(
                        DigestOptionsBase.USE_SHA_OPTION, false),
                CommandLineOptionTest.prepareBooleanFlag(
                        DigestOptionsBase.USE_SHA1_INTRINSICS_OPTION, true),
                CommandLineOptionTest.prepareBooleanFlag(
                        DigestOptionsBase.USE_SHA256_INTRINSICS_OPTION, true),
                CommandLineOptionTest.prepareBooleanFlag(
                        DigestOptionsBase.USE_SHA512_INTRINSICS_OPTION, true),
                CommandLineOptionTest.prepareBooleanFlag(
                        DigestOptionsBase.USE_SHA3_INTRINSICS_OPTION, true));
    }
}
