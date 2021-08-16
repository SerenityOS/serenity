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

#include "precompiled.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/atomic.hpp"
#include "utilities/bitMap.inline.hpp"
#include "utilities/copy.hpp"
#include "utilities/debug.hpp"
#include "utilities/population_count.hpp"

STATIC_ASSERT(sizeof(BitMap::bm_word_t) == BytesPerWord); // "Implementation assumption."

typedef BitMap::bm_word_t bm_word_t;
typedef BitMap::idx_t     idx_t;

class ResourceBitMapAllocator : StackObj {
 public:
  bm_word_t* allocate(idx_t size_in_words) const {
    return NEW_RESOURCE_ARRAY(bm_word_t, size_in_words);
  }
  void free(bm_word_t* map, idx_t size_in_words) const {
    // Don't free resource allocated arrays.
  }
};

class CHeapBitMapAllocator : StackObj {
  MEMFLAGS _flags;

 public:
  CHeapBitMapAllocator(MEMFLAGS flags) : _flags(flags) {}
  bm_word_t* allocate(size_t size_in_words) const {
    return ArrayAllocator<bm_word_t>::allocate(size_in_words, _flags);
  }
  void free(bm_word_t* map, idx_t size_in_words) const {
    ArrayAllocator<bm_word_t>::free(map, size_in_words);
  }
};

class ArenaBitMapAllocator : StackObj {
  Arena* _arena;

 public:
  ArenaBitMapAllocator(Arena* arena) : _arena(arena) {}
  bm_word_t* allocate(idx_t size_in_words) const {
    return (bm_word_t*)_arena->Amalloc(size_in_words * BytesPerWord);
  }
  void free(bm_word_t* map, idx_t size_in_words) const {
    // ArenaBitMaps currently don't free memory.
  }
};

template <class Allocator>
BitMap::bm_word_t* BitMap::reallocate(const Allocator& allocator, bm_word_t* old_map, idx_t old_size_in_bits, idx_t new_size_in_bits, bool clear) {
  size_t old_size_in_words = calc_size_in_words(old_size_in_bits);
  size_t new_size_in_words = calc_size_in_words(new_size_in_bits);

  bm_word_t* map = NULL;

  if (new_size_in_words > 0) {
    map = allocator.allocate(new_size_in_words);

    if (old_map != NULL) {
      Copy::disjoint_words((HeapWord*)old_map, (HeapWord*) map,
                           MIN2(old_size_in_words, new_size_in_words));
    }

    if (clear && (new_size_in_bits > old_size_in_bits)) {
      // If old_size_in_bits is not word-aligned, then the preceeding
      // copy can include some trailing bits in the final copied word
      // that also need to be cleared.  See clear_range_within_word.
      bm_word_t mask = bit_mask(old_size_in_bits) - 1;
      map[raw_to_words_align_down(old_size_in_bits)] &= mask;
      // Clear the remaining full words.
      clear_range_of_words(map, old_size_in_words, new_size_in_words);
    }
  }

  if (old_map != NULL) {
    allocator.free(old_map, old_size_in_words);
  }

  return map;
}

template <class Allocator>
bm_word_t* BitMap::allocate(const Allocator& allocator, idx_t size_in_bits, bool clear) {
  // Reuse reallocate to ensure that the new memory is cleared.
  return reallocate(allocator, NULL, 0, size_in_bits, clear);
}

template <class Allocator>
void BitMap::free(const Allocator& allocator, bm_word_t* map, idx_t  size_in_bits) {
  bm_word_t* ret = reallocate(allocator, map, size_in_bits, 0);
  assert(ret == NULL, "Reallocate shouldn't have allocated");
}

template <class Allocator>
void BitMap::resize(const Allocator& allocator, idx_t new_size_in_bits, bool clear) {
  bm_word_t* new_map = reallocate(allocator, map(), size(), new_size_in_bits, clear);

  update(new_map, new_size_in_bits);
}

template <class Allocator>
void BitMap::initialize(const Allocator& allocator, idx_t size_in_bits, bool clear) {
  assert(map() == NULL, "precondition");
  assert(size() == 0,   "precondition");

  resize(allocator, size_in_bits, clear);
}

