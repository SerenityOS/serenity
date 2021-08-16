/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1BLOCKOFFSETTABLE_INLINE_HPP
#define SHARE_GC_G1_G1BLOCKOFFSETTABLE_INLINE_HPP

#include "gc/g1/g1BlockOffsetTable.hpp"

#include "gc/g1/heapRegion.hpp"
#include "gc/shared/memset_with_concurrent_readers.hpp"
#include "runtime/atomic.hpp"

inline HeapWord* G1BlockOffsetTablePart::block_start(const void* addr) {
  assert(addr >= _hr->bottom() && addr < _hr->top(), "invalid address");
  HeapWord* q = block_at_or_preceding(addr);
  return forward_to_block_containing_addr(q, addr);
}

inline HeapWord* G1BlockOffsetTablePart::block_start_const(const void* addr) const {
  assert(addr >= _hr->bottom() && addr < _hr->top(), "invalid address");
  HeapWord* q = block_at_or_preceding(addr);
  HeapWord* n = q + block_size(q);
  return forward_to_block_containing_addr_const(q, n, addr);
}

u_char G1BlockOffsetTable::offset_array(size_t index) const {
  check_index(index, "index out of range");
  return Atomic::load(&_offset_array[index]);
}

void G1BlockOffsetTable::set_offset_array_raw(size_t index, u_char offset) {
  Atomic::store(&_offset_array[index], offset);
}

void G1BlockOffsetTable::set_offset_array(size_t index, u_char offset) {
  check_index(index, "index out of range");
  set_offset_array_raw(index, offset);
}

void G1BlockOffsetTable::set_offset_array(size_t index, HeapWord* high, HeapWord* low) {
  check_index(index, "index out of range");
  assert(high >= low, "addresses out of order");
  size_t offset = pointer_delta(high, low);
  check_offset(offset, "offset too large");
  set_offset_array(index, (u_char)offset);
}

void G1BlockOffsetTable::set_offset_array(size_t left, size_t right, u_char offset) {
  check_index(right, "right index out of range");
  assert(left <= right, "indexes out of order");
  size_t num_cards = right - left + 1;
  memset_with_concurrent_readers
    (const_cast<u_char*> (&_offset_array[left]), offset, num_cards);
}

// Variant of index_for that does not check the index for validity.
inline size_t G1BlockOffsetTable::index_for_raw(const void* p) const {
  return pointer_delta((char*)p, _reserved.start(), sizeof(char)) >> BOTConstants::LogN;
}

inline size_t G1BlockOffsetTable::index_for(const void* p) const {
  char* pc = (char*)p;
  assert(pc >= (char*)_reserved.start() &&
         pc <  (char*)_reserved.end(),
         "p (" PTR_FORMAT ") not in reserved [" PTR_FORMAT ", " PTR_FORMAT ")",
         p2i(p), p2i(_reserved.start()), p2i(_reserved.end()));
  size_t result = index_for_raw(p);
  check_index(result, "bad index from address");
  return result;
}

inline HeapWord* G1BlockOffsetTable::address_for_index(size_t index) const {
  check_index(index, "index out of range");
  HeapWord* result = address_for_index_raw(index);
  assert(result >= _reserved.start() && result < _reserved.end(),
         "bad address from index result " PTR_FORMAT
         " _reserved.start() " PTR_FORMAT " _reserved.end() " PTR_FORMAT,
         p2i(result), p2i(_reserved.start()), p2i(_reserved.end()));
  return result;
}

inline size_t G1BlockOffsetTablePart::block_size(const HeapWord* p) const {
  return _hr->block_size(p);
}

inline HeapWord* G1BlockOffsetTablePart::block_at_or_preceding(const void* addr) const {
  assert(_object_can_span || _bot->offset_array(_bot->index_for(_hr->bottom())) == 0,
         "Object crossed region boundary, found offset %u instead of 0",
         (uint) _bot->offset_array(_bot->index_for(_hr->bottom())));

  // We must make sure that the offset table entry we use is valid.
  assert(addr < _next_offset_threshold, "Precondition");

  size_t index = _bot->index_for(addr);

  HeapWord* q = _bot->address_for_index(index);

  uint offset = _bot->offset_array(index);  // Extend u_char to uint.
  while (offset >= BOTConstants::N_words) {
    // The excess of the offset from N_words indicates a power of Base
    // to go back by.
    size_t n_cards_back = BOTConstants::entry_to_cards_back(offset);
    q -= (BOTConstants::N_words * n_cards_back);
    index -= n_cards_back;
    offset = _bot->offset_array(index);
  }
  assert(offset < BOTConstants::N_words, "offset too large");
  q -= offset;
  return q;
}

inline HeapWord* G1BlockOffsetTablePart::forward_to_block_containing_addr_const(HeapWord* q, HeapWord* n,
                                                                                const void* addr) const {
  while (n <= addr) {
    q = n;
    oop obj = cast_to_oop(q);
    if (obj->klass_or_null_acquire() == NULL) {
      return q;
    }
    n += block_size(q);
  }
  assert(q <= n, "wrong order for q and addr");
  assert(addr < n, "wrong order for addr and n");
  return q;
}

inline HeapWord* G1BlockOffsetTablePart::forward_to_block_containing_addr(HeapWord* q,
                                                                          const void* addr) {
  if (cast_to_oop(q)->klass_or_null_acquire() == NULL) {
    return q;
  }
  HeapWord* n = q + block_size(q);
  // In the normal case, where the query "addr" is a card boundary, and the
  // offset table chunks are the same size as cards, the block starting at
  // "q" will contain addr, so the test below will fail, and we'll fall
  // through quickly.
  if (n <= addr) {
    q = forward_to_block_containing_addr_slow(q, n, addr);
  }
  assert(q <= addr, "wrong order for current and arg");
  return q;
}

#endif // SHARE_GC_G1_G1BLOCKOFFSETTABLE_INLINE_HPP
