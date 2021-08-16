/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @key stress randomness
 *
 * @summary converted from VM Testbase gc/vector/SimpleGC.
 * VM Testbase keywords: [gc, stress, stressopt, nonconcurrent, monitoring]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm gc.vector.SimpleGC.SimpleGC -ms high
 */

package gc.vector.SimpleGC;

import nsk.share.gc.GC;
import nsk.share.gc.GCTestBase;
import nsk.share.gc.gp.GarbageProducer;
import nsk.share.gc.gp.GarbageProducerAware;
import nsk.share.gc.gp.GarbageUtils;
import nsk.share.gc.gp.MemoryStrategy;
import nsk.share.gc.gp.MemoryStrategyAware;
import nsk.share.runner.RunParams;
import nsk.share.test.Stresser;

/**
 * Test that fills out a certain amount of memory with objects of a given type.
 * The test accepts  MemoryStrategy, type of objects and stress options
 * as command line args.
 */
public class SimpleGC extends GCTestBase implements GarbageProducerAware, MemoryStrategyAware {

    private GarbageProducer garbageProducer;
    private MemoryStrategy memoryStrategy;

    /**
     * Garbage link.
     * The field is static to prevent possible compiler optimizations.
     */
    public static Object[] array;

    @Override
    public void run() {

        long memoryAmount = runParams.getTestMemory();
        long size = GarbageUtils.getArraySize(memoryAmount, memoryStrategy);
        int length = GarbageUtils.getArrayCount(memoryAmount, memoryStrategy);
        Object[] garbage = new Object[length];
        array = garbage;
        log.info("Memory to fill out: " + memoryAmount);
        log.info("Array lenght: " + garbage.length);
        log.info("Object size: " + size);
        log.info("Garbage producer: " + garbageProducer.getClass().getCanonicalName());
        log.info("Memory Strategy: " + memoryStrategy);
        Stresser stresser = new Stresser(runParams.getStressOptions());
        stresser.start(10 * garbage.length);

        int iteration = 0;
        try {
            while (stresser.iteration()) {
                for (int i = 0; i < garbage.length && stresser.continueExecution(); ++i) {
                    garbage[i] = garbageProducer.create(size);
                }
                for (int i = 0; i < garbage.length; ++i) {
                    garbage[i] = null;
                }
                iteration++;
                if (iteration % 1000 == 0) {
                    log.info("  iteration # " + iteration);
                }
            }
        } catch (OutOfMemoryError oom) {
            // normally that should never be thrown
            log.info("OutOfMemory after " + iteration + " iterations");
            throw oom;
        } finally {
            stresser.finish();
        }
        log.info("Success. Total iterations: " + iteration);
    }

    @Override
    public final void setGarbageProducer(GarbageProducer garbageProducer) {
        this.garbageProducer = garbageProducer;
    }

    @Override
    public final void setMemoryStrategy(MemoryStrategy memoryStrategy) {
        this.memoryStrategy = memoryStrategy;
    }

    public static void main(String[] args) {
        RunParams.getInstance().setRunMemDiagThread(true);
        GC.runTest(new SimpleGC(), args);
    }
}
