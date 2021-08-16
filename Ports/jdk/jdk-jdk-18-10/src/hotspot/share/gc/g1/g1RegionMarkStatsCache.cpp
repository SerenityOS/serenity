/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/g1/g1RegionMarkStatsCache.inline.hpp"
#include "memory/allocation.inline.hpp"
#include "utilities/powerOfTwo.hpp"

G1RegionMarkStatsCache::G1RegionMarkStatsCache(G1RegionMarkStats* target, uint num_cache_entries) :
  _target(target),
  _cache(NULL),
  _num_cache_entries(num_cache_entries),
  _cache_hits(0),
  _cache_misses(0),
  _num_cache_entries_mask(_num_cache_entries - 1) {

  guarantee(is_power_of_2(num_cache_entries),
            "Number of cache entries must be power of two, but is %u", num_cache_entries);
  _cache = NEW_C_HEAP_ARRAY(G1RegionMarkStatsCacheEntry, _num_cache_entries, mtGC);
}

G1RegionMarkStatsCache::~G1RegionMarkStatsCache() {
  FREE_C_HEAP_ARRAY(G1RegionMarkStatsCacheEntry, _cache);
}

void G1RegionMarkStatsCache::add_live_words(oop obj) {
  uint region_index = G1CollectedHeap::heap()->addr_to_region(cast_from_oop<HeapWord*>(obj));
  add_live_words(region_index, (size_t) obj->size());
}

// Evict all remaining statistics, returning cache hits and misses.
Pair<size_t, size_t> G1RegionMarkStatsCache::evict_all() {
  for (uint i = 0; i < _num_cache_entries; i++) {
    evict(i);
  }
  return Pair<size_t,size_t>(_cache_hits, _cache_misses);
}

void G1RegionMarkStatsCache::reset() {
  _cache_hits = 0;
  _cache_misses = 0;

  for (uint i = 0; i < _num_cache_entries; i++) {
    // Avoid the initial cache miss and eviction by setting the i'th's cache
    // region_idx to the region_idx due to how the hash is calculated.
    _cache[i].clear(i);
  }
}
