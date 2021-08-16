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

#ifndef SHARE_GC_G1_G1CARDSETCONTAINERS_INLINE_HPP
#define SHARE_GC_G1_G1CARDSETCONTAINERS_INLINE_HPP

#include "gc/g1/g1CardSetContainers.hpp"
#include "gc/g1/g1GCPhaseTimes.hpp"

inline G1CardSetInlinePtr::CardSetPtr G1CardSetInlinePtr::merge(CardSetPtr orig_value, uint card_in_region, uint idx, uint bits_per_card) {
  assert((idx & (SizeFieldMask >> SizeFieldPos)) == idx, "Index %u too large to fit into size field", idx);
  assert(card_in_region < ((uint)1 << bits_per_card), "Card %u too large to fit into card value field", card_in_region);

  uint8_t card_pos = card_pos_for(idx, bits_per_card);
  assert(card_pos + bits_per_card < BitsInValue, "Putting card at pos %u with %u bits would extend beyond pointer", card_pos, bits_per_card);

  // Check that we do not touch any fields we do not own.
  uintptr_t mask = ((((uintptr_t)1 << bits_per_card) - 1) << card_pos);
  assert(((uintptr_t)orig_value & mask) == 0, "The bits in the new range should be empty; orig_value " PTR_FORMAT " mask " PTR_FORMAT, p2i(orig_value), mask);

  uintptr_t value = ((uintptr_t)(idx + 1) << SizeFieldPos) | ((uintptr_t)card_in_region << card_pos);
  uintptr_t res = (((uintptr_t)orig_value & ~SizeFieldMask) | value);
  return (CardSetPtr)res;
}

inline G1AddCardResult G1CardSetInlinePtr::add(uint card_idx, uint bits_per_card, uint max_cards_in_inline_ptr) {
  assert(_value_addr != nullptr, "No value address available, cannot add to set.");

  while (true) {
    uint num_elems = num_cards_in(_value);
    // Check if the card is already stored in the pointer.
    if (contains(card_idx, bits_per_card)) {
      return Found;
    }
    // Check if there is actually enough space.
    if (num_elems >= max_cards_in_inline_ptr) {
      return Overflow;
    }
    CardSetPtr new_value = merge(_value, card_idx, num_elems, bits_per_card);
    CardSetPtr old_value = Atomic::cmpxchg(_value_addr, _value, new_value, memory_order_relaxed);
    if (_value == old_value) {
      return Added;
    }
    // Update values and retry.
    _value = old_value;
    // The value of the pointer may have changed to something different than
    // an inline card set. Exit then instead of overwriting.
    if (G1CardSet::card_set_type(_value) != G1CardSet::CardSetInlinePtr) {
      return Overflow;
    }
  }
}

inline bool G1CardSetInlinePtr::contains(uint card_idx, uint bits_per_card) {
  uint num_elems = num_cards_in(_value);
  uintptr_t const card_mask = (1 << bits_per_card) - 1;

  uintptr_t value = ((uintptr_t)_value) >> card_pos_for(0, bits_per_card);
  // Check if the card is already stored in the pointer.
  for (uint cur_idx = 0; cur_idx < num_elems; cur_idx++) {
    if ((value & card_mask) == card_idx) {
      return true;
    }
    value >>= bits_per_card;
  }
  return false;
}

template <class CardVisitor>
inline void G1CardSetInlinePtr::iterate(CardVisitor& found, uint bits_per_card) {
  uint const num_elems = num_cards_in(_value);
  uintptr_t const card_mask = (1 << bits_per_card) - 1;

  uintptr_t value = ((uintptr_t)_value) >> card_pos_for(0, bits_per_card);
  for (uint cur_idx = 0; cur_idx < num_elems; cur_idx++) {
    found(value & card_mask);
    value >>= bits_per_card;
  }
}

inline bool G1CardSetContainer::try_increment_refcount() {
  uintptr_t old_value = refcount();
  while (true) {
    if (old_value < 3 || (old_value & 0x1) == 0) {  // reclaimed,  reference counts are odd numbers starting at 3
      return false; // dead, can't revive.
    }

    uintptr_t new_value = old_value + 2;
    uintptr_t ref_count = Atomic::cmpxchg(&_ref_count, old_value, new_value);
    if (ref_count == old_value) {
      return true;
    }
    old_value = ref_count;
  }
}

inline uintptr_t G1CardSetContainer::decrement_refcount() {
  uintptr_t old_value = refcount();
  assert((old_value & 0x1) != 0 && old_value >= 3, "precondition");
  return Atomic::sub(&_ref_count, 2u);
}

