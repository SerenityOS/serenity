/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1COLLECTIONSETCANDIDATES_HPP
#define SHARE_GC_G1_G1COLLECTIONSETCANDIDATES_HPP

#include "gc/g1/g1CollectionSetCandidates.hpp"
#include "gc/shared/workgroup.hpp"
#include "memory/allocation.hpp"
#include "runtime/globals.hpp"

class HeapRegion;
class HeapRegionClosure;

// Set of collection set candidates, i.e. all old gen regions we consider worth
// collecting in the remainder of the current mixed phase. Regions are sorted by decreasing
// gc efficiency.
// Maintains a cursor into the list that specifies the next collection set candidate
// to put into the current collection set.
class G1CollectionSetCandidates : public CHeapObj<mtGC> {
  HeapRegion** _regions;
  uint _num_regions; // Total number of regions in the collection set candidate set.

  // The sum of bytes that can be reclaimed in the remaining set of collection
  // set candidates.
  size_t _remaining_reclaimable_bytes;
  // The index of the next candidate old region to be considered for
  // addition to the current collection set.
  uint _front_idx;

public:
  G1CollectionSetCandidates(HeapRegion** regions, uint num_regions, size_t remaining_reclaimable_bytes) :
    _regions(regions),
    _num_regions(num_regions),
    _remaining_reclaimable_bytes(remaining_reclaimable_bytes),
    _front_idx(0) { }

  ~G1CollectionSetCandidates() {
    FREE_C_HEAP_ARRAY(HeapRegion*, _regions);
  }

  // Returns the total number of collection set candidate old regions added.
  uint num_regions() { return _num_regions; }

  uint cur_idx() const { return _front_idx; }

  HeapRegion* at(uint idx) const {
    HeapRegion* res = NULL;
    if (idx < _num_regions) {
      res = _regions[idx];
      assert(res != NULL, "Unexpected NULL HeapRegion at index %u", idx);
    }
    return res;
  }

  // Remove num_regions from the front of the collection set candidate list.
  void remove(uint num_regions);
  // Remove num_remove regions from the back of the collection set candidate list.
  void remove_from_end(uint num_remove, size_t wasted);

  // Iterate over all remaining collection set candidate regions.
  void iterate(HeapRegionClosure* cl);
  // Iterate over all remaining collectin set candidate regions from the end
  // to the beginning of the set.
  void iterate_backwards(HeapRegionClosure* cl);

  // Return the number of candidate regions remaining.
  uint num_remaining() { return _num_regions - _front_idx; }

  bool is_empty() { return num_remaining() == 0; }

  // Return the amount of reclaimable bytes that may be collected by the remaining
  // candidate regions.
  size_t remaining_reclaimable_bytes() { return _remaining_reclaimable_bytes; }

  void verify() const PRODUCT_RETURN;
};

#endif /* SHARE_GC_G1_G1COLLECTIONSETCANDIDATES_HPP */

