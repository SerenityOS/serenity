/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/z/zBitMap.inline.hpp"
#include "unittest.hpp"

class ZBitMapTest : public ::testing::Test {
protected:
  static void test_set_pair_unset(size_t size, bool finalizable) {
    ZBitMap bitmap(size);

    for (BitMap::idx_t i = 0; i < size - 1; i++) {
      if ((i + 1) % BitsPerWord == 0) {
        // Can't set pairs of bits in different words.
        continue;
      }

      // ZBitMaps are not cleared when constructed.
      bitmap.clear();

      bool inc_live = false;

      bool ret = bitmap.par_set_bit_pair(i, finalizable, inc_live);
      EXPECT_TRUE(ret) << "Failed to set bit";
      EXPECT_TRUE(inc_live) << "Should have set inc_live";

      // First bit should always be set
      EXPECT_TRUE(bitmap.at(i)) << "Should be set";

      // Second bit should only be set when marking strong
      EXPECT_NE(bitmap.at(i + 1), finalizable);
    }
  }

  static void test_set_pair_set(size_t size, bool finalizable) {
    ZBitMap bitmap(size);

    for (BitMap::idx_t i = 0; i < size - 1; i++) {
      if ((i + 1) % BitsPerWord == 0) {
        // Can't set pairs of bits in different words.
        continue;
      }

      // Fill the bitmap with ones.
      bitmap.set_range(0, size);

      bool inc_live = false;

      bool ret = bitmap.par_set_bit_pair(i, finalizable, inc_live);
      EXPECT_FALSE(ret) << "Should not succeed setting bit";
      EXPECT_FALSE(inc_live) << "Should not have set inc_live";

      // Both bits were pre-set.
      EXPECT_TRUE(bitmap.at(i)) << "Should be set";
      EXPECT_TRUE(bitmap.at(i + 1)) << "Should be set";
    }
  }

  static void test_set_pair_set(bool finalizable) {
    test_set_pair_set(2,   finalizable);
    test_set_pair_set(62,  finalizable);
    test_set_pair_set(64,  finalizable);
    test_set_pair_set(66,  finalizable);
    test_set_pair_set(126, finalizable);
    test_set_pair_set(128, finalizable);
  }

  static void test_set_pair_unset(bool finalizable) {
    test_set_pair_unset(2,   finalizable);
    test_set_pair_unset(62,  finalizable);
    test_set_pair_unset(64,  finalizable);
    test_set_pair_unset(66,  finalizable);
    test_set_pair_unset(126, finalizable);
    test_set_pair_unset(128, finalizable);
  }

};

TEST_F(ZBitMapTest, test_set_pair_set) {
  test_set_pair_set(false);
  test_set_pair_set(true);
}

TEST_F(ZBitMapTest, test_set_pair_unset) {
  test_set_pair_unset(false);
  test_set_pair_unset(true);
}
