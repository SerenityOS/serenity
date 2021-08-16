/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "memory/resourceArea.hpp"
#include "utilities/bitMap.inline.hpp"
#include "unittest.hpp"

class BitMapTest {

  template <class ResizableBitMapClass>
  static void fillBitMap(ResizableBitMapClass& map) {
    map.set_bit(1);
    map.set_bit(3);
    map.set_bit(17);
    map.set_bit(512);
  }

  template <class ResizableBitMapClass>
  static void testResize(BitMap::idx_t start_size) {
    ResourceMark rm;

    ResizableBitMapClass map(start_size);
    map.resize(BITMAP_SIZE);
    fillBitMap(map);

    ResizableBitMapClass map2(BITMAP_SIZE);
    fillBitMap(map2);
    EXPECT_TRUE(map.is_same(map2)) << "With start_size " << start_size;
  }

 public:
  const static BitMap::idx_t BITMAP_SIZE = 1024;


  template <class ResizableBitMapClass>
  static void testResizeGrow() {
    testResize<ResizableBitMapClass>(0);
    testResize<ResizableBitMapClass>(BITMAP_SIZE >> 3);
  }

  template <class ResizableBitMapClass>
  static void testResizeSame() {
    testResize<ResizableBitMapClass>(BITMAP_SIZE);
  }

  template <class ResizableBitMapClass>
  static void testResizeShrink() {
    testResize<ResizableBitMapClass>(BITMAP_SIZE * 2);
  }

  template <class InitializableBitMapClass>
  static void testInitialize() {
    ResourceMark rm;

    InitializableBitMapClass map;
    map.initialize(BITMAP_SIZE);
    fillBitMap(map);

    InitializableBitMapClass map2(BITMAP_SIZE);
    fillBitMap(map2);
    EXPECT_TRUE(map.is_same(map2));
  }


  static void testReinitialize(BitMap::idx_t init_size) {
    ResourceMark rm;

    ResourceBitMap map(init_size);
    map.reinitialize(BITMAP_SIZE);
    fillBitMap(map);

    ResourceBitMap map2(BITMAP_SIZE);
    fillBitMap(map2);
    EXPECT_TRUE(map.is_same(map2)) << "With init_size " << init_size;
  }

};

TEST_VM(BitMap, resize_grow) {
  BitMapTest::testResizeGrow<ResourceBitMap>();
  EXPECT_FALSE(HasFailure()) << "Failed on type ResourceBitMap";
  BitMapTest::testResizeGrow<CHeapBitMap>();
  EXPECT_FALSE(HasFailure()) << "Failed on type CHeapBitMap";
}

TEST_VM(BitMap, resize_shrink) {
  BitMapTest::testResizeShrink<ResourceBitMap>();
  EXPECT_FALSE(HasFailure()) << "Failed on type ResourceBitMap";
  BitMapTest::testResizeShrink<CHeapBitMap>();
  EXPECT_FALSE(HasFailure()) << "Failed on type CHeapBitMap";
}

TEST_VM(BitMap, resize_same) {
  BitMapTest::testResizeSame<ResourceBitMap>();
  EXPECT_FALSE(HasFailure()) << "Failed on type ResourceBitMap";
  BitMapTest::testResizeSame<CHeapBitMap>();
  EXPECT_FALSE(HasFailure()) << "Failed on type CHeapBitMap";
}

// Verify that when growing with clear, all added bits get cleared,
// even those corresponding to a partial word after the old size.
TEST_VM(BitMap, resize_grow_clear) {
  ResourceMark rm;
  const size_t word_size = sizeof(BitMap::bm_word_t) * BitsPerByte;
  const size_t size = 4 * word_size;
  ResourceBitMap bm(size, true /* clear */);
  bm.set_bit(size - 1);
  EXPECT_EQ(bm.count_one_bits(), size_t(1));
  // Discard the only set bit.  But it might still be "set" in the
  // partial word beyond the new size.
  bm.resize(size - word_size/2);
  EXPECT_EQ(bm.count_one_bits(), size_t(0));
  // Grow to include the previously set bit.  Verify that it ended up cleared.
  bm.resize(2 * size);
  EXPECT_EQ(bm.count_one_bits(), size_t(0));
}

TEST_VM(BitMap, initialize) {
  BitMapTest::testInitialize<ResourceBitMap>();
  EXPECT_FALSE(HasFailure()) << "Failed on type ResourceBitMap";
  BitMapTest::testInitialize<CHeapBitMap>();
  EXPECT_FALSE(HasFailure()) << "Failed on type CHeapBitMap";
}

TEST_VM(BitMap, reinitialize) {
  BitMapTest::testReinitialize(0);
  BitMapTest::testReinitialize(BitMapTest::BITMAP_SIZE >> 3);
  BitMapTest::testReinitialize(BitMapTest::BITMAP_SIZE);
}
