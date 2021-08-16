/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

package java.util;

/**
 * This class provides access to package-private
 * methods of DualPivotQuicksort class.
 *
 * @author Vladimir Yaroslavskiy
 *
 * @version 2019.09.19
 *
 * @since 14
 */
public enum SortingHelper {

    DUAL_PIVOT_QUICKSORT("Dual-Pivot Quicksort") {

        @Override
        public void sort(Object a) {
            if (a instanceof int[]) {
                DualPivotQuicksort.sort((int[]) a, SEQUENTIAL, 0, ((int[]) a).length);
            } else if (a instanceof long[]) {
                DualPivotQuicksort.sort((long[]) a, SEQUENTIAL, 0, ((long[]) a).length);
            } else if (a instanceof byte[]) {
                DualPivotQuicksort.sort((byte[]) a, 0, ((byte[]) a).length);
            } else if (a instanceof char[]) {
                DualPivotQuicksort.sort((char[]) a, SEQUENTIAL, 0, ((char[]) a).length);
            } else if (a instanceof short[]) {
                DualPivotQuicksort.sort((short[]) a, SEQUENTIAL, 0, ((short[]) a).length);
            } else if (a instanceof float[]) {
                DualPivotQuicksort.sort((float[]) a, SEQUENTIAL, 0, ((float[]) a).length);
            } else if (a instanceof double[]) {
                DualPivotQuicksort.sort((double[]) a, SEQUENTIAL, 0, ((double[]) a).length);
            } else {
                fail(a);
            }
        }

        @Override
        public void sort(Object a, int low, int high) {
            if (a instanceof int[]) {
                DualPivotQuicksort.sort((int[]) a, SEQUENTIAL, low, high);
            } else if (a instanceof long[]) {
                DualPivotQuicksort.sort((long[]) a, SEQUENTIAL, low, high);
            } else if (a instanceof byte[]) {
                DualPivotQuicksort.sort((byte[]) a, low, high);
            } else if (a instanceof char[]) {
                DualPivotQuicksort.sort((char[]) a, SEQUENTIAL, low, high);
            } else if (a instanceof short[]) {
                DualPivotQuicksort.sort((short[]) a, SEQUENTIAL, low, high);
            } else if (a instanceof float[]) {
                DualPivotQuicksort.sort((float[]) a, SEQUENTIAL, low, high);
            } else if (a instanceof double[]) {
                DualPivotQuicksort.sort((double[]) a, SEQUENTIAL, low, high);
            } else {
                fail(a);
            }
        }

        @Override
        public void sort(Object[] a) {
            fail(a);
        }

        @Override
        public void sort(Object[] a, Comparator comparator) {
            fail(a);
        }
    },

    PARALLEL_SORT("Parallel sort") {

        @Override
        public void sort(Object a) {
            if (a instanceof int[]) {
                DualPivotQuicksort.sort((int[]) a, PARALLEL, 0, ((int[]) a).length);
            } else if (a instanceof long[]) {
                DualPivotQuicksort.sort((long[]) a, PARALLEL, 0, ((long[]) a).length);
            } else if (a instanceof byte[]) {
                DualPivotQuicksort.sort((byte[]) a, 0, ((byte[]) a).length);
            } else if (a instanceof char[]) {
                DualPivotQuicksort.sort((char[]) a, PARALLEL, 0, ((char[]) a).length);
            } else if (a instanceof short[]) {
                DualPivotQuicksort.sort((short[]) a, PARALLEL, 0, ((short[]) a).length);
            } else if (a instanceof float[]) {
                DualPivotQuicksort.sort((float[]) a, PARALLEL, 0, ((float[]) a).length);
            } else if (a instanceof double[]) {
                DualPivotQuicksort.sort((double[]) a, PARALLEL, 0, ((double[]) a).length);
            } else {
                fail(a);
            }
        }

        @Override
        public void sort(Object a, int low, int high) {
            if (a instanceof int[]) {
                DualPivotQuicksort.sort((int[]) a, PARALLEL, low, high);
            } else if (a instanceof long[]) {
                DualPivotQuicksort.sort((long[]) a, PARALLEL, low, high);
            } else if (a instanceof byte[]) {
                DualPivotQuicksort.sort((byte[]) a, low, high);
            } else if (a instanceof char[]) {
                DualPivotQuicksort.sort((char[]) a, PARALLEL, low, high);
            } else if (a instanceof short[]) {
                DualPivotQuicksort.sort((short[]) a, PARALLEL, low, high);
            } else if (a instanceof float[]) {
                DualPivotQuicksort.sort((float[]) a, PARALLEL, low, high);
            } else if (a instanceof double[]) {
                DualPivotQuicksort.sort((double[]) a, PARALLEL, low, high);
            } else {
                fail(a);
            }
        }

        @Override
        public void sort(Object[] a) {
            fail(a);
        }

        @Override
        public void sort(Object[] a, Comparator comparator) {
            fail(a);
        }
    },

