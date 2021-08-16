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
import jdk.test.lib.cli.predicate.NotPredicate;
import jdk.test.lib.cli.predicate.OrPredicate;

/**
 * UseSHA specific test case targeted to CPUs which don't support all
 * sha* instructions.
 */
public class UseSHASpecificTestCaseForUnsupportedCPU
        extends DigestOptionsBase.TestCase {
    public UseSHASpecificTestCaseForUnsupportedCPU(String optionName) {
        super(DigestOptionsBase.USE_SHA_OPTION, new NotPredicate(
                IntrinsicPredicates.ANY_SHA_INSTRUCTION_AVAILABLE));

        Asserts.assertEQ(optionName, DigestOptionsBase.USE_SHA_OPTION,
                "Test case should be used for " + DigestOptionsBase.USE_SHA_OPTION
                        + " option only.");
    }

    @Override
    protected void verifyWarnings() throws Throwable {
        // Verify that attempt to use UseSHA option will cause a warning.
        String shouldPassMessage = String.format("JVM startup should pass with"
                + " '%s' option on unsupported CPU, but there should be"
                + "the message shown.", optionName);
        CommandLineOptionTest.verifySameJVMStartup(new String[] {
                        DigestOptionsBase.getWarningForUnsupportedCPU(optionName)
                }, null, shouldPassMessage, shouldPassMessage, ExitCode.OK,
                CommandLineOptionTest.prepareBooleanFlag(optionName, true));
    }

    @Override
    protected void verifyOptionValues() throws Throwable {
        // Verify that UseSHA option remains disabled even if all
        // UseSHA*Intrinsics were enabled.
        CommandLineOptionTest.verifyOptionValueForSameVM(
                DigestOptionsBase.USE_SHA_OPTION, "false", String.format(
                    "%s option should be disabled on unsupported CPU"
                        + " even if all UseSHA*Intrinsics options were enabled.",
                    DigestOptionsBase.USE_SHA_OPTION),
                DigestOptionsBase.UNLOCK_DIAGNOSTIC_VM_OPTIONS,
                CommandLineOptionTest.prepareBooleanFlag(
                        DigestOptionsBase.USE_SHA1_INTRINSICS_OPTION, true),
                CommandLineOptionTest.prepareBooleanFlag(
                        DigestOptionsBase.USE_SHA256_INTRINSICS_OPTION, true),
                CommandLineOptionTest.prepareBooleanFlag(
                        DigestOptionsBase.USE_SHA512_INTRINSICS_OPTION, true),
                CommandLineOptionTest.prepareBooleanFlag(
                        DigestOptionsBase.USE_SHA3_INTRINSICS_OPTION, true));

        // Verify that UseSHA option remains disabled even if all
        // UseSHA*Intrinsics options were enabled and UseSHA was enabled as well.
        CommandLineOptionTest.verifyOptionValueForSameVM(
                DigestOptionsBase.USE_SHA_OPTION, "false", String.format(
                    "%s option should be disabled on unsupported CPU"
                        + " even if all UseSHA*Intrinsics options were enabled"
                        + " and %s was enabled as well",
                    DigestOptionsBase.USE_SHA_OPTION,
                    DigestOptionsBase.USE_SHA_OPTION),
                DigestOptionsBase.UNLOCK_DIAGNOSTIC_VM_OPTIONS,
                CommandLineOptionTest.prepareBooleanFlag(
                        DigestOptionsBase.USE_SHA_OPTION, true),
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
