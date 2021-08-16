/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     4959889 6992968
 * @summary Basic unit test of memory management testing:
 *          1) setCollectionUsageThreshold() and getCollectionUsageThreshold()
 *          2) test notification emitted for two different memory pools.
 *
 * @author  Mandy Chung
 *
 * @library /test/lib
 * @modules jdk.management
 * @build CollectionUsageThreshold MemoryUtil RunUtil
 * @requires vm.opt.ExplicitGCInvokesConcurrent == "false" | vm.opt.ExplicitGCInvokesConcurrent == "null"
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm/timeout=300 -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:. CollectionUsageThreshold
 */

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.management.*;
import javax.management.openmbean.CompositeData;
import java.lang.management.*;
import static java.lang.management.MemoryNotificationInfo.*;;
import static java.lang.management.ManagementFactory.*;

import sun.hotspot.code.Compiler;

public class CollectionUsageThreshold {
    private static final MemoryMXBean mm = getMemoryMXBean();
    private static final Map<String, PoolRecord> result = new HashMap<>();
    private static boolean trace = false;
    private static volatile int numMemoryPools = 1;
    private static final int NUM_GCS = 3;
    private static final int THRESHOLD = 10;
    private static volatile int numGCs = 0;

    // semaphore to signal the arrival of a low memory notification
    private static final Semaphore signals = new Semaphore(0);
    // barrier for the main thread to wait until the checker thread
    // finishes checking the low memory notification result
    private static final CyclicBarrier barrier = new CyclicBarrier(2);

    /**
     * Run the test multiple times with different GC versions.
     * First with default command line specified by the framework.
     * Then with GC versions specified by the test.
     */
    public static void main(String a[]) throws Throwable {
        final String main = "CollectionUsageThreshold$TestMain";
        RunUtil.runTestKeepGcOpts(main);
        RunUtil.runTestClearGcOpts(main, "-XX:+UseSerialGC");
        RunUtil.runTestClearGcOpts(main, "-XX:+UseParallelGC");
        RunUtil.runTestClearGcOpts(main, "-XX:+UseG1GC");
    }

    static class PoolRecord {
        private final MemoryPoolMXBean pool;
        private final AtomicInteger listenerInvoked = new AtomicInteger(0);
        private volatile long notifCount = 0;
        PoolRecord(MemoryPoolMXBean p) {
            this.pool = p;
        }
        int getListenerInvokedCount() {
            return listenerInvoked.get();
        }
        long getNotifCount() {
            return notifCount;
        }
        MemoryPoolMXBean getPool() {
            return pool;
        }
        void addNotification(MemoryNotificationInfo minfo) {
            listenerInvoked.incrementAndGet();
            notifCount = minfo.getCount();
        }
    }

    static class SensorListener implements NotificationListener {
        @Override
        public void handleNotification(Notification notif, Object handback) {
            String type = notif.getType();
            if (MEMORY_THRESHOLD_EXCEEDED.equals(type) ||
                MEMORY_COLLECTION_THRESHOLD_EXCEEDED.equals(type)) {
                MemoryNotificationInfo minfo = MemoryNotificationInfo.
                    from((CompositeData) notif.getUserData());

                MemoryUtil.printMemoryNotificationInfo(minfo, type);
                PoolRecord pr = (PoolRecord) result.get(minfo.getPoolName());
                if (pr == null) {
                    throw new RuntimeException("Pool " + minfo.getPoolName() +
                        " is not selected");
                }
                if (!MEMORY_COLLECTION_THRESHOLD_EXCEEDED.equals(type)) {
                    throw new RuntimeException("Pool " + minfo.getPoolName() +
                        " got unexpected notification type: " +
                        type);
                }
                pr.addNotification(minfo);
                System.out.println("notifying the checker thread to check result");
                signals.release();
            }
        }
    }

