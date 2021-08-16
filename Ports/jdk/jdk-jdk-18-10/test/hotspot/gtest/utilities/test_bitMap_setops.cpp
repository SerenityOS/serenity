/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
 */

#include "precompiled.hpp"
#include "utilities/align.hpp"
#include "utilities/bitMap.inline.hpp"
#include "utilities/copy.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"
#include <stdlib.h>
#include "unittest.hpp"

typedef BitMap::idx_t idx_t;
typedef BitMap::bm_word_t bm_word_t;

inline idx_t word_align_down(idx_t bit) {
  return align_down(bit, BitsPerWord);
}

class BitMapMemory {
private:
  idx_t _words;
  bm_word_t* _memory;

public:
  BitMapMemory(idx_t bits) :
    _words(BitMap::calc_size_in_words(bits)),
    _memory(static_cast<bm_word_t*>(malloc(_words * sizeof(bm_word_t))))
  { }

  ~BitMapMemory() {
    free(_memory);
  }

  BitMapView make_view(idx_t bits, bm_word_t value) {
    vmassert(BitMap::calc_size_in_words(bits) <= _words, "invalid request");
    STATIC_ASSERT(sizeof(bm_word_t) == sizeof(HeapWord));
    Copy::fill_to_aligned_words((HeapWord*)_memory, _words, value);
    return BitMapView(_memory, bits);
  }

  bm_word_t* memory() { return _memory; }
};

const idx_t aligned_size = 4 * BitsPerWord;
const idx_t unaligned_size = aligned_size - (BitsPerWord / 2);

static bm_word_t make_even_bits() {
  bm_word_t result = 1;
  while (true) {
    bm_word_t next = (result << 2) | 1;
    if (next == result) {
      return result;
    }
    result = next;
  }
}

const bm_word_t even_bits = make_even_bits();
const bm_word_t odd_bits = ~even_bits;
const bm_word_t one_bits = ~bm_word_t(0);
const bm_word_t zero_bits = 0;

// Scoped set a clear bit and restore to clear.
class WithBitSet {
private:
  BitMap& _bm;
  idx_t _index;

public:
  WithBitSet(BitMap& bm, idx_t index) : _bm(bm), _index(index) {
    // Failure may indicate test bug; can't use ASSERT_xxx in constructor.
    EXPECT_FALSE(_bm.at(_index));
    bm.set_bit(_index);
  }

  ~WithBitSet() {
    _bm.clear_bit(_index);
  }
};

// Scoped clear a set bit and restore to set.
class WithBitClear {
private:
  BitMap& _bm;
  idx_t _index;

public:
  WithBitClear(BitMap& bm, idx_t index) : _bm(bm), _index(index) {
    // Failure may indicate test bug; can't use ASSERT_xxx in constructor.
    EXPECT_TRUE(_bm.at(_index));
    bm.clear_bit(_index);
  }

  ~WithBitClear() {
    _bm.set_bit(_index);
  }
};

//////////////////////////////////////////////////////////////////////////////
// bool is_same(const BitMap& bits);

TEST(BitMap, is_same__aligned) {
  BitMapMemory mx(aligned_size);
  BitMapMemory my(aligned_size);

  BitMapView x = mx.make_view(aligned_size, even_bits);
  BitMapView y = my.make_view(aligned_size, even_bits);
  EXPECT_TRUE(x.is_same(y));

  WithBitClear wbc(x, aligned_size / 2);
  EXPECT_FALSE(x.is_same(y));
}

TEST(BitMap, is_same__unaligned) {
  BitMapMemory mx(aligned_size);
  BitMapMemory my(aligned_size);

  BitMapView x = mx.make_view(unaligned_size, even_bits);
  BitMapView y = my.make_view(unaligned_size, even_bits);

  // Check that a difference beyond the end of x/y doesn't count.
  {
    BitMapView aligned = BitMapView(mx.memory(), aligned_size);
    const idx_t index = aligned_size - 2;
    STATIC_ASSERT(unaligned_size <= index);

    WithBitClear wbc(aligned, index);
    EXPECT_TRUE(x.is_same(y));
  }

  // Check that a difference in the final partial word does count.
  {
    idx_t index = unaligned_size - 2;
    ASSERT_LE(word_align_down(unaligned_size), index);

    WithBitClear wbc(y, index);
    EXPECT_FALSE(x.is_same(y));
  }
}

//////////////////////////////////////////////////////////////////////////////
// bool is_full();
// bool is_empty();

