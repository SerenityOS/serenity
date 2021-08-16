/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, SAP and/or its affiliates.
 *
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
#include "memory/allocation.hpp"
#include "runtime/os.hpp"
#include "utilities/bitMap.inline.hpp"
#include "utilities/ostream.hpp"
#include "unittest.hpp"

// A simple stupid bitmap to mimic BitMap operations.
class SimpleFakeBitmap {

  const int _size;
  char* _buffer;

public:

  SimpleFakeBitmap(int size) : _size(size), _buffer(NEW_C_HEAP_ARRAY(char, size, mtInternal)) {
    clear();
  }

  ~SimpleFakeBitmap() {
    FREE_C_HEAP_ARRAY(char, _buffer);
  }

  // Set or clear the specified bit.
  void set_range(int beg, int end) { ::memset(_buffer + beg, 'X', end - beg); }
  void clear_range(int beg, int end) { ::memset(_buffer + beg, ' ', end - beg); }
  void clear() { clear_range(0, _size); }

  bool at(int idx) const { return _buffer[idx] == 'X'; }

  unsigned popcnt(int beg, int end) const {
    int sum = 0;
    for (int i = beg; i < end; i ++) {
      if (at(i)) {
        sum ++;
      }
    }
    return sum;
  }

  unsigned popcnt() const { return popcnt(0, _size); }

};

#define ASSERT_POPCNT_ALL(bm, expected) \
  ASSERT_EQ(bm.count_one_bits(), (BitMap::idx_t)(expected));

#define ASSERT_POPCNT_RANGE(bm, beg, end, expected) \
  ASSERT_EQ(bm.count_one_bits(beg, end), (BitMap::idx_t)(expected));

#define ASSERT_POPCNT_ALL_CMP(bm, fake_bm) \
  ASSERT_EQ(bm.count_one_bits(), fake_bm.popcnt());

#define ASSERT_POPCNT_RANGE_CMP(bm, beg, end, fake_bm) \
  ASSERT_EQ(bm.count_one_bits(beg, end), fake_bm.popcnt(beg, end));

static void set_or_clear_random_range(BitMap& bm, SimpleFakeBitmap& fbm, int beg, int end) {
  int range = end - beg;
  if (range > 0) {
    int from = os::random() % range;
    int to = os::random() % range;
    if (from > to) {
      int s = from;
      from = to;
      to = s;
    }
    from += beg;
    to += beg;
    if ((os::random() % 10) > 5) {
      bm.set_range(from, to);
      fbm.set_range(from, to);
    } else {
      bm.clear_range(from, to);
      fbm.clear_range(from, to);
    }
  }
}

static void test_bitmap_popcnt(int bitsize) {
  CHeapBitMap bm(bitsize);
  SimpleFakeBitmap fbm(bitsize);

  ASSERT_POPCNT_ALL(bm, 0);
  ASSERT_POPCNT_RANGE(bm, 0, 0, 0);
  ASSERT_POPCNT_RANGE(bm, 0, bitsize, 0);

  const int stepsize = bitsize > 64 ? 5 : 1;

  for (int beg = 0; beg < bitsize; beg += stepsize) {
    for (int end = beg; end < bitsize; end += stepsize) {

      bm.clear();
      bm.set_range(beg, end);

      fbm.clear();
      fbm.set_range(beg, end);

      ASSERT_POPCNT_ALL_CMP(bm, fbm);

      for (int beg_query_range = 0; beg_query_range < bitsize; beg_query_range += stepsize) {
        for (int end_query_range = beg_query_range; end_query_range < bitsize; end_query_range += stepsize) {
          ASSERT_POPCNT_RANGE_CMP(bm, beg_query_range, end_query_range, fbm);

          // Clear some random ranges and retest
          for (int n = 0; n < 3; n ++) {
            set_or_clear_random_range(bm, fbm, beg, end);
            ASSERT_POPCNT_RANGE_CMP(bm, beg_query_range, end_query_range, fbm);
          }

        }
      }
    }
  }

}

TEST_VM(BitMap, popcnt_1)   { test_bitmap_popcnt(1); }
TEST_VM(BitMap, popcnt_8)   { test_bitmap_popcnt(8); }
TEST_VM(BitMap, popcnt_15)  { test_bitmap_popcnt(15); }
TEST_VM(BitMap, popcnt_19)  { test_bitmap_popcnt(17); }
TEST_VM(BitMap, popcnt_63)  { test_bitmap_popcnt(63); }
TEST_VM(BitMap, popcnt_300) { test_bitmap_popcnt(300); }

TEST_VM(BitMap, popcnt_large) {

  CHeapBitMap bm(64 * K);

  ASSERT_POPCNT_ALL(bm, 0);
  ASSERT_POPCNT_RANGE(bm, 0, 64 * K, 0);
  ASSERT_POPCNT_RANGE(bm, 47, 199, 0);

  bm.set_bit(100);

  ASSERT_POPCNT_ALL(bm, 1);
  ASSERT_POPCNT_RANGE(bm, 0, 64 * K, 1);
  ASSERT_POPCNT_RANGE(bm, 47, 199, 1);
  ASSERT_POPCNT_RANGE(bm, 199, 299, 0 );

  bm.set_range(0, 64 * K);

  ASSERT_POPCNT_ALL(bm, 64 * K);
  ASSERT_POPCNT_RANGE(bm, 0, 64 * K, 64 * K);
  ASSERT_POPCNT_RANGE(bm, 47, 199, 152);
  ASSERT_POPCNT_RANGE(bm, 199, 299, 100);

}

