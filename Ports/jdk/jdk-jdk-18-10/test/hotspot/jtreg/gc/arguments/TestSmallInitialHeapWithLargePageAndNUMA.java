/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

package gc.arguments;

/*
 * @test TestSmallInitialHeapWithLargePageAndNUMA
 * @bug 8023905
 * @requires os.family == "linux"
 * @requires vm.gc.Parallel
 * @summary Check large pages and NUMA are working together via the output message.
 * @library /test/lib
 * @library /
 * @modules java.base/jdk.internal.misc
 * @modules java.management/sun.management
 * @build TestSmallInitialHeapWithLargePageAndNUMA
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UseHugeTLBFS -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI gc.arguments.TestSmallInitialHeapWithLargePageAndNUMA
*/

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import sun.hotspot.WhiteBox;
import jtreg.SkippedException;

public class TestSmallInitialHeapWithLargePageAndNUMA {

  private static final String MSG_EXIT_TOO_SMALL_HEAP = "Failed initializing NUMA with large pages. Too small heap size";
  private static final String MSG_GC_TRIGGERED_BEFORE_INIT = "GC triggered before VM initialization completed.";

  public static void main(String[] args) throws Exception {

    WhiteBox wb = WhiteBox.getWhiteBox();
    long heapAlignment = wb.getHeapAlignment();

    // When using large pages, Linux does not support freeing parts of reserved and committed memory.
    // And current Linux implementation uses page size as a condition to actually freeing memory.
    // If we allocate pages less than NUMA node, NUMA will try to use default page size and
    // this will free the memory which Linux does not support.
    // Assume the minimum NUMA node as 2.
    long initHeap = heapAlignment;
    long maxHeap = heapAlignment * 2;

    ProcessBuilder pb_enabled = GCArguments.createJavaProcessBuilder(
        "-XX:+UseParallelGC",
        "-Xms" + String.valueOf(initHeap),
        "-Xmx" + String.valueOf(maxHeap),
        "-XX:+UseNUMA",
        "-XX:+UseHugeTLBFS",
        "-XX:+PrintFlagsFinal",
        "-version");
    OutputAnalyzer analyzer = new OutputAnalyzer(pb_enabled.start());

    if (largePageOrNumaEnabled(analyzer)) {
      // We reach here, if both NUMA and HugeTLB are supported.
      // However final flags will not be printed as NUMA initialization will be failed.
      checkAnalyzerValues(analyzer, 1, MSG_EXIT_TOO_SMALL_HEAP);
    } else {
      throw new SkippedException("either NUMA or HugeTLB is not supported");
    }
  }

  // If both NUMA and large pages are enabled, VM will exit during NUMA initialization
  // under the small heap configuration. So final flags will not be printed.
  private static boolean largePageOrNumaEnabled(OutputAnalyzer analyzer) {
    String output = analyzer.getOutput();

    return !output.contains("[Global flags]");
  }

  // We need to test with small heap but fastdebug binary fails to initialize because of the small heap.
  // So skip that case.
  private static void checkAnalyzerValues(OutputAnalyzer analyzer, int expectedExitValue, String expectedMessage) {
    String output = analyzer.getOutput();

    // If the VM exits because of the small heap, skip checking the exit value.
    if (!output.contains(MSG_GC_TRIGGERED_BEFORE_INIT)) {
      analyzer.shouldHaveExitValue(expectedExitValue);
    }
    if (expectedMessage != null) {
      analyzer.shouldContain(expectedMessage);
    }
  }
}
