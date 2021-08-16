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

import java.util.concurrent.TimeoutException;
import sun.hotspot.WhiteBox;

/*
 * @test TestStressRSetCoarsening.java
 * @key stress
 * @bug 8146984 8147087
 * @requires vm.gc.G1
 * @requires os.maxMemory > 3G
 * @requires vm.opt.MaxGCPauseMillis == "null"
 *
 * @summary Stress G1 Remembered Set by creating a lot of cross region links
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm/timeout=300
 *     -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *     -XX:+UseG1GC -Xlog:gc* -XX:MaxGCPauseMillis=1000
 *     -Xmx500m -XX:G1HeapRegionSize=1m gc.stress.TestStressRSetCoarsening  1  0 300
 * @run main/othervm/timeout=300
 *     -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *     -XX:+UseG1GC -Xlog:gc* -XX:MaxGCPauseMillis=1000
 *     -Xmx500m -XX:G1HeapRegionSize=8m gc.stress.TestStressRSetCoarsening  1 10 300
 * @run main/othervm/timeout=300
 *     -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *     -XX:+UseG1GC -Xlog:gc* -XX:MaxGCPauseMillis=1000
 *     -Xmx500m -XX:G1HeapRegionSize=32m gc.stress.TestStressRSetCoarsening 42 10 300
 * @run main/othervm/timeout=300
 *     -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *     -XX:+UseG1GC -Xlog:gc* -XX:MaxGCPauseMillis=1000
 *     -Xmx500m -XX:G1HeapRegionSize=1m gc.stress.TestStressRSetCoarsening  2 0 300
 * @run main/othervm/timeout=1800
 *     -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *     -XX:+UseG1GC -Xlog:gc* -XX:MaxGCPauseMillis=1000
 *     -Xmx1G -XX:G1HeapRegionSize=1m gc.stress.TestStressRSetCoarsening 500 0  1800
 * @run main/othervm/timeout=1800
 *     -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *     -XX:+UseG1GC -Xlog:gc* -XX:MaxGCPauseMillis=1000
 *     -Xmx1G -XX:G1HeapRegionSize=1m gc.stress.TestStressRSetCoarsening 10  10 1800
 */

/**
 * What the test does.
 * Preparation stage:
 *   Fill out ~90% of the heap with objects, each object is an object array.
 *   If we want to allocate K objects per region, we calculate N to meet:
 *      sizeOf(Object[N]) ~= regionSize / K
 * Stress stage:
 *   No more allocation, so no more GC.
 *   We will perform a number of  iterations. On each iteration i,
 *   for each pair of regions Rx and Ry we will set c[i] references
 *   from Rx to Ry. If c[i] less than c[i-1] at the end of iteration
 *   concurrent mark cycle will be initiated (to recalculate remembered sets).
 *   As the result RSet will be growing up and down, up and down many times.
 *
 * The test expects: no crash and no timeouts.
 *
 * Test Parameters:
 *   args[0] - number of objects per Heap Region (1 - means humongous)
 *   args[1] - number of regions to refresh to provoke GC at the end of cycle.
 *             (0 - means no GC, i.e. no reading from RSet)
 *   args[2] - timeout in seconds (to stop execution to avoid jtreg timeout)
 */
public class TestStressRSetCoarsening {

    public static void main(String... args) throws InterruptedException {
        if (args.length != 3) {
            throw new IllegalArgumentException("Wrong number of arguments " + args.length);
        }
        int objectsPerRegion = Integer.parseInt(args[0]); // 1 means humongous
        int regsToRefresh = Integer.parseInt(args[1]);  // 0 means no regions to refresh at the end of cycle
        int timeout = Integer.parseInt(args[2]); // in seconds, test should stop working eariler
        new TestStressRSetCoarsening(objectsPerRegion, regsToRefresh, timeout).go();
    }

    private static final long KB = 1024;
    private static final long MB = 1024 * KB;

    private static final int CARDSIZE = 512; // Card size in bytes.

    private static final WhiteBox WB = WhiteBox.getWhiteBox();

    public final ObjStorage storage;

    /**
     * Number of objects per region. This is a test parameter.
     */
    public final int K;

    /**
     * Length of object array: sizeOf(Object[N]) ~= regionSize / K
     * N will be calculated as function of K.
     */
    public final int N;

    /**
     * How many regions involved into testing.
     * Will be calculated as heapFractionToAllocate * freeRegionCount.
     */
    public final int regionCount;

    /**
     * How much heap to use.
     */
    public final float heapFractionToAllocate = 0.9f;

    /**
     * How many regions to be refreshed at the end of cycle.
     * This is a test parameter.
     */
    public final int regsToRefresh;

    /**
     * Initial time.
     */
    public final long start;

    /**
     * Time when the test should stop working.
     */
    public final long finishAt;

