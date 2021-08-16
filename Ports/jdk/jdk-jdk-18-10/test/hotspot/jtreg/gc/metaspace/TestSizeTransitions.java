/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, Twitter, Inc.
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
 *
 */

package gc.metaspace;

import jdk.test.lib.Platform;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import java.util.ArrayList;
import java.util.List;

/* @test TestSizeTransitionsSerial
 * @requires vm.gc.Serial
 * @summary Tests that the metaspace size transition logging is done correctly.
 * @library /test/lib
 * @run driver gc.metaspace.TestSizeTransitions false -XX:+UseSerialGC
 * @run driver gc.metaspace.TestSizeTransitions true  -XX:+UseSerialGC
 */

/* @test TestSizeTransitionsParallel
 * @requires vm.gc.Parallel
 * @summary Tests that the metaspace size transition logging is done correctly.
 * @library /test/lib
 * @run driver gc.metaspace.TestSizeTransitions false -XX:+UseParallelGC
 * @run driver gc.metaspace.TestSizeTransitions true  -XX:+UseParallelGC
 */

/* @test TestSizeTransitionsG1
 * @requires vm.gc.G1
 * @summary Tests that the metaspace size transition logging is done correctly.
 * @library /test/lib
 * @run driver gc.metaspace.TestSizeTransitions false -XX:+UseG1GC
 * @run driver gc.metaspace.TestSizeTransitions true  -XX:+UseG1GC
 */

public class TestSizeTransitions {
  public static class Run {
    public static void main(String... args) throws Exception {
      System.out.println("Run started.");

      // easiest way to generate a metaspace transition is to ask for a full GC
      System.gc();

      System.out.println("Run finished.");
    }
  }

  // matches the log tags
  //   e.g., [0.043s][info][gc]
  private static final String LOG_TAGS_REGEX = "(\\[.*\\])+ ";

  // matches a size transition
  //   e.g., 177K(4864K)->177K(4864K)
  private static final String SIZE_TRANSITION_REGEX = "\\d+K\\(\\d+K\\)->\\d+K\\(\\d+K\\)";

  // matches -coops metaspace size transitions
  private static final String NO_COMPRESSED_KLASS_POINTERS_REGEX =
    String.format("^%s.* Metaspace: %s$",
                  LOG_TAGS_REGEX,
                  SIZE_TRANSITION_REGEX);

  // matches +coops metaspace size transitions
  private static final String COMPRESSED_KLASS_POINTERS_REGEX =
    String.format("^%s.* Metaspace: %s NonClass: %s Class: %s$",
                  LOG_TAGS_REGEX,
                  SIZE_TRANSITION_REGEX,
                  SIZE_TRANSITION_REGEX,
                  SIZE_TRANSITION_REGEX);

  public static void main(String... args) throws Exception {
    // args: <use-coops> <gc-arg>
    if (args.length != 2) {
      throw new RuntimeException("wrong number of args: " + args.length);
    }

    final boolean hasCompressedKlassPointers = Platform.is64bit();
    final boolean useCompressedKlassPointers = Boolean.parseBoolean(args[0]);
    final String gcArg = args[1];

    if (!hasCompressedKlassPointers && useCompressedKlassPointers) {
       // No need to run this configuration.
       System.out.println("Skipping test.");
       return;
    }

    List<String> jvmArgs = new ArrayList<>();
    if (hasCompressedKlassPointers) {
      jvmArgs.add(useCompressedKlassPointers ? "-XX:+UseCompressedClassPointers" : "-XX:-UseCompressedClassPointers");
    }
    jvmArgs.add(gcArg);
    jvmArgs.add("-Xmx256m");
    jvmArgs.add("-Xlog:gc,gc+metaspace=info");
    jvmArgs.add(TestSizeTransitions.Run.class.getName());

    System.out.println("JVM args:");
    for (String a : jvmArgs) {
      System.out.println("  " + a);
    }

    final ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(jvmArgs);
    final OutputAnalyzer output = new OutputAnalyzer(pb.start());
    System.out.println(output.getStdout());
    output.shouldHaveExitValue(0);

    if (useCompressedKlassPointers) {
      output.stdoutShouldMatch(COMPRESSED_KLASS_POINTERS_REGEX);
      output.stdoutShouldNotMatch(NO_COMPRESSED_KLASS_POINTERS_REGEX);
    } else {
      output.stdoutShouldMatch(NO_COMPRESSED_KLASS_POINTERS_REGEX);
      output.stdoutShouldNotMatch(COMPRESSED_KLASS_POINTERS_REGEX);
    }
  }
}
