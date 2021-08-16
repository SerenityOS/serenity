/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include "precompiled.hpp"
#include "memory/allocation.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/memRegion.hpp"
#include "runtime/globals.hpp"

// A very simple data structure representing a contigous word-aligned
// region of address space.

MemRegion MemRegion::intersection(const MemRegion mr2) const {
  MemRegion res;
  HeapWord* res_start = MAX2(start(), mr2.start());
  HeapWord* res_end   = MIN2(end(),   mr2.end());
  if (res_start < res_end) {
    res.set_start(res_start);
    res.set_end(res_end);
  }
  return res;
}

MemRegion MemRegion::_union(const MemRegion mr2) const {
  // If one region is empty, return the other
  if (is_empty()) return mr2;
  if (mr2.is_empty()) return MemRegion(start(), end());

  // Otherwise, regions must overlap or be adjacent
  assert(((start() <= mr2.start()) && (end() >= mr2.start())) ||
         ((mr2.start() <= start()) && (mr2.end() >= start())),
             "non-adjacent or overlapping regions");
  MemRegion res;
  HeapWord* res_start = MIN2(start(), mr2.start());
  HeapWord* res_end   = MAX2(end(),   mr2.end());
  res.set_start(res_start);
  res.set_end(res_end);
  return res;
}

MemRegion MemRegion::minus(const MemRegion mr2) const {
  // There seem to be 6 cases:
  //                  |this MemRegion|
  // |strictly below|
  //   |overlap beginning|
  //                    |interior|
  //                        |overlap ending|
  //                                   |strictly above|
  //              |completely overlapping|
  // We can't deal with an interior case because it would
  // produce two disjoint regions as a result.
  // We aren't trying to be optimal in the number of tests below,
  // but the order is important to distinguish the strictly cases
  // from the overlapping cases.
  if (mr2.end() <= start()) {
    // strictly below
    return MemRegion(start(), end());
  }
  if (mr2.start() <= start() && mr2.end() <= end()) {
    // overlap beginning
    return MemRegion(mr2.end(), end());
  }
  if (mr2.start() >= end()) {
    // strictly above
    return MemRegion(start(), end());
  }
  if (mr2.start() >= start() && mr2.end() >= end()) {
    // overlap ending
    return MemRegion(start(), mr2.start());
  }
  if (mr2.start() <= start() && mr2.end() >= end()) {
    // completely overlapping
    return MemRegion();
  }
  if (mr2.start() > start() && mr2.end() < end()) {
    // interior
    guarantee(false, "MemRegion::minus, but interior");
    return MemRegion();
  }
  ShouldNotReachHere();
  return MemRegion();
}

MemRegion* MemRegion::create_array(size_t length, MEMFLAGS flags) {
  MemRegion* result = NEW_C_HEAP_ARRAY(MemRegion, length, flags);
  for (size_t i = 0; i < length; i++) {
    ::new (&result[i]) MemRegion();
  }
  return result;
}

void MemRegion::destroy_array(MemRegion* array, size_t length) {
  if (array == NULL) {
    return;
  }
  for (size_t i = 0; i < length; i++) {
    array[i].~MemRegion();
  }
  FREE_C_HEAP_ARRAY(MemRegion, array);
}