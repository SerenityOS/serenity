/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

#include "precompiled.hpp"
#include "utilities/count_trailing_zeros.hpp"
#include "utilities/globalDefinitions.hpp"
#include "unittest.hpp"


template <typename T> static void test_one_or_two_set_bits() {
  unsigned i = 0;               // Position of a set bit.
  unsigned max = sizeof(T) * BitsPerByte;
  for (T ix = T(1); i < max; ix <<= 1, ++i) {
    unsigned j = 0;             // Position of a set bit.
    for (T jx = T(1); j < max; jx <<= 1, ++j) {
      T value = ix | jx;
      EXPECT_EQ(MIN2(i, j), count_trailing_zeros(value))
        << "value = " << value;
    }
  }
}

TEST(count_trailing_zeros, one_or_two_set_bits) {
  test_one_or_two_set_bits<int8_t>();
  test_one_or_two_set_bits<int16_t>();
  test_one_or_two_set_bits<int32_t>();
  test_one_or_two_set_bits<int64_t>();
  test_one_or_two_set_bits<uint8_t>();
  test_one_or_two_set_bits<uint16_t>();
  test_one_or_two_set_bits<uint32_t>();
  test_one_or_two_set_bits<uint64_t>();
}

template <typename T> static void test_high_zeros_low_ones() {
  T value = std::numeric_limits<T>::max();
  for ( ; value != 0; value >>= 1) {
    EXPECT_EQ(0u, count_trailing_zeros(value))
      << "value = " << value;
  }
}

TEST(count_trailing_zeros, high_zeros_low_ones) {
  test_high_zeros_low_ones<int8_t>();
  test_high_zeros_low_ones<int16_t>();
  test_high_zeros_low_ones<int32_t>();
  test_high_zeros_low_ones<int64_t>();
  test_high_zeros_low_ones<uint8_t>();
  test_high_zeros_low_ones<uint16_t>();
  test_high_zeros_low_ones<uint32_t>();
  test_high_zeros_low_ones<uint64_t>();
}

template <typename T> static void test_high_ones_low_zeros() {
  unsigned i = 0;               // Index of least significant set bit.
  T value = ~T(0);
  unsigned max = sizeof(T) * BitsPerByte;
  for ( ; i < max; value <<= 1, ++i) {
    EXPECT_EQ(i, count_trailing_zeros(value))
      << "value = " << value;
  }
}

TEST(count_trailing_zeros, high_ones_low_zeros) {
  test_high_ones_low_zeros<int8_t>();
  test_high_ones_low_zeros<int16_t>();
  test_high_ones_low_zeros<int32_t>();
  test_high_ones_low_zeros<int64_t>();
  test_high_ones_low_zeros<uint8_t>();
  test_high_ones_low_zeros<uint16_t>();
  test_high_ones_low_zeros<uint32_t>();
  test_high_ones_low_zeros<uint64_t>();
}
