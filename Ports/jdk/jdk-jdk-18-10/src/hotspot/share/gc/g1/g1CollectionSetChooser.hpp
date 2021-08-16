/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1COLLECTIONSETCHOOSER_HPP
#define SHARE_GC_G1_G1COLLECTIONSETCHOOSER_HPP

#include "gc/g1/heapRegion.hpp"
#include "memory/allocation.hpp"
#include "runtime/globals.hpp"

class G1CollectionSetCandidates;
class WorkGang;

// Helper class to calculate collection set candidates, and containing some related
// methods.
class G1CollectionSetChooser : public AllStatic {
  static uint calculate_work_chunk_size(uint num_workers, uint num_regions);

  // Remove regions in the collection set candidates as long as the G1HeapWastePercent
  // criteria is met. Keep at least the minimum amount of old regions to guarantee
  // some progress.
  static void prune(G1CollectionSetCandidates* candidates);
public:

  static size_t mixed_gc_live_threshold_bytes() {
    return HeapRegion::GrainBytes * (size_t) G1MixedGCLiveThresholdPercent / 100;
  }

  static bool region_occupancy_low_enough_for_evac(size_t live_bytes) {
    return live_bytes < mixed_gc_live_threshold_bytes();
  }

  // Determine whether to add the given region to the collection set candidates or
  // not. Currently, we skip pinned regions and regions whose live
  // bytes are over the threshold. Humongous regions may be reclaimed during cleanup.
  // Regions also need a complete remembered set to be a candidate.
  static bool should_add(HeapRegion* hr);

  // Build and return set of collection set candidates sorted by decreasing gc
  // efficiency.
  static G1CollectionSetCandidates* build(WorkGang* workers, uint max_num_regions);
};

#endif // SHARE_GC_G1_G1COLLECTIONSETCHOOSER_HPP
