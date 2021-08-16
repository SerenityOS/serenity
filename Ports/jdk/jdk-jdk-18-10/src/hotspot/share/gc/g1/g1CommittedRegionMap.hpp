/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1COMMITTEDREGIONMAP_HPP
#define SHARE_GC_G1_G1COMMITTEDREGIONMAP_HPP

#include "memory/allocation.hpp"
#include "utilities/bitMap.hpp"
#include "utilities/macros.hpp"

// Helper class to define a range [start, end) of regions.
class HeapRegionRange : public StackObj {
  // Inclusive start of the range.
  uint _start;
  // Exclusive end of the range.
  uint _end;
 public:
  HeapRegionRange(uint start, uint end);

  uint start() const { return _start; }
  uint end() const { return _end; }
  uint length() const { return _end - _start; }
};

// The G1CommittedRegionMap keeps track of which regions are currently committed.
// It tracks both regions ready for use and if there are any regions ready for
// uncommit. We basically have three states. Uncommitted, Active, Inactive. All
// regions that are either Active or Inactive are committed.
//
// State transitions:
//   Uncommitted -> Active      (activate())
//   Active      -> Inactive    (deactivate())
//   Inactive    -> Active      (reactivate())
//   Inactive    -> Uncommitted (uncommit())
//
class G1CommittedRegionMap : public CHeapObj<mtGC> {
  // Each bit in this bitmap indicates that the corresponding region is active
  // and available for allocation.
  CHeapBitMap _active;
  // Each bit in this bitmap indicates that the corresponding region is no longer
  // active and it can be uncommitted.
  CHeapBitMap _inactive;
  // The union of these two bitmaps are the regions that are currently committed.

  // The number of regions active and available for use.
  uint _num_active;

  // The number of regions ready to be uncommitted.
  uint _num_inactive;

  uint max_length() const;

  // Helpers to mark and do accounting for the bitmaps. Depending on when called
  // these helpers require to own different locks. See guarantee_mt_safety_* for
  // details.
  void active_set_range(uint start, uint end);
  void active_clear_range(uint start, uint end);
  void inactive_set_range(uint start, uint end);
  void inactive_clear_range(uint start, uint end);

public:
  G1CommittedRegionMap();
  void initialize(uint num_regions);

  uint num_active() const;
  uint num_inactive() const;

  // Check if a region is marked active.
  inline bool active(uint index) const;
  // Check if a region is marked inactive.
  inline bool inactive(uint index) const;

  // Mark a range of regions as active.
  void activate(uint start, uint end);
  // Mark a range of regions as inactive and ready to be uncommitted.
  void deactivate(uint start, uint end);
  // Mark a range of regions active again and no longer ready for uncommit.
  void reactivate(uint start, uint end);
  // Uncommit a range of inactive regions.
  void uncommit(uint start, uint end);

  // Finds the next range of active regions starting at offset.
  HeapRegionRange next_active_range(uint offset) const;
  // Finds the next range of inactive regions starting at offset.
  HeapRegionRange next_inactive_range(uint offset) const;
  // Finds the next range of committable regions starting at offset.
  // This function must only be called when no inactive regions are
  // present and can be used to activate more regions.
  HeapRegionRange next_committable_range(uint offset) const;

protected:
  virtual void guarantee_mt_safety_active() const;
  virtual void guarantee_mt_safety_inactive() const;

  void verify_active_range(uint start, uint end) const NOT_DEBUG_RETURN;
  void verify_free_range(uint start, uint end) const NOT_DEBUG_RETURN;
  void verify_inactive_range(uint start, uint end) const NOT_DEBUG_RETURN;
  void verify_no_inactive_regons() const NOT_DEBUG_RETURN;
  void verify_active_count(uint start, uint end, uint expected) const NOT_DEBUG_RETURN;
  void verify_inactive_count(uint start, uint end, uint expected) const NOT_DEBUG_RETURN;
};

#endif // SHARE_GC_G1_G1COMMITTEDREGIONMAP_HPP
