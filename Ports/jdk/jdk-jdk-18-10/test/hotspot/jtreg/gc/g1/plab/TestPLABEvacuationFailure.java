/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @test TestPLABEvacuationFailure
 * @bug 8148376
 * @summary Checks PLAB statistics on evacuation failure
 * @requires vm.gc.G1
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 * @modules java.management
 * @run main gc.g1.plab.TestPLABEvacuationFailure
 */
package gc.g1.plab;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.Utils;

import gc.g1.plab.lib.LogParser;
import gc.g1.plab.lib.AppPLABEvacuationFailure;
import gc.g1.plab.lib.PlabInfo;

/**
 * The test runs the AppPLABEvacuationFailure application to provoke a number of
 * Evacuation Failures, parses GC log and analyzes PLAB statistics. The test checks
 * that both fields 'failure_waste' and 'failure_used' for Evacuation Failure statistic
 * are non zero, and zero for other statistics.
 */
public class TestPLABEvacuationFailure {

    /* PLAB statistics fields which are checked.
     * Test expects to find 0 in this fields in survivor statistics.
     * Expects to find 0 in old statistics for GC when evacuation failure
     * did not happen. And expects to find not 0 in old statistics in case when
     * GC causes to evacuation failure.
     */
    private static final List<String> FAILURE_STAT_FIELDS = new ArrayList<>(Arrays.asList(
            "failure used",
            "failure wasted"));

    private static final String[] COMMON_OPTIONS = {
        "-Xlog:gc,gc+plab=debug",
        "-XX:+UseG1GC",
        "-XX:InitiatingHeapOccupancyPercent=100",
        "-XX:-G1UseAdaptiveIHOP",
        "-XX:G1HeapRegionSize=1m"};

    private static final Pattern GC_ID_PATTERN = Pattern.compile("GC\\((\\d+)\\)");
    private static List<Long> evacuationFailureIDs;
    private static LogParser logParser;
    private static String appPlabEvacFailureOutput;

    public static void main(String[] args) throws Throwable {
        // ParallelGCBufferWastePct, PLAB Size, ParallelGCBufferWastePct, MaxHeapSize, is plab fixed.
        runTest(10, 1024, 3, 16, true);
        runTest(15, 2048, 4, 256, true);
        runTest(20, 65536, 7, 128, false);
        runTest(25, 1024, 3, 16, true);
        runTest(30, 16384, 7, 256, false);
        runTest(10, 65536, 4, 32, false);
    }

    private static void runTest(int wastePct, int plabSize, int parGCThreads, int heapSize, boolean plabIsFixed) throws Throwable {
        System.out.println("Test case details:");
        System.out.println("  Heap size : " + heapSize + "M");
        System.out.println("  Initial PLAB size : " + plabSize);
        System.out.println("  Parallel GC buffer waste pct : " + wastePct);
        System.out.println("  Parallel GC threads : " + parGCThreads);
        System.out.println("  PLAB size is fixed: " + (plabIsFixed ? "yes" : "no"));
        // Set up test GC and PLAB options
        List<String> testOptions = new ArrayList<>();
        Collections.addAll(testOptions, COMMON_OPTIONS);
        Collections.addAll(testOptions, Utils.getTestJavaOpts());
        Collections.addAll(testOptions,
                "-XX:ParallelGCThreads=" + parGCThreads,
                "-XX:ParallelGCBufferWastePct=" + wastePct,
                "-XX:OldPLABSize=" + plabSize,
                "-XX:YoungPLABSize=" + plabSize,
                "-XX:" + (plabIsFixed ? "-" : "+") + "ResizePLAB",
                "-XX:MaxHeapSize=" + heapSize + "m");
        testOptions.add(AppPLABEvacuationFailure.class.getName());
        OutputAnalyzer out = ProcessTools.executeTestJvm(testOptions);

        appPlabEvacFailureOutput = out.getOutput();
        if (out.getExitValue() != 0) {
            System.out.println(appPlabEvacFailureOutput);
            throw new RuntimeException("Expect exit code 0.");
        }

        // Get list of GC ID on evacuation failure
        evacuationFailureIDs = getGcIdPlabEvacFailures(out);
        logParser = new LogParser(appPlabEvacFailureOutput);
        checkResults();
    }

