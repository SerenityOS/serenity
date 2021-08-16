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
package gc.g1.ihop.lib;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jdk.test.lib.process.OutputAnalyzer;


/**
 * Utility class to extract IHOP related information from the GC log.
 * The class provides a number of static method to be used from tests.
 */
public class IhopUtils {

    // Examples of GC log for IHOP:
    // [0.402s][debug][gc,ergo,ihop] GC(9) Do not request concurrent cycle initiation (still doing mixed collections) occupancy: 66060288B allocation request: 0B threshold: 59230757B (88.26) source: end of GC
    // [0.466s][debug][gc,ergo,ihop] GC(18) Request concurrent cycle initiation (occupancy higher than threshold) occupancy: 52428800B allocation request: 0B threshold: 0B (0.00) source: end of GC

    /**
     * Patterns are used for extracting occupancy and threshold from GC log.
     */
    private final static Pattern OCCUPANCY = Pattern.compile("occupancy: (\\d+)B");
    private final static Pattern THRESHOLD = Pattern.compile("threshold: (\\d+)B");

    /**
     * Messages related to concurrent cycle initiation.
     */
    private final static String CYCLE_INITIATION_MESSAGE = "Request concurrent cycle initiation (occupancy higher than threshold)";
    private final static String CYCLE_INITIATION_MESSAGE_FALSE = "Do not request concurrent cycle initiation (still doing mixed collections)";
    private final static String ADAPTIVE_IHOP_PREDICTION_ACTIVE_MESSAGE = "prediction active: true";

    /**
     * Finds strings which contains patterns for finding.
     *
     * @param outputAnalyzer List of string for IHOP messages extraction
     * @param stringsToFind Strings which is checked for matching with OutputAnalyzer content
     * @return List of strings which were matched.
     */
    private static List<String> findInLog(OutputAnalyzer outputAnalyzer, String... stringsToFind) {
        return outputAnalyzer.asLines().stream()
                .filter(string -> {
                    return Stream.of(stringsToFind)
                            .filter(find -> string.contains(find))
                            .findAny()
                            .isPresent();
                })
                .collect(Collectors.toList());
    }

    /**
     * Checks that memory occupancy is greater or equal to the threshold.
     * This methods searches for occupancy and threshold in the GC log corresponding Conc Mark Cycle initiation
     * and compare their values.If no CMC initiation happens, does nothing.
     * @param outputAnalyzer OutputAnalyzer which contains GC log to be checked
     * @throw RuntimeException If check fails
     */
    public static void checkIhopLogValues(OutputAnalyzer outputAnalyzer) {
        // Concurrent cycle was initiated but was not expected.
        // Checks occupancy should be greater than threshold.
        List<String> logItems = IhopUtils.getErgoMessages(outputAnalyzer);
        logItems.stream()
                .forEach(item -> {
                    long occupancy = IhopUtils.getLongByPattern(item, IhopUtils.OCCUPANCY);
                    long threshold = IhopUtils.getLongByPattern(item, IhopUtils.THRESHOLD);
                    if (occupancy < threshold) {
                        System.out.println(outputAnalyzer.getOutput());
                        throw new RuntimeException("Concurrent cycle initiation is unexpected. Occupancy (" + occupancy + ") is less then threshold (" + threshold + ")");
                    }
                    System.out.printf("Concurrent cycle was initiated with occupancy = %d and threshold = %d%n", occupancy, threshold);
                });
    }

    private static Long getLongByPattern(String line, Pattern pattern) {
        Matcher number = pattern.matcher(line);
        if (number.find()) {
            return Long.parseLong(number.group(1));
        }
        System.out.println(line);
        throw new RuntimeException("Cannot find Long in string.");
    }

    /**
     * Finds concurrent cycle initiation messages.
     * @param outputAnalyzer OutputAnalyzer
     * @return List with messages which were found.
     */
    public static List<String> getErgoInitiationMessages(OutputAnalyzer outputAnalyzer) {
        return IhopUtils.findInLog(outputAnalyzer, CYCLE_INITIATION_MESSAGE);
    }

    /**
     * Gets IHOP ergo messages from GC log.
     * @param outputAnalyzer
     * @return List with found messages
     */
    private static List<String> getErgoMessages(OutputAnalyzer outputAnalyzer) {
        return IhopUtils.findInLog(outputAnalyzer, CYCLE_INITIATION_MESSAGE, CYCLE_INITIATION_MESSAGE_FALSE);
    }

    /**
     * Checks that GC log contains expected ergonomic messages
     * @param outputAnalyzer OutputAnalyer with GC log for checking
     * @throws RuntimeException If no IHOP ergo messages were not found
     */
    public static void checkErgoMessagesExist(OutputAnalyzer outputAnalyzer) {
        String output = outputAnalyzer.getOutput();
        if (!(output.contains(CYCLE_INITIATION_MESSAGE) | output.contains(CYCLE_INITIATION_MESSAGE_FALSE))) {
            throw new RuntimeException("Cannot find expected IHOP ergonomics messages");
        }
    }

    /**
     * Checks that adaptive IHOP was activated
     * @param outputAnalyzer OutputAnalyer with GC log for checking
     * @throws RuntimeException If IHOP message was not found.
     */
    public static void checkAdaptiveIHOPWasActivated(OutputAnalyzer outputAnalyzer) {
        outputAnalyzer.shouldContain(ADAPTIVE_IHOP_PREDICTION_ACTIVE_MESSAGE);
    }
}
