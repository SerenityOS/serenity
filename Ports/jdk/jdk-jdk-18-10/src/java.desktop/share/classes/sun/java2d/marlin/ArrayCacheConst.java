/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package sun.java2d.marlin;

import java.util.Arrays;
import static sun.java2d.marlin.MarlinUtils.logInfo;

public final class ArrayCacheConst implements MarlinConst {

    static final int BUCKETS = 8;
    static final int MIN_ARRAY_SIZE = 4096;
    // maximum array size
    static final int MAX_ARRAY_SIZE;
    // threshold below to grow arrays by 4
    static final int THRESHOLD_SMALL_ARRAY_SIZE = 4 * 1024 * 1024;
    // threshold to grow arrays only by (3/2) instead of 2
    static final int THRESHOLD_ARRAY_SIZE;
    // threshold to grow arrays only by (5/4) instead of (3/2)
    static final long THRESHOLD_HUGE_ARRAY_SIZE;
    static final int[] ARRAY_SIZES = new int[BUCKETS];

    static {
        // initialize buckets for int/float arrays
        int arraySize = MIN_ARRAY_SIZE;

        int inc_lg = 2; // x4

        for (int i = 0; i < BUCKETS; i++, arraySize <<= inc_lg) {
            ARRAY_SIZES[i] = arraySize;

            if (DO_TRACE) {
                logInfo("arraySize[" + i + "]: " + arraySize);
            }

            if (arraySize >= THRESHOLD_SMALL_ARRAY_SIZE) {
                inc_lg = 1; // x2
            }
        }
        MAX_ARRAY_SIZE = arraySize >> inc_lg;

        if (MAX_ARRAY_SIZE <= 0) {
            throw new IllegalStateException("Invalid max array size !");
        }

        THRESHOLD_ARRAY_SIZE       =  16  * 1024 * 1024; // >16M
        THRESHOLD_HUGE_ARRAY_SIZE  =  48L * 1024 * 1024; // >48M

        if (DO_STATS || DO_MONITORS) {
            logInfo("ArrayCache.BUCKETS        = " + BUCKETS);
            logInfo("ArrayCache.MIN_ARRAY_SIZE = " + MIN_ARRAY_SIZE);
            logInfo("ArrayCache.MAX_ARRAY_SIZE = " + MAX_ARRAY_SIZE);
            logInfo("ArrayCache.ARRAY_SIZES = "
                    + Arrays.toString(ARRAY_SIZES));
            logInfo("ArrayCache.THRESHOLD_ARRAY_SIZE = "
                    + THRESHOLD_ARRAY_SIZE);
            logInfo("ArrayCache.THRESHOLD_HUGE_ARRAY_SIZE = "
                    + THRESHOLD_HUGE_ARRAY_SIZE);
        }
    }

    private ArrayCacheConst() {
        // Utility class
    }

    // small methods used a lot (to be inlined / optimized by hotspot)

