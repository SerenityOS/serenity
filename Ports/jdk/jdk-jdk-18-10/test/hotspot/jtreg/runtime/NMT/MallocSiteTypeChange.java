/*
 * Copyright (c) 2019, Red Hat, Inc. All rights reserved.
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
 * @test MallocSiteTypeChange
 * @bug 8200109
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -XX:NativeMemoryTracking=detail MallocSiteTypeChange
 */

import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import sun.hotspot.WhiteBox;

public class MallocSiteTypeChange {
    public static void main(String args[]) throws Exception {
        OutputAnalyzer output;
        WhiteBox wb = WhiteBox.getWhiteBox();

        // Grab my own PID
        String pid = Long.toString(ProcessTools.getProcessId());
        ProcessBuilder pb = new ProcessBuilder();

        int pc = 1;
        long addr = wb.NMTMallocWithPseudoStack(4 * 1024, pc);

        // Verify that current tracking level is "detail"
        pb.command(new String[] { JDKToolFinder.getJDKTool("jcmd"), pid, "VM.native_memory", "detail"});
        output = new OutputAnalyzer(pb.start());
        output.shouldContain("Test (reserved=4KB, committed=4KB)");

        pb.command(new String[] { JDKToolFinder.getJDKTool("jcmd"), pid, "VM.native_memory", "baseline"});
        output = new OutputAnalyzer(pb.start());
        output.shouldContain("Baseline succeeded");

        wb.NMTFree(addr);
        addr = wb.NMTMallocWithPseudoStackAndType(2 * 1024, pc, 9 /* mtInternal */ );
        pb.command(new String[] { JDKToolFinder.getJDKTool("jcmd"), pid, "VM.native_memory", "detail.diff"});
        output = new OutputAnalyzer(pb.start());
        output.shouldContain("(malloc=0KB type=Test -4KB)");
        output.shouldContain("(malloc=2KB type=Internal +2KB #1 +1)");
        output.shouldHaveExitValue(0);
  }
}
