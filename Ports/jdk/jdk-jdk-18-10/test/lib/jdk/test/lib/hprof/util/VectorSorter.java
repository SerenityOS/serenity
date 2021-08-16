/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
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


/*
 * The Original Code is HAT. The Initial Developer of the
 * Original Code is Bill Foote, with contributions from others
 * at JavaSoft/Sun.
 */

package jdk.test.lib.hprof.util;
import java.util.*;

/**
 * A singleton utility class that sorts a vector.
 * <p>
 * Use:
 * <pre>
 *
 *  Vector v =   <a vector of, say, String objects>;
 *  VectorSorter.sort(v, new Comparer() {
 *      public int compare(Object lhs, Object rhs) {
 *          return ((String) lhs).compareTo((String) rhs);
 *      }
 *  });
 * </pre>
 *
 * @author      Bill Foote
 */


public class VectorSorter {

    /**
     * Sort the given vector, using c for comparison
    **/
    static public void sort(Vector<Object> v, Comparer c)  {
        quickSort(v, c, 0, v.size()-1);
    }


    /**
     * Sort a vector of strings, using String.compareTo()
    **/
    static public void sortVectorOfStrings(Vector<Object> v) {
        sort(v, new Comparer() {
            public int compare(Object lhs, Object rhs) {
                return ((String) lhs).compareTo((String) rhs);
            }
        });
    }


    static private void swap(Vector<Object> v, int a, int b) {
        Object tmp = v.elementAt(a);
        v.setElementAt(v.elementAt(b), a);
        v.setElementAt(tmp, b);
    }

    //
    // Sorts v between from and to, inclusive.  This is a quick, off-the-top-
    // of-my-head quicksort:  I haven't put any thought into optimizing it.
    // I _did_ put thought into making sure it's safe (it will always
    // terminate).  Worst-case it's O(n^2), but it will usually run in
    // in O(n log n).  It's well-behaved if the list is already sorted,
    // or nearly so.
    //
    static private void quickSort(Vector<Object> v, Comparer c, int from, int to) {
        if (to <= from)
            return;
        int mid = (from + to) / 2;
        if (mid != from)
            swap(v, mid, from);
        Object pivot = v.elementAt(from);
                        // Simple-minded, but reasonable
        int highestBelowPivot = from - 1;
        int low = from+1;
        int high = to;
            // We now move low and high toward eachother, maintaining the
            // invariants:
            //      v[i] <= pivot    for all i < low
            //      v[i] > pivot     for all i > high
            // As long as these invariants hold, and every iteration makes
            // progress, we are safe.
        while (low <= high) {
            int cmp = c.compare(v.elementAt(low), pivot);
            if (cmp <= 0) {    // v[low] <= pivot
                if (cmp < 0) {
                    highestBelowPivot = low;
                }
                low++;
            } else {
                int c2;
                for (;;) {
                    c2 = c.compare(v.elementAt(high), pivot);
                        // v[high] > pivot:
                    if (c2 > 0) {
                        high--;
                        if (low > high) {
                            break;
                        }
                    } else {
                        break;
                    }
                }
                // At this point, low is never == high
                if (low <= high) {
                    swap(v, low, high);
                    if (c2 < 0) {
                        highestBelowPivot = low;
                    }
                    low++;
                    high--;
                }
            }
        }
        // Now we just need to sort from from..highestBelowPivot
        // and from high+1..to
        if (highestBelowPivot > from) {
            // pivot == pivot, so ensure algorithm terminates
            swap(v, from, highestBelowPivot);
            quickSort(v, c, from, highestBelowPivot-1);
        }
        quickSort(v, c, high+1, to);
    }
}
