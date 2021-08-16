/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1FULLGCHEAPREGIONATTR_HPP
#define SHARE_GC_G1_G1FULLGCHEAPREGIONATTR_HPP

#include "gc/g1/g1BiasedArray.hpp"

// This table is used to store attribute values of all HeapRegions that need
// fast access during the full collection. In particular some parts of the
// region type information is encoded in these per-region bytes. Value encoding
// has been specifically chosen to make required accesses fast. In particular,
// the table specifies whether a Full GC cycle should be compacting, skip
// compacting, or skip marking (liveness analysis) a region.
// Reasons for not compacting a region:
// (1) the HeapRegion itself has been pinned at the start of Full GC.
// (2) the occupancy of the region is too high to be considered eligible for compaction.
// The only examples for skipping marking for regions are Closed Archive regions.
class G1FullGCHeapRegionAttr : public G1BiasedMappedArray<uint8_t> {
  static const uint8_t Compacting = 0;       // Region will be compacted.
  static const uint8_t SkipCompacting = 1;   // Region should not be compacted, but otherwise handled as usual.
  static const uint8_t SkipMarking = 2;      // Region contents are not even marked through, but contain live objects.

  static const uint8_t Invalid = 255;

  bool is_invalid(HeapWord* obj) const {
    return get_by_address(obj) == Invalid;
  }

protected:
  uint8_t default_value() const { return Invalid; }

public:
  void set_invalid(uint idx) { set_by_index(idx, Invalid); }

  void set_compacting(uint idx) { set_by_index(idx, Compacting); }
  void set_skip_marking(uint idx) { set_by_index(idx, SkipMarking); }
  void set_skip_compacting(uint idx) { set_by_index(idx, SkipCompacting); }

  bool is_skip_marking(HeapWord* obj) const {
    assert(!is_invalid(obj), "not initialized yet");
    return get_by_address(obj) == SkipMarking;
  }

  bool is_compacting(HeapWord* obj) const {
    assert(!is_invalid(obj), "not initialized yet");
    return get_by_address(obj) == Compacting;
  }

  bool is_skip_compacting(uint idx) const {
    return get_by_index(idx) == SkipCompacting;
  }

  void verify_is_compacting(uint idx) { assert(get_by_index(idx) == Compacting, "invariant"); }

  void verify_is_invalid(uint idx) { assert(get_by_index(idx) == Invalid, "invariant"); }
};

#endif // SHARE_GC_G1_G1FULLGCHEAPREGIONATTR_HPP
