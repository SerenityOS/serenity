/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_VM_GC_SHENANDOAH_SHENANDOAHMARKBITMAP_INLINE_HPP
#define SHARE_VM_GC_SHENANDOAH_SHENANDOAHMARKBITMAP_INLINE_HPP

#include "gc/shenandoah/shenandoahMarkBitMap.hpp"

#include "runtime/atomic.hpp"
#include "utilities/count_trailing_zeros.hpp"

inline size_t ShenandoahMarkBitMap::address_to_index(const HeapWord* addr) const {
  return (pointer_delta(addr, _covered.start()) << 1) >> _shift;
}

inline HeapWord* ShenandoahMarkBitMap::index_to_address(size_t offset) const {
  return _covered.start() + ((offset >> 1) << _shift);
}

inline bool ShenandoahMarkBitMap::mark_strong(HeapWord* heap_addr, bool& was_upgraded) {
  check_mark(heap_addr);

  idx_t bit = address_to_index(heap_addr);
  verify_index(bit);
  volatile bm_word_t* const addr = word_addr(bit);
  const bm_word_t mask = bit_mask(bit);
  const bm_word_t mask_weak = (bm_word_t)1 << (bit_in_word(bit) + 1);
  bm_word_t old_val = Atomic::load(addr);

  do {
    const bm_word_t new_val = old_val | mask;
    if (new_val == old_val) {
      assert(!was_upgraded, "Should be false already");
      return false;     // Someone else beat us to it.
    }
    const bm_word_t cur_val = Atomic::cmpxchg(addr, old_val, new_val, memory_order_relaxed);
    if (cur_val == old_val) {
      was_upgraded = (cur_val & mask_weak) != 0;
      return true;      // Success.
    }
    old_val = cur_val;  // The value changed, try again.
  } while (true);
}

inline bool ShenandoahMarkBitMap::mark_weak(HeapWord* heap_addr) {
  check_mark(heap_addr);

  idx_t bit = address_to_index(heap_addr);
  verify_index(bit);
  volatile bm_word_t* const addr = word_addr(bit);
  const bm_word_t mask_weak = (bm_word_t)1 << (bit_in_word(bit) + 1);
  const bm_word_t mask_strong = (bm_word_t)1 << bit_in_word(bit);
  bm_word_t old_val = Atomic::load(addr);

  do {
    if ((old_val & mask_strong) != 0) {
      return false;     // Already marked strong
    }
    const bm_word_t new_val = old_val | mask_weak;
    if (new_val == old_val) {
      return false;     // Someone else beat us to it.
    }
    const bm_word_t cur_val = Atomic::cmpxchg(addr, old_val, new_val, memory_order_relaxed);
    if (cur_val == old_val) {
      return true;      // Success.
    }
    old_val = cur_val;  // The value changed, try again.
  } while (true);
}

inline bool ShenandoahMarkBitMap::is_marked_strong(HeapWord* addr)  const {
  check_mark(addr);
  return at(address_to_index(addr));
}

inline bool ShenandoahMarkBitMap::is_marked_weak(HeapWord* addr) const {
  check_mark(addr);
  return at(address_to_index(addr) + 1);
}

inline bool ShenandoahMarkBitMap::is_marked(HeapWord* addr) const {
  check_mark(addr);
  idx_t index = address_to_index(addr);
  verify_index(index);
  bm_word_t mask = (bm_word_t)3 << bit_in_word(index);
  return (*word_addr(index) & mask) != 0;
}

template<ShenandoahMarkBitMap::bm_word_t flip, bool aligned_right>
inline ShenandoahMarkBitMap::idx_t ShenandoahMarkBitMap::get_next_bit_impl(idx_t l_index, idx_t r_index) const {
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

inline ShenandoahMarkBitMap::idx_t ShenandoahMarkBitMap::get_next_one_offset(idx_t l_offset, idx_t r_offset) const {
  return get_next_bit_impl<find_ones_flip, false>(l_offset, r_offset);
}

// Returns a bit mask for a range of bits [beg, end) within a single word.  Each
// bit in the mask is 0 if the bit is in the range, 1 if not in the range.  The
// returned mask can be used directly to clear the range, or inverted to set the
// range.  Note:  end must not be 0.
inline ShenandoahMarkBitMap::bm_word_t
ShenandoahMarkBitMap::inverted_bit_mask_for_range(idx_t beg, idx_t end) const {
  assert(end != 0, "does not work when end == 0");
  assert(beg == end || to_words_align_down(beg) == to_words_align_down(end - 1),
         "must be a single-word range");
  bm_word_t mask = bit_mask(beg) - 1;   // low (right) bits
  if (bit_in_word(end) != 0) {
    mask |= ~(bit_mask(end) - 1);       // high (left) bits
  }
  return mask;
}

inline void ShenandoahMarkBitMap::clear_range_of_words(bm_word_t* map, idx_t beg, idx_t end) {
  for (idx_t i = beg; i < end; ++i) map[i] = 0;
}

inline void ShenandoahMarkBitMap::clear_large_range_of_words(idx_t beg, idx_t end) {
  assert(beg <= end, "underflow");
  memset(_map + beg, 0, (end - beg) * sizeof(bm_word_t));
}

inline void ShenandoahMarkBitMap::clear_range_of_words(idx_t beg, idx_t end) {
  clear_range_of_words(_map, beg, end);
}


#endif // SHARE_VM_GC_SHENANDOAH_SHENANDOAHMARKBITMAP_INLINE_HPP