    /**
     * Does pre-calculation and allocate necessary objects.
     *
     * @param objPerRegions how many objects per G1 heap region
     */
    TestStressRSetCoarsening(int objPerRegions, int regsToRefresh, int timeout) {
        this.K = objPerRegions;
        this.regsToRefresh = regsToRefresh;
        this.start = System.currentTimeMillis();
        this.finishAt = start + timeout * 900; // 10% ahead of jtreg timeout

        long regionSize = WB.g1RegionSize();

        // How many free regions
        Runtime rt = Runtime.getRuntime();
        long used = rt.totalMemory() - rt.freeMemory();
        long totalFree = rt.maxMemory() - used;
        regionCount = (int) ((totalFree / regionSize) * heapFractionToAllocate);
        long toAllocate = regionCount * regionSize;
        long freeMemoryLimit = totalFree - toAllocate;

        System.out.println("%% Test parameters");
        System.out.println("%%   Objects per region              : " + K);
        System.out.println("%%   Heap fraction to allocate       : " + (int) (heapFractionToAllocate * 100) + "%");
        System.out.println("%%   Regions to refresh to provoke GC: " + regsToRefresh);

        System.out.println("%% Memory");
        System.out.println("%%   used          :        " + used / MB + "M");
        System.out.println("%%   available     :        " + totalFree / MB + "M");
        System.out.println("%%   to allocate   :        " + toAllocate / MB + "M");
        System.out.println("%%     (in regs)   :        " + regionCount);
        System.out.println("%%   G1 Region Size:        " + regionSize / MB + "M");

        int refSize = WB.getHeapOopSize();

        // Calculate N:    K*sizeOf(Object[N]) ~= regionSize
        //                 sizeOf(Object[N]) ~=  (N+4)*refSize
        // ==>
        //                 N = regionSize / K / refSize - 4;
        int n = (int) ((regionSize / K) / refSize) - 5;  // best guess
        long objSize = WB.getObjectSize(new Object[n]);
        while (K*objSize > regionSize) {   // adjust to avoid OOME
            n = n - 1;
            objSize = WB.getObjectSize(new Object[n]);
        }
        N = n;

        /*
         *   --------------
         *   region0   storage[0]        = new Object[N]
         *             ...
         *             storage[K-1]      = new Object[N]
         *   ---------------
         *   region1   storage[K]        = new Object[N]
         *             ...
         *             storage[2*K - 1]  = new Object[N]
         *   --------------
         *   ...
         *   --------------
         *   regionX   storage[X*K]         = new Object[N]
         *             ...
         *             storage[(X+1)*K -1]  = new Object[N]
         *    where X = HeapFraction * TotalRegions
         *   -------------
         */
        System.out.println("%% Objects");
        System.out.println("%%   N (array length)      : " + N);
        System.out.println("%%   K (objects in regions): " + K);
        System.out.println("%%   Object size           : " + objSize +
                "  (sizeOf(new Object[" + N + "])");
        System.out.println("%%   Reference size        : " + refSize);

        // Maximum number of objects to allocate is regionCount * K.
        storage = new ObjStorage(regionCount * K);

        // Add objects as long as there is space in the storage
        // and we haven't used more memory than planned.
        while (!storage.isFull() && (rt.maxMemory() - used) > freeMemoryLimit) {
            storage.addArray(new Object[N]);
            // Update used memory
            used = rt.totalMemory() - rt.freeMemory();
        }
    }

