/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

#ifndef SHARE_UTILITIES_QUICKSORT_HPP
#define SHARE_UTILITIES_QUICKSORT_HPP

#include "memory/allocation.hpp"
#include "runtime/globals.hpp"
#include "utilities/debug.hpp"

class QuickSort : AllStatic {

 private:
  template<class T>
  static void swap(T* array, size_t x, size_t y) {
    T tmp = array[x];
    array[x] = array[y];
    array[y] = tmp;
  }

  // As pivot we use the median of the first, last and middle elements.
  // We swap in these three values at the right place in the array. This
  // means that this method not only returns the index of the pivot
  // element. It also alters the array so that:
  //     array[first] <= array[middle] <= array[last]
  // A side effect of this is that arrays of length <= 3 are sorted.
  template<class T, class C>
  static size_t find_pivot(T* array, size_t length, C comparator) {
    assert(length > 1, "length of array must be > 0");

    size_t middle_index = length / 2;
    size_t last_index = length - 1;

    if (comparator(array[0], array[middle_index]) > 0) {
      swap(array, 0, middle_index);
    }
    if (comparator(array[0], array[last_index]) > 0) {
      swap(array, 0, last_index);
    }
    if (comparator(array[middle_index], array[last_index]) > 0) {
      swap(array, middle_index, last_index);
    }
    // Now the value in the middle of the array is the median
    // of the fist, last and middle values. Use this as pivot.
    return middle_index;
  }

  template<bool idempotent, class T, class C>
  static size_t partition(T* array, size_t pivot, size_t length, C comparator) {
    size_t left_index = 0;
    size_t right_index = length - 1;
    T pivot_val = array[pivot];

    for ( ; true; ++left_index, --right_index) {
      for ( ; comparator(array[left_index], pivot_val) < 0; ++left_index) {
        assert(left_index < length, "reached end of partition");
      }
      for ( ; comparator(array[right_index], pivot_val) > 0; --right_index) {
        assert(right_index > 0, "reached start of partition");
      }

      if (left_index < right_index) {
        if (!idempotent || comparator(array[left_index], array[right_index]) != 0) {
          swap(array, left_index, right_index);
        }
      } else {
        return right_index;
      }
    }

    ShouldNotReachHere();
    return 0;
  }

  template<bool idempotent, class T, class C>
  static void inner_sort(T* array, size_t length, C comparator) {
    if (length < 2) {
      return;
    }
    size_t pivot = find_pivot(array, length, comparator);
    if (length < 4) {
      // arrays up to length 3 will be sorted after finding the pivot
      return;
    }
    size_t split = partition<idempotent>(array, pivot, length, comparator);
    size_t first_part_length = split + 1;
    inner_sort<idempotent>(array, first_part_length, comparator);
    inner_sort<idempotent>(&array[first_part_length], length - first_part_length, comparator);
  }

 public:
  // The idempotent parameter prevents the sort from
  // reordering a previous valid sort by not swapping
  // fields that compare as equal. This requires extra
  // calls to the comparator, so the performance
  // impact depends on the comparator.
  template<class T, class C>
  static void sort(T* array, size_t length, C comparator, bool idempotent) {
    // Switch "idempotent" from function paramter to template parameter
    if (idempotent) {
      inner_sort<true>(array, length, comparator);
    } else {
      inner_sort<false>(array, length, comparator);
    }
  }
};


#endif // SHARE_UTILITIES_QUICKSORT_HPP
