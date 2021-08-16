/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test TestEagerReclaimHumongousRegionsLog
 * @summary Check that G1 reports humongous eager reclaim statistics correctly.
 * @requires vm.gc.G1
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run driver gc.g1.TestEagerReclaimHumongousRegionsLog
 */

import sun.hotspot.WhiteBox;

import java.util.Arrays;
import jdk.test.lib.Asserts;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestEagerReclaimHumongousRegionsLog {

    private static final String LogSeparator = ": ";

    static final String SumSeparator = "Sum: ";

    private static String getSumValue(String s) {
        return s.substring(s.indexOf(SumSeparator) + SumSeparator.length(), s.indexOf(", Workers"));
    }

    public static void runTest() throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
            "-Xbootclasspath/a:.",
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:+WhiteBoxAPI",
            "-XX:+UseG1GC",
            "-XX:G1HeapRegionSize=1M",
            "-Xms128M",
            "-Xmx128M",
            "-Xlog:gc+phases=trace,gc+heap=info",
            GCTest.class.getName());
        OutputAnalyzer output = new OutputAnalyzer(pb.start());

        output.shouldHaveExitValue(0);

        System.out.println(output.getStdout());

        // This gives an array of lines containing eager reclaim of humongous regions
        // log messages contents after the ":" in the following order for every GC:
        //   Humongous Regions: f->g
        //   Region Register: a.ams
        //   Eagerly Reclaim Humonguous Objects b.cms
        //   Humongous Total: Min: 1, Avg:  1.0, Max: 1, Diff: 0, Sum: c, Workers: 1
        //   Humongous Candidate: Min: 1, Avg:  1.0, Max: 1, Diff: 0, Sum: d, Workers: 1
        //   Humongous Reclaimed: Min: 1, Avg:  1.0, Max: 1, Diff: 0, Sum: e, Workers: 1

        String[] lines = Arrays.stream(output.getStdout().split("\\R"))
                         .filter(s -> (s.contains("Humongous") || s.contains("Region Register"))).map(s -> s.substring(s.indexOf(LogSeparator) + LogSeparator.length()))
                         .toArray(String[]::new);

        Asserts.assertTrue(lines.length % 6 == 0, "There seems to be an unexpected amount of log messages (total: " + lines.length + ") per GC");

        for (int i = 0; i < lines.length; i += 6) {
            int total = Integer.parseInt(getSumValue(lines[i + 3]));
            int candidate = Integer.parseInt(getSumValue(lines[i + 4]));
            int reclaimed = Integer.parseInt(getSumValue(lines[i + 5]));

            int before = Integer.parseInt(lines[i].substring(0, 1));
            int after = Integer.parseInt(lines[i].substring(3, 4));
            System.out.println("total " + total + " candidate " + candidate + " reclaimed " + reclaimed + " before " + before + " after " + after);

            Asserts.assertEQ(total, candidate, "Not all humonguous objects are candidates");
            Asserts.assertLTE(reclaimed, candidate, "The number of reclaimed objects must be less or equal than the number of candidates");

            if (reclaimed > 0) {
               Asserts.assertLT(after, before, "Number of regions after must be smaller than before.");
               Asserts.assertEQ(reclaimed, candidate, "Must have reclaimed all candidates.");
               Asserts.assertGT((before - after), reclaimed, "Number of regions reclaimed (" + (before - after) +
                                ") must be larger than number of objects reclaimed (" + reclaimed + ")");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        runTest();
    }

    static class GCTest {
        private static final WhiteBox WB = WhiteBox.getWhiteBox();

        public static Object holder;

        public static void main(String [] args) {
            // Create a humongous objects spanning multiple regions so that the difference
            // between number of humongous objects reclaimed and number of regions reclaimed
            // is apparent.
            holder = new byte[4 * 1024 * 1024];
            WB.youngGC();
            System.out.println(holder);
            holder = null;
            WB.youngGC();
        }
    }
}

