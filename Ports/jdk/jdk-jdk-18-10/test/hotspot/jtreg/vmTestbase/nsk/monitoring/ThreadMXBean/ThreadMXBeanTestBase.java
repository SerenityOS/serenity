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

import java.util.*;
import nsk.share.test.*;
import nsk.monitoring.share.*;
import nsk.share.gc.Memory;

public abstract class ThreadMXBeanTestBase extends MonitoringTestBase
        implements Initializable {

    /**
     * Maximum allocation object size. about 2 Mb
     */
    private static final int MAX_OBJECT_SIZE = 2*1024*1024;
    /**
     * Allocations count for each MXBeanTestThread
     */
    private static final int ALLOCATIONS = 100;
    /**
     * Minimum size of allocated objects for a single thread stressing GC: 1Mb
     */
    protected static final int MIN_STRESS_ALLOCATION_AMOUNT = 1024*1024;
    /**
     * Percent of maximum difference of actual result from expected one
     */
    protected static final int DELTA_PERCENT = 15;
    /**
     * Instance of com.sun.management.ThreadMXBean used by all tests
     * Obtained in initialize() method
     */
    protected com.sun.management.ThreadMXBean threadMXBean;
    /**
     * Stresser class instance used in StressTest
     */
    protected Stresser stresser;
    /**
     * String indicating the GarbageProducer type used in test
     */
    protected String garbageProducerId;
    /**
     * Defined array with allocation objects sizes
     */
    protected static int[] allocArr = new int[ALLOCATIONS];

    /**
     * Obtains instance of com.sun.management.ThreadMXBean
     * and stores it in threadMXBean field
     * If com.sun.management.ThreadMXBean API is not available,
     * threadMXBean is set to null and appropriate message is
     * prompted
     */
    public void initialize() {
        if (monitoringFactory.hasThreadMXBeanNew()) {
            threadMXBean =
                    (com.sun.management.ThreadMXBean) monitoringFactory.getThreadMXBeanNew();
            // ensure LocalRandom is loaded and has enough memory
            LocalRandom.init();
            for (int i = 0; i < ALLOCATIONS; i++) {
                allocArr[i] = Memory.getArrayExtraSize() + Memory.getIntSize()
                        + Memory.getReferenceSize() // don't be zero-length
                        + LocalRandom.nextInt(MAX_OBJECT_SIZE);
            }
        } else {
            log.info("com.sun.management.ThreadMXBean API is not available!");
        }
    }

    /**
     * Obtains instance of Stresser and stores it in stresser field
     * @param args Stresser arguments
     */
    public void setStresser(String[] args) {
        if (stresser == null)
            stresser = new Stresser(args);
    }

    /**
     * Parses input String arrays searching for GarbageProducer
     * settings. If found, sets garbageProducerID filed.
     * @param args input arguments
     * @return input arguments without GarbageProducer options
     */
    public String[] setGarbageProducer(String[] args) {
        if (garbageProducerId == null) {
            ArrayList<String> list = new ArrayList<String>();
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-gp")) {
                    garbageProducerId = args[++i];
                } else {
                    list.add(args[i]);
                }
            }
            return list.toArray(new String[] {});
        } else {
            return args;
        }
    }

    /**
     * Starts all specified TestThread threads
     * @param threads threads to start
     */
    public BarrierHandler startThreads(MXBeanTestThread... threads) {
        BarrierHandler handler = new BarrierHandler(threads);
        for (MXBeanTestThread thread : threads) {
            thread.setHandler(handler);
            thread.start();
        }
        handler.start();
        return handler;
    }

    /**
     * Returns expected memory size allocated by MXBeanTestThreads during
     * stress test (allocateStress() method execution)
     *
     * @param array of MXBeanTestThreads
     * @return expected amount of memory allocated by each StressTestThread
     */
    public long[] getStressAllocatedBytes(MXBeanTestThread... threads) {
        long[] result = new long[threads.length];
        int counter = 0;
        for (MXBeanTestThread thread : threads) {
            result[counter++] = thread.getStressAllocatedBytes();
        }
        return result;
    }
}
