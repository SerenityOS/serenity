/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM Testbase gc/gctests/ReferencesGC.
 * VM Testbase keywords: [gc, stress, stressopt, nonconcurrent, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm
 *      -XX:-UseGCOverheadLimit
 *      gc.gctests.ReferencesGC.ReferencesGC
 *      -range 200
 *      -ratio 0.9
 *      -t 1
 */

package gc.gctests.ReferencesGC;

import java.lang.ref.*;
import nsk.share.TestFailure;
import nsk.share.gc.Algorithms;
import nsk.share.gc.GC;
import nsk.share.gc.ThreadedGCTest;
import nsk.share.gc.gp.GarbageProducer;
import nsk.share.gc.gp.GarbageUtils;
import nsk.share.test.ExecutionController;

public class ReferencesGC extends ThreadedGCTest {

    static int RANGE = 256;
    static float RATIO = (float) 1.0;
    static int REMOVE;          // Initialized in parseArgs.
    static int RETAIN;          // Initialized in parseArgs.

    public static void main(String[] args) {
        parseArgs(args);
        GC.runTest(new ReferencesGC(), args);
    }

    public static void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].compareTo("-range") == 0) {
                RANGE = Integer.valueOf(args[++i]).intValue();
            } else if (args[i].compareTo("-ratio") == 0) {
                RATIO = Float.valueOf(args[++i]).floatValue();
            }
        }
        REMOVE = (int) (RANGE * RATIO);
        RETAIN = RANGE - REMOVE;
    }

    private class Worker implements Runnable {

        static final int WEAK = 0;
        static final int SOFT = 1;
        static final int PHANTOM = 2;
        private ExecutionController stresser;
        int finalizationMaxTime = 1000 * 60 * runParams.getNumberOfThreads();
        ReferenceQueue refq = null; // Reinitialized each time through loop
        int[] alive = null;         // Reinitialized each time through loop
        int[] wrong = null;         // Reinitialized each time through loop
        CircularLinkedList holder[] = new CircularLinkedList[RANGE];
        WeakReference wr[] = new WeakReference[RANGE];
        SoftReference sr[] = new SoftReference[RANGE];
        PhantomReference phr[] = new PhantomReference[RANGE];
        GarbageProducer gp = GarbageUtils.getArrayProducers().get(0);
        int iter = 0;

        @Override
        public void run() {
            if (stresser == null) {
                stresser = getExecutionController();
            }

            while (stresser.continueExecution()) {
                int totalLive = 0;
                try {
                    refq = new ReferenceQueue();
                    alive = new int[3];
                    wrong = new int[3];
                    for (int j = 0; j < RANGE; j++) {
                        holder[j] = new CircularLinkedList();
                        holder[j].addNelements(300);
                        wr[j] = new WeakReference(holder[j], refq);
                        sr[j] = new SoftReference(holder[j], refq);
                        phr[j] = new PhantomReference(holder[j], refq);
                    }
                } catch (OutOfMemoryError oome) {
                    // we should just skip the test
                    // the other thread could eat all memory
                    continue;
                }

                for (int i = 0; i < RANGE; i++) {
                    if (wr[i].refersTo(holder[i])) {
                        ++totalLive;
                    }
                    if (sr[i].refersTo(holder[i])) {
                        ++totalLive;
                    }
                    if (phr[i].refersTo(holder[i])) {
                        ++totalLive;
                    }
                }
                if (totalLive != 3 * RANGE) {
                    throw new TestFailure("There are " + (3 * RANGE - totalLive) + " references cleared before null-assigment.");
                }

                for (int i = 0; i < REMOVE; i++) {
                    holder[i] = null;
                }

                Algorithms.eatMemory(stresser);
                if (!stresser.continueExecution()) {
                    break;
                }
                // At this point OOME was thrown and accordingly to spec
                // all weak refs should be processed

                long waitTime = System.currentTimeMillis() + finalizationMaxTime;
                int totalQ = 0;
                while ((totalQ < (3 * REMOVE)) && (System.currentTimeMillis() < waitTime)) {
                    alive[WEAK] = alive[SOFT] = alive[PHANTOM] = 0;
                    wrong[WEAK] = wrong[SOFT] = wrong[PHANTOM] = 0;
                    for (int i = 0; i < RANGE; i++) {
                        if (!wr[i].refersTo(holder[i])) {
                            ++wrong[WEAK];
                        } else if (holder[i] != null) {
                            ++alive[WEAK];
                        }

                        if (!sr[i].refersTo(holder[i])) {
                            ++wrong[SOFT];
                        } else if (holder[i] != null) {
                            ++alive[SOFT];
                        }

                        if (!phr[i].refersTo(holder[i])) {
                            ++wrong[PHANTOM];
                        } else if (holder[i] != null) {
                            ++alive[PHANTOM];
                        }
                    }

                    try {
                        while (refq.remove(100) != null) {
                            ++totalQ;
                        }
                    } catch (InterruptedException ie) {
                    }
                    if (totalQ < (3 * REMOVE)) {
                        log.debug("After null-assignment to " + REMOVE +
                                  " referent values and provoking gc found:\n\t" +
                                  totalQ + " queued refs.");
                        try {
                            log.debug("sleeping to give reference processing more time ...");
                            Thread.sleep(1000);
                        } catch (InterruptedException ie) {
                        }
                    }
                }
                log.debug("iteration.... " + iter++);
                if (wrong[WEAK] != 0) {
                    throw new TestFailure("Expected " + RETAIN + " weak references still alive: " + alive[WEAK]);
                } else if (wrong[SOFT] != 0) {
                    throw new TestFailure("Expected " + RETAIN + " soft references still alive: " + alive[SOFT]);
                } else if (wrong[PHANTOM] != 0) {
                    throw new TestFailure("Expected " + RETAIN + " phantom references still alive: " + alive[PHANTOM]);
                } else if (totalQ != (3 * REMOVE)) {
                    throw new TestFailure("Expected " + (3 * REMOVE) + " references enqueued: " + totalQ);
                }
            }
        }
    }

    @Override
    protected Runnable createRunnable(int i) {
        return new Worker();
    }
}
