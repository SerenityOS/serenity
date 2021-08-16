/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

package gc.g1.mixedgc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import gc.testlibrary.g1.MixedGCProvoker;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

/*
 * @test TestLogging
 * @summary Check that a mixed GC is reflected in the gc logs
 * @requires vm.gc.G1
 * @requires vm.opt.MaxGCPauseMillis == "null"
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 * @modules java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run driver gc.g1.mixedgc.TestLogging
 */

/**
 * Test spawns MixedGCProvoker in a separate VM and expects to find a message
 * telling that a mixed gc has happened
 */
public class TestLogging {
    private static final String[] COMMON_OPTIONS = new String[]{
            "-Xbootclasspath/a:.", "-XX:+UseG1GC",
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:+WhiteBoxAPI",
            "-Xms10M", "-Xmx10M", "-XX:NewSize=2M", "-XX:MaxNewSize=2M",
            "-XX:+AlwaysTenure", // surviving promote objects immediately
            "-XX:InitiatingHeapOccupancyPercent=100", // set initial CMC threshold and disable adaptive IHOP
            "-XX:-G1UseAdaptiveIHOP",                 // to avoid additional concurrent cycles caused by ergonomics
            "-XX:G1MixedGCCountTarget=4",
            "-XX:MaxGCPauseMillis=30000", // to have enough time
            "-XX:G1HeapRegionSize=1m", "-XX:G1HeapWastePercent=0",
            "-XX:G1MixedGCLiveThresholdPercent=100"};

    public static void main(String args[]) throws Exception {
        // Test turns logging on by giving -Xlog:gc flag
        test("-Xlog:gc,gc+heap=debug");
        // Test turns logging on by giving -Xlog:gc=debug flag
        test("-Xlog:gc=debug,gc+heap=debug");
    }

    private static void test(String vmFlag) throws Exception {
        System.out.println(String.format("%s: running with %s flag", TestLogging.class.getSimpleName(), vmFlag));
        OutputAnalyzer output = spawnMixedGCProvoker(vmFlag);
        System.out.println(output.getStdout());
        output.shouldHaveExitValue(0);
        output.shouldContain("Pause Young (Mixed)");
    }

    /**
     * Method spawns MixedGCProvoker with addition flags set
     *
     * @parameter extraFlags -flags to be added to the common options set
     */
    private static OutputAnalyzer spawnMixedGCProvoker(String... extraFlags)
            throws Exception {
        List<String> testOpts = new ArrayList<>();
        Collections.addAll(testOpts, COMMON_OPTIONS);
        Collections.addAll(testOpts, extraFlags);
        testOpts.add(RunMixedGC.class.getName());
        System.out.println(testOpts);
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(testOpts);
        return new OutputAnalyzer(pb.start());
    }
}

class RunMixedGC {
    public static void main(String[] args) {
        final int MB = 1024 * 1024;
        MixedGCProvoker.allocateAndProvokeMixedGC(MB);
    }
}

