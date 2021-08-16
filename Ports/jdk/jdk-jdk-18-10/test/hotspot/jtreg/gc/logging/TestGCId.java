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

package gc.logging;

/*
 * @test TestGCId
 * @bug 8043607
 * @summary Ensure that the GCId is logged
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI gc.logging.TestGCId
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jtreg.SkippedException;
import sun.hotspot.gc.GC;

public class TestGCId {
  public static void main(String[] args) throws Exception {
    boolean noneGCSupported = true;

    if (GC.Parallel.isSupported()) {
      noneGCSupported = false;
      testGCId("UseParallelGC");
    }
    if (GC.G1.isSupported()) {
      noneGCSupported = false;
      testGCId("UseG1GC");
    }
    if (GC.Serial.isSupported()) {
      noneGCSupported = false;
      testGCId("UseSerialGC");
    }
    if (GC.Shenandoah.isSupported()) {
      noneGCSupported = false;
      testGCId("UseShenandoahGC");
    }

    if (noneGCSupported) {
      throw new SkippedException("Skipping test because none of Parallel/G1/Serial/Shenandoah is supported.");
    }
  }

  private static void verifyContainsGCIDs(OutputAnalyzer output) {
    output.shouldMatch("\\[.*\\]\\[.*\\]\\[.*\\] GC\\(0\\) ");
    output.shouldMatch("\\[.*\\]\\[.*\\]\\[.*\\] GC\\(1\\) ");
    output.shouldHaveExitValue(0);
  }

  private static void testGCId(String gcFlag) throws Exception {
    ProcessBuilder pb_default =
      ProcessTools.createJavaProcessBuilder("-XX:+UnlockExperimentalVMOptions", "-XX:+" + gcFlag, "-Xlog:gc", "-Xmx10M", GCTest.class.getName());
    verifyContainsGCIDs(new OutputAnalyzer(pb_default.start()));
  }

  static class GCTest {
    private static byte[] garbage;
    public static void main(String [] args) {
      System.out.println("Creating garbage");
      // create 128MB of garbage. This should result in at least one GC
      for (int i = 0; i < 1024; i++) {
        garbage = new byte[128 * 1024];
      }
      // do a system gc to get one more gc
      System.gc();
      System.out.println("Done");
    }
  }
}
