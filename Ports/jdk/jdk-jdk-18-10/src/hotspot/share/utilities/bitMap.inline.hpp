/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_UTILITIES_BITMAP_INLINE_HPP
#define SHARE_UTILITIES_BITMAP_INLINE_HPP

#include "utilities/bitMap.hpp"

#include "runtime/atomic.hpp"
#include "utilities/align.hpp"
#include "utilities/count_trailing_zeros.hpp"

inline void BitMap::set_bit(idx_t bit) {
  verify_index(bit);
  *word_addr(bit) |= bit_mask(bit);
}

inline void BitMap::clear_bit(idx_t bit) {
  verify_index(bit);
  *word_addr(bit) &= ~bit_mask(bit);
}

inline const BitMap::bm_word_t BitMap::load_word_ordered(const volatile bm_word_t* const addr, atomic_memory_order memory_order) {
  if (memory_order == memory_order_relaxed || memory_order == memory_order_release) {
    return Atomic::load(addr);
  } else {
    assert(memory_order == memory_order_acq_rel ||
           memory_order == memory_order_acquire ||
           memory_order == memory_order_conservative,
           "unexpected memory ordering");
    return Atomic::load_acquire(addr);
  }
}

inline bool BitMap::par_at(idx_t index, atomic_memory_order memory_order) const {
  verify_index(index);
  assert(memory_order == memory_order_acquire ||
         memory_order == memory_order_relaxed,
         "unexpected memory ordering");
  const volatile bm_word_t* const addr = word_addr(index);
  return (load_word_ordered(addr, memory_order) & bit_mask(index)) != 0;
}

inline bool BitMap::par_set_bit(idx_t bit, atomic_memory_order memory_order) {
  verify_index(bit);
  volatile bm_word_t* const addr = word_addr(bit);
  const bm_word_t mask = bit_mask(bit);
  bm_word_t old_val = load_word_ordered(addr, memory_order);

  do {
    const bm_word_t new_val = old_val | mask;
    if (new_val == old_val) {
      return false;     // Someone else beat us to it.
    }
    const bm_word_t cur_val = Atomic::cmpxchg(addr, old_val, new_val, memory_order);
    if (cur_val == old_val) {
      return true;      // Success.
    }
    old_val = cur_val;  // The value changed, try again.
  } while (true);
}

inline bool BitMap::par_clear_bit(idx_t bit, atomic_memory_order memory_order) {
  verify_index(bit);
  volatile bm_word_t* const addr = word_addr(bit);
  const bm_word_t mask = ~bit_mask(bit);
  bm_word_t old_val = load_word_ordered(addr, memory_order);

  do {
    const bm_word_t new_val = old_val & mask;
    if (new_val == old_val) {
      return false;     // Someone else beat us to it.
    }
    const bm_word_t cur_val = Atomic::cmpxchg(addr, old_val, new_val, memory_order);
    if (cur_val == old_val) {
      return true;      // Success.
    }
    old_val = cur_val;  // The value changed, try again.
  } while (true);
}

inline void BitMap::set_range(idx_t beg, idx_t end, RangeSizeHint hint) {
  if (hint == small_range && end - beg == 1) {
    set_bit(beg);
  } else {
    if (hint == large_range) {
      set_large_range(beg, end);
    } else {
      set_range(beg, end);
    }
  }
}

inline void BitMap::clear_range(idx_t beg, idx_t end, RangeSizeHint hint) {
  if (end - beg == 1) {
    clear_bit(beg);
  } else {
    if (hint == large_range) {
      clear_large_range(beg, end);
    } else {
      clear_range(beg, end);
    }
  }
}

inline void BitMap::par_set_range(idx_t beg, idx_t end, RangeSizeHint hint) {
  if (hint == small_range && end - beg == 1) {
    par_at_put(beg, true);
  } else {
    if (hint == large_range) {
      par_at_put_large_range(beg, end, true);
    } else {
      par_at_put_range(beg, end, true);
    }
  }
}

