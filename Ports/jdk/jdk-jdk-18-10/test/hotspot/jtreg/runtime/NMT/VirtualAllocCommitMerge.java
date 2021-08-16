/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test merging of committed virtual memory and that we track it correctly
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -XX:NativeMemoryTracking=detail VirtualAllocCommitMerge
 *
 */

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.JDKToolFinder;

import sun.hotspot.WhiteBox;

public class VirtualAllocCommitMerge {

    public static WhiteBox wb = WhiteBox.getWhiteBox();

    public static void main(String args[]) throws Exception {
        OutputAnalyzer output;
        long commitSize = 128 * 1024; // 128KB
        long reserveSize = 4 * 1024 * 1024; // 4096KB
        long addr;

        String pid = Long.toString(ProcessTools.getProcessId());
        ProcessBuilder pb = new ProcessBuilder();

        // reserve
        addr = wb.NMTReserveMemory(reserveSize);
        pb.command(new String[] { JDKToolFinder.getJDKTool("jcmd"), pid,
                "VM.native_memory", "detail" });

        output = new OutputAnalyzer(pb.start());
        checkReservedCommittedSummary(output, "4096KB", "0KB");
        checkReserved(output, addr, reserveSize, "4096KB");

        long addrA = addr + (0 * commitSize);
        long addrB = addr + (1 * commitSize);
        long addrC = addr + (2 * commitSize);
        long addrD = addr + (3 * commitSize);
        long addrE = addr + (4 * commitSize);

        {
            // commit overlapping ABC, A, B, C
            wb.NMTCommitMemory(addrA, 3 * commitSize);

            output = new OutputAnalyzer(pb.start());
            checkReservedCommittedSummary(output, "4096KB", "384KB");
            checkReserved(output, addr, reserveSize, "4096KB");

            checkCommitted(output, addrA, 3 * commitSize, "384KB");


            wb.NMTCommitMemory(addrA, commitSize);

            output = new OutputAnalyzer(pb.start());
            checkReservedCommittedSummary(output, "4096KB", "384KB");
            checkReserved(output, addr, reserveSize, "4096KB");

            checkCommitted(output, addrA, 3 * commitSize, "384KB");


            wb.NMTCommitMemory(addrB, commitSize);

            output = new OutputAnalyzer(pb.start());
            checkReservedCommittedSummary(output, "4096KB", "384KB");
            checkReserved(output, addr, reserveSize, "4096KB");

            checkCommitted(output, addrA, 3 * commitSize, "384KB");

            wb.NMTCommitMemory(addrC, commitSize);

            output = new OutputAnalyzer(pb.start());
            checkReservedCommittedSummary(output, "4096KB", "384KB");
            checkReserved(output, addr, reserveSize, "4096KB");

            checkCommitted(output, addrA, 3 * commitSize, "384KB");

            // uncommit
            wb.NMTUncommitMemory(addrA, 3 * commitSize);

            output = new OutputAnalyzer(pb.start());
            checkReservedCommittedSummary(output, "4096KB", "0KB");
        }

        // Test discontigous areas
        {
            // commit ACE
            wb.NMTCommitMemory(addrA, commitSize);
            wb.NMTCommitMemory(addrC, commitSize);
            wb.NMTCommitMemory(addrE, commitSize);

            output = new OutputAnalyzer(pb.start());
            checkReservedCommittedSummary(output, "4096KB", "384KB");
            checkReserved(output, addr, reserveSize, "4096KB");

            checkCommitted(output, addrA, commitSize, "128KB");
            checkCommitted(output, addrC, commitSize, "128KB");
            checkCommitted(output, addrE, commitSize, "128KB");

            // uncommit ACE
            wb.NMTUncommitMemory(addrA, commitSize);
            wb.NMTUncommitMemory(addrC, commitSize);
            wb.NMTUncommitMemory(addrE, commitSize);

            output = new OutputAnalyzer(pb.start());
            checkReservedCommittedSummary(output, "4096KB", "0KB");
        }

        // Test contiguous areas
        {
            // commit AB
            wb.NMTCommitMemory(addrA, commitSize);
            wb.NMTCommitMemory(addrB, commitSize);

            output = new OutputAnalyzer(pb.start());
            checkReservedCommittedSummary(output, "4096KB", "256KB");
            checkReserved(output, addr, reserveSize, "4096KB");

            checkCommitted(output, addrA, 2 * commitSize, "256KB");

            // uncommit AB
            wb.NMTUncommitMemory(addrA, commitSize);
            wb.NMTUncommitMemory(addrB, commitSize);

            output = new OutputAnalyzer(pb.start());
            checkReservedCommittedSummary(output, "4096KB", "0KB");
        }

        {
            // commit BA
            wb.NMTCommitMemory(addrB, commitSize);
            wb.NMTCommitMemory(addrA, commitSize);

            output = new OutputAnalyzer(pb.start());
            checkReservedCommittedSummary(output, "4096KB", "256KB");
            checkReserved(output, addr, reserveSize, "4096KB");

            checkCommitted(output, addrA, 2 * commitSize, "256KB");

            // uncommit AB
            wb.NMTUncommitMemory(addrB, commitSize);
            wb.NMTUncommitMemory(addrA, commitSize);

            output = new OutputAnalyzer(pb.start());
            checkReservedCommittedSummary(output, "4096KB", "0KB");
        }

        {
            // commit ABC
            wb.NMTCommitMemory(addrA, commitSize);
            wb.NMTCommitMemory(addrB, commitSize);
            wb.NMTCommitMemory(addrC, commitSize);

            output = new OutputAnalyzer(pb.start());
            checkReservedCommittedSummary(output, "4096KB", "384KB");
            checkReserved(output, addr, reserveSize, "4096KB");

            checkCommitted(output, addrA, 3 * commitSize, "384KB");

            // uncommit
            wb.NMTUncommitMemory(addrA, commitSize);
            wb.NMTUncommitMemory(addrB, commitSize);
            wb.NMTUncommitMemory(addrC, commitSize);

            output = new OutputAnalyzer(pb.start());
            checkReservedCommittedSummary(output, "4096KB", "0KB");
        }

        {
            // commit ACB
            wb.NMTCommitMemory(addrA, commitSize);
            wb.NMTCommitMemory(addrC, commitSize);
            wb.NMTCommitMemory(addrB, commitSize);

            output = new OutputAnalyzer(pb.start());
            checkReservedCommittedSummary(output, "4096KB", "384KB");
            checkReserved(output, addr, reserveSize, "4096KB");

            checkCommitted(output, addrA, 3 * commitSize, "384KB");

            // uncommit
            wb.NMTUncommitMemory(addrA, commitSize);
            wb.NMTUncommitMemory(addrC, commitSize);
            wb.NMTUncommitMemory(addrB, commitSize);

            output = new OutputAnalyzer(pb.start());
            checkReservedCommittedSummary(output, "4096KB", "0KB");
        }

        {
            // commit BAC
            wb.NMTCommitMemory(addrB, commitSize);
            wb.NMTCommitMemory(addrA, commitSize);
            wb.NMTCommitMemory(addrC, commitSize);

            output = new OutputAnalyzer(pb.start());
            checkReservedCommittedSummary(output, "4096KB", "384KB");
            checkReserved(output, addr, reserveSize, "4096KB");

            checkCommitted(output, addrA, 3 * commitSize, "384KB");

            // uncommit
            wb.NMTUncommitMemory(addrB, commitSize);
            wb.NMTUncommitMemory(addrA, commitSize);
            wb.NMTUncommitMemory(addrC, commitSize);

            output = new OutputAnalyzer(pb.start());
            checkReservedCommittedSummary(output, "4096KB", "0KB");
        }

        {
            // commit BCA
            wb.NMTCommitMemory(addrB, commitSize);
            wb.NMTCommitMemory(addrC, commitSize);
            wb.NMTCommitMemory(addrA, commitSize);

            output = new OutputAnalyzer(pb.start());
            checkReservedCommittedSummary(output, "4096KB", "384KB");
            checkReserved(output, addr, reserveSize, "4096KB");

            checkCommitted(output, addrA, 3 * commitSize, "384KB");

            // uncommit
            wb.NMTUncommitMemory(addrB, commitSize);
            wb.NMTUncommitMemory(addrC, commitSize);
            wb.NMTUncommitMemory(addrA, commitSize);

            output = new OutputAnalyzer(pb.start());
            checkReservedCommittedSummary(output, "4096KB", "0KB");
        }

        {
            // commit CAB
            wb.NMTCommitMemory(addrC, commitSize);
            wb.NMTCommitMemory(addrA, commitSize);
            wb.NMTCommitMemory(addrB, commitSize);

            output = new OutputAnalyzer(pb.start());
            checkReservedCommittedSummary(output, "4096KB", "384KB");
            checkReserved(output, addr, reserveSize, "4096KB");

            checkCommitted(output, addrA, 3 * commitSize, "384KB");

            // uncommit
            wb.NMTUncommitMemory(addrC, commitSize);
            wb.NMTUncommitMemory(addrA, commitSize);
            wb.NMTUncommitMemory(addrB, commitSize);

            output = new OutputAnalyzer(pb.start());
            checkReservedCommittedSummary(output, "4096KB", "0KB");
        }

        {
            // commit CBA
            wb.NMTCommitMemory(addrC, commitSize);
            wb.NMTCommitMemory(addrB, commitSize);
            wb.NMTCommitMemory(addrA, commitSize);

            output = new OutputAnalyzer(pb.start());
            checkReservedCommittedSummary(output, "4096KB", "384KB");
            checkReserved(output, addr, reserveSize, "4096KB");

            checkCommitted(output, addrA, 3 * commitSize, "384KB");

            // uncommit
            wb.NMTUncommitMemory(addrC, commitSize);
            wb.NMTUncommitMemory(addrB, commitSize);
            wb.NMTUncommitMemory(addrA, commitSize);

            output = new OutputAnalyzer(pb.start());
            checkReservedCommittedSummary(output, "4096KB", "0KB");
        }

        // release
        wb.NMTReleaseMemory(addr, reserveSize);
        output = new OutputAnalyzer(pb.start());
        output.shouldNotContain("Test (reserved=");
        output.shouldNotMatch("\\[0x[0]*" + Long.toHexString(addr) + " - 0x[0]*"
                + Long.toHexString(addr + reserveSize) + "\\] reserved 4096KB for Test");
    }

    public static void checkReservedCommittedSummary(OutputAnalyzer output, String reservedString, String committedString) {
        output.shouldContain("Test (reserved=" + reservedString + ", committed=" + committedString + ")");
    }

    public static void checkReserved(OutputAnalyzer output, long addr, long size, String sizeString) {
        output.shouldMatch("\\[0x[0]*" + Long.toHexString(addr) + " - 0x[0]*"
                           + Long.toHexString(addr + size)
                           + "\\] reserved 4096KB for Test");
    }

    public static void checkCommitted(OutputAnalyzer output, long addr, long size, String sizeString) {
        output.shouldMatch("\\[0x[0]*" + Long.toHexString(addr) + " - 0x[0]*"
                           + Long.toHexString(addr + size)
                           + "\\] committed " + sizeString + " from.*");
    }
}
