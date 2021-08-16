/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/g1/g1BiasedArray.hpp"
#include "unittest.hpp"

class TestMappedArray : public G1BiasedMappedArray<int> {
public:
  virtual int default_value() const {
    return 0xBAADBABE;
  }
  int* my_address_mapped_to(HeapWord* address) {
    return address_mapped_to(address);
  }
};

TEST_VM(G1BiasedArray, simple) {
  const size_t REGION_SIZE_IN_WORDS = 512;
  const size_t NUM_REGIONS = 20;
  // Any value that is non-zero
  HeapWord* fake_heap =
          (HeapWord*) LP64_ONLY(0xBAAA00000) NOT_LP64(0xBA000000);

  TestMappedArray array;
  MemRegion range(fake_heap, fake_heap + REGION_SIZE_IN_WORDS * NUM_REGIONS);
  array.initialize(range, REGION_SIZE_IN_WORDS * HeapWordSize);
  const int DEFAULT_VALUE = array.default_value();

  // Check address calculation (bounds)
  ASSERT_EQ(fake_heap, array.bottom_address_mapped())
          << "bottom mapped address should be "
          << p2i(array.bottom_address_mapped())
          << ", but is "
          << p2i(fake_heap);
  ASSERT_EQ(fake_heap + REGION_SIZE_IN_WORDS * NUM_REGIONS,
          array.end_address_mapped());

  int* bottom = array.my_address_mapped_to(fake_heap);
  ASSERT_EQ((void*) bottom, (void*) array.base());
  int* end = array.my_address_mapped_to(fake_heap +
          REGION_SIZE_IN_WORDS * NUM_REGIONS);
  ASSERT_EQ((void*) end, (void*) (array.base() + array.length()));
  // The entire array should contain default value elements
  for (int* current = bottom; current < end; current++) {
    ASSERT_EQ(DEFAULT_VALUE, *current);
  }

  // Test setting values in the table
  HeapWord* region_start_address =
          fake_heap + REGION_SIZE_IN_WORDS * (NUM_REGIONS / 2);
  HeapWord* region_end_address =
          fake_heap + (REGION_SIZE_IN_WORDS * (NUM_REGIONS / 2) +
          REGION_SIZE_IN_WORDS - 1);

  // Set/get by address tests: invert some value; first retrieve one
  int actual_value = array.get_by_index(NUM_REGIONS / 2);
  array.set_by_index(NUM_REGIONS / 2, ~actual_value);
  // Get the same value by address, should correspond to the start of the "region"
  int value = array.get_by_address(region_start_address);
  ASSERT_EQ(value, ~actual_value);
  // Get the same value by address, at one HeapWord before the start
  value = array.get_by_address(region_start_address - 1);
  ASSERT_EQ(DEFAULT_VALUE, value);
  // Get the same value by address, at the end of the "region"
  value = array.get_by_address(region_end_address);
  ASSERT_EQ(value, ~actual_value);
  // Make sure the next value maps to another index
  value = array.get_by_address(region_end_address + 1);
  ASSERT_EQ(DEFAULT_VALUE, value);

  // Reset the value in the array
  array.set_by_address(region_start_address +
          (region_end_address - region_start_address) / 2,
          actual_value);

  // The entire array should have the default value again
  for (int* current = bottom; current < end; current++) {
    ASSERT_EQ(DEFAULT_VALUE, *current);
  }

  // Set/get by index tests: invert some value
  size_t index = NUM_REGIONS / 2;
  actual_value = array.get_by_index(index);
  array.set_by_index(index, ~actual_value);

  value = array.get_by_index(index);
  ASSERT_EQ(~actual_value, value);

  value = array.get_by_index(index - 1);
  ASSERT_EQ(DEFAULT_VALUE, value);

  value = array.get_by_index(index + 1);
  ASSERT_EQ(DEFAULT_VALUE, value);

  array.set_by_index(0, 0);
  value = array.get_by_index(0);
  ASSERT_EQ(0, value);

  array.set_by_index(array.length() - 1, 0);
  value = array.get_by_index(array.length() - 1);
  ASSERT_EQ(0, value);

  array.set_by_index(index, 0);

  // The array should have three zeros, and default values otherwise
  size_t num_zeros = 0;
  for (int* current = bottom; current < end; current++) {
    ASSERT_TRUE(*current == DEFAULT_VALUE || *current == 0);
    if (*current == 0) {
      num_zeros++;
    }
  }
  ASSERT_EQ((size_t) 3, num_zeros);
}

