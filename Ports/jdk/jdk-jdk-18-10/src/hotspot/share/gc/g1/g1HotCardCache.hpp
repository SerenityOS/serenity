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

#ifndef SHARE_GC_G1_G1HOTCARDCACHE_HPP
#define SHARE_GC_G1_G1HOTCARDCACHE_HPP

#include "gc/g1/g1CardCounts.hpp"
#include "memory/allocation.hpp"
#include "runtime/safepoint.hpp"
#include "runtime/thread.hpp"
#include "utilities/globalDefinitions.hpp"

class G1CardTableEntryClosure;
class G1CollectedHeap;
class HeapRegion;

// An evicting cache of cards that have been logged by the G1 post
// write barrier. Placing a card in the cache delays the refinement
// of the card until the card is evicted, or the cache is drained
// during the next evacuation pause.
//
// The first thing the G1 post write barrier does is to check whether
// the card containing the updated pointer is already dirty and, if
// so, skips the remaining code in the barrier.
//
// Delaying the refinement of a card will make the card fail the
// first is_dirty check in the write barrier, skipping the remainder
// of the write barrier.
//
// This can significantly reduce the overhead of the write barrier
// code, increasing throughput.

class G1HotCardCache: public CHeapObj<mtGC> {
public:
  typedef CardTable::CardValue CardValue;

private:
  G1CollectedHeap*  _g1h;

  bool              _use_cache;

  G1CardCounts      _card_counts;


  // The card cache table
  CardValue** _hot_cache;

  size_t            _hot_cache_size;

  size_t            _hot_cache_par_chunk_size;

  // Avoids false sharing when concurrently updating _hot_cache_idx or
  // _hot_cache_par_claimed_idx. These are never updated at the same time
  // thus it's not necessary to separate them as well
  char _pad_before[DEFAULT_CACHE_LINE_SIZE];

  volatile size_t _hot_cache_idx;

  volatile size_t _hot_cache_par_claimed_idx;

  char _pad_after[DEFAULT_CACHE_LINE_SIZE];

  // Records whether insertion overflowed the hot card cache at least once. This
  // avoids the need for a separate atomic counter of how many valid entries are
  // in the HCC.
  volatile bool _cache_wrapped_around;

  // The number of cached cards a thread claims when flushing the cache
  static const int ClaimChunkSize = 32;

 public:
  static bool default_use_cache() {
    return (G1ConcRSLogCacheSize > 0);
  }

  G1HotCardCache(G1CollectedHeap* g1h);
  ~G1HotCardCache();

  void initialize(G1RegionToSpaceMapper* card_counts_storage);

  bool use_cache() { return _use_cache; }

  void set_use_cache(bool b) {
    _use_cache = (b ? default_use_cache() : false);
  }

  // Returns the card to be refined or NULL.
  //
  // Increments the count for given the card. if the card is not 'hot',
  // it is returned for immediate refining. Otherwise the card is
  // added to the hot card cache.
  // If there is enough room in the hot card cache for the card we're
  // adding, NULL is returned and no further action in needed.
  // If we evict a card from the cache to make room for the new card,
  // the evicted card is then returned for refinement.
  CardValue* insert(CardValue* card_ptr);

  // Refine the cards that have delayed as a result of
  // being in the cache.
  void drain(G1CardTableEntryClosure* cl, uint worker_id);

  // Set up for parallel processing of the cards in the hot cache
  void reset_hot_cache_claimed_index() {
    _hot_cache_par_claimed_idx = 0;
  }

  // Resets the hot card cache and discards the entries.
  void reset_hot_cache() {
    assert(SafepointSynchronize::is_at_safepoint(), "Should be at a safepoint");
    if (default_use_cache()) {
      reset_hot_cache_internal();
    }
  }

  // Zeros the values in the card counts table for the given region
  void reset_card_counts(HeapRegion* hr);

  // Number of entries in the HCC.
  size_t num_entries() const {
    return _cache_wrapped_around ? _hot_cache_size : _hot_cache_idx + 1;
  }
 private:
  void reset_hot_cache_internal() {
    assert(_hot_cache != NULL, "Logic");
    _hot_cache_idx = 0;
    for (size_t i = 0; i < _hot_cache_size; i++) {
      _hot_cache[i] = NULL;
    }
    _cache_wrapped_around = false;
  }
};

#endif // SHARE_GC_G1_G1HOTCARDCACHE_HPP
