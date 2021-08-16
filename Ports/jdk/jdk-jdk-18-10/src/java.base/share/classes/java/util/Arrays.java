/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.util.ArraysSupport;
import jdk.internal.vm.annotation.IntrinsicCandidate;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.IntFunction;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.UnaryOperator;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * This class contains various methods for manipulating arrays (such as
 * sorting and searching). This class also contains a static factory
 * that allows arrays to be viewed as lists.
 *
 * <p>The methods in this class all throw a {@code NullPointerException},
 * if the specified array reference is null, except where noted.
 *
 * <p>The documentation for the methods contained in this class includes
 * brief descriptions of the <i>implementations</i>. Such descriptions should
 * be regarded as <i>implementation notes</i>, rather than parts of the
 * <i>specification</i>. Implementors should feel free to substitute other
 * algorithms, so long as the specification itself is adhered to. (For
 * example, the algorithm used by {@code sort(Object[])} does not have to be
 * a MergeSort, but it does have to be <i>stable</i>.)
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/java.base/java/util/package-summary.html#CollectionsFramework">
 * Java Collections Framework</a>.
 *
 * @author Josh Bloch
 * @author Neal Gafter
 * @author John Rose
 * @since  1.2
 */
public class Arrays {

    // Suppresses default constructor, ensuring non-instantiability.
    private Arrays() {}

    /*
     * Sorting methods. Note that all public "sort" methods take the
     * same form: performing argument checks if necessary, and then
     * expanding arguments into those required for the internal
     * implementation methods residing in other package-private
     * classes (except for legacyMergeSort, included in this class).
     */

    /**
     * Sorts the specified array into ascending numerical order.
     *
     * @implNote The sorting algorithm is a Dual-Pivot Quicksort
     * by Vladimir Yaroslavskiy, Jon Bentley, and Joshua Bloch. This algorithm
     * offers O(n log(n)) performance on all data sets, and is typically
     * faster than traditional (one-pivot) Quicksort implementations.
     *
     * @param a the array to be sorted
     */
    public static void sort(int[] a) {
        DualPivotQuicksort.sort(a, 0, 0, a.length);
    }

