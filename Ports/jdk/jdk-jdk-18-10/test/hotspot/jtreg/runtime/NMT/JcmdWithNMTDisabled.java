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

/*
 * @test
 * @summary Verify that jcmd correctly reports that NMT is not enabled
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver JcmdWithNMTDisabled 1
 */

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.JDKToolFinder;

public class JcmdWithNMTDisabled {
  static ProcessBuilder pb = new ProcessBuilder();
  static String pid;

  public static void main(String args[]) throws Exception {

    // This test explicitly needs to be run with the exact command lines below, not passing on
    // arguments from the parent VM is a conscious choice to avoid NMT being turned on.
    if (args.length > 0) {
      ProcessBuilder pb;
      OutputAnalyzer output;
      String testjdkPath = System.getProperty("test.jdk");

      // First run without enabling NMT
      pb = ProcessTools.createJavaProcessBuilder("-Dtest.jdk=" + testjdkPath, "JcmdWithNMTDisabled");
      output = new OutputAnalyzer(pb.start());
      output.shouldHaveExitValue(0);

      // Then run with explicitly disabling NMT, should not be any difference
      pb = ProcessTools.createJavaProcessBuilder("-Dtest.jdk=" + testjdkPath, "-XX:NativeMemoryTracking=off", "JcmdWithNMTDisabled");
      output = new OutputAnalyzer(pb.start());
      output.shouldHaveExitValue(0);

      return;
    }

    // Grab my own PID
    pid = Long.toString(ProcessTools.getProcessId());

    jcmdCommand("summary");
    jcmdCommand("detail");
    jcmdCommand("baseline");
    jcmdCommand("summary.diff");
    jcmdCommand("detail.diff");
    jcmdCommand("scale=GB");
    jcmdCommand("shutdown");
  }

  // Helper method for invoking different jcmd calls, all should fail with the same message saying NMT is not enabled
  public static void jcmdCommand(String command) throws Exception {

    pb.command(new String[] { JDKToolFinder.getJDKTool("jcmd"), pid, "VM.native_memory", command});
    OutputAnalyzer output = new OutputAnalyzer(pb.start());

    // Verify that jcmd reports that NMT is not enabled
    output.shouldContain("Native memory tracking is not enabled");
  }
}