template <class Allocator>
void BitMap::reinitialize(const Allocator& allocator, idx_t new_size_in_bits, bool clear) {
  // Remove previous bits - no need to clear
  resize(allocator, 0, false /* clear */);

  initialize(allocator, new_size_in_bits, clear);
}

ResourceBitMap::ResourceBitMap(idx_t size_in_bits, bool clear)
    : BitMap(allocate(ResourceBitMapAllocator(), size_in_bits, clear), size_in_bits) {
}

void ResourceBitMap::resize(idx_t new_size_in_bits) {
  BitMap::resize(ResourceBitMapAllocator(), new_size_in_bits, true /* clear */);
}

void ResourceBitMap::initialize(idx_t size_in_bits) {
  BitMap::initialize(ResourceBitMapAllocator(), size_in_bits, true /* clear */);
}

void ResourceBitMap::reinitialize(idx_t size_in_bits) {
  BitMap::reinitialize(ResourceBitMapAllocator(), size_in_bits, true /* clear */);
}

ArenaBitMap::ArenaBitMap(Arena* arena, idx_t size_in_bits)
    : BitMap(allocate(ArenaBitMapAllocator(arena), size_in_bits), size_in_bits) {
}

CHeapBitMap::CHeapBitMap(idx_t size_in_bits, MEMFLAGS flags, bool clear)
    : BitMap(allocate(CHeapBitMapAllocator(flags), size_in_bits, clear), size_in_bits), _flags(flags) {
}

CHeapBitMap::~CHeapBitMap() {
  free(CHeapBitMapAllocator(_flags), map(), size());
}

void CHeapBitMap::resize(idx_t new_size_in_bits, bool clear) {
  BitMap::resize(CHeapBitMapAllocator(_flags), new_size_in_bits, clear);
}

void CHeapBitMap::initialize(idx_t size_in_bits, bool clear) {
  BitMap::initialize(CHeapBitMapAllocator(_flags), size_in_bits, clear);
}

void CHeapBitMap::reinitialize(idx_t size_in_bits, bool clear) {
  BitMap::reinitialize(CHeapBitMapAllocator(_flags), size_in_bits, clear);
}

#ifdef ASSERT
void BitMap::verify_size(idx_t size_in_bits) {
  assert(size_in_bits <= max_size_in_bits(),
         "out of bounds: " SIZE_FORMAT, size_in_bits);
}

void BitMap::verify_index(idx_t bit) const {
  assert(bit < _size,
         "BitMap index out of bounds: " SIZE_FORMAT " >= " SIZE_FORMAT,
         bit, _size);
}

void BitMap::verify_limit(idx_t bit) const {
  assert(bit <= _size,
         "BitMap limit out of bounds: " SIZE_FORMAT " > " SIZE_FORMAT,
         bit, _size);
}

void BitMap::verify_range(idx_t beg, idx_t end) const {
  assert(beg <= end,
         "BitMap range error: " SIZE_FORMAT " > " SIZE_FORMAT, beg, end);
  verify_limit(end);
}
#endif // #ifdef ASSERT

void BitMap::pretouch() {
  os::pretouch_memory(word_addr(0), word_addr(size()));
}

void BitMap::set_range_within_word(idx_t beg, idx_t end) {
  // With a valid range (beg <= end), this test ensures that end != 0, as
  // required by inverted_bit_mask_for_range.  Also avoids an unnecessary write.
  if (beg != end) {
    bm_word_t mask = inverted_bit_mask_for_range(beg, end);
    *word_addr(beg) |= ~mask;
  }
}

void BitMap::clear_range_within_word(idx_t beg, idx_t end) {
  // With a valid range (beg <= end), this test ensures that end != 0, as
  // required by inverted_bit_mask_for_range.  Also avoids an unnecessary write.
  if (beg != end) {
    bm_word_t mask = inverted_bit_mask_for_range(beg, end);
    *word_addr(beg) &= mask;
  }
}

