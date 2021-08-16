/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_THREADLOCALALLOCBUFFER_INLINE_HPP
#define SHARE_GC_SHARED_THREADLOCALALLOCBUFFER_INLINE_HPP

#include "gc/shared/threadLocalAllocBuffer.hpp"

#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/tlab_globals.hpp"
#include "memory/universe.hpp"
#include "logging/log.hpp"
#include "runtime/osThread.hpp"
#include "runtime/thread.hpp"
#include "utilities/copy.hpp"

inline HeapWord* ThreadLocalAllocBuffer::allocate(size_t size) {
  invariants();
  HeapWord* obj = top();
  if (pointer_delta(end(), obj) >= size) {
    // successful thread-local allocation
#ifdef ASSERT
    // Skip mangling the space corresponding to the object header to
    // ensure that the returned space is not considered parsable by
    // any concurrent GC thread.
    size_t hdr_size = oopDesc::header_size();
    Copy::fill_to_words(obj + hdr_size, size - hdr_size, badHeapWordVal);
#endif // ASSERT
    // This addition is safe because we know that top is
    // at least size below end, so the add can't wrap.
    set_top(obj + size);

    invariants();
    return obj;
  }
  return NULL;
}

inline size_t ThreadLocalAllocBuffer::compute_size(size_t obj_size) {
  // Compute the size for the new TLAB.
  // The "last" tlab may be smaller to reduce fragmentation.
  // unsafe_max_tlab_alloc is just a hint.
  const size_t available_size = Universe::heap()->unsafe_max_tlab_alloc(thread()) / HeapWordSize;
  size_t new_tlab_size = MIN3(available_size, desired_size() + align_object_size(obj_size), max_size());

  // Make sure there's enough room for object and filler int[].
  if (new_tlab_size < compute_min_size(obj_size)) {
    // If there isn't enough room for the allocation, return failure.
    log_trace(gc, tlab)("ThreadLocalAllocBuffer::compute_size(" SIZE_FORMAT ") returns failure",
                        obj_size);
    return 0;
  }
  log_trace(gc, tlab)("ThreadLocalAllocBuffer::compute_size(" SIZE_FORMAT ") returns " SIZE_FORMAT,
                      obj_size, new_tlab_size);
  return new_tlab_size;
}

inline size_t ThreadLocalAllocBuffer::compute_min_size(size_t obj_size) {
  const size_t aligned_obj_size = align_object_size(obj_size);
  const size_t size_with_reserve = aligned_obj_size + alignment_reserve();
  return MAX2(size_with_reserve, heap_word_size(MinTLABSize));
}

void ThreadLocalAllocBuffer::record_slow_allocation(size_t obj_size) {
  // Raise size required to bypass TLAB next time. Why? Else there's
  // a risk that a thread that repeatedly allocates objects of one
  // size will get stuck on this slow path.

  set_refill_waste_limit(refill_waste_limit() + refill_waste_limit_increment());

  _slow_allocations++;

  log_develop_trace(gc, tlab)("TLAB: %s thread: " INTPTR_FORMAT " [id: %2d]"
                              " obj: " SIZE_FORMAT
                              " free: " SIZE_FORMAT
                              " waste: " SIZE_FORMAT,
                              "slow", p2i(thread()), thread()->osthread()->thread_id(),
                              obj_size, free(), refill_waste_limit());
}

#endif // SHARE_GC_SHARED_THREADLOCALALLOCBUFFER_INLINE_HPP
