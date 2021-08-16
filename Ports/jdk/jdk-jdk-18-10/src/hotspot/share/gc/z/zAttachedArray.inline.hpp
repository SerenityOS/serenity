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

#ifndef SHARE_GC_Z_ZATTACHEDARRAY_INLINE_HPP
#define SHARE_GC_Z_ZATTACHEDARRAY_INLINE_HPP

#include "gc/z/zAttachedArray.hpp"

#include "memory/allocation.hpp"
#include "utilities/align.hpp"

template <typename ObjectT, typename ArrayT>
inline size_t ZAttachedArray<ObjectT, ArrayT>::object_size() {
  return align_up(sizeof(ObjectT), sizeof(ArrayT));
}

template <typename ObjectT, typename ArrayT>
inline size_t ZAttachedArray<ObjectT, ArrayT>::array_size(size_t length) {
  return sizeof(ArrayT) * length;
}

template <typename ObjectT, typename ArrayT>
template <typename Allocator>
inline void* ZAttachedArray<ObjectT, ArrayT>::alloc(Allocator* allocator, size_t length) {
  // Allocate memory for object and array
  const size_t size = object_size() + array_size(length);
  void* const addr = allocator->alloc(size);

  // Placement new array
  void* const array_addr = reinterpret_cast<char*>(addr) + object_size();
  ::new (array_addr) ArrayT[length];

  // Return pointer to object
  return addr;
}

template <typename ObjectT, typename ArrayT>
inline void* ZAttachedArray<ObjectT, ArrayT>::alloc(size_t length) {
  struct Allocator {
    void* alloc(size_t size) const {
      return AllocateHeap(size, mtGC);
    }
  } allocator;
  return alloc(&allocator, length);
}

template <typename ObjectT, typename ArrayT>
inline void ZAttachedArray<ObjectT, ArrayT>::free(ObjectT* obj) {
  FreeHeap(obj);
}

template <typename ObjectT, typename ArrayT>
inline ZAttachedArray<ObjectT, ArrayT>::ZAttachedArray(size_t length) :
    _length(length) {}

template <typename ObjectT, typename ArrayT>
inline size_t ZAttachedArray<ObjectT, ArrayT>::length() const {
  return _length;
}

template <typename ObjectT, typename ArrayT>
inline ArrayT* ZAttachedArray<ObjectT, ArrayT>::operator()(const ObjectT* obj) const {
  return reinterpret_cast<ArrayT*>(reinterpret_cast<uintptr_t>(obj) + object_size());
}

#endif // SHARE_GC_Z_ZATTACHEDARRAY_INLINE_HPP
