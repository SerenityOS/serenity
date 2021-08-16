/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @requires vm.gc != "Epsilon"
 * @summary Stress the string table and cleaning.
 * Test argument is the approximate number of seconds to run.
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm
 *    -Xbootclasspath/a:.
 *    -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *    runtime.stringtable.StringTableCleaningTest 30
 */

package runtime.stringtable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import sun.hotspot.gc.GC;

public class StringTableCleaningTest {
    public static void main(String[] args) throws Exception {
        List<String> subargs = new ArrayList<String>();
        subargs.addAll(List.of("-Xlog:gc,gc+start,stringtable*=trace", "-Xmx1g"));
        subargs.add(Tester.class.getName());
        subargs.addAll(Arrays.asList(args));
        OutputAnalyzer output = ProcessTools.executeTestJvm(subargs);
        output.shouldHaveExitValue(0);
        checkOutput(output);
    }

    private static int fail(String msg) throws Exception {
        throw new RuntimeException(msg);
    }

    // Recognizing GC start and end log lines.

    private static final String gcPrefix = "\\[info\\s*\\]\\[gc";
    private static final String gcMiddle = "\\s*\\] GC\\(\\p{Digit}+\\) ";

    private static final String gcStartPrefix = gcPrefix + ",start" + gcMiddle;
    private static final String gcEndPrefix = gcPrefix + gcMiddle;

    // Suffix for SerialGC and ParallelGC.
    private static final String spSuffix = "Pause";

    // All G1 pauses except Cleanup do weak reference clearing.
    private static final String g1Suffix = "Pause(?! Cleanup)";

    // Suffix for ZGC.
    private static final String zStartSuffix = "Garbage Collection (.*)$";
    private static final String zEndSuffix = "Garbage Collection (.*) .*->.*$";

    // Suffix for Shenandoah.
    private static final String shenSuffix = "Concurrent weak roots";

    private static String getGcStartString() {
        if (GC.Serial.isSelected() || GC.Parallel.isSelected()) {
            return gcStartPrefix + spSuffix;
        } else if (GC.G1.isSelected()) {
            return gcStartPrefix + g1Suffix;
        } else if (GC.Z.isSelected()) {
            return gcStartPrefix + zStartSuffix;
        } else if (GC.Shenandoah.isSelected()) {
            return gcStartPrefix + shenSuffix;
        } else {
            return "unsupported GC";
        }
    }

    private static String getGcEndString() {
        if (GC.Serial.isSelected() || GC.Parallel.isSelected()) {
            return gcEndPrefix + spSuffix;
        } else if (GC.G1.isSelected()) {
            return gcEndPrefix + g1Suffix;
        } else if (GC.Z.isSelected()) {
            return gcEndPrefix + zEndSuffix;
        } else if (GC.Shenandoah.isSelected()) {
            return gcEndPrefix + shenSuffix;
        } else {
            return "unsupported GC";
        }
    }

    private static Pattern getGcStartPattern() {
        return Pattern.compile(getGcStartString());
    }

    private static Pattern getGcEndPattern() {
        return Pattern.compile(getGcEndString());
    }

    private static final Pattern pGcStart = getGcStartPattern();
    private static final Pattern pGcEnd = getGcEndPattern();

    // Recognizing StringTable GC callback log lines.

    private static final Pattern pCallback =
        Pattern.compile("\\[trace\\s*\\]\\[stringtable\\s*\\] Uncleaned items:");

    private static boolean matchesPattern(String line, Pattern regex) {
        return regex.matcher(line).find();
    }

    private static boolean matchesStart(String line) {
        return matchesPattern(line, pGcStart);
    }

    private static boolean matchesEnd(String line) {
        return matchesPattern(line, pGcEnd);
    }

    private static boolean matchesCallback(String line) {
        return matchesPattern(line, pCallback);
    }