    private static class TestMain {
        public static void main(String args[]) throws Exception {
            if (args.length > 0 && args[0].equals("trace")) {
                trace = true;
            }

            List<MemoryPoolMXBean> pools = getMemoryPoolMXBeans();
            List<MemoryManagerMXBean> managers = getMemoryManagerMXBeans();

            if (trace) {
                MemoryUtil.printMemoryPools(pools);
                MemoryUtil.printMemoryManagers(managers);
            }

            // Find the Old generation which supports low memory detection
            for (MemoryPoolMXBean p : pools) {
                if (p.isUsageThresholdSupported() && p.isCollectionUsageThresholdSupported()) {
                    if (p.getName().toLowerCase().contains("perm")) {
                        // if we have a "perm gen" pool increase the number of expected
                        // memory pools by one.
                        numMemoryPools++;
                    }
                    PoolRecord pr = new PoolRecord(p);
                    result.put(p.getName(), pr);
                    if (result.size() == numMemoryPools) {
                        break;
                    }
                }
            }
            if (result.size() != numMemoryPools) {
                throw new RuntimeException("Unexpected number of selected pools");
            }

            try {
                // This test creates a checker thread responsible for checking
                // the low memory notifications.  It blocks until a permit
                // from the signals semaphore is available.
                Checker checker = new Checker("Checker thread");
                checker.setDaemon(true);
                checker.start();

                for (PoolRecord pr : result.values()) {
                    pr.getPool().setCollectionUsageThreshold(THRESHOLD);
                    System.out.println("Collection usage threshold of " +
                        pr.getPool().getName() + " set to " + THRESHOLD);
                }

                SensorListener listener = new SensorListener();
                NotificationEmitter emitter = (NotificationEmitter) mm;
                emitter.addNotificationListener(listener, null, null);

                // The main thread invokes GC to trigger the VM to perform
                // low memory detection and then waits until the checker thread
                // finishes its work to check for a low-memory notification.
                //
                // At GC time, VM will issue low-memory notification and invoke
                // the listener which will release a permit to the signals semaphore.
                // When the checker thread acquires the permit and finishes
                // checking the low-memory notification, it will also call
                // barrier.await() to signal the main thread to resume its work.
                for (int i = 0; i < NUM_GCS; i++) {
                    invokeGC();
                    barrier.await();
                }
            } finally {
                // restore the default
                for (PoolRecord pr : result.values()) {
                    pr.getPool().setCollectionUsageThreshold(0);
                }
            }
            System.out.println(RunUtil.successMessage);
        }


        private static void invokeGC() {
            System.out.println("Calling System.gc()");
            numGCs++;
            mm.gc();

            if (trace) {
                for (PoolRecord pr : result.values()) {
                    System.out.println("Usage after GC for: " + pr.getPool().getName());
                    MemoryUtil.printMemoryUsage(pr.getPool().getUsage());
                }
            }
        }
    }

    static class Checker extends Thread {
        Checker(String name) {
            super(name);
        };
        @Override
        public void run() {
            while (true) {
                try {
                    signals.acquire(numMemoryPools);
                    checkResult();
                } catch (InterruptedException | BrokenBarrierException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        private void checkResult() throws InterruptedException, BrokenBarrierException {
            for (PoolRecord pr : result.values()) {
                if (pr.getListenerInvokedCount() != numGCs) {
                    fail("Listeners invoked count = " +
                         pr.getListenerInvokedCount() + " expected to be " +
                         numGCs);
                }
                if (pr.getNotifCount() != numGCs) {
                    fail("Notif Count = " +
                         pr.getNotifCount() + " expected to be " +
                         numGCs);
                }

                long count = pr.getPool().getCollectionUsageThresholdCount();
                if (count != numGCs) {
                    fail("CollectionUsageThresholdCount = " +
                         count + " expected to be " + numGCs);
                }
                if (!pr.getPool().isCollectionUsageThresholdExceeded()) {
                    fail("isCollectionUsageThresholdExceeded" +
                         " expected to be true");
                }
            }
            // wait until the main thread is waiting for notification
            barrier.await();
            System.out.println("notifying main thread to continue - result checking finished");
        }

        private void fail(String msg) {
            // reset the barrier to cause BrokenBarrierException to avoid hanging
            barrier.reset();
            throw new RuntimeException(msg);
        }
    }
}
