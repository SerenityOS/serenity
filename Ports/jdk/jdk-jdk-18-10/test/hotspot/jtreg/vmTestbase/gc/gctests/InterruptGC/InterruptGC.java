/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM Testbase gc/gctests/InterruptGC.
 * VM Testbase keywords: [gc, stress, stressopt, nonconcurrent]
 * VM Testbase readme:
 * In this test, threads perform garbage collection while constantly
 * interrupting each other. Another thread generates the garbage.
 * The test runs for approximately one minute (see nsk.share.runner.ThreadsRunner
 * and nsk.share.runner.RunParams). The test passes if no exceptions are generated.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm gc.gctests.InterruptGC.InterruptGC -gp random(arrays) -ms low
 */

package gc.gctests.InterruptGC;

import nsk.share.gc.*;
import nsk.share.test.*;
import nsk.share.gc.gp.*;

import java.util.*;

/**
 * The test starts one thread which generates garbage and several other
 * thread which continuously do System.gc() and interrupt each other.
 */
public class InterruptGC extends ThreadedGCTest implements GarbageProducerAware, MemoryStrategyAware {
        private GarbageProducer garbageProducer;
        private MemoryStrategy memoryStrategy;
        private List<Interrupter> interrupters = new ArrayList<Interrupter>();
        private int count;
        private long size;

        private class GarbageCreator implements Runnable {
                public void run() {
                        Object[] arr = new Object[count];
                        for (int i = 0; i < count && getExecutionController().continueExecution(); ++i)
                                arr[i] = garbageProducer.create(size);
                }
        }

        private class Interrupter implements Runnable {
                private Thread thread;

                public void run() {
                        if (thread == null)
                                thread = Thread.currentThread();
                        Interrupter interrupter = interrupters.get(LocalRandom.nextInt(interrupters.size()));
                        Thread thread = interrupter.getThread();
                        if (thread != null)
                                thread.interrupt();
                        System.gc();
                }

                public Thread getThread() {
                        return thread;
                }
        }

        protected Runnable createRunnable(int i) {
                switch (i) {
                case 0:
                        return new GarbageCreator();
                default:
                        Interrupter interrupter = new Interrupter();
                        interrupters.add(interrupter);
                        return interrupter;
                }
        }

        public void run() {
                size = GarbageUtils.getArraySize(runParams.getTestMemory(), memoryStrategy);
                count = GarbageUtils.getArrayCount(runParams.getTestMemory(), memoryStrategy);
                super.run();
        }

        public void setGarbageProducer(GarbageProducer garbageProducer) {
                this.garbageProducer = garbageProducer;
        }

        public void setMemoryStrategy(MemoryStrategy memoryStrategy) {
                this.memoryStrategy = memoryStrategy;
        }

        public static void main(String[] args) {
                GC.runTest(new InterruptGC(), args);
        }
}
