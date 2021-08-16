/*
 * Copyright (c) 2018, 2019, Red Hat, Inc. All rights reserved.
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

#include "gc/shenandoah/shenandoahCollectionSet.hpp"
#include "gc/shenandoah/heuristics/shenandoahCompactHeuristics.hpp"
#include "gc/shenandoah/shenandoahFreeSet.hpp"
#include "gc/shenandoah/shenandoahHeap.inline.hpp"
#include "gc/shenandoah/shenandoahHeapRegion.inline.hpp"
#include "logging/log.hpp"
#include "logging/logTag.hpp"

ShenandoahCompactHeuristics::ShenandoahCompactHeuristics() : ShenandoahHeuristics() {
  SHENANDOAH_ERGO_ENABLE_FLAG(ExplicitGCInvokesConcurrent);
  SHENANDOAH_ERGO_ENABLE_FLAG(ShenandoahImplicitGCInvokesConcurrent);
  SHENANDOAH_ERGO_ENABLE_FLAG(ShenandoahUncommit);
  SHENANDOAH_ERGO_ENABLE_FLAG(ShenandoahAlwaysClearSoftRefs);
  SHENANDOAH_ERGO_OVERRIDE_DEFAULT(ShenandoahAllocationThreshold,  10);
  SHENANDOAH_ERGO_OVERRIDE_DEFAULT(ShenandoahImmediateThreshold,   100);
  SHENANDOAH_ERGO_OVERRIDE_DEFAULT(ShenandoahUncommitDelay,        1000);
  SHENANDOAH_ERGO_OVERRIDE_DEFAULT(ShenandoahGuaranteedGCInterval, 30000);
  SHENANDOAH_ERGO_OVERRIDE_DEFAULT(ShenandoahGarbageThreshold,     10);
}

bool ShenandoahCompactHeuristics::should_start_gc() {
  ShenandoahHeap* heap = ShenandoahHeap::heap();

  size_t max_capacity = heap->max_capacity();
  size_t capacity = heap->soft_max_capacity();
  size_t available = heap->free_set()->available();

  // Make sure the code below treats available without the soft tail.
  size_t soft_tail = max_capacity - capacity;
  available = (available > soft_tail) ? (available - soft_tail) : 0;

  size_t threshold_bytes_allocated = capacity / 100 * ShenandoahAllocationThreshold;
  size_t min_threshold = capacity / 100 * ShenandoahMinFreeThreshold;

  if (available < min_threshold) {
    log_info(gc)("Trigger: Free (" SIZE_FORMAT "%s) is below minimum threshold (" SIZE_FORMAT "%s)",
                 byte_size_in_proper_unit(available),     proper_unit_for_byte_size(available),
                 byte_size_in_proper_unit(min_threshold), proper_unit_for_byte_size(min_threshold));
    return true;
  }

  size_t bytes_allocated = heap->bytes_allocated_since_gc_start();
  if (bytes_allocated > threshold_bytes_allocated) {
    log_info(gc)("Trigger: Allocated since last cycle (" SIZE_FORMAT "%s) is larger than allocation threshold (" SIZE_FORMAT "%s)",
                 byte_size_in_proper_unit(bytes_allocated),           proper_unit_for_byte_size(bytes_allocated),
                 byte_size_in_proper_unit(threshold_bytes_allocated), proper_unit_for_byte_size(threshold_bytes_allocated));
    return true;
  }

  return ShenandoahHeuristics::should_start_gc();
}

void ShenandoahCompactHeuristics::choose_collection_set_from_regiondata(ShenandoahCollectionSet* cset,
                                                                        RegionData* data, size_t size,
                                                                        size_t actual_free) {
  // Do not select too large CSet that would overflow the available free space
  size_t max_cset = actual_free * 3 / 4;

  log_info(gc, ergo)("CSet Selection. Actual Free: " SIZE_FORMAT "%s, Max CSet: " SIZE_FORMAT "%s",
                     byte_size_in_proper_unit(actual_free), proper_unit_for_byte_size(actual_free),
                     byte_size_in_proper_unit(max_cset),    proper_unit_for_byte_size(max_cset));

  size_t threshold = ShenandoahHeapRegion::region_size_bytes() * ShenandoahGarbageThreshold / 100;

  size_t live_cset = 0;
  for (size_t idx = 0; idx < size; idx++) {
    ShenandoahHeapRegion* r = data[idx]._region;
    size_t new_cset = live_cset + r->get_live_data_bytes();
    if (new_cset < max_cset && r->garbage() > threshold) {
      live_cset = new_cset;
      cset->add_region(r);
    }
  }
}
