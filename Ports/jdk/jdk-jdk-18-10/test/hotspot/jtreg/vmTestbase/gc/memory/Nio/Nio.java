/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @modules java.base/jdk.internal.misc
 * @key stress
 *
 * @summary converted from VM Testbase gc/memory/Nio.
 * VM Testbase keywords: [gc, stress, stressopt, monitoring]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm -XX:MaxDirectMemorySize=50M gc.memory.Nio.Nio
 */

package gc.memory.Nio;

import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import jdk.internal.misc.VM;
import com.sun.management.HotSpotDiagnosticMXBean;
import java.io.File;
import java.io.IOException;

/**
 * Test that uses java.nio.ByteBuffer to allocate native memory.
 * The test allocates all the memory available and checks that
 * further attempts to allocate more more will fail.
 * Test also checks that allocating native memory doesn't affect heap.
 * Test also cheks that GC can find unused native memory.
 *
 * @summary Checks that nio.ByteBuffer allocates native memory and doesn't affect heap.
 * @run main/othervm -XX:MaxDirectMemorySize=50M gc.memory.Nio.Nio
 */
public class Nio {

    static final int MAX_SIZE = (int)VM.maxDirectMemory();

    public static void main(String[] args) {
        System.exit(new Nio().run() + 95 /*STATUS_BASE*/);
    }

    public Nio() {
    }

    public int run() {
        // Step0: init
        gc();
        long usedHeap_0 = getUsedHeap();
        long usedNonHeap_0 = getUsedNonHeap();

        // Step1: allocate the all available direct memory
        //        no OOME, no heap memory should be used
        System.out.println("Allocating all the direct memory: " + MAX_SIZE);
        ByteBuffer bb;
        try {
            bb = ByteBuffer.allocateDirect((int)MAX_SIZE);
            System.out.println("... success");
        } catch (OutOfMemoryError oom) {
            throw new Fault("Unexpected OOME during the first allocation " + oom);
        }
        long usedHeap_1 = getUsedHeap();
        long usedNonHeap_1 = getUsedNonHeap();
        checkHeapIsNotAffected(usedHeap_0, usedHeap_1, usedNonHeap_0, usedNonHeap_1);
        // Step2: invoke GC, it shouldn't help.
        System.out.println("Doing gc");
        gc();

        // Step3: allocate 1 byte in the direct memory
        //        OOM is expected
        try {
            System.out.println("Allocating 1 byte");
            ByteBuffer.allocateDirect(1);
            throw new Fault("No OOM, but we already allocated all the memory");
        } catch (OutOfMemoryError oom) {
            System.out.println("Expected OOM " + oom);
        }

        // Step4: read and write into allocated memory
        double d0 = -3.1415;
        float  f0 = 41234.6f;
        bb.putDouble(MAX_SIZE/2, d0);
        bb.putFloat(MAX_SIZE - 17, f0);
        double d1 = bb.getDouble(MAX_SIZE/2);
        float f1 = bb.getFloat(MAX_SIZE - 17);
        System.out.println("put: " + d0 + ", " + f0);
        System.out.println("got: " + d1 + ", " + f1);
        if (d0 != d1 || f0 != f1) {
            throw new Fault("read/write to buffer check failed");
        }

        // Step5:
        //  clean the buffer, use gc, try to allocate again
        //  no OOM is expected
        bb = null;
        gc();
        try {
            System.out.println("Allocating 10 bytes");
            ByteBuffer.allocateDirect(10);
        } catch (OutOfMemoryError oom) {
            throw new Fault("Nop, OOM is unexpected again: " + oom);
        }


        System.out.println("The long quest has done! Congratulations");

        return 0;
    }


    public static void gc() {
        System.gc();
        try {
            Thread.currentThread().sleep(200);
        } catch (Exception ignore) {
        }
    }

    /**
     * @return the size of used heap
     */
    public static long getUsedHeap() {
         return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    }

    /**
     * @return the size of used non heap
     */
    public static long getUsedNonHeap() {
         return ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed();
    }

    /**
     * Check that heap and non-heap memory have NOT changed significantly.
     * Throws a Fault if check failed.
     *
     * @param h_before  used heap before
     * @param h_after   used heap after
     * @param nh_before used non heap before
     * @param nh_after  used non heap after
     */
    void checkHeapIsNotAffected(long h_before, long h_after, long nh_before, long nh_after) {

        if (h_after - h_before > MAX_SIZE * 0.75) {
            System.err.println("Used heap before: " + h_before);
            System.err.println("Used heap after : " + h_after);
            dumpHeap();
            String failed = "Allocating direct memory should not eat the heap!"
                    + " Heap dumped to heapDump.hprof file.";
            throw new Fault(failed);
        }
        if (nh_after - nh_before > MAX_SIZE * 0.75) {
            System.err.println("Used heap before: " + nh_before);
            System.err.println("Used heap after : " + nh_after);
            dumpHeap();
            throw new Fault("Allocating direct memory should not eat non the heap!");
        }
    }

    /**
     * Try to make heap dump
     */
    void dumpHeap() {
        HotSpotDiagnosticMXBean mxBean = ManagementFactory
                .getPlatformMXBean(HotSpotDiagnosticMXBean.class);
        try {
            System.out.println("Try to dump heap to heapDump.hprof file..");
            mxBean.dumpHeap("heapDump.hprof", false);
            System.out.println("Done");
        } catch (IOException e) {
            System.out.println("Failed to dump heap");
            e.printStackTrace();
        }
    }

    /**
     * RuntimeException signaling a test failure.
     */
    public static class Fault extends RuntimeException {
        public Fault(String message) {
            super(message);
        }
    }

}
