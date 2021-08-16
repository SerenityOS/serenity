/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_Z_ZLIVEMAP_INLINE_HPP
#define SHARE_GC_Z_ZLIVEMAP_INLINE_HPP

#include "gc/z/zLiveMap.hpp"

#include "gc/z/zBitMap.inline.hpp"
#include "gc/z/zMark.hpp"
#include "gc/z/zOop.inline.hpp"
#include "gc/z/zUtils.inline.hpp"
#include "runtime/atomic.hpp"
#include "utilities/bitMap.inline.hpp"
#include "utilities/debug.hpp"

inline void ZLiveMap::reset() {
  _seqnum = 0;
}

inline bool ZLiveMap::is_marked() const {
  return Atomic::load_acquire(&_seqnum) == ZGlobalSeqNum;
}

inline uint32_t ZLiveMap::live_objects() const {
  assert(ZGlobalPhase != ZPhaseMark, "Invalid phase");
  return _live_objects;
}

inline size_t ZLiveMap::live_bytes() const {
  assert(ZGlobalPhase != ZPhaseMark, "Invalid phase");
  return _live_bytes;
}

inline const BitMapView ZLiveMap::segment_live_bits() const {
  return BitMapView(const_cast<BitMap::bm_word_t*>(&_segment_live_bits), nsegments);
}

inline const BitMapView ZLiveMap::segment_claim_bits() const {
  return BitMapView(const_cast<BitMap::bm_word_t*>(&_segment_claim_bits), nsegments);
}

inline BitMapView ZLiveMap::segment_live_bits() {
  return BitMapView(&_segment_live_bits, nsegments);
}

inline BitMapView ZLiveMap::segment_claim_bits() {
  return BitMapView(&_segment_claim_bits, nsegments);
}

inline bool ZLiveMap::is_segment_live(BitMap::idx_t segment) const {
  return segment_live_bits().par_at(segment);
}

inline bool ZLiveMap::set_segment_live(BitMap::idx_t segment) {
  return segment_live_bits().par_set_bit(segment, memory_order_release);
}

inline bool ZLiveMap::claim_segment(BitMap::idx_t segment) {
  return segment_claim_bits().par_set_bit(segment, memory_order_acq_rel);
}

inline BitMap::idx_t ZLiveMap::first_live_segment() const {
  return segment_live_bits().get_next_one_offset(0, nsegments);
}

inline BitMap::idx_t ZLiveMap::next_live_segment(BitMap::idx_t segment) const {
  return segment_live_bits().get_next_one_offset(segment + 1, nsegments);
}

inline BitMap::idx_t ZLiveMap::segment_size() const {
  return _bitmap.size() / nsegments;
}

inline BitMap::idx_t ZLiveMap::index_to_segment(BitMap::idx_t index) const {
  return index >> _segment_shift;
}

inline bool ZLiveMap::get(size_t index) const {
  BitMap::idx_t segment = index_to_segment(index);
  return is_marked() &&              // Page is marked
         is_segment_live(segment) && // Segment is marked
         _bitmap.at(index);          // Object is marked
}

inline bool ZLiveMap::set(size_t index, bool finalizable, bool& inc_live) {
  if (!is_marked()) {
    // First object to be marked during this
    // cycle, reset marking information.
    reset(index);
  }

  const BitMap::idx_t segment = index_to_segment(index);
  if (!is_segment_live(segment)) {
    // First object to be marked in this segment during
    // this cycle, reset segment bitmap.
    reset_segment(segment);
  }

  return _bitmap.par_set_bit_pair(index, finalizable, inc_live);
}

inline void ZLiveMap::inc_live(uint32_t objects, size_t bytes) {
  Atomic::add(&_live_objects, objects);
  Atomic::add(&_live_bytes, bytes);
}

inline BitMap::idx_t ZLiveMap::segment_start(BitMap::idx_t segment) const {
  return segment_size() * segment;
}

inline BitMap::idx_t ZLiveMap::segment_end(BitMap::idx_t segment) const {
  return segment_start(segment) + segment_size();
}

inline void ZLiveMap::iterate_segment(ObjectClosure* cl, BitMap::idx_t segment, uintptr_t page_start, size_t page_object_alignment_shift) {
  assert(is_segment_live(segment), "Must be");

  const BitMap::idx_t start_index = segment_start(segment);
  const BitMap::idx_t end_index   = segment_end(segment);
  BitMap::idx_t index = _bitmap.get_next_one_offset(start_index, end_index);

  while (index < end_index) {
    // Calculate object address
    const uintptr_t addr = page_start + ((index / 2) << page_object_alignment_shift);

    // Get the size of the object before calling the closure, which
    // might overwrite the object in case we are relocating in-place.
    const size_t size = ZUtils::object_size(addr);

    // Apply closure
    cl->do_object(ZOop::from_address(addr));

    // Find next bit after this object
    const uintptr_t next_addr = align_up(addr + size, 1 << page_object_alignment_shift);
    const BitMap::idx_t next_index = ((next_addr - page_start) >> page_object_alignment_shift) * 2;
    if (next_index >= end_index) {
      // End of live map
      break;
    }

    index = _bitmap.get_next_one_offset(next_index, end_index);
  }
}

inline void ZLiveMap::iterate(ObjectClosure* cl, uintptr_t page_start, size_t page_object_alignment_shift) {
  if (is_marked()) {
    for (BitMap::idx_t segment = first_live_segment(); segment < nsegments; segment = next_live_segment(segment)) {
      // For each live segment
      iterate_segment(cl, segment, page_start, page_object_alignment_shift);
    }
  }
}

#endif // SHARE_GC_Z_ZLIVEMAP_INLINE_HPP
