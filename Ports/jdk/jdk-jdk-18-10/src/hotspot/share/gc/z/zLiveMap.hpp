/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_Z_ZLIVEMAP_HPP
#define SHARE_GC_Z_ZLIVEMAP_HPP

#include "gc/z/zBitMap.hpp"
#include "memory/allocation.hpp"

class ObjectClosure;

class ZLiveMap {
  friend class ZLiveMapTest;

private:
  static const size_t nsegments = 64;

  volatile uint32_t _seqnum;
  volatile uint32_t _live_objects;
  volatile size_t   _live_bytes;
  BitMap::bm_word_t _segment_live_bits;
  BitMap::bm_word_t _segment_claim_bits;
  ZBitMap           _bitmap;
  size_t            _segment_shift;

  const BitMapView segment_live_bits() const;
  const BitMapView segment_claim_bits() const;

  BitMapView segment_live_bits();
  BitMapView segment_claim_bits();

  BitMap::idx_t segment_size() const;

  BitMap::idx_t segment_start(BitMap::idx_t segment) const;
  BitMap::idx_t segment_end(BitMap::idx_t segment) const;

  bool is_segment_live(BitMap::idx_t segment) const;
  bool set_segment_live(BitMap::idx_t segment);

  BitMap::idx_t first_live_segment() const;
  BitMap::idx_t next_live_segment(BitMap::idx_t segment) const;
  BitMap::idx_t index_to_segment(BitMap::idx_t index) const;

  bool claim_segment(BitMap::idx_t segment);

  void reset(size_t index);
  void reset_segment(BitMap::idx_t segment);

  void iterate_segment(ObjectClosure* cl, BitMap::idx_t segment, uintptr_t page_start, size_t page_object_alignment_shift);

public:
  ZLiveMap(uint32_t size);

  void reset();
  void resize(uint32_t size);

  bool is_marked() const;

  uint32_t live_objects() const;
  size_t live_bytes() const;

  bool get(size_t index) const;
  bool set(size_t index, bool finalizable, bool& inc_live);

  void inc_live(uint32_t objects, size_t bytes);

  void iterate(ObjectClosure* cl, uintptr_t page_start, size_t page_object_alignment_shift);
};

#endif // SHARE_GC_Z_ZLIVEMAP_HPP
