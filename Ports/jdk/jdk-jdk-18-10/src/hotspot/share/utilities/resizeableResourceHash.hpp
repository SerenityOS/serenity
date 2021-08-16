/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_UTILITIES_RESIZEABLERESOURCEHASH_HPP
#define SHARE_UTILITIES_RESIZEABLERESOURCEHASH_HPP

#include "utilities/resourceHash.hpp"

template<
    typename K, typename V,
    ResourceObj::allocation_type ALLOC_TYPE,
    MEMFLAGS MEM_TYPE>
class ResizeableResourceHashtableStorage : public ResourceObj {
  using Node = ResourceHashtableNode<K, V>;

protected:
  unsigned _table_size;
  Node** _table;

  ResizeableResourceHashtableStorage(unsigned table_size) {
    _table_size = table_size;
    _table = alloc_table(table_size);
  }

  ~ResizeableResourceHashtableStorage() {
    if (ALLOC_TYPE == C_HEAP) {
      FREE_C_HEAP_ARRAY(Node*, _table);
    }
  }

  Node** alloc_table(unsigned table_size) {
    Node** table;
    if (ALLOC_TYPE == C_HEAP) {
      table = NEW_C_HEAP_ARRAY(Node*, table_size, MEM_TYPE);
    } else {
      table = NEW_RESOURCE_ARRAY(Node*, table_size);
    }
    memset(table, 0, table_size * sizeof(Node*));
    return table;
  }

  unsigned table_size() const {
    return _table_size;
  }

  Node** table() const {
    return _table;
  }
};

template<
    typename K, typename V,
    ResourceObj::allocation_type ALLOC_TYPE = ResourceObj::RESOURCE_AREA,
    MEMFLAGS MEM_TYPE = mtInternal,
    unsigned (*HASH)  (K const&)           = primitive_hash<K>,
    bool     (*EQUALS)(K const&, K const&) = primitive_equals<K>
    >
class ResizeableResourceHashtable : public ResourceHashtableBase<
    ResizeableResourceHashtableStorage<K, V, ALLOC_TYPE, MEM_TYPE>,
    K, V, ALLOC_TYPE, MEM_TYPE, HASH, EQUALS> {
  unsigned _max_size;

  using BASE = ResourceHashtableBase<ResizeableResourceHashtableStorage<K, V, ALLOC_TYPE, MEM_TYPE>,
                                     K, V, ALLOC_TYPE, MEM_TYPE, HASH, EQUALS>;
  using Node = ResourceHashtableNode<K, V>;
  NONCOPYABLE(ResizeableResourceHashtable);
public:
  ResizeableResourceHashtable(unsigned size, unsigned max_size = 0)
  : BASE(size), _max_size(max_size) {
    assert(size <= 0x3fffffff && max_size <= 0x3fffffff, "avoid overflow in resize");
  }

  bool maybe_grow(int load_factor = 8) {
    unsigned old_size = BASE::_table_size;
    if (old_size >= _max_size) {
      return false;
    }
    if (BASE::number_of_entries() / int(old_size) > load_factor) {
      unsigned new_size = MIN2<unsigned>(old_size * 2, _max_size);
      resize(old_size, new_size);
      return true;
    } else {
      return false;
    }
  }

  void resize(unsigned old_size, unsigned new_size) {
    Node** old_table = BASE::_table;
    Node** new_table = BASE::alloc_table(new_size);

    Node* const* bucket = old_table;
    while (bucket < &old_table[old_size]) {
      Node* node = *bucket;
      while (node != NULL) {
        Node* next = node->_next;
        unsigned hash = HASH(node->_key);
        unsigned index = hash % new_size;

        node->_next = new_table[index];
        new_table[index] = node;

        node = next;
      }
      ++bucket;
    }

    if (ALLOC_TYPE == ResourceObj::C_HEAP) {
      FREE_C_HEAP_ARRAY(Node*, old_table);
    }
    BASE::_table = new_table;
    BASE::_table_size = new_size;
  }
};

#endif // SHARE_UTILITIES_RESIZEABLERESOURCEHASH_HPP
