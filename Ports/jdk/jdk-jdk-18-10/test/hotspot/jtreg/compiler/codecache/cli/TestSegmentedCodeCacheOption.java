/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8015774
 * @summary Verify SegmentedCodeCache option's processing
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.compiler
 *          java.management
 *          jdk.internal.jvmstat/sun.jvmstat.monitor
 *
 * @run driver compiler.codecache.cli.TestSegmentedCodeCacheOption
 */

package compiler.codecache.cli;

import compiler.codecache.cli.common.CodeCacheOptions;
import jdk.test.lib.process.ExitCode;
import jdk.test.lib.Platform;
import jdk.test.lib.cli.CommandLineOptionTest;
import sun.hotspot.code.BlobType;

public class TestSegmentedCodeCacheOption {
    private static final String INT_MODE = "-Xint";
    private static final String TIERED_COMPILATION = "TieredCompilation";
    private static final String SEGMENTED_CODE_CACHE = "SegmentedCodeCache";
    private static final String USE_SEGMENTED_CODE_CACHE
            = CommandLineOptionTest.prepareBooleanFlag(SEGMENTED_CODE_CACHE,
                    true);
    private static final long THRESHOLD_CC_SIZE_VALUE
            = CodeCacheOptions.mB(240);
    private static final long BELOW_THRESHOLD_CC_SIZE
            = THRESHOLD_CC_SIZE_VALUE - CodeCacheOptions.mB(1);
    private static final String[] UNEXPECTED_MESSAGES = new String[] {
            ".*" + SEGMENTED_CODE_CACHE + ".*"
    };


    private static enum TestCase {
        JVM_STARTUP {
            @Override
            public void run() throws Throwable {
                // There should be no errors when we're trying to enable SCC ...
                String testCaseWarningMessage = "JVM output should not contain "
                        + "any warnings related to " + SEGMENTED_CODE_CACHE;
                String testCaseExitCodeMessage = "JVM should start without any "
                        + "issues with " + USE_SEGMENTED_CODE_CACHE;

                CommandLineOptionTest.verifySameJVMStartup(
                        /* expectedMessages */ null, UNEXPECTED_MESSAGES,
                        testCaseExitCodeMessage, testCaseWarningMessage,
                        ExitCode.OK, USE_SEGMENTED_CODE_CACHE);
                // ... and when we're trying to enable it w/o TieredCompilation
                testCaseExitCodeMessage = "Disabled tiered compilation should "
                        + "not cause startup failure w/ "
                        + USE_SEGMENTED_CODE_CACHE;

                CommandLineOptionTest.verifySameJVMStartup(
                        /* expectedMessages */ null, UNEXPECTED_MESSAGES,
                        testCaseExitCodeMessage, testCaseWarningMessage,
                        ExitCode.OK, USE_SEGMENTED_CODE_CACHE,
                        CommandLineOptionTest.prepareBooleanFlag(
                                TIERED_COMPILATION, false));
                // ... and even w/ Xint.
                testCaseExitCodeMessage = "It should be possible to use "
                        + USE_SEGMENTED_CODE_CACHE + " in interpreted mode "
                        + "without any errors.";

                CommandLineOptionTest.verifyJVMStartup(
                        /* expected messages */ null, UNEXPECTED_MESSAGES,
                        testCaseExitCodeMessage, testCaseWarningMessage,
                        ExitCode.OK, false, INT_MODE, USE_SEGMENTED_CODE_CACHE);
            }
        },
        OPTION_VALUES_GENERIC {
            @Override
            public void run() throws Throwable {
                // SCC is disabled w/o TieredCompilation by default
                String errorMessage = SEGMENTED_CODE_CACHE
                        + " should be disabled by default  when tiered "
                        + "compilation is disabled";

                CommandLineOptionTest.verifyOptionValueForSameVM(
                        SEGMENTED_CODE_CACHE, "false", errorMessage,
                        CommandLineOptionTest.prepareBooleanFlag(
                                TIERED_COMPILATION, false));
                // SCC is disabled by default when ReservedCodeCacheSize is too
                // small
                errorMessage = String.format("%s should be disabled bu default "
                        + "when %s value is too small.", SEGMENTED_CODE_CACHE,
                        BlobType.All.sizeOptionName);

                CommandLineOptionTest.verifyOptionValueForSameVM(
                        SEGMENTED_CODE_CACHE, "false", errorMessage,
                        CommandLineOptionTest.prepareNumericFlag(
                                BlobType.All.sizeOptionName,
                                BELOW_THRESHOLD_CC_SIZE));
                // SCC could be explicitly enabled w/ Xint
                errorMessage = String.format("It should be possible to "
                                + "explicitly enable %s in interpreted mode.",
                        SEGMENTED_CODE_CACHE);

                CommandLineOptionTest.verifyOptionValue(SEGMENTED_CODE_CACHE,
                        "true", errorMessage, false, INT_MODE,
                        USE_SEGMENTED_CODE_CACHE);
                // SCC could be explicitly enabled w/o TieredCompilation and w/
                // small ReservedCodeCacheSize value
                errorMessage = String.format("It should be possible to "
                                + "explicitly enable %s with small %s and "
                                + "disabled tiered comp.", SEGMENTED_CODE_CACHE,
                        BlobType.All.sizeOptionName);

                CommandLineOptionTest.verifyOptionValueForSameVM(
                        SEGMENTED_CODE_CACHE, "true", errorMessage,
                        CommandLineOptionTest.prepareBooleanFlag(
                                TIERED_COMPILATION, false),
                        CommandLineOptionTest.prepareNumericFlag(
                                BlobType.All.sizeOptionName,
                                BELOW_THRESHOLD_CC_SIZE),
                        USE_SEGMENTED_CODE_CACHE);
            }
        },
        OPTION_VALUES_SERVER_SPECIFIC {
            @Override
            public boolean isApplicable() {
                return Platform.isServer() && Platform.isTieredSupported();
            }

            @Override
            public void run() throws Throwable {
                // SCC is enabled by default when TieredCompilation is on and
                // ReservedCodeCacheSize is large enough
                String errorMessage = String.format("Large enough %s and "
                                + "enabled tiered compilation should enable %s "
                                + "by default.", BlobType.All.sizeOptionName,
                        SEGMENTED_CODE_CACHE);

                CommandLineOptionTest.verifyOptionValueForSameVM(
                        SEGMENTED_CODE_CACHE, "true", errorMessage,
                        CommandLineOptionTest.prepareNumericFlag(
                                BlobType.All.sizeOptionName,
                                THRESHOLD_CC_SIZE_VALUE),
                        CommandLineOptionTest.prepareBooleanFlag(
                                TIERED_COMPILATION, true));
            }
        };

        TestCase() {
        }

        public boolean isApplicable() {
            return true;
        }

        public abstract void run() throws Throwable;
    }

    public static void main(String args[]) throws Throwable {
        for (TestCase testCase : TestCase.values()) {
            if (testCase.isApplicable()) {
                System.out.println("Running test case: " + testCase.name());
                testCase.run();
            } else {
                System.out.println("Test case skipped: " + testCase.name());
            }
        }
    }
}
