/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test CheckSegmentedCodeCache
 * @bug 8015774
 * @summary Checks VM options related to the segmented code cache
 * @library /test/lib
 * @requires vm.flagless
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI
 *                   compiler.codecache.CheckSegmentedCodeCache
 */

package compiler.codecache;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.Platform;
import jdk.test.lib.process.ProcessTools;
import sun.hotspot.WhiteBox;

public class CheckSegmentedCodeCache {
    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();
    // Code heap names
    private static final String NON_METHOD = "CodeHeap 'non-nmethods'";
    private static final String PROFILED = "CodeHeap 'profiled nmethods'";
    private static final String NON_PROFILED = "CodeHeap 'non-profiled nmethods'";

    private static void verifySegmentedCodeCache(ProcessBuilder pb, boolean enabled) throws Exception {
        OutputAnalyzer out = new OutputAnalyzer(pb.start());
        out.shouldHaveExitValue(0);
        if (enabled) {
            try {
                // Non-nmethod code heap should be always available with the segmented code cache
                out.shouldContain(NON_METHOD);
            } catch (RuntimeException e) {
                // Check if TieredCompilation is disabled (in a client VM)
                if (Platform.isTieredSupported()) {
                    // Code cache is not segmented
                    throw new RuntimeException("No code cache segmentation.");
                }
            }
        } else {
            out.shouldNotContain(NON_METHOD);
        }
    }

    private static void verifyCodeHeapNotExists(ProcessBuilder pb, String... heapNames) throws Exception {
        OutputAnalyzer out = new OutputAnalyzer(pb.start());
        out.shouldHaveExitValue(0);
        for (String name : heapNames) {
            out.shouldNotContain(name);
        }
    }

    private static void failsWith(ProcessBuilder pb, String message) throws Exception {
        OutputAnalyzer out = new OutputAnalyzer(pb.start());
        out.shouldContain(message);
        out.shouldHaveExitValue(1);
    }

    /**
    * Check the result of segmented code cache related VM options.
    */
    public static void main(String[] args) throws Exception {
        ProcessBuilder pb;

        // Disabled with ReservedCodeCacheSize < 240MB
        pb = ProcessTools.createJavaProcessBuilder("-XX:ReservedCodeCacheSize=239m",
                                                   "-XX:+PrintCodeCache",
                                                   "-version");
        verifySegmentedCodeCache(pb, false);

        // Disabled without TieredCompilation
        pb = ProcessTools.createJavaProcessBuilder("-XX:-TieredCompilation",
                                                   "-XX:+PrintCodeCache",
                                                   "-version");
        verifySegmentedCodeCache(pb, false);

        // Enabled with TieredCompilation and ReservedCodeCacheSize >= 240MB
        pb = ProcessTools.createJavaProcessBuilder("-XX:+TieredCompilation",
                                                   "-XX:ReservedCodeCacheSize=240m",
                                                   "-XX:+PrintCodeCache",
                                                   "-version");
        verifySegmentedCodeCache(pb, true);
        pb = ProcessTools.createJavaProcessBuilder("-XX:+TieredCompilation",
                                                   "-XX:ReservedCodeCacheSize=400m",
                                                   "-XX:+PrintCodeCache",
                                                   "-version");
        verifySegmentedCodeCache(pb, true);

        // Always enabled if SegmentedCodeCache is set
        pb = ProcessTools.createJavaProcessBuilder("-XX:+SegmentedCodeCache",
                                                   "-XX:-TieredCompilation",
                                                   "-XX:ReservedCodeCacheSize=239m",
                                                   "-XX:+PrintCodeCache",
                                                   "-version");
        verifySegmentedCodeCache(pb, true);

        // The profiled and non-profiled code heaps should not be available in
        // interpreter-only mode
        pb = ProcessTools.createJavaProcessBuilder("-XX:+SegmentedCodeCache",
                                                   "-Xint",
                                                   "-XX:+PrintCodeCache",
                                                   "-version");
        verifyCodeHeapNotExists(pb, PROFILED, NON_PROFILED);

        // If we stop compilation at CompLevel_none or CompLevel_simple we
        // don't need a profiled code heap.
        pb = ProcessTools.createJavaProcessBuilder("-XX:+SegmentedCodeCache",
                                                   "-XX:TieredStopAtLevel=0",
                                                   "-XX:+PrintCodeCache",
                                                   "-version");
        verifyCodeHeapNotExists(pb, PROFILED);
        pb = ProcessTools.createJavaProcessBuilder("-XX:+SegmentedCodeCache",
                                                   "-XX:TieredStopAtLevel=1",
                                                   "-XX:+PrintCodeCache",
                                                   "-version");
        verifyCodeHeapNotExists(pb, PROFILED);

        // Fails with too small non-nmethod code heap size
        pb = ProcessTools.createJavaProcessBuilder("-XX:NonNMethodCodeHeapSize=100K",
                                                   "-version");
        failsWith(pb, "Invalid NonNMethodCodeHeapSize");

        // Fails if code heap sizes do not add up
        pb = ProcessTools.createJavaProcessBuilder("-XX:+SegmentedCodeCache",
                                                   "-XX:ReservedCodeCacheSize=10M",
                                                   "-XX:NonNMethodCodeHeapSize=5M",
                                                   "-XX:ProfiledCodeHeapSize=5M",
                                                   "-XX:NonProfiledCodeHeapSize=5M",
                                                   "-version");
        failsWith(pb, "Invalid code heap sizes");

        // Fails if not enough space for VM internal code
        long minUseSpace = WHITE_BOX.getUintxVMFlag("CodeCacheMinimumUseSpace");
        // minimum size: CodeCacheMinimumUseSpace DEBUG_ONLY(* 3)
        long minSize = (Platform.isDebugBuild() ? 3 : 1) * minUseSpace;
        pb = ProcessTools.createJavaProcessBuilder("-XX:+SegmentedCodeCache",
                                                   "-XX:ReservedCodeCacheSize=" + minSize,
                                                   "-XX:InitialCodeCacheSize=100K",
                                                   "-version");
        failsWith(pb, "Not enough space in non-nmethod code heap to run VM");
    }
}
