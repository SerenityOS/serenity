/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/g1/g1BarrierSet.inline.hpp"
#include "gc/g1/g1CollectedHeap.inline.hpp"
#include "gc/g1/g1SATBMarkQueueSet.hpp"
#include "gc/g1/g1ThreadLocalData.hpp"
#include "gc/g1/heapRegion.hpp"
#include "gc/shared/satbMarkQueue.hpp"
#include "oops/oop.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"

G1SATBMarkQueueSet::G1SATBMarkQueueSet(BufferNode::Allocator* allocator) :
  SATBMarkQueueSet(allocator)
{}

void G1SATBMarkQueueSet::handle_zero_index_for_thread(Thread* t) {
  G1SATBMarkQueueSet& qset = G1BarrierSet::satb_mark_queue_set();
  qset.handle_zero_index(qset.satb_queue_for_thread(t));
}

SATBMarkQueue& G1SATBMarkQueueSet::satb_queue_for_thread(Thread* const t) const {
  return G1ThreadLocalData::satb_mark_queue(t);
}

// Return true if a SATB buffer entry refers to an object that
// requires marking.
//
// The entry must point into the G1 heap.  In particular, it must not
// be a NULL pointer.  NULL pointers are pre-filtered and never
// inserted into a SATB buffer.
//
// An entry that is below the NTAMS pointer for the containing heap
// region requires marking. Such an entry must point to a valid object.
//
// An entry that is at least the NTAMS pointer for the containing heap
// region might be any of the following, none of which should be marked.
//
// * A reference to an object allocated since marking started.
//   According to SATB, such objects are implicitly kept live and do
//   not need to be dealt with via SATB buffer processing.
//
// * A reference to a young generation object. Young objects are
//   handled separately and are not marked by concurrent marking.
//
// * A stale reference to a young generation object. If a young
//   generation object reference is recorded and not filtered out
//   before being moved by a young collection, the reference becomes
//   stale.
//
// * A stale reference to an eagerly reclaimed humongous object.  If a
//   humongous object is recorded and then reclaimed, the reference
//   becomes stale.
//
// The stale reference cases are implicitly handled by the NTAMS
// comparison. Because of the possibility of stale references, buffer
// processing must be somewhat circumspect and not assume entries
// in an unfiltered buffer refer to valid objects.

static inline bool requires_marking(const void* entry, G1CollectedHeap* g1h) {
  // Includes rejection of NULL pointers.
  assert(g1h->is_in_reserved(entry),
         "Non-heap pointer in SATB buffer: " PTR_FORMAT, p2i(entry));

  HeapRegion* region = g1h->heap_region_containing(entry);
  assert(region != NULL, "No region for " PTR_FORMAT, p2i(entry));
  if (entry >= region->next_top_at_mark_start()) {
    return false;
  }

  assert(oopDesc::is_oop(cast_to_oop(entry), true /* ignore mark word */),
         "Invalid oop in SATB buffer: " PTR_FORMAT, p2i(entry));

  return true;
}

static inline bool discard_entry(const void* entry, G1CollectedHeap* g1h) {
  return !requires_marking(entry, g1h) || g1h->is_marked_next(cast_to_oop(entry));
}

// Workaround for not yet having std::bind.
class G1SATBMarkQueueFilterFn {
  G1CollectedHeap* _g1h;

public:
  G1SATBMarkQueueFilterFn() : _g1h(G1CollectedHeap::heap()) {}

  // Return true if entry should be filtered out (removed), false if
  // it should be retained.
  bool operator()(const void* entry) const {
    return discard_entry(entry, _g1h);
  }
};

void G1SATBMarkQueueSet::filter(SATBMarkQueue& queue) {
  apply_filter(G1SATBMarkQueueFilterFn(), queue);
}
