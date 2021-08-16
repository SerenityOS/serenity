/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test TestParallelGCThreads
 * @bug 8059527 8081382
 * @summary Tests argument processing for ParallelGCThreads
 * @library /test/lib
 * @library /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI gc.arguments.TestParallelGCThreads
 */

import java.util.ArrayList;
import java.util.List;
import jdk.test.lib.Asserts;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jtreg.SkippedException;
import sun.hotspot.gc.GC;

public class TestParallelGCThreads {

  public static void main(String args[]) throws Exception {
    testFlags();
    testDefaultValue();
  }

  private static final String flagName = "ParallelGCThreads";

  // uint ParallelGCThreads = 23 {product}
  private static final String printFlagsFinalPattern = " *uint *" + flagName + " *:?= *(\\d+) *\\{product\\} *";

  public static void testDefaultValue()  throws Exception {
    ProcessBuilder pb = GCArguments.createJavaProcessBuilder(
      "-XX:+UnlockExperimentalVMOptions", "-XX:+PrintFlagsFinal", "-version");

    OutputAnalyzer output = new OutputAnalyzer(pb.start());
    String value = output.firstMatch(printFlagsFinalPattern, 1);

    try {
      Asserts.assertNotNull(value, "Couldn't find uint flag " + flagName);

      Long longValue = new Long(value);

      // Sanity check that we got a non-zero value.
      Asserts.assertNotEquals(longValue, "0");

      output.shouldHaveExitValue(0);
    } catch (Exception e) {
      System.err.println(output.getOutput());
      throw e;
    }
  }

  public static void testFlags() throws Exception {
    // For each parallel collector (G1, Parallel)
    List<String> supportedGC = new ArrayList<String>();

    if (GC.G1.isSupported()) {
      supportedGC.add("G1");
    }
    if (GC.Parallel.isSupported()) {
      supportedGC.add("Parallel");
    }

    if (supportedGC.isEmpty()) {
      throw new SkippedException("Skipping test because none of G1/Parallel is supported.");
    }

    for (String gc : supportedGC) {
      // Make sure the VM does not allow ParallelGCThreads set to 0
      ProcessBuilder pb = GCArguments.createJavaProcessBuilder(
          "-XX:+Use" + gc + "GC",
          "-XX:ParallelGCThreads=0",
          "-XX:+PrintFlagsFinal",
          "-version");
      OutputAnalyzer output = new OutputAnalyzer(pb.start());
      output.shouldHaveExitValue(1);

      // Do some basic testing to ensure the flag updates the count
      for (long i = 1; i <= 3; i++) {
        long count = getParallelGCThreadCount(
            "-XX:+Use" + gc + "GC",
            "-XX:ParallelGCThreads=" + i,
            "-XX:+PrintFlagsFinal",
            "-version");
        Asserts.assertEQ(count, i, "Specifying ParallelGCThreads=" + i + " for " + gc + "GC does not set the thread count properly!");
      }
    }

    // 4294967295 == (unsigned int) -1
    // So setting ParallelGCThreads=4294967295 should give back 4294967295
    // and setting ParallelGCThreads=4294967296 should give back 0. (SerialGC is ok with ParallelGCThreads=0)
    for (long i = 4294967295L; i <= 4294967296L; i++) {
      long count = getParallelGCThreadCount(
          "-XX:+UseSerialGC",
          "-XX:ParallelGCThreads=" + i,
          "-XX:+PrintFlagsFinal",
          "-version");
      Asserts.assertEQ(count, i % 4294967296L, "Specifying ParallelGCThreads=" + i + " does not set the thread count properly!");
    }
  }

  public static long getParallelGCThreadCount(String... flags) throws Exception {
    ProcessBuilder pb = GCArguments.createJavaProcessBuilder(flags);
    OutputAnalyzer output = new OutputAnalyzer(pb.start());
    output.shouldHaveExitValue(0);
    String stdout = output.getStdout();
    return FlagsValue.getFlagLongValue("ParallelGCThreads", stdout);
  }
}