    public void go() throws InterruptedException {
        // Threshold for Array of Cards -> Howl
        final int ARRAY_TO_HOWL_THRESHOLD = WB.getUintVMFlag("G1RemSetArrayOfCardsEntries").intValue();

        // Threshold for Howl -> Full
        int coarsenHowlToFullPercent = WB.getUintVMFlag("G1RemSetCoarsenHowlToFullPercent").intValue();
        int cardsPerRegion = WB.getSizeTVMFlag("G1HeapRegionSize").intValue() / CARDSIZE;
        final int HOWL_TO_FULL_THRESHOLD = (cardsPerRegion * coarsenHowlToFullPercent) / 100;

        // regToRegRefCounts - array of reference counts from region to region
        // at the the end of iteration.
        // The number of test iterations is array length - 1.
        // If c[i] > c[i-1] then during the iteration i more references will
        // be created.
        // If c[i] < c[i-1] then some referenes will be cleaned.
        int[] regToRegRefCounts = {
            0, ARRAY_TO_HOWL_THRESHOLD / 2,
            0, ARRAY_TO_HOWL_THRESHOLD,
            (ARRAY_TO_HOWL_THRESHOLD + HOWL_TO_FULL_THRESHOLD) / 2, 0,
            HOWL_TO_FULL_THRESHOLD, HOWL_TO_FULL_THRESHOLD + 10,
            ARRAY_TO_HOWL_THRESHOLD + 1, ARRAY_TO_HOWL_THRESHOLD / 2,
            0};

        // For progress tracking
        int[] progress = new int[regToRegRefCounts.length];
        progress[0] = 0;
        for (int i = 1; i < regToRegRefCounts.length; i++) {
            progress[i] = progress[i - 1] + Math.abs(regToRegRefCounts[i] - regToRegRefCounts[i - 1]);
        }
        try {
            for (int i = 1; i < regToRegRefCounts.length; i++) {
                int pre = regToRegRefCounts[i - 1];
                int cur = regToRegRefCounts[i];
                float prog = ((float) progress[i - 1] / progress[progress.length - 1]);

                System.out.println("%% step " + i
                        + " out of " + (regToRegRefCounts.length - 1)
                        + " (~" + (int) (100 * prog) + "% done)");
                System.out.println("%%      " + pre + "  --> " + cur);
                for (int to = 0; to < regionCount; to++) {
                    // Select a celebrity object that we will install references to.
                    // The celebrity will be referred from all other regions.
                    // If the number of references after should be less than they
                    // were before, select NULL.
                    Object celebrity = cur > pre ? storage.getArrayAt(to * K) : null;
                    for (int from = 0; from < regionCount; from++) {
                        if (to == from) {
                            continue; // no need to refer to itself
                        }

                        int step = cur > pre ? +1 : -1;
                        for (int rn = pre; rn != cur; rn += step) {
                            Object[] rnArray = storage.getArrayAt(getY(to, from, rn));
                            rnArray[getX(to, from, rn)] = celebrity;
                            if (System.currentTimeMillis() > finishAt) {
                                throw new TimeoutException();
                            }
                        }
                    }
                }
                if (pre > cur) {
                    // Number of references went down.
                    // Need to provoke recalculation of RSet.
                    WB.g1StartConcMarkCycle();
                    while (WB.g1InConcurrentMark()) {
                        Thread.sleep(1);
                    }
                }

                // To force the use of rememebered set entries we need to provoke a GC.
                // To induce some fragmentation, and some mixed GCs, we need
                // to make a few objects unreachable.
                for (int toClean = i * regsToRefresh; toClean < (i + 1) * regsToRefresh; toClean++) {
                    int to = toClean % regionCount;
                    // Need to remove all references from all regions to the region 'to'
                    for (int from = 0; from < regionCount; from++) {
                        if (to == from) {
                            continue; // no need to refer to itself
                        }
                        for (int rn = 0; rn <= cur; rn++) {
                            Object[] rnArray = storage.getArrayAt(getY(to, from, rn));
                            rnArray[getX(to, from, rn)] = null;
                        }
                    }
                    // 'Refresh' storage elements for the region 'to'
                    // After that loop all 'old' objects in the region 'to'
                    // should become unreachable.
                    for (int k = 0; k < K; k++) {
                        storage.setArrayAt(to * K + k, new Object[N]);
                    }
                }
            }
        } catch (TimeoutException e) {
            System.out.println("%% TIMEOUT!!!");
        }
        long now = System.currentTimeMillis();
        System.out.println("%% Summary");
        System.out.println("%%   Time spent          : " + ((now - start) / 1000) + " seconds");
        System.out.println("%%   Free memory left    : " + Runtime.getRuntime().freeMemory() / KB + "K");
        System.out.println("%% Test passed");
    }

    /**
     * Returns X index in the Storage of the reference #rn from the region
     * 'from' to the region 'to'.
     *
     * @param to region # to refer to
     * @param from region # to refer from
     * @param rn number of reference
     *
     * @return X index in the range: [0 ... N-1]
     */
    private int getX(int to, int from, int rn) {
        return (rn * regionCount + to) % N;
    }

    /**
     * Returns Y index in the Storage of the reference #rn from the region
     * 'from' to the region 'to'.
     *
     * @param to region # to refer to
     * @param from region # to refer from
     * @param rn number of reference
     *
     * @return Y index in the range: [0 ... K*regionCount -1]
     */
    private int getY(int to, int from, int rn) {
        return ((rn * regionCount + to) / N + from * K) % (regionCount * K);
    }
}

//Helper class to encapsulate the object array storage.
class ObjStorage {
    public final Object[][] storage;
    public int usedCount;

    ObjStorage(int size) {
        storage  = new Object[size][];
        usedCount = 0;
    }

    public boolean isFull() {
        return usedCount >= storage.length;
    }

    public void addArray(Object[] objects) {
        if (isFull()) {
            throw new IllegalStateException("Storage full maximum number of allowed elements: " + usedCount);
        }
        storage[usedCount++] = objects;
    }

    // Limit by usedCount since memory limits can cause the storage
    // to have unused slots in the end.
    public void setArrayAt(int i, Object[] objects) {
        storage[i % usedCount] = objects;
    }

    // Limit by usedCount since memory limits can cause the storage
    // to have unused slots in the end.
    public Object[] getArrayAt(int i) {
        return storage[i % usedCount];
    }
}
