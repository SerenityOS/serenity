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
 */

#include "precompiled.hpp"

#include "utilities/globalDefinitions.hpp"
#include "utilities/powerOfTwo.hpp"
#include <limits>
#include <type_traits>
#include "unittest.hpp"

struct StaticTestIsPowerOf2Result {
  uint64_t _value;
  int _status;            // 0: success, > 0 indicates which failure case
  constexpr StaticTestIsPowerOf2Result(uint64_t value, int status) :
    _value(value), _status(status) {}
};

// Structure copied from test_is_power_of_2 runtime test (below).
template<typename T>
static constexpr StaticTestIsPowerOf2Result static_test_is_power_of_2_aux(T v) {
  using Result = StaticTestIsPowerOf2Result;
  for ( ; v > 0; v >>= 1) {
    if (!is_power_of_2(v)) {
      return Result(v, 1);
    } else if ((v > 2) && is_power_of_2(T(v - 1))) {
      return Result(v, 2);
    } else if ((v > 1) && is_power_of_2(T(v + 1))) {
      return Result(v, 3);
    }
  }
  return Result(v, 0);
}

template<typename T>
static void static_test_is_power_of_2() {
  constexpr StaticTestIsPowerOf2Result result
    = static_test_is_power_of_2_aux(max_power_of_2<T>());

  EXPECT_EQ(0, result._status)
    << "value = " << result._value << ", status = " << result._status;
}

template <typename T> static void test_is_power_of_2() {
  EXPECT_FALSE(is_power_of_2(T(0)));
  EXPECT_FALSE(is_power_of_2(~T(0)));

  static_assert(!is_power_of_2(T(0)), "");
  static_assert(!is_power_of_2(~T(0)), "");

  // Should be false regardless of whether T is signed or unsigned.
  EXPECT_FALSE(is_power_of_2(std::numeric_limits<T>::min()));
  static_assert(!is_power_of_2(std::numeric_limits<T>::min()), "");

  // Test true
  for (T i = max_power_of_2<T>(); i > 0; i = (i >> 1)) {
    EXPECT_TRUE(is_power_of_2(i)) << "value = " << T(i);
  }

  // Test one less
  for (T i = max_power_of_2<T>(); i > 2; i = (i >> 1)) {
    EXPECT_FALSE(is_power_of_2(i - 1)) << "value = " << T(i - 1);
  }

  // Test one more
  for (T i = max_power_of_2<T>(); i > 1; i = (i >> 1)) {
    EXPECT_FALSE(is_power_of_2(i + 1)) << "value = " << T(i + 1);
  }

  static_test_is_power_of_2<T>();
}

TEST(power_of_2, is_power_of_2) {
  test_is_power_of_2<int8_t>();
  test_is_power_of_2<int16_t>();
  test_is_power_of_2<int32_t>();
  test_is_power_of_2<int64_t>();
  test_is_power_of_2<int8_t>();
  test_is_power_of_2<int16_t>();
  test_is_power_of_2<int32_t>();
  test_is_power_of_2<int64_t>();

  test_is_power_of_2<jint>();
  test_is_power_of_2<jlong>();
}

