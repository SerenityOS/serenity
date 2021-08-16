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

#ifndef SHARE_UTILITIES_COUNT_LEADING_ZEROS_HPP
#define SHARE_UTILITIES_COUNT_LEADING_ZEROS_HPP

#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"

// uint32_t count_leading_zeros(T x)

// Return the number of leading zeros in x, e.g. the zero-based index
// of the most significant set bit in x.  Undefined for 0.

// We implement and support variants for 8, 16, 32 and 64 bit integral types.
template <typename T, size_t n> struct CountLeadingZerosImpl;

template <typename T> unsigned count_leading_zeros(T v) {
  assert(v != 0, "precondition");
  return CountLeadingZerosImpl<T, sizeof(T)>::doit(v);
}

/*****************************************************************************
 * GCC and compatible (including Clang)
 *****************************************************************************/
#if defined(TARGET_COMPILER_gcc)

template <typename T> struct CountLeadingZerosImpl<T, 1> {
  static unsigned doit(T v) {
    return __builtin_clz((uint32_t)v & 0xFF) - 24u;
  }
};

template <typename T> struct CountLeadingZerosImpl<T, 2> {
  static unsigned doit(T v) {
    return __builtin_clz((uint32_t)v & 0xFFFF) - 16u;
  }
};

template <typename T> struct CountLeadingZerosImpl<T, 4> {
  static unsigned doit(T v) {
    return __builtin_clz(v);
  }
};

template <typename T> struct CountLeadingZerosImpl<T, 8> {
  static unsigned doit(T v) {
    return __builtin_clzll(v);
  }
};

/*****************************************************************************
 * Microsoft Visual Studio
 *****************************************************************************/
#elif defined(TARGET_COMPILER_visCPP)

#include <intrin.h>
#pragma intrinsic(_BitScanReverse)

#ifdef _LP64
#pragma intrinsic(_BitScanReverse64)
#endif

template <typename T> struct CountLeadingZerosImpl<T, 1> {
  static unsigned doit(T v) {
    unsigned long index;
    _BitScanReverse(&index, (uint32_t)v & 0xFF);
    return 7u - index;
  }
};

template <typename T> struct CountLeadingZerosImpl<T, 2> {
  static unsigned doit(T v) {
    unsigned long index;
    _BitScanReverse(&index, (uint32_t)v & 0xFFFF);
    return 15u - index;
  }
};

template <typename T> struct CountLeadingZerosImpl<T, 4> {
  static unsigned doit(T v) {
    unsigned long index;
    _BitScanReverse(&index, v);
    return 31u - index;
  }
};

template <typename T> struct CountLeadingZerosImpl<T, 8> {
  static unsigned doit(T v) {
#ifdef _LP64
    unsigned long index;
    _BitScanReverse64(&index, v);
    return 63u - index;
#else
    uint64_t high = ((uint64_t)v) >> 32ULL;
    if (high != 0) {
      return count_leading_zeros((uint32_t)high);
    } else {
      return count_leading_zeros((uint32_t)v) + 32;
    }
#endif
  }
};

/*****************************************************************************
 * IBM XL C/C++
 *****************************************************************************/
#elif defined(TARGET_COMPILER_xlc)

#include <builtins.h>

template <typename T> struct CountLeadingZerosImpl<T, 1> {
  static unsigned doit(T v) {
    return __cntlz4((uint32_t)v & 0xFF) - 24u;
  }
};

template <typename T> struct CountLeadingZerosImpl<T, 2> {
  static unsigned doit(T v) {
    return __cntlz4((uint32_t)v & 0xFFFF) - 16u;
  }
};

template <typename T> struct CountLeadingZerosImpl<T, 4> {
  static unsigned doit(T v) {
    return __cntlz4(v);
  }
};

template <typename T> struct CountLeadingZerosImpl<T, 8> {
  static unsigned doit(T v) {
    return __cntlz8(v);
  }
};

/*****************************************************************************
 * Fallback
 *****************************************************************************/
#else

inline uint32_t count_leading_zeros_32(uint32_t x) {
  assert(x != 0, "precondition");

  // Efficient and portable fallback implementation:
  // http://graphics.stanford.edu/~seander/bithacks.html#IntegerLogDeBruijn
  // - with positions xor'd by 31 to get number of leading zeros
  // rather than position of highest bit.
  static const uint32_t MultiplyDeBruijnBitPosition[32] = {
      31, 22, 30, 21, 18, 10, 29,  2, 20, 17, 15, 13, 9,  6, 28,  1,
      23, 19, 11,  3, 16, 14,  7, 24, 12,  4,  8, 25, 5, 26, 27,  0
  };

  // First round down to one less than a power of 2
  x |= x >> 1;
  x |= x >> 2;
  x |= x >> 4;
  x |= x >> 8;
  x |= x >> 16;
  // Multiply by a magic constant which ensure the highest 5 bits point to
  // the right index in the lookup table
  return MultiplyDeBruijnBitPosition[(x * 0x07c4acddu) >> 27u];
}

template <typename T> struct CountLeadingZerosImpl<T, 1> {
  static unsigned doit(T v) {
    return count_leading_zeros_32((uint32_t)v & 0xFF) - 24u;
  }
};

template <typename T> struct CountLeadingZerosImpl<T, 2> {
  static unsigned doit(T v) {
    return count_leading_zeros_32((uint32_t)v & 0xFFFF) - 16u;
  }
};

template <typename T> struct CountLeadingZerosImpl<T, 4> {
  static unsigned doit(T v) {
    return count_leading_zeros_32(v);
  }
};

template <typename T> struct CountLeadingZerosImpl<T, 8> {
  static unsigned doit(T v) {
    uint64_t high = ((uint64_t)v) >> 32ULL;
    if (high != 0) {
      return count_leading_zeros_32((uint32_t)high);
    } else {
      return count_leading_zeros_32((uint32_t)v) + 32u;
    }
  }
};

#endif

#endif // SHARE_UTILITIES_COUNT_LEADING_ZEROS_HPP
