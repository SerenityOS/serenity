/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_Z_ZARRAY_INLINE_HPP
#define SHARE_GC_Z_ZARRAY_INLINE_HPP

#include "gc/z/zArray.hpp"

#include "runtime/atomic.hpp"

template <typename T, bool Parallel>
inline bool ZArrayIteratorImpl<T, Parallel>::next_serial(T* elem) {
  if (_next == _end) {
    return false;
  }

  *elem = *_next;
  _next++;

  return true;
}

template <typename T, bool Parallel>
inline bool ZArrayIteratorImpl<T, Parallel>::next_parallel(T* elem) {
  const T* old_next = Atomic::load(&_next);

  for (;;) {
    if (old_next == _end) {
      return false;
    }

    const T* const new_next = old_next + 1;
    const T* const prev_next = Atomic::cmpxchg(&_next, old_next, new_next);
    if (prev_next == old_next) {
      *elem = *old_next;
      return true;
    }

    old_next = prev_next;
  }
}

template <typename T, bool Parallel>
inline ZArrayIteratorImpl<T, Parallel>::ZArrayIteratorImpl(const T* array, size_t length) :
    _next(array),
    _end(array + length) {}

template <typename T, bool Parallel>
inline ZArrayIteratorImpl<T, Parallel>::ZArrayIteratorImpl(const ZArray<T>* array) :
    ZArrayIteratorImpl<T, Parallel>(array->is_empty() ? NULL : array->adr_at(0), array->length()) {}

template <typename T, bool Parallel>
inline bool ZArrayIteratorImpl<T, Parallel>::next(T* elem) {
  if (Parallel) {
    return next_parallel(elem);
  } else {
    return next_serial(elem);
  }
}

#endif // SHARE_GC_Z_ZARRAY_INLINE_HPP
