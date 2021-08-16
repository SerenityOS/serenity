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
#include "gc/g1/g1CollectedHeap.hpp"
#include "gc/g1/g1ConcurrentMarkBitMap.inline.hpp"
#include "gc/g1/g1FullCollector.inline.hpp"
#include "gc/g1/g1FullGCCompactionPoint.hpp"
#include "gc/g1/g1FullGCMarker.hpp"
#include "gc/g1/g1FullGCOopClosures.inline.hpp"
#include "gc/g1/g1FullGCPrepareTask.hpp"
#include "gc/g1/g1HotCardCache.hpp"
#include "gc/g1/heapRegion.inline.hpp"
#include "gc/shared/gcTraceTime.inline.hpp"
#include "gc/shared/referenceProcessor.hpp"
#include "logging/log.hpp"
#include "memory/iterator.inline.hpp"
#include "oops/oop.inline.hpp"
#include "utilities/ticks.hpp"

template<bool is_humongous>
void G1FullGCPrepareTask::G1CalculatePointersClosure::free_pinned_region(HeapRegion* hr) {
  _regions_freed = true;
  if (is_humongous) {
    _g1h->free_humongous_region(hr, nullptr);
  } else {
    _g1h->free_region(hr, nullptr);
  }
  prepare_for_compaction(hr);
  _collector->set_invalid(hr->hrm_index());
}

bool G1FullGCPrepareTask::G1CalculatePointersClosure::do_heap_region(HeapRegion* hr) {
  bool force_not_compacted = false;
  if (should_compact(hr)) {
    assert(!hr->is_humongous(), "moving humongous objects not supported.");
    prepare_for_compaction(hr);
  } else {
    // There is no need to iterate and forward objects in pinned regions ie.
    // prepare them for compaction. The adjust pointers phase will skip
    // work for them.
    assert(hr->containing_set() == nullptr, "already cleared by PrepareRegionsClosure");
    if (hr->is_humongous()) {
      oop obj = cast_to_oop(hr->humongous_start_region()->bottom());
      if (!_bitmap->is_marked(obj)) {
        free_pinned_region<true>(hr);
      }
    } else if (hr->is_open_archive()) {
      bool is_empty = _collector->live_words(hr->hrm_index()) == 0;
      if (is_empty) {
        free_pinned_region<false>(hr);
      }
    } else if (hr->is_closed_archive()) {
      // nothing to do with closed archive region
    } else {
      assert(MarkSweepDeadRatio > 0,
             "only skip compaction for other regions when MarkSweepDeadRatio > 0");

      // Too many live objects; skip compacting it.
      _collector->update_from_compacting_to_skip_compacting(hr->hrm_index());
      if (hr->is_young()) {
        // G1 updates the BOT for old region contents incrementally, but young regions
        // lack BOT information for performance reasons.
        // Recreate BOT information of high live ratio young regions here to keep expected
        // performance during scanning their card tables in the collection pauses later.
        hr->update_bot();
      }
      log_trace(gc, phases)("Phase 2: skip compaction region index: %u, live words: " SIZE_FORMAT,
                            hr->hrm_index(), _collector->live_words(hr->hrm_index()));
    }
  }

  // Reset data structures not valid after Full GC.
  reset_region_metadata(hr);

  return false;
}

G1FullGCPrepareTask::G1FullGCPrepareTask(G1FullCollector* collector) :
    G1FullGCTask("G1 Prepare Compact Task", collector),
    _freed_regions(false),
    _hrclaimer(collector->workers()) {
}

void G1FullGCPrepareTask::set_freed_regions() {
  if (!_freed_regions) {
    _freed_regions = true;
  }
}

bool G1FullGCPrepareTask::has_freed_regions() {
  return _freed_regions;
}

void G1FullGCPrepareTask::work(uint worker_id) {
  Ticks start = Ticks::now();
  G1FullGCCompactionPoint* compaction_point = collector()->compaction_point(worker_id);
  G1CalculatePointersClosure closure(collector(), compaction_point);
  G1CollectedHeap::heap()->heap_region_par_iterate_from_start(&closure, &_hrclaimer);

  compaction_point->update();

  // Check if any regions was freed by this worker and store in task.
  if (closure.freed_regions()) {
    set_freed_regions();
  }
  log_task("Prepare compaction task", worker_id, start);
}

