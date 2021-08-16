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

package gc.g1;

/*
 * @test TestRemsetLoggingThreads
 * @requires vm.gc.G1
 * @bug 8025441 8145534
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management/sun.management
 * @summary Ensure that various values of worker threads/concurrent
 * refinement threads do not crash the VM.
 * @run driver gc.g1.TestRemsetLoggingThreads
 */

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestRemsetLoggingThreads {

  private static void runTest(int refinementThreads, int workerThreads) throws Exception {
    ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-XX:+UseG1GC",
                                                              "-XX:+UnlockDiagnosticVMOptions",
                                                              "-Xlog:gc+remset+exit=trace",
                                                              "-XX:G1ConcRefinementThreads=" + refinementThreads,
                                                              "-XX:ParallelGCThreads=" + workerThreads,
                                                              "-version");

    OutputAnalyzer output = new OutputAnalyzer(pb.start());

    String pattern = "Concurrent refinement threads times \\(s\\)$";
    Matcher m = Pattern.compile(pattern, Pattern.MULTILINE).matcher(output.getStdout());

    if (!m.find()) {
      throw new Exception("Could not find correct output for concurrent RS threads times in stdout," +
        " should match the pattern \"" + pattern + "\", but stdout is \n" + output.getStdout());
    }
    output.shouldHaveExitValue(0);
  }

  public static void main(String[] args) throws Exception {
    // different valid combinations of number of refinement and gc worker threads
    runTest(1, 1);
    runTest(1, 5);
    runTest(5, 1);
    runTest(10, 10);
    runTest(1, 2);
    runTest(4, 3);
  }
}
