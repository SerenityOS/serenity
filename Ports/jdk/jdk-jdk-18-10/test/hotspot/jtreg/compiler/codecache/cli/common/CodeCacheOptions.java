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

import jdk.test.lib.cli.CommandLineOptionTest;
import sun.hotspot.code.BlobType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

public class CodeCacheOptions {
    public static final String SEGMENTED_CODE_CACHE = "SegmentedCodeCache";

    private static final EnumSet<BlobType> NON_SEGMENTED_HEAPS
            = EnumSet.of(BlobType.All);
    private static final EnumSet<BlobType> ALL_SEGMENTED_HEAPS
            = EnumSet.complementOf(NON_SEGMENTED_HEAPS);
    private static final EnumSet<BlobType> SEGMENTED_HEAPS_WO_PROFILED
            = EnumSet.of(BlobType.NonNMethod, BlobType.MethodNonProfiled);
    private static final EnumSet<BlobType> ONLY_NON_METHODS_HEAP
            = EnumSet.of(BlobType.NonNMethod);

    public final long reserved;
    public final long nonNmethods;
    public final long nonProfiled;
    public final long profiled;
    public final boolean segmented;

    public static long mB(long val) {
        return CodeCacheOptions.kB(val) * 1024L;
    }

    public static long kB(long val) {
        return val * 1024L;
    }

    public CodeCacheOptions(long reserved) {
        this.reserved = reserved;
        this.nonNmethods = 0;
        this.nonProfiled = 0;
        this.profiled = 0;
        this.segmented = false;
    }

    public CodeCacheOptions(long reserved, long nonNmethods, long nonProfiled,
            long profiled) {
        this.reserved = reserved;
        this.nonNmethods = nonNmethods;
        this.nonProfiled = nonProfiled;
        this.profiled = profiled;
        this.segmented = true;
    }

    public long sizeForHeap(BlobType heap) {
        switch (heap) {
            case All:
                return this.reserved;
            case NonNMethod:
                return this.nonNmethods;
            case MethodNonProfiled:
                return this.nonProfiled;
            case MethodProfiled:
                return this.profiled;
            default:
                throw new Error("Unknown heap: " + heap.name());
        }
    }

    public String[] prepareOptions(String... additionalOptions) {
        List<String> options = new ArrayList<>();
        Collections.addAll(options, additionalOptions);
        Collections.addAll(options,
                CommandLineOptionTest.prepareBooleanFlag(
                        SEGMENTED_CODE_CACHE, segmented),
                CommandLineOptionTest.prepareNumericFlag(
                        BlobType.All.sizeOptionName, reserved));

        if (segmented) {
            Collections.addAll(options,
                    CommandLineOptionTest.prepareNumericFlag(
                            BlobType.NonNMethod.sizeOptionName, nonNmethods),
                    CommandLineOptionTest.prepareNumericFlag(
                            BlobType.MethodNonProfiled.sizeOptionName,
                            nonProfiled),
                    CommandLineOptionTest.prepareNumericFlag(
                            BlobType.MethodProfiled.sizeOptionName, profiled));
        }
        return options.toArray(new String[options.size()]);
    }

    public CodeCacheOptions mapOptions(EnumSet<BlobType> involvedCodeHeaps) {
        if (involvedCodeHeaps.isEmpty()
                || involvedCodeHeaps.equals(NON_SEGMENTED_HEAPS)
                || involvedCodeHeaps.equals(ALL_SEGMENTED_HEAPS)) {
            return this;
        } else if (involvedCodeHeaps.equals(SEGMENTED_HEAPS_WO_PROFILED)) {
            return new CodeCacheOptions(reserved, nonNmethods,
                    profiled + nonProfiled, 0L);
        } else if (involvedCodeHeaps.equals(ONLY_NON_METHODS_HEAP)) {
            return new CodeCacheOptions(reserved, nonNmethods + profiled
                    + nonProfiled, 0L, 0L);
        } else {
            throw new Error("Test bug: unexpected set of code heaps involved "
                    + "into test.");
        }
    }
}
