/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test TestPLABPromotion
 * @bug 8141278 8141141
 * @summary Test PLAB promotion
 * @requires vm.gc.G1
 * @requires !vm.flightRecorder
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 * @modules java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/timeout=240 gc.g1.plab.TestPLABPromotion
 */
package gc.g1.plab;

import java.util.List;
import java.util.Arrays;
import java.io.PrintStream;

import gc.g1.plab.lib.AppPLABPromotion;
import gc.g1.plab.lib.LogParser;
import gc.g1.plab.lib.PLABUtils;
import gc.g1.plab.lib.PlabInfo;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

/**
 * Test checks PLAB promotion of different size objects.
 */
public class TestPLABPromotion {

    // GC ID with survivor PLAB statistics
    private final static long GC_ID_SURVIVOR_STATS = 1l;
    // GC ID with old PLAB statistics
    private final static long GC_ID_OLD_STATS = 2l;

    private final static String PLAB_USED_FIELD_NAME = "used";
    private final static String PLAB_DIRECT_ALLOCATED_FIELD_NAME = "direct allocated";
    private final static List<String> FIELDS_TO_EXTRACT = Arrays.asList(PLAB_USED_FIELD_NAME, PLAB_DIRECT_ALLOCATED_FIELD_NAME);

    private static String output;

    // Allowable difference for memory consumption (percentage)
    private final static long MEM_DIFFERENCE_PCT = 5;

    private static final int PLAB_SIZE_SMALL = 1024;
    private static final int PLAB_SIZE_MEDIUM = 4096;
    private static final int PLAB_SIZE_HIGH = 65536;
    private static final int OBJECT_SIZE_SMALL = 10;
    private static final int OBJECT_SIZE_MEDIUM = 100;
    private static final int OBJECT_SIZE_HIGH = 3250;
    private static final int GC_NUM_SMALL = 1;
    private static final int GC_NUM_MEDIUM = 3;
    private static final int GC_NUM_HIGH = 7;
    private static final int WASTE_PCT_SMALL = 10;
    private static final int WASTE_PCT_MEDIUM = 20;
    private static final int WASTE_PCT_HIGH = 30;
    private static final int YOUNG_SIZE_LOW = 16;
    private static final int YOUNG_SIZE_HIGH = 64;
    private static final boolean PLAB_FIXED = true;
    private static final boolean PLAB_DYNAMIC = false;

