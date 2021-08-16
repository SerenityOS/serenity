/*
 * Copyright (c) 1999, 2010, Oracle and/or its affiliates. All rights reserved.
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

#include "precompiled.hpp"
#include "ci/ciConstantPoolCache.hpp"
#include "ci/ciUtilities.inline.hpp"
#include "memory/allocation.hpp"
#include "memory/allocation.inline.hpp"

// ciConstantPoolCache
//
// This class caches indexed constant pool lookups.

// ------------------------------------------------------------------
// ciConstantPoolCache::ciConstantPoolCache
ciConstantPoolCache::ciConstantPoolCache(Arena* arena,
                                 int expected_size) {
  _elements =
    new (arena) GrowableArray<void*>(arena, expected_size, 0, 0);
  _keys = new (arena) GrowableArray<int>(arena, expected_size, 0, 0);
}

int ciConstantPoolCache::key_compare(const int& key, const int& elt) {
  if (key < elt)      return -1;
  else if (key > elt) return 1;
  else                  return 0;
}

// ------------------------------------------------------------------
// ciConstantPoolCache::get
//
// Get the entry at some index
void* ciConstantPoolCache::get(int index) {
  ASSERT_IN_VM;
  bool found = false;
  int pos = _keys->find_sorted<int, ciConstantPoolCache::key_compare>(index, found);
  if (!found) {
    // This element is not present in the cache.
    return NULL;
  }
  return _elements->at(pos);
}

// ------------------------------------------------------------------
// ciConstantPoolCache::insert
//
// Insert a ciObject into the table at some index.
void ciConstantPoolCache::insert(int index, void* elem) {
  bool found = false;
  int pos = _keys->find_sorted<int, ciConstantPoolCache::key_compare>(index, found);
  assert(!found, "duplicate");
  _keys->insert_before(pos, index);
  _elements->insert_before(pos, elem);
}

// ------------------------------------------------------------------
// ciConstantPoolCache::print
//
// Print debugging information about the cache.
void ciConstantPoolCache::print() {
  Unimplemented();
}
