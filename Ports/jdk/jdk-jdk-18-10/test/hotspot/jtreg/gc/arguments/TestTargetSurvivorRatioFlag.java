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

package gc.arguments;

/*
 * @test TestTargetSurvivorRatioFlag
 * @summary Verify that option TargetSurvivorRatio affects survivor space occupancy after minor GC.
 * @requires vm.opt.ExplicitGCInvokesConcurrent != true
 * @requires vm.opt.UseJVMCICompiler != true
 * @requires vm.gc != "Z" & vm.gc != "Shenandoah"
 * @library /test/lib
 * @library /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run driver gc.arguments.TestTargetSurvivorRatioFlag
 */

import java.lang.management.GarbageCollectorMXBean;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jdk.internal.misc.Unsafe;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.Utils;
import sun.hotspot.WhiteBox;
import static gc.testlibrary.Allocation.blackHole;

/* In order to test that TargetSurvivorRatio affects survivor space occupancy
 * we setup fixed MaxTenuringThreshold and then verifying that if size of allocated
 * objects is lower than (survivor_size * TargetSurvivorRatio / 100) then objects
 * will stay in survivor space until MaxTenuringThreshold minor GC cycles.
 * If more than (survivor_size * TargetSurvivorRatio / 100) objects were allocated,
 * then we verify that after MaxTenuringThreshold minor GC cycles survivor space
 * is almost empty.
 */
public class TestTargetSurvivorRatioFlag {

    public static final long M = 1024 * 1024;

    // VM option values
    public static final long MAX_NEW_SIZE = 40 * M;
    public static final int SURVIVOR_RATIO = 8;
    public static final int MAX_TENURING_THRESHOLD = 15;

    // Value used to estimate amount of memory that should be allocated
    // and placed in survivor space.
    public static final double DELTA = 0.25;

    // Max variance of observed ratio
    public static double VARIANCE = 1;

    // Messages used by debuggee
    public static final String UNSUPPORTED_GC = "Unsupported GC";
    public static final String START_TEST = "Start test";
    public static final String END_TEST = "End test";

    // Patterns used during log parsing
    public static final String TENURING_DISTRIBUTION = "Desired survivor size";
    public static final String AGE_TABLE_ENTRY = ".*-[\\s]+age[\\s]+([0-9]+):[\\s]+([0-9]+)[\\s]+bytes,[\\s]+([0-9]+)[\\s]+total";
    public static final String MAX_SURVIVOR_SIZE = "Max survivor size: ([0-9]+)";

    public static void main(String args[]) throws Exception {

        LinkedList<String> options = new LinkedList<>(Arrays.asList(Utils.getTestJavaOpts()));

        // Need to consider the effect of TargetPLABWastePct=1 for G1 GC
        if (options.contains("-XX:+UseG1GC")) {
            VARIANCE = 2;
        } else {
            VARIANCE = 1;
        }

        negativeTest(-1, options);
        negativeTest(101, options);

        positiveTest(20, options);
        positiveTest(30, options);
        positiveTest(55, options);
        positiveTest(70, options);
    }

    /**
     * Verify that VM will fail to start with specified TargetSurvivorRatio
     *
     * @param ratio value of TargetSurvivorRatio
     * @param options additional VM options
     */
    public static void negativeTest(int ratio, LinkedList<String> options) throws Exception {
        LinkedList<String> vmOptions = new LinkedList<>(options);
        vmOptions.add("-XX:TargetSurvivorRatio=" + ratio);
        vmOptions.add("-version");

        ProcessBuilder procBuilder = GCArguments.createJavaProcessBuilder(vmOptions);
        OutputAnalyzer analyzer = new OutputAnalyzer(procBuilder.start());

        analyzer.shouldHaveExitValue(1);
        analyzer.shouldContain("Error: Could not create the Java Virtual Machine.");
    }

