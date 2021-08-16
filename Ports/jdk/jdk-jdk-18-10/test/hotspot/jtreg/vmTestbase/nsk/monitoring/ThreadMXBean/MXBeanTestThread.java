/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

package nsk.monitoring.ThreadMXBean;

import java.util.ArrayList;
import java.util.List;
import nsk.share.gc.gp.GarbageProducer;
import nsk.share.gc.gp.GarbageUtils;
import nsk.share.test.Stresser;
import nsk.share.test.LocalRandom;


public class MXBeanTestThread extends Thread {

    /**
     * BarrierHandler instance for synchronization with management thread
     */
    protected BarrierHandler handler;

    /**
     * List where allocated objects are stored
     */
    private List<Object> allocatedList;
    /**
     * Number of simultaneously running threads
     */
    private static int threadCount;
    /**
     * Expected amount of memory allocated during stress test
     */
    private long stressAllocatedBytes;
    /**
     * Maximum memory in bytes that one thread is allowed to use at once
     */
    private long maxThreadMemory = 0;
    /**
     * GarbageProducer for objects creation
     */
    private GarbageProducer gp;
    /**
     * Stresser instance for allocateStress()
     */
    private Stresser stresser;

    public static void warmUp(String garbageProducerId) {
        MXBeanTestThread warmUpThread = new MXBeanTestThread(garbageProducerId) {
                @Override
                public void doWork() {
                    allocate();
                }
            };
        warmUpThread.start();
        do {
            try {
                warmUpThread.join();
            } catch (InterruptedException ie) {}
        } while(warmUpThread.isAlive());
    }

    /**
     * Sets BarrierHandler for this thread
     *
     * @param handler BarrierHandler to synchronize with
     */
    public void setHandler(BarrierHandler handler) {
        this.handler = handler;
    }

    /**
     * Returns an instance of MXBeanTestThread with already defined
     * allocatedList List and GarbageProducer
     */
    public MXBeanTestThread(String garbageProducerId) {
        super(Integer.toString(++threadCount));
        allocatedList = new ArrayList<Object>();
        gp = GarbageUtils.getGarbageProducer(garbageProducerId);
        maxThreadMemory = Runtime.getRuntime().maxMemory()/4;
    }

    /**
     * Returns an instance of MXBeanTestThread with already defined
     * allocatedList List and default GarbageProducer
     */
    public MXBeanTestThread() {
        this("intArr");
    }

    /**
     * Returns an instance of MXBeanTestThread with already defined
     * allocatedList List and Stresser
     */
    public MXBeanTestThread(Stresser stresser) {
        this("intArr");
        this.stresser = stresser;
    }

    /**
     * Sets maximum amount of memory that could be used at once for each
     * TestThread in StressTest
     */
    public void setMaxThreadMemory (long memory) {
        maxThreadMemory = memory;
    }

    /**
     * Returns expected memory allocated by TestThread during StressTest
     * @return expected memory amount
     */
    public long getStressAllocatedBytes() {
        return stressAllocatedBytes;
    }

    @Override
    public void run() {
        doWork();
    }

    /**
     * Implementation of TestThread behavior logic
     */
    public void doWork() {
        handler.ready();
        allocate();
        handler.ready();
    }

    /**
     * Allocates memory for amount of time specified in Stresser instance
     */
    protected void allocateStress() {
        // Size of long[] array that allocates 2 Mb + 1 byte
        int MAX_ARR_SIZE=262146;
        // Anount of memory allocated by thread with existing links
        // Which means that these objects can't be collected by GC
        long actuallyAllocated = 0;
        // ensure LocalRandom is loaded and has enough memory
        LocalRandom.init();
        try {
            while (stresser.continueExecution()) {
                //int chunkSize = LocalRandom.nextInt(MAX_OBJECT_SIZE);
                //Object obj = gp.create(chunkSize);
                int chunkSize = LocalRandom.nextInt(MAX_ARR_SIZE);
                Object obj = new long[chunkSize];
                allocatedList.add(obj);
                actuallyAllocated += chunkSize*8;
                if (actuallyAllocated > maxThreadMemory) {
                    // Allocated more then allowed to one thread
                    // re-define allocatedList to allow GC to delete
                    // created objects
                    stressAllocatedBytes += actuallyAllocated;
                    actuallyAllocated = 0;
                    allocatedList = new ArrayList<Object>();
                }
            }
        } catch (OutOfMemoryError e) {
        } finally {
            stressAllocatedBytes += actuallyAllocated;
        }
    }

    /**
     * Allocates memory once according test settings
     */
    protected void allocate() {
        long actuallyAllocated = 0;
        for (int i = 0; i < ThreadMXBeanTestBase.allocArr.length; i++) {
            long size = ThreadMXBeanTestBase.allocArr[i];
            if (actuallyAllocated + size > maxThreadMemory) {
                break;
            }
            allocatedList.add(gp.create(size));
            actuallyAllocated += size;
        }
    }
}
