/*
 * Copyright (c) 2018, Red Hat Inc. All rights reserved.
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
#include "utilities/bitMap.inline.hpp"
#include "unittest.hpp"

// Bitmap size should be large enough to accept large operations.
static const BitMap::idx_t BITMAP_SIZE = 8192;

// The test would like to fuzz indexes in this window. Having the fuzz
// window at bitmap word size makes sure the test would touch every combination
// of indexes (un)aligned on word boundary.
static const BitMap::idx_t FUZZ_WINDOW = sizeof(BitMap::bm_word_t) * 8;

static void verify_set(CHeapBitMap& map, BitMap::idx_t l, BitMap::idx_t r) {
  for (BitMap::idx_t c = l; c < r; c++) {
    EXPECT_TRUE(map.at(c));
  }
}

static void verify_unset(CHeapBitMap& map, BitMap::idx_t l, BitMap::idx_t r) {
  for (BitMap::idx_t c = l; c < r; c++) {
    EXPECT_FALSE(map.at(c));
  }
}

TEST(BitMap, clear_large_range) {
  CHeapBitMap map(BITMAP_SIZE);

  map.set_range(0, BITMAP_SIZE);
  verify_set(map, 0, BITMAP_SIZE);

  for (size_t size_class = 0; size_class <= BITMAP_SIZE; size_class = MAX2<size_t>(1, size_class*2)) {
    for (BitMap::idx_t l = 0; l < FUZZ_WINDOW; l++) {
      for (BitMap::idx_t tr = l; tr < FUZZ_WINDOW; tr++) {
        BitMap::idx_t r = MIN2(BITMAP_SIZE, size_class + tr); // avoid overflow

        map.clear_large_range(l, r);
        verify_unset(map, l, r);
        verify_set(map, 0, l);
        verify_set(map, r, BITMAP_SIZE);

        // Restore cleared
        map.set_range(l, r);
        verify_set(map, l, r);
      }
    }
  }
}

TEST(BitMap, set_large_range) {
  CHeapBitMap map(BITMAP_SIZE);

  map.clear();
  verify_unset(map, 0, BITMAP_SIZE);

  for (size_t size_class = 0; size_class <= BITMAP_SIZE; size_class = MAX2<size_t>(1, size_class*2)) {
    for (BitMap::idx_t l = 0; l < FUZZ_WINDOW; l++) {
      for (BitMap::idx_t tr = l; tr < FUZZ_WINDOW; tr++) {
        BitMap::idx_t r = MIN2(BITMAP_SIZE, size_class + tr); // avoid overflow

        map.set_large_range(l, r);
        verify_set(map, l, r);
        verify_unset(map, 0, l);
        verify_unset(map, r, BITMAP_SIZE);

        // Restore set
        map.clear_range(l, r);
        verify_unset(map, l, r);
      }
    }
  }
}

TEST(BitMap, par_at_put_large_range) {
  CHeapBitMap map(BITMAP_SIZE);

  map.clear();
  verify_unset(map, 0, BITMAP_SIZE);

  for (size_t size_class = 0; size_class <= BITMAP_SIZE; size_class = MAX2<size_t>(1, size_class*2)) {
    for (BitMap::idx_t l = 0; l < FUZZ_WINDOW; l++) {
      for (BitMap::idx_t tr = l; tr < FUZZ_WINDOW; tr++) {
        BitMap::idx_t r = MIN2(BITMAP_SIZE, size_class + tr); // avoid overflow

        map.par_at_put_large_range(l, r, true);
        verify_set(map, l, r);
        verify_unset(map, 0, l);
        verify_unset(map, r, BITMAP_SIZE);

        // Restore set
        map.clear_range(l, r);
        verify_unset(map, l, r);
      }
    }
  }
}