    /**
     * Sorts the specified range of the array into ascending order. The range
     * to be sorted extends from the index {@code fromIndex}, inclusive, to
     * the index {@code toIndex}, exclusive. If {@code fromIndex == toIndex},
     * the range to be sorted is empty.
     *
     * @implNote The sorting algorithm is a Dual-Pivot Quicksort
     * by Vladimir Yaroslavskiy, Jon Bentley, and Joshua Bloch. This algorithm
     * offers O(n log(n)) performance on all data sets, and is typically
     * faster than traditional (one-pivot) Quicksort implementations.
     *
     * @param a the array to be sorted
     * @param fromIndex the index of the first element, inclusive, to be sorted
     * @param toIndex the index of the last element, exclusive, to be sorted
     *
     * @throws IllegalArgumentException if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException
     *     if {@code fromIndex < 0} or {@code toIndex > a.length}
     */
    public static void sort(int[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
        DualPivotQuicksort.sort(a, 0, fromIndex, toIndex);
    }

    /**
     * Sorts the specified array into ascending numerical order.
     *
     * @implNote The sorting algorithm is a Dual-Pivot Quicksort
     * by Vladimir Yaroslavskiy, Jon Bentley, and Joshua Bloch. This algorithm
     * offers O(n log(n)) performance on all data sets, and is typically
     * faster than traditional (one-pivot) Quicksort implementations.
     *
     * @param a the array to be sorted
     */
    public static void sort(long[] a) {
        DualPivotQuicksort.sort(a, 0, 0, a.length);
    }

    /**
     * Sorts the specified range of the array into ascending order. The range
     * to be sorted extends from the index {@code fromIndex}, inclusive, to
     * the index {@code toIndex}, exclusive. If {@code fromIndex == toIndex},
     * the range to be sorted is empty.
     *
     * @implNote The sorting algorithm is a Dual-Pivot Quicksort
     * by Vladimir Yaroslavskiy, Jon Bentley, and Joshua Bloch. This algorithm
     * offers O(n log(n)) performance on all data sets, and is typically
     * faster than traditional (one-pivot) Quicksort implementations.
     *
     * @param a the array to be sorted
     * @param fromIndex the index of the first element, inclusive, to be sorted
     * @param toIndex the index of the last element, exclusive, to be sorted
     *
     * @throws IllegalArgumentException if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException
     *     if {@code fromIndex < 0} or {@code toIndex > a.length}
     */
    public static void sort(long[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
        DualPivotQuicksort.sort(a, 0, fromIndex, toIndex);
    }

    /**
     * Sorts the specified array into ascending numerical order.
     *
     * @implNote The sorting algorithm is a Dual-Pivot Quicksort
     * by Vladimir Yaroslavskiy, Jon Bentley, and Joshua Bloch. This algorithm
     * offers O(n log(n)) performance on all data sets, and is typically
     * faster than traditional (one-pivot) Quicksort implementations.
     *
     * @param a the array to be sorted
     */
    public static void sort(short[] a) {
        DualPivotQuicksort.sort(a, 0, a.length);
    }

    /**
     * Sorts the specified range of the array into ascending order. The range
     * to be sorted extends from the index {@code fromIndex}, inclusive, to
     * the index {@code toIndex}, exclusive. If {@code fromIndex == toIndex},
     * the range to be sorted is empty.
     *
     * @implNote The sorting algorithm is a Dual-Pivot Quicksort
     * by Vladimir Yaroslavskiy, Jon Bentley, and Joshua Bloch. This algorithm
     * offers O(n log(n)) performance on all data sets, and is typically
     * faster than traditional (one-pivot) Quicksort implementations.
     *
     * @param a the array to be sorted
     * @param fromIndex the index of the first element, inclusive, to be sorted
     * @param toIndex the index of the last element, exclusive, to be sorted
     *
     * @throws IllegalArgumentException if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException
     *     if {@code fromIndex < 0} or {@code toIndex > a.length}
     */
    public static void sort(short[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
        DualPivotQuicksort.sort(a, fromIndex, toIndex);
    }

    /**
     * Sorts the specified array into ascending numerical order.
     *
     * @implNote The sorting algorithm is a Dual-Pivot Quicksort
     * by Vladimir Yaroslavskiy, Jon Bentley, and Joshua Bloch. This algorithm
     * offers O(n log(n)) performance on all data sets, and is typically
     * faster than traditional (one-pivot) Quicksort implementations.
     *
     * @param a the array to be sorted
     */
    public static void sort(char[] a) {
        DualPivotQuicksort.sort(a, 0, a.length);
    }

    /**
     * Sorts the specified range of the array into ascending order. The range
     * to be sorted extends from the index {@code fromIndex}, inclusive, to
     * the index {@code toIndex}, exclusive. If {@code fromIndex == toIndex},
     * the range to be sorted is empty.
     *
     * @implNote The sorting algorithm is a Dual-Pivot Quicksort
     * by Vladimir Yaroslavskiy, Jon Bentley, and Joshua Bloch. This algorithm
     * offers O(n log(n)) performance on all data sets, and is typically
     * faster than traditional (one-pivot) Quicksort implementations.
     *
     * @param a the array to be sorted
     * @param fromIndex the index of the first element, inclusive, to be sorted
     * @param toIndex the index of the last element, exclusive, to be sorted
     *
     * @throws IllegalArgumentException if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException
     *     if {@code fromIndex < 0} or {@code toIndex > a.length}
     */
    public static void sort(char[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
        DualPivotQuicksort.sort(a, fromIndex, toIndex);
    }

    /**
     * Sorts the specified array into ascending numerical order.
     *
     * @implNote The sorting algorithm is a Dual-Pivot Quicksort
     * by Vladimir Yaroslavskiy, Jon Bentley, and Joshua Bloch. This algorithm
     * offers O(n log(n)) performance on all data sets, and is typically
     * faster than traditional (one-pivot) Quicksort implementations.
     *
     * @param a the array to be sorted
     */
    public static void sort(byte[] a) {
        DualPivotQuicksort.sort(a, 0, a.length);
    }

    /**
     * Sorts the specified range of the array into ascending order. The range
     * to be sorted extends from the index {@code fromIndex}, inclusive, to
     * the index {@code toIndex}, exclusive. If {@code fromIndex == toIndex},
     * the range to be sorted is empty.
     *
     * @implNote The sorting algorithm is a Dual-Pivot Quicksort
     * by Vladimir Yaroslavskiy, Jon Bentley, and Joshua Bloch. This algorithm
     * offers O(n log(n)) performance on all data sets, and is typically
     * faster than traditional (one-pivot) Quicksort implementations.
     *
     * @param a the array to be sorted
     * @param fromIndex the index of the first element, inclusive, to be sorted
     * @param toIndex the index of the last element, exclusive, to be sorted
     *
     * @throws IllegalArgumentException if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException
     *     if {@code fromIndex < 0} or {@code toIndex > a.length}
     */
    public static void sort(byte[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
        DualPivotQuicksort.sort(a, fromIndex, toIndex);
    }

    /**
     * Sorts the specified array into ascending numerical order.
     *
     * <p>The {@code <} relation does not provide a total order on all float
     * values: {@code -0.0f == 0.0f} is {@code true} and a {@code Float.NaN}
     * value compares neither less than, greater than, nor equal to any value,
     * even itself. This method uses the total order imposed by the method
     * {@link Float#compareTo}: {@code -0.0f} is treated as less than value
     * {@code 0.0f} and {@code Float.NaN} is considered greater than any
     * other value and all {@code Float.NaN} values are considered equal.
     *
     * @implNote The sorting algorithm is a Dual-Pivot Quicksort
     * by Vladimir Yaroslavskiy, Jon Bentley, and Joshua Bloch. This algorithm
     * offers O(n log(n)) performance on all data sets, and is typically
     * faster than traditional (one-pivot) Quicksort implementations.
     *
     * @param a the array to be sorted
     */
    public static void sort(float[] a) {
        DualPivotQuicksort.sort(a, 0, 0, a.length);
    }

    /**
     * Sorts the specified range of the array into ascending order. The range
     * to be sorted extends from the index {@code fromIndex}, inclusive, to
     * the index {@code toIndex}, exclusive. If {@code fromIndex == toIndex},
     * the range to be sorted is empty.
     *
     * <p>The {@code <} relation does not provide a total order on all float
     * values: {@code -0.0f == 0.0f} is {@code true} and a {@code Float.NaN}
     * value compares neither less than, greater than, nor equal to any value,
     * even itself. This method uses the total order imposed by the method
     * {@link Float#compareTo}: {@code -0.0f} is treated as less than value
     * {@code 0.0f} and {@code Float.NaN} is considered greater than any
     * other value and all {@code Float.NaN} values are considered equal.
     *
     * @implNote The sorting algorithm is a Dual-Pivot Quicksort
     * by Vladimir Yaroslavskiy, Jon Bentley, and Joshua Bloch. This algorithm
     * offers O(n log(n)) performance on all data sets, and is typically
     * faster than traditional (one-pivot) Quicksort implementations.
     *
     * @param a the array to be sorted
     * @param fromIndex the index of the first element, inclusive, to be sorted
     * @param toIndex the index of the last element, exclusive, to be sorted
     *
     * @throws IllegalArgumentException if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException
     *     if {@code fromIndex < 0} or {@code toIndex > a.length}
     */
    public static void sort(float[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
        DualPivotQuicksort.sort(a, 0, fromIndex, toIndex);
    }

    /**
     * Sorts the specified array into ascending numerical order.
     *
     * <p>The {@code <} relation does not provide a total order on all double
     * values: {@code -0.0d == 0.0d} is {@code true} and a {@code Double.NaN}
     * value compares neither less than, greater than, nor equal to any value,
     * even itself. This method uses the total order imposed by the method
     * {@link Double#compareTo}: {@code -0.0d} is treated as less than value
     * {@code 0.0d} and {@code Double.NaN} is considered greater than any
     * other value and all {@code Double.NaN} values are considered equal.
     *
     * @implNote The sorting algorithm is a Dual-Pivot Quicksort
     * by Vladimir Yaroslavskiy, Jon Bentley, and Joshua Bloch. This algorithm
     * offers O(n log(n)) performance on all data sets, and is typically
     * faster than traditional (one-pivot) Quicksort implementations.
     *
     * @param a the array to be sorted
     */
    public static void sort(double[] a) {
        DualPivotQuicksort.sort(a, 0, 0, a.length);
    }

    /**
     * Sorts the specified range of the array into ascending order. The range
     * to be sorted extends from the index {@code fromIndex}, inclusive, to
     * the index {@code toIndex}, exclusive. If {@code fromIndex == toIndex},
     * the range to be sorted is empty.
     *
     * <p>The {@code <} relation does not provide a total order on all double
     * values: {@code -0.0d == 0.0d} is {@code true} and a {@code Double.NaN}
     * value compares neither less than, greater than, nor equal to any value,
     * even itself. This method uses the total order imposed by the method
     * {@link Double#compareTo}: {@code -0.0d} is treated as less than value
     * {@code 0.0d} and {@code Double.NaN} is considered greater than any
     * other value and all {@code Double.NaN} values are considered equal.
     *
     * @implNote The sorting algorithm is a Dual-Pivot Quicksort
     * by Vladimir Yaroslavskiy, Jon Bentley, and Joshua Bloch. This algorithm
     * offers O(n log(n)) performance on all data sets, and is typically
     * faster than traditional (one-pivot) Quicksort implementations.
     *
     * @param a the array to be sorted
     * @param fromIndex the index of the first element, inclusive, to be sorted
     * @param toIndex the index of the last element, exclusive, to be sorted
     *
     * @throws IllegalArgumentException if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException
     *     if {@code fromIndex < 0} or {@code toIndex > a.length}
     */
    public static void sort(double[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
        DualPivotQuicksort.sort(a, 0, fromIndex, toIndex);
    }

    /**
     * Sorts the specified array into ascending numerical order.
     *
     * @implNote The sorting algorithm is a Dual-Pivot Quicksort by
     * Vladimir Yaroslavskiy, Jon Bentley and Josh Bloch. This algorithm
     * offers O(n log(n)) performance on all data sets, and is typically
     * faster than traditional (one-pivot) Quicksort implementations.
     *
     * @param a the array to be sorted
     *
     * @since 1.8
     */
    public static void parallelSort(byte[] a) {
        DualPivotQuicksort.sort(a, 0, a.length);
    }

    /**
     * Sorts the specified range of the array into ascending numerical order.
     * The range to be sorted extends from the index {@code fromIndex},
     * inclusive, to the index {@code toIndex}, exclusive. If
     * {@code fromIndex == toIndex}, the range to be sorted is empty.
     *
     * @implNote The sorting algorithm is a Dual-Pivot Quicksort by
     * Vladimir Yaroslavskiy, Jon Bentley and Josh Bloch. This algorithm
     * offers O(n log(n)) performance on all data sets, and is typically
     * faster than traditional (one-pivot) Quicksort implementations.
     *
     * @param a the array to be sorted
     * @param fromIndex the index of the first element, inclusive, to be sorted
     * @param toIndex the index of the last element, exclusive, to be sorted
     *
     * @throws IllegalArgumentException if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException
     *     if {@code fromIndex < 0} or {@code toIndex > a.length}
     *
     * @since 1.8
     */
    public static void parallelSort(byte[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
        DualPivotQuicksort.sort(a, fromIndex, toIndex);
    }

    /**
     * Sorts the specified array into ascending numerical order.
     *
     * @implNote The sorting algorithm is a Dual-Pivot Quicksort by
     * Vladimir Yaroslavskiy, Jon Bentley and Josh Bloch. This algorithm
     * offers O(n log(n)) performance on all data sets, and is typically
     * faster than traditional (one-pivot) Quicksort implementations.
     *
     * @param a the array to be sorted
     *
     * @since 1.8
     */
    public static void parallelSort(char[] a) {
        DualPivotQuicksort.sort(a, 0, a.length);
    }

    /**
     * Sorts the specified range of the array into ascending numerical order.
     * The range to be sorted extends from the index {@code fromIndex},
     * inclusive, to the index {@code toIndex}, exclusive. If
     * {@code fromIndex == toIndex}, the range to be sorted is empty.
     *
     * @implNote The sorting algorithm is a Dual-Pivot Quicksort by
     * Vladimir Yaroslavskiy, Jon Bentley and Josh Bloch. This algorithm
     * offers O(n log(n)) performance on all data sets, and is typically
     * faster than traditional (one-pivot) Quicksort implementations.
     *
     * @param a the array to be sorted
     * @param fromIndex the index of the first element, inclusive, to be sorted
     * @param toIndex the index of the last element, exclusive, to be sorted
     *
     * @throws IllegalArgumentException if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException
     *     if {@code fromIndex < 0} or {@code toIndex > a.length}
     *
     * @since 1.8
     */
    public static void parallelSort(char[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
        DualPivotQuicksort.sort(a, fromIndex, toIndex);
    }

    /**
     * Sorts the specified array into ascending numerical order.
     *
     * @implNote The sorting algorithm is a Dual-Pivot Quicksort by
     * Vladimir Yaroslavskiy, Jon Bentley and Josh Bloch. This algorithm
     * offers O(n log(n)) performance on all data sets, and is typically
     * faster than traditional (one-pivot) Quicksort implementations.
     *
     * @param a the array to be sorted
     *
     * @since 1.8
     */
    public static void parallelSort(short[] a) {
        DualPivotQuicksort.sort(a, 0, a.length);
    }

    /**
     * Sorts the specified range of the array into ascending numerical order.
     * The range to be sorted extends from the index {@code fromIndex},
     * inclusive, to the index {@code toIndex}, exclusive. If
     * {@code fromIndex == toIndex}, the range to be sorted is empty.
     *
     * @implNote The sorting algorithm is a Dual-Pivot Quicksort by
     * Vladimir Yaroslavskiy, Jon Bentley and Josh Bloch. This algorithm
     * offers O(n log(n)) performance on all data sets, and is typically
     * faster than traditional (one-pivot) Quicksort implementations.
     *
     * @param a the array to be sorted
     * @param fromIndex the index of the first element, inclusive, to be sorted
     * @param toIndex the index of the last element, exclusive, to be sorted
     *
     * @throws IllegalArgumentException if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException
     *     if {@code fromIndex < 0} or {@code toIndex > a.length}
     *
     * @since 1.8
     */
    public static void parallelSort(short[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
        DualPivotQuicksort.sort(a, fromIndex, toIndex);
    }

    /**
     * Sorts the specified array into ascending numerical order.
     *
     * @implNote The sorting algorithm is a Dual-Pivot Quicksort by
     * Vladimir Yaroslavskiy, Jon Bentley and Josh Bloch. This algorithm
     * offers O(n log(n)) performance on all data sets, and is typically
     * faster than traditional (one-pivot) Quicksort implementations.
     *
     * @param a the array to be sorted
     *
     * @since 1.8
     */
    public static void parallelSort(int[] a) {
        DualPivotQuicksort.sort(a, ForkJoinPool.getCommonPoolParallelism(), 0, a.length);
    }

    /**
     * Sorts the specified range of the array into ascending numerical order.
     * The range to be sorted extends from the index {@code fromIndex},
     * inclusive, to the index {@code toIndex}, exclusive. If
     * {@code fromIndex == toIndex}, the range to be sorted is empty.
     *
     * @implNote The sorting algorithm is a Dual-Pivot Quicksort by
     * Vladimir Yaroslavskiy, Jon Bentley and Josh Bloch. This algorithm
     * offers O(n log(n)) performance on all data sets, and is typically
     * faster than traditional (one-pivot) Quicksort implementations.
     *
     * @param a the array to be sorted
     * @param fromIndex the index of the first element, inclusive, to be sorted
     * @param toIndex the index of the last element, exclusive, to be sorted
     *
     * @throws IllegalArgumentException if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException
     *     if {@code fromIndex < 0} or {@code toIndex > a.length}
     *
     * @since 1.8
     */
    public static void parallelSort(int[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
        DualPivotQuicksort.sort(a, ForkJoinPool.getCommonPoolParallelism(), fromIndex, toIndex);
    }

    /**
     * Sorts the specified array into ascending numerical order.
     *
     * @implNote The sorting algorithm is a Dual-Pivot Quicksort by
     * Vladimir Yaroslavskiy, Jon Bentley and Josh Bloch. This algorithm
     * offers O(n log(n)) performance on all data sets, and is typically
     * faster than traditional (one-pivot) Quicksort implementations.
     *
     * @param a the array to be sorted
     *
     * @since 1.8
     */
    public static void parallelSort(long[] a) {
        DualPivotQuicksort.sort(a, ForkJoinPool.getCommonPoolParallelism(), 0, a.length);
    }

    /**
     * Sorts the specified range of the array into ascending numerical order.
     * The range to be sorted extends from the index {@code fromIndex},
     * inclusive, to the index {@code toIndex}, exclusive. If
     * {@code fromIndex == toIndex}, the range to be sorted is empty.
     *
     * @implNote The sorting algorithm is a Dual-Pivot Quicksort by
     * Vladimir Yaroslavskiy, Jon Bentley and Josh Bloch. This algorithm
     * offers O(n log(n)) performance on all data sets, and is typically
     * faster than traditional (one-pivot) Quicksort implementations.
     *
     * @param a the array to be sorted
     * @param fromIndex the index of the first element, inclusive, to be sorted
     * @param toIndex the index of the last element, exclusive, to be sorted
     *
     * @throws IllegalArgumentException if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException
     *     if {@code fromIndex < 0} or {@code toIndex > a.length}
     *
     * @since 1.8
     */
    public static void parallelSort(long[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
        DualPivotQuicksort.sort(a, ForkJoinPool.getCommonPoolParallelism(), fromIndex, toIndex);
    }

    /**
     * Sorts the specified array into ascending numerical order.
     *
     * <p>The {@code <} relation does not provide a total order on all float
     * values: {@code -0.0f == 0.0f} is {@code true} and a {@code Float.NaN}
     * value compares neither less than, greater than, nor equal to any value,
     * even itself. This method uses the total order imposed by the method
     * {@link Float#compareTo}: {@code -0.0f} is treated as less than value
     * {@code 0.0f} and {@code Float.NaN} is considered greater than any
     * other value and all {@code Float.NaN} values are considered equal.
     *
     * @implNote The sorting algorithm is a Dual-Pivot Quicksort by
     * Vladimir Yaroslavskiy, Jon Bentley and Josh Bloch. This algorithm
     * offers O(n log(n)) performance on all data sets, and is typically
     * faster than traditional (one-pivot) Quicksort implementations.
     *
     * @param a the array to be sorted
     *
     * @since 1.8
     */
    public static void parallelSort(float[] a) {
        DualPivotQuicksort.sort(a, ForkJoinPool.getCommonPoolParallelism(), 0, a.length);
    }

    /**
     * Sorts the specified range of the array into ascending numerical order.
     * The range to be sorted extends from the index {@code fromIndex},
     * inclusive, to the index {@code toIndex}, exclusive. If
     * {@code fromIndex == toIndex}, the range to be sorted is empty.
     *
     * <p>The {@code <} relation does not provide a total order on all float
     * values: {@code -0.0f == 0.0f} is {@code true} and a {@code Float.NaN}
     * value compares neither less than, greater than, nor equal to any value,
     * even itself. This method uses the total order imposed by the method
     * {@link Float#compareTo}: {@code -0.0f} is treated as less than value
     * {@code 0.0f} and {@code Float.NaN} is considered greater than any
     * other value and all {@code Float.NaN} values are considered equal.
     *
     * @implNote The sorting algorithm is a Dual-Pivot Quicksort by
     * Vladimir Yaroslavskiy, Jon Bentley and Josh Bloch. This algorithm
     * offers O(n log(n)) performance on all data sets, and is typically
     * faster than traditional (one-pivot) Quicksort implementations.
     *
     * @param a the array to be sorted
     * @param fromIndex the index of the first element, inclusive, to be sorted
     * @param toIndex the index of the last element, exclusive, to be sorted
     *
     * @throws IllegalArgumentException if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException
     *     if {@code fromIndex < 0} or {@code toIndex > a.length}
     *
     * @since 1.8
     */
    public static void parallelSort(float[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
        DualPivotQuicksort.sort(a, ForkJoinPool.getCommonPoolParallelism(), fromIndex, toIndex);
    }

    /**
     * Sorts the specified array into ascending numerical order.
     *
     * <p>The {@code <} relation does not provide a total order on all double
     * values: {@code -0.0d == 0.0d} is {@code true} and a {@code Double.NaN}
     * value compares neither less than, greater than, nor equal to any value,
     * even itself. This method uses the total order imposed by the method
     * {@link Double#compareTo}: {@code -0.0d} is treated as less than value
     * {@code 0.0d} and {@code Double.NaN} is considered greater than any
     * other value and all {@code Double.NaN} values are considered equal.
     *
     * @implNote The sorting algorithm is a Dual-Pivot Quicksort by
     * Vladimir Yaroslavskiy, Jon Bentley and Josh Bloch. This algorithm
     * offers O(n log(n)) performance on all data sets, and is typically
     * faster than traditional (one-pivot) Quicksort implementations.
     *
     * @param a the array to be sorted
     *
     * @since 1.8
     */
    public static void parallelSort(double[] a) {
        DualPivotQuicksort.sort(a, ForkJoinPool.getCommonPoolParallelism(), 0, a.length);
    }

    /**
     * Sorts the specified range of the array into ascending numerical order.
     * The range to be sorted extends from the index {@code fromIndex},
     * inclusive, to the index {@code toIndex}, exclusive. If
     * {@code fromIndex == toIndex}, the range to be sorted is empty.
     *
     * <p>The {@code <} relation does not provide a total order on all double
     * values: {@code -0.0d == 0.0d} is {@code true} and a {@code Double.NaN}
     * value compares neither less than, greater than, nor equal to any value,
     * even itself. This method uses the total order imposed by the method
     * {@link Double#compareTo}: {@code -0.0d} is treated as less than value
     * {@code 0.0d} and {@code Double.NaN} is considered greater than any
     * other value and all {@code Double.NaN} values are considered equal.
     *
     * @implNote The sorting algorithm is a Dual-Pivot Quicksort by
     * Vladimir Yaroslavskiy, Jon Bentley and Josh Bloch. This algorithm
     * offers O(n log(n)) performance on all data sets, and is typically
     * faster than traditional (one-pivot) Quicksort implementations.
     *
     * @param a the array to be sorted
     * @param fromIndex the index of the first element, inclusive, to be sorted
     * @param toIndex the index of the last element, exclusive, to be sorted
     *
     * @throws IllegalArgumentException if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException
     *     if {@code fromIndex < 0} or {@code toIndex > a.length}
     *
     * @since 1.8
     */
    public static void parallelSort(double[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
        DualPivotQuicksort.sort(a, ForkJoinPool.getCommonPoolParallelism(), fromIndex, toIndex);
    }

    /**
     * Checks that {@code fromIndex} and {@code toIndex} are in
     * the range and throws an exception if they aren't.
     */
    static void rangeCheck(int arrayLength, int fromIndex, int toIndex) {
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException(
                "fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
        }
        if (fromIndex < 0) {
            throw new ArrayIndexOutOfBoundsException(fromIndex);
        }
        if (toIndex > arrayLength) {
            throw new ArrayIndexOutOfBoundsException(toIndex);
        }
    }

    /**
     * A comparator that implements the natural ordering of a group of
     * mutually comparable elements. May be used when a supplied
     * comparator is null. To simplify code-sharing within underlying
     * implementations, the compare method only declares type Object
     * for its second argument.
     *
     * Arrays class implementor's note: It is an empirical matter
     * whether ComparableTimSort offers any performance benefit over
     * TimSort used with this comparator.  If not, you are better off
     * deleting or bypassing ComparableTimSort.  There is currently no
     * empirical case for separating them for parallel sorting, so all
     * public Object parallelSort methods use the same comparator
     * based implementation.
     */
    static final class NaturalOrder implements Comparator<Object> {
        @SuppressWarnings("unchecked")
        public int compare(Object first, Object second) {
            return ((Comparable<Object>)first).compareTo(second);
        }
        static final NaturalOrder INSTANCE = new NaturalOrder();
    }

    /**
     * The minimum array length below which a parallel sorting
     * algorithm will not further partition the sorting task. Using
     * smaller sizes typically results in memory contention across
     * tasks that makes parallel speedups unlikely.
     */
    private static final int MIN_ARRAY_SORT_GRAN = 1 << 13;

    /**
     * Sorts the specified array of objects into ascending order, according
     * to the {@linkplain Comparable natural ordering} of its elements.
     * All elements in the array must implement the {@link Comparable}
     * interface.  Furthermore, all elements in the array must be
     * <i>mutually comparable</i> (that is, {@code e1.compareTo(e2)} must
     * not throw a {@code ClassCastException} for any elements {@code e1}
     * and {@code e2} in the array).
     *
     * <p>This sort is guaranteed to be <i>stable</i>:  equal elements will
     * not be reordered as a result of the sort.
     *
     * @implNote The sorting algorithm is a parallel sort-merge that breaks the
     * array into sub-arrays that are themselves sorted and then merged. When
     * the sub-array length reaches a minimum granularity, the sub-array is
     * sorted using the appropriate {@link Arrays#sort(Object[]) Arrays.sort}
     * method. If the length of the specified array is less than the minimum
     * granularity, then it is sorted using the appropriate {@link
     * Arrays#sort(Object[]) Arrays.sort} method. The algorithm requires a
     * working space no greater than the size of the original array. The
     * {@link ForkJoinPool#commonPool() ForkJoin common pool} is used to
     * execute any parallel tasks.
     *
     * @param <T> the class of the objects to be sorted
     * @param a the array to be sorted
     *
     * @throws ClassCastException if the array contains elements that are not
     *         <i>mutually comparable</i> (for example, strings and integers)
     * @throws IllegalArgumentException (optional) if the natural
     *         ordering of the array elements is found to violate the
     *         {@link Comparable} contract
     *
     * @since 1.8
     */
    @SuppressWarnings("unchecked")
    public static <T extends Comparable<? super T>> void parallelSort(T[] a) {
        int n = a.length, p, g;
        if (n <= MIN_ARRAY_SORT_GRAN ||
            (p = ForkJoinPool.getCommonPoolParallelism()) == 1)
            TimSort.sort(a, 0, n, NaturalOrder.INSTANCE, null, 0, 0);
        else
            new ArraysParallelSortHelpers.FJObject.Sorter<>
                (null, a,
                 (T[])Array.newInstance(a.getClass().getComponentType(), n),
                 0, n, 0, ((g = n / (p << 2)) <= MIN_ARRAY_SORT_GRAN) ?
                 MIN_ARRAY_SORT_GRAN : g, NaturalOrder.INSTANCE).invoke();
    }

    /**
     * Sorts the specified range of the specified array of objects into
     * ascending order, according to the
     * {@linkplain Comparable natural ordering} of its
     * elements.  The range to be sorted extends from index
     * {@code fromIndex}, inclusive, to index {@code toIndex}, exclusive.
     * (If {@code fromIndex==toIndex}, the range to be sorted is empty.)  All
     * elements in this range must implement the {@link Comparable}
     * interface.  Furthermore, all elements in this range must be <i>mutually
     * comparable</i> (that is, {@code e1.compareTo(e2)} must not throw a
     * {@code ClassCastException} for any elements {@code e1} and
     * {@code e2} in the array).
     *
     * <p>This sort is guaranteed to be <i>stable</i>:  equal elements will
     * not be reordered as a result of the sort.
     *
     * @implNote The sorting algorithm is a parallel sort-merge that breaks the
     * array into sub-arrays that are themselves sorted and then merged. When
     * the sub-array length reaches a minimum granularity, the sub-array is
     * sorted using the appropriate {@link Arrays#sort(Object[]) Arrays.sort}
     * method. If the length of the specified array is less than the minimum
     * granularity, then it is sorted using the appropriate {@link
     * Arrays#sort(Object[]) Arrays.sort} method. The algorithm requires a working
     * space no greater than the size of the specified range of the original
     * array. The {@link ForkJoinPool#commonPool() ForkJoin common pool} is
     * used to execute any parallel tasks.
     *
     * @param <T> the class of the objects to be sorted
     * @param a the array to be sorted
     * @param fromIndex the index of the first element (inclusive) to be
     *        sorted
     * @param toIndex the index of the last element (exclusive) to be sorted
     * @throws IllegalArgumentException if {@code fromIndex > toIndex} or
     *         (optional) if the natural ordering of the array elements is
     *         found to violate the {@link Comparable} contract
     * @throws ArrayIndexOutOfBoundsException if {@code fromIndex < 0} or
     *         {@code toIndex > a.length}
     * @throws ClassCastException if the array contains elements that are
     *         not <i>mutually comparable</i> (for example, strings and
     *         integers).
     *
     * @since 1.8
     */
    @SuppressWarnings("unchecked")
    public static <T extends Comparable<? super T>>
    void parallelSort(T[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
        int n = toIndex - fromIndex, p, g;
        if (n <= MIN_ARRAY_SORT_GRAN ||
            (p = ForkJoinPool.getCommonPoolParallelism()) == 1)
            TimSort.sort(a, fromIndex, toIndex, NaturalOrder.INSTANCE, null, 0, 0);
        else
            new ArraysParallelSortHelpers.FJObject.Sorter<>
                (null, a,
                 (T[])Array.newInstance(a.getClass().getComponentType(), n),
                 fromIndex, n, 0, ((g = n / (p << 2)) <= MIN_ARRAY_SORT_GRAN) ?
                 MIN_ARRAY_SORT_GRAN : g, NaturalOrder.INSTANCE).invoke();
    }

    /**
     * Sorts the specified array of objects according to the order induced by
     * the specified comparator.  All elements in the array must be
     * <i>mutually comparable</i> by the specified comparator (that is,
     * {@code c.compare(e1, e2)} must not throw a {@code ClassCastException}
     * for any elements {@code e1} and {@code e2} in the array).
     *
     * <p>This sort is guaranteed to be <i>stable</i>:  equal elements will
     * not be reordered as a result of the sort.
     *
     * @implNote The sorting algorithm is a parallel sort-merge that breaks the
     * array into sub-arrays that are themselves sorted and then merged. When
     * the sub-array length reaches a minimum granularity, the sub-array is
     * sorted using the appropriate {@link Arrays#sort(Object[]) Arrays.sort}
     * method. If the length of the specified array is less than the minimum
     * granularity, then it is sorted using the appropriate {@link
     * Arrays#sort(Object[]) Arrays.sort} method. The algorithm requires a
     * working space no greater than the size of the original array. The
     * {@link ForkJoinPool#commonPool() ForkJoin common pool} is used to
     * execute any parallel tasks.
     *
     * @param <T> the class of the objects to be sorted
     * @param a the array to be sorted
     * @param cmp the comparator to determine the order of the array.  A
     *        {@code null} value indicates that the elements'
     *        {@linkplain Comparable natural ordering} should be used.
     * @throws ClassCastException if the array contains elements that are
     *         not <i>mutually comparable</i> using the specified comparator
     * @throws IllegalArgumentException (optional) if the comparator is
     *         found to violate the {@link java.util.Comparator} contract
     *
     * @since 1.8
     */
    @SuppressWarnings("unchecked")
    public static <T> void parallelSort(T[] a, Comparator<? super T> cmp) {
        if (cmp == null)
            cmp = NaturalOrder.INSTANCE;
        int n = a.length, p, g;
        if (n <= MIN_ARRAY_SORT_GRAN ||
            (p = ForkJoinPool.getCommonPoolParallelism()) == 1)
            TimSort.sort(a, 0, n, cmp, null, 0, 0);
        else
            new ArraysParallelSortHelpers.FJObject.Sorter<>
                (null, a,
                 (T[])Array.newInstance(a.getClass().getComponentType(), n),
                 0, n, 0, ((g = n / (p << 2)) <= MIN_ARRAY_SORT_GRAN) ?
                 MIN_ARRAY_SORT_GRAN : g, cmp).invoke();
    }

    /**
     * Sorts the specified range of the specified array of objects according
     * to the order induced by the specified comparator.  The range to be
     * sorted extends from index {@code fromIndex}, inclusive, to index
     * {@code toIndex}, exclusive.  (If {@code fromIndex==toIndex}, the
     * range to be sorted is empty.)  All elements in the range must be
     * <i>mutually comparable</i> by the specified comparator (that is,
     * {@code c.compare(e1, e2)} must not throw a {@code ClassCastException}
     * for any elements {@code e1} and {@code e2} in the range).
     *
     * <p>This sort is guaranteed to be <i>stable</i>:  equal elements will
     * not be reordered as a result of the sort.
     *
     * @implNote The sorting algorithm is a parallel sort-merge that breaks the
     * array into sub-arrays that are themselves sorted and then merged. When
     * the sub-array length reaches a minimum granularity, the sub-array is
     * sorted using the appropriate {@link Arrays#sort(Object[]) Arrays.sort}
     * method. If the length of the specified array is less than the minimum
     * granularity, then it is sorted using the appropriate {@link
     * Arrays#sort(Object[]) Arrays.sort} method. The algorithm requires a working
     * space no greater than the size of the specified range of the original
     * array. The {@link ForkJoinPool#commonPool() ForkJoin common pool} is
     * used to execute any parallel tasks.
     *
     * @param <T> the class of the objects to be sorted
     * @param a the array to be sorted
     * @param fromIndex the index of the first element (inclusive) to be
     *        sorted
     * @param toIndex the index of the last element (exclusive) to be sorted
     * @param cmp the comparator to determine the order of the array.  A
     *        {@code null} value indicates that the elements'
     *        {@linkplain Comparable natural ordering} should be used.
     * @throws IllegalArgumentException if {@code fromIndex > toIndex} or
     *         (optional) if the natural ordering of the array elements is
     *         found to violate the {@link Comparable} contract
     * @throws ArrayIndexOutOfBoundsException if {@code fromIndex < 0} or
     *         {@code toIndex > a.length}
     * @throws ClassCastException if the array contains elements that are
     *         not <i>mutually comparable</i> (for example, strings and
     *         integers).
     *
     * @since 1.8
     */
    @SuppressWarnings("unchecked")
    public static <T> void parallelSort(T[] a, int fromIndex, int toIndex,
                                        Comparator<? super T> cmp) {
        rangeCheck(a.length, fromIndex, toIndex);
        if (cmp == null)
            cmp = NaturalOrder.INSTANCE;
        int n = toIndex - fromIndex, p, g;
        if (n <= MIN_ARRAY_SORT_GRAN ||
            (p = ForkJoinPool.getCommonPoolParallelism()) == 1)
            TimSort.sort(a, fromIndex, toIndex, cmp, null, 0, 0);
        else
            new ArraysParallelSortHelpers.FJObject.Sorter<>
                (null, a,
                 (T[])Array.newInstance(a.getClass().getComponentType(), n),
                 fromIndex, n, 0, ((g = n / (p << 2)) <= MIN_ARRAY_SORT_GRAN) ?
                 MIN_ARRAY_SORT_GRAN : g, cmp).invoke();
    }

    /*
     * Sorting of complex type arrays.
     */

    /**
     * Old merge sort implementation can be selected (for
     * compatibility with broken comparators) using a system property.
     * Cannot be a static boolean in the enclosing class due to
     * circular dependencies. To be removed in a future release.
     */
    static final class LegacyMergeSort {
        @SuppressWarnings("removal")
        private static final boolean userRequested =
            java.security.AccessController.doPrivileged(
                new sun.security.action.GetBooleanAction(
                    "java.util.Arrays.useLegacyMergeSort")).booleanValue();
    }

    /**
     * Sorts the specified array of objects into ascending order, according
     * to the {@linkplain Comparable natural ordering} of its elements.
     * All elements in the array must implement the {@link Comparable}
     * interface.  Furthermore, all elements in the array must be
     * <i>mutually comparable</i> (that is, {@code e1.compareTo(e2)} must
     * not throw a {@code ClassCastException} for any elements {@code e1}
     * and {@code e2} in the array).
     *
     * <p>This sort is guaranteed to be <i>stable</i>:  equal elements will
     * not be reordered as a result of the sort.
     *
     * <p>Implementation note: This implementation is a stable, adaptive,
     * iterative mergesort that requires far fewer than n lg(n) comparisons
     * when the input array is partially sorted, while offering the
     * performance of a traditional mergesort when the input array is
     * randomly ordered.  If the input array is nearly sorted, the
     * implementation requires approximately n comparisons.  Temporary
     * storage requirements vary from a small constant for nearly sorted
     * input arrays to n/2 object references for randomly ordered input
     * arrays.
     *
     * <p>The implementation takes equal advantage of ascending and
     * descending order in its input array, and can take advantage of
     * ascending and descending order in different parts of the same
     * input array.  It is well-suited to merging two or more sorted arrays:
     * simply concatenate the arrays and sort the resulting array.
     *
     * <p>The implementation was adapted from Tim Peters's list sort for Python
     * (<a href="http://svn.python.org/projects/python/trunk/Objects/listsort.txt">
     * TimSort</a>).  It uses techniques from Peter McIlroy's "Optimistic
     * Sorting and Information Theoretic Complexity", in Proceedings of the
     * Fourth Annual ACM-SIAM Symposium on Discrete Algorithms, pp 467-474,
     * January 1993.
     *
     * @param a the array to be sorted
     * @throws ClassCastException if the array contains elements that are not
     *         <i>mutually comparable</i> (for example, strings and integers)
     * @throws IllegalArgumentException (optional) if the natural
     *         ordering of the array elements is found to violate the
     *         {@link Comparable} contract
     */
    public static void sort(Object[] a) {
        if (LegacyMergeSort.userRequested)
            legacyMergeSort(a);
        else
            ComparableTimSort.sort(a, 0, a.length, null, 0, 0);
    }

    /** To be removed in a future release. */
    private static void legacyMergeSort(Object[] a) {
        Object[] aux = a.clone();
        mergeSort(aux, a, 0, a.length, 0);
    }

    /**
     * Sorts the specified range of the specified array of objects into
     * ascending order, according to the
     * {@linkplain Comparable natural ordering} of its
     * elements.  The range to be sorted extends from index
     * {@code fromIndex}, inclusive, to index {@code toIndex}, exclusive.
     * (If {@code fromIndex==toIndex}, the range to be sorted is empty.)  All
     * elements in this range must implement the {@link Comparable}
     * interface.  Furthermore, all elements in this range must be <i>mutually
     * comparable</i> (that is, {@code e1.compareTo(e2)} must not throw a
     * {@code ClassCastException} for any elements {@code e1} and
     * {@code e2} in the array).
     *
     * <p>This sort is guaranteed to be <i>stable</i>:  equal elements will
     * not be reordered as a result of the sort.
     *
     * <p>Implementation note: This implementation is a stable, adaptive,
     * iterative mergesort that requires far fewer than n lg(n) comparisons
     * when the input array is partially sorted, while offering the
     * performance of a traditional mergesort when the input array is
     * randomly ordered.  If the input array is nearly sorted, the
     * implementation requires approximately n comparisons.  Temporary
     * storage requirements vary from a small constant for nearly sorted
     * input arrays to n/2 object references for randomly ordered input
     * arrays.
     *
     * <p>The implementation takes equal advantage of ascending and
     * descending order in its input array, and can take advantage of
     * ascending and descending order in different parts of the same
     * input array.  It is well-suited to merging two or more sorted arrays:
     * simply concatenate the arrays and sort the resulting array.
     *
     * <p>The implementation was adapted from Tim Peters's list sort for Python
     * (<a href="http://svn.python.org/projects/python/trunk/Objects/listsort.txt">
     * TimSort</a>).  It uses techniques from Peter McIlroy's "Optimistic
     * Sorting and Information Theoretic Complexity", in Proceedings of the
     * Fourth Annual ACM-SIAM Symposium on Discrete Algorithms, pp 467-474,
     * January 1993.
     *
     * @param a the array to be sorted
     * @param fromIndex the index of the first element (inclusive) to be
     *        sorted
     * @param toIndex the index of the last element (exclusive) to be sorted
     * @throws IllegalArgumentException if {@code fromIndex > toIndex} or
     *         (optional) if the natural ordering of the array elements is
     *         found to violate the {@link Comparable} contract
     * @throws ArrayIndexOutOfBoundsException if {@code fromIndex < 0} or
     *         {@code toIndex > a.length}
     * @throws ClassCastException if the array contains elements that are
     *         not <i>mutually comparable</i> (for example, strings and
     *         integers).
     */
    public static void sort(Object[] a, int fromIndex, int toIndex) {
        rangeCheck(a.length, fromIndex, toIndex);
        if (LegacyMergeSort.userRequested)
            legacyMergeSort(a, fromIndex, toIndex);
        else
            ComparableTimSort.sort(a, fromIndex, toIndex, null, 0, 0);
    }

    /** To be removed in a future release. */
    private static void legacyMergeSort(Object[] a,
                                        int fromIndex, int toIndex) {
        Object[] aux = copyOfRange(a, fromIndex, toIndex);
        mergeSort(aux, a, fromIndex, toIndex, -fromIndex);
    }

    /**
     * Tuning parameter: list size at or below which insertion sort will be
     * used in preference to mergesort.
     * To be removed in a future release.
     */
    private static final int INSERTIONSORT_THRESHOLD = 7;

    /**
     * Src is the source array that starts at index 0
     * Dest is the (possibly larger) array destination with a possible offset
     * low is the index in dest to start sorting
     * high is the end index in dest to end sorting
     * off is the offset to generate corresponding low, high in src
     * To be removed in a future release.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void mergeSort(Object[] src,
                                  Object[] dest,
                                  int low,
                                  int high,
                                  int off) {
        int length = high - low;

        // Insertion sort on smallest arrays
        if (length < INSERTIONSORT_THRESHOLD) {
            for (int i=low; i<high; i++)
                for (int j=i; j>low &&
                         ((Comparable) dest[j-1]).compareTo(dest[j])>0; j--)
                    swap(dest, j, j-1);
            return;
        }

        // Recursively sort halves of dest into src
        int destLow  = low;
        int destHigh = high;
        low  += off;
        high += off;
        int mid = (low + high) >>> 1;
        mergeSort(dest, src, low, mid, -off);
        mergeSort(dest, src, mid, high, -off);

        // If list is already sorted, just copy from src to dest.  This is an
        // optimization that results in faster sorts for nearly ordered lists.
        if (((Comparable)src[mid-1]).compareTo(src[mid]) <= 0) {
            System.arraycopy(src, low, dest, destLow, length);
            return;
        }

        // Merge sorted halves (now in src) into dest
        for(int i = destLow, p = low, q = mid; i < destHigh; i++) {
            if (q >= high || p < mid && ((Comparable)src[p]).compareTo(src[q])<=0)
                dest[i] = src[p++];
            else
                dest[i] = src[q++];
        }
    }

    /**
     * Swaps x[a] with x[b].
     */
    private static void swap(Object[] x, int a, int b) {
        Object t = x[a];
        x[a] = x[b];
        x[b] = t;
    }

    /**
     * Sorts the specified array of objects according to the order induced by
     * the specified comparator.  All elements in the array must be
     * <i>mutually comparable</i> by the specified comparator (that is,
     * {@code c.compare(e1, e2)} must not throw a {@code ClassCastException}
     * for any elements {@code e1} and {@code e2} in the array).
     *
     * <p>This sort is guaranteed to be <i>stable</i>:  equal elements will
     * not be reordered as a result of the sort.
     *
     * <p>Implementation note: This implementation is a stable, adaptive,
     * iterative mergesort that requires far fewer than n lg(n) comparisons
     * when the input array is partially sorted, while offering the
     * performance of a traditional mergesort when the input array is
     * randomly ordered.  If the input array is nearly sorted, the
     * implementation requires approximately n comparisons.  Temporary
     * storage requirements vary from a small constant for nearly sorted
     * input arrays to n/2 object references for randomly ordered input
     * arrays.
     *
     * <p>The implementation takes equal advantage of ascending and
     * descending order in its input array, and can take advantage of
     * ascending and descending order in different parts of the same
     * input array.  It is well-suited to merging two or more sorted arrays:
     * simply concatenate the arrays and sort the resulting array.
     *
     * <p>The implementation was adapted from Tim Peters's list sort for Python
     * (<a href="http://svn.python.org/projects/python/trunk/Objects/listsort.txt">
     * TimSort</a>).  It uses techniques from Peter McIlroy's "Optimistic
     * Sorting and Information Theoretic Complexity", in Proceedings of the
     * Fourth Annual ACM-SIAM Symposium on Discrete Algorithms, pp 467-474,
     * January 1993.
     *
     * @param <T> the class of the objects to be sorted
     * @param a the array to be sorted
     * @param c the comparator to determine the order of the array.  A
     *        {@code null} value indicates that the elements'
     *        {@linkplain Comparable natural ordering} should be used.
     * @throws ClassCastException if the array contains elements that are
     *         not <i>mutually comparable</i> using the specified comparator
     * @throws IllegalArgumentException (optional) if the comparator is
     *         found to violate the {@link Comparator} contract
     */
    public static <T> void sort(T[] a, Comparator<? super T> c) {
        if (c == null) {
            sort(a);
        } else {
            if (LegacyMergeSort.userRequested)
                legacyMergeSort(a, c);
            else
                TimSort.sort(a, 0, a.length, c, null, 0, 0);
        }
    }

    /** To be removed in a future release. */
    private static <T> void legacyMergeSort(T[] a, Comparator<? super T> c) {
        T[] aux = a.clone();
        if (c==null)
            mergeSort(aux, a, 0, a.length, 0);
        else
            mergeSort(aux, a, 0, a.length, 0, c);
    }

    /**
     * Sorts the specified range of the specified array of objects according
     * to the order induced by the specified comparator.  The range to be
     * sorted extends from index {@code fromIndex}, inclusive, to index
     * {@code toIndex}, exclusive.  (If {@code fromIndex==toIndex}, the
     * range to be sorted is empty.)  All elements in the range must be
     * <i>mutually comparable</i> by the specified comparator (that is,
     * {@code c.compare(e1, e2)} must not throw a {@code ClassCastException}
     * for any elements {@code e1} and {@code e2} in the range).
     *
     * <p>This sort is guaranteed to be <i>stable</i>:  equal elements will
     * not be reordered as a result of the sort.
     *
     * <p>Implementation note: This implementation is a stable, adaptive,
     * iterative mergesort that requires far fewer than n lg(n) comparisons
     * when the input array is partially sorted, while offering the
     * performance of a traditional mergesort when the input array is
     * randomly ordered.  If the input array is nearly sorted, the
     * implementation requires approximately n comparisons.  Temporary
     * storage requirements vary from a small constant for nearly sorted
     * input arrays to n/2 object references for randomly ordered input
     * arrays.
     *
     * <p>The implementation takes equal advantage of ascending and
     * descending order in its input array, and can take advantage of
     * ascending and descending order in different parts of the same
     * input array.  It is well-suited to merging two or more sorted arrays:
     * simply concatenate the arrays and sort the resulting array.
     *
     * <p>The implementation was adapted from Tim Peters's list sort for Python
     * (<a href="http://svn.python.org/projects/python/trunk/Objects/listsort.txt">
     * TimSort</a>).  It uses techniques from Peter McIlroy's "Optimistic
     * Sorting and Information Theoretic Complexity", in Proceedings of the
     * Fourth Annual ACM-SIAM Symposium on Discrete Algorithms, pp 467-474,
     * January 1993.
     *
     * @param <T> the class of the objects to be sorted
     * @param a the array to be sorted
     * @param fromIndex the index of the first element (inclusive) to be
     *        sorted
     * @param toIndex the index of the last element (exclusive) to be sorted
     * @param c the comparator to determine the order of the array.  A
     *        {@code null} value indicates that the elements'
     *        {@linkplain Comparable natural ordering} should be used.
     * @throws ClassCastException if the array contains elements that are not
     *         <i>mutually comparable</i> using the specified comparator.
     * @throws IllegalArgumentException if {@code fromIndex > toIndex} or
     *         (optional) if the comparator is found to violate the
     *         {@link Comparator} contract
     * @throws ArrayIndexOutOfBoundsException if {@code fromIndex < 0} or
     *         {@code toIndex > a.length}
     */
    public static <T> void sort(T[] a, int fromIndex, int toIndex,
                                Comparator<? super T> c) {
        if (c == null) {
            sort(a, fromIndex, toIndex);
        } else {
            rangeCheck(a.length, fromIndex, toIndex);
            if (LegacyMergeSort.userRequested)
                legacyMergeSort(a, fromIndex, toIndex, c);
            else
                TimSort.sort(a, fromIndex, toIndex, c, null, 0, 0);
        }
    }

    /** To be removed in a future release. */
    private static <T> void legacyMergeSort(T[] a, int fromIndex, int toIndex,
                                            Comparator<? super T> c) {
        T[] aux = copyOfRange(a, fromIndex, toIndex);
        if (c==null)
            mergeSort(aux, a, fromIndex, toIndex, -fromIndex);
        else
            mergeSort(aux, a, fromIndex, toIndex, -fromIndex, c);
    }

    /**
     * Src is the source array that starts at index 0
     * Dest is the (possibly larger) array destination with a possible offset
     * low is the index in dest to start sorting
     * high is the end index in dest to end sorting
     * off is the offset into src corresponding to low in dest
     * To be removed in a future release.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void mergeSort(Object[] src,
                                  Object[] dest,
                                  int low, int high, int off,
                                  Comparator c) {
        int length = high - low;

        // Insertion sort on smallest arrays
        if (length < INSERTIONSORT_THRESHOLD) {
            for (int i=low; i<high; i++)
                for (int j=i; j>low && c.compare(dest[j-1], dest[j])>0; j--)
                    swap(dest, j, j-1);
            return;
        }

        // Recursively sort halves of dest into src
        int destLow  = low;
        int destHigh = high;
        low  += off;
        high += off;
        int mid = (low + high) >>> 1;
        mergeSort(dest, src, low, mid, -off, c);
        mergeSort(dest, src, mid, high, -off, c);

        // If list is already sorted, just copy from src to dest.  This is an
        // optimization that results in faster sorts for nearly ordered lists.
        if (c.compare(src[mid-1], src[mid]) <= 0) {
           System.arraycopy(src, low, dest, destLow, length);
           return;
        }

        // Merge sorted halves (now in src) into dest
        for(int i = destLow, p = low, q = mid; i < destHigh; i++) {
            if (q >= high || p < mid && c.compare(src[p], src[q]) <= 0)
                dest[i] = src[p++];
            else
                dest[i] = src[q++];
        }
    }

    // Parallel prefix

    /**
     * Cumulates, in parallel, each element of the given array in place,
     * using the supplied function. For example if the array initially
     * holds {@code [2, 1, 0, 3]} and the operation performs addition,
     * then upon return the array holds {@code [2, 3, 3, 6]}.
     * Parallel prefix computation is usually more efficient than
     * sequential loops for large arrays.
     *
     * @param <T> the class of the objects in the array
     * @param array the array, which is modified in-place by this method
     * @param op a side-effect-free, associative function to perform the
     * cumulation
     * @throws NullPointerException if the specified array or function is null
     * @since 1.8
     */
    public static <T> void parallelPrefix(T[] array, BinaryOperator<T> op) {
        Objects.requireNonNull(op);
        if (array.length > 0)
            new ArrayPrefixHelpers.CumulateTask<>
                    (null, op, array, 0, array.length).invoke();
    }

    /**
     * Performs {@link #parallelPrefix(Object[], BinaryOperator)}
     * for the given subrange of the array.
     *
     * @param <T> the class of the objects in the array
     * @param array the array
     * @param fromIndex the index of the first element, inclusive
     * @param toIndex the index of the last element, exclusive
     * @param op a side-effect-free, associative function to perform the
     * cumulation
     * @throws IllegalArgumentException if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException
     *     if {@code fromIndex < 0} or {@code toIndex > array.length}
     * @throws NullPointerException if the specified array or function is null
     * @since 1.8
     */
    public static <T> void parallelPrefix(T[] array, int fromIndex,
                                          int toIndex, BinaryOperator<T> op) {
        Objects.requireNonNull(op);
        rangeCheck(array.length, fromIndex, toIndex);
        if (fromIndex < toIndex)
            new ArrayPrefixHelpers.CumulateTask<>
                    (null, op, array, fromIndex, toIndex).invoke();
    }

    /**
     * Cumulates, in parallel, each element of the given array in place,
     * using the supplied function. For example if the array initially
     * holds {@code [2, 1, 0, 3]} and the operation performs addition,
     * then upon return the array holds {@code [2, 3, 3, 6]}.
     * Parallel prefix computation is usually more efficient than
     * sequential loops for large arrays.
     *
     * @param array the array, which is modified in-place by this method
     * @param op a side-effect-free, associative function to perform the
     * cumulation
     * @throws NullPointerException if the specified array or function is null
     * @since 1.8
     */
    public static void parallelPrefix(long[] array, LongBinaryOperator op) {
        Objects.requireNonNull(op);
        if (array.length > 0)
            new ArrayPrefixHelpers.LongCumulateTask
                    (null, op, array, 0, array.length).invoke();
    }

    /**
     * Performs {@link #parallelPrefix(long[], LongBinaryOperator)}
     * for the given subrange of the array.
     *
     * @param array the array
     * @param fromIndex the index of the first element, inclusive
     * @param toIndex the index of the last element, exclusive
     * @param op a side-effect-free, associative function to perform the
     * cumulation
     * @throws IllegalArgumentException if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException
     *     if {@code fromIndex < 0} or {@code toIndex > array.length}
     * @throws NullPointerException if the specified array or function is null
     * @since 1.8
     */
    public static void parallelPrefix(long[] array, int fromIndex,
                                      int toIndex, LongBinaryOperator op) {
        Objects.requireNonNull(op);
        rangeCheck(array.length, fromIndex, toIndex);
        if (fromIndex < toIndex)
            new ArrayPrefixHelpers.LongCumulateTask
                    (null, op, array, fromIndex, toIndex).invoke();
    }

    /**
     * Cumulates, in parallel, each element of the given array in place,
     * using the supplied function. For example if the array initially
     * holds {@code [2.0, 1.0, 0.0, 3.0]} and the operation performs addition,
     * then upon return the array holds {@code [2.0, 3.0, 3.0, 6.0]}.
     * Parallel prefix computation is usually more efficient than
     * sequential loops for large arrays.
     *
     * <p> Because floating-point operations may not be strictly associative,
     * the returned result may not be identical to the value that would be
     * obtained if the operation was performed sequentially.
     *
     * @param array the array, which is modified in-place by this method
     * @param op a side-effect-free function to perform the cumulation
     * @throws NullPointerException if the specified array or function is null
     * @since 1.8
     */
    public static void parallelPrefix(double[] array, DoubleBinaryOperator op) {
        Objects.requireNonNull(op);
        if (array.length > 0)
            new ArrayPrefixHelpers.DoubleCumulateTask
                    (null, op, array, 0, array.length).invoke();
    }

    /**
     * Performs {@link #parallelPrefix(double[], DoubleBinaryOperator)}
     * for the given subrange of the array.
     *
     * @param array the array
     * @param fromIndex the index of the first element, inclusive
     * @param toIndex the index of the last element, exclusive
     * @param op a side-effect-free, associative function to perform the
     * cumulation
     * @throws IllegalArgumentException if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException
     *     if {@code fromIndex < 0} or {@code toIndex > array.length}
     * @throws NullPointerException if the specified array or function is null
     * @since 1.8
     */
    public static void parallelPrefix(double[] array, int fromIndex,
                                      int toIndex, DoubleBinaryOperator op) {
        Objects.requireNonNull(op);
        rangeCheck(array.length, fromIndex, toIndex);
        if (fromIndex < toIndex)
            new ArrayPrefixHelpers.DoubleCumulateTask
                    (null, op, array, fromIndex, toIndex).invoke();
    }

    /**
     * Cumulates, in parallel, each element of the given array in place,
     * using the supplied function. For example if the array initially
     * holds {@code [2, 1, 0, 3]} and the operation performs addition,
     * then upon return the array holds {@code [2, 3, 3, 6]}.
     * Parallel prefix computation is usually more efficient than
     * sequential loops for large arrays.
     *
     * @param array the array, which is modified in-place by this method
     * @param op a side-effect-free, associative function to perform the
     * cumulation
     * @throws NullPointerException if the specified array or function is null
     * @since 1.8
     */
    public static void parallelPrefix(int[] array, IntBinaryOperator op) {
        Objects.requireNonNull(op);
        if (array.length > 0)
            new ArrayPrefixHelpers.IntCumulateTask
                    (null, op, array, 0, array.length).invoke();
    }

    /**
     * Performs {@link #parallelPrefix(int[], IntBinaryOperator)}
     * for the given subrange of the array.
     *
     * @param array the array
     * @param fromIndex the index of the first element, inclusive
     * @param toIndex the index of the last element, exclusive
     * @param op a side-effect-free, associative function to perform the
     * cumulation
     * @throws IllegalArgumentException if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException
     *     if {@code fromIndex < 0} or {@code toIndex > array.length}
     * @throws NullPointerException if the specified array or function is null
     * @since 1.8
     */
    public static void parallelPrefix(int[] array, int fromIndex,
                                      int toIndex, IntBinaryOperator op) {
        Objects.requireNonNull(op);
        rangeCheck(array.length, fromIndex, toIndex);
        if (fromIndex < toIndex)
            new ArrayPrefixHelpers.IntCumulateTask
                    (null, op, array, fromIndex, toIndex).invoke();
    }

    // Searching

    /**
     * Searches the specified array of longs for the specified value using the
     * binary search algorithm.  The array must be sorted (as
     * by the {@link #sort(long[])} method) prior to making this call.  If it
     * is not sorted, the results are undefined.  If the array contains
     * multiple elements with the specified value, there is no guarantee which
     * one will be found.
     *
     * @param a the array to be searched
     * @param key the value to be searched for
     * @return index of the search key, if it is contained in the array;
     *         otherwise, <code>(-(<i>insertion point</i>) - 1)</code>.  The
     *         <i>insertion point</i> is defined as the point at which the
     *         key would be inserted into the array: the index of the first
     *         element greater than the key, or {@code a.length} if all
     *         elements in the array are less than the specified key.  Note
     *         that this guarantees that the return value will be &gt;= 0 if
     *         and only if the key is found.
     */
    public static int binarySearch(long[] a, long key) {
        return binarySearch0(a, 0, a.length, key);
    }

    /**
     * Searches a range of
     * the specified array of longs for the specified value using the
     * binary search algorithm.
     * The range must be sorted (as
     * by the {@link #sort(long[], int, int)} method)
     * prior to making this call.  If it
     * is not sorted, the results are undefined.  If the range contains
     * multiple elements with the specified value, there is no guarantee which
     * one will be found.
     *
     * @param a the array to be searched
     * @param fromIndex the index of the first element (inclusive) to be
     *          searched
     * @param toIndex the index of the last element (exclusive) to be searched
     * @param key the value to be searched for
     * @return index of the search key, if it is contained in the array
     *         within the specified range;
     *         otherwise, <code>(-(<i>insertion point</i>) - 1)</code>.  The
     *         <i>insertion point</i> is defined as the point at which the
     *         key would be inserted into the array: the index of the first
     *         element in the range greater than the key,
     *         or {@code toIndex} if all
     *         elements in the range are less than the specified key.  Note
     *         that this guarantees that the return value will be &gt;= 0 if
     *         and only if the key is found.
     * @throws IllegalArgumentException
     *         if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code fromIndex < 0 or toIndex > a.length}
     * @since 1.6
     */
    public static int binarySearch(long[] a, int fromIndex, int toIndex,
                                   long key) {
        rangeCheck(a.length, fromIndex, toIndex);
        return binarySearch0(a, fromIndex, toIndex, key);
    }

    // Like public version, but without range checks.
    private static int binarySearch0(long[] a, int fromIndex, int toIndex,
                                     long key) {
        int low = fromIndex;
        int high = toIndex - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            long midVal = a[mid];

            if (midVal < key)
                low = mid + 1;
            else if (midVal > key)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found.
    }

    /**
     * Searches the specified array of ints for the specified value using the
     * binary search algorithm.  The array must be sorted (as
     * by the {@link #sort(int[])} method) prior to making this call.  If it
     * is not sorted, the results are undefined.  If the array contains
     * multiple elements with the specified value, there is no guarantee which
     * one will be found.
     *
     * @param a the array to be searched
     * @param key the value to be searched for
     * @return index of the search key, if it is contained in the array;
     *         otherwise, <code>(-(<i>insertion point</i>) - 1)</code>.  The
     *         <i>insertion point</i> is defined as the point at which the
     *         key would be inserted into the array: the index of the first
     *         element greater than the key, or {@code a.length} if all
     *         elements in the array are less than the specified key.  Note
     *         that this guarantees that the return value will be &gt;= 0 if
     *         and only if the key is found.
     */
    public static int binarySearch(int[] a, int key) {
        return binarySearch0(a, 0, a.length, key);
    }

    /**
     * Searches a range of
     * the specified array of ints for the specified value using the
     * binary search algorithm.
     * The range must be sorted (as
     * by the {@link #sort(int[], int, int)} method)
     * prior to making this call.  If it
     * is not sorted, the results are undefined.  If the range contains
     * multiple elements with the specified value, there is no guarantee which
     * one will be found.
     *
     * @param a the array to be searched
     * @param fromIndex the index of the first element (inclusive) to be
     *          searched
     * @param toIndex the index of the last element (exclusive) to be searched
     * @param key the value to be searched for
     * @return index of the search key, if it is contained in the array
     *         within the specified range;
     *         otherwise, <code>(-(<i>insertion point</i>) - 1)</code>.  The
     *         <i>insertion point</i> is defined as the point at which the
     *         key would be inserted into the array: the index of the first
     *         element in the range greater than the key,
     *         or {@code toIndex} if all
     *         elements in the range are less than the specified key.  Note
     *         that this guarantees that the return value will be &gt;= 0 if
     *         and only if the key is found.
     * @throws IllegalArgumentException
     *         if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code fromIndex < 0 or toIndex > a.length}
     * @since 1.6
     */
    public static int binarySearch(int[] a, int fromIndex, int toIndex,
                                   int key) {
        rangeCheck(a.length, fromIndex, toIndex);
        return binarySearch0(a, fromIndex, toIndex, key);
    }

    // Like public version, but without range checks.
    private static int binarySearch0(int[] a, int fromIndex, int toIndex,
                                     int key) {
        int low = fromIndex;
        int high = toIndex - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midVal = a[mid];

            if (midVal < key)
                low = mid + 1;
            else if (midVal > key)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found.
    }

    /**
     * Searches the specified array of shorts for the specified value using
     * the binary search algorithm.  The array must be sorted
     * (as by the {@link #sort(short[])} method) prior to making this call.  If
     * it is not sorted, the results are undefined.  If the array contains
     * multiple elements with the specified value, there is no guarantee which
     * one will be found.
     *
     * @param a the array to be searched
     * @param key the value to be searched for
     * @return index of the search key, if it is contained in the array;
     *         otherwise, <code>(-(<i>insertion point</i>) - 1)</code>.  The
     *         <i>insertion point</i> is defined as the point at which the
     *         key would be inserted into the array: the index of the first
     *         element greater than the key, or {@code a.length} if all
     *         elements in the array are less than the specified key.  Note
     *         that this guarantees that the return value will be &gt;= 0 if
     *         and only if the key is found.
     */
    public static int binarySearch(short[] a, short key) {
        return binarySearch0(a, 0, a.length, key);
    }

    /**
     * Searches a range of
     * the specified array of shorts for the specified value using
     * the binary search algorithm.
     * The range must be sorted
     * (as by the {@link #sort(short[], int, int)} method)
     * prior to making this call.  If
     * it is not sorted, the results are undefined.  If the range contains
     * multiple elements with the specified value, there is no guarantee which
     * one will be found.
     *
     * @param a the array to be searched
     * @param fromIndex the index of the first element (inclusive) to be
     *          searched
     * @param toIndex the index of the last element (exclusive) to be searched
     * @param key the value to be searched for
     * @return index of the search key, if it is contained in the array
     *         within the specified range;
     *         otherwise, <code>(-(<i>insertion point</i>) - 1)</code>.  The
     *         <i>insertion point</i> is defined as the point at which the
     *         key would be inserted into the array: the index of the first
     *         element in the range greater than the key,
     *         or {@code toIndex} if all
     *         elements in the range are less than the specified key.  Note
     *         that this guarantees that the return value will be &gt;= 0 if
     *         and only if the key is found.
     * @throws IllegalArgumentException
     *         if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code fromIndex < 0 or toIndex > a.length}
     * @since 1.6
     */
    public static int binarySearch(short[] a, int fromIndex, int toIndex,
                                   short key) {
        rangeCheck(a.length, fromIndex, toIndex);
        return binarySearch0(a, fromIndex, toIndex, key);
    }

    // Like public version, but without range checks.
    private static int binarySearch0(short[] a, int fromIndex, int toIndex,
                                     short key) {
        int low = fromIndex;
        int high = toIndex - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            short midVal = a[mid];

            if (midVal < key)
                low = mid + 1;
            else if (midVal > key)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found.
    }

    /**
     * Searches the specified array of chars for the specified value using the
     * binary search algorithm.  The array must be sorted (as
     * by the {@link #sort(char[])} method) prior to making this call.  If it
     * is not sorted, the results are undefined.  If the array contains
     * multiple elements with the specified value, there is no guarantee which
     * one will be found.
     *
     * @param a the array to be searched
     * @param key the value to be searched for
     * @return index of the search key, if it is contained in the array;
     *         otherwise, <code>(-(<i>insertion point</i>) - 1)</code>.  The
     *         <i>insertion point</i> is defined as the point at which the
     *         key would be inserted into the array: the index of the first
     *         element greater than the key, or {@code a.length} if all
     *         elements in the array are less than the specified key.  Note
     *         that this guarantees that the return value will be &gt;= 0 if
     *         and only if the key is found.
     */
    public static int binarySearch(char[] a, char key) {
        return binarySearch0(a, 0, a.length, key);
    }

    /**
     * Searches a range of
     * the specified array of chars for the specified value using the
     * binary search algorithm.
     * The range must be sorted (as
     * by the {@link #sort(char[], int, int)} method)
     * prior to making this call.  If it
     * is not sorted, the results are undefined.  If the range contains
     * multiple elements with the specified value, there is no guarantee which
     * one will be found.
     *
     * @param a the array to be searched
     * @param fromIndex the index of the first element (inclusive) to be
     *          searched
     * @param toIndex the index of the last element (exclusive) to be searched
     * @param key the value to be searched for
     * @return index of the search key, if it is contained in the array
     *         within the specified range;
     *         otherwise, <code>(-(<i>insertion point</i>) - 1)</code>.  The
     *         <i>insertion point</i> is defined as the point at which the
     *         key would be inserted into the array: the index of the first
     *         element in the range greater than the key,
     *         or {@code toIndex} if all
     *         elements in the range are less than the specified key.  Note
     *         that this guarantees that the return value will be &gt;= 0 if
     *         and only if the key is found.
     * @throws IllegalArgumentException
     *         if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code fromIndex < 0 or toIndex > a.length}
     * @since 1.6
     */
    public static int binarySearch(char[] a, int fromIndex, int toIndex,
                                   char key) {
        rangeCheck(a.length, fromIndex, toIndex);
        return binarySearch0(a, fromIndex, toIndex, key);
    }

    // Like public version, but without range checks.
    private static int binarySearch0(char[] a, int fromIndex, int toIndex,
                                     char key) {
        int low = fromIndex;
        int high = toIndex - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            char midVal = a[mid];

            if (midVal < key)
                low = mid + 1;
            else if (midVal > key)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found.
    }

    /**
     * Searches the specified array of bytes for the specified value using the
     * binary search algorithm.  The array must be sorted (as
     * by the {@link #sort(byte[])} method) prior to making this call.  If it
     * is not sorted, the results are undefined.  If the array contains
     * multiple elements with the specified value, there is no guarantee which
     * one will be found.
     *
     * @param a the array to be searched
     * @param key the value to be searched for
     * @return index of the search key, if it is contained in the array;
     *         otherwise, <code>(-(<i>insertion point</i>) - 1)</code>.  The
     *         <i>insertion point</i> is defined as the point at which the
     *         key would be inserted into the array: the index of the first
     *         element greater than the key, or {@code a.length} if all
     *         elements in the array are less than the specified key.  Note
     *         that this guarantees that the return value will be &gt;= 0 if
     *         and only if the key is found.
     */
    public static int binarySearch(byte[] a, byte key) {
        return binarySearch0(a, 0, a.length, key);
    }

    /**
     * Searches a range of
     * the specified array of bytes for the specified value using the
     * binary search algorithm.
     * The range must be sorted (as
     * by the {@link #sort(byte[], int, int)} method)
     * prior to making this call.  If it
     * is not sorted, the results are undefined.  If the range contains
     * multiple elements with the specified value, there is no guarantee which
     * one will be found.
     *
     * @param a the array to be searched
     * @param fromIndex the index of the first element (inclusive) to be
     *          searched
     * @param toIndex the index of the last element (exclusive) to be searched
     * @param key the value to be searched for
     * @return index of the search key, if it is contained in the array
     *         within the specified range;
     *         otherwise, <code>(-(<i>insertion point</i>) - 1)</code>.  The
     *         <i>insertion point</i> is defined as the point at which the
     *         key would be inserted into the array: the index of the first
     *         element in the range greater than the key,
     *         or {@code toIndex} if all
     *         elements in the range are less than the specified key.  Note
     *         that this guarantees that the return value will be &gt;= 0 if
     *         and only if the key is found.
     * @throws IllegalArgumentException
     *         if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code fromIndex < 0 or toIndex > a.length}
     * @since 1.6
     */
    public static int binarySearch(byte[] a, int fromIndex, int toIndex,
                                   byte key) {
        rangeCheck(a.length, fromIndex, toIndex);
        return binarySearch0(a, fromIndex, toIndex, key);
    }

    // Like public version, but without range checks.
    private static int binarySearch0(byte[] a, int fromIndex, int toIndex,
                                     byte key) {
        int low = fromIndex;
        int high = toIndex - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            byte midVal = a[mid];

            if (midVal < key)
                low = mid + 1;
            else if (midVal > key)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found.
    }

    /**
     * Searches the specified array of doubles for the specified value using
     * the binary search algorithm.  The array must be sorted
     * (as by the {@link #sort(double[])} method) prior to making this call.
     * If it is not sorted, the results are undefined.  If the array contains
     * multiple elements with the specified value, there is no guarantee which
     * one will be found.  This method considers all NaN values to be
     * equivalent and equal.
     *
     * @param a the array to be searched
     * @param key the value to be searched for
     * @return index of the search key, if it is contained in the array;
     *         otherwise, <code>(-(<i>insertion point</i>) - 1)</code>.  The
     *         <i>insertion point</i> is defined as the point at which the
     *         key would be inserted into the array: the index of the first
     *         element greater than the key, or {@code a.length} if all
     *         elements in the array are less than the specified key.  Note
     *         that this guarantees that the return value will be &gt;= 0 if
     *         and only if the key is found.
     */
    public static int binarySearch(double[] a, double key) {
        return binarySearch0(a, 0, a.length, key);
    }

    /**
     * Searches a range of
     * the specified array of doubles for the specified value using
     * the binary search algorithm.
     * The range must be sorted
     * (as by the {@link #sort(double[], int, int)} method)
     * prior to making this call.
     * If it is not sorted, the results are undefined.  If the range contains
     * multiple elements with the specified value, there is no guarantee which
     * one will be found.  This method considers all NaN values to be
     * equivalent and equal.
     *
     * @param a the array to be searched
     * @param fromIndex the index of the first element (inclusive) to be
     *          searched
     * @param toIndex the index of the last element (exclusive) to be searched
     * @param key the value to be searched for
     * @return index of the search key, if it is contained in the array
     *         within the specified range;
     *         otherwise, <code>(-(<i>insertion point</i>) - 1)</code>.  The
     *         <i>insertion point</i> is defined as the point at which the
     *         key would be inserted into the array: the index of the first
     *         element in the range greater than the key,
     *         or {@code toIndex} if all
     *         elements in the range are less than the specified key.  Note
     *         that this guarantees that the return value will be &gt;= 0 if
     *         and only if the key is found.
     * @throws IllegalArgumentException
     *         if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code fromIndex < 0 or toIndex > a.length}
     * @since 1.6
     */
    public static int binarySearch(double[] a, int fromIndex, int toIndex,
                                   double key) {
        rangeCheck(a.length, fromIndex, toIndex);
        return binarySearch0(a, fromIndex, toIndex, key);
    }

    // Like public version, but without range checks.
    private static int binarySearch0(double[] a, int fromIndex, int toIndex,
                                     double key) {
        int low = fromIndex;
        int high = toIndex - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            double midVal = a[mid];

            if (midVal < key)
                low = mid + 1;  // Neither val is NaN, thisVal is smaller
            else if (midVal > key)
                high = mid - 1; // Neither val is NaN, thisVal is larger
            else {
                long midBits = Double.doubleToLongBits(midVal);
                long keyBits = Double.doubleToLongBits(key);
                if (midBits == keyBits)     // Values are equal
                    return mid;             // Key found
                else if (midBits < keyBits) // (-0.0, 0.0) or (!NaN, NaN)
                    low = mid + 1;
                else                        // (0.0, -0.0) or (NaN, !NaN)
                    high = mid - 1;
            }
        }
        return -(low + 1);  // key not found.
    }

    /**
     * Searches the specified array of floats for the specified value using
     * the binary search algorithm. The array must be sorted
     * (as by the {@link #sort(float[])} method) prior to making this call. If
     * it is not sorted, the results are undefined. If the array contains
     * multiple elements with the specified value, there is no guarantee which
     * one will be found. This method considers all NaN values to be
     * equivalent and equal.
     *
     * @param a the array to be searched
     * @param key the value to be searched for
     * @return index of the search key, if it is contained in the array;
     *         otherwise, <code>(-(<i>insertion point</i>) - 1)</code>. The
     *         <i>insertion point</i> is defined as the point at which the
     *         key would be inserted into the array: the index of the first
     *         element greater than the key, or {@code a.length} if all
     *         elements in the array are less than the specified key. Note
     *         that this guarantees that the return value will be &gt;= 0 if
     *         and only if the key is found.
     */
    public static int binarySearch(float[] a, float key) {
        return binarySearch0(a, 0, a.length, key);
    }

    /**
     * Searches a range of
     * the specified array of floats for the specified value using
     * the binary search algorithm.
     * The range must be sorted
     * (as by the {@link #sort(float[], int, int)} method)
     * prior to making this call. If
     * it is not sorted, the results are undefined. If the range contains
     * multiple elements with the specified value, there is no guarantee which
     * one will be found. This method considers all NaN values to be
     * equivalent and equal.
     *
     * @param a the array to be searched
     * @param fromIndex the index of the first element (inclusive) to be
     *          searched
     * @param toIndex the index of the last element (exclusive) to be searched
     * @param key the value to be searched for
     * @return index of the search key, if it is contained in the array
     *         within the specified range;
     *         otherwise, <code>(-(<i>insertion point</i>) - 1)</code>. The
     *         <i>insertion point</i> is defined as the point at which the
     *         key would be inserted into the array: the index of the first
     *         element in the range greater than the key,
     *         or {@code toIndex} if all
     *         elements in the range are less than the specified key. Note
     *         that this guarantees that the return value will be &gt;= 0 if
     *         and only if the key is found.
     * @throws IllegalArgumentException
     *         if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code fromIndex < 0 or toIndex > a.length}
     * @since 1.6
     */
    public static int binarySearch(float[] a, int fromIndex, int toIndex,
                                   float key) {
        rangeCheck(a.length, fromIndex, toIndex);
        return binarySearch0(a, fromIndex, toIndex, key);
    }

    // Like public version, but without range checks.
    private static int binarySearch0(float[] a, int fromIndex, int toIndex,
                                     float key) {
        int low = fromIndex;
        int high = toIndex - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            float midVal = a[mid];

            if (midVal < key)
                low = mid + 1;  // Neither val is NaN, thisVal is smaller
            else if (midVal > key)
                high = mid - 1; // Neither val is NaN, thisVal is larger
            else {
                int midBits = Float.floatToIntBits(midVal);
                int keyBits = Float.floatToIntBits(key);
                if (midBits == keyBits)     // Values are equal
                    return mid;             // Key found
                else if (midBits < keyBits) // (-0.0, 0.0) or (!NaN, NaN)
                    low = mid + 1;
                else                        // (0.0, -0.0) or (NaN, !NaN)
                    high = mid - 1;
            }
        }
        return -(low + 1);  // key not found.
    }

    /**
     * Searches the specified array for the specified object using the binary
     * search algorithm. The array must be sorted into ascending order
     * according to the
     * {@linkplain Comparable natural ordering}
     * of its elements (as by the
     * {@link #sort(Object[])} method) prior to making this call.
     * If it is not sorted, the results are undefined.
     * (If the array contains elements that are not mutually comparable (for
     * example, strings and integers), it <i>cannot</i> be sorted according
     * to the natural ordering of its elements, hence results are undefined.)
     * If the array contains multiple
     * elements equal to the specified object, there is no guarantee which
     * one will be found.
     *
     * @param a the array to be searched
     * @param key the value to be searched for
     * @return index of the search key, if it is contained in the array;
     *         otherwise, <code>(-(<i>insertion point</i>) - 1)</code>.  The
     *         <i>insertion point</i> is defined as the point at which the
     *         key would be inserted into the array: the index of the first
     *         element greater than the key, or {@code a.length} if all
     *         elements in the array are less than the specified key.  Note
     *         that this guarantees that the return value will be &gt;= 0 if
     *         and only if the key is found.
     * @throws ClassCastException if the search key is not comparable to the
     *         elements of the array.
     */
    public static int binarySearch(Object[] a, Object key) {
        return binarySearch0(a, 0, a.length, key);
    }

    /**
     * Searches a range of
     * the specified array for the specified object using the binary
     * search algorithm.
     * The range must be sorted into ascending order
     * according to the
     * {@linkplain Comparable natural ordering}
     * of its elements (as by the
     * {@link #sort(Object[], int, int)} method) prior to making this
     * call.  If it is not sorted, the results are undefined.
     * (If the range contains elements that are not mutually comparable (for
     * example, strings and integers), it <i>cannot</i> be sorted according
     * to the natural ordering of its elements, hence results are undefined.)
     * If the range contains multiple
     * elements equal to the specified object, there is no guarantee which
     * one will be found.
     *
     * @param a the array to be searched
     * @param fromIndex the index of the first element (inclusive) to be
     *          searched
     * @param toIndex the index of the last element (exclusive) to be searched
     * @param key the value to be searched for
     * @return index of the search key, if it is contained in the array
     *         within the specified range;
     *         otherwise, <code>(-(<i>insertion point</i>) - 1)</code>.  The
     *         <i>insertion point</i> is defined as the point at which the
     *         key would be inserted into the array: the index of the first
     *         element in the range greater than the key,
     *         or {@code toIndex} if all
     *         elements in the range are less than the specified key.  Note
     *         that this guarantees that the return value will be &gt;= 0 if
     *         and only if the key is found.
     * @throws ClassCastException if the search key is not comparable to the
     *         elements of the array within the specified range.
     * @throws IllegalArgumentException
     *         if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code fromIndex < 0 or toIndex > a.length}
     * @since 1.6
     */
    public static int binarySearch(Object[] a, int fromIndex, int toIndex,
                                   Object key) {
        rangeCheck(a.length, fromIndex, toIndex);
        return binarySearch0(a, fromIndex, toIndex, key);
    }

    // Like public version, but without range checks.
    private static int binarySearch0(Object[] a, int fromIndex, int toIndex,
                                     Object key) {
        int low = fromIndex;
        int high = toIndex - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            @SuppressWarnings("rawtypes")
            Comparable midVal = (Comparable)a[mid];
            @SuppressWarnings("unchecked")
            int cmp = midVal.compareTo(key);

            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found.
    }

    /**
     * Searches the specified array for the specified object using the binary
     * search algorithm.  The array must be sorted into ascending order
     * according to the specified comparator (as by the
     * {@link #sort(Object[], Comparator) sort(T[], Comparator)}
     * method) prior to making this call.  If it is
     * not sorted, the results are undefined.
     * If the array contains multiple
     * elements equal to the specified object, there is no guarantee which one
     * will be found.
     *
     * @param <T> the class of the objects in the array
     * @param a the array to be searched
     * @param key the value to be searched for
     * @param c the comparator by which the array is ordered.  A
     *        {@code null} value indicates that the elements'
     *        {@linkplain Comparable natural ordering} should be used.
     * @return index of the search key, if it is contained in the array;
     *         otherwise, <code>(-(<i>insertion point</i>) - 1)</code>.  The
     *         <i>insertion point</i> is defined as the point at which the
     *         key would be inserted into the array: the index of the first
     *         element greater than the key, or {@code a.length} if all
     *         elements in the array are less than the specified key.  Note
     *         that this guarantees that the return value will be &gt;= 0 if
     *         and only if the key is found.
     * @throws ClassCastException if the array contains elements that are not
     *         <i>mutually comparable</i> using the specified comparator,
     *         or the search key is not comparable to the
     *         elements of the array using this comparator.
     */
    public static <T> int binarySearch(T[] a, T key, Comparator<? super T> c) {
        return binarySearch0(a, 0, a.length, key, c);
    }

    /**
     * Searches a range of
     * the specified array for the specified object using the binary
     * search algorithm.
     * The range must be sorted into ascending order
     * according to the specified comparator (as by the
     * {@link #sort(Object[], int, int, Comparator)
     * sort(T[], int, int, Comparator)}
     * method) prior to making this call.
     * If it is not sorted, the results are undefined.
     * If the range contains multiple elements equal to the specified object,
     * there is no guarantee which one will be found.
     *
     * @param <T> the class of the objects in the array
     * @param a the array to be searched
     * @param fromIndex the index of the first element (inclusive) to be
     *          searched
     * @param toIndex the index of the last element (exclusive) to be searched
     * @param key the value to be searched for
     * @param c the comparator by which the array is ordered.  A
     *        {@code null} value indicates that the elements'
     *        {@linkplain Comparable natural ordering} should be used.
     * @return index of the search key, if it is contained in the array
     *         within the specified range;
     *         otherwise, <code>(-(<i>insertion point</i>) - 1)</code>.  The
     *         <i>insertion point</i> is defined as the point at which the
     *         key would be inserted into the array: the index of the first
     *         element in the range greater than the key,
     *         or {@code toIndex} if all
     *         elements in the range are less than the specified key.  Note
     *         that this guarantees that the return value will be &gt;= 0 if
     *         and only if the key is found.
     * @throws ClassCastException if the range contains elements that are not
     *         <i>mutually comparable</i> using the specified comparator,
     *         or the search key is not comparable to the
     *         elements in the range using this comparator.
     * @throws IllegalArgumentException
     *         if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code fromIndex < 0 or toIndex > a.length}
     * @since 1.6
     */
    public static <T> int binarySearch(T[] a, int fromIndex, int toIndex,
                                       T key, Comparator<? super T> c) {
        rangeCheck(a.length, fromIndex, toIndex);
        return binarySearch0(a, fromIndex, toIndex, key, c);
    }

    // Like public version, but without range checks.
    private static <T> int binarySearch0(T[] a, int fromIndex, int toIndex,
                                         T key, Comparator<? super T> c) {
        if (c == null) {
            return binarySearch0(a, fromIndex, toIndex, key);
        }
        int low = fromIndex;
        int high = toIndex - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            T midVal = a[mid];
            int cmp = c.compare(midVal, key);
            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found.
    }

    // Equality Testing

    /**
     * Returns {@code true} if the two specified arrays of longs are
     * <i>equal</i> to one another.  Two arrays are considered equal if both
     * arrays contain the same number of elements, and all corresponding pairs
     * of elements in the two arrays are equal.  In other words, two arrays
     * are equal if they contain the same elements in the same order.  Also,
     * two array references are considered equal if both are {@code null}.
     *
     * @param a one array to be tested for equality
     * @param a2 the other array to be tested for equality
     * @return {@code true} if the two arrays are equal
     */
    public static boolean equals(long[] a, long[] a2) {
        if (a==a2)
            return true;
        if (a==null || a2==null)
            return false;

        int length = a.length;
        if (a2.length != length)
            return false;

        return ArraysSupport.mismatch(a, a2, length) < 0;
    }

    /**
     * Returns true if the two specified arrays of longs, over the specified
     * ranges, are <i>equal</i> to one another.
     *
     * <p>Two arrays are considered equal if the number of elements covered by
     * each range is the same, and all corresponding pairs of elements over the
     * specified ranges in the two arrays are equal.  In other words, two arrays
     * are equal if they contain, over the specified ranges, the same elements
     * in the same order.
     *
     * @param a the first array to be tested for equality
     * @param aFromIndex the index (inclusive) of the first element in the
     *                   first array to be tested
     * @param aToIndex the index (exclusive) of the last element in the
     *                 first array to be tested
     * @param b the second array to be tested for equality
     * @param bFromIndex the index (inclusive) of the first element in the
     *                   second array to be tested
     * @param bToIndex the index (exclusive) of the last element in the
     *                 second array to be tested
     * @return {@code true} if the two arrays, over the specified ranges, are
     *         equal
     * @throws IllegalArgumentException
     *         if {@code aFromIndex > aToIndex} or
     *         if {@code bFromIndex > bToIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code aFromIndex < 0 or aToIndex > a.length} or
     *         if {@code bFromIndex < 0 or bToIndex > b.length}
     * @throws NullPointerException
     *         if either array is {@code null}
     * @since 9
     */
    public static boolean equals(long[] a, int aFromIndex, int aToIndex,
                                 long[] b, int bFromIndex, int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        if (aLength != bLength)
            return false;

        return ArraysSupport.mismatch(a, aFromIndex,
                                      b, bFromIndex,
                                      aLength) < 0;
    }

    /**
     * Returns {@code true} if the two specified arrays of ints are
     * <i>equal</i> to one another.  Two arrays are considered equal if both
     * arrays contain the same number of elements, and all corresponding pairs
     * of elements in the two arrays are equal.  In other words, two arrays
     * are equal if they contain the same elements in the same order.  Also,
     * two array references are considered equal if both are {@code null}.
     *
     * @param a one array to be tested for equality
     * @param a2 the other array to be tested for equality
     * @return {@code true} if the two arrays are equal
     */
    public static boolean equals(int[] a, int[] a2) {
        if (a==a2)
            return true;
        if (a==null || a2==null)
            return false;

        int length = a.length;
        if (a2.length != length)
            return false;

        return ArraysSupport.mismatch(a, a2, length) < 0;
    }

    /**
     * Returns true if the two specified arrays of ints, over the specified
     * ranges, are <i>equal</i> to one another.
     *
     * <p>Two arrays are considered equal if the number of elements covered by
     * each range is the same, and all corresponding pairs of elements over the
     * specified ranges in the two arrays are equal.  In other words, two arrays
     * are equal if they contain, over the specified ranges, the same elements
     * in the same order.
     *
     * @param a the first array to be tested for equality
     * @param aFromIndex the index (inclusive) of the first element in the
     *                   first array to be tested
     * @param aToIndex the index (exclusive) of the last element in the
     *                 first array to be tested
     * @param b the second array to be tested for equality
     * @param bFromIndex the index (inclusive) of the first element in the
     *                   second array to be tested
     * @param bToIndex the index (exclusive) of the last element in the
     *                 second array to be tested
     * @return {@code true} if the two arrays, over the specified ranges, are
     *         equal
     * @throws IllegalArgumentException
     *         if {@code aFromIndex > aToIndex} or
     *         if {@code bFromIndex > bToIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code aFromIndex < 0 or aToIndex > a.length} or
     *         if {@code bFromIndex < 0 or bToIndex > b.length}
     * @throws NullPointerException
     *         if either array is {@code null}
     * @since 9
     */
    public static boolean equals(int[] a, int aFromIndex, int aToIndex,
                                 int[] b, int bFromIndex, int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        if (aLength != bLength)
            return false;

        return ArraysSupport.mismatch(a, aFromIndex,
                                      b, bFromIndex,
                                      aLength) < 0;
    }

    /**
     * Returns {@code true} if the two specified arrays of shorts are
     * <i>equal</i> to one another.  Two arrays are considered equal if both
     * arrays contain the same number of elements, and all corresponding pairs
     * of elements in the two arrays are equal.  In other words, two arrays
     * are equal if they contain the same elements in the same order.  Also,
     * two array references are considered equal if both are {@code null}.
     *
     * @param a one array to be tested for equality
     * @param a2 the other array to be tested for equality
     * @return {@code true} if the two arrays are equal
     */
    public static boolean equals(short[] a, short a2[]) {
        if (a==a2)
            return true;
        if (a==null || a2==null)
            return false;

        int length = a.length;
        if (a2.length != length)
            return false;

        return ArraysSupport.mismatch(a, a2, length) < 0;
    }

    /**
     * Returns true if the two specified arrays of shorts, over the specified
     * ranges, are <i>equal</i> to one another.
     *
     * <p>Two arrays are considered equal if the number of elements covered by
     * each range is the same, and all corresponding pairs of elements over the
     * specified ranges in the two arrays are equal.  In other words, two arrays
     * are equal if they contain, over the specified ranges, the same elements
     * in the same order.
     *
     * @param a the first array to be tested for equality
     * @param aFromIndex the index (inclusive) of the first element in the
     *                   first array to be tested
     * @param aToIndex the index (exclusive) of the last element in the
     *                 first array to be tested
     * @param b the second array to be tested for equality
     * @param bFromIndex the index (inclusive) of the first element in the
     *                   second array to be tested
     * @param bToIndex the index (exclusive) of the last element in the
     *                 second array to be tested
     * @return {@code true} if the two arrays, over the specified ranges, are
     *         equal
     * @throws IllegalArgumentException
     *         if {@code aFromIndex > aToIndex} or
     *         if {@code bFromIndex > bToIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code aFromIndex < 0 or aToIndex > a.length} or
     *         if {@code bFromIndex < 0 or bToIndex > b.length}
     * @throws NullPointerException
     *         if either array is {@code null}
     * @since 9
     */
    public static boolean equals(short[] a, int aFromIndex, int aToIndex,
                                 short[] b, int bFromIndex, int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        if (aLength != bLength)
            return false;

        return ArraysSupport.mismatch(a, aFromIndex,
                                      b, bFromIndex,
                                      aLength) < 0;
    }

    /**
     * Returns {@code true} if the two specified arrays of chars are
     * <i>equal</i> to one another.  Two arrays are considered equal if both
     * arrays contain the same number of elements, and all corresponding pairs
     * of elements in the two arrays are equal.  In other words, two arrays
     * are equal if they contain the same elements in the same order.  Also,
     * two array references are considered equal if both are {@code null}.
     *
     * @param a one array to be tested for equality
     * @param a2 the other array to be tested for equality
     * @return {@code true} if the two arrays are equal
     */
    @IntrinsicCandidate
    public static boolean equals(char[] a, char[] a2) {
        if (a==a2)
            return true;
        if (a==null || a2==null)
            return false;

        int length = a.length;
        if (a2.length != length)
            return false;

        return ArraysSupport.mismatch(a, a2, length) < 0;
    }

    /**
     * Returns true if the two specified arrays of chars, over the specified
     * ranges, are <i>equal</i> to one another.
     *
     * <p>Two arrays are considered equal if the number of elements covered by
     * each range is the same, and all corresponding pairs of elements over the
     * specified ranges in the two arrays are equal.  In other words, two arrays
     * are equal if they contain, over the specified ranges, the same elements
     * in the same order.
     *
     * @param a the first array to be tested for equality
     * @param aFromIndex the index (inclusive) of the first element in the
     *                   first array to be tested
     * @param aToIndex the index (exclusive) of the last element in the
     *                 first array to be tested
     * @param b the second array to be tested for equality
     * @param bFromIndex the index (inclusive) of the first element in the
     *                   second array to be tested
     * @param bToIndex the index (exclusive) of the last element in the
     *                 second array to be tested
     * @return {@code true} if the two arrays, over the specified ranges, are
     *         equal
     * @throws IllegalArgumentException
     *         if {@code aFromIndex > aToIndex} or
     *         if {@code bFromIndex > bToIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code aFromIndex < 0 or aToIndex > a.length} or
     *         if {@code bFromIndex < 0 or bToIndex > b.length}
     * @throws NullPointerException
     *         if either array is {@code null}
     * @since 9
     */
    public static boolean equals(char[] a, int aFromIndex, int aToIndex,
                                 char[] b, int bFromIndex, int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        if (aLength != bLength)
            return false;

        return ArraysSupport.mismatch(a, aFromIndex,
                                      b, bFromIndex,
                                      aLength) < 0;
    }

    /**
     * Returns {@code true} if the two specified arrays of bytes are
     * <i>equal</i> to one another.  Two arrays are considered equal if both
     * arrays contain the same number of elements, and all corresponding pairs
     * of elements in the two arrays are equal.  In other words, two arrays
     * are equal if they contain the same elements in the same order.  Also,
     * two array references are considered equal if both are {@code null}.
     *
     * @param a one array to be tested for equality
     * @param a2 the other array to be tested for equality
     * @return {@code true} if the two arrays are equal
     */
    @IntrinsicCandidate
    public static boolean equals(byte[] a, byte[] a2) {
        if (a==a2)
            return true;
        if (a==null || a2==null)
            return false;

        int length = a.length;
        if (a2.length != length)
            return false;

        return ArraysSupport.mismatch(a, a2, length) < 0;
    }

    /**
     * Returns true if the two specified arrays of bytes, over the specified
     * ranges, are <i>equal</i> to one another.
     *
     * <p>Two arrays are considered equal if the number of elements covered by
     * each range is the same, and all corresponding pairs of elements over the
     * specified ranges in the two arrays are equal.  In other words, two arrays
     * are equal if they contain, over the specified ranges, the same elements
     * in the same order.
     *
     * @param a the first array to be tested for equality
     * @param aFromIndex the index (inclusive) of the first element in the
     *                   first array to be tested
     * @param aToIndex the index (exclusive) of the last element in the
     *                 first array to be tested
     * @param b the second array to be tested for equality
     * @param bFromIndex the index (inclusive) of the first element in the
     *                   second array to be tested
     * @param bToIndex the index (exclusive) of the last element in the
     *                 second array to be tested
     * @return {@code true} if the two arrays, over the specified ranges, are
     *         equal
     * @throws IllegalArgumentException
     *         if {@code aFromIndex > aToIndex} or
     *         if {@code bFromIndex > bToIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code aFromIndex < 0 or aToIndex > a.length} or
     *         if {@code bFromIndex < 0 or bToIndex > b.length}
     * @throws NullPointerException
     *         if either array is {@code null}
     * @since 9
     */
    public static boolean equals(byte[] a, int aFromIndex, int aToIndex,
                                 byte[] b, int bFromIndex, int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        if (aLength != bLength)
            return false;

        return ArraysSupport.mismatch(a, aFromIndex,
                                      b, bFromIndex,
                                      aLength) < 0;
    }

    /**
     * Returns {@code true} if the two specified arrays of booleans are
     * <i>equal</i> to one another.  Two arrays are considered equal if both
     * arrays contain the same number of elements, and all corresponding pairs
     * of elements in the two arrays are equal.  In other words, two arrays
     * are equal if they contain the same elements in the same order.  Also,
     * two array references are considered equal if both are {@code null}.
     *
     * @param a one array to be tested for equality
     * @param a2 the other array to be tested for equality
     * @return {@code true} if the two arrays are equal
     */
    public static boolean equals(boolean[] a, boolean[] a2) {
        if (a==a2)
            return true;
        if (a==null || a2==null)
            return false;

        int length = a.length;
        if (a2.length != length)
            return false;

        return ArraysSupport.mismatch(a, a2, length) < 0;
    }

    /**
     * Returns true if the two specified arrays of booleans, over the specified
     * ranges, are <i>equal</i> to one another.
     *
     * <p>Two arrays are considered equal if the number of elements covered by
     * each range is the same, and all corresponding pairs of elements over the
     * specified ranges in the two arrays are equal.  In other words, two arrays
     * are equal if they contain, over the specified ranges, the same elements
     * in the same order.
     *
     * @param a the first array to be tested for equality
     * @param aFromIndex the index (inclusive) of the first element in the
     *                   first array to be tested
     * @param aToIndex the index (exclusive) of the last element in the
     *                 first array to be tested
     * @param b the second array to be tested for equality
     * @param bFromIndex the index (inclusive) of the first element in the
     *                   second array to be tested
     * @param bToIndex the index (exclusive) of the last element in the
     *                 second array to be tested
     * @return {@code true} if the two arrays, over the specified ranges, are
     *         equal
     * @throws IllegalArgumentException
     *         if {@code aFromIndex > aToIndex} or
     *         if {@code bFromIndex > bToIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code aFromIndex < 0 or aToIndex > a.length} or
     *         if {@code bFromIndex < 0 or bToIndex > b.length}
     * @throws NullPointerException
     *         if either array is {@code null}
     * @since 9
     */
    public static boolean equals(boolean[] a, int aFromIndex, int aToIndex,
                                 boolean[] b, int bFromIndex, int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        if (aLength != bLength)
            return false;

        return ArraysSupport.mismatch(a, aFromIndex,
                                      b, bFromIndex,
                                      aLength) < 0;
    }

    /**
     * Returns {@code true} if the two specified arrays of doubles are
     * <i>equal</i> to one another.  Two arrays are considered equal if both
     * arrays contain the same number of elements, and all corresponding pairs
     * of elements in the two arrays are equal.  In other words, two arrays
     * are equal if they contain the same elements in the same order.  Also,
     * two array references are considered equal if both are {@code null}.
     *
     * Two doubles {@code d1} and {@code d2} are considered equal if:
     * <pre>    {@code new Double(d1).equals(new Double(d2))}</pre>
     * (Unlike the {@code ==} operator, this method considers
     * {@code NaN} equal to itself, and 0.0d unequal to -0.0d.)
     *
     * @param a one array to be tested for equality
     * @param a2 the other array to be tested for equality
     * @return {@code true} if the two arrays are equal
     * @see Double#equals(Object)
     */
    public static boolean equals(double[] a, double[] a2) {
        if (a==a2)
            return true;
        if (a==null || a2==null)
            return false;

        int length = a.length;
        if (a2.length != length)
            return false;

        return ArraysSupport.mismatch(a, a2, length) < 0;
    }

    /**
     * Returns true if the two specified arrays of doubles, over the specified
     * ranges, are <i>equal</i> to one another.
     *
     * <p>Two arrays are considered equal if the number of elements covered by
     * each range is the same, and all corresponding pairs of elements over the
     * specified ranges in the two arrays are equal.  In other words, two arrays
     * are equal if they contain, over the specified ranges, the same elements
     * in the same order.
     *
     * <p>Two doubles {@code d1} and {@code d2} are considered equal if:
     * <pre>    {@code new Double(d1).equals(new Double(d2))}</pre>
     * (Unlike the {@code ==} operator, this method considers
     * {@code NaN} equal to itself, and 0.0d unequal to -0.0d.)
     *
     * @param a the first array to be tested for equality
     * @param aFromIndex the index (inclusive) of the first element in the
     *                   first array to be tested
     * @param aToIndex the index (exclusive) of the last element in the
     *                 first array to be tested
     * @param b the second array to be tested for equality
     * @param bFromIndex the index (inclusive) of the first element in the
     *                   second array to be tested
     * @param bToIndex the index (exclusive) of the last element in the
     *                 second array to be tested
     * @return {@code true} if the two arrays, over the specified ranges, are
     *         equal
     * @throws IllegalArgumentException
     *         if {@code aFromIndex > aToIndex} or
     *         if {@code bFromIndex > bToIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code aFromIndex < 0 or aToIndex > a.length} or
     *         if {@code bFromIndex < 0 or bToIndex > b.length}
     * @throws NullPointerException
     *         if either array is {@code null}
     * @see Double#equals(Object)
     * @since 9
     */
    public static boolean equals(double[] a, int aFromIndex, int aToIndex,
                                 double[] b, int bFromIndex, int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        if (aLength != bLength)
            return false;

        return ArraysSupport.mismatch(a, aFromIndex,
                                      b, bFromIndex, aLength) < 0;
    }

    /**
     * Returns {@code true} if the two specified arrays of floats are
     * <i>equal</i> to one another.  Two arrays are considered equal if both
     * arrays contain the same number of elements, and all corresponding pairs
     * of elements in the two arrays are equal.  In other words, two arrays
     * are equal if they contain the same elements in the same order.  Also,
     * two array references are considered equal if both are {@code null}.
     *
     * Two floats {@code f1} and {@code f2} are considered equal if:
     * <pre>    {@code new Float(f1).equals(new Float(f2))}</pre>
     * (Unlike the {@code ==} operator, this method considers
     * {@code NaN} equal to itself, and 0.0f unequal to -0.0f.)
     *
     * @param a one array to be tested for equality
     * @param a2 the other array to be tested for equality
     * @return {@code true} if the two arrays are equal
     * @see Float#equals(Object)
     */
    public static boolean equals(float[] a, float[] a2) {
        if (a==a2)
            return true;
        if (a==null || a2==null)
            return false;

        int length = a.length;
        if (a2.length != length)
            return false;

        return ArraysSupport.mismatch(a, a2, length) < 0;
    }

    /**
     * Returns true if the two specified arrays of floats, over the specified
     * ranges, are <i>equal</i> to one another.
     *
     * <p>Two arrays are considered equal if the number of elements covered by
     * each range is the same, and all corresponding pairs of elements over the
     * specified ranges in the two arrays are equal.  In other words, two arrays
     * are equal if they contain, over the specified ranges, the same elements
     * in the same order.
     *
     * <p>Two floats {@code f1} and {@code f2} are considered equal if:
     * <pre>    {@code new Float(f1).equals(new Float(f2))}</pre>
     * (Unlike the {@code ==} operator, this method considers
     * {@code NaN} equal to itself, and 0.0f unequal to -0.0f.)
     *
     * @param a the first array to be tested for equality
     * @param aFromIndex the index (inclusive) of the first element in the
     *                   first array to be tested
     * @param aToIndex the index (exclusive) of the last element in the
     *                 first array to be tested
     * @param b the second array to be tested for equality
     * @param bFromIndex the index (inclusive) of the first element in the
     *                   second array to be tested
     * @param bToIndex the index (exclusive) of the last element in the
     *                 second array to be tested
     * @return {@code true} if the two arrays, over the specified ranges, are
     *         equal
     * @throws IllegalArgumentException
     *         if {@code aFromIndex > aToIndex} or
     *         if {@code bFromIndex > bToIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code aFromIndex < 0 or aToIndex > a.length} or
     *         if {@code bFromIndex < 0 or bToIndex > b.length}
     * @throws NullPointerException
     *         if either array is {@code null}
     * @see Float#equals(Object)
     * @since 9
     */
    public static boolean equals(float[] a, int aFromIndex, int aToIndex,
                                 float[] b, int bFromIndex, int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        if (aLength != bLength)
            return false;

        return ArraysSupport.mismatch(a, aFromIndex,
                                      b, bFromIndex, aLength) < 0;
    }

    /**
     * Returns {@code true} if the two specified arrays of Objects are
     * <i>equal</i> to one another.  The two arrays are considered equal if
     * both arrays contain the same number of elements, and all corresponding
     * pairs of elements in the two arrays are equal.  Two objects {@code e1}
     * and {@code e2} are considered <i>equal</i> if
     * {@code Objects.equals(e1, e2)}.
     * In other words, the two arrays are equal if
     * they contain the same elements in the same order.  Also, two array
     * references are considered equal if both are {@code null}.
     *
     * @param a one array to be tested for equality
     * @param a2 the other array to be tested for equality
     * @return {@code true} if the two arrays are equal
     */
    public static boolean equals(Object[] a, Object[] a2) {
        if (a==a2)
            return true;
        if (a==null || a2==null)
            return false;

        int length = a.length;
        if (a2.length != length)
            return false;

        for (int i=0; i<length; i++) {
            if (!Objects.equals(a[i], a2[i]))
                return false;
        }

        return true;
    }

    /**
     * Returns true if the two specified arrays of Objects, over the specified
     * ranges, are <i>equal</i> to one another.
     *
     * <p>Two arrays are considered equal if the number of elements covered by
     * each range is the same, and all corresponding pairs of elements over the
     * specified ranges in the two arrays are equal.  In other words, two arrays
     * are equal if they contain, over the specified ranges, the same elements
     * in the same order.
     *
     * <p>Two objects {@code e1} and {@code e2} are considered <i>equal</i> if
     * {@code Objects.equals(e1, e2)}.
     *
     * @param a the first array to be tested for equality
     * @param aFromIndex the index (inclusive) of the first element in the
     *                   first array to be tested
     * @param aToIndex the index (exclusive) of the last element in the
     *                 first array to be tested
     * @param b the second array to be tested for equality
     * @param bFromIndex the index (inclusive) of the first element in the
     *                   second array to be tested
     * @param bToIndex the index (exclusive) of the last element in the
     *                 second array to be tested
     * @return {@code true} if the two arrays, over the specified ranges, are
     *         equal
     * @throws IllegalArgumentException
     *         if {@code aFromIndex > aToIndex} or
     *         if {@code bFromIndex > bToIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code aFromIndex < 0 or aToIndex > a.length} or
     *         if {@code bFromIndex < 0 or bToIndex > b.length}
     * @throws NullPointerException
     *         if either array is {@code null}
     * @since 9
     */
    public static boolean equals(Object[] a, int aFromIndex, int aToIndex,
                                 Object[] b, int bFromIndex, int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        if (aLength != bLength)
            return false;

        for (int i = 0; i < aLength; i++) {
            if (!Objects.equals(a[aFromIndex++], b[bFromIndex++]))
                return false;
        }

        return true;
    }

    /**
     * Returns {@code true} if the two specified arrays of Objects are
     * <i>equal</i> to one another.
     *
     * <p>Two arrays are considered equal if both arrays contain the same number
     * of elements, and all corresponding pairs of elements in the two arrays
     * are equal.  In other words, the two arrays are equal if they contain the
     * same elements in the same order.  Also, two array references are
     * considered equal if both are {@code null}.
     *
     * <p>Two objects {@code e1} and {@code e2} are considered <i>equal</i> if,
     * given the specified comparator, {@code cmp.compare(e1, e2) == 0}.
     *
     * @param a one array to be tested for equality
     * @param a2 the other array to be tested for equality
     * @param cmp the comparator to compare array elements
     * @param <T> the type of array elements
     * @return {@code true} if the two arrays are equal
     * @throws NullPointerException if the comparator is {@code null}
     * @since 9
     */
    public static <T> boolean equals(T[] a, T[] a2, Comparator<? super T> cmp) {
        Objects.requireNonNull(cmp);
        if (a==a2)
            return true;
        if (a==null || a2==null)
            return false;

        int length = a.length;
        if (a2.length != length)
            return false;

        for (int i=0; i<length; i++) {
            if (cmp.compare(a[i], a2[i]) != 0)
                return false;
        }

        return true;
    }

    /**
     * Returns true if the two specified arrays of Objects, over the specified
     * ranges, are <i>equal</i> to one another.
     *
     * <p>Two arrays are considered equal if the number of elements covered by
     * each range is the same, and all corresponding pairs of elements over the
     * specified ranges in the two arrays are equal.  In other words, two arrays
     * are equal if they contain, over the specified ranges, the same elements
     * in the same order.
     *
     * <p>Two objects {@code e1} and {@code e2} are considered <i>equal</i> if,
     * given the specified comparator, {@code cmp.compare(e1, e2) == 0}.
     *
     * @param a the first array to be tested for equality
     * @param aFromIndex the index (inclusive) of the first element in the
     *                   first array to be tested
     * @param aToIndex the index (exclusive) of the last element in the
     *                 first array to be tested
     * @param b the second array to be tested for equality
     * @param bFromIndex the index (inclusive) of the first element in the
     *                   second array to be tested
     * @param bToIndex the index (exclusive) of the last element in the
     *                 second array to be tested
     * @param cmp the comparator to compare array elements
     * @param <T> the type of array elements
     * @return {@code true} if the two arrays, over the specified ranges, are
     *         equal
     * @throws IllegalArgumentException
     *         if {@code aFromIndex > aToIndex} or
     *         if {@code bFromIndex > bToIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code aFromIndex < 0 or aToIndex > a.length} or
     *         if {@code bFromIndex < 0 or bToIndex > b.length}
     * @throws NullPointerException
     *         if either array or the comparator is {@code null}
     * @since 9
     */
    public static <T> boolean equals(T[] a, int aFromIndex, int aToIndex,
                                     T[] b, int bFromIndex, int bToIndex,
                                     Comparator<? super T> cmp) {
        Objects.requireNonNull(cmp);
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        if (aLength != bLength)
            return false;

        for (int i = 0; i < aLength; i++) {
            if (cmp.compare(a[aFromIndex++], b[bFromIndex++]) != 0)
                return false;
        }

        return true;
    }

    // Filling

    /**
     * Assigns the specified long value to each element of the specified array
     * of longs.
     *
     * @param a the array to be filled
     * @param val the value to be stored in all elements of the array
     */
    public static void fill(long[] a, long val) {
        for (int i = 0, len = a.length; i < len; i++)
            a[i] = val;
    }

    /**
     * Assigns the specified long value to each element of the specified
     * range of the specified array of longs.  The range to be filled
     * extends from index {@code fromIndex}, inclusive, to index
     * {@code toIndex}, exclusive.  (If {@code fromIndex==toIndex}, the
     * range to be filled is empty.)
     *
     * @param a the array to be filled
     * @param fromIndex the index of the first element (inclusive) to be
     *        filled with the specified value
     * @param toIndex the index of the last element (exclusive) to be
     *        filled with the specified value
     * @param val the value to be stored in all elements of the array
     * @throws IllegalArgumentException if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException if {@code fromIndex < 0} or
     *         {@code toIndex > a.length}
     */
    public static void fill(long[] a, int fromIndex, int toIndex, long val) {
        rangeCheck(a.length, fromIndex, toIndex);
        for (int i = fromIndex; i < toIndex; i++)
            a[i] = val;
    }

    /**
     * Assigns the specified int value to each element of the specified array
     * of ints.
     *
     * @param a the array to be filled
     * @param val the value to be stored in all elements of the array
     */
    public static void fill(int[] a, int val) {
        for (int i = 0, len = a.length; i < len; i++)
            a[i] = val;
    }

    /**
     * Assigns the specified int value to each element of the specified
     * range of the specified array of ints.  The range to be filled
     * extends from index {@code fromIndex}, inclusive, to index
     * {@code toIndex}, exclusive.  (If {@code fromIndex==toIndex}, the
     * range to be filled is empty.)
     *
     * @param a the array to be filled
     * @param fromIndex the index of the first element (inclusive) to be
     *        filled with the specified value
     * @param toIndex the index of the last element (exclusive) to be
     *        filled with the specified value
     * @param val the value to be stored in all elements of the array
     * @throws IllegalArgumentException if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException if {@code fromIndex < 0} or
     *         {@code toIndex > a.length}
     */
    public static void fill(int[] a, int fromIndex, int toIndex, int val) {
        rangeCheck(a.length, fromIndex, toIndex);
        for (int i = fromIndex; i < toIndex; i++)
            a[i] = val;
    }

    /**
     * Assigns the specified short value to each element of the specified array
     * of shorts.
     *
     * @param a the array to be filled
     * @param val the value to be stored in all elements of the array
     */
    public static void fill(short[] a, short val) {
        for (int i = 0, len = a.length; i < len; i++)
            a[i] = val;
    }

    /**
     * Assigns the specified short value to each element of the specified
     * range of the specified array of shorts.  The range to be filled
     * extends from index {@code fromIndex}, inclusive, to index
     * {@code toIndex}, exclusive.  (If {@code fromIndex==toIndex}, the
     * range to be filled is empty.)
     *
     * @param a the array to be filled
     * @param fromIndex the index of the first element (inclusive) to be
     *        filled with the specified value
     * @param toIndex the index of the last element (exclusive) to be
     *        filled with the specified value
     * @param val the value to be stored in all elements of the array
     * @throws IllegalArgumentException if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException if {@code fromIndex < 0} or
     *         {@code toIndex > a.length}
     */
    public static void fill(short[] a, int fromIndex, int toIndex, short val) {
        rangeCheck(a.length, fromIndex, toIndex);
        for (int i = fromIndex; i < toIndex; i++)
            a[i] = val;
    }

    /**
     * Assigns the specified char value to each element of the specified array
     * of chars.
     *
     * @param a the array to be filled
     * @param val the value to be stored in all elements of the array
     */
    public static void fill(char[] a, char val) {
        for (int i = 0, len = a.length; i < len; i++)
            a[i] = val;
    }

    /**
     * Assigns the specified char value to each element of the specified
     * range of the specified array of chars.  The range to be filled
     * extends from index {@code fromIndex}, inclusive, to index
     * {@code toIndex}, exclusive.  (If {@code fromIndex==toIndex}, the
     * range to be filled is empty.)
     *
     * @param a the array to be filled
     * @param fromIndex the index of the first element (inclusive) to be
     *        filled with the specified value
     * @param toIndex the index of the last element (exclusive) to be
     *        filled with the specified value
     * @param val the value to be stored in all elements of the array
     * @throws IllegalArgumentException if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException if {@code fromIndex < 0} or
     *         {@code toIndex > a.length}
     */
    public static void fill(char[] a, int fromIndex, int toIndex, char val) {
        rangeCheck(a.length, fromIndex, toIndex);
        for (int i = fromIndex; i < toIndex; i++)
            a[i] = val;
    }

    /**
     * Assigns the specified byte value to each element of the specified array
     * of bytes.
     *
     * @param a the array to be filled
     * @param val the value to be stored in all elements of the array
     */
    public static void fill(byte[] a, byte val) {
        for (int i = 0, len = a.length; i < len; i++)
            a[i] = val;
    }

    /**
     * Assigns the specified byte value to each element of the specified
     * range of the specified array of bytes.  The range to be filled
     * extends from index {@code fromIndex}, inclusive, to index
     * {@code toIndex}, exclusive.  (If {@code fromIndex==toIndex}, the
     * range to be filled is empty.)
     *
     * @param a the array to be filled
     * @param fromIndex the index of the first element (inclusive) to be
     *        filled with the specified value
     * @param toIndex the index of the last element (exclusive) to be
     *        filled with the specified value
     * @param val the value to be stored in all elements of the array
     * @throws IllegalArgumentException if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException if {@code fromIndex < 0} or
     *         {@code toIndex > a.length}
     */
    public static void fill(byte[] a, int fromIndex, int toIndex, byte val) {
        rangeCheck(a.length, fromIndex, toIndex);
        for (int i = fromIndex; i < toIndex; i++)
            a[i] = val;
    }

    /**
     * Assigns the specified boolean value to each element of the specified
     * array of booleans.
     *
     * @param a the array to be filled
     * @param val the value to be stored in all elements of the array
     */
    public static void fill(boolean[] a, boolean val) {
        for (int i = 0, len = a.length; i < len; i++)
            a[i] = val;
    }

    /**
     * Assigns the specified boolean value to each element of the specified
     * range of the specified array of booleans.  The range to be filled
     * extends from index {@code fromIndex}, inclusive, to index
     * {@code toIndex}, exclusive.  (If {@code fromIndex==toIndex}, the
     * range to be filled is empty.)
     *
     * @param a the array to be filled
     * @param fromIndex the index of the first element (inclusive) to be
     *        filled with the specified value
     * @param toIndex the index of the last element (exclusive) to be
     *        filled with the specified value
     * @param val the value to be stored in all elements of the array
     * @throws IllegalArgumentException if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException if {@code fromIndex < 0} or
     *         {@code toIndex > a.length}
     */
    public static void fill(boolean[] a, int fromIndex, int toIndex,
                            boolean val) {
        rangeCheck(a.length, fromIndex, toIndex);
        for (int i = fromIndex; i < toIndex; i++)
            a[i] = val;
    }

    /**
     * Assigns the specified double value to each element of the specified
     * array of doubles.
     *
     * @param a the array to be filled
     * @param val the value to be stored in all elements of the array
     */
    public static void fill(double[] a, double val) {
        for (int i = 0, len = a.length; i < len; i++)
            a[i] = val;
    }

    /**
     * Assigns the specified double value to each element of the specified
     * range of the specified array of doubles.  The range to be filled
     * extends from index {@code fromIndex}, inclusive, to index
     * {@code toIndex}, exclusive.  (If {@code fromIndex==toIndex}, the
     * range to be filled is empty.)
     *
     * @param a the array to be filled
     * @param fromIndex the index of the first element (inclusive) to be
     *        filled with the specified value
     * @param toIndex the index of the last element (exclusive) to be
     *        filled with the specified value
     * @param val the value to be stored in all elements of the array
     * @throws IllegalArgumentException if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException if {@code fromIndex < 0} or
     *         {@code toIndex > a.length}
     */
    public static void fill(double[] a, int fromIndex, int toIndex,double val){
        rangeCheck(a.length, fromIndex, toIndex);
        for (int i = fromIndex; i < toIndex; i++)
            a[i] = val;
    }

    /**
     * Assigns the specified float value to each element of the specified array
     * of floats.
     *
     * @param a the array to be filled
     * @param val the value to be stored in all elements of the array
     */
    public static void fill(float[] a, float val) {
        for (int i = 0, len = a.length; i < len; i++)
            a[i] = val;
    }

    /**
     * Assigns the specified float value to each element of the specified
     * range of the specified array of floats.  The range to be filled
     * extends from index {@code fromIndex}, inclusive, to index
     * {@code toIndex}, exclusive.  (If {@code fromIndex==toIndex}, the
     * range to be filled is empty.)
     *
     * @param a the array to be filled
     * @param fromIndex the index of the first element (inclusive) to be
     *        filled with the specified value
     * @param toIndex the index of the last element (exclusive) to be
     *        filled with the specified value
     * @param val the value to be stored in all elements of the array
     * @throws IllegalArgumentException if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException if {@code fromIndex < 0} or
     *         {@code toIndex > a.length}
     */
    public static void fill(float[] a, int fromIndex, int toIndex, float val) {
        rangeCheck(a.length, fromIndex, toIndex);
        for (int i = fromIndex; i < toIndex; i++)
            a[i] = val;
    }

    /**
     * Assigns the specified Object reference to each element of the specified
     * array of Objects.
     *
     * @param a the array to be filled
     * @param val the value to be stored in all elements of the array
     * @throws ArrayStoreException if the specified value is not of a
     *         runtime type that can be stored in the specified array
     */
    public static void fill(Object[] a, Object val) {
        for (int i = 0, len = a.length; i < len; i++)
            a[i] = val;
    }

    /**
     * Assigns the specified Object reference to each element of the specified
     * range of the specified array of Objects.  The range to be filled
     * extends from index {@code fromIndex}, inclusive, to index
     * {@code toIndex}, exclusive.  (If {@code fromIndex==toIndex}, the
     * range to be filled is empty.)
     *
     * @param a the array to be filled
     * @param fromIndex the index of the first element (inclusive) to be
     *        filled with the specified value
     * @param toIndex the index of the last element (exclusive) to be
     *        filled with the specified value
     * @param val the value to be stored in all elements of the array
     * @throws IllegalArgumentException if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException if {@code fromIndex < 0} or
     *         {@code toIndex > a.length}
     * @throws ArrayStoreException if the specified value is not of a
     *         runtime type that can be stored in the specified array
     */
    public static void fill(Object[] a, int fromIndex, int toIndex, Object val) {
        rangeCheck(a.length, fromIndex, toIndex);
        for (int i = fromIndex; i < toIndex; i++)
            a[i] = val;
    }

    // Cloning

    /**
     * Copies the specified array, truncating or padding with nulls (if necessary)
     * so the copy has the specified length.  For all indices that are
     * valid in both the original array and the copy, the two arrays will
     * contain identical values.  For any indices that are valid in the
     * copy but not the original, the copy will contain {@code null}.
     * Such indices will exist if and only if the specified length
     * is greater than that of the original array.
     * The resulting array is of exactly the same class as the original array.
     *
     * @param <T> the class of the objects in the array
     * @param original the array to be copied
     * @param newLength the length of the copy to be returned
     * @return a copy of the original array, truncated or padded with nulls
     *     to obtain the specified length
     * @throws NegativeArraySizeException if {@code newLength} is negative
     * @throws NullPointerException if {@code original} is null
     * @since 1.6
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] copyOf(T[] original, int newLength) {
        return (T[]) copyOf(original, newLength, original.getClass());
    }

    /**
     * Copies the specified array, truncating or padding with nulls (if necessary)
     * so the copy has the specified length.  For all indices that are
     * valid in both the original array and the copy, the two arrays will
     * contain identical values.  For any indices that are valid in the
     * copy but not the original, the copy will contain {@code null}.
     * Such indices will exist if and only if the specified length
     * is greater than that of the original array.
     * The resulting array is of the class {@code newType}.
     *
     * @param <U> the class of the objects in the original array
     * @param <T> the class of the objects in the returned array
     * @param original the array to be copied
     * @param newLength the length of the copy to be returned
     * @param newType the class of the copy to be returned
     * @return a copy of the original array, truncated or padded with nulls
     *     to obtain the specified length
     * @throws NegativeArraySizeException if {@code newLength} is negative
     * @throws NullPointerException if {@code original} is null
     * @throws ArrayStoreException if an element copied from
     *     {@code original} is not of a runtime type that can be stored in
     *     an array of class {@code newType}
     * @since 1.6
     */
    @IntrinsicCandidate
    public static <T,U> T[] copyOf(U[] original, int newLength, Class<? extends T[]> newType) {
        @SuppressWarnings("unchecked")
        T[] copy = ((Object)newType == (Object)Object[].class)
            ? (T[]) new Object[newLength]
            : (T[]) Array.newInstance(newType.getComponentType(), newLength);
        System.arraycopy(original, 0, copy, 0,
                         Math.min(original.length, newLength));
        return copy;
    }

    /**
     * Copies the specified array, truncating or padding with zeros (if necessary)
     * so the copy has the specified length.  For all indices that are
     * valid in both the original array and the copy, the two arrays will
     * contain identical values.  For any indices that are valid in the
     * copy but not the original, the copy will contain {@code (byte)0}.
     * Such indices will exist if and only if the specified length
     * is greater than that of the original array.
     *
     * @param original the array to be copied
     * @param newLength the length of the copy to be returned
     * @return a copy of the original array, truncated or padded with zeros
     *     to obtain the specified length
     * @throws NegativeArraySizeException if {@code newLength} is negative
     * @throws NullPointerException if {@code original} is null
     * @since 1.6
     */
    public static byte[] copyOf(byte[] original, int newLength) {
        byte[] copy = new byte[newLength];
        System.arraycopy(original, 0, copy, 0,
                         Math.min(original.length, newLength));
        return copy;
    }

    /**
     * Copies the specified array, truncating or padding with zeros (if necessary)
     * so the copy has the specified length.  For all indices that are
     * valid in both the original array and the copy, the two arrays will
     * contain identical values.  For any indices that are valid in the
     * copy but not the original, the copy will contain {@code (short)0}.
     * Such indices will exist if and only if the specified length
     * is greater than that of the original array.
     *
     * @param original the array to be copied
     * @param newLength the length of the copy to be returned
     * @return a copy of the original array, truncated or padded with zeros
     *     to obtain the specified length
     * @throws NegativeArraySizeException if {@code newLength} is negative
     * @throws NullPointerException if {@code original} is null
     * @since 1.6
     */
    public static short[] copyOf(short[] original, int newLength) {
        short[] copy = new short[newLength];
        System.arraycopy(original, 0, copy, 0,
                         Math.min(original.length, newLength));
        return copy;
    }

    /**
     * Copies the specified array, truncating or padding with zeros (if necessary)
     * so the copy has the specified length.  For all indices that are
     * valid in both the original array and the copy, the two arrays will
     * contain identical values.  For any indices that are valid in the
     * copy but not the original, the copy will contain {@code 0}.
     * Such indices will exist if and only if the specified length
     * is greater than that of the original array.
     *
     * @param original the array to be copied
     * @param newLength the length of the copy to be returned
     * @return a copy of the original array, truncated or padded with zeros
     *     to obtain the specified length
     * @throws NegativeArraySizeException if {@code newLength} is negative
     * @throws NullPointerException if {@code original} is null
     * @since 1.6
     */
    public static int[] copyOf(int[] original, int newLength) {
        int[] copy = new int[newLength];
        System.arraycopy(original, 0, copy, 0,
                         Math.min(original.length, newLength));
        return copy;
    }

    /**
     * Copies the specified array, truncating or padding with zeros (if necessary)
     * so the copy has the specified length.  For all indices that are
     * valid in both the original array and the copy, the two arrays will
     * contain identical values.  For any indices that are valid in the
     * copy but not the original, the copy will contain {@code 0L}.
     * Such indices will exist if and only if the specified length
     * is greater than that of the original array.
     *
     * @param original the array to be copied
     * @param newLength the length of the copy to be returned
     * @return a copy of the original array, truncated or padded with zeros
     *     to obtain the specified length
     * @throws NegativeArraySizeException if {@code newLength} is negative
     * @throws NullPointerException if {@code original} is null
     * @since 1.6
     */
    public static long[] copyOf(long[] original, int newLength) {
        long[] copy = new long[newLength];
        System.arraycopy(original, 0, copy, 0,
                         Math.min(original.length, newLength));
        return copy;
    }

    /**
     * Copies the specified array, truncating or padding with null characters (if necessary)
     * so the copy has the specified length.  For all indices that are valid
     * in both the original array and the copy, the two arrays will contain
     * identical values.  For any indices that are valid in the copy but not
     * the original, the copy will contain {@code '\u005cu0000'}.  Such indices
     * will exist if and only if the specified length is greater than that of
     * the original array.
     *
     * @param original the array to be copied
     * @param newLength the length of the copy to be returned
     * @return a copy of the original array, truncated or padded with null characters
     *     to obtain the specified length
     * @throws NegativeArraySizeException if {@code newLength} is negative
     * @throws NullPointerException if {@code original} is null
     * @since 1.6
     */
    public static char[] copyOf(char[] original, int newLength) {
        char[] copy = new char[newLength];
        System.arraycopy(original, 0, copy, 0,
                         Math.min(original.length, newLength));
        return copy;
    }

    /**
     * Copies the specified array, truncating or padding with zeros (if necessary)
     * so the copy has the specified length.  For all indices that are
     * valid in both the original array and the copy, the two arrays will
     * contain identical values.  For any indices that are valid in the
     * copy but not the original, the copy will contain {@code 0f}.
     * Such indices will exist if and only if the specified length
     * is greater than that of the original array.
     *
     * @param original the array to be copied
     * @param newLength the length of the copy to be returned
     * @return a copy of the original array, truncated or padded with zeros
     *     to obtain the specified length
     * @throws NegativeArraySizeException if {@code newLength} is negative
     * @throws NullPointerException if {@code original} is null
     * @since 1.6
     */
    public static float[] copyOf(float[] original, int newLength) {
        float[] copy = new float[newLength];
        System.arraycopy(original, 0, copy, 0,
                         Math.min(original.length, newLength));
        return copy;
    }

    /**
     * Copies the specified array, truncating or padding with zeros (if necessary)
     * so the copy has the specified length.  For all indices that are
     * valid in both the original array and the copy, the two arrays will
     * contain identical values.  For any indices that are valid in the
     * copy but not the original, the copy will contain {@code 0d}.
     * Such indices will exist if and only if the specified length
     * is greater than that of the original array.
     *
     * @param original the array to be copied
     * @param newLength the length of the copy to be returned
     * @return a copy of the original array, truncated or padded with zeros
     *     to obtain the specified length
     * @throws NegativeArraySizeException if {@code newLength} is negative
     * @throws NullPointerException if {@code original} is null
     * @since 1.6
     */
    public static double[] copyOf(double[] original, int newLength) {
        double[] copy = new double[newLength];
        System.arraycopy(original, 0, copy, 0,
                         Math.min(original.length, newLength));
        return copy;
    }

    /**
     * Copies the specified array, truncating or padding with {@code false} (if necessary)
     * so the copy has the specified length.  For all indices that are
     * valid in both the original array and the copy, the two arrays will
     * contain identical values.  For any indices that are valid in the
     * copy but not the original, the copy will contain {@code false}.
     * Such indices will exist if and only if the specified length
     * is greater than that of the original array.
     *
     * @param original the array to be copied
     * @param newLength the length of the copy to be returned
     * @return a copy of the original array, truncated or padded with false elements
     *     to obtain the specified length
     * @throws NegativeArraySizeException if {@code newLength} is negative
     * @throws NullPointerException if {@code original} is null
     * @since 1.6
     */
    public static boolean[] copyOf(boolean[] original, int newLength) {
        boolean[] copy = new boolean[newLength];
        System.arraycopy(original, 0, copy, 0,
                         Math.min(original.length, newLength));
        return copy;
    }

    /**
     * Copies the specified range of the specified array into a new array.
     * The initial index of the range ({@code from}) must lie between zero
     * and {@code original.length}, inclusive.  The value at
     * {@code original[from]} is placed into the initial element of the copy
     * (unless {@code from == original.length} or {@code from == to}).
     * Values from subsequent elements in the original array are placed into
     * subsequent elements in the copy.  The final index of the range
     * ({@code to}), which must be greater than or equal to {@code from},
     * may be greater than {@code original.length}, in which case
     * {@code null} is placed in all elements of the copy whose index is
     * greater than or equal to {@code original.length - from}.  The length
     * of the returned array will be {@code to - from}.
     * <p>
     * The resulting array is of exactly the same class as the original array.
     *
     * @param <T> the class of the objects in the array
     * @param original the array from which a range is to be copied
     * @param from the initial index of the range to be copied, inclusive
     * @param to the final index of the range to be copied, exclusive.
     *     (This index may lie outside the array.)
     * @return a new array containing the specified range from the original array,
     *     truncated or padded with nulls to obtain the required length
     * @throws ArrayIndexOutOfBoundsException if {@code from < 0}
     *     or {@code from > original.length}
     * @throws IllegalArgumentException if {@code from > to}
     * @throws NullPointerException if {@code original} is null
     * @since 1.6
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] copyOfRange(T[] original, int from, int to) {
        return copyOfRange(original, from, to, (Class<? extends T[]>) original.getClass());
    }

    /**
     * Copies the specified range of the specified array into a new array.
     * The initial index of the range ({@code from}) must lie between zero
     * and {@code original.length}, inclusive.  The value at
     * {@code original[from]} is placed into the initial element of the copy
     * (unless {@code from == original.length} or {@code from == to}).
     * Values from subsequent elements in the original array are placed into
     * subsequent elements in the copy.  The final index of the range
     * ({@code to}), which must be greater than or equal to {@code from},
     * may be greater than {@code original.length}, in which case
     * {@code null} is placed in all elements of the copy whose index is
     * greater than or equal to {@code original.length - from}.  The length
     * of the returned array will be {@code to - from}.
     * The resulting array is of the class {@code newType}.
     *
     * @param <U> the class of the objects in the original array
     * @param <T> the class of the objects in the returned array
     * @param original the array from which a range is to be copied
     * @param from the initial index of the range to be copied, inclusive
     * @param to the final index of the range to be copied, exclusive.
     *     (This index may lie outside the array.)
     * @param newType the class of the copy to be returned
     * @return a new array containing the specified range from the original array,
     *     truncated or padded with nulls to obtain the required length
     * @throws ArrayIndexOutOfBoundsException if {@code from < 0}
     *     or {@code from > original.length}
     * @throws IllegalArgumentException if {@code from > to}
     * @throws NullPointerException if {@code original} is null
     * @throws ArrayStoreException if an element copied from
     *     {@code original} is not of a runtime type that can be stored in
     *     an array of class {@code newType}.
     * @since 1.6
     */
    @IntrinsicCandidate
    public static <T,U> T[] copyOfRange(U[] original, int from, int to, Class<? extends T[]> newType) {
        int newLength = to - from;
        if (newLength < 0)
            throw new IllegalArgumentException(from + " > " + to);
        @SuppressWarnings("unchecked")
        T[] copy = ((Object)newType == (Object)Object[].class)
            ? (T[]) new Object[newLength]
            : (T[]) Array.newInstance(newType.getComponentType(), newLength);
        System.arraycopy(original, from, copy, 0,
                         Math.min(original.length - from, newLength));
        return copy;
    }

    /**
     * Copies the specified range of the specified array into a new array.
     * The initial index of the range ({@code from}) must lie between zero
     * and {@code original.length}, inclusive.  The value at
     * {@code original[from]} is placed into the initial element of the copy
     * (unless {@code from == original.length} or {@code from == to}).
     * Values from subsequent elements in the original array are placed into
     * subsequent elements in the copy.  The final index of the range
     * ({@code to}), which must be greater than or equal to {@code from},
     * may be greater than {@code original.length}, in which case
     * {@code (byte)0} is placed in all elements of the copy whose index is
     * greater than or equal to {@code original.length - from}.  The length
     * of the returned array will be {@code to - from}.
     *
     * @param original the array from which a range is to be copied
     * @param from the initial index of the range to be copied, inclusive
     * @param to the final index of the range to be copied, exclusive.
     *     (This index may lie outside the array.)
     * @return a new array containing the specified range from the original array,
     *     truncated or padded with zeros to obtain the required length
     * @throws ArrayIndexOutOfBoundsException if {@code from < 0}
     *     or {@code from > original.length}
     * @throws IllegalArgumentException if {@code from > to}
     * @throws NullPointerException if {@code original} is null
     * @since 1.6
     */
    public static byte[] copyOfRange(byte[] original, int from, int to) {
        int newLength = to - from;
        if (newLength < 0)
            throw new IllegalArgumentException(from + " > " + to);
        byte[] copy = new byte[newLength];
        System.arraycopy(original, from, copy, 0,
                         Math.min(original.length - from, newLength));
        return copy;
    }

    /**
     * Copies the specified range of the specified array into a new array.
     * The initial index of the range ({@code from}) must lie between zero
     * and {@code original.length}, inclusive.  The value at
     * {@code original[from]} is placed into the initial element of the copy
     * (unless {@code from == original.length} or {@code from == to}).
     * Values from subsequent elements in the original array are placed into
     * subsequent elements in the copy.  The final index of the range
     * ({@code to}), which must be greater than or equal to {@code from},
     * may be greater than {@code original.length}, in which case
     * {@code (short)0} is placed in all elements of the copy whose index is
     * greater than or equal to {@code original.length - from}.  The length
     * of the returned array will be {@code to - from}.
     *
     * @param original the array from which a range is to be copied
     * @param from the initial index of the range to be copied, inclusive
     * @param to the final index of the range to be copied, exclusive.
     *     (This index may lie outside the array.)
     * @return a new array containing the specified range from the original array,
     *     truncated or padded with zeros to obtain the required length
     * @throws ArrayIndexOutOfBoundsException if {@code from < 0}
     *     or {@code from > original.length}
     * @throws IllegalArgumentException if {@code from > to}
     * @throws NullPointerException if {@code original} is null
     * @since 1.6
     */
    public static short[] copyOfRange(short[] original, int from, int to) {
        int newLength = to - from;
        if (newLength < 0)
            throw new IllegalArgumentException(from + " > " + to);
        short[] copy = new short[newLength];
        System.arraycopy(original, from, copy, 0,
                         Math.min(original.length - from, newLength));
        return copy;
    }

    /**
     * Copies the specified range of the specified array into a new array.
     * The initial index of the range ({@code from}) must lie between zero
     * and {@code original.length}, inclusive.  The value at
     * {@code original[from]} is placed into the initial element of the copy
     * (unless {@code from == original.length} or {@code from == to}).
     * Values from subsequent elements in the original array are placed into
     * subsequent elements in the copy.  The final index of the range
     * ({@code to}), which must be greater than or equal to {@code from},
     * may be greater than {@code original.length}, in which case
     * {@code 0} is placed in all elements of the copy whose index is
     * greater than or equal to {@code original.length - from}.  The length
     * of the returned array will be {@code to - from}.
     *
     * @param original the array from which a range is to be copied
     * @param from the initial index of the range to be copied, inclusive
     * @param to the final index of the range to be copied, exclusive.
     *     (This index may lie outside the array.)
     * @return a new array containing the specified range from the original array,
     *     truncated or padded with zeros to obtain the required length
     * @throws ArrayIndexOutOfBoundsException if {@code from < 0}
     *     or {@code from > original.length}
     * @throws IllegalArgumentException if {@code from > to}
     * @throws NullPointerException if {@code original} is null
     * @since 1.6
     */
    public static int[] copyOfRange(int[] original, int from, int to) {
        int newLength = to - from;
        if (newLength < 0)
            throw new IllegalArgumentException(from + " > " + to);
        int[] copy = new int[newLength];
        System.arraycopy(original, from, copy, 0,
                         Math.min(original.length - from, newLength));
        return copy;
    }

    /**
     * Copies the specified range of the specified array into a new array.
     * The initial index of the range ({@code from}) must lie between zero
     * and {@code original.length}, inclusive.  The value at
     * {@code original[from]} is placed into the initial element of the copy
     * (unless {@code from == original.length} or {@code from == to}).
     * Values from subsequent elements in the original array are placed into
     * subsequent elements in the copy.  The final index of the range
     * ({@code to}), which must be greater than or equal to {@code from},
     * may be greater than {@code original.length}, in which case
     * {@code 0L} is placed in all elements of the copy whose index is
     * greater than or equal to {@code original.length - from}.  The length
     * of the returned array will be {@code to - from}.
     *
     * @param original the array from which a range is to be copied
     * @param from the initial index of the range to be copied, inclusive
     * @param to the final index of the range to be copied, exclusive.
     *     (This index may lie outside the array.)
     * @return a new array containing the specified range from the original array,
     *     truncated or padded with zeros to obtain the required length
     * @throws ArrayIndexOutOfBoundsException if {@code from < 0}
     *     or {@code from > original.length}
     * @throws IllegalArgumentException if {@code from > to}
     * @throws NullPointerException if {@code original} is null
     * @since 1.6
     */
    public static long[] copyOfRange(long[] original, int from, int to) {
        int newLength = to - from;
        if (newLength < 0)
            throw new IllegalArgumentException(from + " > " + to);
        long[] copy = new long[newLength];
        System.arraycopy(original, from, copy, 0,
                         Math.min(original.length - from, newLength));
        return copy;
    }

    /**
     * Copies the specified range of the specified array into a new array.
     * The initial index of the range ({@code from}) must lie between zero
     * and {@code original.length}, inclusive.  The value at
     * {@code original[from]} is placed into the initial element of the copy
     * (unless {@code from == original.length} or {@code from == to}).
     * Values from subsequent elements in the original array are placed into
     * subsequent elements in the copy.  The final index of the range
     * ({@code to}), which must be greater than or equal to {@code from},
     * may be greater than {@code original.length}, in which case
     * {@code '\u005cu0000'} is placed in all elements of the copy whose index is
     * greater than or equal to {@code original.length - from}.  The length
     * of the returned array will be {@code to - from}.
     *
     * @param original the array from which a range is to be copied
     * @param from the initial index of the range to be copied, inclusive
     * @param to the final index of the range to be copied, exclusive.
     *     (This index may lie outside the array.)
     * @return a new array containing the specified range from the original array,
     *     truncated or padded with null characters to obtain the required length
     * @throws ArrayIndexOutOfBoundsException if {@code from < 0}
     *     or {@code from > original.length}
     * @throws IllegalArgumentException if {@code from > to}
     * @throws NullPointerException if {@code original} is null
     * @since 1.6
     */
    public static char[] copyOfRange(char[] original, int from, int to) {
        int newLength = to - from;
        if (newLength < 0)
            throw new IllegalArgumentException(from + " > " + to);
        char[] copy = new char[newLength];
        System.arraycopy(original, from, copy, 0,
                         Math.min(original.length - from, newLength));
        return copy;
    }

    /**
     * Copies the specified range of the specified array into a new array.
     * The initial index of the range ({@code from}) must lie between zero
     * and {@code original.length}, inclusive.  The value at
     * {@code original[from]} is placed into the initial element of the copy
     * (unless {@code from == original.length} or {@code from == to}).
     * Values from subsequent elements in the original array are placed into
     * subsequent elements in the copy.  The final index of the range
     * ({@code to}), which must be greater than or equal to {@code from},
     * may be greater than {@code original.length}, in which case
     * {@code 0f} is placed in all elements of the copy whose index is
     * greater than or equal to {@code original.length - from}.  The length
     * of the returned array will be {@code to - from}.
     *
     * @param original the array from which a range is to be copied
     * @param from the initial index of the range to be copied, inclusive
     * @param to the final index of the range to be copied, exclusive.
     *     (This index may lie outside the array.)
     * @return a new array containing the specified range from the original array,
     *     truncated or padded with zeros to obtain the required length
     * @throws ArrayIndexOutOfBoundsException if {@code from < 0}
     *     or {@code from > original.length}
     * @throws IllegalArgumentException if {@code from > to}
     * @throws NullPointerException if {@code original} is null
     * @since 1.6
     */
    public static float[] copyOfRange(float[] original, int from, int to) {
        int newLength = to - from;
        if (newLength < 0)
            throw new IllegalArgumentException(from + " > " + to);
        float[] copy = new float[newLength];
        System.arraycopy(original, from, copy, 0,
                         Math.min(original.length - from, newLength));
        return copy;
    }

    /**
     * Copies the specified range of the specified array into a new array.
     * The initial index of the range ({@code from}) must lie between zero
     * and {@code original.length}, inclusive.  The value at
     * {@code original[from]} is placed into the initial element of the copy
     * (unless {@code from == original.length} or {@code from == to}).
     * Values from subsequent elements in the original array are placed into
     * subsequent elements in the copy.  The final index of the range
     * ({@code to}), which must be greater than or equal to {@code from},
     * may be greater than {@code original.length}, in which case
     * {@code 0d} is placed in all elements of the copy whose index is
     * greater than or equal to {@code original.length - from}.  The length
     * of the returned array will be {@code to - from}.
     *
     * @param original the array from which a range is to be copied
     * @param from the initial index of the range to be copied, inclusive
     * @param to the final index of the range to be copied, exclusive.
     *     (This index may lie outside the array.)
     * @return a new array containing the specified range from the original array,
     *     truncated or padded with zeros to obtain the required length
     * @throws ArrayIndexOutOfBoundsException if {@code from < 0}
     *     or {@code from > original.length}
     * @throws IllegalArgumentException if {@code from > to}
     * @throws NullPointerException if {@code original} is null
     * @since 1.6
     */
    public static double[] copyOfRange(double[] original, int from, int to) {
        int newLength = to - from;
        if (newLength < 0)
            throw new IllegalArgumentException(from + " > " + to);
        double[] copy = new double[newLength];
        System.arraycopy(original, from, copy, 0,
                         Math.min(original.length - from, newLength));
        return copy;
    }

    /**
     * Copies the specified range of the specified array into a new array.
     * The initial index of the range ({@code from}) must lie between zero
     * and {@code original.length}, inclusive.  The value at
     * {@code original[from]} is placed into the initial element of the copy
     * (unless {@code from == original.length} or {@code from == to}).
     * Values from subsequent elements in the original array are placed into
     * subsequent elements in the copy.  The final index of the range
     * ({@code to}), which must be greater than or equal to {@code from},
     * may be greater than {@code original.length}, in which case
     * {@code false} is placed in all elements of the copy whose index is
     * greater than or equal to {@code original.length - from}.  The length
     * of the returned array will be {@code to - from}.
     *
     * @param original the array from which a range is to be copied
     * @param from the initial index of the range to be copied, inclusive
     * @param to the final index of the range to be copied, exclusive.
     *     (This index may lie outside the array.)
     * @return a new array containing the specified range from the original array,
     *     truncated or padded with false elements to obtain the required length
     * @throws ArrayIndexOutOfBoundsException if {@code from < 0}
     *     or {@code from > original.length}
     * @throws IllegalArgumentException if {@code from > to}
     * @throws NullPointerException if {@code original} is null
     * @since 1.6
     */
    public static boolean[] copyOfRange(boolean[] original, int from, int to) {
        int newLength = to - from;
        if (newLength < 0)
            throw new IllegalArgumentException(from + " > " + to);
        boolean[] copy = new boolean[newLength];
        System.arraycopy(original, from, copy, 0,
                         Math.min(original.length - from, newLength));
        return copy;
    }

    // Misc

    /**
     * Returns a fixed-size list backed by the specified array. Changes made to
     * the array will be visible in the returned list, and changes made to the
     * list will be visible in the array. The returned list is
     * {@link Serializable} and implements {@link RandomAccess}.
     *
     * <p>The returned list implements the optional {@code Collection} methods, except
     * those that would change the size of the returned list. Those methods leave
     * the list unchanged and throw {@link UnsupportedOperationException}.
     *
     * @apiNote
     * This method acts as bridge between array-based and collection-based
     * APIs, in combination with {@link Collection#toArray}.
     *
     * <p>This method provides a way to wrap an existing array:
     * <pre>{@code
     *     Integer[] numbers = ...
     *     ...
     *     List<Integer> values = Arrays.asList(numbers);
     * }</pre>
     *
     * <p>This method also provides a convenient way to create a fixed-size
     * list initialized to contain several elements:
     * <pre>{@code
     *     List<String> stooges = Arrays.asList("Larry", "Moe", "Curly");
     * }</pre>
     *
     * <p><em>The list returned by this method is modifiable.</em>
     * To create an unmodifiable list, use
     * {@link Collections#unmodifiableList Collections.unmodifiableList}
     * or <a href="List.html#unmodifiable">Unmodifiable Lists</a>.
     *
     * @param <T> the class of the objects in the array
     * @param a the array by which the list will be backed
     * @return a list view of the specified array
     * @throws NullPointerException if the specified array is {@code null}
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <T> List<T> asList(T... a) {
        return new ArrayList<>(a);
    }

    /**
     * @serial include
     */
    private static class ArrayList<E> extends AbstractList<E>
        implements RandomAccess, java.io.Serializable
    {
        @java.io.Serial
        private static final long serialVersionUID = -2764017481108945198L;
        @SuppressWarnings("serial") // Conditionally serializable
        private final E[] a;

        ArrayList(E[] array) {
            a = Objects.requireNonNull(array);
        }

        @Override
        public int size() {
            return a.length;
        }

        @Override
        public Object[] toArray() {
            return Arrays.copyOf(a, a.length, Object[].class);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T[] toArray(T[] a) {
            int size = size();
            if (a.length < size)
                return Arrays.copyOf(this.a, size,
                                     (Class<? extends T[]>) a.getClass());
            System.arraycopy(this.a, 0, a, 0, size);
            if (a.length > size)
                a[size] = null;
            return a;
        }

        @Override
        public E get(int index) {
            return a[index];
        }

        @Override
        public E set(int index, E element) {
            E oldValue = a[index];
            a[index] = element;
            return oldValue;
        }

        @Override
        public int indexOf(Object o) {
            E[] a = this.a;
            if (o == null) {
                for (int i = 0; i < a.length; i++)
                    if (a[i] == null)
                        return i;
            } else {
                for (int i = 0; i < a.length; i++)
                    if (o.equals(a[i]))
                        return i;
            }
            return -1;
        }

        @Override
        public boolean contains(Object o) {
            return indexOf(o) >= 0;
        }

        @Override
        public Spliterator<E> spliterator() {
            return Spliterators.spliterator(a, Spliterator.ORDERED);
        }

        @Override
        public void forEach(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            for (E e : a) {
                action.accept(e);
            }
        }

        @Override
        public void replaceAll(UnaryOperator<E> operator) {
            Objects.requireNonNull(operator);
            E[] a = this.a;
            for (int i = 0; i < a.length; i++) {
                a[i] = operator.apply(a[i]);
            }
        }

        @Override
        public void sort(Comparator<? super E> c) {
            Arrays.sort(a, c);
        }

        @Override
        public Iterator<E> iterator() {
            return new ArrayItr<>(a);
        }
    }

    private static class ArrayItr<E> implements Iterator<E> {
        private int cursor;
        private final E[] a;

        ArrayItr(E[] a) {
            this.a = a;
        }

        @Override
        public boolean hasNext() {
            return cursor < a.length;
        }

        @Override
        public E next() {
            int i = cursor;
            if (i >= a.length) {
                throw new NoSuchElementException();
            }
            cursor = i + 1;
            return a[i];
        }
    }

    /**
     * Returns a hash code based on the contents of the specified array.
     * For any two {@code long} arrays {@code a} and {@code b}
     * such that {@code Arrays.equals(a, b)}, it is also the case that
     * {@code Arrays.hashCode(a) == Arrays.hashCode(b)}.
     *
     * <p>The value returned by this method is the same value that would be
     * obtained by invoking the {@link List#hashCode() hashCode}
     * method on a {@link List} containing a sequence of {@link Long}
     * instances representing the elements of {@code a} in the same order.
     * If {@code a} is {@code null}, this method returns 0.
     *
     * @param a the array whose hash value to compute
     * @return a content-based hash code for {@code a}
     * @since 1.5
     */
    public static int hashCode(long a[]) {
        if (a == null)
            return 0;

        int result = 1;
        for (long element : a) {
            int elementHash = (int)(element ^ (element >>> 32));
            result = 31 * result + elementHash;
        }

        return result;
    }

    /**
     * Returns a hash code based on the contents of the specified array.
     * For any two non-null {@code int} arrays {@code a} and {@code b}
     * such that {@code Arrays.equals(a, b)}, it is also the case that
     * {@code Arrays.hashCode(a) == Arrays.hashCode(b)}.
     *
     * <p>The value returned by this method is the same value that would be
     * obtained by invoking the {@link List#hashCode() hashCode}
     * method on a {@link List} containing a sequence of {@link Integer}
     * instances representing the elements of {@code a} in the same order.
     * If {@code a} is {@code null}, this method returns 0.
     *
     * @param a the array whose hash value to compute
     * @return a content-based hash code for {@code a}
     * @since 1.5
     */
    public static int hashCode(int a[]) {
        if (a == null)
            return 0;

        int result = 1;
        for (int element : a)
            result = 31 * result + element;

        return result;
    }

    /**
     * Returns a hash code based on the contents of the specified array.
     * For any two {@code short} arrays {@code a} and {@code b}
     * such that {@code Arrays.equals(a, b)}, it is also the case that
     * {@code Arrays.hashCode(a) == Arrays.hashCode(b)}.
     *
     * <p>The value returned by this method is the same value that would be
     * obtained by invoking the {@link List#hashCode() hashCode}
     * method on a {@link List} containing a sequence of {@link Short}
     * instances representing the elements of {@code a} in the same order.
     * If {@code a} is {@code null}, this method returns 0.
     *
     * @param a the array whose hash value to compute
     * @return a content-based hash code for {@code a}
     * @since 1.5
     */
    public static int hashCode(short a[]) {
        if (a == null)
            return 0;

        int result = 1;
        for (short element : a)
            result = 31 * result + element;

        return result;
    }

    /**
     * Returns a hash code based on the contents of the specified array.
     * For any two {@code char} arrays {@code a} and {@code b}
     * such that {@code Arrays.equals(a, b)}, it is also the case that
     * {@code Arrays.hashCode(a) == Arrays.hashCode(b)}.
     *
     * <p>The value returned by this method is the same value that would be
     * obtained by invoking the {@link List#hashCode() hashCode}
     * method on a {@link List} containing a sequence of {@link Character}
     * instances representing the elements of {@code a} in the same order.
     * If {@code a} is {@code null}, this method returns 0.
     *
     * @param a the array whose hash value to compute
     * @return a content-based hash code for {@code a}
     * @since 1.5
     */
    public static int hashCode(char a[]) {
        if (a == null)
            return 0;

        int result = 1;
        for (char element : a)
            result = 31 * result + element;

        return result;
    }

    /**
     * Returns a hash code based on the contents of the specified array.
     * For any two {@code byte} arrays {@code a} and {@code b}
     * such that {@code Arrays.equals(a, b)}, it is also the case that
     * {@code Arrays.hashCode(a) == Arrays.hashCode(b)}.
     *
     * <p>The value returned by this method is the same value that would be
     * obtained by invoking the {@link List#hashCode() hashCode}
     * method on a {@link List} containing a sequence of {@link Byte}
     * instances representing the elements of {@code a} in the same order.
     * If {@code a} is {@code null}, this method returns 0.
     *
     * @param a the array whose hash value to compute
     * @return a content-based hash code for {@code a}
     * @since 1.5
     */
    public static int hashCode(byte a[]) {
        if (a == null)
            return 0;

        int result = 1;
        for (byte element : a)
            result = 31 * result + element;

        return result;
    }

    /**
     * Returns a hash code based on the contents of the specified array.
     * For any two {@code boolean} arrays {@code a} and {@code b}
     * such that {@code Arrays.equals(a, b)}, it is also the case that
     * {@code Arrays.hashCode(a) == Arrays.hashCode(b)}.
     *
     * <p>The value returned by this method is the same value that would be
     * obtained by invoking the {@link List#hashCode() hashCode}
     * method on a {@link List} containing a sequence of {@link Boolean}
     * instances representing the elements of {@code a} in the same order.
     * If {@code a} is {@code null}, this method returns 0.
     *
     * @param a the array whose hash value to compute
     * @return a content-based hash code for {@code a}
     * @since 1.5
     */
    public static int hashCode(boolean a[]) {
        if (a == null)
            return 0;

        int result = 1;
        for (boolean element : a)
            result = 31 * result + (element ? 1231 : 1237);

        return result;
    }

    /**
     * Returns a hash code based on the contents of the specified array.
     * For any two {@code float} arrays {@code a} and {@code b}
     * such that {@code Arrays.equals(a, b)}, it is also the case that
     * {@code Arrays.hashCode(a) == Arrays.hashCode(b)}.
     *
     * <p>The value returned by this method is the same value that would be
     * obtained by invoking the {@link List#hashCode() hashCode}
     * method on a {@link List} containing a sequence of {@link Float}
     * instances representing the elements of {@code a} in the same order.
     * If {@code a} is {@code null}, this method returns 0.
     *
     * @param a the array whose hash value to compute
     * @return a content-based hash code for {@code a}
     * @since 1.5
     */
    public static int hashCode(float a[]) {
        if (a == null)
            return 0;

        int result = 1;
        for (float element : a)
            result = 31 * result + Float.floatToIntBits(element);

        return result;
    }

    /**
     * Returns a hash code based on the contents of the specified array.
     * For any two {@code double} arrays {@code a} and {@code b}
     * such that {@code Arrays.equals(a, b)}, it is also the case that
     * {@code Arrays.hashCode(a) == Arrays.hashCode(b)}.
     *
     * <p>The value returned by this method is the same value that would be
     * obtained by invoking the {@link List#hashCode() hashCode}
     * method on a {@link List} containing a sequence of {@link Double}
     * instances representing the elements of {@code a} in the same order.
     * If {@code a} is {@code null}, this method returns 0.
     *
     * @param a the array whose hash value to compute
     * @return a content-based hash code for {@code a}
     * @since 1.5
     */
    public static int hashCode(double a[]) {
        if (a == null)
            return 0;

        int result = 1;
        for (double element : a) {
            long bits = Double.doubleToLongBits(element);
            result = 31 * result + (int)(bits ^ (bits >>> 32));
        }
        return result;
    }

    /**
     * Returns a hash code based on the contents of the specified array.  If
     * the array contains other arrays as elements, the hash code is based on
     * their identities rather than their contents.  It is therefore
     * acceptable to invoke this method on an array that contains itself as an
     * element,  either directly or indirectly through one or more levels of
     * arrays.
     *
     * <p>For any two arrays {@code a} and {@code b} such that
     * {@code Arrays.equals(a, b)}, it is also the case that
     * {@code Arrays.hashCode(a) == Arrays.hashCode(b)}.
     *
     * <p>The value returned by this method is equal to the value that would
     * be returned by {@code Arrays.asList(a).hashCode()}, unless {@code a}
     * is {@code null}, in which case {@code 0} is returned.
     *
     * @param a the array whose content-based hash code to compute
     * @return a content-based hash code for {@code a}
     * @see #deepHashCode(Object[])
     * @since 1.5
     */
    public static int hashCode(Object a[]) {
        if (a == null)
            return 0;

        int result = 1;

        for (Object element : a)
            result = 31 * result + (element == null ? 0 : element.hashCode());

        return result;
    }

    /**
     * Returns a hash code based on the "deep contents" of the specified
     * array.  If the array contains other arrays as elements, the
     * hash code is based on their contents and so on, ad infinitum.
     * It is therefore unacceptable to invoke this method on an array that
     * contains itself as an element, either directly or indirectly through
     * one or more levels of arrays.  The behavior of such an invocation is
     * undefined.
     *
     * <p>For any two arrays {@code a} and {@code b} such that
     * {@code Arrays.deepEquals(a, b)}, it is also the case that
     * {@code Arrays.deepHashCode(a) == Arrays.deepHashCode(b)}.
     *
     * <p>The computation of the value returned by this method is similar to
     * that of the value returned by {@link List#hashCode()} on a list
     * containing the same elements as {@code a} in the same order, with one
     * difference: If an element {@code e} of {@code a} is itself an array,
     * its hash code is computed not by calling {@code e.hashCode()}, but as
     * by calling the appropriate overloading of {@code Arrays.hashCode(e)}
     * if {@code e} is an array of a primitive type, or as by calling
     * {@code Arrays.deepHashCode(e)} recursively if {@code e} is an array
     * of a reference type.  If {@code a} is {@code null}, this method
     * returns 0.
     *
     * @param a the array whose deep-content-based hash code to compute
     * @return a deep-content-based hash code for {@code a}
     * @see #hashCode(Object[])
     * @since 1.5
     */
    public static int deepHashCode(Object a[]) {
        if (a == null)
            return 0;

        int result = 1;

        for (Object element : a) {
            final int elementHash;
            final Class<?> cl;
            if (element == null)
                elementHash = 0;
            else if ((cl = element.getClass().getComponentType()) == null)
                elementHash = element.hashCode();
            else if (element instanceof Object[])
                elementHash = deepHashCode((Object[]) element);
            else
                elementHash = primitiveArrayHashCode(element, cl);

            result = 31 * result + elementHash;
        }

        return result;
    }

    private static int primitiveArrayHashCode(Object a, Class<?> cl) {
        return
            (cl == byte.class)    ? hashCode((byte[]) a)    :
            (cl == int.class)     ? hashCode((int[]) a)     :
            (cl == long.class)    ? hashCode((long[]) a)    :
            (cl == char.class)    ? hashCode((char[]) a)    :
            (cl == short.class)   ? hashCode((short[]) a)   :
            (cl == boolean.class) ? hashCode((boolean[]) a) :
            (cl == double.class)  ? hashCode((double[]) a)  :
            // If new primitive types are ever added, this method must be
            // expanded or we will fail here with ClassCastException.
            hashCode((float[]) a);
    }

    /**
     * Returns {@code true} if the two specified arrays are <i>deeply
     * equal</i> to one another.  Unlike the {@link #equals(Object[],Object[])}
     * method, this method is appropriate for use with nested arrays of
     * arbitrary depth.
     *
     * <p>Two array references are considered deeply equal if both
     * are {@code null}, or if they refer to arrays that contain the same
     * number of elements and all corresponding pairs of elements in the two
     * arrays are deeply equal.
     *
     * <p>Two possibly {@code null} elements {@code e1} and {@code e2} are
     * deeply equal if any of the following conditions hold:
     * <ul>
     *    <li> {@code e1} and {@code e2} are both arrays of object reference
     *         types, and {@code Arrays.deepEquals(e1, e2) would return true}
     *    <li> {@code e1} and {@code e2} are arrays of the same primitive
     *         type, and the appropriate overloading of
     *         {@code Arrays.equals(e1, e2)} would return true.
     *    <li> {@code e1 == e2}
     *    <li> {@code e1.equals(e2)} would return true.
     * </ul>
     * Note that this definition permits {@code null} elements at any depth.
     *
     * <p>If either of the specified arrays contain themselves as elements
     * either directly or indirectly through one or more levels of arrays,
     * the behavior of this method is undefined.
     *
     * @param a1 one array to be tested for equality
     * @param a2 the other array to be tested for equality
     * @return {@code true} if the two arrays are equal
     * @see #equals(Object[],Object[])
     * @see Objects#deepEquals(Object, Object)
     * @since 1.5
     */
    public static boolean deepEquals(Object[] a1, Object[] a2) {
        if (a1 == a2)
            return true;
        if (a1 == null || a2==null)
            return false;
        int length = a1.length;
        if (a2.length != length)
            return false;

        for (int i = 0; i < length; i++) {
            Object e1 = a1[i];
            Object e2 = a2[i];

            if (e1 == e2)
                continue;
            if (e1 == null)
                return false;

            // Figure out whether the two elements are equal
            boolean eq = deepEquals0(e1, e2);

            if (!eq)
                return false;
        }
        return true;
    }

    static boolean deepEquals0(Object e1, Object e2) {
        assert e1 != null;
        boolean eq;
        if (e1 instanceof Object[] && e2 instanceof Object[])
            eq = deepEquals ((Object[]) e1, (Object[]) e2);
        else if (e1 instanceof byte[] && e2 instanceof byte[])
            eq = equals((byte[]) e1, (byte[]) e2);
        else if (e1 instanceof short[] && e2 instanceof short[])
            eq = equals((short[]) e1, (short[]) e2);
        else if (e1 instanceof int[] && e2 instanceof int[])
            eq = equals((int[]) e1, (int[]) e2);
        else if (e1 instanceof long[] && e2 instanceof long[])
            eq = equals((long[]) e1, (long[]) e2);
        else if (e1 instanceof char[] && e2 instanceof char[])
            eq = equals((char[]) e1, (char[]) e2);
        else if (e1 instanceof float[] && e2 instanceof float[])
            eq = equals((float[]) e1, (float[]) e2);
        else if (e1 instanceof double[] && e2 instanceof double[])
            eq = equals((double[]) e1, (double[]) e2);
        else if (e1 instanceof boolean[] && e2 instanceof boolean[])
            eq = equals((boolean[]) e1, (boolean[]) e2);
        else
            eq = e1.equals(e2);
        return eq;
    }

    /**
     * Returns a string representation of the contents of the specified array.
     * The string representation consists of a list of the array's elements,
     * enclosed in square brackets ({@code "[]"}).  Adjacent elements are
     * separated by the characters {@code ", "} (a comma followed by a
     * space).  Elements are converted to strings as by
     * {@code String.valueOf(long)}.  Returns {@code "null"} if {@code a}
     * is {@code null}.
     *
     * @param a the array whose string representation to return
     * @return a string representation of {@code a}
     * @since 1.5
     */
    public static String toString(long[] a) {
        if (a == null)
            return "null";
        int iMax = a.length - 1;
        if (iMax == -1)
            return "[]";

        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            b.append(a[i]);
            if (i == iMax)
                return b.append(']').toString();
            b.append(", ");
        }
    }

    /**
     * Returns a string representation of the contents of the specified array.
     * The string representation consists of a list of the array's elements,
     * enclosed in square brackets ({@code "[]"}).  Adjacent elements are
     * separated by the characters {@code ", "} (a comma followed by a
     * space).  Elements are converted to strings as by
     * {@code String.valueOf(int)}.  Returns {@code "null"} if {@code a} is
     * {@code null}.
     *
     * @param a the array whose string representation to return
     * @return a string representation of {@code a}
     * @since 1.5
     */
    public static String toString(int[] a) {
        if (a == null)
            return "null";
        int iMax = a.length - 1;
        if (iMax == -1)
            return "[]";

        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            b.append(a[i]);
            if (i == iMax)
                return b.append(']').toString();
            b.append(", ");
        }
    }

    /**
     * Returns a string representation of the contents of the specified array.
     * The string representation consists of a list of the array's elements,
     * enclosed in square brackets ({@code "[]"}).  Adjacent elements are
     * separated by the characters {@code ", "} (a comma followed by a
     * space).  Elements are converted to strings as by
     * {@code String.valueOf(short)}.  Returns {@code "null"} if {@code a}
     * is {@code null}.
     *
     * @param a the array whose string representation to return
     * @return a string representation of {@code a}
     * @since 1.5
     */
    public static String toString(short[] a) {
        if (a == null)
            return "null";
        int iMax = a.length - 1;
        if (iMax == -1)
            return "[]";

        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            b.append(a[i]);
            if (i == iMax)
                return b.append(']').toString();
            b.append(", ");
        }
    }

    /**
     * Returns a string representation of the contents of the specified array.
     * The string representation consists of a list of the array's elements,
     * enclosed in square brackets ({@code "[]"}).  Adjacent elements are
     * separated by the characters {@code ", "} (a comma followed by a
     * space).  Elements are converted to strings as by
     * {@code String.valueOf(char)}.  Returns {@code "null"} if {@code a}
     * is {@code null}.
     *
     * @param a the array whose string representation to return
     * @return a string representation of {@code a}
     * @since 1.5
     */
    public static String toString(char[] a) {
        if (a == null)
            return "null";
        int iMax = a.length - 1;
        if (iMax == -1)
            return "[]";

        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            b.append(a[i]);
            if (i == iMax)
                return b.append(']').toString();
            b.append(", ");
        }
    }

    /**
     * Returns a string representation of the contents of the specified array.
     * The string representation consists of a list of the array's elements,
     * enclosed in square brackets ({@code "[]"}).  Adjacent elements
     * are separated by the characters {@code ", "} (a comma followed
     * by a space).  Elements are converted to strings as by
     * {@code String.valueOf(byte)}.  Returns {@code "null"} if
     * {@code a} is {@code null}.
     *
     * @param a the array whose string representation to return
     * @return a string representation of {@code a}
     * @since 1.5
     */
    public static String toString(byte[] a) {
        if (a == null)
            return "null";
        int iMax = a.length - 1;
        if (iMax == -1)
            return "[]";

        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            b.append(a[i]);
            if (i == iMax)
                return b.append(']').toString();
            b.append(", ");
        }
    }

    /**
     * Returns a string representation of the contents of the specified array.
     * The string representation consists of a list of the array's elements,
     * enclosed in square brackets ({@code "[]"}).  Adjacent elements are
     * separated by the characters {@code ", "} (a comma followed by a
     * space).  Elements are converted to strings as by
     * {@code String.valueOf(boolean)}.  Returns {@code "null"} if
     * {@code a} is {@code null}.
     *
     * @param a the array whose string representation to return
     * @return a string representation of {@code a}
     * @since 1.5
     */
    public static String toString(boolean[] a) {
        if (a == null)
            return "null";
        int iMax = a.length - 1;
        if (iMax == -1)
            return "[]";

        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            b.append(a[i]);
            if (i == iMax)
                return b.append(']').toString();
            b.append(", ");
        }
    }

    /**
     * Returns a string representation of the contents of the specified array.
     * The string representation consists of a list of the array's elements,
     * enclosed in square brackets ({@code "[]"}).  Adjacent elements are
     * separated by the characters {@code ", "} (a comma followed by a
     * space).  Elements are converted to strings as by
     * {@code String.valueOf(float)}.  Returns {@code "null"} if {@code a}
     * is {@code null}.
     *
     * @param a the array whose string representation to return
     * @return a string representation of {@code a}
     * @since 1.5
     */
    public static String toString(float[] a) {
        if (a == null)
            return "null";

        int iMax = a.length - 1;
        if (iMax == -1)
            return "[]";

        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            b.append(a[i]);
            if (i == iMax)
                return b.append(']').toString();
            b.append(", ");
        }
    }

    /**
     * Returns a string representation of the contents of the specified array.
     * The string representation consists of a list of the array's elements,
     * enclosed in square brackets ({@code "[]"}).  Adjacent elements are
     * separated by the characters {@code ", "} (a comma followed by a
     * space).  Elements are converted to strings as by
     * {@code String.valueOf(double)}.  Returns {@code "null"} if {@code a}
     * is {@code null}.
     *
     * @param a the array whose string representation to return
     * @return a string representation of {@code a}
     * @since 1.5
     */
    public static String toString(double[] a) {
        if (a == null)
            return "null";
        int iMax = a.length - 1;
        if (iMax == -1)
            return "[]";

        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            b.append(a[i]);
            if (i == iMax)
                return b.append(']').toString();
            b.append(", ");
        }
    }

    /**
     * Returns a string representation of the contents of the specified array.
     * If the array contains other arrays as elements, they are converted to
     * strings by the {@link Object#toString} method inherited from
     * {@code Object}, which describes their <i>identities</i> rather than
     * their contents.
     *
     * <p>The value returned by this method is equal to the value that would
     * be returned by {@code Arrays.asList(a).toString()}, unless {@code a}
     * is {@code null}, in which case {@code "null"} is returned.
     *
     * @param a the array whose string representation to return
     * @return a string representation of {@code a}
     * @see #deepToString(Object[])
     * @since 1.5
     */
    public static String toString(Object[] a) {
        if (a == null)
            return "null";

        int iMax = a.length - 1;
        if (iMax == -1)
            return "[]";

        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            b.append(String.valueOf(a[i]));
            if (i == iMax)
                return b.append(']').toString();
            b.append(", ");
        }
    }

    /**
     * Returns a string representation of the "deep contents" of the specified
     * array.  If the array contains other arrays as elements, the string
     * representation contains their contents and so on.  This method is
     * designed for converting multidimensional arrays to strings.
     *
     * <p>The string representation consists of a list of the array's
     * elements, enclosed in square brackets ({@code "[]"}).  Adjacent
     * elements are separated by the characters {@code ", "} (a comma
     * followed by a space).  Elements are converted to strings as by
     * {@code String.valueOf(Object)}, unless they are themselves
     * arrays.
     *
     * <p>If an element {@code e} is an array of a primitive type, it is
     * converted to a string as by invoking the appropriate overloading of
     * {@code Arrays.toString(e)}.  If an element {@code e} is an array of a
     * reference type, it is converted to a string as by invoking
     * this method recursively.
     *
     * <p>To avoid infinite recursion, if the specified array contains itself
     * as an element, or contains an indirect reference to itself through one
     * or more levels of arrays, the self-reference is converted to the string
     * {@code "[...]"}.  For example, an array containing only a reference
     * to itself would be rendered as {@code "[[...]]"}.
     *
     * <p>This method returns {@code "null"} if the specified array
     * is {@code null}.
     *
     * @param a the array whose string representation to return
     * @return a string representation of {@code a}
     * @see #toString(Object[])
     * @since 1.5
     */
    public static String deepToString(Object[] a) {
        if (a == null)
            return "null";

        int bufLen = 20 * a.length;
        if (a.length != 0 && bufLen <= 0)
            bufLen = Integer.MAX_VALUE;
        StringBuilder buf = new StringBuilder(bufLen);
        deepToString(a, buf, new HashSet<>());
        return buf.toString();
    }

    private static void deepToString(Object[] a, StringBuilder buf,
                                     Set<Object[]> dejaVu) {
        if (a == null) {
            buf.append("null");
            return;
        }
        int iMax = a.length - 1;
        if (iMax == -1) {
            buf.append("[]");
            return;
        }

        dejaVu.add(a);
        buf.append('[');
        for (int i = 0; ; i++) {

            Object element = a[i];
            if (element == null) {
                buf.append("null");
            } else {
                Class<?> eClass = element.getClass();

                if (eClass.isArray()) {
                    if (eClass == byte[].class)
                        buf.append(toString((byte[]) element));
                    else if (eClass == short[].class)
                        buf.append(toString((short[]) element));
                    else if (eClass == int[].class)
                        buf.append(toString((int[]) element));
                    else if (eClass == long[].class)
                        buf.append(toString((long[]) element));
                    else if (eClass == char[].class)
                        buf.append(toString((char[]) element));
                    else if (eClass == float[].class)
                        buf.append(toString((float[]) element));
                    else if (eClass == double[].class)
                        buf.append(toString((double[]) element));
                    else if (eClass == boolean[].class)
                        buf.append(toString((boolean[]) element));
                    else { // element is an array of object references
                        if (dejaVu.contains(element))
                            buf.append("[...]");
                        else
                            deepToString((Object[])element, buf, dejaVu);
                    }
                } else {  // element is non-null and not an array
                    buf.append(element.toString());
                }
            }
            if (i == iMax)
                break;
            buf.append(", ");
        }
        buf.append(']');
        dejaVu.remove(a);
    }


    /**
     * Set all elements of the specified array, using the provided
     * generator function to compute each element.
     *
     * <p>If the generator function throws an exception, it is relayed to
     * the caller and the array is left in an indeterminate state.
     *
     * @apiNote
     * Setting a subrange of an array, using a generator function to compute
     * each element, can be written as follows:
     * <pre>{@code
     * IntStream.range(startInclusive, endExclusive)
     *          .forEach(i -> array[i] = generator.apply(i));
     * }</pre>
     *
     * @param <T> type of elements of the array
     * @param array array to be initialized
     * @param generator a function accepting an index and producing the desired
     *        value for that position
     * @throws NullPointerException if the generator is null
     * @since 1.8
     */
    public static <T> void setAll(T[] array, IntFunction<? extends T> generator) {
        Objects.requireNonNull(generator);
        for (int i = 0; i < array.length; i++)
            array[i] = generator.apply(i);
    }

    /**
     * Set all elements of the specified array, in parallel, using the
     * provided generator function to compute each element.
     *
     * <p>If the generator function throws an exception, an unchecked exception
     * is thrown from {@code parallelSetAll} and the array is left in an
     * indeterminate state.
     *
     * @apiNote
     * Setting a subrange of an array, in parallel, using a generator function
     * to compute each element, can be written as follows:
     * <pre>{@code
     * IntStream.range(startInclusive, endExclusive)
     *          .parallel()
     *          .forEach(i -> array[i] = generator.apply(i));
     * }</pre>
     *
     * @param <T> type of elements of the array
     * @param array array to be initialized
     * @param generator a function accepting an index and producing the desired
     *        value for that position
     * @throws NullPointerException if the generator is null
     * @since 1.8
     */
    public static <T> void parallelSetAll(T[] array, IntFunction<? extends T> generator) {
        Objects.requireNonNull(generator);
        IntStream.range(0, array.length).parallel().forEach(i -> { array[i] = generator.apply(i); });
    }

    /**
     * Set all elements of the specified array, using the provided
     * generator function to compute each element.
     *
     * <p>If the generator function throws an exception, it is relayed to
     * the caller and the array is left in an indeterminate state.
     *
     * @apiNote
     * Setting a subrange of an array, using a generator function to compute
     * each element, can be written as follows:
     * <pre>{@code
     * IntStream.range(startInclusive, endExclusive)
     *          .forEach(i -> array[i] = generator.applyAsInt(i));
     * }</pre>
     *
     * @param array array to be initialized
     * @param generator a function accepting an index and producing the desired
     *        value for that position
     * @throws NullPointerException if the generator is null
     * @since 1.8
     */
    public static void setAll(int[] array, IntUnaryOperator generator) {
        Objects.requireNonNull(generator);
        for (int i = 0; i < array.length; i++)
            array[i] = generator.applyAsInt(i);
    }

    /**
     * Set all elements of the specified array, in parallel, using the
     * provided generator function to compute each element.
     *
     * <p>If the generator function throws an exception, an unchecked exception
     * is thrown from {@code parallelSetAll} and the array is left in an
     * indeterminate state.
     *
     * @apiNote
     * Setting a subrange of an array, in parallel, using a generator function
     * to compute each element, can be written as follows:
     * <pre>{@code
     * IntStream.range(startInclusive, endExclusive)
     *          .parallel()
     *          .forEach(i -> array[i] = generator.applyAsInt(i));
     * }</pre>
     *
     * @param array array to be initialized
     * @param generator a function accepting an index and producing the desired
     * value for that position
     * @throws NullPointerException if the generator is null
     * @since 1.8
     */
    public static void parallelSetAll(int[] array, IntUnaryOperator generator) {
        Objects.requireNonNull(generator);
        IntStream.range(0, array.length).parallel().forEach(i -> { array[i] = generator.applyAsInt(i); });
    }

    /**
     * Set all elements of the specified array, using the provided
     * generator function to compute each element.
     *
     * <p>If the generator function throws an exception, it is relayed to
     * the caller and the array is left in an indeterminate state.
     *
     * @apiNote
     * Setting a subrange of an array, using a generator function to compute
     * each element, can be written as follows:
     * <pre>{@code
     * IntStream.range(startInclusive, endExclusive)
     *          .forEach(i -> array[i] = generator.applyAsLong(i));
     * }</pre>
     *
     * @param array array to be initialized
     * @param generator a function accepting an index and producing the desired
     *        value for that position
     * @throws NullPointerException if the generator is null
     * @since 1.8
     */
    public static void setAll(long[] array, IntToLongFunction generator) {
        Objects.requireNonNull(generator);
        for (int i = 0; i < array.length; i++)
            array[i] = generator.applyAsLong(i);
    }

    /**
     * Set all elements of the specified array, in parallel, using the
     * provided generator function to compute each element.
     *
     * <p>If the generator function throws an exception, an unchecked exception
     * is thrown from {@code parallelSetAll} and the array is left in an
     * indeterminate state.
     *
     * @apiNote
     * Setting a subrange of an array, in parallel, using a generator function
     * to compute each element, can be written as follows:
     * <pre>{@code
     * IntStream.range(startInclusive, endExclusive)
     *          .parallel()
     *          .forEach(i -> array[i] = generator.applyAsLong(i));
     * }</pre>
     *
     * @param array array to be initialized
     * @param generator a function accepting an index and producing the desired
     *        value for that position
     * @throws NullPointerException if the generator is null
     * @since 1.8
     */
    public static void parallelSetAll(long[] array, IntToLongFunction generator) {
        Objects.requireNonNull(generator);
        IntStream.range(0, array.length).parallel().forEach(i -> { array[i] = generator.applyAsLong(i); });
    }

    /**
     * Set all elements of the specified array, using the provided
     * generator function to compute each element.
     *
     * <p>If the generator function throws an exception, it is relayed to
     * the caller and the array is left in an indeterminate state.
     *
     * @apiNote
     * Setting a subrange of an array, using a generator function to compute
     * each element, can be written as follows:
     * <pre>{@code
     * IntStream.range(startInclusive, endExclusive)
     *          .forEach(i -> array[i] = generator.applyAsDouble(i));
     * }</pre>
     *
     * @param array array to be initialized
     * @param generator a function accepting an index and producing the desired
     *        value for that position
     * @throws NullPointerException if the generator is null
     * @since 1.8
     */
    public static void setAll(double[] array, IntToDoubleFunction generator) {
        Objects.requireNonNull(generator);
        for (int i = 0; i < array.length; i++)
            array[i] = generator.applyAsDouble(i);
    }

    /**
     * Set all elements of the specified array, in parallel, using the
     * provided generator function to compute each element.
     *
     * <p>If the generator function throws an exception, an unchecked exception
     * is thrown from {@code parallelSetAll} and the array is left in an
     * indeterminate state.
     *
     * @apiNote
     * Setting a subrange of an array, in parallel, using a generator function
     * to compute each element, can be written as follows:
     * <pre>{@code
     * IntStream.range(startInclusive, endExclusive)
     *          .parallel()
     *          .forEach(i -> array[i] = generator.applyAsDouble(i));
     * }</pre>
     *
     * @param array array to be initialized
     * @param generator a function accepting an index and producing the desired
     *        value for that position
     * @throws NullPointerException if the generator is null
     * @since 1.8
     */
    public static void parallelSetAll(double[] array, IntToDoubleFunction generator) {
        Objects.requireNonNull(generator);
        IntStream.range(0, array.length).parallel().forEach(i -> { array[i] = generator.applyAsDouble(i); });
    }

    /**
     * Returns a {@link Spliterator} covering all of the specified array.
     *
     * <p>The spliterator reports {@link Spliterator#SIZED},
     * {@link Spliterator#SUBSIZED}, {@link Spliterator#ORDERED}, and
     * {@link Spliterator#IMMUTABLE}.
     *
     * @param <T> type of elements
     * @param array the array, assumed to be unmodified during use
     * @return a spliterator for the array elements
     * @since 1.8
     */
    public static <T> Spliterator<T> spliterator(T[] array) {
        return Spliterators.spliterator(array,
                                        Spliterator.ORDERED | Spliterator.IMMUTABLE);
    }

    /**
     * Returns a {@link Spliterator} covering the specified range of the
     * specified array.
     *
     * <p>The spliterator reports {@link Spliterator#SIZED},
     * {@link Spliterator#SUBSIZED}, {@link Spliterator#ORDERED}, and
     * {@link Spliterator#IMMUTABLE}.
     *
     * @param <T> type of elements
     * @param array the array, assumed to be unmodified during use
     * @param startInclusive the first index to cover, inclusive
     * @param endExclusive index immediately past the last index to cover
     * @return a spliterator for the array elements
     * @throws ArrayIndexOutOfBoundsException if {@code startInclusive} is
     *         negative, {@code endExclusive} is less than
     *         {@code startInclusive}, or {@code endExclusive} is greater than
     *         the array size
     * @since 1.8
     */
    public static <T> Spliterator<T> spliterator(T[] array, int startInclusive, int endExclusive) {
        return Spliterators.spliterator(array, startInclusive, endExclusive,
                                        Spliterator.ORDERED | Spliterator.IMMUTABLE);
    }

    /**
     * Returns a {@link Spliterator.OfInt} covering all of the specified array.
     *
     * <p>The spliterator reports {@link Spliterator#SIZED},
     * {@link Spliterator#SUBSIZED}, {@link Spliterator#ORDERED}, and
     * {@link Spliterator#IMMUTABLE}.
     *
     * @param array the array, assumed to be unmodified during use
     * @return a spliterator for the array elements
     * @since 1.8
     */
    public static Spliterator.OfInt spliterator(int[] array) {
        return Spliterators.spliterator(array,
                                        Spliterator.ORDERED | Spliterator.IMMUTABLE);
    }

    /**
     * Returns a {@link Spliterator.OfInt} covering the specified range of the
     * specified array.
     *
     * <p>The spliterator reports {@link Spliterator#SIZED},
     * {@link Spliterator#SUBSIZED}, {@link Spliterator#ORDERED}, and
     * {@link Spliterator#IMMUTABLE}.
     *
     * @param array the array, assumed to be unmodified during use
     * @param startInclusive the first index to cover, inclusive
     * @param endExclusive index immediately past the last index to cover
     * @return a spliterator for the array elements
     * @throws ArrayIndexOutOfBoundsException if {@code startInclusive} is
     *         negative, {@code endExclusive} is less than
     *         {@code startInclusive}, or {@code endExclusive} is greater than
     *         the array size
     * @since 1.8
     */
    public static Spliterator.OfInt spliterator(int[] array, int startInclusive, int endExclusive) {
        return Spliterators.spliterator(array, startInclusive, endExclusive,
                                        Spliterator.ORDERED | Spliterator.IMMUTABLE);
    }

    /**
     * Returns a {@link Spliterator.OfLong} covering all of the specified array.
     *
     * <p>The spliterator reports {@link Spliterator#SIZED},
     * {@link Spliterator#SUBSIZED}, {@link Spliterator#ORDERED}, and
     * {@link Spliterator#IMMUTABLE}.
     *
     * @param array the array, assumed to be unmodified during use
     * @return the spliterator for the array elements
     * @since 1.8
     */
    public static Spliterator.OfLong spliterator(long[] array) {
        return Spliterators.spliterator(array,
                                        Spliterator.ORDERED | Spliterator.IMMUTABLE);
    }

    /**
     * Returns a {@link Spliterator.OfLong} covering the specified range of the
     * specified array.
     *
     * <p>The spliterator reports {@link Spliterator#SIZED},
     * {@link Spliterator#SUBSIZED}, {@link Spliterator#ORDERED}, and
     * {@link Spliterator#IMMUTABLE}.
     *
     * @param array the array, assumed to be unmodified during use
     * @param startInclusive the first index to cover, inclusive
     * @param endExclusive index immediately past the last index to cover
     * @return a spliterator for the array elements
     * @throws ArrayIndexOutOfBoundsException if {@code startInclusive} is
     *         negative, {@code endExclusive} is less than
     *         {@code startInclusive}, or {@code endExclusive} is greater than
     *         the array size
     * @since 1.8
     */
    public static Spliterator.OfLong spliterator(long[] array, int startInclusive, int endExclusive) {
        return Spliterators.spliterator(array, startInclusive, endExclusive,
                                        Spliterator.ORDERED | Spliterator.IMMUTABLE);
    }

    /**
     * Returns a {@link Spliterator.OfDouble} covering all of the specified
     * array.
     *
     * <p>The spliterator reports {@link Spliterator#SIZED},
     * {@link Spliterator#SUBSIZED}, {@link Spliterator#ORDERED}, and
     * {@link Spliterator#IMMUTABLE}.
     *
     * @param array the array, assumed to be unmodified during use
     * @return a spliterator for the array elements
     * @since 1.8
     */
    public static Spliterator.OfDouble spliterator(double[] array) {
        return Spliterators.spliterator(array,
                                        Spliterator.ORDERED | Spliterator.IMMUTABLE);
    }

    /**
     * Returns a {@link Spliterator.OfDouble} covering the specified range of
     * the specified array.
     *
     * <p>The spliterator reports {@link Spliterator#SIZED},
     * {@link Spliterator#SUBSIZED}, {@link Spliterator#ORDERED}, and
     * {@link Spliterator#IMMUTABLE}.
     *
     * @param array the array, assumed to be unmodified during use
     * @param startInclusive the first index to cover, inclusive
     * @param endExclusive index immediately past the last index to cover
     * @return a spliterator for the array elements
     * @throws ArrayIndexOutOfBoundsException if {@code startInclusive} is
     *         negative, {@code endExclusive} is less than
     *         {@code startInclusive}, or {@code endExclusive} is greater than
     *         the array size
     * @since 1.8
     */
    public static Spliterator.OfDouble spliterator(double[] array, int startInclusive, int endExclusive) {
        return Spliterators.spliterator(array, startInclusive, endExclusive,
                                        Spliterator.ORDERED | Spliterator.IMMUTABLE);
    }

    /**
     * Returns a sequential {@link Stream} with the specified array as its
     * source.
     *
     * @param <T> The type of the array elements
     * @param array The array, assumed to be unmodified during use
     * @return a {@code Stream} for the array
     * @since 1.8
     */
    public static <T> Stream<T> stream(T[] array) {
        return stream(array, 0, array.length);
    }

    /**
     * Returns a sequential {@link Stream} with the specified range of the
     * specified array as its source.
     *
     * @param <T> the type of the array elements
     * @param array the array, assumed to be unmodified during use
     * @param startInclusive the first index to cover, inclusive
     * @param endExclusive index immediately past the last index to cover
     * @return a {@code Stream} for the array range
     * @throws ArrayIndexOutOfBoundsException if {@code startInclusive} is
     *         negative, {@code endExclusive} is less than
     *         {@code startInclusive}, or {@code endExclusive} is greater than
     *         the array size
     * @since 1.8
     */
    public static <T> Stream<T> stream(T[] array, int startInclusive, int endExclusive) {
        return StreamSupport.stream(spliterator(array, startInclusive, endExclusive), false);
    }

    /**
     * Returns a sequential {@link IntStream} with the specified array as its
     * source.
     *
     * @param array the array, assumed to be unmodified during use
     * @return an {@code IntStream} for the array
     * @since 1.8
     */
    public static IntStream stream(int[] array) {
        return stream(array, 0, array.length);
    }

    /**
     * Returns a sequential {@link IntStream} with the specified range of the
     * specified array as its source.
     *
     * @param array the array, assumed to be unmodified during use
     * @param startInclusive the first index to cover, inclusive
     * @param endExclusive index immediately past the last index to cover
     * @return an {@code IntStream} for the array range
     * @throws ArrayIndexOutOfBoundsException if {@code startInclusive} is
     *         negative, {@code endExclusive} is less than
     *         {@code startInclusive}, or {@code endExclusive} is greater than
     *         the array size
     * @since 1.8
     */
    public static IntStream stream(int[] array, int startInclusive, int endExclusive) {
        return StreamSupport.intStream(spliterator(array, startInclusive, endExclusive), false);
    }

    /**
     * Returns a sequential {@link LongStream} with the specified array as its
     * source.
     *
     * @param array the array, assumed to be unmodified during use
     * @return a {@code LongStream} for the array
     * @since 1.8
     */
    public static LongStream stream(long[] array) {
        return stream(array, 0, array.length);
    }

    /**
     * Returns a sequential {@link LongStream} with the specified range of the
     * specified array as its source.
     *
     * @param array the array, assumed to be unmodified during use
     * @param startInclusive the first index to cover, inclusive
     * @param endExclusive index immediately past the last index to cover
     * @return a {@code LongStream} for the array range
     * @throws ArrayIndexOutOfBoundsException if {@code startInclusive} is
     *         negative, {@code endExclusive} is less than
     *         {@code startInclusive}, or {@code endExclusive} is greater than
     *         the array size
     * @since 1.8
     */
    public static LongStream stream(long[] array, int startInclusive, int endExclusive) {
        return StreamSupport.longStream(spliterator(array, startInclusive, endExclusive), false);
    }

    /**
     * Returns a sequential {@link DoubleStream} with the specified array as its
     * source.
     *
     * @param array the array, assumed to be unmodified during use
     * @return a {@code DoubleStream} for the array
     * @since 1.8
     */
    public static DoubleStream stream(double[] array) {
        return stream(array, 0, array.length);
    }

    /**
     * Returns a sequential {@link DoubleStream} with the specified range of the
     * specified array as its source.
     *
     * @param array the array, assumed to be unmodified during use
     * @param startInclusive the first index to cover, inclusive
     * @param endExclusive index immediately past the last index to cover
     * @return a {@code DoubleStream} for the array range
     * @throws ArrayIndexOutOfBoundsException if {@code startInclusive} is
     *         negative, {@code endExclusive} is less than
     *         {@code startInclusive}, or {@code endExclusive} is greater than
     *         the array size
     * @since 1.8
     */
    public static DoubleStream stream(double[] array, int startInclusive, int endExclusive) {
        return StreamSupport.doubleStream(spliterator(array, startInclusive, endExclusive), false);
    }


    // Comparison methods

    // Compare boolean

    /**
     * Compares two {@code boolean} arrays lexicographically.
     *
     * <p>If the two arrays share a common prefix then the lexicographic
     * comparison is the result of comparing two elements, as if by
     * {@link Boolean#compare(boolean, boolean)}, at an index within the
     * respective arrays that is the prefix length.
     * Otherwise, one array is a proper prefix of the other and, lexicographic
     * comparison is the result of comparing the two array lengths.
     * (See {@link #mismatch(boolean[], boolean[])} for the definition of a
     * common and proper prefix.)
     *
     * <p>A {@code null} array reference is considered lexicographically less
     * than a non-{@code null} array reference.  Two {@code null} array
     * references are considered equal.
     *
     * <p>The comparison is consistent with {@link #equals(boolean[], boolean[]) equals},
     * more specifically the following holds for arrays {@code a} and {@code b}:
     * <pre>{@code
     *     Arrays.equals(a, b) == (Arrays.compare(a, b) == 0)
     * }</pre>
     *
     * @apiNote
     * <p>This method behaves as if (for non-{@code null} array references):
     * <pre>{@code
     *     int i = Arrays.mismatch(a, b);
     *     if (i >= 0 && i < Math.min(a.length, b.length))
     *         return Boolean.compare(a[i], b[i]);
     *     return a.length - b.length;
     * }</pre>
     *
     * @param a the first array to compare
     * @param b the second array to compare
     * @return the value {@code 0} if the first and second array are equal and
     *         contain the same elements in the same order;
     *         a value less than {@code 0} if the first array is
     *         lexicographically less than the second array; and
     *         a value greater than {@code 0} if the first array is
     *         lexicographically greater than the second array
     * @since 9
     */
    public static int compare(boolean[] a, boolean[] b) {
        if (a == b)
            return 0;
        if (a == null || b == null)
            return a == null ? -1 : 1;

        int i = ArraysSupport.mismatch(a, b,
                                       Math.min(a.length, b.length));
        if (i >= 0) {
            return Boolean.compare(a[i], b[i]);
        }

        return a.length - b.length;
    }

    /**
     * Compares two {@code boolean} arrays lexicographically over the specified
     * ranges.
     *
     * <p>If the two arrays, over the specified ranges, share a common prefix
     * then the lexicographic comparison is the result of comparing two
     * elements, as if by {@link Boolean#compare(boolean, boolean)}, at a
     * relative index within the respective arrays that is the length of the
     * prefix.
     * Otherwise, one array is a proper prefix of the other and, lexicographic
     * comparison is the result of comparing the two range lengths.
     * (See {@link #mismatch(boolean[], int, int, boolean[], int, int)} for the
     * definition of a common and proper prefix.)
     *
     * <p>The comparison is consistent with
     * {@link #equals(boolean[], int, int, boolean[], int, int) equals}, more
     * specifically the following holds for arrays {@code a} and {@code b} with
     * specified ranges [{@code aFromIndex}, {@code atoIndex}) and
     * [{@code bFromIndex}, {@code btoIndex}) respectively:
     * <pre>{@code
     *     Arrays.equals(a, aFromIndex, aToIndex, b, bFromIndex, bToIndex) ==
     *         (Arrays.compare(a, aFromIndex, aToIndex, b, bFromIndex, bToIndex) == 0)
     * }</pre>
     *
     * @apiNote
     * <p>This method behaves as if:
     * <pre>{@code
     *     int i = Arrays.mismatch(a, aFromIndex, aToIndex,
     *                             b, bFromIndex, bToIndex);
     *     if (i >= 0 && i < Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex))
     *         return Boolean.compare(a[aFromIndex + i], b[bFromIndex + i]);
     *     return (aToIndex - aFromIndex) - (bToIndex - bFromIndex);
     * }</pre>
     *
     * @param a the first array to compare
     * @param aFromIndex the index (inclusive) of the first element in the
     *                   first array to be compared
     * @param aToIndex the index (exclusive) of the last element in the
     *                 first array to be compared
     * @param b the second array to compare
     * @param bFromIndex the index (inclusive) of the first element in the
     *                   second array to be compared
     * @param bToIndex the index (exclusive) of the last element in the
     *                 second array to be compared
     * @return the value {@code 0} if, over the specified ranges, the first and
     *         second array are equal and contain the same elements in the same
     *         order;
     *         a value less than {@code 0} if, over the specified ranges, the
     *         first array is lexicographically less than the second array; and
     *         a value greater than {@code 0} if, over the specified ranges, the
     *         first array is lexicographically greater than the second array
     * @throws IllegalArgumentException
     *         if {@code aFromIndex > aToIndex} or
     *         if {@code bFromIndex > bToIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code aFromIndex < 0 or aToIndex > a.length} or
     *         if {@code bFromIndex < 0 or bToIndex > b.length}
     * @throws NullPointerException
     *         if either array is {@code null}
     * @since 9
     */
    public static int compare(boolean[] a, int aFromIndex, int aToIndex,
                              boolean[] b, int bFromIndex, int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int i = ArraysSupport.mismatch(a, aFromIndex,
                                       b, bFromIndex,
                                       Math.min(aLength, bLength));
        if (i >= 0) {
            return Boolean.compare(a[aFromIndex + i], b[bFromIndex + i]);
        }

        return aLength - bLength;
    }

    // Compare byte

    /**
     * Compares two {@code byte} arrays lexicographically.
     *
     * <p>If the two arrays share a common prefix then the lexicographic
     * comparison is the result of comparing two elements, as if by
     * {@link Byte#compare(byte, byte)}, at an index within the respective
     * arrays that is the prefix length.
     * Otherwise, one array is a proper prefix of the other and, lexicographic
     * comparison is the result of comparing the two array lengths.
     * (See {@link #mismatch(byte[], byte[])} for the definition of a common and
     * proper prefix.)
     *
     * <p>A {@code null} array reference is considered lexicographically less
     * than a non-{@code null} array reference.  Two {@code null} array
     * references are considered equal.
     *
     * <p>The comparison is consistent with {@link #equals(byte[], byte[]) equals},
     * more specifically the following holds for arrays {@code a} and {@code b}:
     * <pre>{@code
     *     Arrays.equals(a, b) == (Arrays.compare(a, b) == 0)
     * }</pre>
     *
     * @apiNote
     * <p>This method behaves as if (for non-{@code null} array references):
     * <pre>{@code
     *     int i = Arrays.mismatch(a, b);
     *     if (i >= 0 && i < Math.min(a.length, b.length))
     *         return Byte.compare(a[i], b[i]);
     *     return a.length - b.length;
     * }</pre>
     *
     * @param a the first array to compare
     * @param b the second array to compare
     * @return the value {@code 0} if the first and second array are equal and
     *         contain the same elements in the same order;
     *         a value less than {@code 0} if the first array is
     *         lexicographically less than the second array; and
     *         a value greater than {@code 0} if the first array is
     *         lexicographically greater than the second array
     * @since 9
     */
    public static int compare(byte[] a, byte[] b) {
        if (a == b)
            return 0;
        if (a == null || b == null)
            return a == null ? -1 : 1;

        int i = ArraysSupport.mismatch(a, b,
                                       Math.min(a.length, b.length));
        if (i >= 0) {
            return Byte.compare(a[i], b[i]);
        }

        return a.length - b.length;
    }

    /**
     * Compares two {@code byte} arrays lexicographically over the specified
     * ranges.
     *
     * <p>If the two arrays, over the specified ranges, share a common prefix
     * then the lexicographic comparison is the result of comparing two
     * elements, as if by {@link Byte#compare(byte, byte)}, at a relative index
     * within the respective arrays that is the length of the prefix.
     * Otherwise, one array is a proper prefix of the other and, lexicographic
     * comparison is the result of comparing the two range lengths.
     * (See {@link #mismatch(byte[], int, int, byte[], int, int)} for the
     * definition of a common and proper prefix.)
     *
     * <p>The comparison is consistent with
     * {@link #equals(byte[], int, int, byte[], int, int) equals}, more
     * specifically the following holds for arrays {@code a} and {@code b} with
     * specified ranges [{@code aFromIndex}, {@code atoIndex}) and
     * [{@code bFromIndex}, {@code btoIndex}) respectively:
     * <pre>{@code
     *     Arrays.equals(a, aFromIndex, aToIndex, b, bFromIndex, bToIndex) ==
     *         (Arrays.compare(a, aFromIndex, aToIndex, b, bFromIndex, bToIndex) == 0)
     * }</pre>
     *
     * @apiNote
     * <p>This method behaves as if:
     * <pre>{@code
     *     int i = Arrays.mismatch(a, aFromIndex, aToIndex,
     *                             b, bFromIndex, bToIndex);
     *     if (i >= 0 && i < Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex))
     *         return Byte.compare(a[aFromIndex + i], b[bFromIndex + i]);
     *     return (aToIndex - aFromIndex) - (bToIndex - bFromIndex);
     * }</pre>
     *
     * @param a the first array to compare
     * @param aFromIndex the index (inclusive) of the first element in the
     *                   first array to be compared
     * @param aToIndex the index (exclusive) of the last element in the
     *                 first array to be compared
     * @param b the second array to compare
     * @param bFromIndex the index (inclusive) of the first element in the
     *                   second array to be compared
     * @param bToIndex the index (exclusive) of the last element in the
     *                 second array to be compared
     * @return the value {@code 0} if, over the specified ranges, the first and
     *         second array are equal and contain the same elements in the same
     *         order;
     *         a value less than {@code 0} if, over the specified ranges, the
     *         first array is lexicographically less than the second array; and
     *         a value greater than {@code 0} if, over the specified ranges, the
     *         first array is lexicographically greater than the second array
     * @throws IllegalArgumentException
     *         if {@code aFromIndex > aToIndex} or
     *         if {@code bFromIndex > bToIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code aFromIndex < 0 or aToIndex > a.length} or
     *         if {@code bFromIndex < 0 or bToIndex > b.length}
     * @throws NullPointerException
     *         if either array is {@code null}
     * @since 9
     */
    public static int compare(byte[] a, int aFromIndex, int aToIndex,
                              byte[] b, int bFromIndex, int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int i = ArraysSupport.mismatch(a, aFromIndex,
                                       b, bFromIndex,
                                       Math.min(aLength, bLength));
        if (i >= 0) {
            return Byte.compare(a[aFromIndex + i], b[bFromIndex + i]);
        }

        return aLength - bLength;
    }

    /**
     * Compares two {@code byte} arrays lexicographically, numerically treating
     * elements as unsigned.
     *
     * <p>If the two arrays share a common prefix then the lexicographic
     * comparison is the result of comparing two elements, as if by
     * {@link Byte#compareUnsigned(byte, byte)}, at an index within the
     * respective arrays that is the prefix length.
     * Otherwise, one array is a proper prefix of the other and, lexicographic
     * comparison is the result of comparing the two array lengths.
     * (See {@link #mismatch(byte[], byte[])} for the definition of a common
     * and proper prefix.)
     *
     * <p>A {@code null} array reference is considered lexicographically less
     * than a non-{@code null} array reference.  Two {@code null} array
     * references are considered equal.
     *
     * @apiNote
     * <p>This method behaves as if (for non-{@code null} array references):
     * <pre>{@code
     *     int i = Arrays.mismatch(a, b);
     *     if (i >= 0 && i < Math.min(a.length, b.length))
     *         return Byte.compareUnsigned(a[i], b[i]);
     *     return a.length - b.length;
     * }</pre>
     *
     * @param a the first array to compare
     * @param b the second array to compare
     * @return the value {@code 0} if the first and second array are
     *         equal and contain the same elements in the same order;
     *         a value less than {@code 0} if the first array is
     *         lexicographically less than the second array; and
     *         a value greater than {@code 0} if the first array is
     *         lexicographically greater than the second array
     * @since 9
     */
    public static int compareUnsigned(byte[] a, byte[] b) {
        if (a == b)
            return 0;
        if (a == null || b == null)
            return a == null ? -1 : 1;

        int i = ArraysSupport.mismatch(a, b,
                                       Math.min(a.length, b.length));
        if (i >= 0) {
            return Byte.compareUnsigned(a[i], b[i]);
        }

        return a.length - b.length;
    }


    /**
     * Compares two {@code byte} arrays lexicographically over the specified
     * ranges, numerically treating elements as unsigned.
     *
     * <p>If the two arrays, over the specified ranges, share a common prefix
     * then the lexicographic comparison is the result of comparing two
     * elements, as if by {@link Byte#compareUnsigned(byte, byte)}, at a
     * relative index within the respective arrays that is the length of the
     * prefix.
     * Otherwise, one array is a proper prefix of the other and, lexicographic
     * comparison is the result of comparing the two range lengths.
     * (See {@link #mismatch(byte[], int, int, byte[], int, int)} for the
     * definition of a common and proper prefix.)
     *
     * @apiNote
     * <p>This method behaves as if:
     * <pre>{@code
     *     int i = Arrays.mismatch(a, aFromIndex, aToIndex,
     *                             b, bFromIndex, bToIndex);
     *     if (i >= 0 && i < Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex))
     *         return Byte.compareUnsigned(a[aFromIndex + i], b[bFromIndex + i]);
     *     return (aToIndex - aFromIndex) - (bToIndex - bFromIndex);
     * }</pre>
     *
     * @param a the first array to compare
     * @param aFromIndex the index (inclusive) of the first element in the
     *                   first array to be compared
     * @param aToIndex the index (exclusive) of the last element in the
     *                 first array to be compared
     * @param b the second array to compare
     * @param bFromIndex the index (inclusive) of the first element in the
     *                   second array to be compared
     * @param bToIndex the index (exclusive) of the last element in the
     *                 second array to be compared
     * @return the value {@code 0} if, over the specified ranges, the first and
     *         second array are equal and contain the same elements in the same
     *         order;
     *         a value less than {@code 0} if, over the specified ranges, the
     *         first array is lexicographically less than the second array; and
     *         a value greater than {@code 0} if, over the specified ranges, the
     *         first array is lexicographically greater than the second array
     * @throws IllegalArgumentException
     *         if {@code aFromIndex > aToIndex} or
     *         if {@code bFromIndex > bToIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code aFromIndex < 0 or aToIndex > a.length} or
     *         if {@code bFromIndex < 0 or bToIndex > b.length}
     * @throws NullPointerException
     *         if either array is null
     * @since 9
     */
    public static int compareUnsigned(byte[] a, int aFromIndex, int aToIndex,
                                      byte[] b, int bFromIndex, int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int i = ArraysSupport.mismatch(a, aFromIndex,
                                       b, bFromIndex,
                                       Math.min(aLength, bLength));
        if (i >= 0) {
            return Byte.compareUnsigned(a[aFromIndex + i], b[bFromIndex + i]);
        }

        return aLength - bLength;
    }

    // Compare short

    /**
     * Compares two {@code short} arrays lexicographically.
     *
     * <p>If the two arrays share a common prefix then the lexicographic
     * comparison is the result of comparing two elements, as if by
     * {@link Short#compare(short, short)}, at an index within the respective
     * arrays that is the prefix length.
     * Otherwise, one array is a proper prefix of the other and, lexicographic
     * comparison is the result of comparing the two array lengths.
     * (See {@link #mismatch(short[], short[])} for the definition of a common
     * and proper prefix.)
     *
     * <p>A {@code null} array reference is considered lexicographically less
     * than a non-{@code null} array reference.  Two {@code null} array
     * references are considered equal.
     *
     * <p>The comparison is consistent with {@link #equals(short[], short[]) equals},
     * more specifically the following holds for arrays {@code a} and {@code b}:
     * <pre>{@code
     *     Arrays.equals(a, b) == (Arrays.compare(a, b) == 0)
     * }</pre>
     *
     * @apiNote
     * <p>This method behaves as if (for non-{@code null} array references):
     * <pre>{@code
     *     int i = Arrays.mismatch(a, b);
     *     if (i >= 0 && i < Math.min(a.length, b.length))
     *         return Short.compare(a[i], b[i]);
     *     return a.length - b.length;
     * }</pre>
     *
     * @param a the first array to compare
     * @param b the second array to compare
     * @return the value {@code 0} if the first and second array are equal and
     *         contain the same elements in the same order;
     *         a value less than {@code 0} if the first array is
     *         lexicographically less than the second array; and
     *         a value greater than {@code 0} if the first array is
     *         lexicographically greater than the second array
     * @since 9
     */
    public static int compare(short[] a, short[] b) {
        if (a == b)
            return 0;
        if (a == null || b == null)
            return a == null ? -1 : 1;

        int i = ArraysSupport.mismatch(a, b,
                                       Math.min(a.length, b.length));
        if (i >= 0) {
            return Short.compare(a[i], b[i]);
        }

        return a.length - b.length;
    }

    /**
     * Compares two {@code short} arrays lexicographically over the specified
     * ranges.
     *
     * <p>If the two arrays, over the specified ranges, share a common prefix
     * then the lexicographic comparison is the result of comparing two
     * elements, as if by {@link Short#compare(short, short)}, at a relative
     * index within the respective arrays that is the length of the prefix.
     * Otherwise, one array is a proper prefix of the other and, lexicographic
     * comparison is the result of comparing the two range lengths.
     * (See {@link #mismatch(short[], int, int, short[], int, int)} for the
     * definition of a common and proper prefix.)
     *
     * <p>The comparison is consistent with
     * {@link #equals(short[], int, int, short[], int, int) equals}, more
     * specifically the following holds for arrays {@code a} and {@code b} with
     * specified ranges [{@code aFromIndex}, {@code atoIndex}) and
     * [{@code bFromIndex}, {@code btoIndex}) respectively:
     * <pre>{@code
     *     Arrays.equals(a, aFromIndex, aToIndex, b, bFromIndex, bToIndex) ==
     *         (Arrays.compare(a, aFromIndex, aToIndex, b, bFromIndex, bToIndex) == 0)
     * }</pre>
     *
     * @apiNote
     * <p>This method behaves as if:
     * <pre>{@code
     *     int i = Arrays.mismatch(a, aFromIndex, aToIndex,
     *                             b, bFromIndex, bToIndex);
     *     if (i >= 0 && i < Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex))
     *         return Short.compare(a[aFromIndex + i], b[bFromIndex + i]);
     *     return (aToIndex - aFromIndex) - (bToIndex - bFromIndex);
     * }</pre>
     *
     * @param a the first array to compare
     * @param aFromIndex the index (inclusive) of the first element in the
     *                   first array to be compared
     * @param aToIndex the index (exclusive) of the last element in the
     *                 first array to be compared
     * @param b the second array to compare
     * @param bFromIndex the index (inclusive) of the first element in the
     *                   second array to be compared
     * @param bToIndex the index (exclusive) of the last element in the
     *                 second array to be compared
     * @return the value {@code 0} if, over the specified ranges, the first and
     *         second array are equal and contain the same elements in the same
     *         order;
     *         a value less than {@code 0} if, over the specified ranges, the
     *         first array is lexicographically less than the second array; and
     *         a value greater than {@code 0} if, over the specified ranges, the
     *         first array is lexicographically greater than the second array
     * @throws IllegalArgumentException
     *         if {@code aFromIndex > aToIndex} or
     *         if {@code bFromIndex > bToIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code aFromIndex < 0 or aToIndex > a.length} or
     *         if {@code bFromIndex < 0 or bToIndex > b.length}
     * @throws NullPointerException
     *         if either array is {@code null}
     * @since 9
     */
    public static int compare(short[] a, int aFromIndex, int aToIndex,
                              short[] b, int bFromIndex, int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int i = ArraysSupport.mismatch(a, aFromIndex,
                                       b, bFromIndex,
                                       Math.min(aLength, bLength));
        if (i >= 0) {
            return Short.compare(a[aFromIndex + i], b[bFromIndex + i]);
        }

        return aLength - bLength;
    }

    /**
     * Compares two {@code short} arrays lexicographically, numerically treating
     * elements as unsigned.
     *
     * <p>If the two arrays share a common prefix then the lexicographic
     * comparison is the result of comparing two elements, as if by
     * {@link Short#compareUnsigned(short, short)}, at an index within the
     * respective arrays that is the prefix length.
     * Otherwise, one array is a proper prefix of the other and, lexicographic
     * comparison is the result of comparing the two array lengths.
     * (See {@link #mismatch(short[], short[])} for the definition of a common
     * and proper prefix.)
     *
     * <p>A {@code null} array reference is considered lexicographically less
     * than a non-{@code null} array reference.  Two {@code null} array
     * references are considered equal.
     *
     * @apiNote
     * <p>This method behaves as if (for non-{@code null} array references):
     * <pre>{@code
     *     int i = Arrays.mismatch(a, b);
     *     if (i >= 0 && i < Math.min(a.length, b.length))
     *         return Short.compareUnsigned(a[i], b[i]);
     *     return a.length - b.length;
     * }</pre>
     *
     * @param a the first array to compare
     * @param b the second array to compare
     * @return the value {@code 0} if the first and second array are
     *         equal and contain the same elements in the same order;
     *         a value less than {@code 0} if the first array is
     *         lexicographically less than the second array; and
     *         a value greater than {@code 0} if the first array is
     *         lexicographically greater than the second array
     * @since 9
     */
    public static int compareUnsigned(short[] a, short[] b) {
        if (a == b)
            return 0;
        if (a == null || b == null)
            return a == null ? -1 : 1;

        int i = ArraysSupport.mismatch(a, b,
                                       Math.min(a.length, b.length));
        if (i >= 0) {
            return Short.compareUnsigned(a[i], b[i]);
        }

        return a.length - b.length;
    }

    /**
     * Compares two {@code short} arrays lexicographically over the specified
     * ranges, numerically treating elements as unsigned.
     *
     * <p>If the two arrays, over the specified ranges, share a common prefix
     * then the lexicographic comparison is the result of comparing two
     * elements, as if by {@link Short#compareUnsigned(short, short)}, at a
     * relative index within the respective arrays that is the length of the
     * prefix.
     * Otherwise, one array is a proper prefix of the other and, lexicographic
     * comparison is the result of comparing the two range lengths.
     * (See {@link #mismatch(short[], int, int, short[], int, int)} for the
     * definition of a common and proper prefix.)
     *
     * @apiNote
     * <p>This method behaves as if:
     * <pre>{@code
     *     int i = Arrays.mismatch(a, aFromIndex, aToIndex,
     *                             b, bFromIndex, bToIndex);
     *     if (i >= 0 && i < Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex))
     *         return Short.compareUnsigned(a[aFromIndex + i], b[bFromIndex + i]);
     *     return (aToIndex - aFromIndex) - (bToIndex - bFromIndex);
     * }</pre>
     *
     * @param a the first array to compare
     * @param aFromIndex the index (inclusive) of the first element in the
     *                   first array to be compared
     * @param aToIndex the index (exclusive) of the last element in the
     *                 first array to be compared
     * @param b the second array to compare
     * @param bFromIndex the index (inclusive) of the first element in the
     *                   second array to be compared
     * @param bToIndex the index (exclusive) of the last element in the
     *                 second array to be compared
     * @return the value {@code 0} if, over the specified ranges, the first and
     *         second array are equal and contain the same elements in the same
     *         order;
     *         a value less than {@code 0} if, over the specified ranges, the
     *         first array is lexicographically less than the second array; and
     *         a value greater than {@code 0} if, over the specified ranges, the
     *         first array is lexicographically greater than the second array
     * @throws IllegalArgumentException
     *         if {@code aFromIndex > aToIndex} or
     *         if {@code bFromIndex > bToIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code aFromIndex < 0 or aToIndex > a.length} or
     *         if {@code bFromIndex < 0 or bToIndex > b.length}
     * @throws NullPointerException
     *         if either array is null
     * @since 9
     */
    public static int compareUnsigned(short[] a, int aFromIndex, int aToIndex,
                                      short[] b, int bFromIndex, int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int i = ArraysSupport.mismatch(a, aFromIndex,
                                       b, bFromIndex,
                                       Math.min(aLength, bLength));
        if (i >= 0) {
            return Short.compareUnsigned(a[aFromIndex + i], b[bFromIndex + i]);
        }

        return aLength - bLength;
    }

    // Compare char

    /**
     * Compares two {@code char} arrays lexicographically.
     *
     * <p>If the two arrays share a common prefix then the lexicographic
     * comparison is the result of comparing two elements, as if by
     * {@link Character#compare(char, char)}, at an index within the respective
     * arrays that is the prefix length.
     * Otherwise, one array is a proper prefix of the other and, lexicographic
     * comparison is the result of comparing the two array lengths.
     * (See {@link #mismatch(char[], char[])} for the definition of a common and
     * proper prefix.)
     *
     * <p>A {@code null} array reference is considered lexicographically less
     * than a non-{@code null} array reference.  Two {@code null} array
     * references are considered equal.
     *
     * <p>The comparison is consistent with {@link #equals(char[], char[]) equals},
     * more specifically the following holds for arrays {@code a} and {@code b}:
     * <pre>{@code
     *     Arrays.equals(a, b) == (Arrays.compare(a, b) == 0)
     * }</pre>
     *
     * @apiNote
     * <p>This method behaves as if (for non-{@code null} array references):
     * <pre>{@code
     *     int i = Arrays.mismatch(a, b);
     *     if (i >= 0 && i < Math.min(a.length, b.length))
     *         return Character.compare(a[i], b[i]);
     *     return a.length - b.length;
     * }</pre>
     *
     * @param a the first array to compare
     * @param b the second array to compare
     * @return the value {@code 0} if the first and second array are equal and
     *         contain the same elements in the same order;
     *         a value less than {@code 0} if the first array is
     *         lexicographically less than the second array; and
     *         a value greater than {@code 0} if the first array is
     *         lexicographically greater than the second array
     * @since 9
     */
    public static int compare(char[] a, char[] b) {
        if (a == b)
            return 0;
        if (a == null || b == null)
            return a == null ? -1 : 1;

        int i = ArraysSupport.mismatch(a, b,
                                       Math.min(a.length, b.length));
        if (i >= 0) {
            return Character.compare(a[i], b[i]);
        }

        return a.length - b.length;
    }

    /**
     * Compares two {@code char} arrays lexicographically over the specified
     * ranges.
     *
     * <p>If the two arrays, over the specified ranges, share a common prefix
     * then the lexicographic comparison is the result of comparing two
     * elements, as if by {@link Character#compare(char, char)}, at a relative
     * index within the respective arrays that is the length of the prefix.
     * Otherwise, one array is a proper prefix of the other and, lexicographic
     * comparison is the result of comparing the two range lengths.
     * (See {@link #mismatch(char[], int, int, char[], int, int)} for the
     * definition of a common and proper prefix.)
     *
     * <p>The comparison is consistent with
     * {@link #equals(char[], int, int, char[], int, int) equals}, more
     * specifically the following holds for arrays {@code a} and {@code b} with
     * specified ranges [{@code aFromIndex}, {@code atoIndex}) and
     * [{@code bFromIndex}, {@code btoIndex}) respectively:
     * <pre>{@code
     *     Arrays.equals(a, aFromIndex, aToIndex, b, bFromIndex, bToIndex) ==
     *         (Arrays.compare(a, aFromIndex, aToIndex, b, bFromIndex, bToIndex) == 0)
     * }</pre>
     *
     * @apiNote
     * <p>This method behaves as if:
     * <pre>{@code
     *     int i = Arrays.mismatch(a, aFromIndex, aToIndex,
     *                             b, bFromIndex, bToIndex);
     *     if (i >= 0 && i < Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex))
     *         return Character.compare(a[aFromIndex + i], b[bFromIndex + i]);
     *     return (aToIndex - aFromIndex) - (bToIndex - bFromIndex);
     * }</pre>
     *
     * @param a the first array to compare
     * @param aFromIndex the index (inclusive) of the first element in the
     *                   first array to be compared
     * @param aToIndex the index (exclusive) of the last element in the
     *                 first array to be compared
     * @param b the second array to compare
     * @param bFromIndex the index (inclusive) of the first element in the
     *                   second array to be compared
     * @param bToIndex the index (exclusive) of the last element in the
     *                 second array to be compared
     * @return the value {@code 0} if, over the specified ranges, the first and
     *         second array are equal and contain the same elements in the same
     *         order;
     *         a value less than {@code 0} if, over the specified ranges, the
     *         first array is lexicographically less than the second array; and
     *         a value greater than {@code 0} if, over the specified ranges, the
     *         first array is lexicographically greater than the second array
     * @throws IllegalArgumentException
     *         if {@code aFromIndex > aToIndex} or
     *         if {@code bFromIndex > bToIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code aFromIndex < 0 or aToIndex > a.length} or
     *         if {@code bFromIndex < 0 or bToIndex > b.length}
     * @throws NullPointerException
     *         if either array is {@code null}
     * @since 9
     */
    public static int compare(char[] a, int aFromIndex, int aToIndex,
                              char[] b, int bFromIndex, int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int i = ArraysSupport.mismatch(a, aFromIndex,
                                       b, bFromIndex,
                                       Math.min(aLength, bLength));
        if (i >= 0) {
            return Character.compare(a[aFromIndex + i], b[bFromIndex + i]);
        }

        return aLength - bLength;
    }

    // Compare int

    /**
     * Compares two {@code int} arrays lexicographically.
     *
     * <p>If the two arrays share a common prefix then the lexicographic
     * comparison is the result of comparing two elements, as if by
     * {@link Integer#compare(int, int)}, at an index within the respective
     * arrays that is the prefix length.
     * Otherwise, one array is a proper prefix of the other and, lexicographic
     * comparison is the result of comparing the two array lengths.
     * (See {@link #mismatch(int[], int[])} for the definition of a common and
     * proper prefix.)
     *
     * <p>A {@code null} array reference is considered lexicographically less
     * than a non-{@code null} array reference.  Two {@code null} array
     * references are considered equal.
     *
     * <p>The comparison is consistent with {@link #equals(int[], int[]) equals},
     * more specifically the following holds for arrays {@code a} and {@code b}:
     * <pre>{@code
     *     Arrays.equals(a, b) == (Arrays.compare(a, b) == 0)
     * }</pre>
     *
     * @apiNote
     * <p>This method behaves as if (for non-{@code null} array references):
     * <pre>{@code
     *     int i = Arrays.mismatch(a, b);
     *     if (i >= 0 && i < Math.min(a.length, b.length))
     *         return Integer.compare(a[i], b[i]);
     *     return a.length - b.length;
     * }</pre>
     *
     * @param a the first array to compare
     * @param b the second array to compare
     * @return the value {@code 0} if the first and second array are equal and
     *         contain the same elements in the same order;
     *         a value less than {@code 0} if the first array is
     *         lexicographically less than the second array; and
     *         a value greater than {@code 0} if the first array is
     *         lexicographically greater than the second array
     * @since 9
     */
    public static int compare(int[] a, int[] b) {
        if (a == b)
            return 0;
        if (a == null || b == null)
            return a == null ? -1 : 1;

        int i = ArraysSupport.mismatch(a, b,
                                       Math.min(a.length, b.length));
        if (i >= 0) {
            return Integer.compare(a[i], b[i]);
        }

        return a.length - b.length;
    }

    /**
     * Compares two {@code int} arrays lexicographically over the specified
     * ranges.
     *
     * <p>If the two arrays, over the specified ranges, share a common prefix
     * then the lexicographic comparison is the result of comparing two
     * elements, as if by {@link Integer#compare(int, int)}, at a relative index
     * within the respective arrays that is the length of the prefix.
     * Otherwise, one array is a proper prefix of the other and, lexicographic
     * comparison is the result of comparing the two range lengths.
     * (See {@link #mismatch(int[], int, int, int[], int, int)} for the
     * definition of a common and proper prefix.)
     *
     * <p>The comparison is consistent with
     * {@link #equals(int[], int, int, int[], int, int) equals}, more
     * specifically the following holds for arrays {@code a} and {@code b} with
     * specified ranges [{@code aFromIndex}, {@code atoIndex}) and
     * [{@code bFromIndex}, {@code btoIndex}) respectively:
     * <pre>{@code
     *     Arrays.equals(a, aFromIndex, aToIndex, b, bFromIndex, bToIndex) ==
     *         (Arrays.compare(a, aFromIndex, aToIndex, b, bFromIndex, bToIndex) == 0)
     * }</pre>
     *
     * @apiNote
     * <p>This method behaves as if:
     * <pre>{@code
     *     int i = Arrays.mismatch(a, aFromIndex, aToIndex,
     *                             b, bFromIndex, bToIndex);
     *     if (i >= 0 && i < Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex))
     *         return Integer.compare(a[aFromIndex + i], b[bFromIndex + i]);
     *     return (aToIndex - aFromIndex) - (bToIndex - bFromIndex);
     * }</pre>
     *
     * @param a the first array to compare
     * @param aFromIndex the index (inclusive) of the first element in the
     *                   first array to be compared
     * @param aToIndex the index (exclusive) of the last element in the
     *                 first array to be compared
     * @param b the second array to compare
     * @param bFromIndex the index (inclusive) of the first element in the
     *                   second array to be compared
     * @param bToIndex the index (exclusive) of the last element in the
     *                 second array to be compared
     * @return the value {@code 0} if, over the specified ranges, the first and
     *         second array are equal and contain the same elements in the same
     *         order;
     *         a value less than {@code 0} if, over the specified ranges, the
     *         first array is lexicographically less than the second array; and
     *         a value greater than {@code 0} if, over the specified ranges, the
     *         first array is lexicographically greater than the second array
     * @throws IllegalArgumentException
     *         if {@code aFromIndex > aToIndex} or
     *         if {@code bFromIndex > bToIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code aFromIndex < 0 or aToIndex > a.length} or
     *         if {@code bFromIndex < 0 or bToIndex > b.length}
     * @throws NullPointerException
     *         if either array is {@code null}
     * @since 9
     */
    public static int compare(int[] a, int aFromIndex, int aToIndex,
                              int[] b, int bFromIndex, int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int i = ArraysSupport.mismatch(a, aFromIndex,
                                       b, bFromIndex,
                                       Math.min(aLength, bLength));
        if (i >= 0) {
            return Integer.compare(a[aFromIndex + i], b[bFromIndex + i]);
        }

        return aLength - bLength;
    }

    /**
     * Compares two {@code int} arrays lexicographically, numerically treating
     * elements as unsigned.
     *
     * <p>If the two arrays share a common prefix then the lexicographic
     * comparison is the result of comparing two elements, as if by
     * {@link Integer#compareUnsigned(int, int)}, at an index within the
     * respective arrays that is the prefix length.
     * Otherwise, one array is a proper prefix of the other and, lexicographic
     * comparison is the result of comparing the two array lengths.
     * (See {@link #mismatch(int[], int[])} for the definition of a common
     * and proper prefix.)
     *
     * <p>A {@code null} array reference is considered lexicographically less
     * than a non-{@code null} array reference.  Two {@code null} array
     * references are considered equal.
     *
     * @apiNote
     * <p>This method behaves as if (for non-{@code null} array references):
     * <pre>{@code
     *     int i = Arrays.mismatch(a, b);
     *     if (i >= 0 && i < Math.min(a.length, b.length))
     *         return Integer.compareUnsigned(a[i], b[i]);
     *     return a.length - b.length;
     * }</pre>
     *
     * @param a the first array to compare
     * @param b the second array to compare
     * @return the value {@code 0} if the first and second array are
     *         equal and contain the same elements in the same order;
     *         a value less than {@code 0} if the first array is
     *         lexicographically less than the second array; and
     *         a value greater than {@code 0} if the first array is
     *         lexicographically greater than the second array
     * @since 9
     */
    public static int compareUnsigned(int[] a, int[] b) {
        if (a == b)
            return 0;
        if (a == null || b == null)
            return a == null ? -1 : 1;

        int i = ArraysSupport.mismatch(a, b,
                                       Math.min(a.length, b.length));
        if (i >= 0) {
            return Integer.compareUnsigned(a[i], b[i]);
        }

        return a.length - b.length;
    }

    /**
     * Compares two {@code int} arrays lexicographically over the specified
     * ranges, numerically treating elements as unsigned.
     *
     * <p>If the two arrays, over the specified ranges, share a common prefix
     * then the lexicographic comparison is the result of comparing two
     * elements, as if by {@link Integer#compareUnsigned(int, int)}, at a
     * relative index within the respective arrays that is the length of the
     * prefix.
     * Otherwise, one array is a proper prefix of the other and, lexicographic
     * comparison is the result of comparing the two range lengths.
     * (See {@link #mismatch(int[], int, int, int[], int, int)} for the
     * definition of a common and proper prefix.)
     *
     * @apiNote
     * <p>This method behaves as if:
     * <pre>{@code
     *     int i = Arrays.mismatch(a, aFromIndex, aToIndex,
     *                             b, bFromIndex, bToIndex);
     *     if (i >= 0 && i < Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex))
     *         return Integer.compareUnsigned(a[aFromIndex + i], b[bFromIndex + i]);
     *     return (aToIndex - aFromIndex) - (bToIndex - bFromIndex);
     * }</pre>
     *
     * @param a the first array to compare
     * @param aFromIndex the index (inclusive) of the first element in the
     *                   first array to be compared
     * @param aToIndex the index (exclusive) of the last element in the
     *                 first array to be compared
     * @param b the second array to compare
     * @param bFromIndex the index (inclusive) of the first element in the
     *                   second array to be compared
     * @param bToIndex the index (exclusive) of the last element in the
     *                 second array to be compared
     * @return the value {@code 0} if, over the specified ranges, the first and
     *         second array are equal and contain the same elements in the same
     *         order;
     *         a value less than {@code 0} if, over the specified ranges, the
     *         first array is lexicographically less than the second array; and
     *         a value greater than {@code 0} if, over the specified ranges, the
     *         first array is lexicographically greater than the second array
     * @throws IllegalArgumentException
     *         if {@code aFromIndex > aToIndex} or
     *         if {@code bFromIndex > bToIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code aFromIndex < 0 or aToIndex > a.length} or
     *         if {@code bFromIndex < 0 or bToIndex > b.length}
     * @throws NullPointerException
     *         if either array is null
     * @since 9
     */
    public static int compareUnsigned(int[] a, int aFromIndex, int aToIndex,
                                      int[] b, int bFromIndex, int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int i = ArraysSupport.mismatch(a, aFromIndex,
                                       b, bFromIndex,
                                       Math.min(aLength, bLength));
        if (i >= 0) {
            return Integer.compareUnsigned(a[aFromIndex + i], b[bFromIndex + i]);
        }

        return aLength - bLength;
    }

    // Compare long

    /**
     * Compares two {@code long} arrays lexicographically.
     *
     * <p>If the two arrays share a common prefix then the lexicographic
     * comparison is the result of comparing two elements, as if by
     * {@link Long#compare(long, long)}, at an index within the respective
     * arrays that is the prefix length.
     * Otherwise, one array is a proper prefix of the other and, lexicographic
     * comparison is the result of comparing the two array lengths.
     * (See {@link #mismatch(long[], long[])} for the definition of a common and
     * proper prefix.)
     *
     * <p>A {@code null} array reference is considered lexicographically less
     * than a non-{@code null} array reference.  Two {@code null} array
     * references are considered equal.
     *
     * <p>The comparison is consistent with {@link #equals(long[], long[]) equals},
     * more specifically the following holds for arrays {@code a} and {@code b}:
     * <pre>{@code
     *     Arrays.equals(a, b) == (Arrays.compare(a, b) == 0)
     * }</pre>
     *
     * @apiNote
     * <p>This method behaves as if (for non-{@code null} array references):
     * <pre>{@code
     *     int i = Arrays.mismatch(a, b);
     *     if (i >= 0 && i < Math.min(a.length, b.length))
     *         return Long.compare(a[i], b[i]);
     *     return a.length - b.length;
     * }</pre>
     *
     * @param a the first array to compare
     * @param b the second array to compare
     * @return the value {@code 0} if the first and second array are equal and
     *         contain the same elements in the same order;
     *         a value less than {@code 0} if the first array is
     *         lexicographically less than the second array; and
     *         a value greater than {@code 0} if the first array is
     *         lexicographically greater than the second array
     * @since 9
     */
    public static int compare(long[] a, long[] b) {
        if (a == b)
            return 0;
        if (a == null || b == null)
            return a == null ? -1 : 1;

        int i = ArraysSupport.mismatch(a, b,
                                       Math.min(a.length, b.length));
        if (i >= 0) {
            return Long.compare(a[i], b[i]);
        }

        return a.length - b.length;
    }

    /**
     * Compares two {@code long} arrays lexicographically over the specified
     * ranges.
     *
     * <p>If the two arrays, over the specified ranges, share a common prefix
     * then the lexicographic comparison is the result of comparing two
     * elements, as if by {@link Long#compare(long, long)}, at a relative index
     * within the respective arrays that is the length of the prefix.
     * Otherwise, one array is a proper prefix of the other and, lexicographic
     * comparison is the result of comparing the two range lengths.
     * (See {@link #mismatch(long[], int, int, long[], int, int)} for the
     * definition of a common and proper prefix.)
     *
     * <p>The comparison is consistent with
     * {@link #equals(long[], int, int, long[], int, int) equals}, more
     * specifically the following holds for arrays {@code a} and {@code b} with
     * specified ranges [{@code aFromIndex}, {@code atoIndex}) and
     * [{@code bFromIndex}, {@code btoIndex}) respectively:
     * <pre>{@code
     *     Arrays.equals(a, aFromIndex, aToIndex, b, bFromIndex, bToIndex) ==
     *         (Arrays.compare(a, aFromIndex, aToIndex, b, bFromIndex, bToIndex) == 0)
     * }</pre>
     *
     * @apiNote
     * <p>This method behaves as if:
     * <pre>{@code
     *     int i = Arrays.mismatch(a, aFromIndex, aToIndex,
     *                             b, bFromIndex, bToIndex);
     *     if (i >= 0 && i < Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex))
     *         return Long.compare(a[aFromIndex + i], b[bFromIndex + i]);
     *     return (aToIndex - aFromIndex) - (bToIndex - bFromIndex);
     * }</pre>
     *
     * @param a the first array to compare
     * @param aFromIndex the index (inclusive) of the first element in the
     *                   first array to be compared
     * @param aToIndex the index (exclusive) of the last element in the
     *                 first array to be compared
     * @param b the second array to compare
     * @param bFromIndex the index (inclusive) of the first element in the
     *                   second array to be compared
     * @param bToIndex the index (exclusive) of the last element in the
     *                 second array to be compared
     * @return the value {@code 0} if, over the specified ranges, the first and
     *         second array are equal and contain the same elements in the same
     *         order;
     *         a value less than {@code 0} if, over the specified ranges, the
     *         first array is lexicographically less than the second array; and
     *         a value greater than {@code 0} if, over the specified ranges, the
     *         first array is lexicographically greater than the second array
     * @throws IllegalArgumentException
     *         if {@code aFromIndex > aToIndex} or
     *         if {@code bFromIndex > bToIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code aFromIndex < 0 or aToIndex > a.length} or
     *         if {@code bFromIndex < 0 or bToIndex > b.length}
     * @throws NullPointerException
     *         if either array is {@code null}
     * @since 9
     */
    public static int compare(long[] a, int aFromIndex, int aToIndex,
                              long[] b, int bFromIndex, int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int i = ArraysSupport.mismatch(a, aFromIndex,
                                       b, bFromIndex,
                                       Math.min(aLength, bLength));
        if (i >= 0) {
            return Long.compare(a[aFromIndex + i], b[bFromIndex + i]);
        }

        return aLength - bLength;
    }

    /**
     * Compares two {@code long} arrays lexicographically, numerically treating
     * elements as unsigned.
     *
     * <p>If the two arrays share a common prefix then the lexicographic
     * comparison is the result of comparing two elements, as if by
     * {@link Long#compareUnsigned(long, long)}, at an index within the
     * respective arrays that is the prefix length.
     * Otherwise, one array is a proper prefix of the other and, lexicographic
     * comparison is the result of comparing the two array lengths.
     * (See {@link #mismatch(long[], long[])} for the definition of a common
     * and proper prefix.)
     *
     * <p>A {@code null} array reference is considered lexicographically less
     * than a non-{@code null} array reference.  Two {@code null} array
     * references are considered equal.
     *
     * @apiNote
     * <p>This method behaves as if (for non-{@code null} array references):
     * <pre>{@code
     *     int i = Arrays.mismatch(a, b);
     *     if (i >= 0 && i < Math.min(a.length, b.length))
     *         return Long.compareUnsigned(a[i], b[i]);
     *     return a.length - b.length;
     * }</pre>
     *
     * @param a the first array to compare
     * @param b the second array to compare
     * @return the value {@code 0} if the first and second array are
     *         equal and contain the same elements in the same order;
     *         a value less than {@code 0} if the first array is
     *         lexicographically less than the second array; and
     *         a value greater than {@code 0} if the first array is
     *         lexicographically greater than the second array
     * @since 9
     */
    public static int compareUnsigned(long[] a, long[] b) {
        if (a == b)
            return 0;
        if (a == null || b == null)
            return a == null ? -1 : 1;

        int i = ArraysSupport.mismatch(a, b,
                                       Math.min(a.length, b.length));
        if (i >= 0) {
            return Long.compareUnsigned(a[i], b[i]);
        }

        return a.length - b.length;
    }

    /**
     * Compares two {@code long} arrays lexicographically over the specified
     * ranges, numerically treating elements as unsigned.
     *
     * <p>If the two arrays, over the specified ranges, share a common prefix
     * then the lexicographic comparison is the result of comparing two
     * elements, as if by {@link Long#compareUnsigned(long, long)}, at a
     * relative index within the respective arrays that is the length of the
     * prefix.
     * Otherwise, one array is a proper prefix of the other and, lexicographic
     * comparison is the result of comparing the two range lengths.
     * (See {@link #mismatch(long[], int, int, long[], int, int)} for the
     * definition of a common and proper prefix.)
     *
     * @apiNote
     * <p>This method behaves as if:
     * <pre>{@code
     *     int i = Arrays.mismatch(a, aFromIndex, aToIndex,
     *                             b, bFromIndex, bToIndex);
     *     if (i >= 0 && i < Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex))
     *         return Long.compareUnsigned(a[aFromIndex + i], b[bFromIndex + i]);
     *     return (aToIndex - aFromIndex) - (bToIndex - bFromIndex);
     * }</pre>
     *
     * @param a the first array to compare
     * @param aFromIndex the index (inclusive) of the first element in the
     *                   first array to be compared
     * @param aToIndex the index (exclusive) of the last element in the
     *                 first array to be compared
     * @param b the second array to compare
     * @param bFromIndex the index (inclusive) of the first element in the
     *                   second array to be compared
     * @param bToIndex the index (exclusive) of the last element in the
     *                 second array to be compared
     * @return the value {@code 0} if, over the specified ranges, the first and
     *         second array are equal and contain the same elements in the same
     *         order;
     *         a value less than {@code 0} if, over the specified ranges, the
     *         first array is lexicographically less than the second array; and
     *         a value greater than {@code 0} if, over the specified ranges, the
     *         first array is lexicographically greater than the second array
     * @throws IllegalArgumentException
     *         if {@code aFromIndex > aToIndex} or
     *         if {@code bFromIndex > bToIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code aFromIndex < 0 or aToIndex > a.length} or
     *         if {@code bFromIndex < 0 or bToIndex > b.length}
     * @throws NullPointerException
     *         if either array is null
     * @since 9
     */
    public static int compareUnsigned(long[] a, int aFromIndex, int aToIndex,
                                      long[] b, int bFromIndex, int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int i = ArraysSupport.mismatch(a, aFromIndex,
                                       b, bFromIndex,
                                       Math.min(aLength, bLength));
        if (i >= 0) {
            return Long.compareUnsigned(a[aFromIndex + i], b[bFromIndex + i]);
        }

        return aLength - bLength;
    }

    // Compare float

    /**
     * Compares two {@code float} arrays lexicographically.
     *
     * <p>If the two arrays share a common prefix then the lexicographic
     * comparison is the result of comparing two elements, as if by
     * {@link Float#compare(float, float)}, at an index within the respective
     * arrays that is the prefix length.
     * Otherwise, one array is a proper prefix of the other and, lexicographic
     * comparison is the result of comparing the two array lengths.
     * (See {@link #mismatch(float[], float[])} for the definition of a common
     * and proper prefix.)
     *
     * <p>A {@code null} array reference is considered lexicographically less
     * than a non-{@code null} array reference.  Two {@code null} array
     * references are considered equal.
     *
     * <p>The comparison is consistent with {@link #equals(float[], float[]) equals},
     * more specifically the following holds for arrays {@code a} and {@code b}:
     * <pre>{@code
     *     Arrays.equals(a, b) == (Arrays.compare(a, b) == 0)
     * }</pre>
     *
     * @apiNote
     * <p>This method behaves as if (for non-{@code null} array references):
     * <pre>{@code
     *     int i = Arrays.mismatch(a, b);
     *     if (i >= 0 && i < Math.min(a.length, b.length))
     *         return Float.compare(a[i], b[i]);
     *     return a.length - b.length;
     * }</pre>
     *
     * @param a the first array to compare
     * @param b the second array to compare
     * @return the value {@code 0} if the first and second array are equal and
     *         contain the same elements in the same order;
     *         a value less than {@code 0} if the first array is
     *         lexicographically less than the second array; and
     *         a value greater than {@code 0} if the first array is
     *         lexicographically greater than the second array
     * @since 9
     */
    public static int compare(float[] a, float[] b) {
        if (a == b)
            return 0;
        if (a == null || b == null)
            return a == null ? -1 : 1;

        int i = ArraysSupport.mismatch(a, b,
                                       Math.min(a.length, b.length));
        if (i >= 0) {
            return Float.compare(a[i], b[i]);
        }

        return a.length - b.length;
    }

    /**
     * Compares two {@code float} arrays lexicographically over the specified
     * ranges.
     *
     * <p>If the two arrays, over the specified ranges, share a common prefix
     * then the lexicographic comparison is the result of comparing two
     * elements, as if by {@link Float#compare(float, float)}, at a relative
     * index within the respective arrays that is the length of the prefix.
     * Otherwise, one array is a proper prefix of the other and, lexicographic
     * comparison is the result of comparing the two range lengths.
     * (See {@link #mismatch(float[], int, int, float[], int, int)} for the
     * definition of a common and proper prefix.)
     *
     * <p>The comparison is consistent with
     * {@link #equals(float[], int, int, float[], int, int) equals}, more
     * specifically the following holds for arrays {@code a} and {@code b} with
     * specified ranges [{@code aFromIndex}, {@code atoIndex}) and
     * [{@code bFromIndex}, {@code btoIndex}) respectively:
     * <pre>{@code
     *     Arrays.equals(a, aFromIndex, aToIndex, b, bFromIndex, bToIndex) ==
     *         (Arrays.compare(a, aFromIndex, aToIndex, b, bFromIndex, bToIndex) == 0)
     * }</pre>
     *
     * @apiNote
     * <p>This method behaves as if:
     * <pre>{@code
     *     int i = Arrays.mismatch(a, aFromIndex, aToIndex,
     *                             b, bFromIndex, bToIndex);
     *     if (i >= 0 && i < Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex))
     *         return Float.compare(a[aFromIndex + i], b[bFromIndex + i]);
     *     return (aToIndex - aFromIndex) - (bToIndex - bFromIndex);
     * }</pre>
     *
     * @param a the first array to compare
     * @param aFromIndex the index (inclusive) of the first element in the
     *                   first array to be compared
     * @param aToIndex the index (exclusive) of the last element in the
     *                 first array to be compared
     * @param b the second array to compare
     * @param bFromIndex the index (inclusive) of the first element in the
     *                   second array to be compared
     * @param bToIndex the index (exclusive) of the last element in the
     *                 second array to be compared
     * @return the value {@code 0} if, over the specified ranges, the first and
     *         second array are equal and contain the same elements in the same
     *         order;
     *         a value less than {@code 0} if, over the specified ranges, the
     *         first array is lexicographically less than the second array; and
     *         a value greater than {@code 0} if, over the specified ranges, the
     *         first array is lexicographically greater than the second array
     * @throws IllegalArgumentException
     *         if {@code aFromIndex > aToIndex} or
     *         if {@code bFromIndex > bToIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code aFromIndex < 0 or aToIndex > a.length} or
     *         if {@code bFromIndex < 0 or bToIndex > b.length}
     * @throws NullPointerException
     *         if either array is {@code null}
     * @since 9
     */
    public static int compare(float[] a, int aFromIndex, int aToIndex,
                              float[] b, int bFromIndex, int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int i = ArraysSupport.mismatch(a, aFromIndex,
                                       b, bFromIndex,
                                       Math.min(aLength, bLength));
        if (i >= 0) {
            return Float.compare(a[aFromIndex + i], b[bFromIndex + i]);
        }

        return aLength - bLength;
    }

    // Compare double

    /**
     * Compares two {@code double} arrays lexicographically.
     *
     * <p>If the two arrays share a common prefix then the lexicographic
     * comparison is the result of comparing two elements, as if by
     * {@link Double#compare(double, double)}, at an index within the respective
     * arrays that is the prefix length.
     * Otherwise, one array is a proper prefix of the other and, lexicographic
     * comparison is the result of comparing the two array lengths.
     * (See {@link #mismatch(double[], double[])} for the definition of a common
     * and proper prefix.)
     *
     * <p>A {@code null} array reference is considered lexicographically less
     * than a non-{@code null} array reference.  Two {@code null} array
     * references are considered equal.
     *
     * <p>The comparison is consistent with {@link #equals(double[], double[]) equals},
     * more specifically the following holds for arrays {@code a} and {@code b}:
     * <pre>{@code
     *     Arrays.equals(a, b) == (Arrays.compare(a, b) == 0)
     * }</pre>
     *
     * @apiNote
     * <p>This method behaves as if (for non-{@code null} array references):
     * <pre>{@code
     *     int i = Arrays.mismatch(a, b);
     *     if (i >= 0 && i < Math.min(a.length, b.length))
     *         return Double.compare(a[i], b[i]);
     *     return a.length - b.length;
     * }</pre>
     *
     * @param a the first array to compare
     * @param b the second array to compare
     * @return the value {@code 0} if the first and second array are equal and
     *         contain the same elements in the same order;
     *         a value less than {@code 0} if the first array is
     *         lexicographically less than the second array; and
     *         a value greater than {@code 0} if the first array is
     *         lexicographically greater than the second array
     * @since 9
     */
    public static int compare(double[] a, double[] b) {
        if (a == b)
            return 0;
        if (a == null || b == null)
            return a == null ? -1 : 1;

        int i = ArraysSupport.mismatch(a, b,
                                       Math.min(a.length, b.length));
        if (i >= 0) {
            return Double.compare(a[i], b[i]);
        }

        return a.length - b.length;
    }

    /**
     * Compares two {@code double} arrays lexicographically over the specified
     * ranges.
     *
     * <p>If the two arrays, over the specified ranges, share a common prefix
     * then the lexicographic comparison is the result of comparing two
     * elements, as if by {@link Double#compare(double, double)}, at a relative
     * index within the respective arrays that is the length of the prefix.
     * Otherwise, one array is a proper prefix of the other and, lexicographic
     * comparison is the result of comparing the two range lengths.
     * (See {@link #mismatch(double[], int, int, double[], int, int)} for the
     * definition of a common and proper prefix.)
     *
     * <p>The comparison is consistent with
     * {@link #equals(double[], int, int, double[], int, int) equals}, more
     * specifically the following holds for arrays {@code a} and {@code b} with
     * specified ranges [{@code aFromIndex}, {@code atoIndex}) and
     * [{@code bFromIndex}, {@code btoIndex}) respectively:
     * <pre>{@code
     *     Arrays.equals(a, aFromIndex, aToIndex, b, bFromIndex, bToIndex) ==
     *         (Arrays.compare(a, aFromIndex, aToIndex, b, bFromIndex, bToIndex) == 0)
     * }</pre>
     *
     * @apiNote
     * <p>This method behaves as if:
     * <pre>{@code
     *     int i = Arrays.mismatch(a, aFromIndex, aToIndex,
     *                             b, bFromIndex, bToIndex);
     *     if (i >= 0 && i < Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex))
     *         return Double.compare(a[aFromIndex + i], b[bFromIndex + i]);
     *     return (aToIndex - aFromIndex) - (bToIndex - bFromIndex);
     * }</pre>
     *
     * @param a the first array to compare
     * @param aFromIndex the index (inclusive) of the first element in the
     *                   first array to be compared
     * @param aToIndex the index (exclusive) of the last element in the
     *                 first array to be compared
     * @param b the second array to compare
     * @param bFromIndex the index (inclusive) of the first element in the
     *                   second array to be compared
     * @param bToIndex the index (exclusive) of the last element in the
     *                 second array to be compared
     * @return the value {@code 0} if, over the specified ranges, the first and
     *         second array are equal and contain the same elements in the same
     *         order;
     *         a value less than {@code 0} if, over the specified ranges, the
     *         first array is lexicographically less than the second array; and
     *         a value greater than {@code 0} if, over the specified ranges, the
     *         first array is lexicographically greater than the second array
     * @throws IllegalArgumentException
     *         if {@code aFromIndex > aToIndex} or
     *         if {@code bFromIndex > bToIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code aFromIndex < 0 or aToIndex > a.length} or
     *         if {@code bFromIndex < 0 or bToIndex > b.length}
     * @throws NullPointerException
     *         if either array is {@code null}
     * @since 9
     */
    public static int compare(double[] a, int aFromIndex, int aToIndex,
                              double[] b, int bFromIndex, int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int i = ArraysSupport.mismatch(a, aFromIndex,
                                       b, bFromIndex,
                                       Math.min(aLength, bLength));
        if (i >= 0) {
            return Double.compare(a[aFromIndex + i], b[bFromIndex + i]);
        }

        return aLength - bLength;
    }

    // Compare objects

    /**
     * Compares two {@code Object} arrays, within comparable elements,
     * lexicographically.
     *
     * <p>If the two arrays share a common prefix then the lexicographic
     * comparison is the result of comparing two elements of type {@code T} at
     * an index {@code i} within the respective arrays that is the prefix
     * length, as if by:
     * <pre>{@code
     *     Comparator.nullsFirst(Comparator.<T>naturalOrder()).
     *         compare(a[i], b[i])
     * }</pre>
     * Otherwise, one array is a proper prefix of the other and, lexicographic
     * comparison is the result of comparing the two array lengths.
     * (See {@link #mismatch(Object[], Object[])} for the definition of a common
     * and proper prefix.)
     *
     * <p>A {@code null} array reference is considered lexicographically less
     * than a non-{@code null} array reference. Two {@code null} array
     * references are considered equal.
     * A {@code null} array element is considered lexicographically less than a
     * non-{@code null} array element. Two {@code null} array elements are
     * considered equal.
     *
     * <p>The comparison is consistent with {@link #equals(Object[], Object[]) equals},
     * more specifically the following holds for arrays {@code a} and {@code b}:
     * <pre>{@code
     *     Arrays.equals(a, b) == (Arrays.compare(a, b) == 0)
     * }</pre>
     *
     * @apiNote
     * <p>This method behaves as if (for non-{@code null} array references
     * and elements):
     * <pre>{@code
     *     int i = Arrays.mismatch(a, b);
     *     if (i >= 0 && i < Math.min(a.length, b.length))
     *         return a[i].compareTo(b[i]);
     *     return a.length - b.length;
     * }</pre>
     *
     * @param a the first array to compare
     * @param b the second array to compare
     * @param <T> the type of comparable array elements
     * @return the value {@code 0} if the first and second array are equal and
     *         contain the same elements in the same order;
     *         a value less than {@code 0} if the first array is
     *         lexicographically less than the second array; and
     *         a value greater than {@code 0} if the first array is
     *         lexicographically greater than the second array
     * @since 9
     */
    public static <T extends Comparable<? super T>> int compare(T[] a, T[] b) {
        if (a == b)
            return 0;
        // A null array is less than a non-null array
        if (a == null || b == null)
            return a == null ? -1 : 1;

        int length = Math.min(a.length, b.length);
        for (int i = 0; i < length; i++) {
            T oa = a[i];
            T ob = b[i];
            if (oa != ob) {
                // A null element is less than a non-null element
                if (oa == null || ob == null)
                    return oa == null ? -1 : 1;
                int v = oa.compareTo(ob);
                if (v != 0) {
                    return v;
                }
            }
        }

        return a.length - b.length;
    }

    /**
     * Compares two {@code Object} arrays lexicographically over the specified
     * ranges.
     *
     * <p>If the two arrays, over the specified ranges, share a common prefix
     * then the lexicographic comparison is the result of comparing two
     * elements of type {@code T} at a relative index {@code i} within the
     * respective arrays that is the prefix length, as if by:
     * <pre>{@code
     *     Comparator.nullsFirst(Comparator.<T>naturalOrder()).
     *         compare(a[aFromIndex + i, b[bFromIndex + i])
     * }</pre>
     * Otherwise, one array is a proper prefix of the other and, lexicographic
     * comparison is the result of comparing the two range lengths.
     * (See {@link #mismatch(Object[], int, int, Object[], int, int)} for the
     * definition of a common and proper prefix.)
     *
     * <p>The comparison is consistent with
     * {@link #equals(Object[], int, int, Object[], int, int) equals}, more
     * specifically the following holds for arrays {@code a} and {@code b} with
     * specified ranges [{@code aFromIndex}, {@code atoIndex}) and
     * [{@code bFromIndex}, {@code btoIndex}) respectively:
     * <pre>{@code
     *     Arrays.equals(a, aFromIndex, aToIndex, b, bFromIndex, bToIndex) ==
     *         (Arrays.compare(a, aFromIndex, aToIndex, b, bFromIndex, bToIndex) == 0)
     * }</pre>
     *
     * @apiNote
     * <p>This method behaves as if (for non-{@code null} array elements):
     * <pre>{@code
     *     int i = Arrays.mismatch(a, aFromIndex, aToIndex,
     *                             b, bFromIndex, bToIndex);
     *     if (i >= 0 && i < Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex))
     *         return a[aFromIndex + i].compareTo(b[bFromIndex + i]);
     *     return (aToIndex - aFromIndex) - (bToIndex - bFromIndex);
     * }</pre>
     *
     * @param a the first array to compare
     * @param aFromIndex the index (inclusive) of the first element in the
     *                   first array to be compared
     * @param aToIndex the index (exclusive) of the last element in the
     *                 first array to be compared
     * @param b the second array to compare
     * @param bFromIndex the index (inclusive) of the first element in the
     *                   second array to be compared
     * @param bToIndex the index (exclusive) of the last element in the
     *                 second array to be compared
     * @param <T> the type of comparable array elements
     * @return the value {@code 0} if, over the specified ranges, the first and
     *         second array are equal and contain the same elements in the same
     *         order;
     *         a value less than {@code 0} if, over the specified ranges, the
     *         first array is lexicographically less than the second array; and
     *         a value greater than {@code 0} if, over the specified ranges, the
     *         first array is lexicographically greater than the second array
     * @throws IllegalArgumentException
     *         if {@code aFromIndex > aToIndex} or
     *         if {@code bFromIndex > bToIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code aFromIndex < 0 or aToIndex > a.length} or
     *         if {@code bFromIndex < 0 or bToIndex > b.length}
     * @throws NullPointerException
     *         if either array is {@code null}
     * @since 9
     */
    public static <T extends Comparable<? super T>> int compare(
            T[] a, int aFromIndex, int aToIndex,
            T[] b, int bFromIndex, int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            T oa = a[aFromIndex++];
            T ob = b[bFromIndex++];
            if (oa != ob) {
                if (oa == null || ob == null)
                    return oa == null ? -1 : 1;
                int v = oa.compareTo(ob);
                if (v != 0) {
                    return v;
                }
            }
        }

        return aLength - bLength;
    }

    /**
     * Compares two {@code Object} arrays lexicographically using a specified
     * comparator.
     *
     * <p>If the two arrays share a common prefix then the lexicographic
     * comparison is the result of comparing with the specified comparator two
     * elements at an index within the respective arrays that is the prefix
     * length.
     * Otherwise, one array is a proper prefix of the other and, lexicographic
     * comparison is the result of comparing the two array lengths.
     * (See {@link #mismatch(Object[], Object[])} for the definition of a common
     * and proper prefix.)
     *
     * <p>A {@code null} array reference is considered lexicographically less
     * than a non-{@code null} array reference.  Two {@code null} array
     * references are considered equal.
     *
     * @apiNote
     * <p>This method behaves as if (for non-{@code null} array references):
     * <pre>{@code
     *     int i = Arrays.mismatch(a, b, cmp);
     *     if (i >= 0 && i < Math.min(a.length, b.length))
     *         return cmp.compare(a[i], b[i]);
     *     return a.length - b.length;
     * }</pre>
     *
     * @param a the first array to compare
     * @param b the second array to compare
     * @param cmp the comparator to compare array elements
     * @param <T> the type of array elements
     * @return the value {@code 0} if the first and second array are equal and
     *         contain the same elements in the same order;
     *         a value less than {@code 0} if the first array is
     *         lexicographically less than the second array; and
     *         a value greater than {@code 0} if the first array is
     *         lexicographically greater than the second array
     * @throws NullPointerException if the comparator is {@code null}
     * @since 9
     */
    public static <T> int compare(T[] a, T[] b,
                                  Comparator<? super T> cmp) {
        Objects.requireNonNull(cmp);
        if (a == b)
            return 0;
        if (a == null || b == null)
            return a == null ? -1 : 1;

        int length = Math.min(a.length, b.length);
        for (int i = 0; i < length; i++) {
            T oa = a[i];
            T ob = b[i];
            if (oa != ob) {
                // Null-value comparison is deferred to the comparator
                int v = cmp.compare(oa, ob);
                if (v != 0) {
                    return v;
                }
            }
        }

        return a.length - b.length;
    }

    /**
     * Compares two {@code Object} arrays lexicographically over the specified
     * ranges.
     *
     * <p>If the two arrays, over the specified ranges, share a common prefix
     * then the lexicographic comparison is the result of comparing with the
     * specified comparator two elements at a relative index within the
     * respective arrays that is the prefix length.
     * Otherwise, one array is a proper prefix of the other and, lexicographic
     * comparison is the result of comparing the two range lengths.
     * (See {@link #mismatch(Object[], int, int, Object[], int, int)} for the
     * definition of a common and proper prefix.)
     *
     * @apiNote
     * <p>This method behaves as if (for non-{@code null} array elements):
     * <pre>{@code
     *     int i = Arrays.mismatch(a, aFromIndex, aToIndex,
     *                             b, bFromIndex, bToIndex, cmp);
     *     if (i >= 0 && i < Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex))
     *         return cmp.compare(a[aFromIndex + i], b[bFromIndex + i]);
     *     return (aToIndex - aFromIndex) - (bToIndex - bFromIndex);
     * }</pre>
     *
     * @param a the first array to compare
     * @param aFromIndex the index (inclusive) of the first element in the
     *                   first array to be compared
     * @param aToIndex the index (exclusive) of the last element in the
     *                 first array to be compared
     * @param b the second array to compare
     * @param bFromIndex the index (inclusive) of the first element in the
     *                   second array to be compared
     * @param bToIndex the index (exclusive) of the last element in the
     *                 second array to be compared
     * @param cmp the comparator to compare array elements
     * @param <T> the type of array elements
     * @return the value {@code 0} if, over the specified ranges, the first and
     *         second array are equal and contain the same elements in the same
     *         order;
     *         a value less than {@code 0} if, over the specified ranges, the
     *         first array is lexicographically less than the second array; and
     *         a value greater than {@code 0} if, over the specified ranges, the
     *         first array is lexicographically greater than the second array
     * @throws IllegalArgumentException
     *         if {@code aFromIndex > aToIndex} or
     *         if {@code bFromIndex > bToIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code aFromIndex < 0 or aToIndex > a.length} or
     *         if {@code bFromIndex < 0 or bToIndex > b.length}
     * @throws NullPointerException
     *         if either array or the comparator is {@code null}
     * @since 9
     */
    public static <T> int compare(
            T[] a, int aFromIndex, int aToIndex,
            T[] b, int bFromIndex, int bToIndex,
            Comparator<? super T> cmp) {
        Objects.requireNonNull(cmp);
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            T oa = a[aFromIndex++];
            T ob = b[bFromIndex++];
            if (oa != ob) {
                // Null-value comparison is deferred to the comparator
                int v = cmp.compare(oa, ob);
                if (v != 0) {
                    return v;
                }
            }
        }

        return aLength - bLength;
    }


    // Mismatch methods

    // Mismatch boolean

    /**
     * Finds and returns the index of the first mismatch between two
     * {@code boolean} arrays, otherwise return -1 if no mismatch is found.  The
     * index will be in the range of 0 (inclusive) up to the length (inclusive)
     * of the smaller array.
     *
     * <p>If the two arrays share a common prefix then the returned index is the
     * length of the common prefix and it follows that there is a mismatch
     * between the two elements at that index within the respective arrays.
     * If one array is a proper prefix of the other then the returned index is
     * the length of the smaller array and it follows that the index is only
     * valid for the larger array.
     * Otherwise, there is no mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b}, share a common
     * prefix of length {@code pl} if the following expression is true:
     * <pre>{@code
     *     pl >= 0 &&
     *     pl < Math.min(a.length, b.length) &&
     *     Arrays.equals(a, 0, pl, b, 0, pl) &&
     *     a[pl] != b[pl]
     * }</pre>
     * Note that a common prefix length of {@code 0} indicates that the first
     * elements from each array mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b}, share a proper
     * prefix if the following expression is true:
     * <pre>{@code
     *     a.length != b.length &&
     *     Arrays.equals(a, 0, Math.min(a.length, b.length),
     *                   b, 0, Math.min(a.length, b.length))
     * }</pre>
     *
     * @param a the first array to be tested for a mismatch
     * @param b the second array to be tested for a mismatch
     * @return the index of the first mismatch between the two arrays,
     *         otherwise {@code -1}.
     * @throws NullPointerException
     *         if either array is {@code null}
     * @since 9
     */
    public static int mismatch(boolean[] a, boolean[] b) {
        int length = Math.min(a.length, b.length); // Check null array refs
        if (a == b)
            return -1;

        int i = ArraysSupport.mismatch(a, b, length);
        return (i < 0 && a.length != b.length) ? length : i;
    }

    /**
     * Finds and returns the relative index of the first mismatch between two
     * {@code boolean} arrays over the specified ranges, otherwise return -1 if
     * no mismatch is found.  The index will be in the range of 0 (inclusive) up
     * to the length (inclusive) of the smaller range.
     *
     * <p>If the two arrays, over the specified ranges, share a common prefix
     * then the returned relative index is the length of the common prefix and
     * it follows that there is a mismatch between the two elements at that
     * relative index within the respective arrays.
     * If one array is a proper prefix of the other, over the specified ranges,
     * then the returned relative index is the length of the smaller range and
     * it follows that the relative index is only valid for the array with the
     * larger range.
     * Otherwise, there is no mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b} with specified
     * ranges [{@code aFromIndex}, {@code atoIndex}) and
     * [{@code bFromIndex}, {@code btoIndex}) respectively, share a common
     * prefix of length {@code pl} if the following expression is true:
     * <pre>{@code
     *     pl >= 0 &&
     *     pl < Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex) &&
     *     Arrays.equals(a, aFromIndex, aFromIndex + pl, b, bFromIndex, bFromIndex + pl) &&
     *     a[aFromIndex + pl] != b[bFromIndex + pl]
     * }</pre>
     * Note that a common prefix length of {@code 0} indicates that the first
     * elements from each array mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b} with specified
     * ranges [{@code aFromIndex}, {@code atoIndex}) and
     * [{@code bFromIndex}, {@code btoIndex}) respectively, share a proper
     * prefix if the following expression is true:
     * <pre>{@code
     *     (aToIndex - aFromIndex) != (bToIndex - bFromIndex) &&
     *     Arrays.equals(a, 0, Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex),
     *                   b, 0, Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex))
     * }</pre>
     *
     * @param a the first array to be tested for a mismatch
     * @param aFromIndex the index (inclusive) of the first element in the
     *                   first array to be tested
     * @param aToIndex the index (exclusive) of the last element in the
     *                 first array to be tested
     * @param b the second array to be tested for a mismatch
     * @param bFromIndex the index (inclusive) of the first element in the
     *                   second array to be tested
     * @param bToIndex the index (exclusive) of the last element in the
     *                 second array to be tested
     * @return the relative index of the first mismatch between the two arrays
     *         over the specified ranges, otherwise {@code -1}.
     * @throws IllegalArgumentException
     *         if {@code aFromIndex > aToIndex} or
     *         if {@code bFromIndex > bToIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code aFromIndex < 0 or aToIndex > a.length} or
     *         if {@code bFromIndex < 0 or bToIndex > b.length}
     * @throws NullPointerException
     *         if either array is {@code null}
     * @since 9
     */
    public static int mismatch(boolean[] a, int aFromIndex, int aToIndex,
                               boolean[] b, int bFromIndex, int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        int i = ArraysSupport.mismatch(a, aFromIndex,
                                       b, bFromIndex,
                                       length);
        return (i < 0 && aLength != bLength) ? length : i;
    }

    // Mismatch byte

    /**
     * Finds and returns the index of the first mismatch between two {@code byte}
     * arrays, otherwise return -1 if no mismatch is found.  The index will be
     * in the range of 0 (inclusive) up to the length (inclusive) of the smaller
     * array.
     *
     * <p>If the two arrays share a common prefix then the returned index is the
     * length of the common prefix and it follows that there is a mismatch
     * between the two elements at that index within the respective arrays.
     * If one array is a proper prefix of the other then the returned index is
     * the length of the smaller array and it follows that the index is only
     * valid for the larger array.
     * Otherwise, there is no mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b}, share a common
     * prefix of length {@code pl} if the following expression is true:
     * <pre>{@code
     *     pl >= 0 &&
     *     pl < Math.min(a.length, b.length) &&
     *     Arrays.equals(a, 0, pl, b, 0, pl) &&
     *     a[pl] != b[pl]
     * }</pre>
     * Note that a common prefix length of {@code 0} indicates that the first
     * elements from each array mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b}, share a proper
     * prefix if the following expression is true:
     * <pre>{@code
     *     a.length != b.length &&
     *     Arrays.equals(a, 0, Math.min(a.length, b.length),
     *                   b, 0, Math.min(a.length, b.length))
     * }</pre>
     *
     * @param a the first array to be tested for a mismatch
     * @param b the second array to be tested for a mismatch
     * @return the index of the first mismatch between the two arrays,
     *         otherwise {@code -1}.
     * @throws NullPointerException
     *         if either array is {@code null}
     * @since 9
     */
    public static int mismatch(byte[] a, byte[] b) {
        int length = Math.min(a.length, b.length); // Check null array refs
        if (a == b)
            return -1;

        int i = ArraysSupport.mismatch(a, b, length);
        return (i < 0 && a.length != b.length) ? length : i;
    }

    /**
     * Finds and returns the relative index of the first mismatch between two
     * {@code byte} arrays over the specified ranges, otherwise return -1 if no
     * mismatch is found.  The index will be in the range of 0 (inclusive) up to
     * the length (inclusive) of the smaller range.
     *
     * <p>If the two arrays, over the specified ranges, share a common prefix
     * then the returned relative index is the length of the common prefix and
     * it follows that there is a mismatch between the two elements at that
     * relative index within the respective arrays.
     * If one array is a proper prefix of the other, over the specified ranges,
     * then the returned relative index is the length of the smaller range and
     * it follows that the relative index is only valid for the array with the
     * larger range.
     * Otherwise, there is no mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b} with specified
     * ranges [{@code aFromIndex}, {@code atoIndex}) and
     * [{@code bFromIndex}, {@code btoIndex}) respectively, share a common
     * prefix of length {@code pl} if the following expression is true:
     * <pre>{@code
     *     pl >= 0 &&
     *     pl < Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex) &&
     *     Arrays.equals(a, aFromIndex, aFromIndex + pl, b, bFromIndex, bFromIndex + pl) &&
     *     a[aFromIndex + pl] != b[bFromIndex + pl]
     * }</pre>
     * Note that a common prefix length of {@code 0} indicates that the first
     * elements from each array mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b} with specified
     * ranges [{@code aFromIndex}, {@code atoIndex}) and
     * [{@code bFromIndex}, {@code btoIndex}) respectively, share a proper
     * prefix if the following expression is true:
     * <pre>{@code
     *     (aToIndex - aFromIndex) != (bToIndex - bFromIndex) &&
     *     Arrays.equals(a, 0, Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex),
     *                   b, 0, Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex))
     * }</pre>
     *
     * @param a the first array to be tested for a mismatch
     * @param aFromIndex the index (inclusive) of the first element in the
     *                   first array to be tested
     * @param aToIndex the index (exclusive) of the last element in the
     *                 first array to be tested
     * @param b the second array to be tested for a mismatch
     * @param bFromIndex the index (inclusive) of the first element in the
     *                   second array to be tested
     * @param bToIndex the index (exclusive) of the last element in the
     *                 second array to be tested
     * @return the relative index of the first mismatch between the two arrays
     *         over the specified ranges, otherwise {@code -1}.
     * @throws IllegalArgumentException
     *         if {@code aFromIndex > aToIndex} or
     *         if {@code bFromIndex > bToIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code aFromIndex < 0 or aToIndex > a.length} or
     *         if {@code bFromIndex < 0 or bToIndex > b.length}
     * @throws NullPointerException
     *         if either array is {@code null}
     * @since 9
     */
    public static int mismatch(byte[] a, int aFromIndex, int aToIndex,
                               byte[] b, int bFromIndex, int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        int i = ArraysSupport.mismatch(a, aFromIndex,
                                       b, bFromIndex,
                                       length);
        return (i < 0 && aLength != bLength) ? length : i;
    }

    // Mismatch char

    /**
     * Finds and returns the index of the first mismatch between two {@code char}
     * arrays, otherwise return -1 if no mismatch is found.  The index will be
     * in the range of 0 (inclusive) up to the length (inclusive) of the smaller
     * array.
     *
     * <p>If the two arrays share a common prefix then the returned index is the
     * length of the common prefix and it follows that there is a mismatch
     * between the two elements at that index within the respective arrays.
     * If one array is a proper prefix of the other then the returned index is
     * the length of the smaller array and it follows that the index is only
     * valid for the larger array.
     * Otherwise, there is no mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b}, share a common
     * prefix of length {@code pl} if the following expression is true:
     * <pre>{@code
     *     pl >= 0 &&
     *     pl < Math.min(a.length, b.length) &&
     *     Arrays.equals(a, 0, pl, b, 0, pl) &&
     *     a[pl] != b[pl]
     * }</pre>
     * Note that a common prefix length of {@code 0} indicates that the first
     * elements from each array mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b}, share a proper
     * prefix if the following expression is true:
     * <pre>{@code
     *     a.length != b.length &&
     *     Arrays.equals(a, 0, Math.min(a.length, b.length),
     *                   b, 0, Math.min(a.length, b.length))
     * }</pre>
     *
     * @param a the first array to be tested for a mismatch
     * @param b the second array to be tested for a mismatch
     * @return the index of the first mismatch between the two arrays,
     *         otherwise {@code -1}.
     * @throws NullPointerException
     *         if either array is {@code null}
     * @since 9
     */
    public static int mismatch(char[] a, char[] b) {
        int length = Math.min(a.length, b.length); // Check null array refs
        if (a == b)
            return -1;

        int i = ArraysSupport.mismatch(a, b, length);
        return (i < 0 && a.length != b.length) ? length : i;
    }

    /**
     * Finds and returns the relative index of the first mismatch between two
     * {@code char} arrays over the specified ranges, otherwise return -1 if no
     * mismatch is found.  The index will be in the range of 0 (inclusive) up to
     * the length (inclusive) of the smaller range.
     *
     * <p>If the two arrays, over the specified ranges, share a common prefix
     * then the returned relative index is the length of the common prefix and
     * it follows that there is a mismatch between the two elements at that
     * relative index within the respective arrays.
     * If one array is a proper prefix of the other, over the specified ranges,
     * then the returned relative index is the length of the smaller range and
     * it follows that the relative index is only valid for the array with the
     * larger range.
     * Otherwise, there is no mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b} with specified
     * ranges [{@code aFromIndex}, {@code atoIndex}) and
     * [{@code bFromIndex}, {@code btoIndex}) respectively, share a common
     * prefix of length {@code pl} if the following expression is true:
     * <pre>{@code
     *     pl >= 0 &&
     *     pl < Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex) &&
     *     Arrays.equals(a, aFromIndex, aFromIndex + pl, b, bFromIndex, bFromIndex + pl) &&
     *     a[aFromIndex + pl] != b[bFromIndex + pl]
     * }</pre>
     * Note that a common prefix length of {@code 0} indicates that the first
     * elements from each array mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b} with specified
     * ranges [{@code aFromIndex}, {@code atoIndex}) and
     * [{@code bFromIndex}, {@code btoIndex}) respectively, share a proper
     * prefix if the following expression is true:
     * <pre>{@code
     *     (aToIndex - aFromIndex) != (bToIndex - bFromIndex) &&
     *     Arrays.equals(a, 0, Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex),
     *                   b, 0, Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex))
     * }</pre>
     *
     * @param a the first array to be tested for a mismatch
     * @param aFromIndex the index (inclusive) of the first element in the
     *                   first array to be tested
     * @param aToIndex the index (exclusive) of the last element in the
     *                 first array to be tested
     * @param b the second array to be tested for a mismatch
     * @param bFromIndex the index (inclusive) of the first element in the
     *                   second array to be tested
     * @param bToIndex the index (exclusive) of the last element in the
     *                 second array to be tested
     * @return the relative index of the first mismatch between the two arrays
     *         over the specified ranges, otherwise {@code -1}.
     * @throws IllegalArgumentException
     *         if {@code aFromIndex > aToIndex} or
     *         if {@code bFromIndex > bToIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code aFromIndex < 0 or aToIndex > a.length} or
     *         if {@code bFromIndex < 0 or bToIndex > b.length}
     * @throws NullPointerException
     *         if either array is {@code null}
     * @since 9
     */
    public static int mismatch(char[] a, int aFromIndex, int aToIndex,
                               char[] b, int bFromIndex, int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        int i = ArraysSupport.mismatch(a, aFromIndex,
                                       b, bFromIndex,
                                       length);
        return (i < 0 && aLength != bLength) ? length : i;
    }

    // Mismatch short

    /**
     * Finds and returns the index of the first mismatch between two {@code short}
     * arrays, otherwise return -1 if no mismatch is found.  The index will be
     * in the range of 0 (inclusive) up to the length (inclusive) of the smaller
     * array.
     *
     * <p>If the two arrays share a common prefix then the returned index is the
     * length of the common prefix and it follows that there is a mismatch
     * between the two elements at that index within the respective arrays.
     * If one array is a proper prefix of the other then the returned index is
     * the length of the smaller array and it follows that the index is only
     * valid for the larger array.
     * Otherwise, there is no mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b}, share a common
     * prefix of length {@code pl} if the following expression is true:
     * <pre>{@code
     *     pl >= 0 &&
     *     pl < Math.min(a.length, b.length) &&
     *     Arrays.equals(a, 0, pl, b, 0, pl) &&
     *     a[pl] != b[pl]
     * }</pre>
     * Note that a common prefix length of {@code 0} indicates that the first
     * elements from each array mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b}, share a proper
     * prefix if the following expression is true:
     * <pre>{@code
     *     a.length != b.length &&
     *     Arrays.equals(a, 0, Math.min(a.length, b.length),
     *                   b, 0, Math.min(a.length, b.length))
     * }</pre>
     *
     * @param a the first array to be tested for a mismatch
     * @param b the second array to be tested for a mismatch
     * @return the index of the first mismatch between the two arrays,
     *         otherwise {@code -1}.
     * @throws NullPointerException
     *         if either array is {@code null}
     * @since 9
     */
    public static int mismatch(short[] a, short[] b) {
        int length = Math.min(a.length, b.length); // Check null array refs
        if (a == b)
            return -1;

        int i = ArraysSupport.mismatch(a, b, length);
        return (i < 0 && a.length != b.length) ? length : i;
    }

    /**
     * Finds and returns the relative index of the first mismatch between two
     * {@code short} arrays over the specified ranges, otherwise return -1 if no
     * mismatch is found.  The index will be in the range of 0 (inclusive) up to
     * the length (inclusive) of the smaller range.
     *
     * <p>If the two arrays, over the specified ranges, share a common prefix
     * then the returned relative index is the length of the common prefix and
     * it follows that there is a mismatch between the two elements at that
     * relative index within the respective arrays.
     * If one array is a proper prefix of the other, over the specified ranges,
     * then the returned relative index is the length of the smaller range and
     * it follows that the relative index is only valid for the array with the
     * larger range.
     * Otherwise, there is no mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b} with specified
     * ranges [{@code aFromIndex}, {@code atoIndex}) and
     * [{@code bFromIndex}, {@code btoIndex}) respectively, share a common
     * prefix of length {@code pl} if the following expression is true:
     * <pre>{@code
     *     pl >= 0 &&
     *     pl < Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex) &&
     *     Arrays.equals(a, aFromIndex, aFromIndex + pl, b, bFromIndex, bFromIndex + pl) &&
     *     a[aFromIndex + pl] != b[bFromIndex + pl]
     * }</pre>
     * Note that a common prefix length of {@code 0} indicates that the first
     * elements from each array mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b} with specified
     * ranges [{@code aFromIndex}, {@code atoIndex}) and
     * [{@code bFromIndex}, {@code btoIndex}) respectively, share a proper
     * prefix if the following expression is true:
     * <pre>{@code
     *     (aToIndex - aFromIndex) != (bToIndex - bFromIndex) &&
     *     Arrays.equals(a, 0, Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex),
     *                   b, 0, Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex))
     * }</pre>
     *
     * @param a the first array to be tested for a mismatch
     * @param aFromIndex the index (inclusive) of the first element in the
     *                   first array to be tested
     * @param aToIndex the index (exclusive) of the last element in the
     *                 first array to be tested
     * @param b the second array to be tested for a mismatch
     * @param bFromIndex the index (inclusive) of the first element in the
     *                   second array to be tested
     * @param bToIndex the index (exclusive) of the last element in the
     *                 second array to be tested
     * @return the relative index of the first mismatch between the two arrays
     *         over the specified ranges, otherwise {@code -1}.
     * @throws IllegalArgumentException
     *         if {@code aFromIndex > aToIndex} or
     *         if {@code bFromIndex > bToIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code aFromIndex < 0 or aToIndex > a.length} or
     *         if {@code bFromIndex < 0 or bToIndex > b.length}
     * @throws NullPointerException
     *         if either array is {@code null}
     * @since 9
     */
    public static int mismatch(short[] a, int aFromIndex, int aToIndex,
                               short[] b, int bFromIndex, int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        int i = ArraysSupport.mismatch(a, aFromIndex,
                                       b, bFromIndex,
                                       length);
        return (i < 0 && aLength != bLength) ? length : i;
    }

    // Mismatch int

    /**
     * Finds and returns the index of the first mismatch between two {@code int}
     * arrays, otherwise return -1 if no mismatch is found.  The index will be
     * in the range of 0 (inclusive) up to the length (inclusive) of the smaller
     * array.
     *
     * <p>If the two arrays share a common prefix then the returned index is the
     * length of the common prefix and it follows that there is a mismatch
     * between the two elements at that index within the respective arrays.
     * If one array is a proper prefix of the other then the returned index is
     * the length of the smaller array and it follows that the index is only
     * valid for the larger array.
     * Otherwise, there is no mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b}, share a common
     * prefix of length {@code pl} if the following expression is true:
     * <pre>{@code
     *     pl >= 0 &&
     *     pl < Math.min(a.length, b.length) &&
     *     Arrays.equals(a, 0, pl, b, 0, pl) &&
     *     a[pl] != b[pl]
     * }</pre>
     * Note that a common prefix length of {@code 0} indicates that the first
     * elements from each array mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b}, share a proper
     * prefix if the following expression is true:
     * <pre>{@code
     *     a.length != b.length &&
     *     Arrays.equals(a, 0, Math.min(a.length, b.length),
     *                   b, 0, Math.min(a.length, b.length))
     * }</pre>
     *
     * @param a the first array to be tested for a mismatch
     * @param b the second array to be tested for a mismatch
     * @return the index of the first mismatch between the two arrays,
     *         otherwise {@code -1}.
     * @throws NullPointerException
     *         if either array is {@code null}
     * @since 9
     */
    public static int mismatch(int[] a, int[] b) {
        int length = Math.min(a.length, b.length); // Check null array refs
        if (a == b)
            return -1;

        int i = ArraysSupport.mismatch(a, b, length);
        return (i < 0 && a.length != b.length) ? length : i;
    }

    /**
     * Finds and returns the relative index of the first mismatch between two
     * {@code int} arrays over the specified ranges, otherwise return -1 if no
     * mismatch is found.  The index will be in the range of 0 (inclusive) up to
     * the length (inclusive) of the smaller range.
     *
     * <p>If the two arrays, over the specified ranges, share a common prefix
     * then the returned relative index is the length of the common prefix and
     * it follows that there is a mismatch between the two elements at that
     * relative index within the respective arrays.
     * If one array is a proper prefix of the other, over the specified ranges,
     * then the returned relative index is the length of the smaller range and
     * it follows that the relative index is only valid for the array with the
     * larger range.
     * Otherwise, there is no mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b} with specified
     * ranges [{@code aFromIndex}, {@code atoIndex}) and
     * [{@code bFromIndex}, {@code btoIndex}) respectively, share a common
     * prefix of length {@code pl} if the following expression is true:
     * <pre>{@code
     *     pl >= 0 &&
     *     pl < Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex) &&
     *     Arrays.equals(a, aFromIndex, aFromIndex + pl, b, bFromIndex, bFromIndex + pl) &&
     *     a[aFromIndex + pl] != b[bFromIndex + pl]
     * }</pre>
     * Note that a common prefix length of {@code 0} indicates that the first
     * elements from each array mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b} with specified
     * ranges [{@code aFromIndex}, {@code atoIndex}) and
     * [{@code bFromIndex}, {@code btoIndex}) respectively, share a proper
     * prefix if the following expression is true:
     * <pre>{@code
     *     (aToIndex - aFromIndex) != (bToIndex - bFromIndex) &&
     *     Arrays.equals(a, 0, Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex),
     *                   b, 0, Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex))
     * }</pre>
     *
     * @param a the first array to be tested for a mismatch
     * @param aFromIndex the index (inclusive) of the first element in the
     *                   first array to be tested
     * @param aToIndex the index (exclusive) of the last element in the
     *                 first array to be tested
     * @param b the second array to be tested for a mismatch
     * @param bFromIndex the index (inclusive) of the first element in the
     *                   second array to be tested
     * @param bToIndex the index (exclusive) of the last element in the
     *                 second array to be tested
     * @return the relative index of the first mismatch between the two arrays
     *         over the specified ranges, otherwise {@code -1}.
     * @throws IllegalArgumentException
     *         if {@code aFromIndex > aToIndex} or
     *         if {@code bFromIndex > bToIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code aFromIndex < 0 or aToIndex > a.length} or
     *         if {@code bFromIndex < 0 or bToIndex > b.length}
     * @throws NullPointerException
     *         if either array is {@code null}
     * @since 9
     */
    public static int mismatch(int[] a, int aFromIndex, int aToIndex,
                               int[] b, int bFromIndex, int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        int i = ArraysSupport.mismatch(a, aFromIndex,
                                       b, bFromIndex,
                                       length);
        return (i < 0 && aLength != bLength) ? length : i;
    }

    // Mismatch long

    /**
     * Finds and returns the index of the first mismatch between two {@code long}
     * arrays, otherwise return -1 if no mismatch is found.  The index will be
     * in the range of 0 (inclusive) up to the length (inclusive) of the smaller
     * array.
     *
     * <p>If the two arrays share a common prefix then the returned index is the
     * length of the common prefix and it follows that there is a mismatch
     * between the two elements at that index within the respective arrays.
     * If one array is a proper prefix of the other then the returned index is
     * the length of the smaller array and it follows that the index is only
     * valid for the larger array.
     * Otherwise, there is no mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b}, share a common
     * prefix of length {@code pl} if the following expression is true:
     * <pre>{@code
     *     pl >= 0 &&
     *     pl < Math.min(a.length, b.length) &&
     *     Arrays.equals(a, 0, pl, b, 0, pl) &&
     *     a[pl] != b[pl]
     * }</pre>
     * Note that a common prefix length of {@code 0} indicates that the first
     * elements from each array mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b}, share a proper
     * prefix if the following expression is true:
     * <pre>{@code
     *     a.length != b.length &&
     *     Arrays.equals(a, 0, Math.min(a.length, b.length),
     *                   b, 0, Math.min(a.length, b.length))
     * }</pre>
     *
     * @param a the first array to be tested for a mismatch
     * @param b the second array to be tested for a mismatch
     * @return the index of the first mismatch between the two arrays,
     *         otherwise {@code -1}.
     * @throws NullPointerException
     *         if either array is {@code null}
     * @since 9
     */
    public static int mismatch(long[] a, long[] b) {
        int length = Math.min(a.length, b.length); // Check null array refs
        if (a == b)
            return -1;

        int i = ArraysSupport.mismatch(a, b, length);
        return (i < 0 && a.length != b.length) ? length : i;
    }

    /**
     * Finds and returns the relative index of the first mismatch between two
     * {@code long} arrays over the specified ranges, otherwise return -1 if no
     * mismatch is found.  The index will be in the range of 0 (inclusive) up to
     * the length (inclusive) of the smaller range.
     *
     * <p>If the two arrays, over the specified ranges, share a common prefix
     * then the returned relative index is the length of the common prefix and
     * it follows that there is a mismatch between the two elements at that
     * relative index within the respective arrays.
     * If one array is a proper prefix of the other, over the specified ranges,
     * then the returned relative index is the length of the smaller range and
     * it follows that the relative index is only valid for the array with the
     * larger range.
     * Otherwise, there is no mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b} with specified
     * ranges [{@code aFromIndex}, {@code atoIndex}) and
     * [{@code bFromIndex}, {@code btoIndex}) respectively, share a common
     * prefix of length {@code pl} if the following expression is true:
     * <pre>{@code
     *     pl >= 0 &&
     *     pl < Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex) &&
     *     Arrays.equals(a, aFromIndex, aFromIndex + pl, b, bFromIndex, bFromIndex + pl) &&
     *     a[aFromIndex + pl] != b[bFromIndex + pl]
     * }</pre>
     * Note that a common prefix length of {@code 0} indicates that the first
     * elements from each array mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b} with specified
     * ranges [{@code aFromIndex}, {@code atoIndex}) and
     * [{@code bFromIndex}, {@code btoIndex}) respectively, share a proper
     * prefix if the following expression is true:
     * <pre>{@code
     *     (aToIndex - aFromIndex) != (bToIndex - bFromIndex) &&
     *     Arrays.equals(a, 0, Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex),
     *                   b, 0, Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex))
     * }</pre>
     *
     * @param a the first array to be tested for a mismatch
     * @param aFromIndex the index (inclusive) of the first element in the
     *                   first array to be tested
     * @param aToIndex the index (exclusive) of the last element in the
     *                 first array to be tested
     * @param b the second array to be tested for a mismatch
     * @param bFromIndex the index (inclusive) of the first element in the
     *                   second array to be tested
     * @param bToIndex the index (exclusive) of the last element in the
     *                 second array to be tested
     * @return the relative index of the first mismatch between the two arrays
     *         over the specified ranges, otherwise {@code -1}.
     * @throws IllegalArgumentException
     *         if {@code aFromIndex > aToIndex} or
     *         if {@code bFromIndex > bToIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code aFromIndex < 0 or aToIndex > a.length} or
     *         if {@code bFromIndex < 0 or bToIndex > b.length}
     * @throws NullPointerException
     *         if either array is {@code null}
     * @since 9
     */
    public static int mismatch(long[] a, int aFromIndex, int aToIndex,
                               long[] b, int bFromIndex, int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        int i = ArraysSupport.mismatch(a, aFromIndex,
                                       b, bFromIndex,
                                       length);
        return (i < 0 && aLength != bLength) ? length : i;
    }

    // Mismatch float

    /**
     * Finds and returns the index of the first mismatch between two {@code float}
     * arrays, otherwise return -1 if no mismatch is found.  The index will be
     * in the range of 0 (inclusive) up to the length (inclusive) of the smaller
     * array.
     *
     * <p>If the two arrays share a common prefix then the returned index is the
     * length of the common prefix and it follows that there is a mismatch
     * between the two elements at that index within the respective arrays.
     * If one array is a proper prefix of the other then the returned index is
     * the length of the smaller array and it follows that the index is only
     * valid for the larger array.
     * Otherwise, there is no mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b}, share a common
     * prefix of length {@code pl} if the following expression is true:
     * <pre>{@code
     *     pl >= 0 &&
     *     pl < Math.min(a.length, b.length) &&
     *     Arrays.equals(a, 0, pl, b, 0, pl) &&
     *     Float.compare(a[pl], b[pl]) != 0
     * }</pre>
     * Note that a common prefix length of {@code 0} indicates that the first
     * elements from each array mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b}, share a proper
     * prefix if the following expression is true:
     * <pre>{@code
     *     a.length != b.length &&
     *     Arrays.equals(a, 0, Math.min(a.length, b.length),
     *                   b, 0, Math.min(a.length, b.length))
     * }</pre>
     *
     * @param a the first array to be tested for a mismatch
     * @param b the second array to be tested for a mismatch
     * @return the index of the first mismatch between the two arrays,
     *         otherwise {@code -1}.
     * @throws NullPointerException
     *         if either array is {@code null}
     * @since 9
     */
    public static int mismatch(float[] a, float[] b) {
        int length = Math.min(a.length, b.length); // Check null array refs
        if (a == b)
            return -1;

        int i = ArraysSupport.mismatch(a, b, length);
        return (i < 0 && a.length != b.length) ? length : i;
    }

    /**
     * Finds and returns the relative index of the first mismatch between two
     * {@code float} arrays over the specified ranges, otherwise return -1 if no
     * mismatch is found.  The index will be in the range of 0 (inclusive) up to
     * the length (inclusive) of the smaller range.
     *
     * <p>If the two arrays, over the specified ranges, share a common prefix
     * then the returned relative index is the length of the common prefix and
     * it follows that there is a mismatch between the two elements at that
     * relative index within the respective arrays.
     * If one array is a proper prefix of the other, over the specified ranges,
     * then the returned relative index is the length of the smaller range and
     * it follows that the relative index is only valid for the array with the
     * larger range.
     * Otherwise, there is no mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b} with specified
     * ranges [{@code aFromIndex}, {@code atoIndex}) and
     * [{@code bFromIndex}, {@code btoIndex}) respectively, share a common
     * prefix of length {@code pl} if the following expression is true:
     * <pre>{@code
     *     pl >= 0 &&
     *     pl < Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex) &&
     *     Arrays.equals(a, aFromIndex, aFromIndex + pl, b, bFromIndex, bFromIndex + pl) &&
     *     Float.compare(a[aFromIndex + pl], b[bFromIndex + pl]) != 0
     * }</pre>
     * Note that a common prefix length of {@code 0} indicates that the first
     * elements from each array mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b} with specified
     * ranges [{@code aFromIndex}, {@code atoIndex}) and
     * [{@code bFromIndex}, {@code btoIndex}) respectively, share a proper
     * prefix if the following expression is true:
     * <pre>{@code
     *     (aToIndex - aFromIndex) != (bToIndex - bFromIndex) &&
     *     Arrays.equals(a, 0, Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex),
     *                   b, 0, Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex))
     * }</pre>
     *
     * @param a the first array to be tested for a mismatch
     * @param aFromIndex the index (inclusive) of the first element in the
     *                   first array to be tested
     * @param aToIndex the index (exclusive) of the last element in the
     *                 first array to be tested
     * @param b the second array to be tested for a mismatch
     * @param bFromIndex the index (inclusive) of the first element in the
     *                   second array to be tested
     * @param bToIndex the index (exclusive) of the last element in the
     *                 second array to be tested
     * @return the relative index of the first mismatch between the two arrays
     *         over the specified ranges, otherwise {@code -1}.
     * @throws IllegalArgumentException
     *         if {@code aFromIndex > aToIndex} or
     *         if {@code bFromIndex > bToIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code aFromIndex < 0 or aToIndex > a.length} or
     *         if {@code bFromIndex < 0 or bToIndex > b.length}
     * @throws NullPointerException
     *         if either array is {@code null}
     * @since 9
     */
    public static int mismatch(float[] a, int aFromIndex, int aToIndex,
                               float[] b, int bFromIndex, int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        int i = ArraysSupport.mismatch(a, aFromIndex,
                                       b, bFromIndex,
                                       length);
        return (i < 0 && aLength != bLength) ? length : i;
    }

    // Mismatch double

    /**
     * Finds and returns the index of the first mismatch between two
     * {@code double} arrays, otherwise return -1 if no mismatch is found.  The
     * index will be in the range of 0 (inclusive) up to the length (inclusive)
     * of the smaller array.
     *
     * <p>If the two arrays share a common prefix then the returned index is the
     * length of the common prefix and it follows that there is a mismatch
     * between the two elements at that index within the respective arrays.
     * If one array is a proper prefix of the other then the returned index is
     * the length of the smaller array and it follows that the index is only
     * valid for the larger array.
     * Otherwise, there is no mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b}, share a common
     * prefix of length {@code pl} if the following expression is true:
     * <pre>{@code
     *     pl >= 0 &&
     *     pl < Math.min(a.length, b.length) &&
     *     Arrays.equals(a, 0, pl, b, 0, pl) &&
     *     Double.compare(a[pl], b[pl]) != 0
     * }</pre>
     * Note that a common prefix length of {@code 0} indicates that the first
     * elements from each array mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b}, share a proper
     * prefix if the following expression is true:
     * <pre>{@code
     *     a.length != b.length &&
     *     Arrays.equals(a, 0, Math.min(a.length, b.length),
     *                   b, 0, Math.min(a.length, b.length))
     * }</pre>
     *
     * @param a the first array to be tested for a mismatch
     * @param b the second array to be tested for a mismatch
     * @return the index of the first mismatch between the two arrays,
     *         otherwise {@code -1}.
     * @throws NullPointerException
     *         if either array is {@code null}
     * @since 9
     */
    public static int mismatch(double[] a, double[] b) {
        int length = Math.min(a.length, b.length); // Check null array refs
        if (a == b)
            return -1;

        int i = ArraysSupport.mismatch(a, b, length);
        return (i < 0 && a.length != b.length) ? length : i;
    }

    /**
     * Finds and returns the relative index of the first mismatch between two
     * {@code double} arrays over the specified ranges, otherwise return -1 if
     * no mismatch is found.  The index will be in the range of 0 (inclusive) up
     * to the length (inclusive) of the smaller range.
     *
     * <p>If the two arrays, over the specified ranges, share a common prefix
     * then the returned relative index is the length of the common prefix and
     * it follows that there is a mismatch between the two elements at that
     * relative index within the respective arrays.
     * If one array is a proper prefix of the other, over the specified ranges,
     * then the returned relative index is the length of the smaller range and
     * it follows that the relative index is only valid for the array with the
     * larger range.
     * Otherwise, there is no mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b} with specified
     * ranges [{@code aFromIndex}, {@code atoIndex}) and
     * [{@code bFromIndex}, {@code btoIndex}) respectively, share a common
     * prefix of length {@code pl} if the following expression is true:
     * <pre>{@code
     *     pl >= 0 &&
     *     pl < Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex) &&
     *     Arrays.equals(a, aFromIndex, aFromIndex + pl, b, bFromIndex, bFromIndex + pl) &&
     *     Double.compare(a[aFromIndex + pl], b[bFromIndex + pl]) != 0
     * }</pre>
     * Note that a common prefix length of {@code 0} indicates that the first
     * elements from each array mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b} with specified
     * ranges [{@code aFromIndex}, {@code atoIndex}) and
     * [{@code bFromIndex}, {@code btoIndex}) respectively, share a proper
     * prefix if the following expression is true:
     * <pre>{@code
     *     (aToIndex - aFromIndex) != (bToIndex - bFromIndex) &&
     *     Arrays.equals(a, 0, Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex),
     *                   b, 0, Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex))
     * }</pre>
     *
     * @param a the first array to be tested for a mismatch
     * @param aFromIndex the index (inclusive) of the first element in the
     *                   first array to be tested
     * @param aToIndex the index (exclusive) of the last element in the
     *                 first array to be tested
     * @param b the second array to be tested for a mismatch
     * @param bFromIndex the index (inclusive) of the first element in the
     *                   second array to be tested
     * @param bToIndex the index (exclusive) of the last element in the
     *                 second array to be tested
     * @return the relative index of the first mismatch between the two arrays
     *         over the specified ranges, otherwise {@code -1}.
     * @throws IllegalArgumentException
     *         if {@code aFromIndex > aToIndex} or
     *         if {@code bFromIndex > bToIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code aFromIndex < 0 or aToIndex > a.length} or
     *         if {@code bFromIndex < 0 or bToIndex > b.length}
     * @throws NullPointerException
     *         if either array is {@code null}
     * @since 9
     */
    public static int mismatch(double[] a, int aFromIndex, int aToIndex,
                               double[] b, int bFromIndex, int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        int i = ArraysSupport.mismatch(a, aFromIndex,
                                       b, bFromIndex,
                                       length);
        return (i < 0 && aLength != bLength) ? length : i;
    }

    // Mismatch objects

    /**
     * Finds and returns the index of the first mismatch between two
     * {@code Object} arrays, otherwise return -1 if no mismatch is found.  The
     * index will be in the range of 0 (inclusive) up to the length (inclusive)
     * of the smaller array.
     *
     * <p>If the two arrays share a common prefix then the returned index is the
     * length of the common prefix and it follows that there is a mismatch
     * between the two elements at that index within the respective arrays.
     * If one array is a proper prefix of the other then the returned index is
     * the length of the smaller array and it follows that the index is only
     * valid for the larger array.
     * Otherwise, there is no mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b}, share a common
     * prefix of length {@code pl} if the following expression is true:
     * <pre>{@code
     *     pl >= 0 &&
     *     pl < Math.min(a.length, b.length) &&
     *     Arrays.equals(a, 0, pl, b, 0, pl) &&
     *     !Objects.equals(a[pl], b[pl])
     * }</pre>
     * Note that a common prefix length of {@code 0} indicates that the first
     * elements from each array mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b}, share a proper
     * prefix if the following expression is true:
     * <pre>{@code
     *     a.length != b.length &&
     *     Arrays.equals(a, 0, Math.min(a.length, b.length),
     *                   b, 0, Math.min(a.length, b.length))
     * }</pre>
     *
     * @param a the first array to be tested for a mismatch
     * @param b the second array to be tested for a mismatch
     * @return the index of the first mismatch between the two arrays,
     *         otherwise {@code -1}.
     * @throws NullPointerException
     *         if either array is {@code null}
     * @since 9
     */
    public static int mismatch(Object[] a, Object[] b) {
        int length = Math.min(a.length, b.length); // Check null array refs
        if (a == b)
            return -1;

        for (int i = 0; i < length; i++) {
            if (!Objects.equals(a[i], b[i]))
                return i;
        }

        return a.length != b.length ? length : -1;
    }

    /**
     * Finds and returns the relative index of the first mismatch between two
     * {@code Object} arrays over the specified ranges, otherwise return -1 if
     * no mismatch is found.  The index will be in the range of 0 (inclusive) up
     * to the length (inclusive) of the smaller range.
     *
     * <p>If the two arrays, over the specified ranges, share a common prefix
     * then the returned relative index is the length of the common prefix and
     * it follows that there is a mismatch between the two elements at that
     * relative index within the respective arrays.
     * If one array is a proper prefix of the other, over the specified ranges,
     * then the returned relative index is the length of the smaller range and
     * it follows that the relative index is only valid for the array with the
     * larger range.
     * Otherwise, there is no mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b} with specified
     * ranges [{@code aFromIndex}, {@code atoIndex}) and
     * [{@code bFromIndex}, {@code btoIndex}) respectively, share a common
     * prefix of length {@code pl} if the following expression is true:
     * <pre>{@code
     *     pl >= 0 &&
     *     pl < Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex) &&
     *     Arrays.equals(a, aFromIndex, aFromIndex + pl, b, bFromIndex, bFromIndex + pl) &&
     *     !Objects.equals(a[aFromIndex + pl], b[bFromIndex + pl])
     * }</pre>
     * Note that a common prefix length of {@code 0} indicates that the first
     * elements from each array mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b} with specified
     * ranges [{@code aFromIndex}, {@code atoIndex}) and
     * [{@code bFromIndex}, {@code btoIndex}) respectively, share a proper
     * prefix if the following expression is true:
     * <pre>{@code
     *     (aToIndex - aFromIndex) != (bToIndex - bFromIndex) &&
     *     Arrays.equals(a, 0, Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex),
     *                   b, 0, Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex))
     * }</pre>
     *
     * @param a the first array to be tested for a mismatch
     * @param aFromIndex the index (inclusive) of the first element in the
     *                   first array to be tested
     * @param aToIndex the index (exclusive) of the last element in the
     *                 first array to be tested
     * @param b the second array to be tested for a mismatch
     * @param bFromIndex the index (inclusive) of the first element in the
     *                   second array to be tested
     * @param bToIndex the index (exclusive) of the last element in the
     *                 second array to be tested
     * @return the relative index of the first mismatch between the two arrays
     *         over the specified ranges, otherwise {@code -1}.
     * @throws IllegalArgumentException
     *         if {@code aFromIndex > aToIndex} or
     *         if {@code bFromIndex > bToIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code aFromIndex < 0 or aToIndex > a.length} or
     *         if {@code bFromIndex < 0 or bToIndex > b.length}
     * @throws NullPointerException
     *         if either array is {@code null}
     * @since 9
     */
    public static int mismatch(
            Object[] a, int aFromIndex, int aToIndex,
            Object[] b, int bFromIndex, int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            if (!Objects.equals(a[aFromIndex++], b[bFromIndex++]))
                return i;
        }

        return aLength != bLength ? length : -1;
    }

    /**
     * Finds and returns the index of the first mismatch between two
     * {@code Object} arrays, otherwise return -1 if no mismatch is found.
     * The index will be in the range of 0 (inclusive) up to the length
     * (inclusive) of the smaller array.
     *
     * <p>The specified comparator is used to determine if two array elements
     * from the each array are not equal.
     *
     * <p>If the two arrays share a common prefix then the returned index is the
     * length of the common prefix and it follows that there is a mismatch
     * between the two elements at that index within the respective arrays.
     * If one array is a proper prefix of the other then the returned index is
     * the length of the smaller array and it follows that the index is only
     * valid for the larger array.
     * Otherwise, there is no mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b}, share a common
     * prefix of length {@code pl} if the following expression is true:
     * <pre>{@code
     *     pl >= 0 &&
     *     pl < Math.min(a.length, b.length) &&
     *     Arrays.equals(a, 0, pl, b, 0, pl, cmp)
     *     cmp.compare(a[pl], b[pl]) != 0
     * }</pre>
     * Note that a common prefix length of {@code 0} indicates that the first
     * elements from each array mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b}, share a proper
     * prefix if the following expression is true:
     * <pre>{@code
     *     a.length != b.length &&
     *     Arrays.equals(a, 0, Math.min(a.length, b.length),
     *                   b, 0, Math.min(a.length, b.length),
     *                   cmp)
     * }</pre>
     *
     * @param a the first array to be tested for a mismatch
     * @param b the second array to be tested for a mismatch
     * @param cmp the comparator to compare array elements
     * @param <T> the type of array elements
     * @return the index of the first mismatch between the two arrays,
     *         otherwise {@code -1}.
     * @throws NullPointerException
     *         if either array or the comparator is {@code null}
     * @since 9
     */
    public static <T> int mismatch(T[] a, T[] b, Comparator<? super T> cmp) {
        Objects.requireNonNull(cmp);
        int length = Math.min(a.length, b.length); // Check null array refs
        if (a == b)
            return -1;

        for (int i = 0; i < length; i++) {
            T oa = a[i];
            T ob = b[i];
            if (oa != ob) {
                // Null-value comparison is deferred to the comparator
                int v = cmp.compare(oa, ob);
                if (v != 0) {
                    return i;
                }
            }
        }

        return a.length != b.length ? length : -1;
    }

    /**
     * Finds and returns the relative index of the first mismatch between two
     * {@code Object} arrays over the specified ranges, otherwise return -1 if
     * no mismatch is found.  The index will be in the range of 0 (inclusive) up
     * to the length (inclusive) of the smaller range.
     *
     * <p>If the two arrays, over the specified ranges, share a common prefix
     * then the returned relative index is the length of the common prefix and
     * it follows that there is a mismatch between the two elements at that
     * relative index within the respective arrays.
     * If one array is a proper prefix of the other, over the specified ranges,
     * then the returned relative index is the length of the smaller range and
     * it follows that the relative index is only valid for the array with the
     * larger range.
     * Otherwise, there is no mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b} with specified
     * ranges [{@code aFromIndex}, {@code atoIndex}) and
     * [{@code bFromIndex}, {@code btoIndex}) respectively, share a common
     * prefix of length {@code pl} if the following expression is true:
     * <pre>{@code
     *     pl >= 0 &&
     *     pl < Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex) &&
     *     Arrays.equals(a, aFromIndex, aFromIndex + pl, b, bFromIndex, bFromIndex + pl, cmp) &&
     *     cmp.compare(a[aFromIndex + pl], b[bFromIndex + pl]) != 0
     * }</pre>
     * Note that a common prefix length of {@code 0} indicates that the first
     * elements from each array mismatch.
     *
     * <p>Two non-{@code null} arrays, {@code a} and {@code b} with specified
     * ranges [{@code aFromIndex}, {@code atoIndex}) and
     * [{@code bFromIndex}, {@code btoIndex}) respectively, share a proper
     * prefix if the following expression is true:
     * <pre>{@code
     *     (aToIndex - aFromIndex) != (bToIndex - bFromIndex) &&
     *     Arrays.equals(a, 0, Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex),
     *                   b, 0, Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex),
     *                   cmp)
     * }</pre>
     *
     * @param a the first array to be tested for a mismatch
     * @param aFromIndex the index (inclusive) of the first element in the
     *                   first array to be tested
     * @param aToIndex the index (exclusive) of the last element in the
     *                 first array to be tested
     * @param b the second array to be tested for a mismatch
     * @param bFromIndex the index (inclusive) of the first element in the
     *                   second array to be tested
     * @param bToIndex the index (exclusive) of the last element in the
     *                 second array to be tested
     * @param cmp the comparator to compare array elements
     * @param <T> the type of array elements
     * @return the relative index of the first mismatch between the two arrays
     *         over the specified ranges, otherwise {@code -1}.
     * @throws IllegalArgumentException
     *         if {@code aFromIndex > aToIndex} or
     *         if {@code bFromIndex > bToIndex}
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code aFromIndex < 0 or aToIndex > a.length} or
     *         if {@code bFromIndex < 0 or bToIndex > b.length}
     * @throws NullPointerException
     *         if either array or the comparator is {@code null}
     * @since 9
     */
    public static <T> int mismatch(
            T[] a, int aFromIndex, int aToIndex,
            T[] b, int bFromIndex, int bToIndex,
            Comparator<? super T> cmp) {
        Objects.requireNonNull(cmp);
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        for (int i = 0; i < length; i++) {
            T oa = a[aFromIndex++];
            T ob = b[bFromIndex++];
            if (oa != ob) {
                // Null-value comparison is deferred to the comparator
                int v = cmp.compare(oa, ob);
                if (v != 0) {
                    return i;
                }
            }
        }

        return aLength != bLength ? length : -1;
    }
}
