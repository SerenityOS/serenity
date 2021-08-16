/*
 * Copyright 2015 Goldman Sachs.
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class SortingLongBenchmarkTestJMH {
    private static final int QUICKSORT_THRESHOLD = 286;
    private static final int MAX_RUN_COUNT = 67;
    private static final int INSERTION_SORT_THRESHOLD = 47;
    public static final int MAX_VALUE = 1_000_000;

    @Param({"pairFlipZeroPairFlip", "descendingAscending", "zeroHi", "hiZeroLow", "hiFlatLow", "identical",
            "randomDups", "randomNoDups", "sortedReversedSorted", "pairFlip", "endLessThan"})
    public String listType;

    private long[] array;
    private static final int LIST_SIZE = 10_000_000;
    public static final int NUMBER_OF_ITERATIONS = 10;

    @Setup
    public void setUp() {
        Random random = new Random(123456789012345L);
        this.array = new long[LIST_SIZE];
        int threeQuarters = (int) (LIST_SIZE * 0.75);
        if ("zeroHi".equals(this.listType)) {
            for (int i = 0; i < threeQuarters; i++) {
                this.array[i] = 0;
            }
            int k = 1;
            for (int i = threeQuarters; i < LIST_SIZE; i++) {
                this.array[i] = k;
                k++;
            }
        }
        else if ("hiFlatLow".equals(this.listType)) {
            int oneThird = LIST_SIZE / 3;
            for (int i = 0; i < oneThird; i++) {
                this.array[i] = i;
            }
            int twoThirds = oneThird * 2;
            int constant = oneThird - 1;
            for (int i = oneThird; i < twoThirds; i++) {
                this.array[i] = constant;
            }
            for (int i = twoThirds; i < LIST_SIZE; i++) {
                this.array[i] = constant - i + twoThirds;
            }
        }
        else if ("hiZeroLow".equals(this.listType)) {
            int oneThird = LIST_SIZE / 3;
            for (int i = 0; i < oneThird; i++) {
                this.array[i] = i;
            }
            int twoThirds = oneThird * 2;
            for (int i = oneThird; i < twoThirds; i++) {
                this.array[i] = 0;
            }
            for (int i = twoThirds; i < LIST_SIZE; i++) {
                this.array[i] = oneThird - i + twoThirds;
            }
        }
        else if ("identical".equals(this.listType)) {
            for (int i = 0; i < LIST_SIZE; i++) {
                this.array[i] = 0;
            }
        }
        else if ("randomDups".equals(this.listType)) {
            for (int i = 0; i < LIST_SIZE; i++) {
                this.array[i] = random.nextInt(1000);
            }
        }
        else if ("randomNoDups".equals(this.listType)) {
            Set<Integer> set = new HashSet<>();
            while (set.size() < LIST_SIZE + 1) {
                set.add(random.nextInt());
            }
            List<Integer> list = new ArrayList<>(LIST_SIZE);
            list.addAll(set);
            for (int i = 0; i < LIST_SIZE; i++) {
                this.array[i] = list.get(i);
            }
        }
        else if ("sortedReversedSorted".equals(this.listType)) {
            for (int i = 0; i < LIST_SIZE / 2; i++) {
                this.array[i] = i;
            }
            int num = 0;
            for (int i = LIST_SIZE / 2; i < LIST_SIZE; i++) {
                this.array[i] = LIST_SIZE - num;
                num++;
            }
        }
        else if ("pairFlip".equals(this.listType)) {
            for (int i = 0; i < LIST_SIZE; i++) {
                this.array[i] = i;
            }
            for (int i = 0; i < LIST_SIZE; i += 2) {
                long temp = this.array[i];
                this.array[i] = this.array[i + 1];
                this.array[i + 1] = temp;
            }
        }
        else if ("endLessThan".equals(this.listType)) {
            for (int i = 0; i < LIST_SIZE - 1; i++) {
                this.array[i] = 3;
            }
            this.array[LIST_SIZE - 1] = 1;
        }
        else if ("pairFlipZeroPairFlip".equals(this.listType)) {
            //pairflip
            for (int i = 0; i < 64; i++) {
                this.array[i] = i;
            }
            for (int i = 0; i < 64; i += 2) {
                long temp = this.array[i];
                this.array[i] = this.array[i + 1];
                this.array[i + 1] = temp;
            }
            //zero
            for (int i = 64; i < this.array.length - 64; i++) {
                this.array[i] = 0;
            }
            //pairflip
            for (int i = this.array.length - 64; i < this.array.length; i++) {
                this.array[i] = i;
            }
            for (int i = this.array.length - 64; i < this.array.length; i += 2) {
                long temp = this.array[i];
                this.array[i] = this.array[i + 1];
                this.array[i + 1] = temp;
            }
        }
        else if ("pairFlipOneHundredPairFlip".equals(this.listType)) {
            //10, 5
            for (int i = 0; i < 64; i++) {
                if (i % 2 == 0) {
                    this.array[i] = 10;
                }
                else {
                    this.array[i] = 5;
                }
            }

            //100
            for (int i = 64; i < this.array.length - 64; i++) {
                this.array[i] = 100;
            }

            //10, 5
            for (int i = this.array.length - 64; i < this.array.length; i++) {
                if (i % 2 == 0) {
                    this.array[i] = 10;
                }
                else {
                    this.array[i] = 5;
                }
            }
        }
    }

    @Warmup(iterations = 20)
    @Measurement(iterations = 10)
    @Benchmark
    public void sortNewWay() {
        for (int i = 0; i < NUMBER_OF_ITERATIONS; i++) {
            SortingLongTestJMH.sort(this.array, 0, this.array.length - 1, null, 0, 0);
        }
    }

    @Warmup(iterations = 20)
    @Measurement(iterations = 10)
    @Benchmark
    public void sortOldWay() {
        for (int i = 0; i < NUMBER_OF_ITERATIONS; i++) {
            Arrays.sort(this.array);
        }
    }

    /**
     * Sorts the specified range of the array using the given
     * workspace array slice if possible for merging
     *
     * @param a the array to be sorted
     * @param left the index of the first element, inclusive, to be sorted
     * @param right the index of the last element, inclusive, to be sorted
     * @param work a workspace array (slice)
     * @param workBase origin of usable space in work array
     * @param workLen usable size of work array
     */
    static void sort(long[] a, int left, int right,
                     long[] work, int workBase, int workLen) {
// Use Quicksort on small arrays
        if (right - left < QUICKSORT_THRESHOLD) {
            SortingLongTestJMH.sort(a, left, right, true);
            return;
        }

          /*
         * Index run[i] is the start of i-th run
         * (ascending or descending sequence).
         */
        int[] run = new int[MAX_RUN_COUNT + 1];
        int count = 0;
        run[0] = left;

        // Check if the array is nearly sorted
        for (int k = left; k < right; run[count] = k) {
            while (k < right && a[k] == a[k + 1])
                k++;
            if (k == right) break;
            if (a[k] < a[k + 1]) { // ascending
                while (++k <= right && a[k - 1] <= a[k]) ;
            }
            else if (a[k] > a[k + 1]) { // descending
                while (++k <= right && a[k - 1] >= a[k]) ;
                for (int lo = run[count] - 1, hi = k; ++lo < --hi; ) {
                    long t = a[lo];
                    a[lo] = a[hi];
                    a[hi] = t;
                }
            }
            if (run[count] > left && a[run[count]] >= a[run[count] - 1]) {
                count--;
            }
            /*
             * The array is not highly structured,
             * use Quicksort instead of merge sort.
             */
            if (++count == MAX_RUN_COUNT) {
                sort(a, left, right, true);
                return;
            }
        }

        // Check special cases
        // Implementation note: variable "right" is increased by 1.
        if (run[count] == right++) {
            run[++count] = right;
        }
        if (count <= 1) { // The array is already sorted
            return;
        }

        // Determine alternation base for merge
        byte odd = 0;
        for (int n = 1; (n <<= 1) < count; odd ^= 1) {
        }

        // Use or create temporary array b for merging
        long[] b;                  // temp array; alternates with a
        int ao, bo;                 // array offsets from 'left'
        int blen = right - left; // space needed for b
        if (work == null || workLen < blen || workBase + blen > work.length) {
            work = new long[blen];
            workBase = 0;
        }
        if (odd == 0) {
            System.arraycopy(a, left, work, workBase, blen);
            b = a;
            bo = 0;
            a = work;
            ao = workBase - left;
        }
        else {
            b = work;
            ao = 0;
            bo = workBase - left;
        }

        // Merging
        for (int last; count > 1; count = last) {
            for (int k = (last = 0) + 2; k <= count; k += 2) {
                int hi = run[k], mi = run[k - 1];
                for (int i = run[k - 2], p = i, q = mi; i < hi; ++i) {
                    if (q >= hi || p < mi && a[p + ao] <= a[q + ao]) {
                        b[i + bo] = a[p++ + ao];
                    }
                    else {
                        b[i + bo] = a[q++ + ao];
                    }
                }
                run[++last] = hi;
            }
            if ((count & 1) != 0) {
                for (int i = right, lo = run[count - 1]; --i >= lo;
                     b[i + bo] = a[i + ao]
                        ) {
                }
                run[++last] = right;
            }
            long[] t = a;
            a = b;
            b = t;
            int o = ao;
            ao = bo;
            bo = o;
        }
    }

    /**
     * Sorts the specified range of the array by Dual-Pivot Quicksort.
     *
     * @param a the array to be sorted
     * @param left the index of the first element, inclusive, to be sorted
     * @param right the index of the last element, inclusive, to be sorted
     * @param leftmost indicates if this part is the leftmost in the range
     */
    private static void sort(long[] a, int left, int right, boolean leftmost) {
        int length = right - left + 1;

        // Use insertion sort on tiny arrays
        if (length < INSERTION_SORT_THRESHOLD) {
            if (leftmost) {
                /*
                 * Traditional (without sentinel) insertion sort,
                 * optimized for server VM, is used in case of
                 * the leftmost part.
                 */
                for (int i = left, j = i; i < right; j = ++i) {
                    long ai = a[i + 1];
                    while (ai < a[j]) {
                        a[j + 1] = a[j];
                        if (j-- == left) {
                            break;
                        }
                    }
                    a[j + 1] = ai;
                }
            }
            else {
                /*
                 * Skip the longest ascending sequence.
                 */
                do {
                    if (left >= right) {
                        return;
                    }
                }
                while (a[++left] >= a[left - 1]);

                /*
                 * Every element from adjoining part plays the role
                 * of sentinel, therefore this allows us to avoid the
                 * left range check on each iteration. Moreover, we use
                 * the more optimized algorithm, so called pair insertion
                 * sort, which is faster (in the context of Quicksort)
                 * than traditional implementation of insertion sort.
                 */
                for (int k = left; ++left <= right; k = ++left) {
                    long a1 = a[k], a2 = a[left];

                    if (a1 < a2) {
                        a2 = a1;
                        a1 = a[left];
                    }
                    while (a1 < a[--k]) {
                        a[k + 2] = a[k];
                    }
                    a[++k + 1] = a1;

                    while (a2 < a[--k]) {
                        a[k + 1] = a[k];
                    }
                    a[k + 1] = a2;
                }
                long last = a[right];

                while (last < a[--right]) {
                    a[right + 1] = a[right];
                }
                a[right + 1] = last;
            }
            return;
        }

        // Inexpensive approximation of length / 7
        int seventh = (length >> 3) + (length >> 6) + 1;

        /*
         * Sort five evenly spaced elements around (and including) the
         * center element in the range. These elements will be used for
         * pivot selection as described below. The choice for spacing
         * these elements was empirically determined to work well on
         * a wide variety of inputs.
         */
        int e3 = (left + right) >>> 1; // The midpoint
        int e2 = e3 - seventh;
        int e1 = e2 - seventh;
        int e4 = e3 + seventh;
        int e5 = e4 + seventh;

        // Sort these elements using insertion sort
        if (a[e2] < a[e1]) {
            long t = a[e2];
            a[e2] = a[e1];
            a[e1] = t;
        }

        if (a[e3] < a[e2]) {
            long t = a[e3];
            a[e3] = a[e2];
            a[e2] = t;
            if (t < a[e1]) {
                a[e2] = a[e1];
                a[e1] = t;
            }
        }
        if (a[e4] < a[e3]) {
            long t = a[e4];
            a[e4] = a[e3];
            a[e3] = t;
            if (t < a[e2]) {
                a[e3] = a[e2];
                a[e2] = t;
                if (t < a[e1]) {
                    a[e2] = a[e1];
                    a[e1] = t;
                }
            }
        }
        if (a[e5] < a[e4]) {
            long t = a[e5];
            a[e5] = a[e4];
            a[e4] = t;
            if (t < a[e3]) {
                a[e4] = a[e3];
                a[e3] = t;
                if (t < a[e2]) {
                    a[e3] = a[e2];
                    a[e2] = t;
                    if (t < a[e1]) {
                        a[e2] = a[e1];
                        a[e1] = t;
                    }
                }
            }
        }

        // Pointers
        int less = left;  // The index of the first element of center part
        int great = right; // The index before the first element of right part

        if (a[e1] != a[e2] && a[e2] != a[e3] && a[e3] != a[e4] && a[e4] != a[e5]) {
            /*
             * Use the second and fourth of the five sorted elements as pivots.
             * These values are inexpensive approximations of the first and
             * second terciles of the array. Note that pivot1 <= pivot2.
             */
            long pivot1 = a[e2];
            long pivot2 = a[e4];

            /*
             * The first and the last elements to be sorted are moved to the
             * locations formerly occupied by the pivots. When partitioning
             * is complete, the pivots are swapped back into their final
             * positions, and excluded from subsequent sorting.
             */
            a[e2] = a[left];
            a[e4] = a[right];

            /*
             * Skip elements, which are less or greater than pivot values.
             */
            while (a[++less] < pivot1) {
            }
            while (a[--great] > pivot2) {
            }

            /*
             * Partitioning:
             *
             *   left part           center part                   right part
             * +--------------------------------------------------------------+
             * |  < pivot1  |  pivot1 <= && <= pivot2  |    ?    |  > pivot2  |
             * +--------------------------------------------------------------+
             *               ^                          ^       ^
             *               |                          |       |
             *              less                        k     great
             *
             * Invariants:
             *
             *              all in (left, less)   < pivot1
             *    pivot1 <= all in [less, k)      <= pivot2
             *              all in (great, right) > pivot2
             *
             * Pointer k is the first index of ?-part.
             */
            outer:
            for (int k = less - 1; ++k <= great; ) {
                long ak = a[k];
                if (ak < pivot1) { // Move a[k] to left part
                    a[k] = a[less];
                    /*
                     * Here and below we use "a[i] = b; i++;" instead
                     * of "a[i++] = b;" due to performance issue.
                     */
                    a[less] = ak;
                    ++less;
                }
                else if (ak > pivot2) { // Move a[k] to right part
                    while (a[great] > pivot2) {
                        if (great-- == k) {
                            break outer;
                        }
                    }
                    if (a[great] < pivot1) { // a[great] <= pivot2
                        a[k] = a[less];
                        a[less] = a[great];
                        ++less;
                    }
                    else { // pivot1 <= a[great] <= pivot2
                        a[k] = a[great];
                    }
                    /*
                     * Here and below we use "a[i] = b; i--;" instead
                     * of "a[i--] = b;" due to performance issue.
                     */
                    a[great] = ak;
                    --great;
                }
            }

            // Swap pivots into their final positions
            a[left] = a[less - 1];
            a[less - 1] = pivot1;
            a[right] = a[great + 1];
            a[great + 1] = pivot2;

            // Sort left and right parts recursively, excluding known pivots
            SortingLongTestJMH.sort(a, left, less - 2, leftmost);
            SortingLongTestJMH.sort(a, great + 2, right, false);

            /*
             * If center part is too large (comprises > 4/7 of the array),
             * swap internal pivot values to ends.
             */
            if (less < e1 && e5 < great) {
                /*
                 * Skip elements, which are equal to pivot values.
                 */
                while (a[less] == pivot1) {
                    ++less;
                }

                while (a[great] == pivot2) {
                    --great;
                }

                /*
                 * Partitioning:
                 *
                 *   left part         center part                  right part
                 * +----------------------------------------------------------+
                 * | == pivot1 |  pivot1 < && < pivot2  |    ?    | == pivot2 |
                 * +----------------------------------------------------------+
                 *              ^                        ^       ^
                 *              |                        |       |
                 *             less                      k     great
                 *
                 * Invariants:
                 *
                 *              all in (*,  less) == pivot1
                 *     pivot1 < all in [less,  k)  < pivot2
                 *              all in (great, *) == pivot2
                 *
                 * Pointer k is the first index of ?-part.
                 */
                outer:
                for (int k = less - 1; ++k <= great; ) {
                    long ak = a[k];
                    if (ak == pivot1) { // Move a[k] to left part
                        a[k] = a[less];
                        a[less] = ak;
                        ++less;
                    }
                    else if (ak == pivot2) { // Move a[k] to right part
                        while (a[great] == pivot2) {
                            if (great-- == k) {
                                break outer;
                            }
                        }
                        if (a[great] == pivot1) { // a[great] < pivot2
                            a[k] = a[less];
                            /*
                             * Even though a[great] equals to pivot1, the
                             * assignment a[less] = pivot1 may be incorrect,
                             * if a[great] and pivot1 are floating-point zeros
                             * of different signs. Therefore in float and
                             * double sorting methods we have to use more
                             * accurate assignment a[less] = a[great].
                             */
                            a[less] = pivot1;
                            ++less;
                        }
                        else { // pivot1 < a[great] < pivot2
                            a[k] = a[great];
                        }
                        a[great] = ak;
                        --great;
                    }
                }
            }

            // Sort center part recursively
            SortingLongTestJMH.sort(a, less, great, false);
        }
        else { // Partitioning with one pivot
            /*
             * Use the third of the five sorted elements as pivot.
             * This value is inexpensive approximation of the median.
             */
            long pivot = a[e3];

            /*
             * Partitioning degenerates to the traditional 3-way
             * (or "Dutch National Flag") schema:
             *
             *   left part    center part              right part
             * +-------------------------------------------------+
             * |  < pivot  |   == pivot   |     ?    |  > pivot  |
             * +-------------------------------------------------+
             *              ^              ^        ^
             *              |              |        |
             *             less            k      great
             *
             * Invariants:
             *
             *   all in (left, less)   < pivot
             *   all in [less, k)     == pivot
             *   all in (great, right) > pivot
             *
             * Pointer k is the first index of ?-part.
             */
            for (int k = less; k <= great; ++k) {
                if (a[k] == pivot) {
                    continue;
                }
                long ak = a[k];
                if (ak < pivot) { // Move a[k] to left part
                    a[k] = a[less];
                    a[less] = ak;
                    ++less;
                }
                else { // a[k] > pivot - Move a[k] to right part
                    while (a[great] > pivot) {
                        --great;
                    }
                    if (a[great] < pivot) { // a[great] <= pivot
                        a[k] = a[less];
                        a[less] = a[great];
                        ++less;
                    }
                    else { // a[great] == pivot
                        /*
                         * Even though a[great] equals to pivot, the
                         * assignment a[k] = pivot may be incorrect,
                         * if a[great] and pivot are floating-point
                         * zeros of different signs. Therefore in float
                         * and double sorting methods we have to use
                         * more accurate assignment a[k] = a[great].
                         */
                        a[k] = pivot;
                    }
                    a[great] = ak;
                    --great;
                }
            }

            /*
             * Sort left and right parts recursively.
             * All elements from center part are equal
             * and, therefore, already sorted.
             */
            SortingLongTestJMH.sort(a, left, less - 1, leftmost);
            SortingLongTestJMH.sort(a, great + 1, right, false);
        }
    }

    private static void swap(long[] arr, int i, int j) {
        long tmp = arr[i];
        arr[i] = arr[j];
        arr[j] = tmp;
    }
}
