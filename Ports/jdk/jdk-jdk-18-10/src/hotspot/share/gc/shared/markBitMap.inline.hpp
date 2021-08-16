/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_MARKBITMAP_INLINE_HPP
#define SHARE_GC_SHARED_MARKBITMAP_INLINE_HPP

#include "gc/shared/markBitMap.hpp"

#include "gc/shared/collectedHeap.hpp"
#include "memory/memRegion.hpp"
#include "utilities/align.hpp"
#include "utilities/bitMap.inline.hpp"

inline HeapWord* MarkBitMap::get_next_marked_addr(const HeapWord* addr,
                                                const HeapWord* limit) const {
  assert(limit != NULL, "limit must not be NULL");
  // Round addr up to a possible object boundary to be safe.
  size_t const addr_offset = addr_to_offset(align_up(addr, HeapWordSize << _shifter));
  size_t const limit_offset = addr_to_offset(limit);
  size_t const nextOffset = _bm.get_next_one_offset(addr_offset, limit_offset);
  return offset_to_addr(nextOffset);
}

inline void MarkBitMap::mark(HeapWord* addr) {
  check_mark(addr);
  _bm.set_bit(addr_to_offset(addr));
}

inline void MarkBitMap::mark(oop obj) {
  return mark(cast_from_oop<HeapWord*>(obj));
}

inline void MarkBitMap::clear(HeapWord* addr) {
  check_mark(addr);
  _bm.clear_bit(addr_to_offset(addr));
}

inline bool MarkBitMap::par_mark(HeapWord* addr) {
  check_mark(addr);
  return _bm.par_set_bit(addr_to_offset(addr));
}

inline bool MarkBitMap::par_mark(oop obj) {
  return par_mark(cast_from_oop<HeapWord*>(obj));
}

inline bool MarkBitMap::is_marked(oop obj) const{
  return is_marked(cast_from_oop<HeapWord*>(obj));
}

inline void MarkBitMap::clear(oop obj) {
  clear(cast_from_oop<HeapWord*>(obj));
}

#endif // SHARE_GC_SHARED_MARKBITMAP_INLINE_HPP
