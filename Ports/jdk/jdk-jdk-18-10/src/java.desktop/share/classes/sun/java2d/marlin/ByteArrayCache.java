/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

import static sun.java2d.marlin.ArrayCacheConst.ARRAY_SIZES;
import static sun.java2d.marlin.ArrayCacheConst.BUCKETS;
import static sun.java2d.marlin.ArrayCacheConst.MAX_ARRAY_SIZE;
import static sun.java2d.marlin.MarlinUtils.logInfo;
import static sun.java2d.marlin.MarlinUtils.logException;

import java.lang.ref.WeakReference;
import java.util.Arrays;

import sun.java2d.marlin.ArrayCacheConst.BucketStats;
import sun.java2d.marlin.ArrayCacheConst.CacheStats;

/*
 * Note that the [BYTE/INT/FLOAT/DOUBLE]ArrayCache files are nearly identical except
 * for a few type and name differences. Typically, the [BYTE]ArrayCache.java file
 * is edited manually and then [INT/FLOAT/DOUBLE]ArrayCache.java
 * files are generated with the following command lines:
 */
// % sed -e 's/(b\yte)[ ]*//g' -e 's/b\yte/int/g' -e 's/B\yte/Int/g' < B\yteArrayCache.java > IntArrayCache.java
// % sed -e 's/(b\yte)[ ]*0/0.0f/g' -e 's/(b\yte)[ ]*/(float) /g' -e 's/b\yte/float/g' -e 's/B\yte/Float/g' < B\yteArrayCache.java > FloatArrayCache.java
// % sed -e 's/(b\yte)[ ]*0/0.0d/g' -e 's/(b\yte)[ ]*/(double) /g' -e 's/b\yte/double/g' -e 's/B\yte/Double/g' < B\yteArrayCache.java > DoubleArrayCache.java

final class ByteArrayCache implements MarlinConst {

    final boolean clean;
    private final int bucketCapacity;
    private WeakReference<Bucket[]> refBuckets = null;
    final CacheStats stats;

    ByteArrayCache(final boolean clean, final int bucketCapacity) {
        this.clean = clean;
        this.bucketCapacity = bucketCapacity;
        this.stats = (DO_STATS) ?
            new CacheStats(getLogPrefix(clean) + "ByteArrayCache") : null;
    }

    Bucket getCacheBucket(final int length) {
        final int bucket = ArrayCacheConst.getBucket(length);
        return getBuckets()[bucket];
    }

    private Bucket[] getBuckets() {
        // resolve reference:
        Bucket[] buckets = (refBuckets != null) ? refBuckets.get() : null;

        // create a new buckets ?
        if (buckets == null) {
            buckets = new Bucket[BUCKETS];

            for (int i = 0; i < BUCKETS; i++) {
                buckets[i] = new Bucket(clean, ARRAY_SIZES[i], bucketCapacity,
                        (DO_STATS) ? stats.bucketStats[i] : null);
            }

            // update weak reference:
            refBuckets = new WeakReference<Bucket[]>(buckets);
        }
        return buckets;
    }

    Reference createRef(final int initialSize) {
        return new Reference(this, initialSize);
    }

    static final class Reference {

        // initial array reference (direct access)
        final byte[] initial;
        private final boolean clean;
        private final ByteArrayCache cache;

        Reference(final ByteArrayCache cache, final int initialSize) {
            this.cache = cache;
            this.clean = cache.clean;
            this.initial = createArray(initialSize);
            if (DO_STATS) {
                cache.stats.totalInitial += initialSize;
            }
        }

        byte[] getArray(final int length) {
            if (length <= MAX_ARRAY_SIZE) {
                return cache.getCacheBucket(length).getArray();
            }
            if (DO_STATS) {
                cache.stats.oversize++;
            }
            if (DO_LOG_OVERSIZE) {
                logInfo(getLogPrefix(clean) + "ByteArrayCache: "
                        + "getArray[oversize]: length=\t" + length);
            }
            return createArray(length);
        }

