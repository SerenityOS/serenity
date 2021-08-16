/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "runtime/os.hpp"
#include "utilities/population_count.hpp"
#include "utilities/powerOfTwo.hpp"
#include "utilities/globalDefinitions.hpp"
#include <limits>
#include "unittest.hpp"

#define BITS_IN_BYTE_ARRAY_SIZE 256

const uint8_t test_popcnt_bitsInByte[BITS_IN_BYTE_ARRAY_SIZE] = {
        0, 1, 1, 2, 1, 2, 2, 3, 1, 2, 2, 3, 2, 3, 3, 4,
        1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5,
        1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5,
        2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
        1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5,
        2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
        2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
        3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7,
        1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5,
        2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
        2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
        3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7,
        2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
        3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7,
        3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7,
        4, 5, 5, 6, 5, 6, 6, 7, 5, 6, 6, 7, 6, 7, 7, 8
};

template <typename T>
static void sparse() {
  const T max_val = std::numeric_limits<T>::max();

  // Step through the entire input range from a random starting point,
  // verify population_count return values against the lookup table
  // approach used historically
  T step = T(1) << ((sizeof(T) * 8) - 7);

  for (T value = os::random() % step; value < max_val - step; value += step) {
    uint64_t v = (uint64_t)value;
    unsigned lookup = 0u;
    for (unsigned i = 0u; i < sizeof(T); i++) {
      lookup += test_popcnt_bitsInByte[v & 0xff];
      v >>= 8u;
    }
    EXPECT_EQ(lookup, population_count(value))
        << "value = " << value;
  }

  // Test a few edge cases
  EXPECT_EQ(0u, population_count(T(0u)))
      << "value = " << 0;
  EXPECT_EQ(1u, population_count(T(1u)))
      << "value = " << 1;
  EXPECT_EQ(1u, population_count(T(2u)))
      << "value = " << 2;
  EXPECT_EQ(T(sizeof(T) * BitsPerByte), population_count(max_val))
      << "value = " << max_val;
  EXPECT_EQ(T(sizeof(T) * BitsPerByte - 1u), population_count(T(max_val - 1u)))
      << "value = " << (max_val - 1u);
}


TEST(population_count, sparse8) {
  sparse<uint8_t>();
}
TEST(population_count, sparse16) {
  sparse<uint16_t>();
}
TEST(population_count, sparse32) {
  sparse<uint32_t>();
}
TEST(population_count, sparse64) {
  sparse<uint64_t>();
}
