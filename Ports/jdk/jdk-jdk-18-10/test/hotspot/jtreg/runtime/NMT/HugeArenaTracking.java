/*
 * Copyright (c) 2019, 2020, Red Hat, Inc. All rights reserved.
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

/*
 * @test
 * @key randomness
 * @library /test/lib
 * @requires vm.bits == 64
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -XX:NativeMemoryTracking=detail HugeArenaTracking
 */

import java.util.Random;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.Utils;
import sun.hotspot.WhiteBox;

public class HugeArenaTracking {
  private static final long GB = 1024 * 1024 * 1024;

  public static void main(String args[]) throws Exception {
    OutputAnalyzer output;
    final WhiteBox wb = WhiteBox.getWhiteBox();

    // Grab my own PID
    String pid = Long.toString(ProcessTools.getProcessId());
    ProcessBuilder pb = new ProcessBuilder();

    long arena1 = wb.NMTNewArena(1024);
    long arena2 = wb.NMTNewArena(1024);

    // Run 'jcmd <pid> VM.native_memory summary'
    pb.command(new String[] { JDKToolFinder.getJDKTool("jcmd"), pid, "VM.native_memory", "summary"});
    output = new OutputAnalyzer(pb.start());
    output.shouldContain("Test (reserved=2KB, committed=2KB)");

    Random rand = Utils.getRandomInstance();

    // Allocate 2GB+ from arena
    long total = 0;
    while (total < 2 * GB) {
      // Cap to 10M
      long inc = rand.nextInt(10 * 1024 * 1024);
      wb.NMTArenaMalloc(arena1, inc);
      total += inc;
    }

    ProcessBuilder pb2 = new ProcessBuilder();
    // Run 'jcmd <pid> VM.native_memory summary'
    pb2.command(new String[] { JDKToolFinder.getJDKTool("jcmd"), pid, "VM.native_memory", "summary", "scale=GB"});
    output = new OutputAnalyzer(pb2.start());
    output.shouldContain("Test (reserved=2GB, committed=2GB)");

    wb.NMTFreeArena(arena1);

    output = new OutputAnalyzer(pb.start());
    output.shouldContain("Test (reserved=1KB, committed=1KB)");
    wb.NMTFreeArena(arena2);

    output = new OutputAnalyzer(pb.start());
    output.shouldNotContain("Test (reserved");
  }
}
