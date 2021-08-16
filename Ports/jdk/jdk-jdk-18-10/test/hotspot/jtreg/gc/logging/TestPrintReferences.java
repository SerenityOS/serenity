/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

package gc.logging;

/*
 * @test TestPrintReferences
 * @bug 8136991 8186402 8186465 8188245
 * @summary Validate the reference processing logging
 * @requires vm.gc.G1
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver gc.logging.TestPrintReferences
 */

import java.lang.ref.SoftReference;
import java.math.BigDecimal;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class TestPrintReferences {
    static String output;
    static final String doubleRegex = "[0-9]+[.,][0-9]+";
    static final String referenceProcessing = "Reference Processing";
    static final String softReference = "SoftReference";
    static final String weakReference = "WeakReference";
    static final String finalReference = "FinalReference";
    static final String phantomReference = "PhantomReference";

    static final String phaseNotifySoftWeakReferences = "Notify Soft/WeakReferences";
    static final String phaseNotifyKeepAliveFinalizer = "Notify and keep alive finalizable";
    static final String phaseNotifyPhantomReferences  = "Notify PhantomReferences";

    static final String gcLogTimeRegex = ".* GC\\([0-9]+\\) ";

    public static void main(String[] args) throws Exception {
        testPhases(true);
        testPhases(false);
        testRefs();
    }

    static String indent(int count) {
        return " {" + count + "}";
    }

    public static void testRefs() throws Exception {
        ProcessBuilder pb_enabled = ProcessTools.createJavaProcessBuilder("-Xlog:gc+ref+phases=debug",
                                                                          "-XX:+UseG1GC",
                                                                          "-Xmx32M",
                                                                          GCTest.class.getName());
        OutputAnalyzer output = new OutputAnalyzer(pb_enabled.start());

        checkRefsLogFormat(output);

        output.shouldHaveExitValue(0);
    }

    private static String refRegex(String reftype) {
        String countRegex = "[0-9]+";
        return gcLogTimeRegex + indent(6) + reftype + ":\n" +
               gcLogTimeRegex + indent(8) + "Discovered: " + countRegex + "\n" +
               gcLogTimeRegex + indent(8) + "Cleared: " + countRegex + "\n";
    }

    private static void checkRefsLogFormat(OutputAnalyzer output) {
        output.shouldMatch(refRegex("SoftReference") +
                           refRegex("WeakReference") +
                           refRegex("FinalReference") +
                           refRegex("PhantomReference"));
    }

    public static void testPhases(boolean parallelRefProcEnabled) throws Exception {
        ProcessBuilder pb_enabled = ProcessTools.createJavaProcessBuilder("-Xlog:gc+phases+ref=debug",
                                                                          "-XX:+UseG1GC",
                                                                          "-Xmx32M",
                                                                          "-XX:" + (parallelRefProcEnabled ? "+" : "-") + "ParallelRefProcEnabled",
                                                                          "-XX:-UseDynamicNumberOfGCThreads",
                                                                          "-XX:ParallelGCThreads=2",
                                                                          GCTest.class.getName());
        OutputAnalyzer output = new OutputAnalyzer(pb_enabled.start());

        checkLogFormat(output, parallelRefProcEnabled);
        checkLogValue(output);

        output.shouldHaveExitValue(0);
    }

    private static String phaseRegex(String phaseName) {
        final String timeRegex = doubleRegex + "ms";
        return indent(6) + phaseName + ": " + timeRegex + "\n";
    }

    private static String subphaseRegex(String subphaseName, boolean parallelRefProcEnabled) {
        final String timeRegex = "\\s+" + doubleRegex;
        if (parallelRefProcEnabled) {
            final String timeInParRegex = timeRegex +",\\s";
            return gcLogTimeRegex + indent(8) + subphaseName +
                   " \\(ms\\):\\s+(Min:" + timeInParRegex + "Avg:" + timeInParRegex + "Max:" + timeInParRegex + "Diff:" + timeInParRegex + "Sum:" + timeInParRegex +
                   "Workers: [0-9]+|skipped)" + "\n";
        } else {
            return gcLogTimeRegex + indent(8) + subphaseName + ":(" + timeRegex + "ms|\\s+skipped)\n";
        }
    }

    // Find the first Reference Processing log and check its format.
    private static void checkLogFormat(OutputAnalyzer output, boolean parallelRefProcEnabled) {
        String timeRegex = doubleRegex + "ms";

        /* Total Reference processing time */
        String totalRegex = gcLogTimeRegex + indent(4) + referenceProcessing + ": " + timeRegex + "\n";

        String balanceRegex = parallelRefProcEnabled ? "(" + gcLogTimeRegex + indent(8) + "Balance queues: " + timeRegex + "\n)??" : "";

        final boolean p = parallelRefProcEnabled;

        String phase2Regex = gcLogTimeRegex + phaseRegex(phaseNotifySoftWeakReferences) +
                             balanceRegex +
                             subphaseRegex("SoftRef", p) +
                             subphaseRegex("WeakRef", p) +
                             subphaseRegex("FinalRef", p) +
                             subphaseRegex("Total", p);
        String phase3Regex = gcLogTimeRegex + phaseRegex(phaseNotifyKeepAliveFinalizer) + balanceRegex + subphaseRegex("FinalRef", p);
        String phase4Regex = gcLogTimeRegex + phaseRegex(phaseNotifyPhantomReferences) + balanceRegex + subphaseRegex("PhantomRef", p);

        output.shouldMatch(totalRegex +
                           phase2Regex +
                           phase3Regex +
                           phase4Regex);
    }

    // After getting time value, update 'output' for next use.
    private static BigDecimal getTimeValue(String name, int indentCount) {
        // Pattern of 'name', 'value' and some extra strings.
        String patternString = gcLogTimeRegex + indent(indentCount) + name + ": " + "(" + doubleRegex + ")";
        Matcher m = Pattern.compile(patternString).matcher(output);
        if (!m.find()) {
            throw new RuntimeException("Could not find time log for " + patternString);
        }

        String match = m.group();
        String value = m.group(1);

        double result = Double.parseDouble(value);

        int index = output.indexOf(match);
        if (index != -1) {
            output = output.substring(index, output.length());
        }

        // Convert to BigDecimal to control the precision of floating point arithmetic.
        return BigDecimal.valueOf(result);
   }

    // Reference log is printing 1 decimal place of elapsed time.
    // So sum of each sub-phases could be slightly larger than the enclosing phase in some cases.
    // Because of this we need method to verify that our measurements and calculations are valid.
    private static boolean greaterThanOrApproximatelyEqual(BigDecimal phaseTime, BigDecimal sumOfSubPhasesTime, BigDecimal tolerance) {
        if (phaseTime.compareTo(sumOfSubPhasesTime) >= 0) {
            // phaseTime is greater than or equal.
            return true;
        }

        BigDecimal diff = sumOfSubPhasesTime.subtract(phaseTime);
        if (diff.compareTo(tolerance) <= 0) {
            // Difference is within tolerance, so approximately equal.
            return true;
        }

        // sumOfSubPhasesTime is greater than phaseTime and not within tolerance.
        return false;
    }

    // Find the first concurrent Reference Processing log and compare phase time vs. sum of sub-phases.
    public static void checkLogValue(OutputAnalyzer out) {
        output = out.getStdout();

        String patternString = gcLogTimeRegex + indent(0) +
                               referenceProcessing + ": " + "[0-9]+[.,][0-9]+";
        Matcher m = Pattern.compile(patternString).matcher(output);
        if (m.find()) {
            int start = m.start();
            int end = output.length();
            // If there's another concurrent Reference Processing log, ignore it.
            if (m.find()) {
                end = m.start();
            }
            if (start != -1) {
                output = output.substring(start, end);
                checkTrimmedLogValue();
            }
        }
    }

    private static void checkTrimmedLogValue() {
        BigDecimal refProcTime = getTimeValue(referenceProcessing, 0);

        BigDecimal sumOfSubPhasesTime = BigDecimal.ZERO;
        sumOfSubPhasesTime = sumOfSubPhasesTime.add(getTimeValue(phaseNotifySoftWeakReferences, 2));
        sumOfSubPhasesTime = sumOfSubPhasesTime.add(getTimeValue(phaseNotifyKeepAliveFinalizer, 2));
        sumOfSubPhasesTime = sumOfSubPhasesTime.add(getTimeValue(phaseNotifyPhantomReferences, 2));

        // If there are 3 phases, we should allow 0.2 tolerance.
        final BigDecimal toleranceFor3SubPhases = BigDecimal.valueOf(0.2);
        if (!greaterThanOrApproximatelyEqual(refProcTime, sumOfSubPhasesTime, toleranceFor3SubPhases)) {
            throw new RuntimeException("Reference Processing time(" + refProcTime + "ms) is less than the sum("
                                       + sumOfSubPhasesTime + "ms) of each phases");
        }
    }

    static class GCTest {
        static final int SIZE = 512 * 1024;
        static Object[] dummy = new Object[SIZE];

        public static void main(String [] args) {
             for (int i = 0; i < SIZE; i++) {
                  dummy[i] = new SoftReference<>(new Object());
             }
        }
    }
}
