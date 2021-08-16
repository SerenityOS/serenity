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

#ifndef SHARE_UTILITIES_POWEROFTWO_HPP
#define SHARE_UTILITIES_POWEROFTWO_HPP

#include "metaprogramming/enableIf.hpp"
#include "utilities/count_leading_zeros.hpp"
#include "utilities/count_trailing_zeros.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"
#include <limits>
#include <type_traits>

// Power of two convenience library.

template <typename T, ENABLE_IF(std::is_integral<T>::value)>
constexpr T max_power_of_2() {
  T max_val = std::numeric_limits<T>::max();
  return max_val - (max_val >> 1);
}

// Returns true iff there exists integer i such that (T(1) << i) == value.
template <typename T, ENABLE_IF(std::is_integral<T>::value)>
constexpr bool is_power_of_2(T value) {
  return (value > T(0)) && ((value & (value - 1)) == T(0));
}

// Log2 of a positive, integral value, i.e., largest i such that 2^i <= value
// Precondition: value > 0
template<typename T, ENABLE_IF(std::is_integral<T>::value)>
inline int log2i(T value) {
  assert(value > T(0), "value must be > 0");
  const int bits = sizeof(value) * BitsPerByte;
  return bits - count_leading_zeros(value) - 1;
}

// Log2 of positive, integral value, i.e., largest i such that 2^i <= value
// Returns -1 if value is zero
// For negative values this will return 63 for 64-bit types, 31 for
// 32-bit types, and so on.
template<typename T, ENABLE_IF(std::is_integral<T>::value)>
inline int log2i_graceful(T value) {
  if (value == 0) {
    return -1;
  }
  const int bits = sizeof(value) * BitsPerByte;
  return bits - count_leading_zeros(value) - 1;
}

// Log2 of a power of 2, i.e., i such that 2^i == value
// Preconditions: value > 0, value is a power of two
template<typename T, ENABLE_IF(std::is_integral<T>::value)>
inline int log2i_exact(T value) {
  assert(is_power_of_2(value),
         "value must be a power of 2: " UINT64_FORMAT_X,
         static_cast<uint64_t>(value));
  return count_trailing_zeros(value);
}

// Preconditions: value != 0, and the unsigned representation of value is a power of two
inline int exact_log2(intptr_t value) {
  return log2i_exact((uintptr_t)value);
}

// Preconditions: value != 0, and the unsigned representation of value is a power of two
inline int exact_log2_long(jlong value) {
  return log2i_exact((julong)value);
}

// Round down to the closest power of two less than or equal to the given value.
// precondition: value > 0.
template<typename T, ENABLE_IF(std::is_integral<T>::value)>
inline T round_down_power_of_2(T value) {
  assert(value > 0, "Invalid value");
  return T(1) << log2i(value);
}

// Round up to the closest power of two greater to or equal to the given value.
// precondition: value > 0.
// precondition: value <= maximum power of two representable by T.
template<typename T, ENABLE_IF(std::is_integral<T>::value)>
inline T round_up_power_of_2(T value) {
  assert(value > 0, "Invalid value");
  assert(value <= max_power_of_2<T>(), "Overflow");
  if (is_power_of_2(value)) {
    return value;
  }
  return T(1) << (log2i(value) + 1);
}

// Calculate the next power of two greater than the given value.
// precondition: if signed, value >= 0.
// precondition: value < maximum power of two representable by T.
template <typename T, ENABLE_IF(std::is_integral<T>::value)>
inline T next_power_of_2(T value)  {
  assert(value < std::numeric_limits<T>::max(), "Overflow");
  return round_up_power_of_2(value + 1);
}

#endif // SHARE_UTILITIES_POWEROFTWO_HPP
