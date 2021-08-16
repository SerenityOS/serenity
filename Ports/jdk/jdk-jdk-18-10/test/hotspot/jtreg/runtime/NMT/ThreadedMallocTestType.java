/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -XX:NativeMemoryTracking=detail ThreadedMallocTestType
 */

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.JDKToolFinder;
import sun.hotspot.WhiteBox;

public class ThreadedMallocTestType {
  public static long memAlloc1;
  public static long memAlloc2;
  public static long memAlloc3;

  public static void main(String args[]) throws Exception {
    OutputAnalyzer output;
    final WhiteBox wb = WhiteBox.getWhiteBox();

    // Grab my own PID
    String pid = Long.toString(ProcessTools.getProcessId());
    ProcessBuilder pb = new ProcessBuilder();

    Thread allocThread = new Thread() {
      public void run() {
        // Alloc memory using the WB api
        memAlloc1 = wb.NMTMalloc(128 * 1024);
        memAlloc2 = wb.NMTMalloc(256 * 1024);
        memAlloc3 = wb.NMTMalloc(512 * 1024);
      }
    };

    allocThread.start();
    allocThread.join();

    // Run 'jcmd <pid> VM.native_memory summary'
    pb.command(new String[] { JDKToolFinder.getJDKTool("jcmd"), pid, "VM.native_memory", "summary"});
    output = new OutputAnalyzer(pb.start());
    output.shouldContain("Test (reserved=896KB, committed=896KB)");

    Thread freeThread = new Thread() {
      public void run() {
        // Free the memory allocated by NMTMalloc
        wb.NMTFree(memAlloc1);
        wb.NMTFree(memAlloc2);
        wb.NMTFree(memAlloc3);
      }
    };

    freeThread.start();
    freeThread.join();

    output = new OutputAnalyzer(pb.start());
    output.shouldNotContain("Test (reserved=");
  }
}
