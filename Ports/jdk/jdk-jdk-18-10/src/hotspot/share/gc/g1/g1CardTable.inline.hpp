/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1CARDTABLE_INLINE_HPP
#define SHARE_GC_G1_G1CARDTABLE_INLINE_HPP

#include "gc/g1/g1CardTable.hpp"

#include "gc/g1/heapRegion.hpp"

inline uint G1CardTable::region_idx_for(CardValue* p) {
  size_t const card_idx = pointer_delta(p, _byte_map, sizeof(CardValue));
  return (uint)(card_idx >> (HeapRegion::LogOfHRGrainBytes - card_shift));
}

inline bool G1CardTable::mark_clean_as_dirty(CardValue* card) {
  CardValue value = *card;
  if (value == clean_card_val()) {
    *card = dirty_card_val();
    return true;
  }
  return false;
}

inline size_t G1CardTable::mark_range_dirty(size_t start_card_index, size_t num_cards) {
  assert(is_aligned(start_card_index, sizeof(size_t)), "Start card index must be aligned.");
  assert(is_aligned(num_cards, sizeof(size_t)), "Number of cards to change must be evenly divisible.");

  size_t result = 0;

  size_t const num_chunks = num_cards / sizeof(size_t);

  size_t* cur_word = (size_t*)&_byte_map[start_card_index];
  size_t* const end_word_map = cur_word + num_chunks;
  while (cur_word < end_word_map) {
    size_t value = *cur_word;
    if (value == WordAllClean) {
      *cur_word = WordAllDirty;
      result += sizeof(value);
    } else if (value == WordAllDirty) {
      // do nothing.
    } else {
      // There is a mix of cards in there. Tread slowly.
      CardValue* cur = (CardValue*)cur_word;
      for (size_t i = 0; i < sizeof(size_t); i++) {
        CardValue value = *cur;
        if (value == clean_card_val()) {
          *cur = dirty_card_val();
          result++;
        }
        cur++;
      }
    }
    cur_word++;
  }

  return result;
}

inline void G1CardTable::change_dirty_cards_to(size_t start_card_index, size_t num_cards, CardValue which) {
  CardValue* start = &_byte_map[start_card_index];
  CardValue* const end = start + num_cards;
  while (start < end) {
    CardValue value = *start;
    assert(value == dirty_card_val(),
           "Must have been dirty %d start " PTR_FORMAT " " PTR_FORMAT, value, p2i(start), p2i(end));
    *start++ = which;
  }
}

#endif /* SHARE_GC_G1_G1CARDTABLE_INLINE_HPP */
