/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1FROMCARDCACHE_HPP
#define SHARE_GC_G1_G1FROMCARDCACHE_HPP

#include "memory/allocation.hpp"
#include "utilities/ostream.hpp"

// G1FromCardCache remembers the most recently processed card on the heap on
// a per-region and per-thread basis.
class G1FromCardCache : public AllStatic {
private:
  // Array of card indices. Indexed by heap region (rows) and thread (columns) to minimize
  // thread contention.
  // This order minimizes the time to clear all entries for a given region during region
  // freeing. I.e. a single clear of a single memory area instead of multiple separate
  // accesses with a large stride per region.
  static uintptr_t** _cache;
  static uint _max_reserved_regions;
  static size_t _static_mem_size;
#ifdef ASSERT
  static uint _max_workers;

  static void check_bounds(uint worker_id, uint region_idx) {
    assert(worker_id < _max_workers, "Worker_id %u is larger than maximum %u", worker_id, _max_workers);
    assert(region_idx < _max_reserved_regions, "Region_idx %u is larger than maximum %u", region_idx, _max_reserved_regions);
  }
#endif

  // This card index indicates "no card for that entry" yet. This allows us to use the OS
  // lazy backing of memory with zero-filled pages to avoid initial actual memory use.
  // This means that the heap must not contain card zero.
  static const uintptr_t InvalidCard = 0;

  // Gives an approximation on how many threads can be expected to add records to
  // a remembered set in parallel. This is used for sizing the G1FromCardCache to
  // decrease performance losses due to data structure sharing.
  // Examples for quantities that influence this value are the maximum number of
  // mutator threads, maximum number of concurrent refinement or GC threads.
  static uint num_par_rem_sets();

public:
  static void clear(uint region_idx);

  // Returns true if the given card is in the cache at the given location, or
  // replaces the card at that location and returns false.
  static bool contains_or_replace(uint worker_id, uint region_idx, uintptr_t card) {
    uintptr_t card_in_cache = at(worker_id, region_idx);
    if (card_in_cache == card) {
      return true;
    } else {
      set(worker_id, region_idx, card);
      return false;
    }
  }

  static uintptr_t at(uint worker_id, uint region_idx) {
    DEBUG_ONLY(check_bounds(worker_id, region_idx);)
    return _cache[region_idx][worker_id];
  }

  static void set(uint worker_id, uint region_idx, uintptr_t val) {
    DEBUG_ONLY(check_bounds(worker_id, region_idx);)
    _cache[region_idx][worker_id] = val;
  }

  static void initialize(uint max_reserved_regions);

  static void invalidate(uint start_idx, size_t num_regions);

  static void print(outputStream* out = tty) PRODUCT_RETURN;

  static size_t static_mem_size() {
    return _static_mem_size;
  }
};

#endif // SHARE_GC_G1_G1FROMCARDCACHE_HPP
