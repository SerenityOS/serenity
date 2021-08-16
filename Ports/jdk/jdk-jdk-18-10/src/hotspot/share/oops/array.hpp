/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OOPS_ARRAY_HPP
#define SHARE_OOPS_ARRAY_HPP

#include "runtime/atomic.hpp"
#include "utilities/align.hpp"
#include "utilities/exceptions.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/ostream.hpp"

// Array for metadata allocation

template <typename T>
class Array: public MetaspaceObj {
  friend class ArchiveBuilder;
  friend class MetadataFactory;
  friend class VMStructs;
  friend class JVMCIVMStructs;
  friend class MethodHandleCompiler;           // special case
  friend class WhiteBox;
protected:
  int _length;                                 // the number of array elements
  T   _data[1];                                // the array memory

  void initialize(int length) {
    _length = length;
  }

 private:
  NONCOPYABLE(Array);

  inline void* operator new(size_t size, ClassLoaderData* loader_data, int length, TRAPS) throw();

  static size_t byte_sizeof(int length, size_t elm_byte_size) {
    return sizeof(Array<T>) + MAX2(length - 1, 0) * elm_byte_size;
  }
  static size_t byte_sizeof(int length) { return byte_sizeof(length, sizeof(T)); }

  // WhiteBox API helper.
  // Can't distinguish between array of length 0 and length 1,
  // will always return 0 in those cases.
  static int bytes_to_length(size_t bytes)       {
    assert(is_aligned(bytes, BytesPerWord), "Must be, for now");

    if (sizeof(Array<T>) >= bytes) {
      return 0;
    }

    size_t left = bytes - sizeof(Array<T>);
    assert(is_aligned(left, sizeof(T)), "Must be");

    size_t elements = left / sizeof(T);
    assert(elements <= (size_t)INT_MAX, "number of elements " SIZE_FORMAT "doesn't fit into an int.", elements);

    int length = (int)elements;

    assert((size_t)size(length) * BytesPerWord == (size_t)bytes,
           "Expected: " SIZE_FORMAT " got: " SIZE_FORMAT,
           bytes, (size_t)size(length) * BytesPerWord);

    return length;
  }

  explicit Array(int length) : _length(length) {
    assert(length >= 0, "illegal length");
  }

  Array(int length, T init) : _length(length) {
    assert(length >= 0, "illegal length");
    for (int i = 0; i < length; i++) {
      _data[i] = init;
    }
  }

 public:

  // standard operations
  int  length() const                 { return _length; }
  T* data()                           { return _data; }
  bool is_empty() const               { return length() == 0; }

  int index_of(const T& x) const {
    int i = length();
    while (i-- > 0 && _data[i] != x) ;

    return i;
  }

  // sort the array.
  bool contains(const T& x) const      { return index_of(x) >= 0; }

  T    at(int i) const                 { assert(i >= 0 && i< _length, "oob: 0 <= %d < %d", i, _length); return _data[i]; }
  void at_put(const int i, const T& x) { assert(i >= 0 && i< _length, "oob: 0 <= %d < %d", i, _length); _data[i] = x; }
  T*   adr_at(const int i)             { assert(i >= 0 && i< _length, "oob: 0 <= %d < %d", i, _length); return &_data[i]; }
  int  find(const T& x)                { return index_of(x); }

  T at_acquire(const int i)            { return Atomic::load_acquire(adr_at(i)); }
  void release_at_put(int i, T x)      { Atomic::release_store(adr_at(i), x); }

  static int size(int length) {
    size_t bytes = align_up(byte_sizeof(length), BytesPerWord);
    size_t words = bytes / BytesPerWord;

    assert(words <= INT_MAX, "Overflow: " SIZE_FORMAT, words);

    return (int)words;
  }
  int size() {
    return size(_length);
  }

  static int length_offset_in_bytes() { return (int) (offset_of(Array<T>, _length)); }
  // Note, this offset don't have to be wordSize aligned.
  static int base_offset_in_bytes() { return (int) (offset_of(Array<T>, _data)); };

  // FIXME: How to handle this?
  void print_value_on(outputStream* st) const {
    st->print("Array<T>(" INTPTR_FORMAT ")", p2i(this));
  }

#ifndef PRODUCT
  void print(outputStream* st) {
     for (int i = 0; i< _length; i++) {
       st->print_cr("%d: " INTPTR_FORMAT, i, (intptr_t)at(i));
     }
  }
  void print() { print(tty); }
#endif // PRODUCT
};


#endif // SHARE_OOPS_ARRAY_HPP
