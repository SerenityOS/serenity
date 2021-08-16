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
 * @summary Test corner case that overflows malloc site hashtable bucket
 * @requires sun.arch.data.model == "32"
 * @key stress
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -XX:NativeMemoryTracking=detail MallocSiteHashOverflow
 */

import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import sun.hotspot.WhiteBox;

public class MallocSiteHashOverflow {

    public static void main(String args[]) throws Exception {

        // Size of entries based on malloc tracking header defined in mallocTracker.hpp
        // For 32-bit systems, create 257 malloc sites with the same hash bucket to overflow a hash bucket
        long entries = 257;

        OutputAnalyzer output;
        WhiteBox wb = WhiteBox.getWhiteBox();
        int MAX_HASH_SIZE = wb.NMTGetHashSize();

        // Grab my own PID
        String pid = Long.toString(ProcessTools.getProcessId());
        ProcessBuilder pb = new ProcessBuilder();

        // Verify that current tracking level is "detail"
        pb.command(new String[] { JDKToolFinder.getJDKTool("jcmd"), pid, "VM.native_memory", "statistics"});
        output = new OutputAnalyzer(pb.start());
        output.shouldContain("Native Memory Tracking Statistics");

        // Attempt to cause NMT to downgrade tracking level by allocating small amounts
        // of memory with random pseudo call stack
        int pc = 1;
        for (int i = 0; i < entries; i++) {
            long addr = wb.NMTMallocWithPseudoStack(1, pc);
            if (addr == 0) {
                throw new RuntimeException("NMTMallocWithPseudoStack: out of memory");
            }
            // We free memory here since it doesn't affect pseudo malloc alloc site hash table entries
            wb.NMTFree(addr);
            pc += MAX_HASH_SIZE;
            if (i == entries) {
                // Verify that tracking has been downgraded
                pb.command(new String[] { JDKToolFinder.getJDKTool("jcmd"), pid, "VM.native_memory", "statistics"});
                output = new OutputAnalyzer(pb.start());
                output.shouldContain("Tracking level has been downgraded due to lack of resources");
            }
        }
    }
}
