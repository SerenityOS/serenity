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

import java.util.Arrays;
import java.text.NumberFormat;

import static jdk.test.lib.Asserts.*;
import jdk.test.lib.Utils;

/**
 * The helper class for parsing following output from command 'jstat -gcutil':
 *
 *  S0     S1     E      O      M     CCS    YGC     YGCT     FGC    FGCT     CGC    CGCT       GCT
 *  0.00   0.00   0.00  52.39  97.76  92.71      4     0.286    28    28.006     2     0.086    28.378
 *
 *  It will be verified that numerical values have defined types and are reasonable,
 *  for example percentage should fit within 0-100 interval.
 */
public class JstatGCUtilParser {

    public enum GcStatisticsType {
        INTEGER, DOUBLE, PERCENTAGE, PERCENTAGE_OR_DASH;
    }

    public enum GcStatistics {
        S0(GcStatisticsType.PERCENTAGE),
        S1(GcStatisticsType.PERCENTAGE),
        E(GcStatisticsType.PERCENTAGE),
        O(GcStatisticsType.PERCENTAGE),
        M(GcStatisticsType.PERCENTAGE_OR_DASH),
        CCS(GcStatisticsType.PERCENTAGE_OR_DASH),
        YGC(GcStatisticsType.INTEGER),
        YGCT(GcStatisticsType.DOUBLE),
        FGC(GcStatisticsType.INTEGER),
        FGCT(GcStatisticsType.DOUBLE),
        CGC(GcStatisticsType.INTEGER),
        CGCT(GcStatisticsType.DOUBLE),
        GCT(GcStatisticsType.DOUBLE);

        private final GcStatisticsType type;

        private GcStatistics(GcStatisticsType type) {
            this.type = type;
        }

        private GcStatisticsType getType() {
            return type;
        }

        public static boolean isHeadline(String... valueArray) {
            if (valueArray.length != values().length) {
                return false;
            }
            int headersCount = 0;
            for (int i = 0; i < values().length; i++) {
                if (valueArray[i].equals(values()[i].toString())) {
                    headersCount++;
                }
            }
            if (headersCount != values().length) {
                return false;
            }
            return true;
        }

        private static void verifyLength(String... valueArray) throws Exception {
            assertEquals(valueArray.length, values().length,
                    "Invalid number of data columns: " + Arrays.toString(valueArray));
        }

        public static void verify(String... valueArray) throws Exception {
            verifyLength(valueArray);
            for (int i = 0; i < values().length; i++) {
                GcStatisticsType type = values()[i].getType();
                String value = valueArray[i].trim();
                if (type.equals(GcStatisticsType.INTEGER)) {
                    NumberFormat.getInstance().parse(value).intValue();
                    break;
                }
                if (type.equals(GcStatisticsType.DOUBLE)) {
                    NumberFormat.getInstance().parse(value).doubleValue();
                    break;
                }
                if (type.equals(GcStatisticsType.PERCENTAGE_OR_DASH) &&
                        value.equals("-")) {
                    break;
                }
                double percentage = NumberFormat.getInstance().parse(value).doubleValue();
                assertTrue(0 <= percentage && percentage <= 100,
                        "Not a percentage: " + value);
            }
        }

    }

    private final String output;

    public JstatGCUtilParser(String output) {
        this.output = output;
    }

    public String getOutput() {
        return output;
    }

    /**
     * The function will discard any lines that come before the header line.
     * This can happen if the JVM outputs a warning message for some reason
     * before running jstat.
     */
    public void parse(int samples) throws Exception {
        boolean headlineFound = false;
        int datalineCount = 0;

        String[] lines = output.split(Utils.NEW_LINE);
        for (String line : lines) {
            line = line.replaceAll("\\s+", " ").trim();
            String[] valueArray = line.split(" ");

            if (!headlineFound) {
                headlineFound = GcStatistics.isHeadline(valueArray);
                continue;
            }

            GcStatistics.verify(valueArray);
            datalineCount++;
        }

        assertTrue(headlineFound, "No or invalid headline found, expected: " +
                Utils.NEW_LINE + Arrays.toString(GcStatistics.values()).replaceAll(",", " "));
        assertEquals(samples, datalineCount,
                "Expected " + samples + " samples, got " + datalineCount);
    }

}
