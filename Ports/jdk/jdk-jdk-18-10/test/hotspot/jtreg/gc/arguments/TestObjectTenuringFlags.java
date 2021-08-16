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
 * @test TestObjectTenuringFlags
 * @bug 6521376
 * @requires vm.gc.Parallel
 * @summary Tests argument processing for NeverTenure, AlwaysTenure,
 * and MaxTenuringThreshold
 * @library /test/lib
 * @library /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver gc.arguments.TestObjectTenuringFlags
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

import java.util.*;

public class TestObjectTenuringFlags {
  public static void main(String args[]) throws Exception {
    // default case
    runTenuringFlagsConsistencyTest(
        new String[]{},
        false /* shouldFail */,
        new ExpectedTenuringFlags(false /* alwaysTenure */, false /* neverTenure */, 7, 15));

    // valid cases
    runTenuringFlagsConsistencyTest(
        new String[]{"-XX:+NeverTenure"},
        false /* shouldFail */,
        new ExpectedTenuringFlags(false /* alwaysTenure */, true /* neverTenure */, 7, 16));

    runTenuringFlagsConsistencyTest(
        new String[]{"-XX:+AlwaysTenure"},
        false /* shouldFail */,
        new ExpectedTenuringFlags(true /* alwaysTenure */, false /* neverTenure */, 0, 0));

    runTenuringFlagsConsistencyTest(
        new String[]{"-XX:MaxTenuringThreshold=0"},
        false /* shouldFail */,
        new ExpectedTenuringFlags(true /* alwaysTenure */, false /* neverTenure */, 0, 0));

    runTenuringFlagsConsistencyTest(
        new String[]{"-XX:MaxTenuringThreshold=5"},
        false /* shouldFail */,
        new ExpectedTenuringFlags(false /* alwaysTenure */, false /* neverTenure */, 5, 5));

    runTenuringFlagsConsistencyTest(
        new String[]{"-XX:MaxTenuringThreshold=10"},
        false /* shouldFail */,
        new ExpectedTenuringFlags(false /* alwaysTenure */, false /* neverTenure */, 7, 10));

    runTenuringFlagsConsistencyTest(
        new String[]{"-XX:MaxTenuringThreshold=15"},
        false /* shouldFail */,
        new ExpectedTenuringFlags(false /* alwaysTenure */, false /* neverTenure */, 7, 15));

    runTenuringFlagsConsistencyTest(
        new String[]{"-XX:MaxTenuringThreshold=16"},
        false /* shouldFail */,
        new ExpectedTenuringFlags(false /* alwaysTenure */, false /* neverTenure */, 7, 16));

    runTenuringFlagsConsistencyTest(
        new String[]{"-XX:InitialTenuringThreshold=0"},
        false /* shouldFail */,
        new ExpectedTenuringFlags(false /* alwaysTenure */, false /* neverTenure */, 0, 15));

    runTenuringFlagsConsistencyTest(
        new String[]{"-XX:InitialTenuringThreshold=5"},
        false /* shouldFail */,
        new ExpectedTenuringFlags(false /* alwaysTenure */, false /* neverTenure */, 5, 15));

    runTenuringFlagsConsistencyTest(
        new String[]{"-XX:InitialTenuringThreshold=10"},
        false /* shouldFail */,
        new ExpectedTenuringFlags(false /* alwaysTenure */, false /* neverTenure */, 10, 15));

    runTenuringFlagsConsistencyTest(
        new String[]{"-XX:InitialTenuringThreshold=15"},
        false /* shouldFail */,
        new ExpectedTenuringFlags(false /* alwaysTenure */, false /* neverTenure */, 15, 15));

    // "Last option wins" cases
    runTenuringFlagsConsistencyTest(
        new String[]{"-XX:+AlwaysTenure", "-XX:+NeverTenure"},
        false /* shouldFail */,
        new ExpectedTenuringFlags(false /* alwaysTenure */, true /* neverTenure */, 7, 16));

    runTenuringFlagsConsistencyTest(
        new String[]{"-XX:+NeverTenure", "-XX:+AlwaysTenure"},
        false /* shouldFail */,
        new ExpectedTenuringFlags(true /* alwaysTenure */, false /* neverTenure */, 0, 0));

    runTenuringFlagsConsistencyTest(
        new String[]{"-XX:MaxTenuringThreshold=16", "-XX:+AlwaysTenure"},
        false /* shouldFail */,
        new ExpectedTenuringFlags(true /* alwaysTenure */, false /* neverTenure */, 0, 0));

    runTenuringFlagsConsistencyTest(
        new String[]{"-XX:+AlwaysTenure", "-XX:MaxTenuringThreshold=16"},
        false /* shouldFail */,
        new ExpectedTenuringFlags(false /* alwaysTenure */, false /* neverTenure */, 7, 16));

    runTenuringFlagsConsistencyTest(
        new String[]{"-XX:MaxTenuringThreshold=0", "-XX:+NeverTenure"},
        false /* shouldFail */,
        new ExpectedTenuringFlags(false /* alwaysTenure */, true /* neverTenure */, 7, 16));

    runTenuringFlagsConsistencyTest(
        new String[]{"-XX:+NeverTenure", "-XX:MaxTenuringThreshold=0"},
        false /* shouldFail */,
        new ExpectedTenuringFlags(true /* alwaysTenure */, false /* neverTenure */, 0, 0));

    // Illegal cases
    runTenuringFlagsConsistencyTest(
        new String[]{"-XX:MaxTenuringThreshold=17"},
        true /* shouldFail */,
        new ExpectedTenuringFlags());

    runTenuringFlagsConsistencyTest(
        new String[]{"-XX:InitialTenuringThreshold=16"},
        true /* shouldFail */,
        new ExpectedTenuringFlags());

    runTenuringFlagsConsistencyTest(
        new String[]{"-XX:InitialTenuringThreshold=17"},
        true /* shouldFail */,
        new ExpectedTenuringFlags());
  }

