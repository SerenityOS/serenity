/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

package gc.g1;

/*
 * Common helpers for TestRemsetLogging* tests
 */

import sun.hotspot.WhiteBox;

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import java.util.ArrayList;
import java.util.Arrays;

class VerifySummaryOutput {
    public static void main(String[] args) {
        int numGCs = Integer.parseInt(args[0]);

        // Perform the requested amount of GCs.
        WhiteBox wb = WhiteBox.getWhiteBox();
        for (int i = 0; i < numGCs - 1; i++) {
            wb.youngGC();
        }
        if (numGCs > 0) {
          wb.fullGC();
        }
    }
}

public class TestRemsetLoggingTools {

    public static String runTest(String[] additionalArgs, int numGCs) throws Exception {
        ArrayList<String> finalargs = new ArrayList<String>();
        String[] defaultArgs = new String[] {
            "-Xbootclasspath/a:.",
            "-XX:+UnlockDiagnosticVMOptions", "-XX:+WhiteBoxAPI",
            "-cp", System.getProperty("java.class.path"),
            "-XX:+UseG1GC",
            "-Xmn4m",
            "-Xint", // -Xint makes the test run faster
            "-Xms20m",
            "-Xmx20m",
            "-XX:ParallelGCThreads=1",
            "-XX:InitiatingHeapOccupancyPercent=100", // we don't want the additional GCs due to marking
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:G1HeapRegionSize=1M",
        };

        finalargs.addAll(Arrays.asList(defaultArgs));

        if (additionalArgs != null) {
            finalargs.addAll(Arrays.asList(additionalArgs));
        }

        finalargs.add(VerifySummaryOutput.class.getName());
        finalargs.add(String.valueOf(numGCs));

        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(finalargs);
        OutputAnalyzer output = new OutputAnalyzer(pb.start());

        output.shouldHaveExitValue(0);

        String result = output.getStdout();
        return result;
    }

    private static void checkCounts(int expected, int actual, String which) throws Exception {
        if (expected != actual) {
            throw new Exception("RSet summaries mention " + which + " regions an incorrect number of times. Expected " + expected + ", got " + actual);
        }
    }

    public static void expectPerRegionRSetSummaries(String result, int expectedCumulative, int expectedPeriodic) throws Exception {
        expectRSetSummaries(result, expectedCumulative, expectedPeriodic);
        int actualYoung = result.split("Young regions").length - 1;
        int actualHumongous = result.split("Humongous regions").length - 1;
        int actualFree = result.split("Free regions").length - 1;
        int actualOther = result.split("Old regions").length - 1;

        // the strings we check for above are printed four times per summary
        int expectedPerRegionTypeInfo = (expectedCumulative + expectedPeriodic) * 4;

        checkCounts(expectedPerRegionTypeInfo, actualYoung, "Young");
        checkCounts(expectedPerRegionTypeInfo, actualHumongous, "Humongous");
        checkCounts(expectedPerRegionTypeInfo, actualFree, "Free");
        checkCounts(expectedPerRegionTypeInfo, actualOther, "Old");
    }

    public static void expectRSetSummaries(String result, int expectedCumulative, int expectedPeriodic) throws Exception {
        int actualTotal = result.split("Concurrent refinement threads times").length - 1;
        int actualCumulative = result.split("Cumulative RS summary").length - 1;

        if (expectedCumulative != actualCumulative) {
            throw new Exception("Incorrect amount of RSet summaries at the end. Expected " + expectedCumulative + ", got " + actualCumulative);
        }

        if (expectedPeriodic != (actualTotal - actualCumulative)) {
            throw new Exception("Incorrect amount of per-period RSet summaries at the end. Expected " + expectedPeriodic + ", got " + (actualTotal - actualCumulative));
        }
    }
}
