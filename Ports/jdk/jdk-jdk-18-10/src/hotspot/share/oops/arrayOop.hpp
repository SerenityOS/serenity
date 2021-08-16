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

#ifndef SHARE_OOPS_ARRAYOOP_HPP
#define SHARE_OOPS_ARRAYOOP_HPP

#include "oops/oop.hpp"
#include "utilities/align.hpp"

// arrayOopDesc is the abstract baseclass for all arrays.  It doesn't
// declare pure virtual to enforce this because that would allocate a vtbl
// in each instance, which we don't want.

// The layout of array Oops is:
//
//  markWord
//  Klass*    // 32 bits if compressed but declared 64 in LP64.
//  length    // shares klass memory or allocated after declared fields.


class arrayOopDesc : public oopDesc {
  friend class VMStructs;
  friend class arrayOopDescTest;

  // Interpreter/Compiler offsets

  // Header size computation.
  // The header is considered the oop part of this type plus the length.
  // Returns the aligned header_size_in_bytes.  This is not equivalent to
  // sizeof(arrayOopDesc) which should not appear in the code.
  static int header_size_in_bytes() {
    size_t hs = align_up(length_offset_in_bytes() + sizeof(int),
                              HeapWordSize);
#ifdef ASSERT
    // make sure it isn't called before UseCompressedOops is initialized.
    static size_t arrayoopdesc_hs = 0;
    if (arrayoopdesc_hs == 0) arrayoopdesc_hs = hs;
    assert(arrayoopdesc_hs == hs, "header size can't change");
#endif // ASSERT
    return (int)hs;
  }

  // Returns the address of the length "field".  See length_offset_in_bytes().
  static int* length_addr_impl(void* obj_ptr) {
    char* ptr = static_cast<char*>(obj_ptr);
    return reinterpret_cast<int*>(ptr + length_offset_in_bytes());
  }

  // Check whether an element of a typeArrayOop with the given type must be
  // aligned 0 mod 8.  The typeArrayOop itself must be aligned at least this
  // strongly.
  static bool element_type_should_be_aligned(BasicType type) {
    return type == T_DOUBLE || type == T_LONG;
  }

 public:
  // The _length field is not declared in C++.  It is allocated after the
  // declared nonstatic fields in arrayOopDesc if not compressed, otherwise
  // it occupies the second half of the _klass field in oopDesc.
  static int length_offset_in_bytes() {
    return UseCompressedClassPointers ? klass_gap_offset_in_bytes() :
                               sizeof(arrayOopDesc);
  }

  // Returns the offset of the first element.
  static int base_offset_in_bytes(BasicType type) {
    return header_size(type) * HeapWordSize;
  }

  // Returns the address of the first element. The elements in the array will not
  // relocate from this address until a subsequent thread transition.
  void* base(BasicType type) const {
    return reinterpret_cast<void*>(cast_from_oop<intptr_t>(as_oop()) + base_offset_in_bytes(type));
  }

  template <typename T>
  static T* obj_offset_to_raw(arrayOop obj, size_t offset_in_bytes, T* raw) {
    if (obj != NULL) {
      assert(raw == NULL, "either raw or in-heap");
      char* base = reinterpret_cast<char*>((void*) obj);
      raw = reinterpret_cast<T*>(base + offset_in_bytes);
    } else {
      assert(raw != NULL, "either raw or in-heap");
    }
    return raw;
  }

  // Tells whether index is within bounds.
  bool is_within_bounds(int index) const        { return 0 <= index && index < length(); }

  // Accessors for array length.  There's not a member variable for
  // it; see length_offset_in_bytes().
  int length() const { return *length_addr_impl(const_cast<arrayOopDesc*>(this)); }
  void set_length(int length) { *length_addr_impl(this) = length; }

  int* length_addr() {
    return length_addr_impl(this);
  }

  static void set_length(HeapWord* mem, int length) {
    *length_addr_impl(mem) = length;
  }

  // Should only be called with constants as argument
  // (will not constant fold otherwise)
  // Returns the header size in words aligned to the requirements of the
  // array object type.
  static int header_size(BasicType type) {
    size_t typesize_in_bytes = header_size_in_bytes();
    return (int)(element_type_should_be_aligned(type)
      ? align_object_offset(typesize_in_bytes/HeapWordSize)
      : typesize_in_bytes/HeapWordSize);
  }

  // Return the maximum length of an array of BasicType.  The length can passed
  // to typeArrayOop::object_size(scale, length, header_size) without causing an
  // overflow. We also need to make sure that this will not overflow a size_t on
  // 32 bit platforms when we convert it to a byte size.
  static int32_t max_array_length(BasicType type) {
    assert(type >= 0 && type < T_CONFLICT, "wrong type");
    assert(type2aelembytes(type) != 0, "wrong type");

    const size_t max_element_words_per_size_t =
      align_down((SIZE_MAX/HeapWordSize - header_size(type)), MinObjAlignment);
    const size_t max_elements_per_size_t =
      HeapWordSize * max_element_words_per_size_t / type2aelembytes(type);
    if ((size_t)max_jint < max_elements_per_size_t) {
      // It should be ok to return max_jint here, but parts of the code
      // (CollectedHeap, Klass::oop_oop_iterate(), and more) uses an int for
      // passing around the size (in words) of an object. So, we need to avoid
      // overflowing an int when we add the header. See CRs 4718400 and 7110613.
      return align_down(max_jint - header_size(type), MinObjAlignment);
    }
    return (int32_t)max_elements_per_size_t;
  }

};

#endif // SHARE_OOPS_ARRAYOOP_HPP
