/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020 SAP SE. All rights reserved.
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
 *
 */

import java.util.concurrent.CyclicBarrier;

import static java.lang.System.currentTimeMillis;

public class MetaspaceTestOneArenaManyThreads extends MetaspaceTestWithThreads {

    // Several threads allocate from a single arena.
    // This mimicks several threads loading classes via the same class loader.

    public MetaspaceTestOneArenaManyThreads(MetaspaceTestContext context, long testAllocationCeiling, int numThreads, int seconds) {
        super(context, testAllocationCeiling, numThreads, seconds);
    }

    public void runTest() throws Exception {

        long t_start = currentTimeMillis();
        long t_stop = t_start + (seconds * 1000);

        // We create a single arena, and n threads which will allocate from that single arena.

        MetaspaceTestArena arena = context.createArena(RandomHelper.fiftyfifty(), testAllocationCeiling);
        CyclicBarrier gate = new CyclicBarrier(numThreads + 1);

        for (int i = 0; i < numThreads; i ++) {
            RandomAllocator allocator = new RandomAllocator(arena);
            RandomAllocatorThread thread = new RandomAllocatorThread(gate, allocator, i);
            threads[i] = thread;
            thread.start();
        }

        gate.await();

        while (System.currentTimeMillis() < t_stop) {
            Thread.sleep(200);
        }

        stopAllThreads();

        context.updateTotals();
        System.out.println("  ## Finished: " + context);

        context.checkStatistics();

        context.destroyArena(arena);

        context.purge();

        context.destroy();

        System.out.println("This took " + (System.currentTimeMillis() - t_start) + "ms");

    }

}

