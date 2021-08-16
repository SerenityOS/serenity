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

package compiler.codecache.cli.codeheapsize;

import compiler.codecache.cli.common.CodeCacheCLITestCase;
import compiler.codecache.cli.common.CodeCacheOptions;
import jdk.test.lib.process.ExitCode;
import jdk.test.lib.Platform;
import jdk.test.lib.cli.CommandLineOptionTest;
import sun.hotspot.code.BlobType;

/**
 * Test case runner aimed to verify that NonNMethodCodeHeapSize smaller than
 * CodeCacheMinimumUseSpace cause JVM startup failure.
 */
public class CodeCacheFreeSpaceRunner implements CodeCacheCLITestCase.Runner {
    private static final String CC_MIN_USE_SPACE = "CodeCacheMinimumUseSpace";
    private static final String TOO_SMALL_NMETHOD_CH_ERROR
            = "Invalid NonNMethodCodeHeapSize.*";
    private static final long MULTIPLIER = Platform.isDebugBuild() ? 3L : 1L;
    @Override
    public void run(CodeCacheCLITestCase.Description testCaseDescription,
            CodeCacheOptions options) throws Throwable {
        long ccMinUseSpace = ((options.nonNmethods - 1) / MULTIPLIER + 1);

        String exitCodeErrorMessage = String.format("JVM startup should fail "
                        + "if %s's value lower then %s.",
                BlobType.NonNMethod.sizeOptionName, CC_MIN_USE_SPACE);
        String vmOutputErrorMessage = String.format("JVM's output should "
                        + "contain appropriate error message when %s lower "
                        + "then %s.", BlobType.NonNMethod.sizeOptionName,
                CC_MIN_USE_SPACE);

        CommandLineOptionTest.verifySameJVMStartup(
                new String[]{ TOO_SMALL_NMETHOD_CH_ERROR },
                /* unexpected messages */ null,
                exitCodeErrorMessage, vmOutputErrorMessage, ExitCode.FAIL,
                testCaseDescription.getTestOptions(options,
                        CommandLineOptionTest.prepareNumericFlag(
                                CC_MIN_USE_SPACE, ccMinUseSpace + 1)));
    }
}
