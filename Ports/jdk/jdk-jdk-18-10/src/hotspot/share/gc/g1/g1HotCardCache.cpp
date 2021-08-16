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

#include "precompiled.hpp"
#include "gc/g1/g1CardTableEntryClosure.hpp"
#include "gc/g1/g1CollectedHeap.inline.hpp"
#include "gc/g1/g1DirtyCardQueue.hpp"
#include "gc/g1/g1HotCardCache.hpp"
#include "runtime/atomic.hpp"

G1HotCardCache::G1HotCardCache(G1CollectedHeap *g1h):
  _g1h(g1h), _use_cache(false), _card_counts(g1h),
  _hot_cache(NULL), _hot_cache_size(0), _hot_cache_par_chunk_size(0),
  _hot_cache_idx(0), _hot_cache_par_claimed_idx(0), _cache_wrapped_around(false)
{}

void G1HotCardCache::initialize(G1RegionToSpaceMapper* card_counts_storage) {
  if (default_use_cache()) {
    _use_cache = true;

    _hot_cache_size = (size_t)1 << G1ConcRSLogCacheSize;
    _hot_cache = ArrayAllocator<CardValue*>::allocate(_hot_cache_size, mtGC);

    reset_hot_cache_internal();

    // For refining the cards in the hot cache in parallel
    _hot_cache_par_chunk_size = ClaimChunkSize;
    _hot_cache_par_claimed_idx = 0;

    _cache_wrapped_around = false;

    _card_counts.initialize(card_counts_storage);
  }
}

G1HotCardCache::~G1HotCardCache() {
  if (default_use_cache()) {
    assert(_hot_cache != NULL, "Logic");
    ArrayAllocator<CardValue*>::free(_hot_cache, _hot_cache_size);
    _hot_cache = NULL;
  }
}

CardTable::CardValue* G1HotCardCache::insert(CardValue* card_ptr) {
  uint count = _card_counts.add_card_count(card_ptr);
  if (!_card_counts.is_hot(count)) {
    // The card is not hot so do not store it in the cache;
    // return it for immediate refining.
    return card_ptr;
  }
  // Otherwise, the card is hot.
  size_t index = Atomic::fetch_and_add(&_hot_cache_idx, 1u);
  if (index == _hot_cache_size) {
    // Can use relaxed store because all racing threads are writing the same
    // value and there aren't any concurrent readers.
    Atomic::store(&_cache_wrapped_around, true);
  }
  size_t masked_index = index & (_hot_cache_size - 1);
  CardValue* current_ptr = _hot_cache[masked_index];

  // Try to store the new card pointer into the cache. Compare-and-swap to guard
  // against the unlikely event of a race resulting in another card pointer to
  // have already been written to the cache. In this case we will return
  // card_ptr in favor of the other option, which would be starting over. This
  // should be OK since card_ptr will likely be the older card already when/if
  // this ever happens.
  CardValue* previous_ptr = Atomic::cmpxchg(&_hot_cache[masked_index],
                                            current_ptr,
                                            card_ptr);
  return (previous_ptr == current_ptr) ? previous_ptr : card_ptr;
}

void G1HotCardCache::drain(G1CardTableEntryClosure* cl, uint worker_id) {
  assert(default_use_cache(), "Drain only necessary if we use the hot card cache.");

  assert(_hot_cache != NULL, "Logic");
  assert(!use_cache(), "cache should be disabled");

  while (_hot_cache_par_claimed_idx < _hot_cache_size) {
    size_t end_idx = Atomic::add(&_hot_cache_par_claimed_idx,
                                 _hot_cache_par_chunk_size);
    size_t start_idx = end_idx - _hot_cache_par_chunk_size;
    // The current worker has successfully claimed the chunk [start_idx..end_idx)
    end_idx = MIN2(end_idx, _hot_cache_size);
    for (size_t i = start_idx; i < end_idx; i++) {
      CardValue* card_ptr = _hot_cache[i];
      if (card_ptr != NULL) {
        cl->do_card_ptr(card_ptr, worker_id);
      } else {
        break;
      }
    }
  }

  // The existing entries in the hot card cache, which were just refined
  // above, are discarded prior to re-enabling the cache near the end of the GC.
}

void G1HotCardCache::reset_card_counts(HeapRegion* hr) {
  _card_counts.clear_region(hr);
}
