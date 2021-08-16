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
 * @bug 8054836
 * @summary Test to verify correctness of malloc tracking
 * @key randomness
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -XX:NativeMemoryTracking=detail MallocTrackingVerify
 *
 */

import java.util.ArrayList;
import java.util.Random;

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.Utils;

import sun.hotspot.WhiteBox;

public class MallocTrackingVerify {
    private static int MAX_ALLOC = 4 * 1024;

    static ArrayList<MallocMemory> mallocd_memory = new ArrayList<MallocMemory>();
    static long mallocd_total = 0;
    public static WhiteBox wb = WhiteBox.getWhiteBox();

    public static void main(String args[]) throws Exception {
        OutputAnalyzer output;

        // Grab my own PID
        String pid = Long.toString(ProcessTools.getProcessId());
        ProcessBuilder pb = new ProcessBuilder();

        Random random = Utils.getRandomInstance();
        // Allocate small amounts of memory with random pseudo call stack
        while (mallocd_total < MAX_ALLOC) {
            int size = random.nextInt(31) + 1;
            long addr = wb.NMTMallocWithPseudoStack(size, random.nextInt());
            if (addr != 0) {
                MallocMemory mem = new MallocMemory(addr, size);
                mallocd_memory.add(mem);
                mallocd_total += size;
            } else {
                System.out.println("Out of malloc memory");
                break;
            }
        }

        pb.command(new String[] { JDKToolFinder.getJDKTool("jcmd"), pid, "VM.native_memory", "summary" });
        output = new OutputAnalyzer(pb.start());
        output.shouldContain("Test (reserved=4KB, committed=4KB)");

        // Free
        for (MallocMemory mem : mallocd_memory) {
            wb.NMTFree(mem.addr());
        }

        // Run 'jcmd <pid> VM.native_memory summary', check for expected output
        pb.command(new String[] { JDKToolFinder.getJDKTool("jcmd"), pid,
                "VM.native_memory", "summary" });
        output = new OutputAnalyzer(pb.start());
        output.shouldNotContain("Test (reserved=");
    }

    static class MallocMemory {
        private long addr;
        private int size;

        MallocMemory(long addr, int size) {
            this.addr = addr;
            this.size = size;
        }

        long addr() {
            return this.addr;
        }

        int size() {
            return this.size;
        }
    }
}
