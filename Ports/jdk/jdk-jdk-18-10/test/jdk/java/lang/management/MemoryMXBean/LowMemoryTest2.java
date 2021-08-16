/*
 * Copyright (c) 2004, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * Test low memory detection of non-heap memory pool.
 *
 * The test set a listener to be notified when any of the non-heap pools
 * exceed 80%. It then starts a thread that continuously loads classes.
 * In the HotSpot implementation this causes metaspace to be consumed.
 * Test completes when we the notification is received or an OutOfMemory
 * is generated.
 */

import java.lang.management.*;
import javax.management.*;
import javax.management.openmbean.CompositeData;
import java.util.*;

public class LowMemoryTest2 {

    private static volatile boolean listenerInvoked = false;

    private static String INDENT = "    ";

    static class SensorListener implements NotificationListener {
        public void handleNotification(Notification notif, Object handback) {
            String type = notif.getType();
            if (type.equals(MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED) ||
                type.equals(MemoryNotificationInfo.
                    MEMORY_COLLECTION_THRESHOLD_EXCEEDED)) {
                listenerInvoked = true;
                MemoryNotificationInfo minfo = MemoryNotificationInfo.
                    from((CompositeData) notif.getUserData());

                System.out.print("Notification for " + minfo.getPoolName());
                System.out.print(" [type = " + type);
                System.out.println(" count = " + minfo.getCount() + "]");
                System.out.println(INDENT + "usage = " + minfo.getUsage());
            }
        }
    }

    // Loads classes Test100001, Test100002, .... until OutOfMemoryErrror or
    // low memory notification

    static class BoundlessLoaderThread extends ClassLoader implements Runnable {
        private final List<MemoryPoolMXBean> pools;

        public BoundlessLoaderThread(List<MemoryPoolMXBean> pools) {
            this.pools = pools;
        }

        static int count = 100000;

        Class loadNext() {

            // public class TestNNNNNN extends java.lang.Object{
            // public TestNNNNNN();
            //   Code:
            //    0:    aload_0
            //    1:    invokespecial   #1; //Method java/lang/Object."<init>":()V
            //    4:    return
            // }

            int begin[] = {
                0xca, 0xfe, 0xba, 0xbe, 0x00, 0x00, 0x00, 0x30,
                0x00, 0x0a, 0x0a, 0x00, 0x03, 0x00, 0x07, 0x07,
                0x00, 0x08, 0x07, 0x00, 0x09, 0x01, 0x00, 0x06,
                0x3c, 0x69, 0x6e, 0x69, 0x74, 0x3e, 0x01, 0x00,
                0x03, 0x28, 0x29, 0x56, 0x01, 0x00, 0x04, 0x43,
                0x6f, 0x64, 0x65, 0x0c, 0x00, 0x04, 0x00, 0x05,
                0x01, 0x00, 0x0a, 0x54, 0x65, 0x73, 0x74 };

            int end [] = {
                0x01, 0x00, 0x10,
                0x6a, 0x61, 0x76, 0x61, 0x2f, 0x6c, 0x61, 0x6e,
                0x67, 0x2f, 0x4f, 0x62, 0x6a, 0x65, 0x63, 0x74,
                0x00, 0x21, 0x00, 0x02, 0x00, 0x03, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x04,
                0x00, 0x05, 0x00, 0x01, 0x00, 0x06, 0x00, 0x00,
                0x00, 0x11, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00,
                0x00, 0x05, 0x2a, 0xb7, 0x00, 0x01, 0xb1, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00 };


            // TestNNNNNN

            int load_count = count++;
            if (load_count > 999999) {
                // The test will create a corrupt class file if the count
                // exceeds 999999. Fix the test if this exception is thrown.
                throw new RuntimeException("Load count exceeded");
            }

            String name = "Test" + Integer.toString(load_count);

            byte value[];
            try {
                value = name.substring(4).getBytes("UTF-8");
            } catch (java.io.UnsupportedEncodingException x) {
                throw new Error();
            }

            // construct class file

            int len = begin.length + value.length + end.length;
            byte b[] = new byte[len];
            int i, pos=0;
            for (i=0; i<begin.length; i++) {
                b[pos++] = (byte)begin[i];
            }
            for (i=0; i<value.length; i++) {
                b[pos++] = value[i];
            }
            for (i=0; i<end.length; i++) {
                b[pos++] = (byte)end[i];
            }

            return defineClass(name, b, 0, b.length);
        }

