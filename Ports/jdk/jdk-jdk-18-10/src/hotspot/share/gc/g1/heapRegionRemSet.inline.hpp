/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_VM_GC_G1_HEAPREGIONREMSET_INLINE_HPP
#define SHARE_VM_GC_G1_HEAPREGIONREMSET_INLINE_HPP

#include "gc/g1/heapRegionRemSet.hpp"

#include "gc/g1/g1CardSet.inline.hpp"
#include "gc/g1/g1CollectedHeap.inline.hpp"
#include "gc/g1/heapRegion.inline.hpp"
#include "runtime/atomic.hpp"
#include "utilities/bitMap.inline.hpp"

void HeapRegionRemSet::set_state_empty() {
  guarantee(SafepointSynchronize::is_at_safepoint() || !is_tracked(),
            "Should only set to Untracked during safepoint but is %s.", get_state_str());
  if (_state == Untracked) {
    return;
  }
  clear_fcc();
  _state = Untracked;
}

void HeapRegionRemSet::set_state_updating() {
  guarantee(SafepointSynchronize::is_at_safepoint() && !is_tracked(),
            "Should only set to Updating from Untracked during safepoint but is %s", get_state_str());
  clear_fcc();
  _state = Updating;
}

void HeapRegionRemSet::set_state_complete() {
  clear_fcc();
  _state = Complete;
}


template <class CardOrRangeVisitor>
inline void HeapRegionRemSet::iterate_for_merge(CardOrRangeVisitor& cl) {
  _card_set.iterate_for_merge(cl);
}

void HeapRegionRemSet::split_card(OopOrNarrowOopStar from, uint& card_region, uint& card_within_region) const {
  HeapRegion* hr = G1CollectedHeap::heap()->heap_region_containing(from);
  card_region = hr->hrm_index();
  card_within_region = (uint)(pointer_delta((HeapWord*)from, hr->bottom()) >> (CardTable::card_shift - LogHeapWordSize));
}

void HeapRegionRemSet::add_reference(OopOrNarrowOopStar from, uint tid) {
  RemSetState state = _state;
  if (state == Untracked) {
    return;
  }

  uint cur_idx = _hr->hrm_index();
  uintptr_t from_card = uintptr_t(from) >> CardTable::card_shift;

  if (G1FromCardCache::contains_or_replace(tid, cur_idx, from_card)) {
    // We can't check whether the card is in the remembered set - the card container
    // may be coarsened just now.
    //assert(contains_reference(from), "We just found " PTR_FORMAT " in the FromCardCache", p2i(from));
   return;
  }

  uint card_region;
  uint card_within_region;

  split_card(from, card_region, card_within_region);

  _card_set.add_card(card_region, card_within_region);
}

bool HeapRegionRemSet::contains_reference(OopOrNarrowOopStar from) {
  uint card_region;
  uint card_within_region;

  split_card(from, card_region, card_within_region);

  return _card_set.contains_card(card_region, card_within_region);
}

void HeapRegionRemSet::print_info(outputStream* st, OopOrNarrowOopStar from) {
  uint card_region;
  uint card_within_region;

  split_card(from, card_region, card_within_region);

  _card_set.print_info(st, card_region, card_within_region);
}

#endif // SHARE_VM_GC_G1_HEAPREGIONREMSET_INLINE_HPP
