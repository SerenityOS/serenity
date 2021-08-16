/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_BLOCKOFFSETTABLE_INLINE_HPP
#define SHARE_GC_SHARED_BLOCKOFFSETTABLE_INLINE_HPP

#include "gc/shared/blockOffsetTable.hpp"

#include "gc/shared/space.hpp"
#include "runtime/safepoint.hpp"

//////////////////////////////////////////////////////////////////////////
// BlockOffsetTable inlines
//////////////////////////////////////////////////////////////////////////
inline HeapWord* BlockOffsetTable::block_start(const void* addr) const {
  if (addr >= _bottom && addr < _end) {
    return block_start_unsafe(addr);
  } else {
    return NULL;
  }
}

//////////////////////////////////////////////////////////////////////////
// BlockOffsetSharedArray inlines
//////////////////////////////////////////////////////////////////////////
inline size_t BlockOffsetSharedArray::index_for(const void* p) const {
  char* pc = (char*)p;
  assert(pc >= (char*)_reserved.start() &&
         pc <  (char*)_reserved.end(),
         "p not in range.");
  size_t delta = pointer_delta(pc, _reserved.start(), sizeof(char));
  size_t result = delta >> BOTConstants::LogN;
  assert(result < _vs.committed_size(), "bad index from address");
  return result;
}

inline HeapWord* BlockOffsetSharedArray::address_for_index(size_t index) const {
  assert(index < _vs.committed_size(), "bad index");
  HeapWord* result = _reserved.start() + (index << BOTConstants::LogN_words);
  assert(result >= _reserved.start() && result < _reserved.end(),
         "bad address from index");
  return result;
}

inline void BlockOffsetSharedArray::check_reducing_assertion(bool reducing) {
    assert(reducing || !SafepointSynchronize::is_at_safepoint() || init_to_zero() ||
           Thread::current()->is_VM_thread() ||
           Thread::current()->is_ConcurrentGC_thread() ||
           ((!Thread::current()->is_ConcurrentGC_thread()) &&
            ParGCRareEvent_lock->owned_by_self()), "Crack");
}

#endif // SHARE_GC_SHARED_BLOCKOFFSETTABLE_INLINE_HPP
