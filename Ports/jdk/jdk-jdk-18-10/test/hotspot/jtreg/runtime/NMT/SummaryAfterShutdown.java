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
 * @summary Verify that jcmd correctly reports that NMT is not enabled after a shutdown
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run main/othervm -XX:NativeMemoryTracking=detail SummaryAfterShutdown
 */

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.JDKToolFinder;

public class SummaryAfterShutdown {

  public static void main(String args[]) throws Exception {
    OutputAnalyzer output;
    // Grab my own PID
    String pid = Long.toString(ProcessTools.getProcessId());
    ProcessBuilder pb = new ProcessBuilder();

    // Run 'jcmd <pid> VM.native_memory shutdown'
    pb.command(new String[] { JDKToolFinder.getJDKTool("jcmd"), pid, "VM.native_memory", "shutdown"});
    output = new OutputAnalyzer(pb.start());

    // Verify that jcmd reports that NMT is shutting down
    output.shouldContain("Native memory tracking has been turned off");

    // Run 'jcmd <pid> VM.native_memory summary'
    pb.command(new String[] { JDKToolFinder.getJDKTool("jcmd"), pid, "VM.native_memory", "summary"});
    output = new OutputAnalyzer(pb.start());

    // Verify that jcmd reports that NMT has been shutdown
    output.shouldContain("Native memory tracking has been shutdown");
  }
}
