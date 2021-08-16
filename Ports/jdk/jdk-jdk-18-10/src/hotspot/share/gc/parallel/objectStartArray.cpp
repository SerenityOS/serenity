/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/parallel/objectStartArray.inline.hpp"
#include "gc/shared/cardTableBarrierSet.hpp"
#include "memory/allocation.inline.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/java.hpp"
#include "services/memTracker.hpp"
#include "utilities/align.hpp"

void ObjectStartArray::initialize(MemRegion reserved_region) {
  // We're based on the assumption that we use the same
  // size blocks as the card table.
  assert((int)block_size == (int)CardTable::card_size, "Sanity");
  assert((int)block_size <= 512, "block_size must be less than or equal to 512");

  // Calculate how much space must be reserved
  _reserved_region = reserved_region;

  size_t bytes_to_reserve = reserved_region.word_size() / block_size_in_words;
  assert(bytes_to_reserve > 0, "Sanity");

  bytes_to_reserve =
    align_up(bytes_to_reserve, os::vm_allocation_granularity());

  // Do not use large-pages for the backing store. The one large page region
  // will be used for the heap proper.
  ReservedSpace backing_store(bytes_to_reserve);
  if (!backing_store.is_reserved()) {
    vm_exit_during_initialization("Could not reserve space for ObjectStartArray");
  }
  MemTracker::record_virtual_memory_type((address)backing_store.base(), mtGC);

  // We do not commit any memory initially
  if (!_virtual_space.initialize(backing_store, 0)) {
    vm_exit_during_initialization("Could not commit space for ObjectStartArray");
  }

  _raw_base = (jbyte*)_virtual_space.low_boundary();

  if (_raw_base == NULL) {
    vm_exit_during_initialization("Could not get raw_base address");
  }

  MemTracker::record_virtual_memory_type((address)_raw_base, mtGC);


  _offset_base = _raw_base - (size_t(reserved_region.start()) >> block_shift);

  _covered_region.set_start(reserved_region.start());
  _covered_region.set_word_size(0);

  _blocks_region.set_start((HeapWord*)_raw_base);
  _blocks_region.set_word_size(0);
}

void ObjectStartArray::set_covered_region(MemRegion mr) {
  assert(_reserved_region.contains(mr), "MemRegion outside of reserved space");
  assert(_reserved_region.start() == mr.start(), "Attempt to move covered region");

  HeapWord* low_bound  = mr.start();
  HeapWord* high_bound = mr.end();
  assert((uintptr_t(low_bound)  & (block_size - 1))  == 0, "heap must start at block boundary");
  assert((uintptr_t(high_bound) & (block_size - 1))  == 0, "heap must end at block boundary");

  size_t requested_blocks_size_in_bytes = mr.word_size() / block_size_in_words;

  // Only commit memory in page sized chunks
  requested_blocks_size_in_bytes =
    align_up(requested_blocks_size_in_bytes, os::vm_page_size());

  _covered_region = mr;

  size_t current_blocks_size_in_bytes = _blocks_region.byte_size();

  if (requested_blocks_size_in_bytes > current_blocks_size_in_bytes) {
    // Expand
    size_t expand_by = requested_blocks_size_in_bytes - current_blocks_size_in_bytes;
    if (!_virtual_space.expand_by(expand_by)) {
      vm_exit_out_of_memory(expand_by, OOM_MMAP_ERROR, "object start array expansion");
    }
    // Clear *only* the newly allocated region
    memset(_blocks_region.end(), clean_block, expand_by);
  }

  if (requested_blocks_size_in_bytes < current_blocks_size_in_bytes) {
    // Shrink
    size_t shrink_by = current_blocks_size_in_bytes - requested_blocks_size_in_bytes;
    _virtual_space.shrink_by(shrink_by);
  }

  _blocks_region.set_word_size(requested_blocks_size_in_bytes / sizeof(HeapWord));

  assert(requested_blocks_size_in_bytes % sizeof(HeapWord) == 0, "Block table not expanded in word sized increment");
  assert(requested_blocks_size_in_bytes == _blocks_region.byte_size(), "Sanity");
  assert(block_for_addr(low_bound) == &_raw_base[0], "Checking start of map");
  assert(block_for_addr(high_bound-1) <= &_raw_base[_blocks_region.byte_size()-1], "Checking end of map");
}

void ObjectStartArray::reset() {
  memset(_blocks_region.start(), clean_block, _blocks_region.byte_size());
}

bool ObjectStartArray::object_starts_in_range(HeapWord* start_addr,
                                              HeapWord* end_addr) const {
  assert(start_addr <= end_addr,
         "Range is wrong. start_addr (" PTR_FORMAT ") is after end_addr (" PTR_FORMAT ")",
         p2i(start_addr), p2i(end_addr));

  jbyte* start_block = block_for_addr(start_addr);
  jbyte* end_block = block_for_addr(end_addr);

  for (jbyte* block = start_block; block <= end_block; block++) {
    if (*block != clean_block) {
      return true;
    }
  }

  return false;
}