    private final static TestCase[] TEST_CASES = {
        // Test cases for unreachable object, PLAB size is fixed
        new TestCase(WASTE_PCT_SMALL, PLAB_SIZE_SMALL, OBJECT_SIZE_MEDIUM, GC_NUM_SMALL, YOUNG_SIZE_LOW, PLAB_FIXED, false, false),
        new TestCase(WASTE_PCT_HIGH, PLAB_SIZE_MEDIUM, OBJECT_SIZE_SMALL, GC_NUM_HIGH, YOUNG_SIZE_HIGH, PLAB_FIXED, false, false),
        // Test cases for reachable objects, PLAB size is fixed
        new TestCase(WASTE_PCT_SMALL, PLAB_SIZE_SMALL, OBJECT_SIZE_SMALL, GC_NUM_HIGH, YOUNG_SIZE_HIGH, PLAB_FIXED, true, true),
        new TestCase(WASTE_PCT_SMALL, PLAB_SIZE_MEDIUM, OBJECT_SIZE_MEDIUM, GC_NUM_SMALL, YOUNG_SIZE_LOW, PLAB_FIXED, true, true),
        new TestCase(WASTE_PCT_SMALL, PLAB_SIZE_SMALL, OBJECT_SIZE_HIGH, GC_NUM_MEDIUM, YOUNG_SIZE_LOW, PLAB_FIXED, true, false),
        new TestCase(WASTE_PCT_MEDIUM, PLAB_SIZE_HIGH, OBJECT_SIZE_SMALL, GC_NUM_HIGH, YOUNG_SIZE_HIGH, PLAB_FIXED, true, true),
        new TestCase(WASTE_PCT_MEDIUM, PLAB_SIZE_SMALL, OBJECT_SIZE_MEDIUM, GC_NUM_SMALL, YOUNG_SIZE_LOW, PLAB_FIXED, true, true),
        new TestCase(WASTE_PCT_MEDIUM, PLAB_SIZE_MEDIUM, OBJECT_SIZE_HIGH, GC_NUM_MEDIUM, YOUNG_SIZE_LOW, PLAB_FIXED, true, true),
        new TestCase(WASTE_PCT_HIGH, PLAB_SIZE_SMALL, OBJECT_SIZE_SMALL, GC_NUM_HIGH, YOUNG_SIZE_HIGH, PLAB_FIXED, true, true),
        new TestCase(WASTE_PCT_HIGH, PLAB_SIZE_HIGH, OBJECT_SIZE_MEDIUM, GC_NUM_SMALL, YOUNG_SIZE_LOW, PLAB_FIXED, true, true),
        new TestCase(WASTE_PCT_HIGH, PLAB_SIZE_SMALL, OBJECT_SIZE_HIGH, GC_NUM_MEDIUM, YOUNG_SIZE_HIGH, PLAB_FIXED, true, false),
        // Test cases for unreachable object, PLAB size is not fixed
        new TestCase(WASTE_PCT_MEDIUM, PLAB_SIZE_MEDIUM, OBJECT_SIZE_SMALL, GC_NUM_HIGH, YOUNG_SIZE_LOW, PLAB_DYNAMIC, false, false),
        // Test cases for reachable objects, PLAB size is not fixed
        new TestCase(WASTE_PCT_SMALL, PLAB_SIZE_HIGH, OBJECT_SIZE_SMALL, GC_NUM_HIGH, YOUNG_SIZE_HIGH, PLAB_DYNAMIC, true, true),
        new TestCase(WASTE_PCT_MEDIUM, PLAB_SIZE_MEDIUM, OBJECT_SIZE_SMALL, GC_NUM_SMALL, YOUNG_SIZE_LOW, PLAB_DYNAMIC, true, true),
        new TestCase(WASTE_PCT_SMALL, PLAB_SIZE_MEDIUM, OBJECT_SIZE_HIGH, GC_NUM_HIGH, YOUNG_SIZE_HIGH, PLAB_DYNAMIC, true, false),
        new TestCase(WASTE_PCT_MEDIUM, PLAB_SIZE_SMALL, OBJECT_SIZE_MEDIUM, GC_NUM_MEDIUM, YOUNG_SIZE_LOW, PLAB_DYNAMIC, true, true),
        new TestCase(WASTE_PCT_HIGH, PLAB_SIZE_HIGH, OBJECT_SIZE_MEDIUM, GC_NUM_SMALL, YOUNG_SIZE_HIGH, PLAB_DYNAMIC, true, true),
        new TestCase(WASTE_PCT_HIGH, PLAB_SIZE_HIGH, OBJECT_SIZE_SMALL, GC_NUM_HIGH, YOUNG_SIZE_LOW, PLAB_DYNAMIC, true, true)
    };

    public static void main(String[] args) throws Throwable {

        for (TestCase testCase : TEST_CASES) {
            // What we going to check.
            testCase.print(System.out);
            List<String> options = PLABUtils.prepareOptions(testCase.toOptions());
            options.add(AppPLABPromotion.class.getName());
            OutputAnalyzer out = ProcessTools.executeTestJvm(options);
            PLABUtils.commonCheck(out);
            output = out.getOutput();
            checkResults(testCase);
        }
    }

