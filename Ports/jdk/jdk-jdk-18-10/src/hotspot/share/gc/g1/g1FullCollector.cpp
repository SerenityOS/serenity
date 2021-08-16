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
#include "classfile/systemDictionary.hpp"
#include "code/codeCache.hpp"
#include "compiler/oopMap.hpp"
#include "gc/g1/g1CollectedHeap.hpp"
#include "gc/g1/g1FullCollector.inline.hpp"
#include "gc/g1/g1FullGCAdjustTask.hpp"
#include "gc/g1/g1FullGCCompactTask.hpp"
#include "gc/g1/g1FullGCMarker.inline.hpp"
#include "gc/g1/g1FullGCMarkTask.hpp"
#include "gc/g1/g1FullGCPrepareTask.hpp"
#include "gc/g1/g1FullGCScope.hpp"
#include "gc/g1/g1OopClosures.hpp"
#include "gc/g1/g1Policy.hpp"
#include "gc/g1/g1RegionMarkStatsCache.inline.hpp"
#include "gc/shared/gcTraceTime.inline.hpp"
#include "gc/shared/preservedMarks.hpp"
#include "gc/shared/referenceProcessor.hpp"
#include "gc/shared/verifyOption.hpp"
#include "gc/shared/weakProcessor.inline.hpp"
#include "gc/shared/workerPolicy.hpp"
#include "logging/log.hpp"
#include "runtime/handles.inline.hpp"
#include "utilities/debug.hpp"

static void clear_and_activate_derived_pointers() {
#if COMPILER2_OR_JVMCI
  DerivedPointerTable::clear();
#endif
}

static void deactivate_derived_pointers() {
#if COMPILER2_OR_JVMCI
  DerivedPointerTable::set_active(false);
#endif
}

static void update_derived_pointers() {
#if COMPILER2_OR_JVMCI
  DerivedPointerTable::update_pointers();
#endif
}

G1CMBitMap* G1FullCollector::mark_bitmap() {
  return _heap->concurrent_mark()->next_mark_bitmap();
}

ReferenceProcessor* G1FullCollector::reference_processor() {
  return _heap->ref_processor_stw();
}

uint G1FullCollector::calc_active_workers() {
  G1CollectedHeap* heap = G1CollectedHeap::heap();
  uint max_worker_count = heap->workers()->total_workers();
  // Only calculate number of workers if UseDynamicNumberOfGCThreads
  // is enabled, otherwise use max.
  if (!UseDynamicNumberOfGCThreads) {
    return max_worker_count;
  }

  // Consider G1HeapWastePercent to decide max number of workers. Each worker
  // will in average cause half a region waste.
  uint max_wasted_regions_allowed = ((heap->num_regions() * G1HeapWastePercent) / 100);
  uint waste_worker_count = MAX2((max_wasted_regions_allowed * 2) , 1u);
  uint heap_waste_worker_limit = MIN2(waste_worker_count, max_worker_count);

  // Also consider HeapSizePerGCThread by calling WorkerPolicy to calculate
  // the number of workers.
  uint current_active_workers = heap->workers()->active_workers();
  uint active_worker_limit = WorkerPolicy::calc_active_workers(max_worker_count, current_active_workers, 0);

  // Finally consider the amount of used regions.
  uint used_worker_limit = heap->num_used_regions();
  assert(used_worker_limit > 0, "Should never have zero used regions.");

  // Update active workers to the lower of the limits.
  uint worker_count = MIN3(heap_waste_worker_limit, active_worker_limit, used_worker_limit);
  log_debug(gc, task)("Requesting %u active workers for full compaction (waste limited workers: %u, "
                      "adaptive workers: %u, used limited workers: %u)",
                      worker_count, heap_waste_worker_limit, active_worker_limit, used_worker_limit);
  worker_count = heap->workers()->update_active_workers(worker_count);
  log_info(gc, task)("Using %u workers of %u for full compaction", worker_count, max_worker_count);

  return worker_count;
}

