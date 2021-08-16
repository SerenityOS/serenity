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
package gc.g1.plab.lib;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * LogParser class parses VM output to get PLAB and ConsumptionStats values.
 *
 * Typical GC log with PLAB statistics (options - -Xlog:gc=debug,gc+plab=debug) looks like:
 *
 * [0.330s][debug][gc,plab  ] GC(0) Young PLAB allocation: allocated: 1825632B, wasted: 29424B, unused: 2320B, used: 1793888B, undo waste: 0B,
 * [0.330s][debug][gc,plab  ] GC(0) Young other allocation: region end waste: 0B, regions filled: 2, direct allocated: 271520B, failure used: 0B, failure wasted: 0B
 * [0.330s][debug][gc,plab  ] GC(0) Young sizing: calculated: 358776B, actual: 358776B
 * [0.330s][debug][gc,plab  ] GC(0) Old PLAB allocation: allocated: 427248B, wasted: 592B, unused: 368584B, used: 58072B, undo waste: 0B,
 * [0.330s][debug][gc,plab  ] GC(0) Old other allocation: region end waste: 0B, regions filled: 1, direct allocated: 41704B, failure used: 0B, failure wasted: 0B
 * [0.330s][debug][gc,plab  ] GC(0) Old sizing: calculated: 11608B, actual: 11608B
 */
final public class LogParser {

    /**
     * Type of parsed log element.
     */
    public static enum ReportType {
        SURVIVOR_STATS,
        OLD_STATS
    }

    private final String log;

    // Contains Map of PLAB statistics for given log.
    private final PlabReport report;

    // GC ID
    private static final Pattern GC_ID_PATTERN = Pattern.compile("\\[gc,plab\\s*\\] GC\\((\\d+)\\)");
    // Pattern for extraction pair <name>: <numeric value>
    private static final Pattern PAIRS_PATTERN = Pattern.compile("\\w* \\w+:\\s+\\d+");

    /**
     * Construct LogParser object, parse log file with PLAB statistics and store it into report.
     *
     * @param log - VM Output
     */
    public LogParser(String log) {
        if (log == null) {
            throw new IllegalArgumentException("Parameter log should not be null.");
        }
        this.log = log;
        report = parseLines();
    }

    /**
     * @return log which was processed
     */
    public String getLog() {
        return log;
    }

    /**
     * Returns the GC log entries for Survivor and Old stats.
     * The entries are represented as a map of gcID to the StatMap.
     *
     * @return The log entries for the Survivor and Old stats.
     */
    public PlabReport getEntries() {
        return report;
    }

    private PlabReport parseLines() throws NumberFormatException {
        try (Scanner lineScanner = new Scanner(log)) {
            PlabReport plabReport = new PlabReport();
            Optional<Long> gc_id;
            while (lineScanner.hasNextLine()) {
                String line = lineScanner.nextLine();
                gc_id = getGcId(line, GC_ID_PATTERN);
                if (gc_id.isPresent()) {
                    Matcher matcher = PAIRS_PATTERN.matcher(line);
                    if (matcher.find()) {
                        if (!plabReport.containsKey(gc_id.get())) {
                            plabReport.put(gc_id.get(), new PlabGCStatistics());
                        }
                        ReportType reportType = line.contains("Young") ? ReportType.SURVIVOR_STATS : ReportType.OLD_STATS;

                        PlabGCStatistics gcStat = plabReport.get(gc_id.get());
                        if (!gcStat.containsKey(reportType)) {
                            gcStat.put(reportType, new PlabInfo());
                        }

                        // Extract all pairs from log.
                        PlabInfo plabInfo = gcStat.get(reportType);
                        do {
                            String pair = matcher.group();
                            String[] nameValue = pair.replaceAll(": ", ":").split(":");
                            plabInfo.put(nameValue[0].trim(), Long.parseLong(nameValue[1]));
                        } while (matcher.find());
                    }
                }
            }
            return plabReport;
        }
    }

    private static Optional<Long> getGcId(String line, Pattern pattern) {
        Matcher number = pattern.matcher(line);
        if (number.find()) {
            return Optional.of(Long.parseLong(number.group(1)));
        }
        return Optional.empty();
    }

    /**
     * Extracts GC ID from log.
     *
     * @param line - one line of log.
     * @return GC ID
     */
    public static Long getGcIdFromLine(String line, Pattern pattern) {
        Optional<Long> gcId = getGcId(line, pattern);
        if (!gcId.isPresent()) {
            System.out.println(line);
            throw new RuntimeException("Cannot find GC ID in log.");
        }
        return gcId.get();
    }

    /**
     * Returns Map<Long,PlabStatistics> which contains specified statistics for specified gc ids.
     * @param specifiedGcId gc id to get
     * @param type PLAB type
     * @param fieldsName name of fields in PlabStatistics
     * @return
     **/
    public Map<Long, PlabInfo> getSpecifiedStats(List<Long> specifiedGcId, LogParser.ReportType type, List<String> fieldsName) {
        return getSpecifiedStats(specifiedGcId, type, fieldsName, true);
    }

    /**
     * Returns PlabStatistics for specified GC ID.
     * @param specifiedGcId
     * @param type type of statistics
     * @param fieldsName name of fields in PlabStatistics
     * @return
     **/
    public PlabInfo getSpecifiedStats(long specifiedGcId, LogParser.ReportType type, List<String> fieldsName) {
        PlabInfo info = getSpecifiedStats(Arrays.asList(specifiedGcId), type, fieldsName, true).get(specifiedGcId);
        if (info == null) {
            System.out.println(log);
            throw new RuntimeException("Cannot find PLAB statistics in log ( GC_ID=" + specifiedGcId + " type=" + type + " )");
        }
        return info;
    }

    /**
     * Returns Map<Long,PlabStatistics> which contains specified statistics. Filters out specified gc ids.
     * @param specifiedGcIdForExclude
     * @param type
     * @param fieldsName
     * @return
     **/
    public Map<Long, PlabInfo> getExcludedSpecifiedStats(List<Long> specifiedGcIdForExclude, LogParser.ReportType type, List<String> fieldsName) {
        return getSpecifiedStats(specifiedGcIdForExclude, type, fieldsName, false);
    }

    private Map<Long, PlabInfo> getSpecifiedStats(List<Long> gcIds, LogParser.ReportType type, List<String> fieldNames, boolean extractId) {
        var map = new HashMap<>(
                        getEntries().entryStream()
                        .filter(gcLogItem -> extractId == gcIds.contains(gcLogItem.getKey()))
                        .collect(Collectors.toMap(gcLogItem -> gcLogItem.getKey(),
                                        gcLogItem -> gcLogItem.getValue().get(type).filter(fieldNames)
                                )
                        )
                 );
        if (map.isEmpty()) {
            throw new RuntimeException("Cannot find relevant PLAB statistics in the log");
        }
        return map;
    }
}
