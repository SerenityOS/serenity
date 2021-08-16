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
import jdk.test.lib.process.ExitCode;
import jdk.test.lib.Platform;
import jdk.test.lib.cli.CommandLineOptionTest;
import jdk.test.lib.cli.predicate.AndPredicate;
import jdk.test.lib.cli.predicate.NotPredicate;
import jdk.test.lib.cli.predicate.OrPredicate;

/**
 * Test case specific to UseSHA*Intrinsics options targeted to CPUs
 * which don't support required instruction, but support other SHA-related
 * instructions.
 *
 * For example, CPU supports sha1 instruction, but doesn't support sha256 or
 * sha512.
 */
public class UseSHAIntrinsicsSpecificTestCaseForUnsupportedCPU
        extends DigestOptionsBase.TestCase {
    public UseSHAIntrinsicsSpecificTestCaseForUnsupportedCPU(
            String optionName) {
        // execute test case on CPU that supports any sha* instructions,
        // but does not support sha* instruction required by the tested option.
        super(optionName, new AndPredicate(
                IntrinsicPredicates.ANY_SHA_INSTRUCTION_AVAILABLE,
                new NotPredicate(DigestOptionsBase.getPredicateForOption(optionName))));
    }
    @Override
    protected void verifyWarnings() throws Throwable {
        String shouldPassMessage = String.format("JVM should start with "
                + "'-XX:+%s' flag, but output should contain warning.",
                optionName);
        // Verify that attempt to enable the tested option will cause a warning
        CommandLineOptionTest.verifySameJVMStartup(new String[] {
                        DigestOptionsBase.getWarningForUnsupportedCPU(optionName)
                }, null, shouldPassMessage, shouldPassMessage, ExitCode.OK,
                DigestOptionsBase.UNLOCK_DIAGNOSTIC_VM_OPTIONS,
                CommandLineOptionTest.prepareBooleanFlag(optionName, true));
    }
}
