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
 * @summary Test that os::attempt_reserve_memory_at doesn't register the memory as committed
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -XX:NativeMemoryTracking=detail VirtualAllocAttemptReserveMemoryAt
 *
 */

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.JDKToolFinder;

import sun.hotspot.WhiteBox;

import static jdk.test.lib.Asserts.*;

public class VirtualAllocAttemptReserveMemoryAt {

    public static WhiteBox wb = WhiteBox.getWhiteBox();

    public static void main(String args[]) throws Exception {
        long reserveSize = 4 * 1024 * 1024; // 4096KB

        String pid = Long.toString(ProcessTools.getProcessId());
        ProcessBuilder pb = new ProcessBuilder();

        // Find an address
        long addr = wb.NMTReserveMemory(reserveSize);

        // Release it
        wb.NMTReleaseMemory(addr, reserveSize);

        long attempt_addr = wb.NMTAttemptReserveMemoryAt(addr, reserveSize);

        if (attempt_addr == 0) {
            // We didn't manage ot get the requested memory address.
            // It's not necessarily a bug, so giving up.
            return;
        }

        assertEQ(addr, attempt_addr);

        pb.command(new String[] { JDKToolFinder.getJDKTool("jcmd"), pid,
                "VM.native_memory", "detail" });

        OutputAnalyzer output = new OutputAnalyzer(pb.start());

        output.shouldContain("Test (reserved=4096KB, committed=0KB)");

        wb.NMTReleaseMemory(addr, reserveSize);
        output = new OutputAnalyzer(pb.start());
        output.shouldNotContain("Test (reserved=");
        output.shouldNotMatch("\\[0x[0]*" + Long.toHexString(addr) + " - 0x[0]*"
                + Long.toHexString(addr + reserveSize) + "\\] reserved 4096KB for Test");
    }
}
