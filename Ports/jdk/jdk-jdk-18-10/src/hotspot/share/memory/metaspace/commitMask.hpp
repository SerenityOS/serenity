/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020 SAP SE. All rights reserved.
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

#ifndef SHARE_MEMORY_METASPACE_COMMITMASK_HPP
#define SHARE_MEMORY_METASPACE_COMMITMASK_HPP

#include "utilities/bitMap.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"

class outputStream;

namespace metaspace {

// The CommitMask is a bitmask used to store the commit state of commit granules.
// It keeps one bit per granule; 1 means committed, 0 means uncommitted.

class CommitMask : public CHeapBitMap {

  const MetaWord* const _base;
  const size_t _word_size;
  const size_t _words_per_bit;

  // Given an offset, in words, into the area, return the number of the bit
  // covering it.
  static idx_t bitno_for_word_offset(size_t offset, size_t words_per_bit) {
    return offset / words_per_bit;
  }

  idx_t bitno_for_address(const MetaWord* p) const {
    // Note: we allow one-beyond since this is a typical need.
    assert(p >= _base && p <= _base + _word_size, "Invalid address");
    const size_t off = p - _base;
    return bitno_for_word_offset(off, _words_per_bit);
  }

  static idx_t mask_size(size_t word_size, size_t words_per_bit) {
    return bitno_for_word_offset(word_size, words_per_bit);
  }

  // Marks a single commit granule as committed (value == true)
  // or uncomitted (value == false) and returns
  // its prior state.
  bool mark_granule(idx_t bitno, bool value) {
    bool b = at(bitno);
    at_put(bitno, value);
    return b;
  }

#ifdef ASSERT

  // Given a pointer, check if it points into the range this bitmap covers.
  bool is_pointer_valid(const MetaWord* p) const;

  // Given a pointer, check if it points into the range this bitmap covers.
  void check_pointer(const MetaWord* p) const;

  // Given a pointer, check if it points into the range this bitmap covers,
  // and if it is aligned to commit granule border.
  void check_pointer_aligned(const MetaWord* p) const;

  // Given a range, check if it points into the range this bitmap covers,
  // and if its borders are aligned to commit granule border.
  void check_range(const MetaWord* start, size_t word_size) const;

#endif // ASSERT

public:

  // Create a commit mask covering a range [start, start + word_size).
  CommitMask(const MetaWord* start, size_t word_size);

  const MetaWord* base() const  { return _base; }
  size_t word_size() const      { return _word_size; }
  const MetaWord* end() const   { return _base + word_size(); }

  // Given an address, returns true if the address is committed, false if not.
  bool is_committed_address(const MetaWord* p) const {
    DEBUG_ONLY(check_pointer(p));
    const idx_t bitno = bitno_for_address(p);
    return at(bitno);
  }

  // Given an address range, return size, in number of words, of committed area within that range.
  size_t get_committed_size_in_range(const MetaWord* start, size_t word_size) const {
    DEBUG_ONLY(check_range(start, word_size));
    assert(word_size > 0, "zero range");
    const idx_t b1 = bitno_for_address(start);
    const idx_t b2 = bitno_for_address(start + word_size);
    const idx_t num_bits = count_one_bits(b1, b2);
    return num_bits * _words_per_bit;
  }

  // Return total committed size, in number of words.
  size_t get_committed_size() const {
    return count_one_bits() * _words_per_bit;
  }

  // Mark a whole address range [start, end) as committed.
  // Return the number of words which had already been committed before this operation.
  size_t mark_range_as_committed(const MetaWord* start, size_t word_size) {
    DEBUG_ONLY(check_range(start, word_size));
    assert(word_size > 0, "zero range");
    const idx_t b1 = bitno_for_address(start);
    const idx_t b2 = bitno_for_address(start + word_size);
    if (b1 == b2) { // Simple case, 1 granule
      bool was_committed = mark_granule(b1, true);
      return was_committed ? _words_per_bit : 0;
    }
    const idx_t one_bits_in_range_before = count_one_bits(b1, b2);
    set_range(b1, b2);
    return one_bits_in_range_before * _words_per_bit;
  }

  // Mark a whole address range [start, end) as uncommitted.
  // Return the number of words which had already been uncommitted before this operation.
  size_t mark_range_as_uncommitted(const MetaWord* start, size_t word_size) {
    DEBUG_ONLY(check_range(start, word_size));
    assert(word_size > 0, "zero range");
    const idx_t b1 = bitno_for_address(start);
    const idx_t b2 = bitno_for_address(start + word_size);
    if (b1 == b2) { // Simple case, 1 granule
      bool was_committed = mark_granule(b1, false);
      return was_committed ? 0 : _words_per_bit;
    }
    const idx_t zero_bits_in_range_before =
        (b2 - b1) - count_one_bits(b1, b2);
    clear_range(b1, b2);
    return zero_bits_in_range_before * _words_per_bit;
  }

  //// Debug stuff ////

  // Verify internals.
  DEBUG_ONLY(void verify() const;)

  void print_on(outputStream* st) const;

};

} // namespace metaspace

#endif // SHARE_MEMORY_METASPACE_COMMITMASK_HPP
