/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020 SAP SE. All rights reserved.
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

#ifndef GTEST_METASPACE_METASPACEGTESTSPARSEARRAY_HPP
#define GTEST_METASPACE_METASPACEGTESTSPARSEARRAY_HPP

#include "memory/allocation.hpp"
#include "runtime/os.hpp"
#include "utilities/debug.hpp"
#include "metaspaceGtestCommon.hpp"
#include "metaspaceGtestRangeHelpers.hpp"

/////// SparseArray<T> ////////////////

// Throughout these tests we need to keep track of allocated items (ranges of metaspace memory, metachunks, ..)
//  and be able to random-access them. Makes sense to have a helper for that.
template <class T>
class SparseArray : public StackObj {

  T* const _slots;
  const int _num;

  // For convenience: a range covering all possible slot indices.
  const IntRange _index_range;

  bool contains(int index) const {
    return _index_range.contains(index);
  }

  // Check slot intex for oob
  void check_index(int i) const {
    assert(contains(i), "Sanity");
  }

  // Swap the content of two slots.
  void swap(int i1, int i2) {
    check_index(i1);
    check_index(i2);
    T tmp = _slots[i1];
    _slots[i1] = _slots[i2];
    _slots[i2] = tmp;
  }

  enum condition_t { cond_null = 0, cond_non_null = 1, cond_dontcare = 2 };

  // Helper for next_matching_slot
  bool slot_matches(int slot, condition_t c) const {
    switch(c) {
    case cond_null:     return _slots[slot] == NULL;
    case cond_non_null: return _slots[slot] != NULL;
    case cond_dontcare: return true;
    }
    ShouldNotReachHere();
    return false;
  }

  // Starting at (including) index, find the next matching slot. Returns index or -1 if none found.
  int next_matching_slot(int slot, condition_t c) const {
    while(slot < _num) {
      if (slot_matches(slot, c)) {
        return slot;
      }
      slot++;
    }
    return -1;
  }

public:

  SparseArray(int num) :
    _slots(NEW_C_HEAP_ARRAY(T, num, mtInternal)),
    _num(num),
    _index_range(num)
  {
    for (int i = 0; i < _num; i++) {
      _slots[i] = NULL;
    }
  }

  T at(int i)              { return _slots[i]; }
  const T at(int i) const  { return _slots[i]; }
  void set_at(int i, T e)  { _slots[i] = e; }

  int size() const         { return _num; }

  bool slot_is_null(int i) const                      { check_index(i); return _slots[i] == NULL; }

  DEBUG_ONLY(void check_slot_is_null(int i) const     { assert(slot_is_null(i), "Slot %d is not null", i); })
  DEBUG_ONLY(void check_slot_is_not_null(int i) const { assert(!slot_is_null(i), "Slot %d is null", i); })

  // Shuffle all elements randomly
  void shuffle() {
    for (int i = 0; i < _num; i++) {
      swap(i, random_slot_index());
    }
  }

  // Reverse elements
  void reverse() {
    for (int i = 0; i < _num / 2; i++) {
      swap(i, _num - i);
    }
  }

  int first_slot() const            { return 0; }
  int next_slot(int index) const    { return index == _index_range.highest() ? -1 : index + 1; }

  int first_non_null_slot() const         { return next_matching_slot(0, cond_non_null); }
  int next_non_null_slot(int index) const { return next_matching_slot(index + 1, cond_non_null); }

  int first_null_slot() const             { return next_matching_slot(0, cond_null); }
  int next_null_slot(int index) const     { return next_matching_slot(index + 1, cond_null); }

  // Return a random slot index.
  int random_slot_index() const {
    return _index_range.random_value();
  }

  int random_non_null_slot_index() const {
    int i = next_non_null_slot(_index_range.random_value());
    if (i == -1) {
      i = first_non_null_slot();
    }
    return i;
  }

  int random_null_slot_index() const {
    int i = next_null_slot(_index_range.random_value());
    if (i == -1) {
      i = first_null_slot();
    }
    return i;
  }

  IntRange random_slot_range() const {
    return _index_range.random_subrange();
  }

};

#endif // GTEST_METASPACE_METASPACEGTESTSPARSEARRAY_HPP

