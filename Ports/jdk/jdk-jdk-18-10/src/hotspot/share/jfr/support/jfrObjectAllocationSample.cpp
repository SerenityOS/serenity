/*
* Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/shared/threadLocalAllocBuffer.inline.hpp"
#include "gc/shared/tlab_globals.hpp"
#include "jfr/jfrEvents.hpp"
#include "jfr/support/jfrObjectAllocationSample.hpp"
#include "utilities/globalDefinitions.hpp"

static THREAD_LOCAL int64_t _last_allocated_bytes = 0;

inline void send_allocation_sample(const Klass* klass, int64_t allocated_bytes) {
  assert(allocated_bytes > 0, "invariant");
  EventObjectAllocationSample event;
  if (event.should_commit()) {
    const size_t weight = allocated_bytes - _last_allocated_bytes;
    assert(weight > 0, "invariant");
    event.set_objectClass(klass);
    event.set_weight(weight);
    event.commit();
    _last_allocated_bytes = allocated_bytes;
  }
}

inline bool send_allocation_sample_with_result(const Klass* klass, int64_t allocated_bytes) {
  assert(allocated_bytes > 0, "invariant");
  EventObjectAllocationSample event;
  if (event.should_commit()) {
    const size_t weight = allocated_bytes - _last_allocated_bytes;
    assert(weight > 0, "invariant");
    event.set_objectClass(klass);
    event.set_weight(weight);
    event.commit();
    _last_allocated_bytes = allocated_bytes;
    return true;
  }
  return false;
}

inline intptr_t estimate_tlab_size_bytes(Thread* thread) {
  const size_t desired_tlab_size_bytes = thread->tlab().desired_size() * HeapWordSize;
  const size_t alignment_reserve_bytes = thread->tlab().alignment_reserve_in_bytes();
  assert(desired_tlab_size_bytes > alignment_reserve_bytes, "invariant");
  return static_cast<intptr_t>(desired_tlab_size_bytes - alignment_reserve_bytes);
}

inline int64_t load_allocated_bytes(Thread* thread) {
  assert(thread != NULL, "invariant");
  const int64_t allocated_bytes = thread->allocated_bytes();
  if (allocated_bytes < _last_allocated_bytes) {
    // A hw thread can detach and reattach to the VM, and when it does,
    // it gets a new JavaThread representation. The thread local variable
    // tracking _last_allocated_bytes is mapped to the existing hw thread,
    // so it needs to be reset.
    _last_allocated_bytes = 0;
  }
  return allocated_bytes == _last_allocated_bytes ? 0 : allocated_bytes;
}

// To avoid large objects from being undersampled compared to the regular TLAB samples,
// the data amount is normalized as if it was a TLAB, giving a number of TLAB sampling attempts to the large object.
static void normalize_as_tlab_and_send_allocation_samples(const Klass* klass, intptr_t obj_alloc_size_bytes, Thread* thread) {
  const int64_t allocated_bytes = load_allocated_bytes(thread);
  assert(allocated_bytes > 0, "invariant"); // obj_alloc_size_bytes is already attributed to allocated_bytes at this point.
  if (!UseTLAB) {
    send_allocation_sample(klass, allocated_bytes);
    return;
  }
  const intptr_t tlab_size_bytes = estimate_tlab_size_bytes(thread);
  if (allocated_bytes - _last_allocated_bytes < tlab_size_bytes) {
    return;
  }
  assert(obj_alloc_size_bytes > 0, "invariant");
  do {
    if (send_allocation_sample_with_result(klass, allocated_bytes)) {
      return;
    }
    obj_alloc_size_bytes -= tlab_size_bytes;
  } while (obj_alloc_size_bytes > 0);
}

void JfrObjectAllocationSample::send_event(const Klass* klass, size_t alloc_size, bool outside_tlab, Thread* thread) {
  if (outside_tlab) {
    normalize_as_tlab_and_send_allocation_samples(klass, static_cast<intptr_t>(alloc_size), thread);
    return;
  }
  const int64_t allocated_bytes = load_allocated_bytes(thread);
  if (allocated_bytes == 0) {
    return;
  }
  send_allocation_sample(klass, allocated_bytes);
}
