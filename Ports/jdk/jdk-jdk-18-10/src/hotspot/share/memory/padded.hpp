/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_MEMORY_PADDED_HPP
#define SHARE_MEMORY_PADDED_HPP

#include "memory/allocation.hpp"
#include "utilities/align.hpp"
#include "utilities/globalDefinitions.hpp"

// Bytes needed to pad type to avoid cache-line sharing; alignment should be the
// expected cache line size (a power of two).  The first addend avoids sharing
// when the start address is not a multiple of alignment; the second maintains
// alignment of starting addresses that happen to be a multiple.
#define PADDING_SIZE(type, alignment)                           \
  ((alignment) + align_up(sizeof(type), (alignment)))

// Templates to create a subclass padded to avoid cache line sharing.  These are
// effective only when applied to derived-most (leaf) classes.

// When no args are passed to the base ctor.
template <class T, size_t alignment = DEFAULT_CACHE_LINE_SIZE>
class Padded : public T {
 private:
  char _pad_buf_[PADDING_SIZE(T, alignment)];
};

// When either 0 or 1 args may be passed to the base ctor.
template <class T, typename Arg1T, size_t alignment = DEFAULT_CACHE_LINE_SIZE>
class Padded01 : public T {
 public:
  Padded01(): T() { }
  Padded01(Arg1T arg1): T(arg1) { }
 private:
  char _pad_buf_[PADDING_SIZE(T, alignment)];
};

// Super class of PaddedEnd when pad_size != 0.
template <class T, size_t pad_size>
class PaddedEndImpl : public T {
 private:
  char _pad_buf[pad_size];
};

// Super class of PaddedEnd when pad_size == 0.
template <class T>
class PaddedEndImpl<T, /*pad_size*/ 0> : public T {
  // No padding.
};

#define PADDED_END_SIZE(type, alignment) (align_up(sizeof(type), (alignment)) - sizeof(type))

// More memory conservative implementation of Padded. The subclass adds the
// minimal amount of padding needed to make the size of the objects be aligned.
// This will help reducing false sharing,
// if the start address is a multiple of alignment.
template <class T, size_t alignment = DEFAULT_CACHE_LINE_SIZE>
class PaddedEnd : public PaddedEndImpl<T, PADDED_END_SIZE(T, alignment)> {
  // C++ doesn't allow zero-length arrays. The padding is put in a
  // super class that is specialized for the pad_size == 0 case.
};

// Similar to PaddedEnd, this macro defines a _pad_buf#id field
// that is (alignment - size) bytes in size. This macro is used
// to add padding in between non-class fields in a class or struct.
#define DEFINE_PAD_MINUS_SIZE(id, alignment, size) \
          char _pad_buf##id[(alignment) - (size)]

// Helper class to create an array of PaddedEnd<T> objects. All elements will
// start at a multiple of alignment and the size will be aligned to alignment.
template <class T, MEMFLAGS flags, size_t alignment = DEFAULT_CACHE_LINE_SIZE>
class PaddedArray {
 public:
  // Creates an aligned padded array.
  // The memory can't be deleted since the raw memory chunk is not returned.
  static PaddedEnd<T>* create_unfreeable(uint length);
};

// Helper class to create an array of references to arrays of primitive types
// Both the array of references and the data arrays are aligned to the given
// alignment. The allocated memory is zero-filled.
template <class T, MEMFLAGS flags, size_t alignment = DEFAULT_CACHE_LINE_SIZE>
class Padded2DArray {
 public:
  // Creates an aligned padded 2D array.
  // The memory cannot be deleted since the raw memory chunk is not returned.
  // Always uses mmap to reserve memory. Only the first few pages with the index to
  // the rows are touched. Allocation size should be "large" to cover page overhead.
  static T** create_unfreeable(uint rows, uint columns, size_t* allocation_size = NULL);
};

// Helper class to create an array of T objects. The array as a whole will
// start at a multiple of alignment and its size will be aligned to alignment.
template <class T, MEMFLAGS flags, size_t alignment = DEFAULT_CACHE_LINE_SIZE>
class PaddedPrimitiveArray {
 public:
  static T* create_unfreeable(size_t length);
  static T* create(size_t length, void** alloc_base);
};

#endif // SHARE_MEMORY_PADDED_HPP
