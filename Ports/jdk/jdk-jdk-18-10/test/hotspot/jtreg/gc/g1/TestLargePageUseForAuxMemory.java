/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test TestLargePageUseForAuxMemory.java
 * @summary Test that auxiliary data structures are allocated using large pages if available.
 * @bug 8058354 8079208
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @requires vm.gc.G1
 * @requires vm.opt.LargePageSizeInBytes == null
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UseG1GC -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -XX:+IgnoreUnrecognizedVMOptions -XX:+UseLargePages gc.g1.TestLargePageUseForAuxMemory
 */

import java.lang.Math;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.Asserts;
import jdk.test.lib.Platform;
import jtreg.SkippedException;
import sun.hotspot.WhiteBox;

public class TestLargePageUseForAuxMemory {
    static final long HEAP_REGION_SIZE = 1 * 1024 * 1024;
    static long largePageSize;
    static long smallPageSize;
    static long allocGranularity;

    static boolean largePagesEnabled(OutputAnalyzer output) {
        // The gc+init logging includes information about large pages.
        String lp = output.firstMatch("Large Page Support: (\\w*)", 1);
        return lp != null && lp.equals("Enabled");
    }

    static boolean largePagesAllocationFailure(OutputAnalyzer output, String pattern) {
        // Check if there is a large page failure associated with the data  structure
        // being checked. In case of a large page allocation failure the output will
        // include logs like this for the affected data structure:
        // [0.048s][debug][gc,heap,coops] Reserve regular memory without large pages
        // [0.048s][info ][pagesize     ] Next Bitmap: ... page_size=4K ...
        //
        // The pattern passed in should match the second line.
        String failureMatch = output.firstMatch("Reserve regular memory without large pages\\n.*" + pattern, 1);
        if (failureMatch != null) {
            return true;
        }
        return false;
    }