    /**
     * Verify that actual survivor space usage ratio conforms specified TargetSurvivorRatio
     *
     * @param ratio value of TargetSurvivorRatio
     * @param options additional VM options
     */
    public static void positiveTest(int ratio, LinkedList<String> options) throws Exception {
        LinkedList<String> vmOptions = new LinkedList<>(options);
        Collections.addAll(vmOptions,
                "-Xbootclasspath/a:.",
                "--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED",
                "-XX:+UnlockDiagnosticVMOptions",
                "-XX:+WhiteBoxAPI",
                "-XX:+UseAdaptiveSizePolicy",
                "-Xlog:gc+age=trace",
                "-XX:MaxTenuringThreshold=" + MAX_TENURING_THRESHOLD,
                "-XX:NewSize=" + MAX_NEW_SIZE,
                "-XX:MaxNewSize=" + MAX_NEW_SIZE,
                "-XX:InitialHeapSize=" + 2 * MAX_NEW_SIZE,
                "-XX:MaxHeapSize=" + 2 * MAX_NEW_SIZE,
                "-XX:SurvivorRatio=" + SURVIVOR_RATIO,
                "-XX:TargetSurvivorRatio=" + ratio,
                // For reducing variance of survivor size.
                "-XX:TargetPLABWastePct=" + 1,
                TargetSurvivorRatioVerifier.class.getName(),
                Integer.toString(ratio)
        );

        ProcessBuilder procBuilder = GCArguments.createJavaProcessBuilder(vmOptions);
        OutputAnalyzer analyzer = new OutputAnalyzer(procBuilder.start());

        analyzer.shouldHaveExitValue(0);

        String output = analyzer.getOutput();

        // Test avoids verification for parallel GC
        if (!output.contains(UNSUPPORTED_GC)) {
            // Two tests should be done - when actual ratio is lower than TargetSurvivorRatio
            // and when it is higher. We chech that output contains results for exactly two tests.
            List<Double> ratios = parseTestOutput(output);

            if (ratios.size() != 2) {
                System.out.println(output);
                throw new RuntimeException("Expected number of ratios extraced for output is 2,"
                        + " but " + ratios.size() + " ratios were extracted");
            }

            // At the end of the first test survivor space usage ratio should lies between
            // TargetSurvivorRatio and TargetSurvivorRatio - 2*DELTA
            if (ratio < ratios.get(0) || ratio - ratios.get(0) > VARIANCE) {
                System.out.println(output);
                throw new RuntimeException("Survivor space usage ratio expected to be close to "
                        + ratio + ", but observed ratio is: " + ratios.get(0));
            }

            // After second test survivor space should be almost empty.
            if (ratios.get(1) > VARIANCE) {
                System.out.println(output);
                throw new RuntimeException("Survivor space expected to be empty due to "
                        + "TargetSurvivorRatio overlimit, however observed "
                        + "survivor space usage ratio is: " + ratios.get(1));
            }
        } else {
            System.out.println("Selected GC does not support TargetSurvivorRatio option.");
        }
    }

    /**
     * Parse output produced by TargetSurvivorRatioVerifier.
     *
     * @param output output obtained from TargetSurvivorRatioVerifier
     * @return list of parsed test results, where each result is an actual
     *         survivor ratio after MaxTenuringThreshold minor GC cycles.
     */
    public static List<Double> parseTestOutput(String output) {
        List<Double> ratios = new LinkedList<Double>();
        String lines[] = output.split("[\n\r]");
        boolean testStarted = false;
        long survivorSize = 0;
        long survivorOccupancy = 0;
        int gcCount = 0;
        Pattern ageTableEntry = Pattern.compile(AGE_TABLE_ENTRY);
        Pattern maxSurvivorSize = Pattern.compile(MAX_SURVIVOR_SIZE);
        for (String line : lines) {
            if (Pattern.matches(MAX_SURVIVOR_SIZE, line)) {
                // We found estimated survivor space size
                Matcher m = maxSurvivorSize.matcher(line);
                m.find();
                survivorSize = Long.valueOf(m.group(1));
            } else if (line.contains(START_TEST) && !testStarted) {
                // Start collecting test results
                testStarted = true;
                gcCount = 0;
            } else if (testStarted) {
                if (line.contains(TENURING_DISTRIBUTION)) {
                    // We found start of output emitted by -XX:+PrintTenuringDistribution
                    // If it is associated with "MaxTenuringThreshold" GC cycle, then it's
                    // time to report observed survivor usage ratio
                    gcCount++;
                    double survivorRatio = survivorOccupancy / (double) survivorSize;
                    if (gcCount == MAX_TENURING_THRESHOLD || gcCount == MAX_TENURING_THRESHOLD * 2) {
                        ratios.add(survivorRatio * 100.0);
                        testStarted = false;
                    }
                    survivorOccupancy = 0;
                } else if (Pattern.matches(AGE_TABLE_ENTRY, line)) {
                    // Obtain survivor space usage from "total" age table log entry
                    Matcher m = ageTableEntry.matcher(line);
                    m.find();
                    survivorOccupancy = Long.valueOf(m.group(3));
                } else if (line.contains(END_TEST)) {
                    // It is expected to find at least MaxTenuringThreshold GC events
                    // until test end
                    if (gcCount < MAX_TENURING_THRESHOLD) {
                        throw new RuntimeException("Observed " + gcCount + " GC events, "
                                + "while it is expected to see at least "
                                + MAX_TENURING_THRESHOLD);
                    }
                    testStarted = false;
                }
            }
        }
        return ratios;
    }