inline G1CardSetArray::G1CardSetArray(uint card_in_region, EntryCountType num_elems) :
  G1CardSetContainer(),
  _size(num_elems),
  _num_entries(1) {
  assert(_size > 0, "CardSetArray of size 0 not supported.");
  assert(_size < LockBitMask, "Only support CardSetArray of size %u or smaller.", LockBitMask - 1);
  _data[0] = card_in_region;
}

inline G1CardSetArray::G1CardSetArrayLocker::G1CardSetArrayLocker(EntryCountType volatile* value) :
  _value(value),
  _success(false) {
  SpinYield s;
  EntryCountType original_value = (*_value) & EntryMask;
  while (true) {
    EntryCountType old_value = Atomic::cmpxchg(_value,
                                               original_value,
                                               (EntryCountType)(original_value | LockBitMask));
    if (old_value == original_value) {
      // Succeeded locking the array.
      _original_value = original_value;
      break;
    }
    // Failed. Retry (with the lock bit stripped again).
    original_value = old_value & EntryMask;
    s.wait();
  }
}

inline G1AddCardResult G1CardSetArray::add(uint card_idx) {
  assert(card_idx < (1u << (sizeof(_data[0]) * BitsPerByte)),
         "Card index %u does not fit card element.", card_idx);
  EntryCountType num_entries = Atomic::load_acquire(&_num_entries) & EntryMask;
  EntryCountType idx = 0;
  for (; idx < num_entries; idx++) {
    if (_data[idx] == card_idx) {
      return Found;
    }
  }

  // Since we did not find the card, lock.
  G1CardSetArrayLocker x(&_num_entries);

  // Reload number of entries from the G1CardSetArrayLocker as it might have changed.
  // It already read the actual value with the necessary synchronization.
  num_entries = x.num_entries();
  // Look if the elements added while waiting for the lock are the same as our card.
  for (; idx < num_entries; idx++) {
    if (_data[idx] == card_idx) {
      return Found;
    }
  }

  // Check if there is space left.
  if (num_entries == _size) {
    return Overflow;
  }

  _data[num_entries] = card_idx;

  x.inc_num_entries();

  return Added;
}

inline bool G1CardSetArray::contains(uint card_idx) {
  EntryCountType num_entries = Atomic::load_acquire(&_num_entries) & EntryMask;

  for (EntryCountType idx = 0; idx < num_entries; idx++) {
    if (_data[idx] == card_idx) {
      return true;
    }
  }
  return false;
}

template <class CardVisitor>
void G1CardSetArray::iterate(CardVisitor& found) {
  EntryCountType num_entries = Atomic::load_acquire(&_num_entries) & EntryMask;
  for (EntryCountType idx = 0; idx < num_entries; idx++) {
    found(_data[idx]);
  }
}

inline G1CardSetBitMap::G1CardSetBitMap(uint card_in_region, uint size_in_bits) :
  G1CardSetContainer(), _num_bits_set(1) {
  assert(size_in_bits % (sizeof(_bits[0]) * BitsPerByte) == 0,
         "Size %u should be aligned to bitmap word size.", size_in_bits);
  BitMapView bm(_bits, size_in_bits);
  bm.clear();
  bm.set_bit(card_in_region);
}

inline G1AddCardResult G1CardSetBitMap::add(uint card_idx, size_t threshold, size_t size_in_bits) {
  BitMapView bm(_bits, size_in_bits);
  if (_num_bits_set >= threshold) {
    return bm.at(card_idx) ? Found : Overflow;
  }
  if (bm.par_set_bit(card_idx)) {
    Atomic::inc(&_num_bits_set, memory_order_relaxed);
    return Added;
  }
  return Found;
}

template <class CardVisitor>
inline void G1CardSetBitMap::iterate(CardVisitor& found, size_t size_in_bits, uint offset) {
  BitMapView bm(_bits, size_in_bits);
  BitMap::idx_t idx = bm.get_next_one_offset(0);
  while (idx != size_in_bits) {
    found((offset | (uint)idx));
    idx = bm.get_next_one_offset(idx + 1);
  }
}

inline G1CardSetHowl::G1CardSetHowl(EntryCountType card_in_region, G1CardSetConfiguration* config) :
  G1CardSetContainer(),
  _num_entries((config->num_cards_in_array() + 1)) /* Card Transfer will not increment _num_entries */ {
  EntryCountType num_buckets = config->num_buckets_in_howl();
  EntryCountType bucket = config->howl_bucket_index(card_in_region);
  for (uint i = 0; i < num_buckets; ++i) {
    _buckets[i] = G1CardSetInlinePtr();
    if (i == bucket) {
      G1CardSetInlinePtr value(&_buckets[i], _buckets[i]);
      value.add(card_in_region, config->inline_ptr_bits_per_card(), config->num_cards_in_inline_ptr());
    }
  }
}

