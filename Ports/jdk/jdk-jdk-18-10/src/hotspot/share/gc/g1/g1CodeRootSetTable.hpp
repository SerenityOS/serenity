/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1CODEROOTSETTABLE_HPP
#define SHARE_GC_G1_G1CODEROOTSETTABLE_HPP

#include "utilities/hashtable.hpp"

class nmethod;

class G1CodeRootSetTable : public Hashtable<nmethod*, mtGC> {
  friend class G1CodeRootSetTest;
  typedef HashtableEntry<nmethod*, mtGC> Entry;

  static G1CodeRootSetTable* volatile _purge_list;

  G1CodeRootSetTable* _purge_next;

  unsigned int compute_hash(nmethod* nm) {
    uintptr_t hash = (uintptr_t)nm;
    return hash ^ (hash >> 7); // code heap blocks are 128byte aligned
  }

  void remove_entry(Entry* e, Entry* previous);
  Entry* new_entry(nmethod* nm);

 public:
  G1CodeRootSetTable(int size) : Hashtable<nmethod*, mtGC>(size, sizeof(Entry)), _purge_next(NULL) {}
  ~G1CodeRootSetTable();

  // Needs to be protected by locks
  bool add(nmethod* nm);
  bool remove(nmethod* nm);

  // Can be called without locking
  bool contains(nmethod* nm);

  int entry_size() const { return BasicHashtable<mtGC>::entry_size(); }

  void copy_to(G1CodeRootSetTable* new_table);
  void nmethods_do(CodeBlobClosure* blk);

  template<typename CB>
  int remove_if(CB& should_remove);

  static void purge_list_append(G1CodeRootSetTable* tbl);
  static void purge();

  static size_t static_mem_size() {
    return sizeof(_purge_list);
  }

  size_t mem_size();
};

#endif // SHARE_GC_G1_G1CODEROOTSETTABLE_HPP
