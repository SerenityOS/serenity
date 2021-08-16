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
 * @test TestPLABResize
 * @bug 8141278 8141141
 * @summary Test for PLAB resizing
 * @requires vm.gc.G1
 * @requires !vm.flightRecorder
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 * @modules java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main gc.g1.plab.TestPLABResize
 */
package gc.g1.plab;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.io.PrintStream;

import gc.g1.plab.lib.LogParser;
import gc.g1.plab.lib.PLABUtils;
import gc.g1.plab.lib.AppPLABResize;
import gc.g1.plab.lib.PlabReport;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

/**
 * Test for PLAB resizing.
 */
public class TestPLABResize {

    private static final int OBJECT_SIZE_SMALL = 10;
    private static final int OBJECT_SIZE_MEDIUM = 70;
    private static final int OBJECT_SIZE_HIGH = 150;
    private static final int GC_NUM_SMALL = 1;
    private static final int GC_NUM_MEDIUM = 3;
    private static final int GC_NUM_HIGH = 7;
    private static final int WASTE_PCT_SMALL = 10;
    private static final int WASTE_PCT_MEDIUM = 20;
    private static final int WASTE_PCT_HIGH = 30;

    private static final int ITERATIONS_SMALL = 3;
    private static final int ITERATIONS_MEDIUM = 5;
    private static final int ITERATIONS_HIGH = 8;

    private static final String PLAB_SIZE_FIELD_NAME = "actual";

    private final static TestCase[] TEST_CASES = {
        new TestCase(WASTE_PCT_SMALL, OBJECT_SIZE_SMALL, GC_NUM_SMALL, ITERATIONS_MEDIUM),
        new TestCase(WASTE_PCT_SMALL, OBJECT_SIZE_MEDIUM, GC_NUM_HIGH, ITERATIONS_SMALL),
        new TestCase(WASTE_PCT_SMALL, OBJECT_SIZE_HIGH, GC_NUM_MEDIUM, ITERATIONS_HIGH),
        new TestCase(WASTE_PCT_MEDIUM, OBJECT_SIZE_SMALL, GC_NUM_HIGH, ITERATIONS_MEDIUM),
        new TestCase(WASTE_PCT_MEDIUM, OBJECT_SIZE_MEDIUM, GC_NUM_SMALL, ITERATIONS_SMALL),
        new TestCase(WASTE_PCT_MEDIUM, OBJECT_SIZE_HIGH, GC_NUM_MEDIUM, ITERATIONS_HIGH),
        new TestCase(WASTE_PCT_HIGH, OBJECT_SIZE_SMALL, GC_NUM_HIGH, ITERATIONS_MEDIUM),
        new TestCase(WASTE_PCT_HIGH, OBJECT_SIZE_MEDIUM, GC_NUM_SMALL, ITERATIONS_SMALL),
        new TestCase(WASTE_PCT_HIGH, OBJECT_SIZE_HIGH, GC_NUM_MEDIUM, ITERATIONS_HIGH)
    };

    public static void main(String[] args) throws Throwable {
        for (TestCase testCase : TEST_CASES) {
            testCase.print(System.out);
            List<String> options = PLABUtils.prepareOptions(testCase.toOptions());
            options.add(AppPLABResize.class.getName());
            OutputAnalyzer out = ProcessTools.executeTestJvm(options);
            PLABUtils.commonCheck(out);
            checkResults(out.getOutput(), testCase);
        }
    }

    /**
     * Checks testing results.
     * Expected results - desired PLAB size is decreased and increased during promotion to Survivor.
     *
     * @param output - VM output
     * @param testCase
     */
    private static void checkResults(String output, TestCase testCase) {
        final LogParser log = new LogParser(output);
        final PlabReport report = log.getEntries();

        final List<Long> plabSizes = report.entryStream()
                .map(item -> item.getValue()
                        .get(LogParser.ReportType.SURVIVOR_STATS)
                        .get(PLAB_SIZE_FIELD_NAME)
                )
                .collect(Collectors.toList());

        // Check that desired plab size was changed during iterations.
        // The test case does 3 rounds of allocations.  The second round of N allocations and GC's
        // has a decreasing size of allocations so that iterations N to 2*N -1 will be of decreasing size.
        // The third round with iterations 2*N to 3*N -1 has increasing sizes of allocation.
        if ( plabSizes.size() != testCase.iterations * 3 ) {
            System.out.println(output);
            throw new RuntimeException ("Expects for " + testCase.iterations * 3 + " PLAB entries in log, found " + plabSizes.size());
        }

        long startDesiredPLABSize = plabSizes.get(testCase.getIterations());
        long endDesiredPLABSize = plabSizes.get(testCase.getIterations() * 2 - 1);

        if (startDesiredPLABSize < endDesiredPLABSize) {
            System.out.println(output);
            throw new RuntimeException("Test failed! Expect that initial PLAB size should be greater than checked. Initial size: " + startDesiredPLABSize + " Checked size:" + endDesiredPLABSize);
        }

        startDesiredPLABSize = plabSizes.get(testCase.getIterations() * 2);
        endDesiredPLABSize = plabSizes.get(testCase.getIterations() * 3 - 1);

        if (startDesiredPLABSize > endDesiredPLABSize) {
            System.out.println(output);
            throw new RuntimeException("Test failed! Expect that initial PLAB size should be less than checked. Initial size: " + startDesiredPLABSize + " Checked size:" + endDesiredPLABSize);
        }

        System.out.println("Test passed!");
    }

    /**
     * Description of one test case.
     */
    private static class TestCase {

        private final int wastePct;
        private final int chunkSize;
        private final int parGCThreads;
        private final int iterations;

        /**
         * @param wastePct
         * ParallelGCBufferWastePct
         * @param chunkSize
         * requested object size for memory consumption
         * @param parGCThreads
         * -XX:ParallelGCThreads
         * @param iterations
         *
         */
        public TestCase(int wastePct,
                int chunkSize,
                int parGCThreads,
                int iterations
        ) {
            if (wastePct == 0 || chunkSize == 0 || parGCThreads == 0 || iterations == 0) {
                throw new IllegalArgumentException("Parameters should not be 0");
            }
            this.wastePct = wastePct;

            this.chunkSize = chunkSize;
            this.parGCThreads = parGCThreads;
            this.iterations = iterations;
        }

        /**
         * Convert current TestCase to List of options.
         *
         * @return
         * List of options
         */
        public List<String> toOptions() {
            return Arrays.asList("-XX:ParallelGCThreads=" + parGCThreads,
                    "-XX:ParallelGCBufferWastePct=" + wastePct,
                    "-XX:+ResizePLAB",
                    "-Dchunk.size=" + chunkSize,
                    "-Diterations=" + iterations,
                    "-XX:NewSize=16m",
                    "-XX:MaxNewSize=16m"
            );
        }

        /**
         * Print details about test case.
         */
        public void print(PrintStream out) {
            out.println("Test case details:");
            out.println("  Parallel GC buffer waste pct : " + wastePct);
            out.println("  Chunk size : " + chunkSize);
            out.println("  Parallel GC threads : " + parGCThreads);
            out.println("  Iterations: " + iterations);
        }

        /**
         * @return iterations
         */
        public int getIterations() {
            return iterations;
        }
    }
}