    static void checkSize(OutputAnalyzer output, long expectedSize, String pattern) {
        // First check the output for any large page allocation failure associated with
        // the checked data structure. If we detect a failure then expect small pages.
        if (largePagesAllocationFailure(output, pattern)) {
            // This should only happen when we are expecting large pages
            if (expectedSize == smallPageSize) {
                throw new RuntimeException("Expected small page size when large page failure was detected");
            }
            expectedSize = smallPageSize;
        }

        // Now check what page size is traced.
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

    static void checkSmallTables(OutputAnalyzer output, long expectedPageSize) throws Exception {
        checkSize(output, expectedPageSize, "Block Offset Table: .*page_size=([^ ]+)");
        checkSize(output, expectedPageSize, "Card Counts Table: .*page_size=([^ ]+)");
    }

    static void checkBitmaps(OutputAnalyzer output, long expectedPageSize) throws Exception {
        checkSize(output, expectedPageSize, "Prev Bitmap: .*page_size=([^ ]+)");
        checkSize(output, expectedPageSize, "Next Bitmap: .*page_size=([^ ]+)");
    }

    static void testVM(String what, long heapsize, boolean cardsShouldUseLargePages, boolean bitmapShouldUseLargePages) throws Exception {
        System.out.println(what + " heapsize " + heapsize + " card table should use large pages " + cardsShouldUseLargePages + " " +
                           "bitmaps should use large pages " + bitmapShouldUseLargePages);
        ProcessBuilder pb;
        // Test with large page enabled.
        pb = ProcessTools.createJavaProcessBuilder("-XX:+UseG1GC",
                                                   "-XX:G1HeapRegionSize=" + HEAP_REGION_SIZE,
                                                   "-Xmx" + heapsize,
                                                   "-Xlog:pagesize,gc+init,gc+heap+coops=debug",
                                                   "-XX:+UseLargePages",
                                                   "-XX:+IgnoreUnrecognizedVMOptions",  // there is no ObjectAlignmentInBytes in 32 bit builds
                                                   "-XX:ObjectAlignmentInBytes=8",
                                                   "-version");

        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        // Only expect large page size if large pages are enabled.
        if (largePagesEnabled(output)) {
            checkSmallTables(output, (cardsShouldUseLargePages ? largePageSize : smallPageSize));
            checkBitmaps(output, (bitmapShouldUseLargePages ? largePageSize : smallPageSize));
        } else {
            checkSmallTables(output, smallPageSize);
            checkBitmaps(output, smallPageSize);
        }
        output.shouldHaveExitValue(0);

        // Test with large page disabled.
        pb = ProcessTools.createJavaProcessBuilder("-XX:+UseG1GC",
                                                   "-XX:G1HeapRegionSize=" + HEAP_REGION_SIZE,
                                                   "-Xmx" + heapsize,
                                                   "-Xlog:pagesize",
                                                   "-XX:-UseLargePages",
                                                   "-XX:+IgnoreUnrecognizedVMOptions",  // there is no ObjectAlignmentInBytes in 32 bit builds
                                                   "-XX:ObjectAlignmentInBytes=8",
                                                   "-version");

        output = new OutputAnalyzer(pb.start());
        checkSmallTables(output, smallPageSize);
        checkBitmaps(output, smallPageSize);
        output.shouldHaveExitValue(0);
    }

    private static long gcd(long x, long y) {
        while (x > 0) {
            long t = x;
            x = y % x;
            y = t;
        }
        return y;
    }

    private static long lcm(long x, long y) {
        return x * (y / gcd(x, y));
    }

    public static void main(String[] args) throws Exception {
        // Size that a single card covers.
        final int cardSize = 512;
        WhiteBox wb = WhiteBox.getWhiteBox();
        smallPageSize = wb.getVMPageSize();
        largePageSize = wb.getVMLargePageSize();
        allocGranularity = wb.getVMAllocationGranularity();
        final long heapAlignment = lcm(cardSize * smallPageSize, largePageSize);

        if (largePageSize == 0) {
            throw new SkippedException("Large page support does not seem to be available on this platform.");
        }
        if (largePageSize == smallPageSize) {
            throw new SkippedException("Large page support does not seem to be available on this platform."
                    + "Small and large page size are the same.");
        }

        // To get large pages for the card table etc. we need at least a 1G heap (with 4k page size).
        // 32 bit systems will have problems reserving such an amount of contiguous space, so skip the
        // test there.
        if (!Platform.is32bit()) {
            final long heapSizeForCardTableUsingLargePages = largePageSize * cardSize;
            final long heapSizeDiffForCardTable = Math.max(Math.max(allocGranularity * cardSize, HEAP_REGION_SIZE), largePageSize);

            Asserts.assertGT(heapSizeForCardTableUsingLargePages, heapSizeDiffForCardTable,
                             "To test we would require to use an invalid heap size");
            testVM("case1: card table and bitmap use large pages (barely)", heapSizeForCardTableUsingLargePages, true, true);
            testVM("case2: card table and bitmap use large pages (extra slack)", heapSizeForCardTableUsingLargePages + heapSizeDiffForCardTable, true, true);
            testVM("case3: only bitmap uses large pages (barely not)", heapSizeForCardTableUsingLargePages - heapSizeDiffForCardTable, false, true);
        }

        // Minimum heap requirement to get large pages for bitmaps is 128M heap. This seems okay to test
        // everywhere.
        final int bitmapTranslationFactor = 8 * 8; // ObjectAlignmentInBytes * BitsPerByte
        final long heapSizeForBitmapUsingLargePages = largePageSize * bitmapTranslationFactor;
        final long heapSizeDiffForBitmap = Math.max(Math.max(allocGranularity * bitmapTranslationFactor, HEAP_REGION_SIZE),
                                                    Math.max(largePageSize, heapAlignment));

        Asserts.assertGT(heapSizeForBitmapUsingLargePages, heapSizeDiffForBitmap,
                         "To test we would require to use an invalid heap size");

        testVM("case4: only bitmap uses large pages (barely)", heapSizeForBitmapUsingLargePages, false, true);
        testVM("case5: only bitmap uses large pages (extra slack)", heapSizeForBitmapUsingLargePages + heapSizeDiffForBitmap, false, true);
        testVM("case6: nothing uses large pages (barely not)", heapSizeForBitmapUsingLargePages - heapSizeDiffForBitmap, false, false);
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