        byte[] widenArray(final byte[] array, final int usedSize,
                          final int needSize)
        {
            final int length = array.length;
            if (DO_CHECKS && length >= needSize) {
                return array;
            }
            if (DO_STATS) {
                cache.stats.resize++;
            }

            // maybe change bucket:
            // ensure getNewSize() > newSize:
            final byte[] res = getArray(ArrayCacheConst.getNewSize(usedSize, needSize));

            // use wrapper to ensure proper copy:
            System.arraycopy(array, 0, res, 0, usedSize); // copy only used elements

            // maybe return current array:
            putArray(array, 0, usedSize); // ensure array is cleared

            if (DO_LOG_WIDEN_ARRAY) {
                logInfo(getLogPrefix(clean) + "ByteArrayCache: "
                        + "widenArray[" + res.length
                        + "]: usedSize=\t" + usedSize + "\tlength=\t" + length
                        + "\tneeded length=\t" + needSize);
            }
            return res;
        }

        byte[] putArray(final byte[] array)
        {
            // dirty array helper:
            return putArray(array, 0, array.length);
        }

        byte[] putArray(final byte[] array, final int fromIndex,
                        final int toIndex)
        {
            if (array.length <= MAX_ARRAY_SIZE) {
                if ((clean || DO_CLEAN_DIRTY) && (toIndex != 0)) {
                    // clean-up array of dirty part[fromIndex; toIndex[
                    fill(array, fromIndex, toIndex, (byte)0);
                }
                // ensure to never store initial arrays in cache:
                if (array != initial) {
                    cache.getCacheBucket(array.length).putArray(array);
                }
            }
            return initial;
        }
    }

    static final class Bucket {

        private int tail = 0;
        private final int arraySize;
        private final boolean clean;
        private final byte[][] arrays;
        private final BucketStats stats;

        Bucket(final boolean clean, final int arraySize,
               final int capacity, final BucketStats stats)
        {
            this.arraySize = arraySize;
            this.clean = clean;
            this.stats = stats;
            this.arrays = new byte[capacity][];
        }

        byte[] getArray() {
            if (DO_STATS) {
                stats.getOp++;
            }
            // use cache:
            if (tail != 0) {
                final byte[] array = arrays[--tail];
                arrays[tail] = null;
                return array;
            }
            if (DO_STATS) {
                stats.createOp++;
            }
            return createArray(arraySize);
        }

        void putArray(final byte[] array)
        {
            if (DO_CHECKS && (array.length != arraySize)) {
                logInfo(getLogPrefix(clean) + "ByteArrayCache: "
                        + "bad length = " + array.length);
                return;
            }
            if (DO_STATS) {
                stats.returnOp++;
            }
            // fill cache:
            if (arrays.length > tail) {
                arrays[tail++] = array;

                if (DO_STATS) {
                    stats.updateMaxSize(tail);
                }
            } else if (DO_CHECKS) {
                logInfo(getLogPrefix(clean) + "ByteArrayCache: "
                        + "array capacity exceeded !");
            }
        }
    }

    static byte[] createArray(final int length) {
        return new byte[length];
    }

    static void fill(final byte[] array, final int fromIndex,
                     final int toIndex, final byte value)
    {
        // clear array data:
        Arrays.fill(array, fromIndex, toIndex, value);
        if (DO_CHECKS) {
            check(array, fromIndex, toIndex, value);
        }
    }

    static void check(final byte[] array, final int fromIndex,
                      final int toIndex, final byte value)
    {
        if (DO_CHECKS) {
            // check zero on full array:
            for (int i = 0; i < array.length; i++) {
                if (array[i] != value) {
                    logException("Invalid value at: " + i + " = " + array[i]
                            + " from: " + fromIndex + " to: " + toIndex + "\n"
                            + Arrays.toString(array), new Throwable());

                    // ensure array is correctly filled:
                    Arrays.fill(array, value);

                    return;
                }
            }
        }
    }

    static String getLogPrefix(final boolean clean) {
        return (clean) ? "Clean" : "Dirty";
    }
}
