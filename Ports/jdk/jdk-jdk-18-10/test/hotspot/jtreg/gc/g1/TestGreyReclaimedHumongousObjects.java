/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

package gc.g1;

/*
 * @test TestGreyReclaimedHumongousObjects.java
 * @bug 8069367 8185278
 * @requires vm.gc.G1
 * @summary Test handling of marked but unscanned reclaimed humongous objects.
 * @modules jdk.management
 * @run main/othervm -XX:+UseG1GC -Xss32m -Xmx128m -XX:G1HeapRegionSize=1m
 *      -XX:+UnlockExperimentalVMOptions
 *          -XX:+G1EagerReclaimHumongousObjects
 *          -XX:+G1EagerReclaimHumongousObjectsWithStaleRefs
 *      gc.g1.TestGreyReclaimedHumongousObjects 1048576 90
 */

// This test spawns a bunch of threads, each of them rapidly
// allocating large objects and storing them into a circular buffer
// associated with the thread.  The circular buffer results in these
// objects becoming dead in fairly short order.
//
// The situation we're trying to provoke is
//
// (1) A humongous object H is marked and added to the mark stack.
//
// (2) An evacuation pause determines H is no longer live, and
// reclaims it.  This occurs before concurrent marking has gotten
// around to processing the mark stack entry for H.
//
// (3) Concurrent marking processes the mark stack entry for H.  The
// bug is that it would attempt to scan the now dead object.
//
// Unfortunately, this test is *very* sensitive to configuration.
// Among the parameters that affect whether / how often we'll get into
// the desired situation within a reasonable amount of time are:
//
// - THREAD_COUNT: The number of allocating threads.
//
// - OLD_COUNT: The number of objects each thread keeps.
//
// - MAX_MEMORY: The maximum heap size.
//
// - G1HeapRegionSize
//
// - The size of the objects being allocated.
//
// The parameter values specified here:
//
// - THREAD_COUNT = 12
// - OLD_COUNT == 4
// - MAX_MEMORY == 128m
// - G1HeapRegionSize = 1m
// - Object size = 1048576 (2 regions after header overhead and roundup)
//
// seems to work well at provoking the desired state fairly quickly.
// Even relatively small perturbations may change that.  The key
// factors seem to be keeping the heap mostly full of live objects but
// having them become dead fairly quickly.

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import com.sun.management.HotSpotDiagnosticMXBean;
import java.lang.management.ManagementFactory;

public class TestGreyReclaimedHumongousObjects {

    static class NamedThreadFactory implements ThreadFactory {
       private int threadNum = 0;

       @Override
       public Thread newThread(Runnable r) {
         return new Thread(r, THREAD_NAME + (threadNum++));
       }
    }

    static class Runner extends Thread {
        private final Date startDate = new Date();
        private final int obj_size;
        private final Object[] old_garbage;
        private int old_index = 0;

        public Runner(int obj_size) {
            this.obj_size = obj_size;
            old_garbage = new Object[OLD_COUNT];
        }

        private void allocate_garbage() {
            byte[] garbage = new byte[obj_size];
            old_garbage[Math.abs(++old_index % OLD_COUNT)] = garbage;
        }

        @Override
        public void run() {
            try {
                while (!isInterrupted()) {
                    allocate_garbage();
                    Thread.sleep(0); // Yield, to ensure interruptable.
                }
            } catch (InterruptedException e) {
                System.out.println("Aborted after "
                                   + (new Date().getTime() - startDate.getTime())
                                   + " ms");
                interrupt();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        HotSpotDiagnosticMXBean diagnostic =
                ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);

        System.out.println("Max memory= " + MAX_MEMORY + " bytes");

        int obj_size = 0;
        long seconds_to_run = 0;
        if (args.length != 2) {
            throw new RuntimeException("Object size argument must be supplied");
        } else {
            obj_size = Integer.parseInt(args[0]);
            seconds_to_run = Integer.parseInt(args[1]);
        }
        System.out.println("Objects size= " + obj_size + " bytes");
        System.out.println("Seconds to run=" + seconds_to_run);

        int region_size =
            Integer.parseInt(diagnostic.getVMOption("G1HeapRegionSize").getValue());
        if (obj_size < (region_size / 2)) {
            throw new RuntimeException("Object size " + obj_size +
                                       " is not humongous with region size " + region_size);
        }

        ExecutorService executor =
            Executors.newFixedThreadPool(THREAD_COUNT, new NamedThreadFactory());
        System.out.println("Starting " + THREAD_COUNT + " threads");

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.execute(new Runner(obj_size));
        }

        Thread.sleep(seconds_to_run * 1000);
        executor.shutdownNow();

        if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
            System.err.println("Thread pool did not terminate after 10 seconds after shutdown");
        }
    }

    private static final long MAX_MEMORY = Runtime.getRuntime().maxMemory();
    private static final int OLD_COUNT = 4;
    private static final int THREAD_COUNT = 12;
    private static final String THREAD_NAME = "TestGreyRH-";
}
