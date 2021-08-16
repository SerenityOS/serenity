/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

package gc.stress;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import sun.hotspot.WhiteBox;
import jdk.test.lib.Utils;

/*
 * @test TestMultiThreadStressRSet.java
 * @key stress randomness
 * @requires vm.gc.G1
 * @requires os.maxMemory > 2G
 * @requires vm.opt.MaxGCPauseMillis == "null"
 *
 * @summary Stress G1 Remembered Set using multiple threads
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *   -XX:+UseG1GC -XX:G1SummarizeRSetStatsPeriod=1 -Xlog:gc
 *   -Xmx500m -XX:G1HeapRegionSize=1m -XX:MaxGCPauseMillis=1000 gc.stress.TestMultiThreadStressRSet 10 4
 *
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *   -XX:+UseG1GC -XX:G1SummarizeRSetStatsPeriod=100 -Xlog:gc
 *   -Xmx1G -XX:G1HeapRegionSize=8m -XX:MaxGCPauseMillis=1000 gc.stress.TestMultiThreadStressRSet 60 16
 *
 * @run main/othervm/timeout=700 -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *   -XX:+UseG1GC -XX:G1SummarizeRSetStatsPeriod=100 -Xlog:gc
 *   -Xmx500m -XX:G1HeapRegionSize=1m -XX:MaxGCPauseMillis=1000 gc.stress.TestMultiThreadStressRSet 600 32
 */
public class TestMultiThreadStressRSet {

    private static final WhiteBox WB = WhiteBox.getWhiteBox();
    private static final int REF_SIZE = WB.getHeapOopSize();
    private static final int REGION_SIZE = WB.g1RegionSize();

    // How many regions to use for the storage
    private static final int STORAGE_REGIONS = 20;

    // Size a single obj in the storage
    private static final int OBJ_SIZE = 1024;

    // How many regions of young/old gen to use in the BUFFER
    private static final int BUFFER_YOUNG_REGIONS = 60;
    private static final int BUFFER_OLD_REGIONS = 40;

    // Total number of objects in the storage.
    private final int N;

    // The storage of byte[]
    private final List<Object> STORAGE;

    // Where references to the Storage will be stored
    private final List<Object[]> BUFFER;

    // The length of a buffer element.
    // RSet deals with "cards" (areas of 512 bytes), not with single refs
    // So, to affect the RSet the BUFFER refs should be allocated in different
    // memory cards.
    private final int BUF_ARR_LEN = 100 * (512 / REF_SIZE);

    // Total number of objects in the young/old buffers
    private final int YOUNG;
    private final int OLD;

    // To cause Remembered Sets change their coarse level the test uses a window
    // within STORAGE. All the BUFFER elements refer to only STORAGE objects
    // from the current window. The window is defined by a range.
    // The first element has got the index: 'windowStart',
    // the last one: 'windowStart + windowSize - 1'
    // The window is shifting periodically.
    private int windowStart;
    private final int windowSize;

    // Counter of created worker threads
    private int counter = 0;

    private volatile String errorMessage = null;
    private volatile boolean isEnough = false;

    public static void main(String args[]) {
        if (args.length != 2) {
            throw new IllegalArgumentException("TEST BUG: wrong arg count " + args.length);
        }
        long time = Long.parseLong(args[0]);
        int threads = Integer.parseInt(args[1]);
        new TestMultiThreadStressRSet().test(time * 1000, threads);
    }

    /**
     * Initiates test parameters, fills out the STORAGE and BUFFER.
     */
    public TestMultiThreadStressRSet() {

        N = (REGION_SIZE - 1) * STORAGE_REGIONS / OBJ_SIZE + 1;
        STORAGE = new ArrayList<>(N);
        int bytes = OBJ_SIZE - 20;
        for (int i = 0; i < N - 1; i++) {
            STORAGE.add(new byte[bytes]);
        }
        STORAGE.add(new byte[REGION_SIZE / 2 + 100]); // humongous
        windowStart = 0;
        windowSize = REGION_SIZE / OBJ_SIZE;

        BUFFER = new ArrayList<>();
        int sizeOfBufferObject = 20 + REF_SIZE * BUF_ARR_LEN;
        OLD = REGION_SIZE * BUFFER_OLD_REGIONS / sizeOfBufferObject;
        YOUNG = REGION_SIZE * BUFFER_YOUNG_REGIONS / sizeOfBufferObject;
        for (int i = 0; i < OLD + YOUNG; i++) {
            BUFFER.add(new Object[BUF_ARR_LEN]);
        }
    }

