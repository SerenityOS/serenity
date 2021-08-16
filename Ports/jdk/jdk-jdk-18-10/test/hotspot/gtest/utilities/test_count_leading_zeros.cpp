/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "metaprogramming/isSigned.hpp"
#include "utilities/count_leading_zeros.hpp"
#include "utilities/globalDefinitions.hpp"
#include "unittest.hpp"

#include <limits>

template <typename T> void one_or_two_set_bits() {
  uint32_t bit1_pos = 0;
  uint32_t bits = sizeof(T) * BitsPerByte;
  uint32_t limit = bits - (IsSigned<T>::value ? 1 : 0);
  for (uint64_t ix = 1; bit1_pos < limit; ix = ix * 2, ++bit1_pos) {
    uint32_t bit2_pos = 0;
    for (uint64_t jx = 1; bit2_pos < limit; jx = jx * 2, ++bit2_pos) {
      T value = T(ix | jx);
      EXPECT_EQ((uint32_t)(bits - 1u - MAX2(bit1_pos, bit2_pos)), count_leading_zeros(value))
        << "value = " << value;
    }
  }
}

TEST(count_leading_zeros, one_or_two_set_bits) {
  one_or_two_set_bits<int8_t>();
  one_or_two_set_bits<int16_t>();
  one_or_two_set_bits<int32_t>();
  one_or_two_set_bits<int64_t>();
  one_or_two_set_bits<uint8_t>();
  one_or_two_set_bits<uint16_t>();
  one_or_two_set_bits<uint32_t>();
  one_or_two_set_bits<uint64_t>();
}

template <typename T> void high_zeros_low_ones() {
  uint32_t number_of_leading_zeros = (IsSigned<T>::value ? 1 : 0);
  T value = std::numeric_limits<T>::max();
  for ( ; value != 0; value >>= 1, ++number_of_leading_zeros) {
    EXPECT_EQ(number_of_leading_zeros, count_leading_zeros(value))
      << "value = " << value;
  }
}

TEST(count_leading_zeros, high_zeros_low_ones) {
  high_zeros_low_ones<int8_t>();
  high_zeros_low_ones<int16_t>();
  high_zeros_low_ones<int32_t>();
  high_zeros_low_ones<int64_t>();
  high_zeros_low_ones<uint8_t>();
  high_zeros_low_ones<uint16_t>();
  high_zeros_low_ones<uint32_t>();
  high_zeros_low_ones<uint64_t>();
}

template <typename T> void high_ones_low_zeros() {
  T value = std::numeric_limits<T>::max();

  uint32_t number_of_leading_zeros = (IsSigned<T>::value ? 1 : 0);
  for (uint64_t i = 1; value != 0; value -= i, i <<= 1) {
    EXPECT_EQ(number_of_leading_zeros, count_leading_zeros(value))
      << "value = " << value;
  }
  value = (T)(~((uint64_t)0)); // all ones
  EXPECT_EQ(0u, count_leading_zeros(value))
    << "value = " << value;
}

TEST(count_leading_zeros, high_ones_low_zeros) {
  high_ones_low_zeros<int8_t>();
  high_ones_low_zeros<int16_t>();
  high_ones_low_zeros<int32_t>();
  high_ones_low_zeros<int64_t>();
  high_ones_low_zeros<uint8_t>();
  high_ones_low_zeros<uint16_t>();
  high_ones_low_zeros<uint32_t>();
  high_ones_low_zeros<uint64_t>();
}