TEST(BitMap, is_full_or_empty__aligned) {
  BitMapMemory mx(aligned_size);

  {
    BitMapView x = mx.make_view(aligned_size, even_bits);
    EXPECT_FALSE(x.is_full());
    EXPECT_FALSE(x.is_empty());
  }

  {
    BitMapView x = mx.make_view(aligned_size, zero_bits);
    EXPECT_FALSE(x.is_full());
    EXPECT_TRUE(x.is_empty());
  }

  {
    BitMapView x = mx.make_view(aligned_size, one_bits);
    EXPECT_TRUE(x.is_full());
    EXPECT_FALSE(x.is_empty());
  }
}

TEST(BitMap, is_full__unaligned) {
  BitMapMemory mx(aligned_size);

  BitMapView x = mx.make_view(unaligned_size, one_bits);
  EXPECT_TRUE(x.is_full());

  // Check that a missing bit beyond the end doesn't count.
  {
    idx_t index = aligned_size - 1;
    BitMapView aligned = BitMapView(mx.memory(), aligned_size);

    WithBitClear wcb(aligned, index);
    EXPECT_FALSE(aligned.is_full());
    EXPECT_TRUE(x.is_full());
  }

  // Check that a missing bit in the final partial word does count.
  {
    WithBitClear wcb(x, unaligned_size - 1);
    EXPECT_FALSE(x.is_full());
  }
}

TEST(BitMap, is_empty__unaligned) {
  BitMapMemory mx(aligned_size);

  BitMapView x = mx.make_view(unaligned_size, zero_bits);
  EXPECT_TRUE(x.is_empty());

  // Check that a set bit beyond the end doesn't count.
  {
    idx_t index = aligned_size - 1;
    BitMapView aligned = BitMapView(mx.memory(), aligned_size);

    WithBitSet wbs(aligned, index);
    EXPECT_FALSE(aligned.is_empty());
    EXPECT_TRUE(x.is_empty());
  }

  // Check that a set bit in the final partial word does count.
  {
    WithBitSet wbs(x, unaligned_size - 1);
    EXPECT_FALSE(x.is_empty());
  }
}

//////////////////////////////////////////////////////////////////////////////
// bool contains(const BitMap& bits);

TEST(BitMap, contains__aligned) {
  BitMapMemory mx(aligned_size);
  BitMapMemory my(aligned_size);

  BitMapView x = mx.make_view(aligned_size, even_bits);
  BitMapView y = my.make_view(aligned_size, even_bits);
  EXPECT_TRUE(x.contains(y));

  WithBitClear wbc(x, aligned_size / 2);
  EXPECT_FALSE(x.contains(y));
}

TEST(BitMap, contains__unaligned) {
  BitMapMemory mx(aligned_size);
  BitMapMemory my(aligned_size);

  BitMapView x = mx.make_view(unaligned_size, even_bits);
  BitMapView y = my.make_view(unaligned_size, even_bits);

  // Check that a missing bit beyond the end of x doesn't count.
  {
    BitMapView aligned = BitMapView(mx.memory(), aligned_size);
    const idx_t index = aligned_size - 2;
    STATIC_ASSERT(unaligned_size <= index);

    WithBitClear wbc(aligned, index);
    EXPECT_TRUE(x.contains(y));
  }

  // Check that a missing bit in the final partial word does count.
  {
    idx_t index = unaligned_size - 2;
    ASSERT_LE(word_align_down(unaligned_size), index);

    WithBitClear wbc(x, index);
    EXPECT_FALSE(x.contains(y));
  }
}

//////////////////////////////////////////////////////////////////////////////
// bool intersects(const BitMap& bits);

TEST(BitMap, intersects__aligned) {
  BitMapMemory mx(aligned_size);
  BitMapMemory my(aligned_size);

  BitMapView x = mx.make_view(aligned_size, even_bits);
  BitMapView y = my.make_view(aligned_size, zero_bits);
  EXPECT_FALSE(x.intersects(y));

  ASSERT_TRUE(x.at(aligned_size / 2));
  WithBitSet wbs(y, aligned_size / 2);
  EXPECT_TRUE(x.intersects(y));
}

TEST(BitMap, intersects__unaligned) {
  BitMapMemory mx(aligned_size);
  BitMapMemory my(aligned_size);

  BitMapView x = mx.make_view(unaligned_size, even_bits);
  BitMapView y = my.make_view(unaligned_size, zero_bits);
  EXPECT_FALSE(x.intersects(y));

  // Check that adding a bit beyond the end of y doesn't count.
  {
    BitMapView aligned_x = BitMapView(mx.memory(), aligned_size);
    BitMapView aligned_y = BitMapView(my.memory(), aligned_size);
    const idx_t index = aligned_size - 2;
    STATIC_ASSERT(unaligned_size <= index);
    ASSERT_TRUE(aligned_x.at(index));

    WithBitSet wbs(aligned_y, index);
    EXPECT_FALSE(x.intersects(y));
  }

  // Check that adding a bit in the final partial word does count.
  {
    idx_t index = unaligned_size - 2;
    ASSERT_LE(word_align_down(unaligned_size), index);
    ASSERT_TRUE(x.at(index));

    WithBitSet wbs(y, index);
    EXPECT_TRUE(x.intersects(y));
  }
}

