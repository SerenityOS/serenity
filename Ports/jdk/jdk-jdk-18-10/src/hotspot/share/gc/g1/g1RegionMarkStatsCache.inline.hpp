/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1REGIONMARKSTATSCACHE_INLINE_HPP
#define SHARE_GC_G1_G1REGIONMARKSTATSCACHE_INLINE_HPP

#include "gc/g1/g1RegionMarkStatsCache.hpp"

#include "runtime/atomic.hpp"

inline G1RegionMarkStatsCache::G1RegionMarkStatsCacheEntry* G1RegionMarkStatsCache::find_for_add(uint region_idx) {
  uint const cache_idx = hash(region_idx);

  G1RegionMarkStatsCacheEntry* cur = &_cache[cache_idx];
  if (cur->_region_idx != region_idx) {
    evict(cache_idx);
    cur->_region_idx = region_idx;
    _cache_misses++;
  } else {
    _cache_hits++;
  }

  return cur;
}

inline void G1RegionMarkStatsCache::evict(uint idx) {
  G1RegionMarkStatsCacheEntry* cur = &_cache[idx];
  if (cur->_stats._live_words != 0) {
    Atomic::add(&_target[cur->_region_idx]._live_words, cur->_stats._live_words);
  }
  cur->clear();
}

#endif // SHARE_GC_G1_G1REGIONMARKSTATSCACHE_INLINE_HPP
