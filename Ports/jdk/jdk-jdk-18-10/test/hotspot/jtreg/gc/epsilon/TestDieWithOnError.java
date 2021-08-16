/*
 * Copyright (c) 2017, 2018, Red Hat, Inc. All rights reserved.
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

package gc.epsilon;

/**
 * @test TestDieWithOnError
 * @requires vm.gc.Epsilon
 * @summary Epsilon GC should die on heap exhaustion with error handler attached
 * @library /test/lib
 * @run driver gc.epsilon.TestDieWithOnError
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestDieWithOnError {

  static String ON_ERR_MSG = "Epsilon error handler message";

  public static void passWith(String... args) throws Exception {
    ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(args);
    OutputAnalyzer out = new OutputAnalyzer(pb.start());
    out.shouldNotContain("OutOfMemoryError");
    out.stdoutShouldNotMatch("^" + ON_ERR_MSG);
    out.shouldHaveExitValue(0);
  }

  public static void failWith(String... args) throws Exception {
    ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(args);
    OutputAnalyzer out = new OutputAnalyzer(pb.start());
    out.shouldContain("OutOfMemoryError");
    if (out.getExitValue() == 0) {
      throw new IllegalStateException("Should have failed with non-zero exit code");
    }
    out.stdoutShouldMatch("^" + ON_ERR_MSG);
  }

  public static void main(String[] args) throws Exception {
    passWith("-Xmx128m",
             "-XX:+UnlockExperimentalVMOptions",
             "-XX:+UseEpsilonGC",
             "-Dcount=1",
             "-XX:OnOutOfMemoryError=echo " + ON_ERR_MSG,
             TestDieWithOnError.Workload.class.getName());

    failWith("-Xmx128m",
             "-XX:+UnlockExperimentalVMOptions",
             "-XX:+UseEpsilonGC",
             "-XX:OnOutOfMemoryError=echo " + ON_ERR_MSG,
             TestDieWithOnError.Workload.class.getName());

    failWith("-Xmx128m",
             "-Xint",
             "-XX:+UnlockExperimentalVMOptions",
             "-XX:+UseEpsilonGC",
             "-XX:OnOutOfMemoryError=echo " + ON_ERR_MSG,
             TestDieWithOnError.Workload.class.getName());

    failWith("-Xmx128m",
             "-Xbatch",
             "-Xcomp",
             "-XX:+UnlockExperimentalVMOptions",
             "-XX:+UseEpsilonGC",
             "-XX:OnOutOfMemoryError=echo " + ON_ERR_MSG,
             TestDieWithOnError.Workload.class.getName());

    failWith("-Xmx128m",
             "-Xbatch",
             "-Xcomp",
             "-XX:TieredStopAtLevel=1",
             "-XX:+UnlockExperimentalVMOptions",
             "-XX:+UseEpsilonGC",
             "-XX:OnOutOfMemoryError=echo " + ON_ERR_MSG,
             TestDieWithOnError.Workload.class.getName());

    failWith("-Xmx128m",
             "-Xbatch",
             "-Xcomp",
             "-XX:-TieredCompilation",
             "-XX:+UnlockExperimentalVMOptions",
             "-XX:+UseEpsilonGC",
             "-XX:OnOutOfMemoryError=echo " + ON_ERR_MSG,
             TestDieWithOnError.Workload.class.getName());
  }

  public static class Workload {
    static int COUNT = Integer.getInteger("count", 1_000_000_000); // ~24 GB allocation

    static volatile Object sink;

    public static void main(String... args) {
      for (int c = 0; c < COUNT; c++) {
        sink = new Object();
      }
    }
  }

}