  private static void runTenuringFlagsConsistencyTest(String[] tenuringFlags,
          boolean shouldFail,
          ExpectedTenuringFlags expectedFlags) throws Exception {
    List<String> vmOpts = new ArrayList<>();
    if (tenuringFlags.length > 0) {
      Collections.addAll(vmOpts, tenuringFlags);
    }
    Collections.addAll(vmOpts, "-XX:+UseParallelGC", "-XX:+PrintFlagsFinal", "-version");

    ProcessBuilder pb = GCArguments.createJavaProcessBuilder(vmOpts);
    OutputAnalyzer output = new OutputAnalyzer(pb.start());

    if (shouldFail) {
      output.shouldHaveExitValue(1);
    } else {
      output.shouldHaveExitValue(0);
      String stdout = output.getStdout();
      checkTenuringFlagsConsistency(stdout, expectedFlags);
    }
  }

  private static void checkTenuringFlagsConsistency(String output, ExpectedTenuringFlags expectedFlags) {
    if (expectedFlags.alwaysTenure != FlagsValue.getFlagBoolValue("AlwaysTenure", output)) {
      throw new RuntimeException(
            "Actual flag AlwaysTenure " + FlagsValue.getFlagBoolValue("AlwaysTenure", output) +
            " is not equal to expected flag AlwaysTenure " + expectedFlags.alwaysTenure);
    }

    if (expectedFlags.neverTenure != FlagsValue.getFlagBoolValue("NeverTenure", output)) {
      throw new RuntimeException(
            "Actual flag NeverTenure " + FlagsValue.getFlagBoolValue("NeverTenure", output) +
            " is not equal to expected flag NeverTenure " + expectedFlags.neverTenure);
    }

    if (expectedFlags.initialTenuringThreshold != FlagsValue.getFlagLongValue("InitialTenuringThreshold", output)) {
      throw new RuntimeException(
            "Actual flag InitialTenuringThreshold " + FlagsValue.getFlagLongValue("InitialTenuringThreshold", output) +
            " is not equal to expected flag InitialTenuringThreshold " + expectedFlags.initialTenuringThreshold);
    }

    if (expectedFlags.maxTenuringThreshold != FlagsValue.getFlagLongValue("MaxTenuringThreshold", output)) {
      throw new RuntimeException(
            "Actual flag MaxTenuringThreshold " + FlagsValue.getFlagLongValue("MaxTenuringThreshold", output) +
            " is not equal to expected flag MaxTenuringThreshold " + expectedFlags.maxTenuringThreshold);
    }
  }
}

class ExpectedTenuringFlags {
    public boolean alwaysTenure;
    public boolean neverTenure;
    public long initialTenuringThreshold;
    public long maxTenuringThreshold;

    public ExpectedTenuringFlags(boolean alwaysTenure,
            boolean neverTenure,
            long initialTenuringThreshold,
            long maxTenuringThreshold) {
      this.alwaysTenure = alwaysTenure;
      this.neverTenure = neverTenure;
      this.initialTenuringThreshold = initialTenuringThreshold;
      this.maxTenuringThreshold = maxTenuringThreshold;
    }
    public ExpectedTenuringFlags() {}
}