G1FullCollector::G1FullCollector(G1CollectedHeap* heap,
                                 bool explicit_gc,
                                 bool clear_soft_refs,
                                 bool do_maximum_compaction) :
    _heap(heap),
    _scope(heap->monitoring_support(), explicit_gc, clear_soft_refs, do_maximum_compaction),
    _num_workers(calc_active_workers()),
    _oop_queue_set(_num_workers),
    _array_queue_set(_num_workers),
    _preserved_marks_set(true),
    _serial_compaction_point(),
    _is_alive(this, heap->concurrent_mark()->next_mark_bitmap()),
    _is_alive_mutator(heap->ref_processor_stw(), &_is_alive),
    _always_subject_to_discovery(),
    _is_subject_mutator(heap->ref_processor_stw(), &_always_subject_to_discovery),
    _region_attr_table() {
  assert(SafepointSynchronize::is_at_safepoint(), "must be at a safepoint");

  _preserved_marks_set.init(_num_workers);
  _markers = NEW_C_HEAP_ARRAY(G1FullGCMarker*, _num_workers, mtGC);
  _compaction_points = NEW_C_HEAP_ARRAY(G1FullGCCompactionPoint*, _num_workers, mtGC);

  _live_stats = NEW_C_HEAP_ARRAY(G1RegionMarkStats, _heap->max_regions(), mtGC);
  for (uint j = 0; j < heap->max_regions(); j++) {
    _live_stats[j].clear();
  }

  for (uint i = 0; i < _num_workers; i++) {
    _markers[i] = new G1FullGCMarker(this, i, _preserved_marks_set.get(i), _live_stats);
    _compaction_points[i] = new G1FullGCCompactionPoint();
    _oop_queue_set.register_queue(i, marker(i)->oop_stack());
    _array_queue_set.register_queue(i, marker(i)->objarray_stack());
  }
  _region_attr_table.initialize(heap->reserved(), HeapRegion::GrainBytes);
}

G1FullCollector::~G1FullCollector() {
  for (uint i = 0; i < _num_workers; i++) {
    delete _markers[i];
    delete _compaction_points[i];
  }
  FREE_C_HEAP_ARRAY(G1FullGCMarker*, _markers);
  FREE_C_HEAP_ARRAY(G1FullGCCompactionPoint*, _compaction_points);
  FREE_C_HEAP_ARRAY(G1RegionMarkStats, _live_stats);
}

class PrepareRegionsClosure : public HeapRegionClosure {
  G1FullCollector* _collector;

public:
  PrepareRegionsClosure(G1FullCollector* collector) : _collector(collector) { }

  bool do_heap_region(HeapRegion* hr) {
    G1CollectedHeap::heap()->prepare_region_for_full_compaction(hr);
    _collector->before_marking_update_attribute_table(hr);
    return false;
  }
};

void G1FullCollector::prepare_collection() {
  _heap->policy()->record_full_collection_start();

  _heap->abort_concurrent_cycle();
  _heap->verify_before_full_collection(scope()->is_explicit_gc());

  _heap->gc_prologue(true);
  _heap->retire_tlabs();
  _heap->prepare_heap_for_full_collection();

  PrepareRegionsClosure cl(this);
  _heap->heap_region_iterate(&cl);

  reference_processor()->start_discovery(scope()->should_clear_soft_refs());

  // Clear and activate derived pointer collection.
  clear_and_activate_derived_pointers();
}

void G1FullCollector::collect() {
  phase1_mark_live_objects();
  verify_after_marking();

  // Don't add any more derived pointers during later phases
  deactivate_derived_pointers();

  phase2_prepare_compaction();

  phase3_adjust_pointers();

  phase4_do_compaction();
}

void G1FullCollector::complete_collection() {
  // Restore all marks.
  restore_marks();

  // When the pointers have been adjusted and moved, we can
  // update the derived pointer table.
  update_derived_pointers();

  _heap->concurrent_mark()->swap_mark_bitmaps();
  // Prepare the bitmap for the next (potentially concurrent) marking.
  _heap->concurrent_mark()->clear_next_bitmap(_heap->workers());

  _heap->prepare_heap_for_mutators();

  _heap->resize_all_tlabs();

  _heap->policy()->record_full_collection_end();
  _heap->gc_epilogue(true);

  _heap->verify_after_full_collection();
}

void G1FullCollector::before_marking_update_attribute_table(HeapRegion* hr) {
  if (hr->is_free()) {
    // Set as Invalid by default.
    _region_attr_table.verify_is_invalid(hr->hrm_index());
  } else if (hr->is_closed_archive()) {
    _region_attr_table.set_skip_marking(hr->hrm_index());
  } else if (hr->is_pinned()) {
    _region_attr_table.set_skip_compacting(hr->hrm_index());
  } else {
    // Everything else should be compacted.
    _region_attr_table.set_compacting(hr->hrm_index());
  }
}

class G1FullGCRefProcProxyTask : public RefProcProxyTask {
  G1FullCollector& _collector;

public:
  G1FullGCRefProcProxyTask(G1FullCollector &collector, uint max_workers)
    : RefProcProxyTask("G1FullGCRefProcProxyTask", max_workers),
      _collector(collector) {}

