/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test TestLargePageUseForHeap.java
 * @summary Test that Java heap is allocated using large pages of the appropriate size if available.
 * @bug 8221517
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @requires vm.gc.G1
 * @requires vm.opt.LargePageSizeInBytes == null
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UseG1GC -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
        -XX:+IgnoreUnrecognizedVMOptions -XX:+UseLargePages gc.g1.TestLargePageUseForHeap
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jtreg.SkippedException;
import sun.hotspot.WhiteBox;

public class TestLargePageUseForHeap {
    static long largePageSize;
    static long smallPageSize;

    static void checkSize(OutputAnalyzer output, long expectedSize, String pattern) {
        String pageSizeStr = output.firstMatch(pattern, 1);

        if (pageSizeStr == null) {
            output.reportDiagnosticSummary();
            throw new RuntimeException("Match from '" + pattern + "' got 'null' expected: " + expectedSize);
        }

        long size = parseMemoryString(pageSizeStr);
        if (size != expectedSize) {
            output.reportDiagnosticSummary();
            throw new RuntimeException("Match from '" + pattern + "' got " + size + " expected: " + expectedSize);
        }
    }

    static boolean checkLargePageEnabled(OutputAnalyzer output) {
        String lp = output.firstMatch("Large Page Support: (\\w*)", 1);
        // Make sure large pages really are enabled.
        if (lp == null || lp.equals("Disabled")) {
            return false;
        }
        // This message is printed when tried to reserve a memory with large page but it failed.
        String errorStr = "Reserve regular memory without large pages";
        String heapPattern = ".*Heap: ";
        // If errorStr is printed just before heap page log, reservation for Java Heap is failed.
        String result = output.firstMatch(errorStr + "\n" +
                                          "(?:.*Heap address: .*\n)?" // Heap address: 0x00000000f8000000, size: 128 MB, Compressed Oops mode: 32-bit
                                          + heapPattern);
        if (result != null) {
            return false;
        }
        return true;
    }

    static void checkHeap(OutputAnalyzer output, long expectedPageSize) throws Exception {
        checkSize(output, expectedPageSize, "Heap: .*page_size=([^ ]+)");
    }

    static void testVM(long regionSize) throws Exception {
        ProcessBuilder pb;
        // Test with large page enabled.
        pb = ProcessTools.createJavaProcessBuilder("-XX:+UseG1GC",
                                                   "-XX:G1HeapRegionSize=" + regionSize,
                                                   "-Xmx128m",
                                                   "-Xlog:gc+init,pagesize,gc+heap+coops=debug",
                                                   "-XX:+UseLargePages",
                                                   "-version");

        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        boolean largePageEnabled = checkLargePageEnabled(output);
        checkHeap(output, largePageEnabled ? largePageSize : smallPageSize);
        output.shouldHaveExitValue(0);

        // Test with large page disabled.
        pb = ProcessTools.createJavaProcessBuilder("-XX:+UseG1GC",
                                                   "-XX:G1HeapRegionSize=" + regionSize,
                                                   "-Xmx128m",
                                                   "-Xlog:gc+init,pagesize,gc+heap+coops=debug",
                                                   "-XX:-UseLargePages",
                                                   "-version");

        output = new OutputAnalyzer(pb.start());
        checkHeap(output, smallPageSize);
        output.shouldHaveExitValue(0);
    }

    public static void main(String[] args) throws Exception {
        WhiteBox wb = WhiteBox.getWhiteBox();
        smallPageSize = wb.getVMPageSize();
        largePageSize = wb.getVMLargePageSize();

        if (largePageSize == 0) {
            throw new SkippedException("Large page support does not seem to be available on this platform.");
        }
        if (largePageSize == smallPageSize) {
            throw new SkippedException("Large page support does not seem to be available on this platform."
                    + "Small and large page size are the same.");
        }

        // G1HeapRegionSize=1MB
        testVM(1 * 1024 * 1024);

        // G1HeapRegionSize=2MB
        testVM(2 * 1024 * 1024);

        // G1HeapRegionSize=8MB
        testVM(8 * 1024 * 1024);
    }

    public static long parseMemoryString(String value) {
        long multiplier = 1;

        if (value.endsWith("B")) {
            multiplier = 1;
        } else if (value.endsWith("K")) {
            multiplier = 1024;
        } else if (value.endsWith("M")) {
            multiplier = 1024 * 1024;
        } else if (value.endsWith("G")) {
            multiplier = 1024 * 1024 * 1024;
        } else {
            throw new IllegalArgumentException("Expected memory string '" + value + "'to end with either of: B, K, M, G");
        }

        long longValue = Long.parseUnsignedLong(value.substring(0, value.length() - 1));

        return longValue * multiplier;
    }
}
