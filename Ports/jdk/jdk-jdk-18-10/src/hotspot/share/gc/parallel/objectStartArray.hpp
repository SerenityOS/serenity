/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_PARALLEL_OBJECTSTARTARRAY_HPP
#define SHARE_GC_PARALLEL_OBJECTSTARTARRAY_HPP

#include "gc/parallel/psVirtualspace.hpp"
#include "memory/allocation.hpp"
#include "memory/memRegion.hpp"
#include "oops/oop.hpp"

//
// This class can be used to locate the beginning of an object in the
// covered region.
//

class ObjectStartArray : public CHeapObj<mtGC> {
 friend class VerifyObjectStartArrayClosure;

 private:
  PSVirtualSpace  _virtual_space;
  MemRegion       _reserved_region;
  MemRegion       _covered_region;
  MemRegion       _blocks_region;
  jbyte*          _raw_base;
  jbyte*          _offset_base;

 public:

  enum BlockValueConstants {
    clean_block                  = -1
  };

  enum BlockSizeConstants {
    block_shift                  = 9,
    block_size                   = 1 << block_shift,
    block_size_in_words          = block_size / sizeof(HeapWord)
  };

 protected:

  // Mapping from address to object start array entry
  jbyte* block_for_addr(void* p) const {
    assert(_covered_region.contains(p),
           "out of bounds access to object start array");
    jbyte* result = &_offset_base[uintptr_t(p) >> block_shift];
    assert(_blocks_region.contains(result),
           "out of bounds result in byte_for");
    return result;
  }

  // Mapping from object start array entry to address of first word
  HeapWord* addr_for_block(jbyte* p) {
    assert(_blocks_region.contains(p),
           "out of bounds access to object start array");
    size_t delta = pointer_delta(p, _offset_base, sizeof(jbyte));
    HeapWord* result = (HeapWord*) (delta << block_shift);
    assert(_covered_region.contains(result),
           "out of bounds accessor from card marking array");
    return result;
  }

  // Mapping that includes the derived offset.
  // If the block is clean, returns the last address in the covered region.
  // If the block is < index 0, returns the start of the covered region.
  HeapWord* offset_addr_for_block(jbyte* p) const {
    // We have to do this before the assert
    if (p < _raw_base) {
      return _covered_region.start();
    }

    assert(_blocks_region.contains(p),
           "out of bounds access to object start array");

    if (*p == clean_block) {
      return _covered_region.end();
    }

    size_t delta = pointer_delta(p, _offset_base, sizeof(jbyte));
    HeapWord* result = (HeapWord*) (delta << block_shift);
    result += *p;

    assert(_covered_region.contains(result),
           "out of bounds accessor from card marking array");

    return result;
  }

 public:

  // This method is in lieu of a constructor, so that this class can be
  // embedded inline in other classes.
  void initialize(MemRegion reserved_region);

  void set_covered_region(MemRegion mr);

  void reset();

  MemRegion covered_region() { return _covered_region; }

#define assert_covered_region_contains(addr)                                                                 \
        assert(_covered_region.contains(addr),                                                               \
               #addr " (" PTR_FORMAT ") is not in covered region [" PTR_FORMAT ", " PTR_FORMAT "]",          \
               p2i(addr), p2i(_covered_region.start()), p2i(_covered_region.end()))

  void allocate_block(HeapWord* p) {
    assert_covered_region_contains(p);
    jbyte* block = block_for_addr(p);
    HeapWord* block_base = addr_for_block(block);
    size_t offset = pointer_delta(p, block_base, sizeof(HeapWord*));
    assert(offset < 128, "Sanity");
    // When doing MT offsets, we can't assert this.
    //assert(offset > *block, "Found backwards allocation");
    *block = (jbyte)offset;
  }

  // Optimized for finding the first object that crosses into
  // a given block. The blocks contain the offset of the last
  // object in that block. Scroll backwards by one, and the first
  // object hit should be at the beginning of the block
  inline HeapWord* object_start(HeapWord* addr) const;

  bool is_block_allocated(HeapWord* addr) {
    assert_covered_region_contains(addr);
    jbyte* block = block_for_addr(addr);
    return *block != clean_block;
  }

  // Return true if an object starts in the range of heap addresses.
  // If an object starts at an address corresponding to
  // "start", the method will return true.
  bool object_starts_in_range(HeapWord* start_addr, HeapWord* end_addr) const;
};

#endif // SHARE_GC_PARALLEL_OBJECTSTARTARRAY_HPP