void BitMap::par_put_range_within_word(idx_t beg, idx_t end, bool value) {
  assert(value == 0 || value == 1, "0 for clear, 1 for set");
  // With a valid range (beg <= end), this test ensures that end != 0, as
  // required by inverted_bit_mask_for_range.  Also avoids an unnecessary write.
  if (beg != end) {
    bm_word_t* pw = word_addr(beg);
    bm_word_t  w  = *pw;
    bm_word_t  mr = inverted_bit_mask_for_range(beg, end);
    bm_word_t  nw = value ? (w | ~mr) : (w & mr);
    while (true) {
      bm_word_t res = Atomic::cmpxchg(pw, w, nw);
      if (res == w) break;
      w  = res;
      nw = value ? (w | ~mr) : (w & mr);
    }
  }
}

void BitMap::set_range(idx_t beg, idx_t end) {
  verify_range(beg, end);

  idx_t beg_full_word = to_words_align_up(beg);
  idx_t end_full_word = to_words_align_down(end);

  if (beg_full_word < end_full_word) {
    // The range includes at least one full word.
    set_range_within_word(beg, bit_index(beg_full_word));
    set_range_of_words(beg_full_word, end_full_word);
    set_range_within_word(bit_index(end_full_word), end);
  } else {
    // The range spans at most 2 partial words.
    idx_t boundary = MIN2(bit_index(beg_full_word), end);
    set_range_within_word(beg, boundary);
    set_range_within_word(boundary, end);
  }
}

