/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/g1/g1FreeIdSet.hpp"
#include "memory/allocation.inline.hpp"
#include "runtime/atomic.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"
#include "utilities/powerOfTwo.hpp"

G1FreeIdSet::G1FreeIdSet(uint start, uint size) :
  _sem(size),          // counting semaphore for available ids
  _next(NULL),         // array of "next" indices
  _start(start),       // first id value
  _size(size),         // number of available ids
  _head_index_mask(0), // mask for extracting index from a _head value.
  _head(0)             // low part: index; high part: update counter
{
  assert(size != 0, "precondition");
  assert(start <= (UINT_MAX - size),
         "start (%u) + size (%u) overflow: ", start, size);
  // 2^shift must be greater than size. Equal is not permitted, because
  // size is the "end of list" value, and can be the index part of _head.
  uint shift = log2i(size) + 1;
  assert(shift <= (BitsPerWord / 2), "excessive size %u", size);
  _head_index_mask = (uintx(1) << shift) - 1;
  assert(size <= _head_index_mask, "invariant");
  _next = NEW_C_HEAP_ARRAY(uint, size, mtGC);
  for (uint i = 0; i < size; ++i) {
    _next[i] = i + 1;
  }
}

G1FreeIdSet::~G1FreeIdSet() {
  FREE_C_HEAP_ARRAY(uint, _next);
}

uint G1FreeIdSet::head_index(uintx head) const {
  return head & _head_index_mask;
}

uintx G1FreeIdSet::make_head(uint index, uintx old_head) const {
  // Include incremented old update counter to avoid ABA problem.
  return index | ((old_head & ~_head_index_mask) + 1 + _head_index_mask);
}

const uint Claimed = UINT_MAX;

uint G1FreeIdSet::claim_par_id() {
  _sem.wait();
  // Semaphore gate permits passage by no more than the number of
  // available ids, so there must be one that we can claim.  But there
  // may be multiple threads trying to claim ids at the same time.
  uintx old_head = Atomic::load(&_head);
  uint index;
  while (true) {
    index = head_index(old_head);
    assert(index < _size, "invariant");
    uintx new_head = make_head(_next[index], old_head);
    new_head = Atomic::cmpxchg(&_head, old_head, new_head);
    if (new_head == old_head) break;
    old_head = new_head;
  }
  DEBUG_ONLY(_next[index] = Claimed;)
  return _start + index;
}

void G1FreeIdSet::release_par_id(uint id) {
  uint index = id - _start;
  assert(index < _size, "invalid id %u", id);
  assert(_next[index] == Claimed, "precondition");
  uintx old_head = Atomic::load(&_head);
  while (true) {
    _next[index] = head_index(old_head);
    uintx new_head = make_head(index, old_head);
    new_head = Atomic::cmpxchg(&_head, old_head, new_head);
    if (new_head == old_head) break;
    old_head = new_head;
  }
  // Now that id has been released, permit another thread through the gate.
  _sem.signal();
}
