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

#ifndef SHARE_JFR_LEAKPROFILER_CHAINS_BITSET_HPP
#define SHARE_JFR_LEAKPROFILER_CHAINS_BITSET_HPP

#include "memory/allocation.hpp"
#include "oops/oop.hpp"
#include "oops/oopsHierarchy.hpp"
#include "utilities/bitMap.hpp"
#include "utilities/hashtable.hpp"

class JfrVirtualMemory;
class MemRegion;

class BitSet : public CHeapObj<mtTracing> {
  const static size_t _bitmap_granularity_shift = 26; // 64M
  const static size_t _bitmap_granularity_size = (size_t)1 << _bitmap_granularity_shift;
  const static size_t _bitmap_granularity_mask = _bitmap_granularity_size - 1;

  class BitMapFragment;

  class BitMapFragmentTable : public BasicHashtable<mtTracing> {
    class Entry : public BasicHashtableEntry<mtTracing> {
    public:
      uintptr_t _key;
      CHeapBitMap* _value;

      Entry* next() {
        return (Entry*)BasicHashtableEntry<mtTracing>::next();
      }
    };

  protected:
    Entry* bucket(int i) const;

    Entry* new_entry(unsigned int hashValue, uintptr_t key, CHeapBitMap* value);

    unsigned hash_segment(uintptr_t key) {
      unsigned hash = (unsigned)key;
      return hash ^ (hash >> 3);
    }

    unsigned hash_to_index(unsigned hash) {
      return hash & (BasicHashtable<mtTracing>::table_size() - 1);
    }

  public:
    BitMapFragmentTable(int table_size) : BasicHashtable<mtTracing>(table_size, sizeof(Entry)) {}
    void add(uintptr_t key, CHeapBitMap* value);
    CHeapBitMap** lookup(uintptr_t key);
  };

  CHeapBitMap* get_fragment_bits(uintptr_t addr);

  BitMapFragmentTable _bitmap_fragments;
  BitMapFragment* _fragment_list;
  CHeapBitMap* _last_fragment_bits;
  uintptr_t _last_fragment_granule;

 public:
  BitSet();
  ~BitSet();

  BitMap::idx_t addr_to_bit(uintptr_t addr) const;

  void mark_obj(uintptr_t addr);

  void mark_obj(oop obj) {
    return mark_obj(cast_from_oop<uintptr_t>(obj));
  }

  bool is_marked(uintptr_t addr);

  bool is_marked(oop obj) {
    return is_marked(cast_from_oop<uintptr_t>(obj));
  }
};

class BitSet::BitMapFragment : public CHeapObj<mtTracing> {
  CHeapBitMap _bits;
  BitMapFragment* _next;

public:
  BitMapFragment(uintptr_t granule, BitMapFragment* next);

  BitMapFragment* next() const {
    return _next;
  }

  CHeapBitMap* bits() {
    return &_bits;
  }
};

#endif // SHARE_JFR_LEAKPROFILER_CHAINS_BITSET_HPP
