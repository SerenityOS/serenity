/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, Red Hat, Inc. and/or its affiliates.
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
#include "gc/shenandoah/shenandoahMarkBitMap.inline.hpp"
#include "gc/shenandoah/shenandoahHeap.inline.hpp"
#include "utilities/globalDefinitions.hpp"

ShenandoahMarkBitMap::ShenandoahMarkBitMap(MemRegion heap, MemRegion storage) :
  _shift(LogMinObjAlignment),
  _covered(heap),
  _map((BitMap::bm_word_t*) storage.start()),
  _size((heap.word_size() * 2) >> _shift) {
}

size_t ShenandoahMarkBitMap::compute_size(size_t heap_size) {
  return ReservedSpace::allocation_align_size_up(heap_size / mark_distance());
}

size_t ShenandoahMarkBitMap::mark_distance() {
  return MinObjAlignmentInBytes * BitsPerByte / 2;
}

HeapWord* ShenandoahMarkBitMap::get_next_marked_addr(const HeapWord* addr,
                                                     const HeapWord* limit) const {
  assert(limit != NULL, "limit must not be NULL");
  // Round addr up to a possible object boundary to be safe.
  size_t const addr_offset = address_to_index(align_up(addr, HeapWordSize << LogMinObjAlignment));
  size_t const limit_offset = address_to_index(limit);
  size_t const nextOffset = get_next_one_offset(addr_offset, limit_offset);
  return index_to_address(nextOffset);
}

void ShenandoahMarkBitMap::clear_range_within_word(idx_t beg, idx_t end) {
  // With a valid range (beg <= end), this test ensures that end != 0, as
  // required by inverted_bit_mask_for_range.  Also avoids an unnecessary write.
  if (beg != end) {
    bm_word_t mask = inverted_bit_mask_for_range(beg, end);
    *word_addr(beg) &= mask;
  }
}

void ShenandoahMarkBitMap::clear_range(idx_t beg, idx_t end) {
  verify_range(beg, end);

  idx_t beg_full_word = to_words_align_up(beg);
  idx_t end_full_word = to_words_align_down(end);

  if (beg_full_word < end_full_word) {
    // The range includes at least one full word.
    clear_range_within_word(beg, bit_index(beg_full_word));
    clear_range_of_words(beg_full_word, end_full_word);
    clear_range_within_word(bit_index(end_full_word), end);
  } else {
    // The range spans at most 2 partial words.
    idx_t boundary = MIN2(bit_index(beg_full_word), end);
    clear_range_within_word(beg, boundary);
    clear_range_within_word(boundary, end);
  }
}

bool ShenandoahMarkBitMap::is_small_range_of_words(idx_t beg_full_word, idx_t end_full_word) {
  // There is little point to call large version on small ranges.
  // Need to check carefully, keeping potential idx_t over/underflow in mind,
  // because beg_full_word > end_full_word can occur when beg and end are in
  // the same word.
  // The threshold should be at least one word.
  STATIC_ASSERT(small_range_words >= 1);
  return beg_full_word + small_range_words >= end_full_word;
}


void ShenandoahMarkBitMap::clear_large_range(idx_t beg, idx_t end) {
  verify_range(beg, end);

  idx_t beg_full_word = to_words_align_up(beg);
  idx_t end_full_word = to_words_align_down(end);

  if (is_small_range_of_words(beg_full_word, end_full_word)) {
    clear_range(beg, end);
    return;
  }

  // The range includes at least one full word.
  clear_range_within_word(beg, bit_index(beg_full_word));
  clear_large_range_of_words(beg_full_word, end_full_word);
  clear_range_within_word(bit_index(end_full_word), end);
}

void ShenandoahMarkBitMap::clear_range_large(MemRegion mr) {
  MemRegion intersection = mr.intersection(_covered);
  assert(!intersection.is_empty(),
         "Given range from " PTR_FORMAT " to " PTR_FORMAT " is completely outside the heap",
          p2i(mr.start()), p2i(mr.end()));
  // convert address range into offset range
  size_t beg = address_to_index(intersection.start());
  size_t end = address_to_index(intersection.end());
  clear_large_range(beg, end);
}

#ifdef ASSERT
void ShenandoahMarkBitMap::check_mark(HeapWord* addr) const {
  assert(ShenandoahHeap::heap()->is_in(addr),
         "Trying to access bitmap " PTR_FORMAT " for address " PTR_FORMAT " not in the heap.",
         p2i(this), p2i(addr));
}

void ShenandoahMarkBitMap::verify_index(idx_t bit) const {
  assert(bit < _size,
         "BitMap index out of bounds: " SIZE_FORMAT " >= " SIZE_FORMAT,
         bit, _size);
}

void ShenandoahMarkBitMap::verify_limit(idx_t bit) const {
  assert(bit <= _size,
         "BitMap limit out of bounds: " SIZE_FORMAT " > " SIZE_FORMAT,
         bit, _size);
}

void ShenandoahMarkBitMap::verify_range(idx_t beg, idx_t end) const {
  assert(beg <= end,
         "BitMap range error: " SIZE_FORMAT " > " SIZE_FORMAT, beg, end);
  verify_limit(end);
}
#endif
