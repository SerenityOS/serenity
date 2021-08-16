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

#ifndef SHARE_MEMORY_PADDED_INLINE_HPP
#define SHARE_MEMORY_PADDED_INLINE_HPP

#include "memory/padded.hpp"

#include "memory/allocation.inline.hpp"
#include "utilities/align.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"

// Creates an aligned padded array.
// The memory can't be deleted since the raw memory chunk is not returned.
template <class T, MEMFLAGS flags, size_t alignment>
PaddedEnd<T>* PaddedArray<T, flags, alignment>::create_unfreeable(uint length) {
  // Check that the PaddedEnd class works as intended.
  STATIC_ASSERT(is_aligned(sizeof(PaddedEnd<T>), alignment));

  // Allocate a chunk of memory large enough to allow for some alignment.
  void* chunk = AllocateHeap(length * sizeof(PaddedEnd<T, alignment>) + alignment, flags);

  // Make the initial alignment.
  PaddedEnd<T>* aligned_padded_array = (PaddedEnd<T>*)align_up(chunk, alignment);

  // Call the default constructor for each element.
  for (uint i = 0; i < length; i++) {
    ::new (&aligned_padded_array[i]) T();
  }

  return aligned_padded_array;
}

template <class T, MEMFLAGS flags, size_t alignment>
T** Padded2DArray<T, flags, alignment>::create_unfreeable(uint rows, uint columns, size_t* allocation_size) {
  // Calculate and align the size of the first dimension's table.
  size_t table_size = align_up(rows * sizeof(T*), alignment);
  // The size of the separate rows.
  size_t row_size = align_up(columns * sizeof(T), alignment);
  // Total size consists of the indirection table plus the rows.
  size_t total_size = table_size + rows * row_size + alignment;

  // Allocate a chunk of memory large enough to allow alignment of the chunk.
  void* chunk = MmapArrayAllocator<uint8_t>::allocate(total_size, flags);
  // Clear the allocated memory.
  // Align the chunk of memory.
  T** result = (T**)align_up(chunk, alignment);
  void* data_start = (void*)((uintptr_t)result + table_size);

  // Fill in the row table.
  for (size_t i = 0; i < rows; i++) {
    result[i] = (T*)((uintptr_t)data_start + i * row_size);
  }

  if (allocation_size != NULL) {
    *allocation_size = total_size;
  }

  return result;
}

template <class T, MEMFLAGS flags, size_t alignment>
T* PaddedPrimitiveArray<T, flags, alignment>::create_unfreeable(size_t length) {
  void* temp;
  return create(length, &temp);
}

template <class T, MEMFLAGS flags, size_t alignment>
T* PaddedPrimitiveArray<T, flags, alignment>::create(size_t length, void** alloc_base) {
  // Allocate a chunk of memory large enough to allow for some alignment.
  void* chunk = AllocateHeap(length * sizeof(T) + alignment, flags);

  memset(chunk, 0, length * sizeof(T) + alignment);

  *alloc_base = chunk;
  return (T*)align_up(chunk, alignment);
}

#endif // SHARE_MEMORY_PADDED_INLINE_HPP
