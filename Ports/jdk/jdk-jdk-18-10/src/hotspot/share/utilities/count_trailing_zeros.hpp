/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_UTILITIES_COUNT_TRAILING_ZEROS_HPP
#define SHARE_UTILITIES_COUNT_TRAILING_ZEROS_HPP

#include "metaprogramming/enableIf.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"

// unsigned count_trailing_zeros(T x)

// Return the number of trailing zeros in x, e.g. the zero-based index
// of the least significant set bit in x.
// Precondition: x != 0.

// We implement and support variants for 8, 16, 32 and 64 bit integral types.

// Dispatch on toolchain to select implementation.

/*****************************************************************************
 * GCC and compatible (including Clang)
 *****************************************************************************/
#if defined(TARGET_COMPILER_gcc)

inline unsigned count_trailing_zeros_32(uint32_t x) {
  return __builtin_ctz(x);
}

inline unsigned count_trailing_zeros_64(uint64_t x) {
  return __builtin_ctzll(x);
}

/*****************************************************************************
 * Microsoft Visual Studio
 *****************************************************************************/
#elif defined(TARGET_COMPILER_visCPP)

#include <intrin.h>

#pragma intrinsic(_BitScanForward)
#ifdef _LP64
#pragma intrinsic(_BitScanForward64)
#endif

inline unsigned count_trailing_zeros_32(uint32_t x) {
  unsigned long index;
  _BitScanForward(&index, x);
  return index;
}

inline unsigned count_trailing_zeros_64(uint64_t x) {
  unsigned long index;
#ifdef _LP64
  _BitScanForward64(&index, x);
#else
  if (_BitScanForward(&index, static_cast<uint32_t>(x)) == 0) {
    // no bit found? If so, try the upper dword. Otherwise index already contains the result
    _BitScanForward(&index, static_cast<uint32_t>(x >> 32));
    index += 32;
  }
#endif
  return index;
}

/*****************************************************************************
 * IBM XL C/C++
 *****************************************************************************/
#elif defined(TARGET_COMPILER_xlc)

#include <builtins.h>

inline unsigned count_trailing_zeros_32(uint32_t x) {
  return __cnttz4(x);
}

inline unsigned count_trailing_zeros_64(uint64_t x) {
  return __cnttz8(x);
}

/*****************************************************************************
 * Unknown toolchain
 *****************************************************************************/
#else
#error Unknown TARGET_COMPILER

#endif // Toolchain dispatch

template<typename T,
         ENABLE_IF(std::is_integral<T>::value),
         ENABLE_IF(sizeof(T) <= sizeof(uint64_t))>
inline unsigned count_trailing_zeros(T x) {
  assert(x != 0, "precondition");
  return (sizeof(x) <= sizeof(uint32_t)) ?
         count_trailing_zeros_32(static_cast<uint32_t>(x)) :
         count_trailing_zeros_64(x);
}


#endif // SHARE_UTILITIES_COUNT_TRAILING_ZEROS_HPP
