/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     4530538 6980984
 * @summary Basic unit test of memory management testing:
 *          1) setUsageThreshold() and getUsageThreshold()
 *          2) test low memory detection on the old generation.
 *
 * @author  Mandy Chung
 *
 * @modules jdk.management
 * @build MemoryManagement MemoryUtil
 * @run main/othervm/timeout=600 -Xmn8m -XX:+IgnoreUnrecognizedVMOptions -XX:G1HeapRegionSize=1 -XX:-UseLargePages MemoryManagement
 */

import java.lang.management.*;
import java.util.*;
import javax.management.*;
import javax.management.openmbean.CompositeData;

public class MemoryManagement {
    private static final MemoryMXBean mm = ManagementFactory.getMemoryMXBean();
    private static final List pools =
            Collections.synchronizedList(ManagementFactory.getMemoryPoolMXBeans());
    private static final List managers =
            Collections.synchronizedList(ManagementFactory.getMemoryManagerMXBeans());
    private static volatile MemoryPoolMXBean mpool = null;
    private static volatile boolean trace = false;
    private static volatile boolean testFailed = false;
    private static final int NUM_CHUNKS = 2;
    // Must match -Xmn set on the @run line
    private static final int YOUNG_GEN_SIZE = 8 * 1024 * 1024;
    private static volatile long chunkSize;
    private static volatile int listenerInvoked = 0;

    static class SensorListener implements NotificationListener {
        public void handleNotification(Notification notif, Object handback) {
            String type = notif.getType();
            if (type.equals(MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED) ||
                type.equals(MemoryNotificationInfo.
                    MEMORY_COLLECTION_THRESHOLD_EXCEEDED)) {

                MemoryNotificationInfo minfo = MemoryNotificationInfo.
                    from((CompositeData) notif.getUserData());

                MemoryUtil.printMemoryNotificationInfo(minfo, type);
                listenerInvoked++;
            }
        }
    }

    private static long newThreshold;
    public static void main(String args[]) throws Exception {
        if (args.length > 0 && args[0].equals("trace")) {
            trace = true;
        }

        if (trace) {
            MemoryUtil.printMemoryPools(pools);
            MemoryUtil.printMemoryManagers(managers);
        }

        // Find the Old generation which supports low memory detection
        ListIterator iter = pools.listIterator();
        while (iter.hasNext()) {
            MemoryPoolMXBean p = (MemoryPoolMXBean) iter.next();
            if (p.getType() == MemoryType.HEAP &&
                p.isUsageThresholdSupported()) {
                mpool = p;
                if (trace) {
                    System.out.println("Selected memory pool for low memory " +
                        "detection.");
                    MemoryUtil.printMemoryPool(mpool);
                }
                break;
            }
        }

        SensorListener listener = new SensorListener();
        NotificationEmitter emitter = (NotificationEmitter) mm;
        emitter.addNotificationListener(listener, null, null);

        Thread allocator = new AllocatorThread();

        // The chunk size needs to be larger than YOUNG_GEN_SIZE,
        // otherwise we will get intermittent failures when objects
        // end up in the young gen instead of the old gen.
        final long epsilon = 1024;
        chunkSize = YOUNG_GEN_SIZE + epsilon;

        // Now set threshold
        MemoryUsage mu = mpool.getUsage();
        newThreshold = mu.getUsed() + (chunkSize * NUM_CHUNKS);

        // Sanity check. Make sure the new threshold isn't too large.
        // Tweak the test if this fails.
        final long headRoom = chunkSize * 2;
        final long max = mu.getMax();
        if (max != -1 && newThreshold > max - headRoom) {
            throw new RuntimeException("TEST FAILED: newThreshold: " + newThreshold +
                    " is too near the maximum old gen size: " + max +
                    " used: " + mu.getUsed() + " headRoom: " + headRoom);
        }

        System.out.println("Setting threshold for " + mpool.getName() +
            " from " + mpool.getUsageThreshold() + " to " + newThreshold +
            ".  Current used = " + mu.getUsed());
        mpool.setUsageThreshold(newThreshold);

        if (mpool.getUsageThreshold() != newThreshold) {
            throw new RuntimeException("TEST FAILED: " +
                "Threshold for Memory pool " + mpool.getName() +
                "is " + mpool.getUsageThreshold() + " but expected to be" +
                newThreshold);
        }

        // Start the AllocatorThread to continously allocate memory
        System.out.println("Starting an AllocatorThread to allocate memory.");
        allocator.start();

        try {
            allocator.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.out.println("Unexpected exception.");
            testFailed = true;
        }

        if (listenerInvoked == 0) {
            throw new RuntimeException("No listener is invoked");
        }

        if (testFailed)
            throw new RuntimeException("TEST FAILED.");

        System.out.println("Test passed.");

    }

    static class AllocatorThread extends Thread {
        private List objectPool = new ArrayList();
        public void run() {
            int iterations = 0;
            int numElements = (int) (chunkSize / 4); // minimal object size
            while (listenerInvoked == 0) {
                iterations++;
                if (trace) {
                    System.out.println("    Iteration " + iterations +
                        ": before allocation " +
                        mpool.getUsage().getUsed());
                }

                Object[] o = new Object[numElements];
                if (iterations <= NUM_CHUNKS) {
                    // only hold a reference to the first NUM_CHUNKS
                    // allocated objects
                    objectPool.add(o);
                }

                if (trace) {
                    System.out.println("                " +
                        "  after allocation " +
                        mpool.getUsage().getUsed());
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.out.println("Unexpected exception.");
                    testFailed = true;
                }
            }

            System.out.println("AllocatedThread finished memory allocation " +
                " num_iteration = " + iterations);
        }
    }

}
