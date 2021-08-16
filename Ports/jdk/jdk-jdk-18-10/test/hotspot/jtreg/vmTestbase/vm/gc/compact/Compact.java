/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
package vm.gc.compact;

import java.util.*;

import java.util.concurrent.atomic.AtomicInteger;
import nsk.share.test.*;
import nsk.share.gc.*;
import nsk.share.gc.gp.*;

/**
 * Test garbage collector compaction.
 *
 * The test starts several threads which create objects using
 * given garbage producer until OOM. The references are kept
 * in an array. The references to even elements are cleared
 * and objects of larger size are created until OOM. The garbage
 * collector will have to compact free space to free memory for
 * new objects.
 *
 * The size of the second half of created objects could be set explictly.
 *
 * This process is repeated.
 */
public class Compact extends ThreadedGCTest implements GarbageProducerAware, GarbageProducer1Aware, MemoryStrategyAware {

    private GarbageProducer garbageProducer;
    private GarbageProducer garbageProducer1;
    private MemoryStrategy memoryStrategy;
    private long size;
    private long size2;
    private static long customSize = 0;
    static AtomicInteger allocations = new AtomicInteger();

    private class Worker implements Runnable {

        private List<Object> bricks;
        private ExecutionController stresser;

        public void run() {
            if (stresser == null) {
                stresser = getExecutionController();
            }
            try {
                bricks = new ArrayList<Object>();
                while (stresser.continueExecution()) {
                    bricks.add(garbageProducer.create(size));
                }
            } catch (OutOfMemoryError e) {
            }
            if (bricks == null) {
                return;
            }
            int count = bricks.size();
            for (int i = 0; stresser.continueExecution() && i < count; i += 2) {
                bricks.set(i, null);
            }
            try {
                for (int i = 0; stresser.continueExecution() && i < count; i += 2) {
                    bricks.set(i, garbageProducer1.create(size2));
                    allocations.incrementAndGet();
                }
            } catch (OutOfMemoryError e) {
            }
            bricks = null;
        }
    }

    public Runnable createRunnable(int i) {
        return new Worker();
    }

    public void run() {
        size = memoryStrategy.getSize(runParams.getTestMemory());
        size2 = customSize == 0
                ? size2 = size * 2
                : customSize;
        super.run();
    }

    public final void setGarbageProducer(GarbageProducer garbageProducer) {
        this.garbageProducer = garbageProducer;
    }

    public final void setGarbageProducer1(GarbageProducer garbageProducer1) {
        this.garbageProducer1 = garbageProducer1;
    }

    public final void setMemoryStrategy(MemoryStrategy memoryStrategy) {
        this.memoryStrategy = memoryStrategy;
    }

    static void setHumongousSizeFromParams(String[] args) {
        for (int i = 0; i < args.length; ++i) {
            if (args[i].equals("-size2")) {
                customSize = Long.parseLong(args[i + 1]);
                return;
            }
        }
    }

    public static void main(String[] args) {
        setHumongousSizeFromParams(args);
        GC.runTest(new Compact(), args);
    }
}
