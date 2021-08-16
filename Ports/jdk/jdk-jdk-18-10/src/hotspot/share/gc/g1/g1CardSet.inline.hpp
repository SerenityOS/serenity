/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1CARDSET_INLINE_HPP
#define SHARE_GC_G1_G1CARDSET_INLINE_HPP

#include "gc/g1/g1CardSet.hpp"
#include "gc/g1/g1CardSetContainers.inline.hpp"
#include "gc/g1/g1GCPhaseTimes.hpp"
#include "runtime/atomic.hpp"
#include "logging/log.hpp"

template <class T>
inline T* G1CardSet::card_set_ptr(CardSetPtr ptr) {
  return (T*)strip_card_set_type(ptr);
}

inline G1CardSet::CardSetPtr G1CardSet::make_card_set_ptr(void* value, uintptr_t type) {
  assert(card_set_type(value) == 0, "Given ptr " PTR_FORMAT " already has type bits set", p2i(value));
  return (CardSetPtr)((uintptr_t)value | type);
}

template <class CardOrRangeVisitor>
inline void G1CardSet::iterate_cards_or_ranges_in_container(CardSetPtr const card_set, CardOrRangeVisitor& found) {
  switch (card_set_type(card_set)) {
    case CardSetInlinePtr: {
      if (found.start_iterate(G1GCPhaseTimes::MergeRSMergedInline)) {
        G1CardSetInlinePtr ptr(card_set);
        ptr.iterate(found, _config->inline_ptr_bits_per_card());
      }
      return;
    }
    case CardSetArrayOfCards : {
      if (found.start_iterate(G1GCPhaseTimes::MergeRSMergedArrayOfCards)) {
        card_set_ptr<G1CardSetArray>(card_set)->iterate(found);
      }
      return;
    }
    case CardSetBitMap: {
      // There is no first-level bitmap spanning the whole area.
      ShouldNotReachHere();
      return;
    }
    case CardSetHowl: {
      assert(card_set_type(FullCardSet) == CardSetHowl, "Must be");
      if (card_set == FullCardSet) {
        if (found.start_iterate(G1GCPhaseTimes::MergeRSMergedFull)) {
          found(0, _config->max_cards_in_region());
        }
        return;
      }
      if (found.start_iterate(G1GCPhaseTimes::MergeRSMergedHowl)) {
        card_set_ptr<G1CardSetHowl>(card_set)->iterate(found, _config);
      }
      return;
    }
  }
  log_error(gc)("Unkown card set type %u", card_set_type(card_set));
  ShouldNotReachHere();
}

template <typename Closure>
class G1ContainerCardsOrRanges {
  Closure& _iter;
  uint _region_idx;

public:
  G1ContainerCardsOrRanges(Closure& iter, uint region_idx) : _iter(iter), _region_idx(region_idx) { }

  bool start_iterate(uint tag) {
    return _iter.start_iterate(tag, _region_idx);
  }

  void operator()(uint card_idx) {
    _iter.do_card(card_idx);
  }

  void operator()(uint card_idx, uint length) {
    _iter.do_card_range(card_idx, length);
  }
};

template <typename Closure, template <typename> class CardOrRanges>
class G1CardSetMergeCardIterator : public G1CardSet::G1CardSetPtrIterator {
  G1CardSet* _card_set;
  Closure& _iter;

public:

  G1CardSetMergeCardIterator(G1CardSet* card_set, Closure& iter) : _card_set(card_set), _iter(iter) { }

  void do_cardsetptr(uint region_idx, size_t num_occupied, G1CardSet::CardSetPtr card_set) override {
    CardOrRanges<Closure> cl(_iter, region_idx);
    _card_set->iterate_cards_or_ranges_in_container(card_set, cl);
  }
};

template <class CardOrRangeVisitor>
inline void G1CardSet::iterate_for_merge(CardOrRangeVisitor& cl) {
  G1CardSetMergeCardIterator<CardOrRangeVisitor, G1ContainerCardsOrRanges> cl2(this, cl);
  iterate_containers(&cl2, true /* at_safepoint */);
}

#endif // SHARE_GC_G1_G1CARDSET_INLINE_HPP
