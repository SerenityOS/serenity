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
 * @summary Stress test for malloc tracking
 * @key stress randomness
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm/timeout=1200 -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -XX:NativeMemoryTracking=detail MallocStressTest
 */

import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.Platform;
import jdk.test.lib.Utils;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import sun.hotspot.WhiteBox;

public class MallocStressTest {
    private static int K = 1024;

    // The stress test runs in three phases:
    // 1. alloc: A lot of malloc with fewer free, which simulates a burst memory allocation
    //    that is usually seen during startup or class loading.
    // 2. pause: Pause the test to check accuracy of native memory tracking
    // 3. release: Release all malloc'd memory and check native memory tracking result.
    public enum TestPhase {
        alloc,
        pause,
        release
    };

    static volatile TestPhase phase = TestPhase.alloc;

    // malloc'd memory
    static final ArrayList<MallocMemory>  mallocd_memory = new ArrayList<MallocMemory>();
    static long                     mallocd_total  = 0;
    static WhiteBox                 whiteBox;
    static AtomicInteger            pause_count = new AtomicInteger();

    static final boolean            is_64_bit_system = Platform.is64bit();

    private static boolean is_64_bit_system() { return is_64_bit_system; }

    public static void main(String args[]) throws Exception {
        OutputAnalyzer output;
        whiteBox = WhiteBox.getWhiteBox();

        // Grab my own PID
        String pid = Long.toString(ProcessTools.getProcessId());
        ProcessBuilder pb = new ProcessBuilder();

        AllocThread[]   alloc_threads = new AllocThread[40];
        ReleaseThread[] release_threads = new ReleaseThread[10];

        int index;
        // Create many allocation threads
        for (index = 0; index < alloc_threads.length; index ++) {
            alloc_threads[index] = new AllocThread();
        }

        // Fewer release threads
        for (index = 0; index < release_threads.length; index ++) {
            release_threads[index] = new ReleaseThread();
        }

        // pause the stress test
        phase = TestPhase.pause;
        while (pause_count.intValue() <  alloc_threads.length + release_threads.length) {
            sleep_wait(10);
        }

        long mallocd_total_in_KB = (mallocd_total + K / 2) / K;

        // Now check if the result from NMT matches the total memory allocated.
        String expected_test_summary = "Test (reserved=" + mallocd_total_in_KB +"KB, committed=" + mallocd_total_in_KB + "KB)";
        // Run 'jcmd <pid> VM.native_memory summary'
        pb.command(new String[] { JDKToolFinder.getJDKTool("jcmd"), pid, "VM.native_memory", "summary"});
        output = new OutputAnalyzer(pb.start());
        output.shouldContain(expected_test_summary);

        // Release all allocated memory
        phase = TestPhase.release;
        synchronized(mallocd_memory) {
            mallocd_memory.notifyAll();
        }

        // Join all threads
        for (index = 0; index < alloc_threads.length; index ++) {
            try {
                alloc_threads[index].join();
            } catch (InterruptedException e) {
            }
        }

        for (index = 0; index < release_threads.length; index ++) {
            try {
                release_threads[index].join();
            } catch (InterruptedException e) {
            }
        }

        // All test memory allocated should be released
        output = new OutputAnalyzer(pb.start());
        output.shouldNotContain("Test (reserved=");

        // Verify that tracking level has not been downgraded
        pb.command(new String[] { JDKToolFinder.getJDKTool("jcmd"), pid, "VM.native_memory", "statistics"});
        output = new OutputAnalyzer(pb.start());
        output.shouldNotContain("Tracking level has been downgraded due to lack of resources");
    }

    private static void sleep_wait(int n) {
        try {
            Thread.sleep(n);
        } catch (InterruptedException e) {
        }
    }


    static class MallocMemory {
        private long  addr;
        private int   size;

        MallocMemory(long addr, int size) {
            this.addr = addr;
            this.size = size;
        }

        long addr()  { return this.addr; }
        int  size()  { return this.size; }
    }

    static class AllocThread extends Thread {
        private final Random random = new Random(Utils.getRandomInstance().nextLong());
        AllocThread() {
            this.setName("MallocThread");
            this.start();
        }

        // AllocThread only runs "Alloc" phase
        public void run() {
            // MallocStressTest.phase == TestPhase.alloc
            for (int loops = 0; loops < 100; loops++) {
                int r = random.nextInt(Integer.MAX_VALUE);
                // Only malloc small amount to avoid OOM
                int size = r % 32;
                if (is_64_bit_system()) {
                    r = r % 32 * K;
                } else {
                    r = r % 64;
                }
                if (size == 0) size = 1;
                long addr = MallocStressTest.whiteBox.NMTMallocWithPseudoStack(size, r);
                if (addr != 0) {
                    try {
                        MallocMemory mem = new MallocMemory(addr, size);
                        synchronized(MallocStressTest.mallocd_memory) {
                            MallocStressTest.mallocd_memory.add(mem);
                            MallocStressTest.mallocd_total += size;
                        }
                    } catch (OutOfMemoryError e) {
                        // Don't include this malloc memory because it didn't
                        // get recorded in mallocd_memory list.
                        MallocStressTest.whiteBox.NMTFree(addr);
                        break;
                    }
                } else {
                    break;
                }
            }
            MallocStressTest.pause_count.incrementAndGet();
        }
    }

    static class ReleaseThread extends Thread {
        private final Random random = new Random(Utils.getRandomInstance().nextLong());
        ReleaseThread() {
            this.setName("ReleaseThread");
            this.start();
        }

        public void run() {
            while(true) {
                switch(MallocStressTest.phase) {
                case alloc:
                    slow_release();
                    break;
                case pause:
                    enter_pause();
                    break;
                case release:
                    quick_release();
                    return;
                }
            }
        }

        private void enter_pause() {
            MallocStressTest.pause_count.incrementAndGet();
            while (MallocStressTest.phase != MallocStressTest.TestPhase.release) {
                try {
                    synchronized(MallocStressTest.mallocd_memory) {
                        MallocStressTest.mallocd_memory.wait(10);
                    }
                } catch (InterruptedException e) {
                }
            }
        }

        private void quick_release() {
            List<MallocMemory> free_list;
            while (true) {
                synchronized(MallocStressTest.mallocd_memory) {
                    if (MallocStressTest.mallocd_memory.isEmpty()) return;
                    int size =  Math.min(MallocStressTest.mallocd_memory.size(), 5000);
                    List<MallocMemory> subList = MallocStressTest.mallocd_memory.subList(0, size);
                    free_list = new ArrayList<MallocMemory>(subList);
                    subList.clear();
                }
                for (int index = 0; index < free_list.size(); index ++) {
                    MallocMemory mem = free_list.get(index);
                    MallocStressTest.whiteBox.NMTFree(mem.addr());
                }
            }
        }

        private void slow_release() {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
            synchronized(MallocStressTest.mallocd_memory) {
                if (MallocStressTest.mallocd_memory.isEmpty()) return;
                int n = random.nextInt(MallocStressTest.mallocd_memory.size());
                MallocMemory mem = mallocd_memory.remove(n);
                MallocStressTest.whiteBox.NMTFree(mem.addr());
                MallocStressTest.mallocd_total -= mem.size();
            }
        }
    }
}
