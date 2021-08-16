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

#ifndef SHARE_VM_GC_SHENANDOAH_SHENANDOAHMARKBITMAP_HPP
#define SHARE_VM_GC_SHENANDOAH_SHENANDOAHMARKBITMAP_HPP

#include "memory/memRegion.hpp"
#include "runtime/atomic.hpp"
#include "utilities/globalDefinitions.hpp"

class ShenandoahMarkBitMap {
public:
  typedef size_t idx_t;         // Type used for bit and word indices.
  typedef uintptr_t bm_word_t;  // Element type of array that represents the
                                // bitmap, with BitsPerWord bits per element.

private:
  // Values for get_next_bit_impl flip parameter.
  static const bm_word_t find_ones_flip = 0;
  static const bm_word_t find_zeros_flip = ~(bm_word_t)0;

  int const _shift;
  MemRegion _covered;

  bm_word_t* _map;     // First word in bitmap
  idx_t      _size;    // Size of bitmap (in bits)

  // Threshold for performing small range operation, even when large range
  // operation was requested. Measured in words.
  static const size_t small_range_words = 32;

  static bool is_small_range_of_words(idx_t beg_full_word, idx_t end_full_word);

  inline size_t address_to_index(const HeapWord* addr) const;
  inline HeapWord* index_to_address(size_t offset) const;

  void check_mark(HeapWord* addr) const NOT_DEBUG_RETURN;

  // Return a mask that will select the specified bit, when applied to the word
  // containing the bit.
  static bm_word_t bit_mask(idx_t bit) { return (bm_word_t)1 << bit_in_word(bit); }

  // Return the bit number of the first bit in the specified word.
  static idx_t bit_index(idx_t word)  { return word << LogBitsPerWord; }

  // Return the position of bit within the word that contains it (e.g., if
  // bitmap words are 32 bits, return a number 0 <= n <= 31).
  static idx_t bit_in_word(idx_t bit) { return bit & (BitsPerWord - 1); }

  bm_word_t* map()                 { return _map; }
  const bm_word_t* map() const     { return _map; }
  bm_word_t map(idx_t word) const { return _map[word]; }

  // Return a pointer to the word containing the specified bit.
  bm_word_t* word_addr(idx_t bit) {
    return map() + to_words_align_down(bit);
  }

  const bm_word_t* word_addr(idx_t bit) const {
    return map() + to_words_align_down(bit);
  }

  bool at(idx_t index) const {
    verify_index(index);
    return (*word_addr(index) & bit_mask(index)) != 0;
  }

  // Assumes relevant validity checking for bit has already been done.
  static idx_t raw_to_words_align_up(idx_t bit) {
    return raw_to_words_align_down(bit + (BitsPerWord - 1));
  }

  // Assumes relevant validity checking for bit has already been done.
  static idx_t raw_to_words_align_down(idx_t bit) {
    return bit >> LogBitsPerWord;
  }

  // Word-aligns bit and converts it to a word offset.
  // precondition: bit <= size()
  idx_t to_words_align_up(idx_t bit) const {
    verify_limit(bit);
    return raw_to_words_align_up(bit);
  }

  // Word-aligns bit and converts it to a word offset.
  // precondition: bit <= size()
  inline idx_t to_words_align_down(idx_t bit) const {
    verify_limit(bit);
    return raw_to_words_align_down(bit);
  }

  // Helper for get_next_{zero,one}_bit variants.
  // - flip designates whether searching for 1s or 0s.  Must be one of
  //   find_{zeros,ones}_flip.
  // - aligned_right is true if r_index is a priori on a bm_word_t boundary.
  template<bm_word_t flip, bool aligned_right>
  inline idx_t get_next_bit_impl(idx_t l_index, idx_t r_index) const;

  inline idx_t get_next_one_offset (idx_t l_index, idx_t r_index) const;

  void clear_large_range (idx_t beg, idx_t end);

  // Verify bit is less than size().
  void verify_index(idx_t bit) const NOT_DEBUG_RETURN;
  // Verify bit is not greater than size().
  void verify_limit(idx_t bit) const NOT_DEBUG_RETURN;
  // Verify [beg,end) is a valid range, e.g. beg <= end <= size().
  void verify_range(idx_t beg, idx_t end) const NOT_DEBUG_RETURN;

public:
  static size_t compute_size(size_t heap_size);
  // Returns the amount of bytes on the heap between two marks in the bitmap.
  static size_t mark_distance();
  // Returns how many bytes (or bits) of the heap a single byte (or bit) of the
  // mark bitmap corresponds to. This is the same as the mark distance above.
  static size_t heap_map_factor() {
    return mark_distance();
  }

  ShenandoahMarkBitMap(MemRegion heap, MemRegion storage);

  // Mark word as 'strong' if it hasn't been marked strong yet.
  // Return true if the word has been marked strong, false if it has already been
  // marked strong or if another thread has beat us by marking it
  // strong.
  // Words that have been marked final before or by a concurrent thread will be
  // upgraded to strong. In this case, this method also returns true.
  inline bool mark_strong(HeapWord* w, bool& was_upgraded);

  // Mark word as 'weak' if it hasn't been marked weak or strong yet.
  // Return true if the word has been marked weak, false if it has already been
  // marked strong or weak or if another thread has beat us by marking it
  // strong or weak.
  inline bool mark_weak(HeapWord* heap_addr);

  inline bool is_marked(HeapWord* addr) const;
  inline bool is_marked_strong(HeapWord* w)  const;
  inline bool is_marked_weak(HeapWord* addr) const;

  // Return the address corresponding to the next marked bit at or after
  // "addr", and before "limit", if "limit" is non-NULL.  If there is no
  // such bit, returns "limit" if that is non-NULL, or else "endWord()".
  HeapWord* get_next_marked_addr(const HeapWord* addr,
                                 const HeapWord* limit) const;

  bm_word_t inverted_bit_mask_for_range(idx_t beg, idx_t end) const;
  void  clear_range_within_word    (idx_t beg, idx_t end);
  void clear_range (idx_t beg, idx_t end);
  void clear_range_large(MemRegion mr);

  void clear_range_of_words(idx_t beg, idx_t end);
  void clear_large_range_of_words(idx_t beg, idx_t end);
  static void clear_range_of_words(bm_word_t* map, idx_t beg, idx_t end);

};

#endif // SHARE_VM_GC_SHENANDOAH_SHENANDOAHMARKBITMAP_HPP