    public static class TargetSurvivorRatioVerifier {

        static final WhiteBox wb = WhiteBox.getWhiteBox();
        static final Unsafe unsafe = Unsafe.getUnsafe();

        // Desired size of memory allocated at once
        public static final int CHUNK_SIZE = 1024;
        // Length of byte[] array that will have occupy CHUNK_SIZE bytes in heap
        public static final int ARRAY_LENGTH = CHUNK_SIZE - Unsafe.ARRAY_BYTE_BASE_OFFSET;

        public static void main(String args[]) throws Exception {
            if (args.length != 1) {
                throw new IllegalArgumentException("Expected 1 arg: <ratio>");
            }
            if (GCTypes.YoungGCType.getYoungGCType() == GCTypes.YoungGCType.PSNew) {
                System.out.println(UNSUPPORTED_GC);
                return;
            }

            int ratio = Integer.valueOf(args[0]);
            long maxSurvivorSize = getMaxSurvivorSize();
            System.out.println("Max survivor size: " + maxSurvivorSize);

            allocateMemory(ratio - DELTA, maxSurvivorSize);
            allocateMemory(ratio + DELTA, maxSurvivorSize);
        }

        /**
         * Allocate (<b>ratio</b> * <b>maxSize</b> / 100) bytes of objects
         * and force at least "MaxTenuringThreshold" minor GCs.
         *
         * @param ratio ratio used to calculate how many objects should be allocated
         * @param maxSize estimated max survivor space size
         */
        public static void allocateMemory(double ratio, long maxSize) throws Exception {
            GarbageCollectorMXBean youngGCBean = GCTypes.YoungGCType.getYoungGCBean();
            long garbageSize = (long) (maxSize * (ratio / 100.0));
            int arrayLength = (int) (garbageSize / CHUNK_SIZE);
            AllocationHelper allocator = new AllocationHelper(1, arrayLength, ARRAY_LENGTH, null);

            System.out.println(START_TEST);
            System.gc();
            final long initialGcId = youngGCBean.getCollectionCount();
            // allocate memory
            allocator.allocateMemoryAndVerify();

            // force minor GC
            while (youngGCBean.getCollectionCount() <= initialGcId + MAX_TENURING_THRESHOLD * 2) {
                blackHole(new byte[ARRAY_LENGTH]);
            }

            allocator.release();
            System.out.println(END_TEST);
        }

        /**
         * Estimate max survivor space size.
         *
         * For non-G1 GC returns value reported by MemoryPoolMXBean
         * associated with survivor space.
         * For G1 GC return max number of survivor regions * region size.
         * Number if survivor regions estimated from MaxNewSize and SurvivorRatio.
         */
        public static long getMaxSurvivorSize() {
            if (GCTypes.YoungGCType.getYoungGCType() == GCTypes.YoungGCType.G1) {
                int youngLength = (int) Math.max(MAX_NEW_SIZE / wb.g1RegionSize(), 1);
                return (long) Math.ceil(youngLength / (double) SURVIVOR_RATIO) * wb.g1RegionSize();
            } else {
                return HeapRegionUsageTool.getSurvivorUsage().getMax();
            }
        }
    }
}
