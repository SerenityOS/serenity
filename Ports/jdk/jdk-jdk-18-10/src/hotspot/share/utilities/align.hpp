/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_UTILITIES_ALIGN_HPP
#define SHARE_UTILITIES_ALIGN_HPP

#include "metaprogramming/enableIf.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/powerOfTwo.hpp"
#include <type_traits>

// Compute mask to use for aligning to or testing alignment.
// The alignment must be a power of 2. Returns alignment - 1, which is
// a mask with all bits set below alignment's single bit.
template<typename T, ENABLE_IF(std::is_integral<T>::value)>
static constexpr T alignment_mask(T alignment) {
  assert(is_power_of_2(alignment),
         "must be a power of 2: " UINT64_FORMAT, (uint64_t)alignment);
  return alignment - 1;
}

// Some "integral" constant alignments are defined via enum.
template<typename T, ENABLE_IF(std::is_enum<T>::value)>
static constexpr auto alignment_mask(T alignment) {
  return alignment_mask(static_cast<std::underlying_type_t<T>>(alignment));
}

// Align integers and check for alignment.
// The is_integral filtering here is not for disambiguation with the T*
// overloads; if those match then they are a better match.  Rather, the
// is_integral filtering is to prevent back-sliding on the use of enums
// as "integral" constants that need aligning.

template<typename T, typename A, ENABLE_IF(std::is_integral<T>::value)>
constexpr bool is_aligned(T size, A alignment) {
  return (size & alignment_mask(alignment)) == 0;
}

template<typename T, typename A, ENABLE_IF(std::is_integral<T>::value)>
constexpr T align_down(T size, A alignment) {
  // Convert mask to T before logical_not.  Otherwise, if alignment is unsigned
  // and smaller than T, the result of the logical_not will be zero-extended
  // by integral promotion, and upper bits of size will be discarded.
  T result = size & ~T(alignment_mask(alignment));
  assert(is_aligned(result, alignment),
         "must be aligned: " UINT64_FORMAT, (uint64_t)result);
  return result;
}

template<typename T, typename A, ENABLE_IF(std::is_integral<T>::value)>
constexpr T align_up(T size, A alignment) {
  T adjusted = size + alignment_mask(alignment);
  return align_down(adjusted, alignment);
}

// Align down with a lower bound. If the aligning results in 0, return 'alignment'.
template <typename T, typename A>
constexpr T align_down_bounded(T size, A alignment) {
  T aligned_size = align_down(size, alignment);
  return (aligned_size > 0) ? aligned_size : T(alignment);
}

// Align pointers and check for alignment.

template <typename T, typename A>
inline T* align_up(T* ptr, A alignment) {
  return (T*)align_up((uintptr_t)ptr, alignment);
}

template <typename T, typename A>
inline T* align_down(T* ptr, A alignment) {
  return (T*)align_down((uintptr_t)ptr, alignment);
}

template <typename T, typename A>
inline bool is_aligned(T* ptr, A alignment) {
  return is_aligned((uintptr_t)ptr, alignment);
}

// Align metaspace objects by rounding up to natural word boundary
template <typename T>
inline T align_metadata_size(T size) {
  return align_up(size, 1);
}

// Align objects in the Java Heap by rounding up their size, in HeapWord units.
template <typename T>
inline T align_object_size(T word_size) {
  return align_up(word_size, MinObjAlignment);
}

inline bool is_object_aligned(size_t word_size) {
  return is_aligned(word_size, MinObjAlignment);
}

inline bool is_object_aligned(const void* addr) {
  return is_aligned(addr, MinObjAlignmentInBytes);
}

// Pad out certain offsets to jlong alignment, in HeapWord units.
template <typename T>
constexpr T align_object_offset(T offset) {
  return align_up(offset, HeapWordsPerLong);
}

// Clamp an address to be within a specific page
// 1. If addr is on the page it is returned as is
// 2. If addr is above the page_address the start of the *next* page will be returned
// 3. Otherwise, if addr is below the page_address the start of the page will be returned
template <typename T>
inline T* clamp_address_in_page(T* addr, T* page_address, size_t page_size) {
  if (align_down(addr, page_size) == align_down(page_address, page_size)) {
    // address is in the specified page, just return it as is
    return addr;
  } else if (addr > page_address) {
    // address is above specified page, return start of next page
    return align_down(page_address, page_size) + page_size;
  } else {
    // address is below specified page, return start of page
    return align_down(page_address, page_size);
  }
}

#endif // SHARE_UTILITIES_ALIGN_HPP