    // Search the lines for the first GC start log line in lines, starting
    // from fromIndex.  Returns the index of that line, or -1 if no GC start
    // line found.  Throws if a callback or GC end line is found first.
    private static int findStart(List<String> lines, int fromIndex)
        throws Exception
    {
        for (int i = fromIndex; i < lines.size(); ++i) {
            String line = lines.get(i);
            if (matchesStart(line)) {
                return i;
            } else if (matchesEnd(line)) {
                fail("End without Start: " + i);
            } else if (matchesCallback(line)) {
                fail("Callback without Start: " + i);
            }
        }
        return -1;
    }

    // Search the lines for the first callback log line in lines, starting
    // after gcStart.  Returns the index of that line, or -1 if no callback
    // line is found (concurrent GC could start but not complete).  Throws
    // if a GC start or GC end log line is found first.
    private static int findCallback(List<String> lines, int gcStart)
        throws Exception
    {
        for (int i = gcStart + 1; i < lines.size(); ++i) {
            String line = lines.get(i);
            if (matchesCallback(line)) {
                return i;
            } else if (matchesEnd(line)) {
                fail("Missing Callback in [" + gcStart + ", " + i + "]");
            } else if (matchesStart(line)) {
                fail("Two Starts: " + gcStart + ", " + i);
            }
        }
        return -1;
    }

    // Search the lines for the first GC end log line in lines, starting
    // after callback.  Returns the index of that line, or -1 if no GC end
    // line is found (concurrent GC could start but not complete).  Throws
    // if a GC start or a callback log line is found first.
    private static int findEnd(List<String> lines, int gcStart, int callback)
        throws Exception
    {
        for (int i = callback + 1; i < lines.size(); ++i) {
            String line = lines.get(i);
            if (matchesEnd(line)) {
                return i;
            } else if (matchesStart(line)) {
                fail("Missing End for Start: " + gcStart + " at " + i);
            } else if (matchesCallback(line)) {
                fail("Multiple Callbacks for Start: " + gcStart + " at " + i);
            }
        }
        return -1;
    }

    private static int check(List<String> lines, int fromIndex) throws Exception {
        int gcStart = findStart(lines, fromIndex);
        if (gcStart < 0) return -1;
        int callback = findCallback(lines, gcStart);
        if (callback < 0) return -1;
        int gcEnd = findEnd(lines, gcStart, callback);
        if (gcEnd < 0) return -1;
        return gcEnd + 1;
    }

    private static void checkOutput(OutputAnalyzer output) throws Exception {
        List<String> lines = output.asLines();
        int count = -1;
        int i = 0;
        try {
            for ( ; i >= 0; i = check(lines, i)) { ++count; }
        } finally {
            if (i < 0) {
                System.out.println("Output check passed with " + count + " GCs");
            } else {
                System.out.println("--- Output check failed: " + count + " -----");
                System.out.println(output.getOutput());
            }
        }
    }

    static class Tester {
        private static volatile boolean stopRequested = false;

        private static final TimeUnit durationUnits = TimeUnit.SECONDS;

        public static void main(String[] args) throws Exception {
            long duration = Long.parseLong(args[0]);
            runTest(duration);
        }

        public static void runTest(long duration) throws Exception {
            ScheduledExecutorService scheduler =
                Executors.newScheduledThreadPool(1);
            try {
                ScheduledFuture<?> stopper =
                    scheduler.schedule(() -> stopRequested = true,
                                       duration,
                                       durationUnits);
                try {
                    stringMaker(10000000, 100000, 50000);
                } finally {
                    stopper.cancel(false);
                }
            } finally {
                scheduler.shutdownNow();
            }
        }

        private static void stringMaker(int maxSize, int growStep, int shrinkStep)
            throws Exception
        {
            long stringNum = 0;
            while (true) {
                LinkedList<String> list = new LinkedList<String>();
                for (int i = 0; i < maxSize; ++i, ++stringNum) {
                    if (stopRequested) {
                        return;
                    }
                    if ((i != 0) && ((i % growStep) == 0)) {
                        list.subList(0, shrinkStep).clear();
                    }
                    list.push(Long.toString(stringNum).intern());
                }
                // For generational collectors, try to move current list
                // contents to old-gen before dropping the list.
                System.gc();
            }
        }
    }
}