    /**
     * Does the testing. Steps:
     * <ul>
     * <li> starts the Shifter thread
     * <li> during the given time starts new Worker threads, keeping the number
     * of live thread under limit.
     * <li> stops the Shifter thread
     * </ul>
     *
     * @param timeInMillis how long to stress
     * @param maxThreads the maximum number of Worker thread working together.
     */
    public void test(long timeInMillis, int maxThreads) {
        if (timeInMillis <= 0 || maxThreads <= 0) {
            throw new IllegalArgumentException("TEST BUG: be positive!");
        }
        System.out.println("%% Time to work: " + timeInMillis / 1000 + "s");
        System.out.println("%% Number of threads: " + maxThreads);
        long finish = System.currentTimeMillis() + timeInMillis;
        Shifter shift = new Shifter(this, 1000, (int) (windowSize * 0.9));
        shift.start();
        for (int i = 0; i < maxThreads; i++) {
            new Worker(this, 100).start();
        }
        try {
            while (System.currentTimeMillis() < finish && errorMessage == null) {
                Thread.sleep(100);
            }
        } catch (Throwable t) {
            printAllStackTraces(System.err);
            t.printStackTrace(System.err);
            this.errorMessage = t.getMessage();
        } finally {
            isEnough = true;
        }
        System.out.println("%% Total work cycles: " + counter);
        if (errorMessage != null) {
            throw new RuntimeException(errorMessage);
        }
    }

    /**
     * Returns an element from from the BUFFER (an object array) to keep
     * references to the storage.
     *
     * @return an Object[] from buffer.
     */
    private Object[] getFromBuffer() {
        int index = counter % (OLD + YOUNG);
        synchronized (BUFFER) {
            if (index < OLD) {
                if (counter % 100 == (counter / 100) % 100) {
                    // need to generate garbage in the old gen to provoke mixed GC
                    return replaceInBuffer(index);
                } else {
                    return BUFFER.get(index);
                }
            } else {
                return replaceInBuffer(index);
            }
        }
    }

    private Object[] replaceInBuffer(int index) {
        Object[] objs = new Object[BUF_ARR_LEN];
        BUFFER.set(index, objs);
        return objs;
    }

    /**
     * Returns a random object from the current window within the storage.
     * A storage element with index from windowStart to windowStart+windowSize.
     *
     * @return a random element from the current window within the storage.
     */
    private Object getRandomObject(Random rnd) {
        int index = (windowStart + rnd.nextInt(windowSize)) % N;
        return STORAGE.get(index);
    }

    private static void printAllStackTraces(PrintStream ps) {
        Map<Thread, StackTraceElement[]> traces = Thread.getAllStackTraces();
        for (Thread t : traces.keySet()) {
            ps.println(t.toString() + " " + t.getState());
            for (StackTraceElement traceElement : traces.get(t)) {
                ps.println("\tat " + traceElement);
            }
        }
    }

    /**
     * Thread to create a number of references from BUFFER to STORAGE.
     */
    private static class Worker extends Thread {
        final Random rnd;
        final TestMultiThreadStressRSet boss;
        final int refs; // number of refs to OldGen

        /**
         * @param boss the tests
         * @param refsToOldGen how many references to the OldGen to create
         */
        Worker(TestMultiThreadStressRSet boss, int refsToOldGen) {
            this.boss = boss;
            this.refs = refsToOldGen;
            this.rnd = new Random(Utils.getRandomInstance().nextLong());
        }

        @Override
        public void run() {
            try {
                while (!boss.isEnough) {
                    Object[] objs = boss.getFromBuffer();
                    int step = objs.length / refs;
                    for (int i = 0; i < refs; i += step) {
                        objs[i] = boss.getRandomObject(rnd);
                    }
                    boss.counter++;
                }
            } catch (Throwable t) {
                t.printStackTrace(System.out);
                boss.errorMessage = t.getMessage();
            }
        }
    }

    /**
     * Periodically shifts the current STORAGE window, removing references
     * in BUFFER that refer to objects outside the window.
     */
    private static class Shifter extends Thread {

        final TestMultiThreadStressRSet boss;
        final int sleepTime;
        final int shift;

        Shifter(TestMultiThreadStressRSet boss, int sleepTime, int shift) {
            this.boss = boss;
            this.sleepTime = sleepTime;
            this.shift = shift;
        }

        @Override
        public void run() {
            try {
                while (!boss.isEnough) {
                    Thread.sleep(sleepTime);
                    boss.windowStart += shift;
                    for (int i = 0; i < boss.OLD; i++) {
                        Object[] objs = boss.BUFFER.get(i);
                        for (int j = 0; j < objs.length; j++) {
                            objs[j] = null;
                        }
                    }
                    if (!WB.g1InConcurrentMark()) {
                        System.out.println("%% start CMC");
                        WB.g1StartConcMarkCycle();
                    } else {
                        System.out.println("%% CMC is already in progress");
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace(System.out);
                boss.errorMessage = t.getMessage();
            }
        }
    }
}