//////////////////////////////////////////////////////////////////////////////
// void set_from(const BitMap& bits);
// void set_union(const BitMap& bits);
// void set_difference(const BitMap& bits);
// void set_intersection(const BitMap& bits);
//
// bool set_union_with_result(const BitMap& bits);
// bool set_difference_with_result(const BitMap& bits);
// bool set_intersection_with_result(const BitMap& bits);

static void check_tail_unmodified(BitMapMemory& mem,
                                  idx_t bits,
                                  bm_word_t fill_word) {
  if (!is_aligned(bits, BitsPerWord)) {
    idx_t last_word_bit_index = word_align_down(bits);
    idx_t last_word_index = BitMap::calc_size_in_words(last_word_bit_index);
    bm_word_t last_word = mem.memory()[last_word_index];
    idx_t shift = bits - last_word_bit_index;
    EXPECT_EQ(fill_word >> shift, last_word >> shift);
  }
}

static void check_mod_setop(void (BitMap::*f)(const BitMap&),
                            idx_t bits,
                            bm_word_t wx,
                            bm_word_t wy,
                            bm_word_t wexp) {
  BitMapMemory mx(bits);
  BitMapMemory my(bits);
  BitMapMemory mexp(bits);

  BitMapView x = mx.make_view(bits, wx);
  BitMapView y = my.make_view(bits, wy);
  BitMapView exp = mexp.make_view(bits, wexp);

  (x.*f)(y);

  EXPECT_TRUE(exp.is_same(x));
  check_tail_unmodified(mx, bits, wx);
}

static void check_mod_setop_with_result(bool (BitMap::*f)(const BitMap&),
                                        idx_t bits,
                                        bm_word_t wx,
                                        bm_word_t wy,
                                        bm_word_t wexp) {
  BitMapMemory mx(bits);
  BitMapMemory my(bits);
  BitMapMemory mexp(bits);

  BitMapView x = mx.make_view(bits, wx);
  BitMapView y = my.make_view(bits, wy);
  BitMapView exp = mexp.make_view(bits, wexp);

  bool value = (x.*f)(y);
  EXPECT_EQ(value, wx != wexp);

  EXPECT_TRUE(exp.is_same(x));
  check_tail_unmodified(mx, bits, wx);
}

#define CHECK_MOD_SETOP_AUX(checker, name, x, y, exp)   \
  TEST(BitMap, name ## __ ## x ## _ ## y) {             \
    checker(&BitMap::name, aligned_size,                \
            x ## _bits, y ## _bits, exp ## _bits);      \
    checker(&BitMap::name, unaligned_size,              \
            x ## _bits, y ## _bits, exp ## _bits);      \
  }

#define CHECK_MOD_SETOP(name, x, y, exp) \
  CHECK_MOD_SETOP_AUX(check_mod_setop, name, x, y, exp)

#define CHECK_MOD_SETOP_WITH_RESULT(name, x, y, exp) \
  CHECK_MOD_SETOP_AUX(check_mod_setop_with_result, name, x, y, exp)

#define CHECK_MOD_SETOPS(name, x, y, exp)                       \
  CHECK_MOD_SETOP(name, x, y, exp)                              \
  CHECK_MOD_SETOP_WITH_RESULT(name ## _with_result, x, y, exp)

CHECK_MOD_SETOP(set_from, even, even, even)
CHECK_MOD_SETOP(set_from, even, odd, odd)
CHECK_MOD_SETOP(set_from, even, one, one)
CHECK_MOD_SETOP(set_from, even, zero, zero)

CHECK_MOD_SETOPS(set_union, even, even, even)
CHECK_MOD_SETOPS(set_union, even, odd, one)
CHECK_MOD_SETOPS(set_union, even, one, one)
CHECK_MOD_SETOPS(set_union, even, zero, even)

CHECK_MOD_SETOPS(set_difference, even, even, zero)
CHECK_MOD_SETOPS(set_difference, even, odd, even)
CHECK_MOD_SETOPS(set_difference, even, one, zero)
CHECK_MOD_SETOPS(set_difference, even, zero, even)

CHECK_MOD_SETOPS(set_intersection, even, even, even)
CHECK_MOD_SETOPS(set_intersection, even, odd, zero)
CHECK_MOD_SETOPS(set_intersection, even, one, even)
CHECK_MOD_SETOPS(set_intersection, even, zero, zero)

