/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @test TestG1ConcRefinementThreads
 * @bug 8047976
 * @requires vm.gc.G1
 * @summary Tests argument processing for G1ConcRefinementThreads
 * @library /test/lib
 * @library /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver gc.arguments.TestG1ConcRefinementThreads
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import java.util.*;
import java.util.regex.*;

public class TestG1ConcRefinementThreads {

  static final int AUTO_SELECT_THREADS_COUNT = -1;
  static final int PASSED_THREADS_COUNT = 11;

  public static void main(String args[]) throws Exception {
    // default case
    runG1ConcRefinementThreadsTest(
        new String[]{}, // automatically selected
        AUTO_SELECT_THREADS_COUNT /* use default setting */);

    // zero setting case
    runG1ConcRefinementThreadsTest(
        new String[]{"-XX:G1ConcRefinementThreads=0"},
        0);

    // non-zero sestting case
    runG1ConcRefinementThreadsTest(
        new String[]{"-XX:G1ConcRefinementThreads="+Integer.toString(PASSED_THREADS_COUNT)},
        PASSED_THREADS_COUNT);
  }

  private static void runG1ConcRefinementThreadsTest(String[] passedOpts,
          int expectedValue) throws Exception {
    List<String> vmOpts = new ArrayList<>();
    if (passedOpts.length > 0) {
      Collections.addAll(vmOpts, passedOpts);
    }
    Collections.addAll(vmOpts, "-XX:+UseG1GC", "-XX:+PrintFlagsFinal", "-version");

    ProcessBuilder pb = GCArguments.createJavaProcessBuilder(vmOpts);
    OutputAnalyzer output = new OutputAnalyzer(pb.start());

    output.shouldHaveExitValue(0);
    String stdout = output.getStdout();
    checkG1ConcRefinementThreadsConsistency(stdout, expectedValue);
  }

  private static void checkG1ConcRefinementThreadsConsistency(String output, int expectedValue) {
    int actualValue = getIntValue("G1ConcRefinementThreads", output);

    if (expectedValue == AUTO_SELECT_THREADS_COUNT) {
      // If expectedValue is automatically selected, set it same as ParallelGCThreads.
      expectedValue = getIntValue("ParallelGCThreads", output);
    }

    if (expectedValue != actualValue) {
      throw new RuntimeException(
            "Actual G1ConcRefinementThreads(" + Integer.toString(actualValue)
            + ") is not equal to expected value(" + Integer.toString(expectedValue) + ")");
    }
  }

  public static int getIntValue(String flag, String where) {
    Matcher m = Pattern.compile(flag + "\\s+:?=\\s+\\d+").matcher(where);
    if (!m.find()) {
      throw new RuntimeException("Could not find value for flag " + flag + " in output string");
    }
    String match = m.group();
    return Integer.parseInt(match.substring(match.lastIndexOf(" ") + 1, match.length()));
  }
}
