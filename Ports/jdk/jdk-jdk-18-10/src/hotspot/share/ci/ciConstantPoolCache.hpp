/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CI_CICONSTANTPOOLCACHE_HPP
#define SHARE_CI_CICONSTANTPOOLCACHE_HPP

#include "memory/resourceArea.hpp"
#include "utilities/growableArray.hpp"

// ciConstantPoolCache
//
// The class caches indexed constant pool lookups.
//
// Usage note: this klass has nothing to do with ConstantPoolCache*.
class ciConstantPoolCache : public ResourceObj {
private:
  GrowableArray<int>*   _keys;
  GrowableArray<void*>* _elements;

  static int key_compare(const int& key, const int& elt);

public:
  ciConstantPoolCache(Arena* arena, int expected_size);

  // Get the element associated with some index.
  void* get(int index);

  // Associate an element with an index.
  void insert(int index, void* element);

  void print();
};

#endif // SHARE_CI_CICONSTANTPOOLCACHE_HPP
