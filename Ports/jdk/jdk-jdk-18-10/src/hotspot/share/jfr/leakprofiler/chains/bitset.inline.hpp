/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_LEAKPROFILER_CHAINS_BITSET_INLINE_HPP
#define SHARE_JFR_LEAKPROFILER_CHAINS_BITSET_INLINE_HPP

#include "jfr/leakprofiler/chains/bitset.hpp"

#include "jfr/recorder/storage/jfrVirtualMemory.hpp"
#include "memory/memRegion.hpp"
#include "utilities/bitMap.inline.hpp"
#include "utilities/hashtable.inline.hpp"

inline BitSet::BitMapFragmentTable::Entry* BitSet::BitMapFragmentTable::bucket(int i) const {
  return (Entry*)BasicHashtable<mtTracing>::bucket(i);
}

inline BitSet::BitMapFragmentTable::Entry* BitSet::BitMapFragmentTable::new_entry(unsigned int hash,
                                                                                  uintptr_t key,
                                                                                  CHeapBitMap* value) {
  Entry* entry = (Entry*)BasicHashtable<mtTracing>::new_entry(hash);
  entry->_key = key;
  entry->_value = value;
  return entry;
}

inline void BitSet::BitMapFragmentTable::add(uintptr_t key, CHeapBitMap* value) {
  unsigned hash = hash_segment(key);
  Entry* entry = new_entry(hash, key, value);
  BasicHashtable<mtTracing>::add_entry(hash_to_index(hash), entry);
}

inline CHeapBitMap** BitSet::BitMapFragmentTable::lookup(uintptr_t key) {
  unsigned hash = hash_segment(key);
  int index = hash_to_index(hash);
  for (Entry* e = bucket(index); e != NULL; e = e->next()) {
    if (e->hash() == hash && e->_key == key) {
      return &(e->_value);
    }
  }
  return NULL;
}

inline BitMap::idx_t BitSet::addr_to_bit(uintptr_t addr) const {
  return (addr & _bitmap_granularity_mask) >> LogMinObjAlignmentInBytes;
}

inline CHeapBitMap* BitSet::get_fragment_bits(uintptr_t addr) {
  uintptr_t granule = addr >> _bitmap_granularity_shift;
  if (granule == _last_fragment_granule) {
    return _last_fragment_bits;
  }
  CHeapBitMap* bits = NULL;

  CHeapBitMap** found = _bitmap_fragments.lookup(granule);
  if (found != NULL) {
    bits = *found;
  } else {
    BitMapFragment* fragment = new BitMapFragment(granule, _fragment_list);
    bits = fragment->bits();
    _fragment_list = fragment;
    if (_bitmap_fragments.number_of_entries() * 100 / _bitmap_fragments.table_size() > 25) {
      _bitmap_fragments.resize(_bitmap_fragments.table_size() * 2);
    }
    _bitmap_fragments.add(granule, bits);
  }

  _last_fragment_bits = bits;
  _last_fragment_granule = granule;

  return bits;
}

inline void BitSet::mark_obj(uintptr_t addr) {
  CHeapBitMap* bits = get_fragment_bits(addr);
  const BitMap::idx_t bit = addr_to_bit(addr);
  bits->set_bit(bit);
}

inline bool BitSet::is_marked(uintptr_t addr) {
  CHeapBitMap* bits = get_fragment_bits(addr);
  const BitMap::idx_t bit = addr_to_bit(addr);
  return bits->at(bit);
}

#endif // SHARE_JFR_LEAKPROFILER_CHAINS_BITSET_INLINE_HPP
