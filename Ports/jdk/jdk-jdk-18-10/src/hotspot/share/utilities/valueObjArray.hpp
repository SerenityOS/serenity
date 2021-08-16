/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_UTILITIES_VALUEOBJARRAY_HPP
#define SHARE_UTILITIES_VALUEOBJARRAY_HPP

#include "utilities/debug.hpp"

// Stamps out Count instances of Type using a recursive template.
template <typename Type, int Count>
class ValueObjBlock {
  typedef ValueObjBlock<Type, Count - 1> Next;

  Type _instance;
  Next _next;

public:
  template <typename Generator>
  ValueObjBlock(Generator g, Type** save_to) :
      _instance(*g),
      _next(++g, save_to + 1) {
    *save_to = &_instance;
  }
};

// Specialization for the recursion base case.
template <typename Type>
class ValueObjBlock<Type, 0> {
public:
  template <typename Generator>
  ValueObjBlock(Generator, Type**) {}
};

// Maps an array of size Count over stamped-out instances of Type.
template <typename Type, int Count>
struct ValueObjArray {
  Type*                      _ptrs[Count];
  ValueObjBlock<Type, Count> _block;

  template <typename Generator>
  ValueObjArray(Generator g) : _ptrs(), _block(g, _ptrs) {}

  Type* at(int index) const {
    assert(0 <= index && index < Count, "index out-of-bounds: %d", index);
    return _ptrs[index];
  }

  static int count() {
    return Count;
  }
};

#endif // SHARE_UTILITIES_VALUEOBJARRAY_HPP