    HEAP_SORT("Heap sort") {

        @Override
        public void sort(Object a) {
            if (a instanceof int[]) {
                DualPivotQuicksort.sort(null, (int[]) a, BIG_DEPTH, 0, ((int[]) a).length);
            } else if (a instanceof long[]) {
                DualPivotQuicksort.sort(null, (long[]) a, BIG_DEPTH, 0, ((long[]) a).length);
            } else if (a instanceof byte[]) {
                DualPivotQuicksort.sort((byte[]) a, 0, ((byte[]) a).length);
            } else if (a instanceof char[]) {
                DualPivotQuicksort.sort((char[]) a, BIG_DEPTH, 0, ((char[]) a).length);
            } else if (a instanceof short[]) {
                DualPivotQuicksort.sort((short[]) a, BIG_DEPTH, 0, ((short[]) a).length);
            } else if (a instanceof float[]) {
                DualPivotQuicksort.sort(null, (float[]) a, BIG_DEPTH, 0, ((float[]) a).length);
            } else if (a instanceof double[]) {
                DualPivotQuicksort.sort(null, (double[]) a, BIG_DEPTH, 0, ((double[]) a).length);
            } else {
                fail(a);
            }
        }

        @Override
        public void sort(Object a, int low, int high) {
            if (a instanceof int[]) {
                DualPivotQuicksort.sort(null, (int[]) a, BIG_DEPTH, low, high);
            } else if (a instanceof long[]) {
                DualPivotQuicksort.sort(null, (long[]) a, BIG_DEPTH, low, high);
            } else if (a instanceof byte[]) {
                DualPivotQuicksort.sort((byte[]) a, low, high);
            } else if (a instanceof char[]) {
                DualPivotQuicksort.sort((char[]) a, BIG_DEPTH, low, high);
            } else if (a instanceof short[]) {
                DualPivotQuicksort.sort((short[]) a, BIG_DEPTH, low, high);
            } else if (a instanceof float[]) {
                DualPivotQuicksort.sort(null, (float[]) a, BIG_DEPTH, low, high);
            } else if (a instanceof double[]) {
                DualPivotQuicksort.sort(null, (double[]) a, BIG_DEPTH, low, high);
            } else {
                fail(a);
            }
        }

        @Override
        public void sort(Object[] a) {
            fail(a);
        }

        @Override
        public void sort(Object[] a, Comparator comparator) {
            fail(a);
        }
    },

    ARRAYS_SORT("Arrays.sort") {

        @Override
        public void sort(Object a) {
            if (a instanceof int[]) {
                Arrays.sort((int[]) a);
            } else if (a instanceof long[]) {
                Arrays.sort((long[]) a);
            } else if (a instanceof byte[]) {
                Arrays.sort((byte[]) a);
            } else if (a instanceof char[]) {
                Arrays.sort((char[]) a);
            } else if (a instanceof short[]) {
                Arrays.sort((short[]) a);
            } else if (a instanceof float[]) {
                Arrays.sort((float[]) a);
            } else if (a instanceof double[]) {
                Arrays.sort((double[]) a);
            } else {
                fail(a);
            }
        }

        @Override
        public void sort(Object a, int low, int high) {
            if (a instanceof int[]) {
                Arrays.sort((int[]) a, low, high);
            } else if (a instanceof long[]) {
                Arrays.sort((long[]) a, low, high);
            } else if (a instanceof byte[]) {
                Arrays.sort((byte[]) a, low, high);
            } else if (a instanceof char[]) {
                Arrays.sort((char[]) a, low, high);
            } else if (a instanceof short[]) {
                Arrays.sort((short[]) a, low, high);
            } else if (a instanceof float[]) {
                Arrays.sort((float[]) a, low, high);
            } else if (a instanceof double[]) {
                Arrays.sort((double[]) a, low, high);
            } else {
                fail(a);
            }
        }

        @Override
        public void sort(Object[] a) {
            Arrays.sort(a);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void sort(Object[] a, Comparator comparator) {
            Arrays.sort(a, comparator);
        }
    },

    ARRAYS_PARALLEL_SORT("Arrays.parallelSort") {

        @Override
        public void sort(Object a) {
            if (a instanceof int[]) {
                Arrays.parallelSort((int[]) a);
            } else if (a instanceof long[]) {
                Arrays.parallelSort((long[]) a);
            } else if (a instanceof byte[]) {
                Arrays.parallelSort((byte[]) a);
            } else if (a instanceof char[]) {
                Arrays.parallelSort((char[]) a);
            } else if (a instanceof short[]) {
                Arrays.parallelSort((short[]) a);
            } else if (a instanceof float[]) {
                Arrays.parallelSort((float[]) a);
            } else if (a instanceof double[]) {
                Arrays.parallelSort((double[]) a);
            } else {
                fail(a);
            }
        }

        @Override
        public void sort(Object a, int low, int high) {
            if (a instanceof int[]) {
                Arrays.parallelSort((int[]) a, low, high);
            } else if (a instanceof long[]) {
                Arrays.parallelSort((long[]) a, low, high);
            } else if (a instanceof byte[]) {
                Arrays.parallelSort((byte[]) a, low, high);
            } else if (a instanceof char[]) {
                Arrays.parallelSort((char[]) a, low, high);
            } else if (a instanceof short[]) {
                Arrays.parallelSort((short[]) a, low, high);
            } else if (a instanceof float[]) {
                Arrays.parallelSort((float[]) a, low, high);
            } else if (a instanceof double[]) {
                Arrays.parallelSort((double[]) a, low, high);
            } else {
                fail(a);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public void sort(Object[] a) {
            Arrays.parallelSort((Comparable[]) a);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void sort(Object[] a, Comparator comparator) {
            Arrays.parallelSort(a, comparator);
        }
    };

    abstract public void sort(Object a);

    abstract public void sort(Object a, int low, int high);

    abstract public void sort(Object[] a);

    abstract public void sort(Object[] a, Comparator comparator);

    private SortingHelper(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    private static void fail(Object a) {
        throw new RuntimeException("Unexpected type of array: " + a.getClass().getName());
    }

    private String name;

    /**
     * Parallelism level for sequential and parallel sorting.
     */
    private static final int SEQUENTIAL = 0;
    private static final int PARALLEL = 87;

    /**
     * Heap sort will be invoked, if recursion depth is too big.
     * Value is taken from DualPivotQuicksort.MAX_RECURSION_DEPTH.
     */
    private static final int BIG_DEPTH = 64 * (3 << 1);
}