  void work(uint worker_id) override {
    assert(worker_id < _max_workers, "sanity");
    G1IsAliveClosure is_alive(&_collector);
    uint index = (_tm == RefProcThreadModel::Single) ? 0 : worker_id;
    G1FullKeepAliveClosure keep_alive(_collector.marker(index));
    G1FollowStackClosure* complete_gc = _collector.marker(index)->stack_closure();
    _rp_task->rp_work(worker_id, &is_alive, &keep_alive, complete_gc);
  }
};

void G1FullCollector::phase1_mark_live_objects() {
  // Recursively traverse all live objects and mark them.
  GCTraceTime(Info, gc, phases) info("Phase 1: Mark live objects", scope()->timer());

  {
    // Do the actual marking.
    G1FullGCMarkTask marking_task(this);
    run_task(&marking_task);
  }

  {
    uint old_active_mt_degree = reference_processor()->num_queues();
    reference_processor()->set_active_mt_degree(workers());
    GCTraceTime(Debug, gc, phases) debug("Phase 1: Reference Processing", scope()->timer());
    // Process reference objects found during marking.
    ReferenceProcessorPhaseTimes pt(scope()->timer(), reference_processor()->max_num_queues());
    G1FullGCRefProcProxyTask task(*this, reference_processor()->max_num_queues());
    const ReferenceProcessorStats& stats = reference_processor()->process_discovered_references(task, pt);
    scope()->tracer()->report_gc_reference_stats(stats);
    pt.print_all_references();
    assert(marker(0)->oop_stack()->is_empty(), "Should be no oops on the stack");

    reference_processor()->set_active_mt_degree(old_active_mt_degree);
  }

  // Weak oops cleanup.
  {
    GCTraceTime(Debug, gc, phases) debug("Phase 1: Weak Processing", scope()->timer());
    WeakProcessor::weak_oops_do(_heap->workers(), &_is_alive, &do_nothing_cl, 1);
  }

  // Class unloading and cleanup.
  if (ClassUnloading) {
    GCTraceTime(Debug, gc, phases) debug("Phase 1: Class Unloading and Cleanup", scope()->timer());
    // Unload classes and purge the SystemDictionary.
    bool purged_class = SystemDictionary::do_unloading(scope()->timer());
    _heap->complete_cleaning(&_is_alive, purged_class);
  }

  scope()->tracer()->report_object_count_after_gc(&_is_alive);
}

void G1FullCollector::phase2_prepare_compaction() {
  GCTraceTime(Info, gc, phases) info("Phase 2: Prepare for compaction", scope()->timer());
  G1FullGCPrepareTask task(this);
  run_task(&task);

  // To avoid OOM when there is memory left.
  if (!task.has_freed_regions()) {
    task.prepare_serial_compaction();
  }
}

void G1FullCollector::phase3_adjust_pointers() {
  // Adjust the pointers to reflect the new locations
  GCTraceTime(Info, gc, phases) info("Phase 3: Adjust pointers", scope()->timer());

  G1FullGCAdjustTask task(this);
  run_task(&task);
}

void G1FullCollector::phase4_do_compaction() {
  // Compact the heap using the compaction queues created in phase 2.
  GCTraceTime(Info, gc, phases) info("Phase 4: Compact heap", scope()->timer());
  G1FullGCCompactTask task(this);
  run_task(&task);

  // Serial compact to avoid OOM when very few free regions.
  if (serial_compaction_point()->has_regions()) {
    task.serial_compaction();
  }
}

void G1FullCollector::restore_marks() {
  _preserved_marks_set.restore(_heap->workers());
  _preserved_marks_set.reclaim();
}

void G1FullCollector::run_task(AbstractGangTask* task) {
  _heap->workers()->run_task(task, _num_workers);
}

void G1FullCollector::verify_after_marking() {
  if (!VerifyDuringGC || !_heap->verifier()->should_verify(G1HeapVerifier::G1VerifyFull)) {
    // Only do verification if VerifyDuringGC and G1VerifyFull is set.
    return;
  }

#if COMPILER2_OR_JVMCI
  DerivedPointerTableDeactivate dpt_deact;
#endif
  _heap->prepare_for_verify();
  // Note: we can verify only the heap here. When an object is
  // marked, the previous value of the mark word (including
  // identity hash values, ages, etc) is preserved, and the mark
  // word is set to markWord::marked_value - effectively removing
  // any hash values from the mark word. These hash values are
  // used when verifying the dictionaries and so removing them
  // from the mark word can make verification of the dictionaries
  // fail. At the end of the GC, the original mark word values
  // (including hash values) are restored to the appropriate
  // objects.
  GCTraceTime(Info, gc, verify) tm("Verifying During GC (full)");
  _heap->verify(VerifyOption_G1UseFullMarking);
}
