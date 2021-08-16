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
 * @summary converted from VM Testbase gc/gctests/ThreadGC.
 * VM Testbase keywords: [gc, stress, stressopt, nonconcurrent]
 * VM Testbase readme:
 * This tests attempts to stress the garbage collector my making
 * synchronous calls to the garbage collector after producing garbage.
 * The garbage collector is invoked in a separate thread.
 * The test runs for one minute (see nsk.share.runner.ThreadsRunner and
 * nsk.share.runner.RunParams. It passes if no exceptions are generated.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm gc.gctests.ThreadGC.ThreadGC -gp random(arrays) -ms low
 */

package gc.gctests.ThreadGC;

import nsk.share.gc.*;
import nsk.share.gc.gp.*;
import java.util.*;

public class ThreadGC extends ThreadedGCTest implements GarbageProducerAware, MemoryStrategyAware {
        private GarbageProducer garbageProducer;
        private MemoryStrategy memoryStrategy;
        private Reclaimer reclaimer;
        private int count;
        private long size;

        private class Devourer implements Runnable {
                private Object[] arr = null;
                private int index;

                public void run() {
                        if (arr == null || index >= count) {
                                arr = null;
                                arr = new Object[count];
                                index = 0;
                                synchronized (reclaimer) {
                                        reclaimer.notify();
                                }
                        }
                        arr[index] = garbageProducer.create(size);
                        ++index;
                }
        }

        private class Reclaimer implements Runnable {
                private long waitTime = 1000;

                public void run() {
                        try {
                                synchronized (this) {
                                        this.wait(waitTime);
                                }
                        } catch (InterruptedException e) {
                        }
                        System.gc();
                }
        }

        protected Runnable createRunnable(int i) {
                if (i == 0)
                        return new Devourer();
                else if (i == 1) {
                        reclaimer = new Reclaimer();
                        return reclaimer;
                } else
                        return null;
        }

        public void run() {
                size = GarbageUtils.getArraySize(runParams.getTestMemory(), memoryStrategy);
                count = GarbageUtils.getArrayCount(runParams.getTestMemory(), memoryStrategy);
                runParams.setIterations(count);
                super.run();
        }

        public static void main(String[] args) {
                GC.runTest(new ThreadGC(), args);
        }

        public void setGarbageProducer(GarbageProducer garbageProducer) {
                this.garbageProducer = garbageProducer;
        }

        public void setMemoryStrategy(MemoryStrategy memoryStrategy) {
                this.memoryStrategy = memoryStrategy;
        }

}
