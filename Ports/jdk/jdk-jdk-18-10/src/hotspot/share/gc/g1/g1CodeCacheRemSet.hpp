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

#ifndef SHARE_GC_G1_G1CODECACHEREMSET_HPP
#define SHARE_GC_G1_G1CODECACHEREMSET_HPP

class CodeBlobClosure;
class G1CodeRootSetTable;
class HeapRegion;
class nmethod;

// Implements storage for a set of code roots.
// All methods that modify the set are not thread-safe except if otherwise noted.
class G1CodeRootSet {
  friend class G1CodeRootSetTest;
 private:

  const static size_t SmallSize = 32;
  const static size_t Threshold = 24;
  const static size_t LargeSize = 512;

  G1CodeRootSetTable* _table;
  G1CodeRootSetTable* load_acquire_table();

  size_t _length;

  void move_to_large();
  void allocate_small_table();

 public:
  G1CodeRootSet() : _table(NULL), _length(0) {}
  ~G1CodeRootSet();

  static void purge();

  static size_t static_mem_size();

  void add(nmethod* method);

  bool remove(nmethod* method);

  // Safe to call without synchronization, but may return false negatives.
  bool contains(nmethod* method);

  void clear();

  void nmethods_do(CodeBlobClosure* blk) const;

  // Remove all nmethods which no longer contain pointers into our "owner" region
  void clean(HeapRegion* owner);

  bool is_empty() {
    bool empty = length() == 0;
    assert(empty == (_table == NULL), "is empty only if table is deallocated");
    return empty;
  }

  // Length in elements
  size_t length() const { return _length; }

  // Memory size in bytes taken by this set.
  size_t mem_size();

};

#endif // SHARE_GC_G1_G1CODECACHEREMSET_HPP
