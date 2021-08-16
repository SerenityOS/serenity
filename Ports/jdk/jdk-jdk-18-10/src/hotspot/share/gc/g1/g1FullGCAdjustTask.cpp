/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/classLoaderDataGraph.hpp"
#include "gc/g1/g1CollectedHeap.hpp"
#include "gc/g1/g1ConcurrentMarkBitMap.inline.hpp"
#include "gc/g1/g1FullCollector.inline.hpp"
#include "gc/g1/g1FullGCAdjustTask.hpp"
#include "gc/g1/g1FullGCCompactionPoint.hpp"
#include "gc/g1/g1FullGCMarker.hpp"
#include "gc/g1/g1FullGCOopClosures.inline.hpp"
#include "gc/g1/heapRegion.inline.hpp"
#include "gc/shared/gcTraceTime.inline.hpp"
#include "gc/shared/referenceProcessor.hpp"
#include "gc/shared/weakProcessor.inline.hpp"
#include "logging/log.hpp"
#include "memory/iterator.inline.hpp"
#include "runtime/atomic.hpp"

class G1AdjustLiveClosure : public StackObj {
  G1AdjustClosure* _adjust_closure;
public:
  G1AdjustLiveClosure(G1AdjustClosure* cl) :
    _adjust_closure(cl) { }

  size_t apply(oop object) {
    return object->oop_iterate_size(_adjust_closure);
  }
};

class G1AdjustRegionClosure : public HeapRegionClosure {
  G1FullCollector* _collector;
  G1CMBitMap* _bitmap;
  uint _worker_id;
 public:
  G1AdjustRegionClosure(G1FullCollector* collector, uint worker_id) :
    _collector(collector),
    _bitmap(collector->mark_bitmap()),
    _worker_id(worker_id) { }

  bool do_heap_region(HeapRegion* r) {
    G1AdjustClosure cl(_collector);
    if (r->is_humongous()) {
      // Special handling for humongous regions to get somewhat better
      // work distribution.
      oop obj = cast_to_oop(r->humongous_start_region()->bottom());
      obj->oop_iterate(&cl, MemRegion(r->bottom(), r->top()));
    } else if (!r->is_closed_archive() && !r->is_free()) {
      // Closed archive regions never change references and only contain
      // references into other closed regions and are always live. Free
      // regions do not contain objects to iterate. So skip both.
      G1AdjustLiveClosure adjust(&cl);
      r->apply_to_marked_objects(_bitmap, &adjust);
    }
    return false;
  }
};

G1FullGCAdjustTask::G1FullGCAdjustTask(G1FullCollector* collector) :
    G1FullGCTask("G1 Adjust", collector),
    _root_processor(G1CollectedHeap::heap(), collector->workers()),
    _references_done(false),
    _weak_proc_task(collector->workers()),
    _hrclaimer(collector->workers()),
    _adjust(collector) {
  // Need cleared claim bits for the roots processing
  ClassLoaderDataGraph::clear_claimed_marks();
}

void G1FullGCAdjustTask::work(uint worker_id) {
  Ticks start = Ticks::now();
  ResourceMark rm;

  // Adjust preserved marks first since they are not balanced.
  G1FullGCMarker* marker = collector()->marker(worker_id);
  marker->preserved_stack()->adjust_during_full_gc();

  // Adjust the weak roots.
  if (!Atomic::cmpxchg(&_references_done, false, true)) {
    G1CollectedHeap::heap()->ref_processor_stw()->weak_oops_do(&_adjust);
  }

  AlwaysTrueClosure always_alive;
  _weak_proc_task.work(worker_id, &always_alive, &_adjust);

  CLDToOopClosure adjust_cld(&_adjust, ClassLoaderData::_claim_strong);
  CodeBlobToOopClosure adjust_code(&_adjust, CodeBlobToOopClosure::FixRelocations);
  _root_processor.process_all_roots(&_adjust, &adjust_cld, &adjust_code);

  // Now adjust pointers region by region
  G1AdjustRegionClosure blk(collector(), worker_id);
  G1CollectedHeap::heap()->heap_region_par_iterate_from_worker_offset(&blk, &_hrclaimer, worker_id);
  log_task("Adjust task", worker_id, start);
}
