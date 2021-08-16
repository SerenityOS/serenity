/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/g1/g1CollectedHeap.inline.hpp"
#include "gc/g1/g1ConcurrentMarkBitMap.inline.hpp"
#include "gc/g1/heapRegion.hpp"
#include "memory/virtualspace.hpp"

void G1CMBitMap::initialize(MemRegion heap, G1RegionToSpaceMapper* storage) {
  MarkBitMap::initialize(heap, storage->reserved());
  storage->set_mapping_changed_listener(&_listener);
}

void G1CMBitMapMappingChangedListener::on_commit(uint start_region, size_t num_regions, bool zero_filled) {
  if (zero_filled) {
    return;
  }
  // We need to clear the bitmap on commit, removing any existing information.
  MemRegion mr(G1CollectedHeap::heap()->bottom_addr_for_region(start_region), num_regions * HeapRegion::GrainWords);
  _bm->clear_range(mr);
}

void G1CMBitMap::clear_region(HeapRegion* region) {
 if (!region->is_empty()) {
   MemRegion mr(region->bottom(), region->top());
   clear_range(mr);
 }
}

#ifdef ASSERT
void G1CMBitMap::check_mark(HeapWord* addr) {
  assert(G1CollectedHeap::heap()->is_in(addr),
         "Trying to access bitmap " PTR_FORMAT " for address " PTR_FORMAT " not in the heap.",
         p2i(this), p2i(addr));
}
#endif