        /*
         * Load classes until MemoryPoolMXBean.getUsageThresholdCount() > 0.
         * Then wait for the memory threshold notification to be received.
         */
        public void run() {
            // Load classes until MemoryPoolMXBean.getUsageThresholdCount() > 0
            boolean isThresholdCountSet = false;
            try {
                while (!isThresholdCountSet) {
                    // the classes are small so we load 10 at a time
                    for (int i=0; i<10; i++) {
                        loadNext();
                    }

                    if (isAnyUsageAboveThreshold(pools)) {
                        // UsageThresholdCount is only updated during GC.
                        // Force GC to update counters.
                        // If we don't force a GC we may get an
                        // OutOfMemoryException before the counters are updated.
                        System.out.println("Force GC");
                        System.gc();
                    }
                    isThresholdCountSet = isAnyThresholdCountSet(pools);
                }
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
                MemoryUtil.printMemoryPools(pools);
                throw e;
            }

            System.out.println("thresholdExceeded. Waiting for notification");
            while (!listenerInvoked) {
                try {
                    Thread.currentThread().sleep(10);
                } catch (InterruptedException x) {}
            }
        }

        private boolean isAnyUsageAboveThreshold(List<MemoryPoolMXBean> pools) {
            for (MemoryPoolMXBean p : pools) {
                if (p.isUsageThresholdExceeded()) {
                    System.out.println("isAnyUsageAboveThreshold is true for " + p.getName());
                    MemoryUtil.printMemoryPool(p);
                    return true;
                }
            }
            return false;
        }

        private boolean isAnyThresholdCountSet(List<MemoryPoolMXBean> pools) {
            for (MemoryPoolMXBean p : pools) {
                if (p.getUsageThresholdCount() > 0) {
                    System.out.println("isAnyThresholdCountSet is true for " + p.getName());
                    MemoryUtil.printMemoryPool(p);
                    return true;
                }
            }
            return false;
        }
    }

    public static void main(String args[]) {
        // The pools list will only contain the pools that we are interested in.
        List<MemoryPoolMXBean> pools = new ArrayList<MemoryPoolMXBean>();

        // Set threshold of 80% of all NON_HEAP memory pools
        // In the Hotspot implementation this means we should get a notification
        // if the CodeCache or metaspace fills up.

        for (MemoryPoolMXBean p : ManagementFactory.getMemoryPoolMXBeans()) {
            if (p.getType() == MemoryType.NON_HEAP && p.isUsageThresholdSupported()) {

                // set threshold
                MemoryUsage mu = p.getUsage();
                long max = mu.getMax();
                if (max < 0) {
                    throw new RuntimeException("There is no maximum set for "
                            + p.getName() + " memory pool so the test is invalid");
                }
                long threshold = (max * 80) / 100;

                p.setUsageThreshold(threshold);
                pools.add(p);

                System.out.println("Selected memory pool for low memory " +
                        "detection.");
                MemoryUtil.printMemoryPool(p);

            }
        }


        // Install the listener

        MemoryMXBean mm = ManagementFactory.getMemoryMXBean();
        SensorListener l2 = new SensorListener();

        NotificationEmitter emitter = (NotificationEmitter) mm;
        emitter.addNotificationListener(l2, null, null);

        // Start the thread loading classes

        Thread thr = new Thread(new BoundlessLoaderThread(pools));
        thr.start();

        // Wait for the thread to terminate
        try {
            thr.join();
        } catch (InterruptedException x) {
            throw new RuntimeException(x);
        }

        if (listenerInvoked) {
            System.out.println("Notification received - test passed.");
        } else {
            throw new RuntimeException("Test failed - notification not received!");
        }
    }

}
