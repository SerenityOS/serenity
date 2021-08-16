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

#ifndef SHARE_GC_Z_ZFORWARDING_INLINE_HPP
#define SHARE_GC_Z_ZFORWARDING_INLINE_HPP

#include "gc/z/zForwarding.hpp"

#include "gc/z/zAttachedArray.inline.hpp"
#include "gc/z/zForwardingAllocator.inline.hpp"
#include "gc/z/zHash.inline.hpp"
#include "gc/z/zHeap.hpp"
#include "gc/z/zLock.inline.hpp"
#include "gc/z/zPage.inline.hpp"
#include "gc/z/zVirtualMemory.inline.hpp"
#include "runtime/atomic.hpp"
#include "utilities/debug.hpp"
#include "utilities/powerOfTwo.hpp"

inline uint32_t ZForwarding::nentries(const ZPage* page) {
  // The number returned by the function is used to size the hash table of
  // forwarding entries for this page. This hash table uses linear probing.
  // The size of the table must be a power of two to allow for quick and
  // inexpensive indexing/masking. The table is also sized to have a load
  // factor of 50%, i.e. sized to have double the number of entries actually
  // inserted, to allow for good lookup/insert performance.
  return round_up_power_of_2(page->live_objects() * 2);
}

inline ZForwarding* ZForwarding::alloc(ZForwardingAllocator* allocator, ZPage* page) {
  const size_t nentries = ZForwarding::nentries(page);
  void* const addr = AttachedArray::alloc(allocator, nentries);
  return ::new (addr) ZForwarding(page, nentries);
}

inline ZForwarding::ZForwarding(ZPage* page, size_t nentries) :
    _virtual(page->virtual_memory()),
    _object_alignment_shift(page->object_alignment_shift()),
    _entries(nentries),
    _page(page),
    _ref_lock(),
    _ref_count(1),
    _ref_abort(false),
    _in_place(false) {}

inline uint8_t ZForwarding::type() const {
  return _page->type();
}

inline uintptr_t ZForwarding::start() const {
  return _virtual.start();
}

inline size_t ZForwarding::size() const {
  return _virtual.size();
}

inline size_t ZForwarding::object_alignment_shift() const {
  return _object_alignment_shift;
}

inline void ZForwarding::object_iterate(ObjectClosure *cl) {
  return _page->object_iterate(cl);
}

inline void ZForwarding::set_in_place() {
  _in_place = true;
}

inline bool ZForwarding::in_place() const {
  return _in_place;
}

inline ZForwardingEntry* ZForwarding::entries() const {
  return _entries(this);
}

inline ZForwardingEntry ZForwarding::at(ZForwardingCursor* cursor) const {
  // Load acquire for correctness with regards to
  // accesses to the contents of the forwarded object.
  return Atomic::load_acquire(entries() + *cursor);
}

inline ZForwardingEntry ZForwarding::first(uintptr_t from_index, ZForwardingCursor* cursor) const {
  const size_t mask = _entries.length() - 1;
  const size_t hash = ZHash::uint32_to_uint32((uint32_t)from_index);
  *cursor = hash & mask;
  return at(cursor);
}

inline ZForwardingEntry ZForwarding::next(ZForwardingCursor* cursor) const {
  const size_t mask = _entries.length() - 1;
  *cursor = (*cursor + 1) & mask;
  return at(cursor);
}

inline ZForwardingEntry ZForwarding::find(uintptr_t from_index, ZForwardingCursor* cursor) const {
  // Reading entries in the table races with the atomic CAS done for
  // insertion into the table. This is safe because each entry is at
  // most updated once (from zero to something else).
  ZForwardingEntry entry = first(from_index, cursor);
  while (entry.populated()) {
    if (entry.from_index() == from_index) {
      // Match found, return matching entry
      return entry;
    }

    entry = next(cursor);
  }

  // Match not found, return empty entry
  return entry;
}

inline uintptr_t ZForwarding::insert(uintptr_t from_index, uintptr_t to_offset, ZForwardingCursor* cursor) {
  const ZForwardingEntry new_entry(from_index, to_offset);
  const ZForwardingEntry old_entry; // Empty

  for (;;) {
    const ZForwardingEntry prev_entry = Atomic::cmpxchg(entries() + *cursor, old_entry, new_entry, memory_order_release);
    if (!prev_entry.populated()) {
      // Success
      return to_offset;
    }

    // Find next empty or matching entry
    ZForwardingEntry entry = at(cursor);
    while (entry.populated()) {
      if (entry.from_index() == from_index) {
        // Match found, return already inserted address
        return entry.to_offset();
      }

      entry = next(cursor);
    }
  }
}

#endif // SHARE_GC_Z_ZFORWARDING_INLINE_HPP
