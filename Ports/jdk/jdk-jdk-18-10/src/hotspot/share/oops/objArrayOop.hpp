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

#ifndef SHARE_OOPS_OBJARRAYOOP_HPP
#define SHARE_OOPS_OBJARRAYOOP_HPP

#include "oops/arrayOop.hpp"
#include "utilities/align.hpp"

class Klass;

// An objArrayOop is an array containing oops.
// Evaluating "String arg[10]" will create an objArrayOop.

class objArrayOopDesc : public arrayOopDesc {
  friend class ObjArrayKlass;
  friend class Runtime1;
  friend class psPromotionManager;
  friend class CSetMarkWordClosure;

  template <class T> T* obj_at_addr(int index) const;

  template <class T>
  static ptrdiff_t obj_at_offset(int index) {
    return base_offset_in_bytes() + sizeof(T) * index;
  }

private:
  // Give size of objArrayOop in HeapWords minus the header
  static int array_size(int length) {
    const uint OopsPerHeapWord = HeapWordSize/heapOopSize;
    assert(OopsPerHeapWord >= 1 && (HeapWordSize % heapOopSize == 0),
           "Else the following (new) computation would be in error");
    uint res = ((uint)length + OopsPerHeapWord - 1)/OopsPerHeapWord;
#ifdef ASSERT
    // The old code is left in for sanity-checking; it'll
    // go away pretty soon. XXX
    // Without UseCompressedOops, this is simply:
    // oop->length() * HeapWordsPerOop;
    // With narrowOops, HeapWordsPerOop is 1/2 or equal 0 as an integer.
    // The oop elements are aligned up to wordSize
    const uint HeapWordsPerOop = heapOopSize/HeapWordSize;
    uint old_res;
    if (HeapWordsPerOop > 0) {
      old_res = length * HeapWordsPerOop;
    } else {
      old_res = align_up((uint)length, OopsPerHeapWord)/OopsPerHeapWord;
    }
    assert(res == old_res, "Inconsistency between old and new.");
#endif  // ASSERT
    return res;
  }

 public:
  // Returns the offset of the first element.
  static int base_offset_in_bytes() {
    return arrayOopDesc::base_offset_in_bytes(T_OBJECT);
  }

  // base is the address following the header.
  HeapWord* base() const;

  // Accessing
  oop obj_at(int index) const;

  void obj_at_put(int index, oop value);

  oop atomic_compare_exchange_oop(int index, oop exchange_value, oop compare_value);

  // Sizing
  static int header_size()    { return arrayOopDesc::header_size(T_OBJECT); }
  int object_size()           { return object_size(length()); }

  static int object_size(int length) {
    // This returns the object size in HeapWords.
    uint asz = array_size(length);
    uint osz = align_object_size(header_size() + asz);
    assert(osz >= asz,   "no overflow");
    assert((int)osz > 0, "no overflow");
    return (int)osz;
  }

  Klass* element_klass();

public:
  // special iterators for index ranges, returns size of object
  template <typename OopClosureType>
  void oop_iterate_range(OopClosureType* blk, int start, int end);
};

#endif // SHARE_OOPS_OBJARRAYOOP_HPP