    private static void checkResults(TestCase testCase) {
        long plabAllocatedSurvivor;
        long directAllocatedSurvivor;
        long plabAllocatedOld;
        long directAllocatedOld;
        long memAllocated = testCase.getMemToFill();
        LogParser logParser = new LogParser(output);

        PlabInfo survivorPlabInfo = logParser.getSpecifiedStats(GC_ID_SURVIVOR_STATS, LogParser.ReportType.SURVIVOR_STATS, FIELDS_TO_EXTRACT);
        PlabInfo oldPlabInfo = logParser.getSpecifiedStats(GC_ID_OLD_STATS, LogParser.ReportType.OLD_STATS, FIELDS_TO_EXTRACT);

        checkFields(survivorPlabInfo);
        checkFields(oldPlabInfo);

        plabAllocatedSurvivor = survivorPlabInfo.get(PLAB_USED_FIELD_NAME);
        directAllocatedSurvivor = survivorPlabInfo.get(PLAB_DIRECT_ALLOCATED_FIELD_NAME);
        plabAllocatedOld = oldPlabInfo.get(PLAB_USED_FIELD_NAME);
        directAllocatedOld = oldPlabInfo.get(PLAB_DIRECT_ALLOCATED_FIELD_NAME);

        System.out.printf("Survivor PLAB allocated:%17d Direct allocated: %17d Mem consumed:%17d%n", plabAllocatedSurvivor, directAllocatedSurvivor, memAllocated);
        System.out.printf("Old      PLAB allocated:%17d Direct allocated: %17d Mem consumed:%17d%n", plabAllocatedOld, directAllocatedOld, memAllocated);

        // Unreachable objects case
        if (testCase.isDeadObjectCase()) {
            checkDeadObjectsPromotion(plabAllocatedSurvivor, directAllocatedSurvivor, memAllocated);
            checkDeadObjectsPromotion(plabAllocatedOld, directAllocatedOld, memAllocated);

        } else {
            // Live objects case
            if (testCase.isPromotedByPLAB()) {
                checkLiveObjectsPromotion(plabAllocatedSurvivor, memAllocated, "Expect that Survivor PLAB allocation are similar to all mem consumed");
                checkLiveObjectsPromotion(plabAllocatedOld, memAllocated, "Expect that Old PLAB allocation are similar to all mem consumed");
            } else {
                // All big objects should be directly allocated
                checkLiveObjectsPromotion(directAllocatedSurvivor, memAllocated, "Expect that Survivor direct allocation are similar to all mem consumed");
                checkLiveObjectsPromotion(directAllocatedOld, memAllocated, "Expect that Old direct allocation are similar to all mem consumed");
            }

            checkTotalPromotion(plabAllocatedSurvivor, directAllocatedSurvivor, memAllocated, "Expect that Survivor gen total allocation are similar to all mem consumed");
            checkTotalPromotion(plabAllocatedOld, directAllocatedOld, memAllocated, "Expect that Old gen total allocation are similar to all mem consumed");
        }
        System.out.println("Test passed!");
    }

    private static void checkTotalPromotion(long plabAllocatedSurvivor, long directAllocatedSurvivor, long memAllocated, String exceptionMessage) {
        // All promoted objects size should be similar to all consumed memory
        if (!checkDifferenceRatio(plabAllocatedSurvivor + directAllocatedSurvivor, memAllocated)) {
            System.out.println(output);
            throw new RuntimeException(exceptionMessage);
        }
    }

    /**
     * Checks that live objects were promoted as expected.
     * @param plabAllocated
     * @param totalMemAllocated
     * @param exceptionMessage
     */
    private static void checkLiveObjectsPromotion(long plabAllocated, long totalMemAllocated, String exceptionMessage) {
        // All live small objects should be promoted using PLAB
        if (!checkDifferenceRatio(plabAllocated, totalMemAllocated)) {
            System.out.println(output);
            throw new RuntimeException(exceptionMessage);
        }
    }

    /**
     * Checks that dead objects are not promoted.
     * @param plabPromoted promoted by PLAB
     * @param directlyPromoted
     * @param memoryAllocated total memory allocated
     */
    private static void checkDeadObjectsPromotion(long plabPromoted, long directlyPromoted, long memoryAllocated) {
        // No dead objects should be promoted
        if (!(checkRatio(plabPromoted, memoryAllocated) && checkRatio(directlyPromoted, memoryAllocated))) {
            System.out.println(output);
            throw new RuntimeException("Unreachable objects should not be allocated using PLAB or directly allocated to Survivor/Old");
        }
    }

    /**
     * Checks that PLAB statistics contains expected fields.
     * @param info
     */
    private static void checkFields(PlabInfo info) {
        if (!info.checkFields(FIELDS_TO_EXTRACT)) {
            System.out.println(output);
            throw new RuntimeException("PLAB log does not contain expected fields");
        }
    }

    /**
     * Returns true if checkedValue is less than MEM_DIFFERENCE_PCT percent of controlValue.
     *
     * @param checkedValue - checked value
     * @param controlValue - referent value
     * @return true if checkedValue is less than MEM_DIFFERENCE_PCT percent of controlValue
     */
    private static boolean checkRatio(long checkedValue, long controlValue) {
        return Math.abs(checkedValue * 100.0 / controlValue) < MEM_DIFFERENCE_PCT;
    }

    /**
     * Returns true if difference of checkedValue and controlValue is less than
     * MEM_DIFFERENCE_PCT percent of controlValue.
     *
     * @param checkedValue - checked value
     * @param controlValue - referent value
     * @return true if difference of checkedValue and controlValue is less than
     * MEM_DIFFERENCE_PCT percent of controlValue
     */
    private static boolean checkDifferenceRatio(long checkedValue, long controlValue) {
        return Math.abs((checkedValue - controlValue) * 100.0 / controlValue) < MEM_DIFFERENCE_PCT;
    }