void BitMap::clear_range(idx_t beg, idx_t end) {
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

bool BitMap::is_small_range_of_words(idx_t beg_full_word, idx_t end_full_word) {
  // There is little point to call large version on small ranges.
  // Need to check carefully, keeping potential idx_t over/underflow in mind,
  // because beg_full_word > end_full_word can occur when beg and end are in
  // the same word.
  // The threshold should be at least one word.
  STATIC_ASSERT(small_range_words >= 1);
  return beg_full_word + small_range_words >= end_full_word;
}

void BitMap::set_large_range(idx_t beg, idx_t end) {
  verify_range(beg, end);

  idx_t beg_full_word = to_words_align_up(beg);
  idx_t end_full_word = to_words_align_down(end);

  if (is_small_range_of_words(beg_full_word, end_full_word)) {
    set_range(beg, end);
    return;
  }

  // The range includes at least one full word.
  set_range_within_word(beg, bit_index(beg_full_word));
  set_large_range_of_words(beg_full_word, end_full_word);
  set_range_within_word(bit_index(end_full_word), end);
}

void BitMap::clear_large_range(idx_t beg, idx_t end) {
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

void BitMap::at_put(idx_t offset, bool value) {
  if (value) {
    set_bit(offset);
  } else {
    clear_bit(offset);
  }
}

// Return true to indicate that this thread changed
// the bit, false to indicate that someone else did.
// In either case, the requested bit is in the
// requested state some time during the period that
// this thread is executing this call. More importantly,
// if no other thread is executing an action to
// change the requested bit to a state other than
// the one that this thread is trying to set it to,
// then the the bit is in the expected state
// at exit from this method. However, rather than
// make such a strong assertion here, based on
// assuming such constrained use (which though true
// today, could change in the future to service some
// funky parallel algorithm), we encourage callers
// to do such verification, as and when appropriate.
bool BitMap::par_at_put(idx_t bit, bool value) {
  return value ? par_set_bit(bit) : par_clear_bit(bit);
}

void BitMap::at_put_range(idx_t start_offset, idx_t end_offset, bool value) {
  if (value) {
    set_range(start_offset, end_offset);
  } else {
    clear_range(start_offset, end_offset);
  }
}

void BitMap::par_at_put_range(idx_t beg, idx_t end, bool value) {
  verify_range(beg, end);

  idx_t beg_full_word = to_words_align_up(beg);
  idx_t end_full_word = to_words_align_down(end);

  if (beg_full_word < end_full_word) {
    // The range includes at least one full word.
    par_put_range_within_word(beg, bit_index(beg_full_word), value);
    if (value) {
      set_range_of_words(beg_full_word, end_full_word);
    } else {
      clear_range_of_words(beg_full_word, end_full_word);
    }
    par_put_range_within_word(bit_index(end_full_word), end, value);
  } else {
    // The range spans at most 2 partial words.
    idx_t boundary = MIN2(bit_index(beg_full_word), end);
    par_put_range_within_word(beg, boundary, value);
    par_put_range_within_word(boundary, end, value);
  }

}

void BitMap::at_put_large_range(idx_t beg, idx_t end, bool value) {
  if (value) {
    set_large_range(beg, end);
  } else {
    clear_large_range(beg, end);
  }
}

void BitMap::par_at_put_large_range(idx_t beg, idx_t end, bool value) {
  verify_range(beg, end);

  idx_t beg_full_word = to_words_align_up(beg);
  idx_t end_full_word = to_words_align_down(end);

  if (is_small_range_of_words(beg_full_word, end_full_word)) {
    par_at_put_range(beg, end, value);
    return;
  }

  // The range includes at least one full word.
  par_put_range_within_word(beg, bit_index(beg_full_word), value);
  if (value) {
    set_large_range_of_words(beg_full_word, end_full_word);
  } else {
    clear_large_range_of_words(beg_full_word, end_full_word);
  }
  par_put_range_within_word(bit_index(end_full_word), end, value);
}

inline bm_word_t tail_mask(idx_t tail_bits) {
  assert(tail_bits != 0, "precondition"); // Works, but shouldn't be called.
  assert(tail_bits < (idx_t)BitsPerWord, "precondition");
  return (bm_word_t(1) << tail_bits) - 1;
}

// Get the low tail_bits of value, which is the last partial word of a map.
inline bm_word_t tail_of_map(bm_word_t value, idx_t tail_bits) {
  return value & tail_mask(tail_bits);
}

// Compute the new last word of a map with a non-aligned length.
// new_value has the new trailing bits of the map in the low tail_bits.
// old_value is the last word of the map, including bits beyond the end.
// Returns old_value with the low tail_bits replaced by the corresponding
// bits in new_value.
inline bm_word_t merge_tail_of_map(bm_word_t new_value,
                                   bm_word_t old_value,
                                   idx_t tail_bits) {
  bm_word_t mask = tail_mask(tail_bits);
  return (new_value & mask) | (old_value & ~mask);
}

bool BitMap::contains(const BitMap& other) const {
  assert(size() == other.size(), "must have same size");
  const bm_word_t* dest_map = map();
  const bm_word_t* other_map = other.map();
  idx_t limit = to_words_align_down(size());
  for (idx_t index = 0; index < limit; ++index) {
    // false if other bitmap has bits set which are clear in this bitmap.
    if ((~dest_map[index] & other_map[index]) != 0) return false;
  }
  idx_t rest = bit_in_word(size());
  // true unless there is a partial-word tail in which the other
  // bitmap has bits set which are clear in this bitmap.
  return (rest == 0) || tail_of_map(~dest_map[limit] & other_map[limit], rest) == 0;
}

bool BitMap::intersects(const BitMap& other) const {
  assert(size() == other.size(), "must have same size");
  const bm_word_t* dest_map = map();
  const bm_word_t* other_map = other.map();
  idx_t limit = to_words_align_down(size());
  for (idx_t index = 0; index < limit; ++index) {
    if ((dest_map[index] & other_map[index]) != 0) return true;
  }
  idx_t rest = bit_in_word(size());
  // false unless there is a partial-word tail with non-empty intersection.
  return (rest > 0) && tail_of_map(dest_map[limit] & other_map[limit], rest) != 0;
}

void BitMap::set_union(const BitMap& other) {
  assert(size() == other.size(), "must have same size");
  bm_word_t* dest_map = map();
  const bm_word_t* other_map = other.map();
  idx_t limit = to_words_align_down(size());
  for (idx_t index = 0; index < limit; ++index) {
    dest_map[index] |= other_map[index];
  }
  idx_t rest = bit_in_word(size());
  if (rest > 0) {
    bm_word_t orig = dest_map[limit];
    dest_map[limit] = merge_tail_of_map(orig | other_map[limit], orig, rest);
  }
}

void BitMap::set_difference(const BitMap& other) {
  assert(size() == other.size(), "must have same size");
  bm_word_t* dest_map = map();
  const bm_word_t* other_map = other.map();
  idx_t limit = to_words_align_down(size());
  for (idx_t index = 0; index < limit; ++index) {
    dest_map[index] &= ~other_map[index];
  }
  idx_t rest = bit_in_word(size());
  if (rest > 0) {
    bm_word_t orig = dest_map[limit];
    dest_map[limit] = merge_tail_of_map(orig & ~other_map[limit], orig, rest);
  }
}

void BitMap::set_intersection(const BitMap& other) {
  assert(size() == other.size(), "must have same size");
  bm_word_t* dest_map = map();
  const bm_word_t* other_map = other.map();
  idx_t limit = to_words_align_down(size());
  for (idx_t index = 0; index < limit; ++index) {
    dest_map[index] &= other_map[index];
  }
  idx_t rest = bit_in_word(size());
  if (rest > 0) {
    bm_word_t orig = dest_map[limit];
    dest_map[limit] = merge_tail_of_map(orig & other_map[limit], orig, rest);
  }
}

bool BitMap::set_union_with_result(const BitMap& other) {
  assert(size() == other.size(), "must have same size");
  bool changed = false;
  bm_word_t* dest_map = map();
  const bm_word_t* other_map = other.map();
  idx_t limit = to_words_align_down(size());
  for (idx_t index = 0; index < limit; ++index) {
    bm_word_t orig = dest_map[index];
    bm_word_t temp = orig | other_map[index];
    changed = changed || (temp != orig);
    dest_map[index] = temp;
  }
  idx_t rest = bit_in_word(size());
  if (rest > 0) {
    bm_word_t orig = dest_map[limit];
    bm_word_t temp = merge_tail_of_map(orig | other_map[limit], orig, rest);
    changed = changed || (temp != orig);
    dest_map[limit] = temp;
  }
  return changed;
}

bool BitMap::set_difference_with_result(const BitMap& other) {
  assert(size() == other.size(), "must have same size");
  bool changed = false;
  bm_word_t* dest_map = map();
  const bm_word_t* other_map = other.map();
  idx_t limit = to_words_align_down(size());
  for (idx_t index = 0; index < limit; ++index) {
    bm_word_t orig = dest_map[index];
    bm_word_t temp = orig & ~other_map[index];
    changed = changed || (temp != orig);
    dest_map[index] = temp;
  }
  idx_t rest = bit_in_word(size());
  if (rest > 0) {
    bm_word_t orig = dest_map[limit];
    bm_word_t temp = merge_tail_of_map(orig & ~other_map[limit], orig, rest);
    changed = changed || (temp != orig);
    dest_map[limit] = temp;
  }
  return changed;
}

bool BitMap::set_intersection_with_result(const BitMap& other) {
  assert(size() == other.size(), "must have same size");
  bool changed = false;
  bm_word_t* dest_map = map();
  const bm_word_t* other_map = other.map();
  idx_t limit = to_words_align_down(size());
  for (idx_t index = 0; index < limit; ++index) {
    bm_word_t orig = dest_map[index];
    bm_word_t temp = orig & other_map[index];
    changed = changed || (temp != orig);
    dest_map[index] = temp;
  }
  idx_t rest = bit_in_word(size());
  if (rest > 0) {
    bm_word_t orig = dest_map[limit];
    bm_word_t temp = merge_tail_of_map(orig & other_map[limit], orig, rest);
    changed = changed || (temp != orig);
    dest_map[limit] = temp;
  }
  return changed;
}

void BitMap::set_from(const BitMap& other) {
  assert(size() == other.size(), "must have same size");
  bm_word_t* dest_map = map();
  const bm_word_t* other_map = other.map();
  idx_t copy_words = to_words_align_down(size());
  Copy::disjoint_words((HeapWord*)other_map, (HeapWord*)dest_map, copy_words);
  idx_t rest = bit_in_word(size());
  if (rest > 0) {
    dest_map[copy_words] = merge_tail_of_map(other_map[copy_words],
                                             dest_map[copy_words],
                                             rest);
  }
}

bool BitMap::is_same(const BitMap& other) const {
  assert(size() == other.size(), "must have same size");
  const bm_word_t* dest_map = map();
  const bm_word_t* other_map = other.map();
  idx_t limit = to_words_align_down(size());
  for (idx_t index = 0; index < limit; ++index) {
    if (dest_map[index] != other_map[index]) return false;
  }
  idx_t rest = bit_in_word(size());
  return (rest == 0) || (tail_of_map(dest_map[limit] ^ other_map[limit], rest) == 0);
}

bool BitMap::is_full() const {
  const bm_word_t* words = map();
  idx_t limit = to_words_align_down(size());
  for (idx_t index = 0; index < limit; ++index) {
    if (~words[index] != 0) return false;
  }
  idx_t rest = bit_in_word(size());
  return (rest == 0) || (tail_of_map(~words[limit], rest) == 0);
}

bool BitMap::is_empty() const {
  const bm_word_t* words = map();
  idx_t limit = to_words_align_down(size());
  for (idx_t index = 0; index < limit; ++index) {
    if (words[index] != 0) return false;
  }
  idx_t rest = bit_in_word(size());
  return (rest == 0) || (tail_of_map(words[limit], rest) == 0);
}

void BitMap::clear_large() {
  clear_large_range_of_words(0, size_in_words());
}

BitMap::idx_t BitMap::count_one_bits_in_range_of_words(idx_t beg_full_word, idx_t end_full_word) const {
  idx_t sum = 0;
  for (idx_t i = beg_full_word; i < end_full_word; i++) {
    bm_word_t w = map()[i];
    sum += population_count(w);
  }
  return sum;
}

BitMap::idx_t BitMap::count_one_bits_within_word(idx_t beg, idx_t end) const {
  if (beg != end) {
    assert(end > beg, "must be");
    bm_word_t mask = ~inverted_bit_mask_for_range(beg, end);
    bm_word_t w = *word_addr(beg);
    w &= mask;
    return population_count(w);
  }
  return 0;
}

BitMap::idx_t BitMap::count_one_bits() const {
  return count_one_bits(0, size());
}

// Returns the number of bits set within  [beg, end).
BitMap::idx_t BitMap::count_one_bits(idx_t beg, idx_t end) const {
  verify_range(beg, end);

  idx_t beg_full_word = to_words_align_up(beg);
  idx_t end_full_word = to_words_align_down(end);

  idx_t sum = 0;

  if (beg_full_word < end_full_word) {
    // The range includes at least one full word.
    sum += count_one_bits_within_word(beg, bit_index(beg_full_word));
    sum += count_one_bits_in_range_of_words(beg_full_word, end_full_word);
    sum += count_one_bits_within_word(bit_index(end_full_word), end);
  } else {
    // The range spans at most 2 partial words.
    idx_t boundary = MIN2(bit_index(beg_full_word), end);
    sum += count_one_bits_within_word(beg, boundary);
    sum += count_one_bits_within_word(boundary, end);
  }

  assert(sum <= (beg - end), "must be");

  return sum;

}

void BitMap::print_on_error(outputStream* st, const char* prefix) const {
  st->print_cr("%s[" PTR_FORMAT ", " PTR_FORMAT ")",
      prefix, p2i(map()), p2i((char*)map() + (size() >> LogBitsPerByte)));
}

void BitMap::write_to(bm_word_t* buffer, size_t buffer_size_in_bytes) const {
  assert(buffer_size_in_bytes == size_in_bytes(), "must be");
  memcpy(buffer, _map, size_in_bytes());
}

#ifndef PRODUCT

void BitMap::print_on(outputStream* st) const {
  tty->print("Bitmap(" SIZE_FORMAT "):", size());
  for (idx_t index = 0; index < size(); index++) {
    tty->print("%c", at(index) ? '1' : '0');
  }
  tty->cr();
}

#endif
