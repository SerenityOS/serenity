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

package gc.ergonomics;

/*
 * @test TestDynamicNumberOfGCThreads
 * @bug 8017462
 * @summary Ensure that UseDynamicNumberOfGCThreads runs
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI gc.ergonomics.TestDynamicNumberOfGCThreads
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jtreg.SkippedException;
import sun.hotspot.gc.GC;

public class TestDynamicNumberOfGCThreads {
  public static void main(String[] args) throws Exception {
    boolean noneGCSupported = true;

    if (GC.G1.isSupported()) {
      noneGCSupported = false;
      testDynamicNumberOfGCThreads("UseG1GC");
    }

    if (GC.Parallel.isSupported()) {
      noneGCSupported = false;
      testDynamicNumberOfGCThreads("UseParallelGC");
    }

    if (GC.Shenandoah.isSupported()) {
      noneGCSupported = false;
      testDynamicNumberOfGCThreads("UseShenandoahGC");
    }

    if (noneGCSupported) {
      throw new SkippedException("Skipping test because none of G1/Parallel/Shenandoah is supported.");
    }
  }

  private static void verifyDynamicNumberOfGCThreads(OutputAnalyzer output) {
    output.shouldHaveExitValue(0); // test should run succesfully
    output.shouldContain("new_active_workers");
  }

  private static void testDynamicNumberOfGCThreads(String gcFlag) throws Exception {
    // UseDynamicNumberOfGCThreads enabled
    String[] baseArgs = {"-XX:+UnlockExperimentalVMOptions", "-XX:+" + gcFlag, "-Xmx10M", "-XX:+UseDynamicNumberOfGCThreads", "-Xlog:gc+task=trace", GCTest.class.getName()};

    // Base test with gc and +UseDynamicNumberOfGCThreads:
    ProcessBuilder pb_enabled = ProcessTools.createJavaProcessBuilder(baseArgs);
    verifyDynamicNumberOfGCThreads(new OutputAnalyzer(pb_enabled.start()));

    // Turn on parallel reference processing
    String[] parRefProcArg = {"-XX:+ParallelRefProcEnabled", "-XX:-ShowMessageBoxOnError"};
    String[] parRefArgs = new String[baseArgs.length + parRefProcArg.length];
    System.arraycopy(parRefProcArg, 0, parRefArgs, 0,                parRefProcArg.length);
    System.arraycopy(baseArgs,  0, parRefArgs, parRefProcArg.length, baseArgs.length);
    pb_enabled = ProcessTools.createJavaProcessBuilder(parRefArgs);
    verifyDynamicNumberOfGCThreads(new OutputAnalyzer(pb_enabled.start()));
  }

  static class GCTest {
    private static byte[] garbage;
    public static void main(String [] args) {
      System.out.println("Creating garbage");
      // create 128MB of garbage. This should result in at least one GC
      for (int i = 0; i < 1024; i++) {
        garbage = new byte[128 * 1024];
      }
      System.out.println("Done");
    }
  }
}
