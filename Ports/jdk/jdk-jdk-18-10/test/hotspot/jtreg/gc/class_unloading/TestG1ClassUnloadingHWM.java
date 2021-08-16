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

package gc.class_unloading;

/*
 * @test
 * @bug 8049831
 * @requires vm.gc.G1
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run driver gc.class_unloading.TestG1ClassUnloadingHWM
 * @summary Test that -XX:-ClassUnloadingWithConcurrentMark will trigger a Full GC when more than MetaspaceSize metadata is allocated.
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import sun.hotspot.WhiteBox;

public class TestG1ClassUnloadingHWM {
  private static long MetaspaceSize = 32 * 1024 * 1024;
  private static long YoungGenSize  = 32 * 1024 * 1024;

  private static OutputAnalyzer run(boolean enableUnloading) throws Exception {
    ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
      "-Xbootclasspath/a:.",
      "-XX:+UnlockDiagnosticVMOptions",
      "-XX:+WhiteBoxAPI",
      "-XX:MetaspaceSize=" + MetaspaceSize,
      "-Xmn" + YoungGenSize,
      "-XX:+UseG1GC",
      "-XX:" + (enableUnloading ? "+" : "-") + "ClassUnloadingWithConcurrentMark",
      "-Xlog:gc",
      TestG1ClassUnloadingHWM.AllocateBeyondMetaspaceSize.class.getName(),
      "" + MetaspaceSize,
      "" + YoungGenSize);
    return new OutputAnalyzer(pb.start());
  }

  public static OutputAnalyzer runWithG1ClassUnloading() throws Exception {
    return run(true);
  }

  public static OutputAnalyzer runWithoutG1ClassUnloading() throws Exception {
    return run(false);
  }

  public static void testWithoutG1ClassUnloading() throws Exception {
    // -XX:-ClassUnloadingWithConcurrentMark is used, so we expect a full GC instead of a concurrent cycle.
    OutputAnalyzer out = runWithoutG1ClassUnloading();

    out.shouldMatch(".*Pause Full.*");
    out.shouldNotMatch(".*Pause Young \\(Concurrent Start\\).*");
  }

  public static void testWithG1ClassUnloading() throws Exception {
    // -XX:+ClassUnloadingWithConcurrentMark is used, so we expect a concurrent cycle instead of a full GC.
    OutputAnalyzer out = runWithG1ClassUnloading();

    out.shouldMatch(".*Pause Young \\(Concurrent Start\\).*");
    out.shouldNotMatch(".*Pause Full.*");
  }

  public static void main(String args[]) throws Exception {
    testWithG1ClassUnloading();
    testWithoutG1ClassUnloading();
  }

  public static class AllocateBeyondMetaspaceSize {
    public static Object dummy;

    public static void main(String [] args) throws Exception {
      if (args.length != 2) {
        throw new IllegalArgumentException("Usage: <MetaspaceSize> <YoungGenSize>");
      }

      WhiteBox wb = WhiteBox.getWhiteBox();

      // Allocate past the MetaspaceSize limit
      long metaspaceSize = Long.parseLong(args[0]);
      long allocationBeyondMetaspaceSize  = metaspaceSize * 2;

      // There is a cap on how large a single metaspace allocation can get. So we may have to allocate in blocks.
      final long max = wb.maxMetaspaceAllocationSize();
      while (allocationBeyondMetaspaceSize > 0) {
        long s = max < allocationBeyondMetaspaceSize ? max : allocationBeyondMetaspaceSize;
        wb.allocateMetaspace(null, s);
        allocationBeyondMetaspaceSize -= s;
      }

      long youngGenSize = Long.parseLong(args[1]);
      triggerYoungGCs(youngGenSize);

    }

    public static void triggerYoungGCs(long youngGenSize) {
      long approxAllocSize = 32 * 1024;
      long numAllocations  = 2 * youngGenSize / approxAllocSize;

      for (long i = 0; i < numAllocations; i++) {
        dummy = new byte[(int)approxAllocSize];
      }
    }
  }
}

