/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

package gc;

/* @test TestAllocateHeapAtMultiple.java
 * @summary Test to check allocation of Java Heap with AllocateHeapAt option. Has multiple sub-tests to cover different code paths.
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @requires vm.bits == "64" & vm.gc != "Z" & os.family != "aix"
 * @run driver/timeout=360 gc.TestAllocateHeapAtMultiple
 */

import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import java.util.ArrayList;
import java.util.Collections;

public class TestAllocateHeapAtMultiple {
  public static void main(String args[]) throws Exception {
    ArrayList<String> flags = new ArrayList<>();

    String test_dir = System.getProperty("test.dir", ".");

    // Extra flags for each of the sub-tests
    String[][] extraFlagsList = new String[][] {
      {"-Xmx32m", "-Xms32m", "-XX:+UseCompressedOops"},     // 1. With compressedoops enabled.
      {"-Xmx32m", "-Xms32m", "-XX:-UseCompressedOops"},     // 2. With compressedoops disabled.
      {"-Xmx32m", "-Xms32m", "-XX:HeapBaseMinAddress=3g"},  // 3. With user specified HeapBaseMinAddress.
      {"-Xmx32m", "-Xms32m", "-XX:+UseLargePages"},         // 4. Set UseLargePages.
      {"-Xmx32m", "-Xms32m", "-XX:+UseNUMA"}                // 5. Set UseNUMA.
    };

    for (String[] extraFlags : extraFlagsList) {
      flags.clear();
      // Add extra flags specific to the sub-test.
      Collections.addAll(flags, extraFlags);
      // Add common flags
      Collections.addAll(flags, new String[] {"-XX:AllocateHeapAt=" + test_dir,
                                              "-Xlog:gc+heap=info",
                                              "-version"});

      ProcessBuilder pb = ProcessTools.createTestJvm(flags);
      OutputAnalyzer output = new OutputAnalyzer(pb.start());

      System.out.println("Output:\n" + output.getOutput());

      output.shouldContain("Successfully allocated Java heap at location");
      output.shouldHaveExitValue(0);
    }
  }
}