TEST(power_of_2, exact_log2) {
  {
    uintptr_t j = 1;
#ifdef _LP64
    for (int i = 0; i < 64; i++, j <<= 1) {
#else
    for (int i = 0; i < 32; i++, j <<= 1) {
#endif
      EXPECT_EQ(i, exact_log2(j));
    }
  }
  {
    julong j = 1;
    for (int i = 0; i < 64; i++, j <<= 1) {
      EXPECT_EQ(i, exact_log2_long(j));
    }
  }
}

template <typename T> void round_up_power_of_2() {
  EXPECT_EQ(round_up_power_of_2(T(1)), T(1)) << "value = " << T(1);
  EXPECT_EQ(round_up_power_of_2(T(2)), T(2)) << "value = " << T(2);
  EXPECT_EQ(round_up_power_of_2(T(3)), T(4)) << "value = " << T(3);
  EXPECT_EQ(round_up_power_of_2(T(4)), T(4)) << "value = " << T(4);
  EXPECT_EQ(round_up_power_of_2(T(5)), T(8)) << "value = " << T(5);
  EXPECT_EQ(round_up_power_of_2(T(6)), T(8)) << "value = " << T(6);
  EXPECT_EQ(round_up_power_of_2(T(7)), T(8)) << "value = " << T(7);
  EXPECT_EQ(round_up_power_of_2(T(8)), T(8)) << "value = " << T(8);
  EXPECT_EQ(round_up_power_of_2(T(9)), T(16)) << "value = " << T(9);
  EXPECT_EQ(round_up_power_of_2(T(10)), T(16)) << "value = " << T(10);

  T t_max_pow2 = max_power_of_2<T>();

  // round_up(any power of two) should return input
  for (T pow2 = T(1); pow2 < t_max_pow2; pow2 *= 2) {
    EXPECT_EQ(pow2, round_up_power_of_2(pow2))
      << "value = " << pow2;
  }
  EXPECT_EQ(round_up_power_of_2(t_max_pow2), t_max_pow2)
    << "value = " << (t_max_pow2);

  // For each pow2 gt 2, round_up(pow2 - 1) should return pow2
  for (T pow2 = T(4); pow2 < t_max_pow2; pow2 *= 2) {
    EXPECT_EQ(pow2, round_up_power_of_2(pow2 - 1))
      << "value = " << pow2;
  }
  EXPECT_EQ(round_up_power_of_2(t_max_pow2 - 1), t_max_pow2)
    << "value = " << (t_max_pow2 - 1);

}

TEST(power_of_2, round_up_power_of_2) {
  round_up_power_of_2<int8_t>();
  round_up_power_of_2<int16_t>();
  round_up_power_of_2<int32_t>();
  round_up_power_of_2<int64_t>();
  round_up_power_of_2<uint8_t>();
  round_up_power_of_2<uint16_t>();
  round_up_power_of_2<uint32_t>();
  round_up_power_of_2<uint64_t>();
}

template <typename T> void round_down_power_of_2() {
  EXPECT_EQ(round_down_power_of_2(T(1)), T(1)) << "value = " << T(1);
  EXPECT_EQ(round_down_power_of_2(T(2)), T(2)) << "value = " << T(2);
  EXPECT_EQ(round_down_power_of_2(T(3)), T(2)) << "value = " << T(3);
  EXPECT_EQ(round_down_power_of_2(T(4)), T(4)) << "value = " << T(4);
  EXPECT_EQ(round_down_power_of_2(T(5)), T(4)) << "value = " << T(5);
  EXPECT_EQ(round_down_power_of_2(T(6)), T(4)) << "value = " << T(6);
  EXPECT_EQ(round_down_power_of_2(T(7)), T(4)) << "value = " << T(7);
  EXPECT_EQ(round_down_power_of_2(T(8)), T(8)) << "value = " << T(8);
  EXPECT_EQ(round_down_power_of_2(T(9)), T(8)) << "value = " << T(9);
  EXPECT_EQ(round_down_power_of_2(T(10)), T(8)) << "value = " << T(10);

  T t_max_pow2 = max_power_of_2<T>();

  // For each pow2 >= 2:
  // - round_down(pow2) should return pow2
  // - round_down(pow2 + 1) should return pow2
  // - round_down(pow2 - 1) should return pow2 / 2
  for (T pow2 = T(2); pow2 < t_max_pow2; pow2 = pow2 * 2) {
    EXPECT_EQ(pow2, round_down_power_of_2(pow2))
      << "value = " << pow2;
    EXPECT_EQ(pow2, round_down_power_of_2(pow2 + 1))
      << "value = " << pow2;
    EXPECT_EQ(pow2 / 2, round_down_power_of_2(pow2 - 1))
      << "value = " << (pow2 / 2);
  }
  EXPECT_EQ(round_down_power_of_2(t_max_pow2), t_max_pow2)
    << "value = " << (t_max_pow2);
  EXPECT_EQ(round_down_power_of_2(t_max_pow2 + 1), t_max_pow2)
    << "value = " << (t_max_pow2 + 1);
  EXPECT_EQ(round_down_power_of_2(t_max_pow2 - 1), t_max_pow2 / 2)
    << "value = " << (t_max_pow2 - 1);
}

TEST(power_of_2, round_down_power_of_2) {
  round_down_power_of_2<int8_t>();
  round_down_power_of_2<int16_t>();
  round_down_power_of_2<int32_t>();
  round_down_power_of_2<int64_t>();
  round_down_power_of_2<uint8_t>();
  round_down_power_of_2<uint16_t>();
  round_down_power_of_2<uint32_t>();
  round_down_power_of_2<uint64_t>();
}

template <typename T> void next_power_of_2() {
  EXPECT_EQ(next_power_of_2(T(0)), T(1)) << "value = " << T(0);
  EXPECT_EQ(next_power_of_2(T(1)), T(2)) << "value = " << T(1);
  EXPECT_EQ(next_power_of_2(T(2)), T(4)) << "value = " << T(2);
  EXPECT_EQ(next_power_of_2(T(3)), T(4)) << "value = " << T(3);
  EXPECT_EQ(next_power_of_2(T(4)), T(8)) << "value = " << T(4);
  EXPECT_EQ(next_power_of_2(T(5)), T(8)) << "value = " << T(5);
  EXPECT_EQ(next_power_of_2(T(6)), T(8)) << "value = " << T(6);
  EXPECT_EQ(next_power_of_2(T(7)), T(8)) << "value = " << T(7);
  EXPECT_EQ(next_power_of_2(T(8)), T(16)) << "value = " << T(8);
  EXPECT_EQ(next_power_of_2(T(9)), T(16)) << "value = " << T(9);
  EXPECT_EQ(next_power_of_2(T(10)), T(16)) << "value = " << T(10);

  T t_max_pow2 = max_power_of_2<T>();

  // next(pow2 - 1) should return pow2
  for (T pow2 = T(1); pow2 < t_max_pow2; pow2 = pow2 * 2) {
    EXPECT_EQ(pow2, next_power_of_2(pow2 - 1))
      << "value = " << pow2 - 1;
  }
  EXPECT_EQ(next_power_of_2(t_max_pow2 - 1), t_max_pow2)
    << "value = " << (t_max_pow2 - 1);

  // next(pow2) should return pow2 * 2
  for (T pow2 = T(1); pow2 < t_max_pow2 / 2; pow2 = pow2 * 2) {
    EXPECT_EQ(pow2 * 2, next_power_of_2(pow2))
      << "value = " << pow2;
  }
}

TEST(power_of_2, next_power_of_2) {
  next_power_of_2<int8_t>();
  next_power_of_2<int16_t>();
  next_power_of_2<int32_t>();
  next_power_of_2<int64_t>();
  next_power_of_2<uint8_t>();
  next_power_of_2<uint16_t>();
  next_power_of_2<uint32_t>();
  next_power_of_2<uint64_t>();
}

TEST(power_of_2, max) {
  EXPECT_EQ(max_power_of_2<int8_t>(),  0x40);
  EXPECT_EQ(max_power_of_2<int16_t>(), 0x4000);
  EXPECT_EQ(max_power_of_2<int32_t>(), 0x40000000);
  EXPECT_EQ(max_power_of_2<int64_t>(), CONST64(0x4000000000000000));
  EXPECT_EQ(max_power_of_2<uint8_t>(),  0x80u);
  EXPECT_EQ(max_power_of_2<uint16_t>(), 0x8000u);
  EXPECT_EQ(max_power_of_2<uint32_t>(), 0x80000000u);
  EXPECT_EQ(max_power_of_2<uint64_t>(), UCONST64(0x8000000000000000));
}

template <typename T, ENABLE_IF(std::is_integral<T>::value)>
void check_log2i_variants_for(T dummy) {
  int limit = sizeof(T) * BitsPerByte;
  if (std::is_signed<T>::value) {
    T min = std::numeric_limits<T>::min();
    EXPECT_EQ(limit - 1, log2i_graceful(min));
    EXPECT_EQ(limit - 1, log2i_graceful((T)-1));
    limit--;
  }
  {
    // Test log2i_graceful handles 0 input
    EXPECT_EQ(-1, log2i_graceful(T(0)));
  }
  {
    // Test the all-1s bit patterns
    T var = 1;
    for (int i = 0; i < limit; i++, var = (var << 1) | 1) {
      EXPECT_EQ(i, log2i(var));
    }
  }
  {
    // Test the powers of 2 and powers + 1
    T var = 1;
    for (int i = 0; i < limit; i++, var <<= 1) {
      EXPECT_EQ(i, log2i(var));
      EXPECT_EQ(i, log2i_graceful(var));
      EXPECT_EQ(i, log2i_exact(var));
      EXPECT_EQ(i, log2i(var | 1));
    }
  }
}

TEST(power_of_2, log2i) {
  check_log2i_variants_for((uintptr_t)0);
  check_log2i_variants_for((intptr_t)0);
  check_log2i_variants_for((julong)0);
  check_log2i_variants_for((int)0);
  check_log2i_variants_for((jint)0);
  check_log2i_variants_for((uint)0);
  check_log2i_variants_for((jlong)0);
}
