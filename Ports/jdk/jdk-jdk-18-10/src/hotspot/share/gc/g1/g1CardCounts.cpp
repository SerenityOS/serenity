/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/g1/g1CardCounts.hpp"
#include "gc/g1/g1CollectedHeap.inline.hpp"
#include "gc/shared/cardTableBarrierSet.hpp"
#include "services/memTracker.hpp"
#include "utilities/copy.hpp"

void G1CardCountsMappingChangedListener::on_commit(uint start_idx, size_t num_regions, bool zero_filled) {
  if (zero_filled) {
    return;
  }
  MemRegion mr(G1CollectedHeap::heap()->bottom_addr_for_region(start_idx), num_regions * HeapRegion::GrainWords);
  _counts->clear_range(mr);
}

size_t G1CardCounts::compute_size(size_t mem_region_size_in_words) {
  // We keep card counts for every card, so the size of the card counts table must
  // be the same as the card table.
  return G1CardTable::compute_size(mem_region_size_in_words);
}

size_t G1CardCounts::heap_map_factor() {
  // See G1CardCounts::compute_size() why we reuse the card table value.
  return G1CardTable::heap_map_factor();
}

void G1CardCounts::clear_range(size_t from_card_num, size_t to_card_num) {
  if (has_count_table()) {
    assert(from_card_num < to_card_num,
           "Wrong order? from: " SIZE_FORMAT ", to: " SIZE_FORMAT,
           from_card_num, to_card_num);
    Copy::fill_to_bytes(&_card_counts[from_card_num], (to_card_num - from_card_num));
  }
}

G1CardCounts::G1CardCounts(G1CollectedHeap *g1h):
  _listener(), _g1h(g1h), _ct(NULL), _card_counts(NULL), _reserved_max_card_num(0), _ct_bot(NULL) {
  _listener.set_cardcounts(this);
}

void G1CardCounts::initialize(G1RegionToSpaceMapper* mapper) {
  assert(_g1h->reserved().byte_size() > 0, "initialization order");
  assert(_g1h->capacity() == 0, "initialization order");

  if (G1ConcRSHotCardLimit > 0) {
    // The max value we can store in the counts table is
    // max_jubyte. Guarantee the value of the hot
    // threshold limit is no more than this.
    guarantee(G1ConcRSHotCardLimit <= max_jubyte, "sanity");

    _ct = _g1h->card_table();
    _ct_bot = _ct->byte_for_const(_g1h->reserved().start());

    _card_counts = (jubyte*) mapper->reserved().start();
    _reserved_max_card_num = mapper->reserved().byte_size();
    mapper->set_mapping_changed_listener(&_listener);
  }
}

uint G1CardCounts::add_card_count(CardValue* card_ptr) {
  // Returns the number of times the card has been refined.
  // If we failed to reserve/commit the counts table, return 0.
  // If card_ptr is beyond the committed end of the counts table,
  // return 0.
  // Otherwise return the actual count.
  // Unless G1ConcRSHotCardLimit has been set appropriately,
  // returning 0 will result in the card being considered
  // cold and will be refined immediately.
  uint count = 0;
  if (has_count_table()) {
    size_t card_num = ptr_2_card_num(card_ptr);
    assert(card_num < _reserved_max_card_num,
           "Card " SIZE_FORMAT " outside of card counts table (max size " SIZE_FORMAT ")",
           card_num, _reserved_max_card_num);
    count = (uint) _card_counts[card_num];
    if (count < G1ConcRSHotCardLimit) {
      _card_counts[card_num] =
        (jubyte)(MIN2((uintx)(_card_counts[card_num] + 1), G1ConcRSHotCardLimit));
    }
  }
  return count;
}

bool G1CardCounts::is_hot(uint count) {
  return (count >= G1ConcRSHotCardLimit);
}

void G1CardCounts::clear_region(HeapRegion* hr) {
  MemRegion mr(hr->bottom(), hr->end());
  clear_range(mr);
}

void G1CardCounts::clear_range(MemRegion mr) {
  if (has_count_table()) {
    const CardValue* from_card_ptr = _ct->byte_for_const(mr.start());
    // We use the last address in the range as the range could represent the
    // last region in the heap. In which case trying to find the card will be an
    // OOB access to the card table.
    const CardValue* last_card_ptr = _ct->byte_for_const(mr.last());

#ifdef ASSERT
    HeapWord* start_addr = _ct->addr_for(from_card_ptr);
    assert(start_addr == mr.start(), "MemRegion start must be aligned to a card.");
    HeapWord* last_addr = _ct->addr_for(last_card_ptr);
    assert((last_addr + G1CardTable::card_size_in_words) == mr.end(), "MemRegion end must be aligned to a card.");
#endif // ASSERT

    // Clear the counts for the (exclusive) card range.
    size_t from_card_num = ptr_2_card_num(from_card_ptr);
    size_t to_card_num = ptr_2_card_num(last_card_ptr) + 1;
    clear_range(from_card_num, to_card_num);
  }
}

class G1CardCountsClearClosure : public HeapRegionClosure {
 private:
  G1CardCounts* _card_counts;
 public:
  G1CardCountsClearClosure(G1CardCounts* card_counts) :
    HeapRegionClosure(), _card_counts(card_counts) { }


  virtual bool do_heap_region(HeapRegion* r) {
    _card_counts->clear_region(r);
    return false;
  }
};

void G1CardCounts::clear_all() {
  assert(SafepointSynchronize::is_at_safepoint(), "don't call this otherwise");
  G1CardCountsClearClosure cl(this);
  _g1h->heap_region_iterate(&cl);
}
