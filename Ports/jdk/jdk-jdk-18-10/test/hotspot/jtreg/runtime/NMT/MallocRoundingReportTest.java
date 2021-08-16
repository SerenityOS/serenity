/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test consistency of NMT by creating allocations of the Test type with various sizes and verifying visibility with jcmd
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -XX:NativeMemoryTracking=detail MallocRoundingReportTest
 *
 */

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.JDKToolFinder;

import sun.hotspot.WhiteBox;

public class MallocRoundingReportTest {
    private static long K = 1024;

    public static void main(String args[]) throws Exception {
        OutputAnalyzer output;
        WhiteBox wb = WhiteBox.getWhiteBox();

        // Grab my own PID
        String pid = Long.toString(ProcessTools.getProcessId());
        ProcessBuilder pb = new ProcessBuilder();

        long[] additionalBytes = {0, 1, 512, 650};
        long[] kByteSize = {1024, 2048};
        long mallocd_total = 0;
        for ( int i = 0; i < kByteSize.length; i++)
        {
            for (int j = 0; j < (additionalBytes.length); j++) {
                long curKB = kByteSize[i] + additionalBytes[j];
                // round up/down to the nearest KB to match NMT reporting
                long numKB = (curKB % kByteSize[i] >= 512) ? ((curKB / K) + 1) : curKB / K;
                // Use WB API to alloc and free with the mtTest type
                mallocd_total = wb.NMTMalloc(curKB);
                // Run 'jcmd <pid> VM.native_memory summary', check for expected output
                // NMT does not track memory allocations less than 1KB, and rounds to the nearest KB
                String expectedOut = ("Test (reserved=" + numKB + "KB, committed=" + numKB + "KB)");

                pb.command(new String[] { JDKToolFinder.getJDKTool("jcmd"), pid, "VM.native_memory", "summary" });
                output = new OutputAnalyzer(pb.start());
                output.shouldContain(expectedOut);

                wb.NMTFree(mallocd_total);
                // Run 'jcmd <pid> VM.native_memory summary', check for expected output
                pb.command(new String[] { JDKToolFinder.getJDKTool("jcmd"), pid, "VM.native_memory", "summary" });
                output = new OutputAnalyzer(pb.start());
                output.shouldNotContain("Test (reserved=");
            }
        }
    }
}