    /**
     * Description of one test case.
     */
    private static class TestCase {

        private final int wastePct;
        private final int plabSize;
        private final int chunkSize;
        private final int parGCThreads;
        private final int edenSize;
        private final boolean plabIsFixed;
        private final boolean objectsAreReachable;
        private final boolean promotedByPLAB;

        /**
         * @param wastePct
         * ParallelGCBufferWastePct
         * @param plabSize
         * -XX:OldPLABSize and -XX:YoungPLABSize
         * @param chunkSize
         * requested object size for memory consumption
         * @param parGCThreads
         * -XX:ParallelGCThreads
         * @param edenSize
         * NewSize and MaxNewSize
         * @param plabIsFixed
         * Use dynamic PLAB or fixed size PLAB
         * @param objectsAreReachable
         * true - allocate live objects
         * false - allocate unreachable objects
         * @param promotedByPLAB
         * true - we expect to see PLAB allocation during promotion
         * false - objects will be directly allocated during promotion
         */
        public TestCase(int wastePct,
                int plabSize,
                int chunkSize,
                int parGCThreads,
                int edenSize,
                boolean plabIsFixed,
                boolean objectsAreReachable,
                boolean promotedByPLAB
        ) {
            if (wastePct == 0 || plabSize == 0 || chunkSize == 0 || parGCThreads == 0 || edenSize == 0) {
                throw new IllegalArgumentException("Parameters should not be 0");
            }
            this.wastePct = wastePct;
            this.plabSize = plabSize;
            this.chunkSize = chunkSize;
            this.parGCThreads = parGCThreads;
            this.edenSize = edenSize;
            this.plabIsFixed = plabIsFixed;
            this.objectsAreReachable = objectsAreReachable;
            this.promotedByPLAB = promotedByPLAB;
        }

        /**
         * Convert current TestCase to List of options.
         * Assume test will fill half of existed eden.
         *
         * @return
         * List of options
         */
        public List<String> toOptions() {
            return Arrays.asList("-XX:ParallelGCThreads=" + parGCThreads,
                    "-XX:ParallelGCBufferWastePct=" + wastePct,
                    "-XX:ParallelGCThreads=1", // Avoid dynamic sizing of threads.
                    "-XX:OldPLABSize=" + plabSize,
                    "-XX:YoungPLABSize=" + plabSize,
                    "-XX:" + (plabIsFixed ? "-" : "+") + "ResizePLAB",
                    "-Dchunk.size=" + chunkSize,
                    "-Dreachable=" + objectsAreReachable,
                    "-XX:NewSize=" + edenSize + "m",
                    "-XX:MaxNewSize=" + edenSize + "m",
                    "-Dmem.to.fill=" + getMemToFill()
            );
        }

        /**
         * Print details about test case.
         */
        public void print(PrintStream out) {
            boolean expectPLABAllocation = promotedByPLAB && objectsAreReachable;
            boolean expectDirectAllocation = (!promotedByPLAB) && objectsAreReachable;

            out.println("Test case details:");
            out.println("  Young gen size : " + edenSize + "M");
            out.println("  Predefined PLAB size : " + plabSize);
            out.println("  Parallel GC buffer waste pct : " + wastePct);
            out.println("  Chunk size : " + chunkSize);
            out.println("  Parallel GC threads : " + parGCThreads);
            out.println("  Objects are created : " + (objectsAreReachable ? "reachable" : "unreachable"));
            out.println("  PLAB size is fixed: " + (plabIsFixed ? "yes" : "no"));
            out.println("Test expectations:");
            out.println("  PLAB allocation : " + (expectPLABAllocation ? "expected" : "unexpected"));
            out.println("  Direct allocation : " + (expectDirectAllocation ? "expected" : "unexpected"));
        }

        /**
         * @return
         * true if we expect PLAB allocation
         * false if no
         */
        public boolean isPromotedByPLAB() {
            return promotedByPLAB;
        }

        /**
         * @return
         * true if it is test case for unreachable objects
         * false for live objects
         */
        public boolean isDeadObjectCase() {
            return !objectsAreReachable;
        }

        /**
         * Returns amount of memory to fill
         *
         * @return amount of memory
         */
        public long getMemToFill() {
            return (long) (edenSize) * 1024l * 1024l / 2;
        }
    }
}
