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
 * @summary converted from VM Testbase gc/ArrayJuggle/Juggle01.
 * VM Testbase keywords: [gc, stress, stressopt, nonconcurrent, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm -Xlog:gc=debug:gc.log gc.ArrayJuggle.Juggle01.Juggle01 -gp byteArr -ms low
 */

package gc.ArrayJuggle.Juggle01;

import nsk.share.test.*;
import nsk.share.gc.*;
import nsk.share.gc.gp.*;

/**
 * This test randomly replaces elements of an array with new
 * objects using given garbage producer and memory strategy.
 */
public class Juggle01 extends ThreadedGCTest implements GarbageProducerAware, MemoryStrategyAware {
        private GarbageProducer garbageProducer;
        private MemoryStrategy memoryStrategy;
        private Object[] array;
        long objectSize;

        private class Juggler implements Runnable {
                public void run() {
                        synchronized (this) {
                                int index = LocalRandom.nextInt(array.length);
                                array[index] = garbageProducer.create(objectSize);
                        }
                }
        }

        protected Runnable createRunnable(int i) {
                return new Juggler();
        }

        public void run() {
                log.debug("Garbage producer: " + garbageProducer);
                log.debug("Memory strategy: " + memoryStrategy);
                long memory = runParams.getTestMemory();
                int objectCount = memoryStrategy.getCount(memory);
                objectSize = memoryStrategy.getSize(memory);
                log.debug("Object count: " + objectCount);
                log.debug("Object size: " + objectSize);
                array = new Object[objectCount - 1];
                super.run();
        }

        public void setGarbageProducer(GarbageProducer garbageProducer) {
                this.garbageProducer = garbageProducer;
        }

        public void setMemoryStrategy(MemoryStrategy memoryStrategy) {
                this.memoryStrategy = memoryStrategy;
        }

        public static void main(String[] args) {
                GC.runTest(new Juggle01(), args);
        }
}
