/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package compiler.codecache.cli.common;

import jdk.test.lib.Platform;
import jdk.test.lib.cli.CommandLineOptionTest;
import sun.hotspot.code.BlobType;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

/**
 * Code cache related command line option test case consisting of description
 * of code heaps used during test case run and additional options that should
 * be passed to JVM and runner aimed to perform actual testing based on the
 * description.
 */
public class CodeCacheCLITestCase {
    private static final Function<CodeCacheOptions, Boolean> ONLY_SEGMENTED
            = options -> options.segmented;
    private static final Function<CodeCacheOptions, Boolean> SEGMENTED_SERVER
            = ONLY_SEGMENTED.andThen(isSegmented -> isSegmented
                    && Platform.isServer() && Platform.isTieredSupported());
    private static final String USE_INT_MODE = "-Xint";
    private static final String SEGMENTED_CODE_CACHE = "SegmentedCodeCache";
    private static final String TIERED_COMPILATION = "TieredCompilation";
    private static final String TIERED_STOP_AT = "TieredStopAtLevel";

    private final Description description;
    private final Runner runner;

    public CodeCacheCLITestCase(Description description, Runner runner) {
        this.description = description;
        this.runner = runner;
    }

    public final void run(CodeCacheOptions options) throws Throwable {
        if (description.isApplicable(options)) {
            runner.run(description, options);
        }
    }

    public enum CommonDescriptions {
        /**
         * Verifies that in interpreted mode PrintCodeCache output contains
         * only NonNMethod code heap.
         */
        INT_MODE(ONLY_SEGMENTED, EnumSet.of(BlobType.NonNMethod), USE_INT_MODE),
        /**
         * Verifies that with disabled SegmentedCodeCache PrintCodeCache output
         * contains only CodeCache's entry.
         */
        NON_SEGMENTED(options -> !options.segmented, EnumSet.of(BlobType.All),
                CommandLineOptionTest.prepareBooleanFlag(SEGMENTED_CODE_CACHE,
                        false)),
        /**
         * Verifies that with disabled tiered compilation and enabled segmented
         * code cache PrintCodeCache output does not contain information about
         * profiled-nmethods heap and non-segmented CodeCache.
         */
        NON_TIERED(ONLY_SEGMENTED,
                EnumSet.of(BlobType.NonNMethod, BlobType.MethodNonProfiled),
                CommandLineOptionTest.prepareBooleanFlag(TIERED_COMPILATION,
                        false)),
        /**
         * Verifies that with TieredStopAtLevel=0 PrintCodeCache output will
         * contain information about non-nmethods and non-profiled nmethods
         * heaps only.
         */
        TIERED_LEVEL_0(SEGMENTED_SERVER,
                EnumSet.of(BlobType.NonNMethod, BlobType.MethodNonProfiled),
                CommandLineOptionTest.prepareBooleanFlag(TIERED_COMPILATION,
                        true),
                CommandLineOptionTest.prepareNumericFlag(TIERED_STOP_AT, 0)),
        /**
         * Verifies that with TieredStopAtLevel=1 PrintCodeCache output will
         * contain information about non-nmethods and non-profiled nmethods
         * heaps only.
         */
        TIERED_LEVEL_1(SEGMENTED_SERVER,
                EnumSet.of(BlobType.NonNMethod, BlobType.MethodNonProfiled),
                CommandLineOptionTest.prepareBooleanFlag(TIERED_COMPILATION,
                        true),
                CommandLineOptionTest.prepareNumericFlag(TIERED_STOP_AT, 1)),
        /**
         * Verifies that with TieredStopAtLevel=4 PrintCodeCache output will
         * contain information about all three code heaps.
         */
        TIERED_LEVEL_4(SEGMENTED_SERVER,
                EnumSet.complementOf(EnumSet.of(BlobType.All)),
                CommandLineOptionTest.prepareBooleanFlag(TIERED_COMPILATION,
                        true),
                CommandLineOptionTest.prepareNumericFlag(TIERED_STOP_AT, 4));

        CommonDescriptions(Function<CodeCacheOptions, Boolean> predicate,
                EnumSet<BlobType> involvedCodeHeaps,
                String... additionalOptions) {
            this.description = new Description(predicate,
                    involvedCodeHeaps, additionalOptions);
        }


        public final Description description;
    }

    public static class Description {
        public final EnumSet<BlobType> involvedCodeHeaps;
        private final String[] testCaseSpecificOptions;
        private final Function<CodeCacheOptions, Boolean> predicate;

        public Description(Function<CodeCacheOptions, Boolean> predicate,
                EnumSet<BlobType> involvedCodeHeaps,
                String... testCaseSpecificOptions) {
            this.involvedCodeHeaps = involvedCodeHeaps;
            this.testCaseSpecificOptions = testCaseSpecificOptions;
            this.predicate = predicate;
        }

        public boolean isApplicable(CodeCacheOptions options) {
            return predicate.apply(options);
        }

        public CodeCacheOptions expectedValues(CodeCacheOptions options) {
            return options.mapOptions(involvedCodeHeaps);
        }

        public String[] getTestOptions(CodeCacheOptions codeCacheOptions,
                String... additionalOptions) {
            List<String> options = new LinkedList<>();
            Collections.addAll(options, testCaseSpecificOptions);
            Collections.addAll(options, additionalOptions);
            return codeCacheOptions.prepareOptions(
                    options.toArray(new String[options.size()]));
        }
    }

    public static interface Runner {
        public void run(Description testCaseDescription,
                CodeCacheOptions options) throws Throwable;
    }
}

