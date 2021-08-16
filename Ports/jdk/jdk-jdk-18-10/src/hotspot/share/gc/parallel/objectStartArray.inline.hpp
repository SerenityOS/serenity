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
 *
 */

#ifndef SHARE_GC_PARALLEL_OBJECTSTARTARRAY_INLINE_HPP
#define SHARE_GC_PARALLEL_OBJECTSTARTARRAY_INLINE_HPP

#include "gc/parallel/objectStartArray.hpp"

// Optimized for finding the first object that crosses into
// a given block. The blocks contain the offset of the last
// object in that block. Scroll backwards by one, and the first
// object hit should be at the beginning of the block
HeapWord* ObjectStartArray::object_start(HeapWord* addr) const {
  assert_covered_region_contains(addr);
  jbyte* block = block_for_addr(addr);
  HeapWord* scroll_forward = offset_addr_for_block(block--);
  while (scroll_forward > addr) {
    scroll_forward = offset_addr_for_block(block--);
  }

  HeapWord* next = scroll_forward;
  while (next <= addr) {
    scroll_forward = next;
    next += cast_to_oop(next)->size();
  }
  assert(scroll_forward <= addr, "wrong order for current and arg");
  assert(addr <= next, "wrong order for arg and next");
  return scroll_forward;
}


#endif // SHARE_GC_PARALLEL_OBJECTSTARTARRAY_INLINE_HPP