G1FullGCPrepareTask::G1CalculatePointersClosure::G1CalculatePointersClosure(G1FullCollector* collector,
                                                                            G1FullGCCompactionPoint* cp) :
    _g1h(G1CollectedHeap::heap()),
    _collector(collector),
    _bitmap(collector->mark_bitmap()),
    _cp(cp),
    _regions_freed(false) { }

bool G1FullGCPrepareTask::G1CalculatePointersClosure::should_compact(HeapRegion* hr) {
  if (hr->is_pinned()) {
    return false;
  }
  size_t live_words = _collector->live_words(hr->hrm_index());
  size_t live_words_threshold = _collector->scope()->region_compaction_threshold();
  // High live ratio region will not be compacted.
  return live_words <= live_words_threshold;
}

void G1FullGCPrepareTask::G1CalculatePointersClosure::reset_region_metadata(HeapRegion* hr) {
  hr->rem_set()->clear();
  hr->clear_cardtable();

  G1HotCardCache* hcc = _g1h->hot_card_cache();
  if (hcc->use_cache()) {
    hcc->reset_card_counts(hr);
  }
}

G1FullGCPrepareTask::G1PrepareCompactLiveClosure::G1PrepareCompactLiveClosure(G1FullGCCompactionPoint* cp) :
    _cp(cp) { }

size_t G1FullGCPrepareTask::G1PrepareCompactLiveClosure::apply(oop object) {
  size_t size = object->size();
  _cp->forward(object, size);
  return size;
}

size_t G1FullGCPrepareTask::G1RePrepareClosure::apply(oop obj) {
  // We only re-prepare objects forwarded within the current region, so
  // skip objects that are already forwarded to another region.
  oop forwarded_to = obj->forwardee();
  if (forwarded_to != NULL && !_current->is_in(forwarded_to)) {
    return obj->size();
  }

  // Get size and forward.
  size_t size = obj->size();
  _cp->forward(obj, size);

  return size;
}

void G1FullGCPrepareTask::G1CalculatePointersClosure::prepare_for_compaction_work(G1FullGCCompactionPoint* cp,
                                                                                  HeapRegion* hr) {
  G1PrepareCompactLiveClosure prepare_compact(cp);
  hr->set_compaction_top(hr->bottom());
  hr->apply_to_marked_objects(_bitmap, &prepare_compact);
}

void G1FullGCPrepareTask::G1CalculatePointersClosure::prepare_for_compaction(HeapRegion* hr) {
  if (!_cp->is_initialized()) {
    hr->set_compaction_top(hr->bottom());
    _cp->initialize(hr, true);
  }
  // Add region to the compaction queue and prepare it.
  _cp->add(hr);
  prepare_for_compaction_work(_cp, hr);
}

void G1FullGCPrepareTask::prepare_serial_compaction() {
  GCTraceTime(Debug, gc, phases) debug("Phase 2: Prepare Serial Compaction", collector()->scope()->timer());
  // At this point we know that no regions were completely freed by
  // the parallel compaction. That means that the last region of
  // all compaction queues still have data in them. We try to compact
  // these regions in serial to avoid a premature OOM.
  for (uint i = 0; i < collector()->workers(); i++) {
    G1FullGCCompactionPoint* cp = collector()->compaction_point(i);
    if (cp->has_regions()) {
      collector()->serial_compaction_point()->add(cp->remove_last());
    }
  }

  // Update the forwarding information for the regions in the serial
  // compaction point.
  G1FullGCCompactionPoint* cp = collector()->serial_compaction_point();
  for (GrowableArrayIterator<HeapRegion*> it = cp->regions()->begin(); it != cp->regions()->end(); ++it) {
    HeapRegion* current = *it;
    if (!cp->is_initialized()) {
      // Initialize the compaction point. Nothing more is needed for the first heap region
      // since it is already prepared for compaction.
      cp->initialize(current, false);
    } else {
      assert(!current->is_humongous(), "Should be no humongous regions in compaction queue");
      G1RePrepareClosure re_prepare(cp, current);
      current->set_compaction_top(current->bottom());
      current->apply_to_marked_objects(collector()->mark_bitmap(), &re_prepare);
    }
  }
  cp->update();
}

bool G1FullGCPrepareTask::G1CalculatePointersClosure::freed_regions() {
  if (_regions_freed) {
    return true;
  }

  if (!_cp->has_regions()) {
    // No regions in queue, so no free ones either.
    return false;
  }

  if (_cp->current_region() != _cp->regions()->last()) {
    // The current region used for compaction is not the last in the
    // queue. That means there is at least one free region in the queue.
    return true;
  }

  // No free regions in the queue.
  return false;
}
