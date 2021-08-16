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

#ifndef SHARE_GC_G1_G1REGIONMARKSTATSCACHE_HPP
#define SHARE_GC_G1_G1REGIONMARKSTATSCACHE_HPP

#include "memory/allocation.hpp"
#include "oops/oop.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/pair.hpp"

// Per-Region statistics gathered during marking.
//
// This includes
// * the number of live words gathered during marking for the area from bottom
// to ntams. This is an exact measure.
// The code corrects later for the live data between ntams and top.
struct G1RegionMarkStats {
  size_t _live_words;

  // Clear all members.
  void clear() {
    _live_words = 0;
  }
  // Clear all members after a marking overflow. Nothing to do as the live words
  // are updated by the atomic mark. We do not remark objects after overflow.
  void clear_during_overflow() {
  }

  bool is_clear() const { return _live_words == 0; }
};

// Per-marking thread cache for the region mark statistics.
//
// Each cache is a larg'ish map of region-idx -> G1RegionMarkStats entries that cache
// currently gathered statistics; entries are evicted to the global statistics array
// on every collision. This minimizes synchronization overhead which would be required
// every time statistics change, as marking is very localized.
// The map entry number is a power of two to allow simple and fast hashing using
// logical and.
class G1RegionMarkStatsCache {
private:
  // The array of statistics entries to evict to; the global array.
  G1RegionMarkStats* _target;

  // An entry of the statistics cache.
  struct G1RegionMarkStatsCacheEntry {
    uint _region_idx;
    G1RegionMarkStats _stats;

    void clear(uint idx = 0) {
      _region_idx = idx;
      _stats.clear();
    }
  };

  // The actual cache and its number of entries.
  G1RegionMarkStatsCacheEntry* _cache;
  uint _num_cache_entries;

  // Cache hits/miss counters.
  size_t _cache_hits;
  size_t _cache_misses;

  // Evict a given element of the statistics cache.
  void evict(uint idx);

  size_t _num_cache_entries_mask;

  uint hash(uint idx) {
    return idx & _num_cache_entries_mask;
  }

  G1RegionMarkStatsCacheEntry* find_for_add(uint region_idx);
public:
  // Number of entries in the per-task stats entry. This value seems enough
  // to have a very low cache miss rate.
  static const uint RegionMarkStatsCacheSize = 1024;

  G1RegionMarkStatsCache(G1RegionMarkStats* target, uint num_cache_entries);

  ~G1RegionMarkStatsCache();

  void add_live_words(oop obj);
  void add_live_words(uint region_idx, size_t live_words) {
    G1RegionMarkStatsCacheEntry* const cur = find_for_add(region_idx);
    cur->_stats._live_words += live_words;
  }

  void reset(uint region_idx) {
    uint const cache_idx = hash(region_idx);
    G1RegionMarkStatsCacheEntry* cur = &_cache[cache_idx];
    if (cur->_region_idx == region_idx) {
      _cache[cache_idx].clear();
    }
  }

  // Evict all remaining statistics, returning cache hits and misses.
  Pair<size_t, size_t> evict_all();

  // Reset liveness of all cache entries to their default values,
  // initialize _region_idx to avoid initial cache miss.
  void reset();

  size_t hits() const { return _cache_hits; }
  size_t misses() const { return _cache_misses; }
};

#endif // SHARE_GC_G1_G1REGIONMARKSTATSCACHE_HPP