inline bool G1CardSetHowl::contains(uint card_idx, G1CardSetConfiguration* config) {
  EntryCountType bucket = config->howl_bucket_index(card_idx);
  CardSetPtr* array_entry = get_card_set_addr(bucket);
  CardSetPtr card_set = Atomic::load_acquire(array_entry);

  switch (G1CardSet::card_set_type(card_set)) {
    case G1CardSet::CardSetArrayOfCards : {
      return G1CardSet::card_set_ptr<G1CardSetArray>(card_set)->contains(card_idx);
    }
    case G1CardSet::CardSetBitMap: {
      uint card_offset = config->howl_bitmap_offset(card_idx);
      return G1CardSet::card_set_ptr<G1CardSetBitMap>(card_set)->contains(card_offset, config->num_cards_in_howl_bitmap());
    }
    case G1CardSet::CardSetInlinePtr: {
      G1CardSetInlinePtr ptr(card_set);
      return ptr.contains(card_idx, config->inline_ptr_bits_per_card());
    }
    case G1CardSet::CardSetHowl: {// Fullcard set entry
      assert(card_set == G1CardSet::FullCardSet, "Must be");
      return true;
    }
  }
  return false;
}

template <class CardOrRangeVisitor>
inline void G1CardSetHowl::iterate(CardOrRangeVisitor& found, G1CardSetConfiguration* config) {
  for (uint i = 0; i < config->num_buckets_in_howl(); ++i) {
    iterate_cardset(_buckets[i], i, found, config);
  }
}

template <class CardSetPtrVisitor>
inline void G1CardSetHowl::iterate(CardSetPtrVisitor& found, uint num_card_sets) {
  for (uint i = 0; i < num_card_sets; ++i) {
    found(&_buckets[i]);
  }
}

template <class CardOrRangeVisitor>
inline void G1CardSetHowl::iterate_cardset(CardSetPtr const card_set, uint index, CardOrRangeVisitor& found, G1CardSetConfiguration* config) {
  switch (G1CardSet::card_set_type(card_set)) {
    case G1CardSet::CardSetInlinePtr: {
      if (found.start_iterate(G1GCPhaseTimes::MergeRSHowlInline)) {
        G1CardSetInlinePtr ptr(card_set);
        ptr.iterate(found, config->inline_ptr_bits_per_card());
      }
      return;
    }
    case G1CardSet::CardSetArrayOfCards : {
      if (found.start_iterate(G1GCPhaseTimes::MergeRSHowlArrayOfCards)) {
        G1CardSet::card_set_ptr<G1CardSetArray>(card_set)->iterate(found);
      }
      return;
    }
    case G1CardSet::CardSetBitMap: {
      if (found.start_iterate(G1GCPhaseTimes::MergeRSHowlBitmap)) {
        uint offset = index << config->log2_num_cards_in_howl_bitmap();
        G1CardSet::card_set_ptr<G1CardSetBitMap>(card_set)->iterate(found, config->num_cards_in_howl_bitmap(), offset);
      }
      return;
    }
    case G1CardSet::CardSetHowl: { // actually FullCardSet
      if (found.start_iterate(G1GCPhaseTimes::MergeRSHowlFull)) {
        assert(card_set == G1CardSet::FullCardSet, "Must be");
        uint offset = index << config->log2_num_cards_in_howl_bitmap();
        for (uint i = 0; i < config->max_cards_in_region(); i++) {
          found((offset | (uint)i));
        }
      }
      return;
    }
  }
}

inline G1CardSetHowl::EntryCountType G1CardSetHowl::num_buckets(size_t size_in_bits, size_t num_cards_in_array, size_t max_num_buckets) {
  size_t size_bitmap_bytes = BitMap::calc_size_in_words(size_in_bits) * BytesPerWord;
  // Ensure that in the worst case arrays consume half the memory size
  // of storing the entire bitmap
  size_t max_size_arrays_bytes = size_bitmap_bytes / 2;
  size_t size_array_bytes = num_cards_in_array * sizeof(G1CardSetArray::EntryDataType);
  size_t num_arrays = max_size_arrays_bytes / size_array_bytes;
  // We use shifts and masks for indexing the array. So round down to the next
  // power of two to not use more than expected memory.
  num_arrays = round_down_power_of_2(MAX2((size_t)1, MIN2(num_arrays, max_num_buckets)));
  return (EntryCountType)num_arrays;
}

#endif // SHARE_GC_G1_G1CARDSETCONTAINERS_INLINE_HPP