inline void BitMap::set_range_of_words(idx_t beg, idx_t end) {
  bm_word_t* map = _map;
  for (idx_t i = beg; i < end; ++i) map[i] = ~(bm_word_t)0;
}

inline void BitMap::clear_range_of_words(bm_word_t* map, idx_t beg, idx_t end) {
  for (idx_t i = beg; i < end; ++i) map[i] = 0;
}

inline void BitMap::clear_range_of_words(idx_t beg, idx_t end) {
  clear_range_of_words(_map, beg, end);
}

inline void BitMap::clear() {
  clear_range_of_words(0, size_in_words());
}

inline void BitMap::par_clear_range(idx_t beg, idx_t end, RangeSizeHint hint) {
  if (hint == small_range && end - beg == 1) {
    par_at_put(beg, false);
  } else {
    if (hint == large_range) {
      par_at_put_large_range(beg, end, false);
    } else {
      par_at_put_range(beg, end, false);
    }
  }
}

template<BitMap::bm_word_t flip, bool aligned_right>
inline BitMap::idx_t BitMap::get_next_bit_impl(idx_t l_index, idx_t r_index) const {
  STATIC_ASSERT(flip == find_ones_flip || flip == find_zeros_flip);
  verify_range(l_index, r_index);
  assert(!aligned_right || is_aligned(r_index, BitsPerWord), "r_index not aligned");

  // The first word often contains an interesting bit, either due to
  // density or because of features of the calling algorithm.  So it's
  // important to examine that first word with a minimum of fuss,
  // minimizing setup time for later words that will be wasted if the
  // first word is indeed interesting.

  // The benefit from aligned_right being true is relatively small.
  // It saves an operation in the setup for the word search loop.
  // It also eliminates the range check on the final result.
  // However, callers often have a comparison with r_index, and
  // inlining often allows the two comparisons to be combined; it is
  // important when !aligned_right that return paths either return
  // r_index or a value dominated by a comparison with r_index.
  // aligned_right is still helpful when the caller doesn't have a
  // range check because features of the calling algorithm guarantee
  // an interesting bit will be present.

  if (l_index < r_index) {
    // Get the word containing l_index, and shift out low bits.
    idx_t index = to_words_align_down(l_index);
    bm_word_t cword = (map(index) ^ flip) >> bit_in_word(l_index);
    if ((cword & 1) != 0) {
      // The first bit is similarly often interesting. When it matters
      // (density or features of the calling algorithm make it likely
      // the first bit is set), going straight to the next clause compares
      // poorly with doing this check first; count_trailing_zeros can be
      // relatively expensive, plus there is the additional range check.
      // But when the first bit isn't set, the cost of having tested for
      // it is relatively small compared to the rest of the search.
      return l_index;
    } else if (cword != 0) {
      // Flipped and shifted first word is non-zero.
      idx_t result = l_index + count_trailing_zeros(cword);
      if (aligned_right || (result < r_index)) return result;
      // Result is beyond range bound; return r_index.
    } else {
      // Flipped and shifted first word is zero.  Word search through
      // aligned up r_index for a non-zero flipped word.
      idx_t limit = aligned_right
        ? to_words_align_down(r_index) // Miniscule savings when aligned.
        : to_words_align_up(r_index);
      while (++index < limit) {
        cword = map(index) ^ flip;
        if (cword != 0) {
          idx_t result = bit_index(index) + count_trailing_zeros(cword);
          if (aligned_right || (result < r_index)) return result;
          // Result is beyond range bound; return r_index.
          assert((index + 1) == limit, "invariant");
          break;
        }
      }
      // No bits in range; return r_index.
    }
  }
  return r_index;
}

inline BitMap::idx_t
BitMap::get_next_one_offset(idx_t l_offset, idx_t r_offset) const {
  return get_next_bit_impl<find_ones_flip, false>(l_offset, r_offset);
}