    static int getBucket(final int length) {
        for (int i = 0; i < ARRAY_SIZES.length; i++) {
            if (length <= ARRAY_SIZES[i]) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Return the new array size (~ x2)
     * @param curSize current used size
     * @param needSize needed size
     * @return new array size
     */
    public static int getNewSize(final int curSize, final int needSize) {
        // check if needSize is negative or integer overflow:
        if (needSize < 0) {
            // hard overflow failure - we can't even accommodate
            // new items without overflowing
            throw new ArrayIndexOutOfBoundsException(
                          "array exceeds maximum capacity !");
        }
        assert curSize >= 0;
        final int initial = curSize;
        int size;
        if (initial > THRESHOLD_ARRAY_SIZE) {
            size = initial + (initial >> 1); // x(3/2)
        } else {
            size = (initial << 1); // x2
        }
        // ensure the new size is >= needed size:
        if (size < needSize) {
            // align to 4096 (may overflow):
            size = ((needSize >> 12) + 1) << 12;
        }
        // check integer overflow:
        if (size < 0) {
            // resize to maximum capacity:
            size = Integer.MAX_VALUE;
        }
        return size;
    }

    /**
     * Return the new array size (~ x2)
     * @param curSize current used size
     * @param needSize needed size
     * @return new array size
     */
    public static long getNewLargeSize(final long curSize, final long needSize) {
        // check if needSize is negative or integer overflow:
        if ((needSize >> 31L) != 0L) {
            // hard overflow failure - we can't even accommodate
            // new items without overflowing
            throw new ArrayIndexOutOfBoundsException(
                          "array exceeds maximum capacity !");
        }
        assert curSize >= 0L;
        long size;
        if (curSize > THRESHOLD_HUGE_ARRAY_SIZE) {
            size = curSize + (curSize >> 2L); // x(5/4)
        } else if (curSize > THRESHOLD_ARRAY_SIZE) {
            size = curSize + (curSize >> 1L); // x(3/2)
        } else if (curSize > THRESHOLD_SMALL_ARRAY_SIZE) {
            size = (curSize << 1L); // x2
        } else {
            size = (curSize << 2L); // x4
        }
        // ensure the new size is >= needed size:
        if (size < needSize) {
            // align to 4096:
            size = ((needSize >> 12L) + 1L) << 12L;
        }
        // check integer overflow:
        if (size > Integer.MAX_VALUE) {
            // resize to maximum capacity:
            size = Integer.MAX_VALUE;
        }
        return size;
    }

    static final class CacheStats {
        final String name;
        final BucketStats[] bucketStats;
        int resize = 0;
        int oversize = 0;
        long totalInitial = 0L;

        CacheStats(final String name) {
            this.name = name;

            bucketStats = new BucketStats[BUCKETS];
            for (int i = 0; i < BUCKETS; i++) {
                bucketStats[i] = new BucketStats();
            }
        }

        void reset() {
            resize = 0;
            oversize = 0;

            for (int i = 0; i < BUCKETS; i++) {
                bucketStats[i].reset();
            }
        }

        long dumpStats() {
            long totalCacheBytes = 0L;

            if (DO_STATS) {
                for (int i = 0; i < BUCKETS; i++) {
                    final BucketStats s = bucketStats[i];

                    if (s.maxSize != 0) {
                        totalCacheBytes += getByteFactor()
                                           * (s.maxSize * ARRAY_SIZES[i]);
                    }
                }

                if (totalInitial != 0L || totalCacheBytes != 0L
                    || resize != 0 || oversize != 0)
                {
                    logInfo(name + ": resize: " + resize
                            + " - oversize: " + oversize
                            + " - initial: " + getTotalInitialBytes()
                            + " bytes (" + totalInitial + " elements)"
                            + " - cache: " + totalCacheBytes + " bytes"
                    );
                }

                if (totalCacheBytes != 0L) {
                    logInfo(name + ": usage stats:");

                    for (int i = 0; i < BUCKETS; i++) {
                        final BucketStats s = bucketStats[i];

                        if (s.getOp != 0) {
                            logInfo("  Bucket[" + ARRAY_SIZES[i] + "]: "
                                    + "get: " + s.getOp
                                    + " - put: " + s.returnOp
                                    + " - create: " + s.createOp
                                    + " :: max size: " + s.maxSize
                            );
                        }
                    }
                }
            }
            return totalCacheBytes;
        }

        private int getByteFactor() {
            int factor = 1;
            if (name.contains("Int") || name.contains("Float")) {
                factor = 4;
            } else if (name.contains("Double")) {
                factor = 8;
            }
            return factor;
        }

        long getTotalInitialBytes() {
            return getByteFactor() * totalInitial;
        }
    }

    static final class BucketStats {
        int getOp = 0;
        int createOp = 0;
        int returnOp = 0;
        int maxSize = 0;

        void reset() {
            getOp = 0;
            createOp = 0;
            returnOp = 0;
            maxSize = 0;
        }

        void updateMaxSize(final int size) {
            if (size > maxSize) {
                maxSize = size;
            }
        }
    }
}
