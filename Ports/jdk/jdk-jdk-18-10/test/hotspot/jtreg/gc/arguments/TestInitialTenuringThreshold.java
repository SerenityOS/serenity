/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @test TestInitialTenuringThreshold
 * @bug 8014765
 * @requires vm.gc.Parallel
 * @summary Tests argument processing for initial tenuring threshold
 * @library /test/lib
 * @library /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver gc.arguments.TestInitialTenuringThreshold
 * @author thomas.schatzl@oracle.com
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestInitialTenuringThreshold {

  public static void runWithThresholds(int initial, int max, boolean shouldfail) throws Exception {
    ProcessBuilder pb = GCArguments.createJavaProcessBuilder(
      "-XX:+UseParallelGC",
      "-XX:InitialTenuringThreshold=" + String.valueOf(initial),
      "-XX:MaxTenuringThreshold=" + String.valueOf(max),
      "-version"
      );

    OutputAnalyzer output = new OutputAnalyzer(pb.start());
    if (shouldfail) {
      output.shouldHaveExitValue(1);
    } else {
      output.shouldHaveExitValue(0);
    }
  }


  public static void main(String args[]) throws Exception {
    ProcessBuilder pb = GCArguments.createJavaProcessBuilder(
      // some value below the default value of InitialTenuringThreshold of 7
      "-XX:MaxTenuringThreshold=1",
      "-version"
      );

    OutputAnalyzer output = new OutputAnalyzer(pb.start());
    output.shouldHaveExitValue(0);
    // successful tests
    runWithThresholds(0, 10, false);
    runWithThresholds(5, 5, false);
    runWithThresholds(8, 16, false);
    // failing tests
    runWithThresholds(10, 0, true);
    runWithThresholds(9, 8, true);
    runWithThresholds(-1, 8, true);
    runWithThresholds(0, -1, true);
    runWithThresholds(8, -1, true);
    runWithThresholds(16, 8, true);
    runWithThresholds(8, 17, true);
  }
}
