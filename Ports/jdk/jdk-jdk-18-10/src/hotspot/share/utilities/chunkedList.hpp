/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_UTILITIES_CHUNKEDLIST_HPP
#define SHARE_UTILITIES_CHUNKEDLIST_HPP

#include "memory/allocation.hpp"
#include "utilities/debug.hpp"

template <class T, MEMFLAGS F> class ChunkedList : public CHeapObj<F> {
  template <class U> friend class TestChunkedList;

  static const size_t BufferSize = 64;

  T  _values[BufferSize];
  T* _top;

  ChunkedList<T, F>* _next_used;
  ChunkedList<T, F>* _next_free;

  T const * end() const {
    return &_values[BufferSize];
  }

 public:
  ChunkedList<T, F>() : _top(_values), _next_used(NULL), _next_free(NULL) {}

  bool is_full() const {
    return _top == end();
  }

  void clear() {
    _top = _values;
    // Don't clear the next pointers since that would interfere
    // with other threads trying to iterate through the lists.
  }

  void push(T m) {
    assert(!is_full(), "Buffer is full");
    *_top = m;
    _top++;
  }

  void set_next_used(ChunkedList<T, F>* buffer) { _next_used = buffer; }
  void set_next_free(ChunkedList<T, F>* buffer) { _next_free = buffer; }

  ChunkedList<T, F>* next_used() const          { return _next_used; }
  ChunkedList<T, F>* next_free() const          { return _next_free; }

  size_t size() const {
    return pointer_delta(_top, _values, sizeof(T));
  }

  T at(size_t i) {
    assert(i < size(), "IOOBE i: " SIZE_FORMAT " size(): " SIZE_FORMAT, i, size());
    return _values[i];
  }
};

#endif // SHARE_UTILITIES_CHUNKEDLIST_HPP
