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
import jdk.test.lib.Utils;
import jdk.test.lib.cli.CommandLineOptionTest;
import sun.hotspot.code.BlobType;

import java.util.Random;

/**
 * Test case runner aimed to verify option's consistency.
 */
public class JVMStartupRunner implements CodeCacheCLITestCase.Runner {
    private static final String INCONSISTENT_CH_SIZES_ERROR
            = "Invalid code heap sizes.*";

    @Override
    public void run(CodeCacheCLITestCase.Description testCaseDescription,
            CodeCacheOptions options) throws Throwable {
        // Everything should be fine when
        // sum(all code heap sizes) == reserved CC size
        CommandLineOptionTest.verifySameJVMStartup(/* expected messages */ null,
                new String[]{ INCONSISTENT_CH_SIZES_ERROR },
                "JVM startup should not fail with consistent code heap sizes",
                "JVM output should not contain warning about inconsistent code "
                + "heap sizes", ExitCode.OK, options.prepareOptions());

        verifySingleInconsistentValue(options);
        verifyAllInconsistentValues(options);
    }

    /**
     * Verifies that if at least one of three options will have value, such
     * that sum of all three values will be inconsistent, then JVM startup will
     * fail.
     */
    private static void verifySingleInconsistentValue(CodeCacheOptions options)
            throws Throwable {
        verifyHeapSizesSum(options.reserved,
                scaleCodeHeapSize(options.profiled), options.nonProfiled,
                options.nonNmethods);
        verifyHeapSizesSum(options.reserved, options.profiled,
                scaleCodeHeapSize(options.nonProfiled), options.nonNmethods);
        verifyHeapSizesSum(options.reserved, options.profiled,
                options.nonProfiled, scaleCodeHeapSize(options.nonNmethods));
    }

    /**
     * Verifies that if all three options will have values such that their sum
     * is inconsistent with ReservedCodeCacheSize value, then JVM startup will
     * fail.
     */
    private static void verifyAllInconsistentValues(CodeCacheOptions options)
            throws Throwable {
        long profiled = options.profiled;
        long nonProfiled = options.nonProfiled;
        long nonNMethods = options.nonNmethods;

        while (options.reserved == profiled + nonProfiled + nonNMethods) {
            profiled = scaleCodeHeapSize(profiled);
            nonProfiled = scaleCodeHeapSize(nonProfiled);
            nonNMethods = scaleCodeHeapSize(nonNMethods);
        }

        verifyHeapSizesSum(options.reserved, profiled, nonProfiled,
                nonNMethods);
    }

    private static void verifyHeapSizesSum(long reserved, long profiled,
            long nonProfiled, long nonNmethods) throws Throwable {
        // JVM startup expected to fail when
        // sum(all code heap sizes) != reserved CC size
        CommandLineOptionTest.verifySameJVMStartup(
                new String[]{ INCONSISTENT_CH_SIZES_ERROR },
                /* unexpected messages */ null,
                "JVM startup should fail with inconsistent code heap size.",
                "JVM output should contain appropriate error message of code "
                        + "heap sizes are inconsistent",
                ExitCode.FAIL,
                CommandLineOptionTest.prepareBooleanFlag(
                        CodeCacheOptions.SEGMENTED_CODE_CACHE, true),
                CommandLineOptionTest.prepareNumericFlag(
                        BlobType.All.sizeOptionName, reserved),
                CommandLineOptionTest.prepareNumericFlag(
                        BlobType.MethodProfiled.sizeOptionName, profiled),
                CommandLineOptionTest.prepareNumericFlag(
                        BlobType.MethodNonProfiled.sizeOptionName, nonProfiled),
                CommandLineOptionTest.prepareNumericFlag(
                        BlobType.NonNMethod.sizeOptionName, nonNmethods));
    }

    /**
     * Returns {@code unscaledSize} value scaled by a random factor from
     * range (1, 2). If {@code unscaledSize} is not 0, then this
     * method will return value that won't be equal to {@code unscaledSize}.
     *
     * @param unscaledSize The value to be scaled.
     * @return {@code unscaledSize} value scaled by a factor from range (1, 2).
     */
    private static long scaleCodeHeapSize(long unscaledSize) {
        Random random = Utils.getRandomInstance();

        long scaledSize = unscaledSize;
        while (scaledSize == unscaledSize && unscaledSize != 0) {
            float scale = 1.0f + random.nextFloat();
            scaledSize = (long) Math.ceil(scale * unscaledSize);
        }
        return scaledSize;
    }
}