inline BitMap::idx_t
BitMap::get_next_zero_offset(idx_t l_offset, idx_t r_offset) const {
  return get_next_bit_impl<find_zeros_flip, false>(l_offset, r_offset);
}

inline BitMap::idx_t
BitMap::get_next_one_offset_aligned_right(idx_t l_offset, idx_t r_offset) const {
  return get_next_bit_impl<find_ones_flip, true>(l_offset, r_offset);
}

inline bool BitMap::iterate(BitMapClosure* cl, idx_t beg, idx_t end) {
  for (idx_t index = beg; true; ++index) {
    index = get_next_one_offset(index, end);
    if (index >= end) {
      return true;
    } else if (!cl->do_bit(index)) {
      return false;
    }
  }
}

inline bool BitMap::iterate(BitMapClosure* cl) {
  return iterate(cl, 0, size());
}

// Returns a bit mask for a range of bits [beg, end) within a single word.  Each
// bit in the mask is 0 if the bit is in the range, 1 if not in the range.  The
// returned mask can be used directly to clear the range, or inverted to set the
// range.  Note:  end must not be 0.
inline BitMap::bm_word_t
BitMap::inverted_bit_mask_for_range(idx_t beg, idx_t end) const {
  assert(end != 0, "does not work when end == 0");
  assert(beg == end || to_words_align_down(beg) == to_words_align_down(end - 1),
         "must be a single-word range");
  bm_word_t mask = bit_mask(beg) - 1;   // low (right) bits
  if (bit_in_word(end) != 0) {
    mask |= ~(bit_mask(end) - 1);       // high (left) bits
  }
  return mask;
}

inline void BitMap::set_large_range_of_words(idx_t beg, idx_t end) {
  assert(beg <= end, "underflow");
  memset(_map + beg, ~(unsigned char)0, (end - beg) * sizeof(bm_word_t));
}

inline void BitMap::clear_large_range_of_words(idx_t beg, idx_t end) {
  assert(beg <= end, "underflow");
  memset(_map + beg, 0, (end - beg) * sizeof(bm_word_t));
}

inline bool BitMap2D::is_valid_index(idx_t slot_index, idx_t bit_within_slot_index) {
  verify_bit_within_slot_index(bit_within_slot_index);
  return (bit_index(slot_index, bit_within_slot_index) < size_in_bits());
}

inline bool BitMap2D::at(idx_t slot_index, idx_t bit_within_slot_index) const {
  verify_bit_within_slot_index(bit_within_slot_index);
  return _map.at(bit_index(slot_index, bit_within_slot_index));
}

inline void BitMap2D::set_bit(idx_t slot_index, idx_t bit_within_slot_index) {
  verify_bit_within_slot_index(bit_within_slot_index);
  _map.set_bit(bit_index(slot_index, bit_within_slot_index));
}

inline void BitMap2D::clear_bit(idx_t slot_index, idx_t bit_within_slot_index) {
  verify_bit_within_slot_index(bit_within_slot_index);
  _map.clear_bit(bit_index(slot_index, bit_within_slot_index));
}

inline void BitMap2D::at_put(idx_t slot_index, idx_t bit_within_slot_index, bool value) {
  verify_bit_within_slot_index(bit_within_slot_index);
  _map.at_put(bit_index(slot_index, bit_within_slot_index), value);
}

inline void BitMap2D::at_put_grow(idx_t slot_index, idx_t bit_within_slot_index, bool value) {
  verify_bit_within_slot_index(bit_within_slot_index);

  idx_t bit = bit_index(slot_index, bit_within_slot_index);
  if (bit >= _map.size()) {
    _map.resize(2 * MAX2(_map.size(), bit));
  }
  _map.at_put(bit, value);
}

#endif // SHARE_UTILITIES_BITMAP_INLINE_HPP
