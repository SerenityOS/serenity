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
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -XX:NativeMemoryTracking=detail ThreadedVirtualAllocTestType
 */

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.JDKToolFinder;
import sun.hotspot.WhiteBox;

public class ThreadedVirtualAllocTestType {
  public static long addr;
  public static final WhiteBox wb = WhiteBox.getWhiteBox();
  public static final long commitSize = 128 * 1024;
  public static final long reserveSize = 512 * 1024;

  public static void main(String args[]) throws Exception {
    OutputAnalyzer output;

    String pid = Long.toString(ProcessTools.getProcessId());
    ProcessBuilder pb = new ProcessBuilder();

    Thread reserveThread = new Thread() {
      public void run() {
        addr = wb.NMTReserveMemory(reserveSize);
      }
    };
    reserveThread.start();
    reserveThread.join();

    pb.command(new String[] { JDKToolFinder.getJDKTool("jcmd"), pid, "VM.native_memory", "detail"});
    output = new OutputAnalyzer(pb.start());
    output.shouldContain("Test (reserved=512KB, committed=0KB)");
    output.shouldMatch("\\[0x[0]*" + Long.toHexString(addr) + " - 0x[0]*" + Long.toHexString(addr + reserveSize) + "\\] reserved 512KB for Test");

    Thread commitThread = new Thread() {
      public void run() {
        wb.NMTCommitMemory(addr, commitSize);
      }
    };
    commitThread.start();
    commitThread.join();

    output = new OutputAnalyzer(pb.start());
    output.shouldContain("Test (reserved=512KB, committed=128KB)");
    output.shouldMatch("\\[0x[0]*" + Long.toHexString(addr) + " - 0x[0]*" + Long.toHexString(addr + commitSize) + "\\] committed 128KB");

    Thread uncommitThread = new Thread() {
      public void run() {
        wb.NMTUncommitMemory(addr, commitSize);
      }
    };
    uncommitThread.start();
    uncommitThread.join();

    output = new OutputAnalyzer(pb.start());
    output.shouldContain("Test (reserved=512KB, committed=0KB)");
    output.shouldNotMatch("\\[0x[0]*" + Long.toHexString(addr) + " - 0x[0]*" + Long.toHexString(addr + commitSize) + "\\] committed");

    Thread releaseThread = new Thread() {
      public void run() {
        wb.NMTReleaseMemory(addr, reserveSize);
      }
    };
    releaseThread.start();
    releaseThread.join();

    output = new OutputAnalyzer(pb.start());
    output.shouldNotContain("Test (reserved=");
    output.shouldNotContain("\\[0x[0]*" + Long.toHexString(addr) + " - 0x[0]*" + Long.toHexString(addr + reserveSize) + "\\] reserved");
  }

}