    private static void checkResults() {

        if (evacuationFailureIDs.isEmpty()) {
            System.out.println(appPlabEvacFailureOutput);
            throw new RuntimeException("AppPLABEvacuationFailure did not reach Evacuation Failure.");
        }

        Map<Long, PlabInfo> valuesToCheck = getNonEvacFailureSurvivorStats();
        checkValuesIsZero(valuesToCheck, "Expect that SURVIVOR PLAB failure statistics should be 0 when no evacuation failure");

        valuesToCheck = getNonEvacFailureOldStats();
        checkValuesIsZero(valuesToCheck, "Expect that OLD PLAB failure statistics should be 0 when no evacuation failure");

        valuesToCheck = getEvacFailureSurvivorStats();
        checkValuesIsZero(valuesToCheck, "Expect that failure statistics should be 0 in SURVIVOR PLAB statistics at evacuation failure");

        valuesToCheck = getEvacFailureOldStats();
        checkValuesIsNotZero(valuesToCheck, "Expect that failure statistics should not be 0 in OLD PLAB statistics at evacuation failure");
    }

    /**
     * Checks logItems for non-zero values. Throws RuntimeException if found.
     *
     * @param logItems
     * @param errorMessage
     */
    private static void checkValuesIsZero(Map<Long, PlabInfo> logItems, String errorMessage) {
        checkValues(logItems, errorMessage, true);
    }

    /**
     * Checks logItems for zero values. Throws RuntimeException if found.
     *
     * @param logItems
     * @param errorMessage
     */
    private static void checkValuesIsNotZero(Map<Long, PlabInfo> logItems, String errorMessage) {
        checkValues(logItems, errorMessage, false);
    }

    private static void checkValues(Map<Long, PlabInfo> logItems, String errorMessage, boolean expectZero) {
        logItems.entrySet()
                .forEach(item -> item.getValue()
                        .values()
                        .forEach(items -> {
                            if (expectZero != (items == 0)) {
                                System.out.println(appPlabEvacFailureOutput);
                                throw new RuntimeException(errorMessage);
                            }
                        })
                );
    }

    /**
     * For tracking PLAB statistics for specified PLAB type - survivor and old
     */
    private static Map<Long, PlabInfo> getNonEvacFailureSurvivorStats() {
        return logParser.getExcludedSpecifiedStats(evacuationFailureIDs, LogParser.ReportType.SURVIVOR_STATS, FAILURE_STAT_FIELDS);
    }

    private static Map<Long, PlabInfo> getNonEvacFailureOldStats() {
        return logParser.getExcludedSpecifiedStats(evacuationFailureIDs, LogParser.ReportType.OLD_STATS, FAILURE_STAT_FIELDS);
    }

    private static Map<Long, PlabInfo> getEvacFailureSurvivorStats() {
        return logParser.getSpecifiedStats(evacuationFailureIDs, LogParser.ReportType.SURVIVOR_STATS, FAILURE_STAT_FIELDS);
    }

    private static Map<Long, PlabInfo> getEvacFailureOldStats() {
        return logParser.getSpecifiedStats(evacuationFailureIDs, LogParser.ReportType.OLD_STATS, FAILURE_STAT_FIELDS);
    }

    private static List<Long> getGcIdPlabEvacFailures(OutputAnalyzer out) {
        return out.asLines().stream()
                .filter(line -> line.contains("(Evacuation Failure)"))
                .map(line -> LogParser.getGcIdFromLine(line, GC_ID_PATTERN))
                .collect(Collectors.toList());
    }
}
