/*
 * Copyright (c) 2014, 2021, Red Hat, Inc. All rights reserved.
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

#include "compiler/oopMap.hpp"
#include "gc/shared/gcTraceTime.inline.hpp"
#include "gc/shared/preservedMarks.inline.hpp"
#include "gc/shared/tlab_globals.hpp"
#include "gc/shenandoah/heuristics/shenandoahHeuristics.hpp"
#include "gc/shenandoah/shenandoahConcurrentGC.hpp"
#include "gc/shenandoah/shenandoahCollectionSet.hpp"
#include "gc/shenandoah/shenandoahFreeSet.hpp"
#include "gc/shenandoah/shenandoahFullGC.hpp"
#include "gc/shenandoah/shenandoahPhaseTimings.hpp"
#include "gc/shenandoah/shenandoahMark.inline.hpp"
#include "gc/shenandoah/shenandoahMonitoringSupport.hpp"
#include "gc/shenandoah/shenandoahHeapRegionSet.hpp"
#include "gc/shenandoah/shenandoahHeap.inline.hpp"
#include "gc/shenandoah/shenandoahHeapRegion.inline.hpp"
#include "gc/shenandoah/shenandoahMarkingContext.inline.hpp"
#include "gc/shenandoah/shenandoahMetrics.hpp"
#include "gc/shenandoah/shenandoahOopClosures.inline.hpp"
#include "gc/shenandoah/shenandoahReferenceProcessor.hpp"
#include "gc/shenandoah/shenandoahRootProcessor.inline.hpp"
#include "gc/shenandoah/shenandoahSTWMark.hpp"
#include "gc/shenandoah/shenandoahUtils.hpp"
#include "gc/shenandoah/shenandoahVerifier.hpp"
#include "gc/shenandoah/shenandoahVMOperations.hpp"
#include "gc/shenandoah/shenandoahWorkerPolicy.hpp"
#include "memory/metaspaceUtils.hpp"
#include "memory/universe.hpp"
#include "oops/compressedOops.inline.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/orderAccess.hpp"
#include "runtime/thread.hpp"
#include "runtime/vmThread.hpp"
#include "utilities/copy.hpp"
#include "utilities/events.hpp"
#include "utilities/growableArray.hpp"
#include "gc/shared/workgroup.hpp"

ShenandoahFullGC::ShenandoahFullGC() :
  _gc_timer(ShenandoahHeap::heap()->gc_timer()),
  _preserved_marks(new PreservedMarksSet(true)) {}

bool ShenandoahFullGC::collect(GCCause::Cause cause) {
  vmop_entry_full(cause);
  // Always success
  return true;
}

void ShenandoahFullGC::vmop_entry_full(GCCause::Cause cause) {
  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  TraceCollectorStats tcs(heap->monitoring_support()->full_stw_collection_counters());
  ShenandoahTimingsTracker timing(ShenandoahPhaseTimings::full_gc_gross);

  heap->try_inject_alloc_failure();
  VM_ShenandoahFullGC op(cause, this);
  VMThread::execute(&op);
}

void ShenandoahFullGC::entry_full(GCCause::Cause cause) {
  static const char* msg = "Pause Full";
  ShenandoahPausePhase gc_phase(msg, ShenandoahPhaseTimings::full_gc, true /* log_heap_usage */);
  EventMark em("%s", msg);

  ShenandoahWorkerScope scope(ShenandoahHeap::heap()->workers(),
                              ShenandoahWorkerPolicy::calc_workers_for_fullgc(),
                              "full gc");

  op_full(cause);
}

void ShenandoahFullGC::op_full(GCCause::Cause cause) {
  ShenandoahMetricsSnapshot metrics;
  metrics.snap_before();

  // Perform full GC
  do_it(cause);

  metrics.snap_after();

  if (metrics.is_good_progress()) {
    ShenandoahHeap::heap()->notify_gc_progress();
  } else {
    // Nothing to do. Tell the allocation path that we have failed to make
    // progress, and it can finally fail.
    ShenandoahHeap::heap()->notify_gc_no_progress();
  }
}

void ShenandoahFullGC::do_it(GCCause::Cause gc_cause) {
  ShenandoahHeap* heap = ShenandoahHeap::heap();

  if (ShenandoahVerify) {
    heap->verifier()->verify_before_fullgc();
  }

  if (VerifyBeforeGC) {
    Universe::verify();
  }

  // Degenerated GC may carry concurrent root flags when upgrading to
  // full GC. We need to reset it before mutators resume.
  heap->set_concurrent_strong_root_in_progress(false);
  heap->set_concurrent_weak_root_in_progress(false);

  heap->set_full_gc_in_progress(true);

  assert(ShenandoahSafepoint::is_at_shenandoah_safepoint(), "must be at a safepoint");
  assert(Thread::current()->is_VM_thread(), "Do full GC only while world is stopped");

  {
    ShenandoahGCPhase phase(ShenandoahPhaseTimings::full_gc_heapdump_pre);
    heap->pre_full_gc_dump(_gc_timer);
  }

  {
    ShenandoahGCPhase prepare_phase(ShenandoahPhaseTimings::full_gc_prepare);
    // Full GC is supposed to recover from any GC state:

    // a0. Remember if we have forwarded objects
    bool has_forwarded_objects = heap->has_forwarded_objects();

    // a1. Cancel evacuation, if in progress
    if (heap->is_evacuation_in_progress()) {
      heap->set_evacuation_in_progress(false);
    }
    assert(!heap->is_evacuation_in_progress(), "sanity");

    // a2. Cancel update-refs, if in progress
    if (heap->is_update_refs_in_progress()) {
      heap->set_update_refs_in_progress(false);
    }
    assert(!heap->is_update_refs_in_progress(), "sanity");

    // b. Cancel concurrent mark, if in progress
    if (heap->is_concurrent_mark_in_progress()) {
      ShenandoahConcurrentGC::cancel();
      heap->set_concurrent_mark_in_progress(false);
    }
    assert(!heap->is_concurrent_mark_in_progress(), "sanity");

    // c. Update roots if this full GC is due to evac-oom, which may carry from-space pointers in roots.
    if (has_forwarded_objects) {
      update_roots(true /*full_gc*/);
    }

    // d. Reset the bitmaps for new marking
    heap->reset_mark_bitmap();
    assert(heap->marking_context()->is_bitmap_clear(), "sanity");
    assert(!heap->marking_context()->is_complete(), "sanity");

    // e. Abandon reference discovery and clear all discovered references.
    ShenandoahReferenceProcessor* rp = heap->ref_processor();
    rp->abandon_partial_discovery();

    // f. Sync pinned region status from the CP marks
    heap->sync_pinned_region_status();

    // The rest of prologue:
    _preserved_marks->init(heap->workers()->active_workers());

    assert(heap->has_forwarded_objects() == has_forwarded_objects, "This should not change");
  }

  if (UseTLAB) {
    heap->gclabs_retire(ResizeTLAB);
    heap->tlabs_retire(ResizeTLAB);
  }

  OrderAccess::fence();

  phase1_mark_heap();

  // Once marking is done, which may have fixed up forwarded objects, we can drop it.
  // Coming out of Full GC, we would not have any forwarded objects.
  // This also prevents resolves with fwdptr from kicking in while adjusting pointers in phase3.
  heap->set_has_forwarded_objects(false);

  heap->set_full_gc_move_in_progress(true);

  // Setup workers for the rest
  OrderAccess::fence();

  // Initialize worker slices
  ShenandoahHeapRegionSet** worker_slices = NEW_C_HEAP_ARRAY(ShenandoahHeapRegionSet*, heap->max_workers(), mtGC);
  for (uint i = 0; i < heap->max_workers(); i++) {
    worker_slices[i] = new ShenandoahHeapRegionSet();
  }

  {
    // The rest of code performs region moves, where region status is undefined
    // until all phases run together.
    ShenandoahHeapLocker lock(heap->lock());

    phase2_calculate_target_addresses(worker_slices);

    OrderAccess::fence();

    phase3_update_references();

    phase4_compact_objects(worker_slices);
  }

  {
    // Epilogue
    _preserved_marks->restore(heap->workers());
    _preserved_marks->reclaim();
  }

  // Resize metaspace
  MetaspaceGC::compute_new_size();

  // Free worker slices
  for (uint i = 0; i < heap->max_workers(); i++) {
    delete worker_slices[i];
  }
  FREE_C_HEAP_ARRAY(ShenandoahHeapRegionSet*, worker_slices);

  heap->set_full_gc_move_in_progress(false);
  heap->set_full_gc_in_progress(false);

  if (ShenandoahVerify) {
    heap->verifier()->verify_after_fullgc();
  }

  if (VerifyAfterGC) {
    Universe::verify();
  }

  {
    ShenandoahGCPhase phase(ShenandoahPhaseTimings::full_gc_heapdump_post);
    heap->post_full_gc_dump(_gc_timer);
  }
}

class ShenandoahPrepareForMarkClosure: public ShenandoahHeapRegionClosure {
private:
  ShenandoahMarkingContext* const _ctx;

public:
  ShenandoahPrepareForMarkClosure() : _ctx(ShenandoahHeap::heap()->marking_context()) {}

  void heap_region_do(ShenandoahHeapRegion *r) {
    _ctx->capture_top_at_mark_start(r);
    r->clear_live_data();
  }
};

void ShenandoahFullGC::phase1_mark_heap() {
  GCTraceTime(Info, gc, phases) time("Phase 1: Mark live objects", _gc_timer);
  ShenandoahGCPhase mark_phase(ShenandoahPhaseTimings::full_gc_mark);

  ShenandoahHeap* heap = ShenandoahHeap::heap();

  ShenandoahPrepareForMarkClosure cl;
  heap->heap_region_iterate(&cl);

  heap->set_unload_classes(heap->heuristics()->can_unload_classes());

  ShenandoahReferenceProcessor* rp = heap->ref_processor();
  // enable ("weak") refs discovery
  rp->set_soft_reference_policy(true); // forcefully purge all soft references

  ShenandoahSTWMark mark(true /*full_gc*/);
  mark.mark();
  heap->parallel_cleaning(true /* full_gc */);
}

class ShenandoahPrepareForCompactionObjectClosure : public ObjectClosure {
private:
  PreservedMarks*          const _preserved_marks;
  ShenandoahHeap*          const _heap;
  GrowableArray<ShenandoahHeapRegion*>& _empty_regions;
  int _empty_regions_pos;
  ShenandoahHeapRegion*          _to_region;
  ShenandoahHeapRegion*          _from_region;
  HeapWord* _compact_point;

public:
  ShenandoahPrepareForCompactionObjectClosure(PreservedMarks* preserved_marks,
                                              GrowableArray<ShenandoahHeapRegion*>& empty_regions,
                                              ShenandoahHeapRegion* to_region) :
    _preserved_marks(preserved_marks),
    _heap(ShenandoahHeap::heap()),
    _empty_regions(empty_regions),
    _empty_regions_pos(0),
    _to_region(to_region),
    _from_region(NULL),
    _compact_point(to_region->bottom()) {}

  void set_from_region(ShenandoahHeapRegion* from_region) {
    _from_region = from_region;
  }

  void finish_region() {
    assert(_to_region != NULL, "should not happen");
    _to_region->set_new_top(_compact_point);
  }

  bool is_compact_same_region() {
    return _from_region == _to_region;
  }

  int empty_regions_pos() {
    return _empty_regions_pos;
  }

  void do_object(oop p) {
    assert(_from_region != NULL, "must set before work");
    assert(_heap->complete_marking_context()->is_marked(p), "must be marked");
    assert(!_heap->complete_marking_context()->allocated_after_mark_start(p), "must be truly marked");

    size_t obj_size = p->size();
    if (_compact_point + obj_size > _to_region->end()) {
      finish_region();

      // Object doesn't fit. Pick next empty region and start compacting there.
      ShenandoahHeapRegion* new_to_region;
      if (_empty_regions_pos < _empty_regions.length()) {
        new_to_region = _empty_regions.at(_empty_regions_pos);
        _empty_regions_pos++;
      } else {
        // Out of empty region? Compact within the same region.
        new_to_region = _from_region;
      }

      assert(new_to_region != _to_region, "must not reuse same to-region");
      assert(new_to_region != NULL, "must not be NULL");
      _to_region = new_to_region;
      _compact_point = _to_region->bottom();
    }

    // Object fits into current region, record new location:
    assert(_compact_point + obj_size <= _to_region->end(), "must fit");
    shenandoah_assert_not_forwarded(NULL, p);
    _preserved_marks->push_if_necessary(p, p->mark());
    p->forward_to(cast_to_oop(_compact_point));
    _compact_point += obj_size;
  }
};

class ShenandoahPrepareForCompactionTask : public AbstractGangTask {
private:
  PreservedMarksSet*        const _preserved_marks;
  ShenandoahHeap*           const _heap;
  ShenandoahHeapRegionSet** const _worker_slices;

public:
  ShenandoahPrepareForCompactionTask(PreservedMarksSet *preserved_marks, ShenandoahHeapRegionSet **worker_slices) :
    AbstractGangTask("Shenandoah Prepare For Compaction"),
    _preserved_marks(preserved_marks),
    _heap(ShenandoahHeap::heap()), _worker_slices(worker_slices) {
  }

  static bool is_candidate_region(ShenandoahHeapRegion* r) {
    // Empty region: get it into the slice to defragment the slice itself.
    // We could have skipped this without violating correctness, but we really
    // want to compact all live regions to the start of the heap, which sometimes
    // means moving them into the fully empty regions.
    if (r->is_empty()) return true;

    // Can move the region, and this is not the humongous region. Humongous
    // moves are special cased here, because their moves are handled separately.
    return r->is_stw_move_allowed() && !r->is_humongous();
  }

  void work(uint worker_id) {
    ShenandoahParallelWorkerSession worker_session(worker_id);
    ShenandoahHeapRegionSet* slice = _worker_slices[worker_id];
    ShenandoahHeapRegionSetIterator it(slice);
    ShenandoahHeapRegion* from_region = it.next();
    // No work?
    if (from_region == NULL) {
       return;
    }

    // Sliding compaction. Walk all regions in the slice, and compact them.
    // Remember empty regions and reuse them as needed.
    ResourceMark rm;

    GrowableArray<ShenandoahHeapRegion*> empty_regions((int)_heap->num_regions());

    ShenandoahPrepareForCompactionObjectClosure cl(_preserved_marks->get(worker_id), empty_regions, from_region);

    while (from_region != NULL) {
      assert(is_candidate_region(from_region), "Sanity");

      cl.set_from_region(from_region);
      if (from_region->has_live()) {
        _heap->marked_object_iterate(from_region, &cl);
      }

      // Compacted the region to somewhere else? From-region is empty then.
      if (!cl.is_compact_same_region()) {
        empty_regions.append(from_region);
      }
      from_region = it.next();
    }
    cl.finish_region();

    // Mark all remaining regions as empty
    for (int pos = cl.empty_regions_pos(); pos < empty_regions.length(); ++pos) {
      ShenandoahHeapRegion* r = empty_regions.at(pos);
      r->set_new_top(r->bottom());
    }
  }
};

void ShenandoahFullGC::calculate_target_humongous_objects() {
  ShenandoahHeap* heap = ShenandoahHeap::heap();

  // Compute the new addresses for humongous objects. We need to do this after addresses
  // for regular objects are calculated, and we know what regions in heap suffix are
  // available for humongous moves.
  //
  // Scan the heap backwards, because we are compacting humongous regions towards the end.
  // Maintain the contiguous compaction window in [to_begin; to_end), so that we can slide
  // humongous start there.
  //
  // The complication is potential non-movable regions during the scan. If such region is
  // detected, then sliding restarts towards that non-movable region.

  size_t to_begin = heap->num_regions();
  size_t to_end = heap->num_regions();

  for (size_t c = heap->num_regions(); c > 0; c--) {
    ShenandoahHeapRegion *r = heap->get_region(c - 1);
    if (r->is_humongous_continuation() || (r->new_top() == r->bottom())) {
      // To-region candidate: record this, and continue scan
      to_begin = r->index();
      continue;
    }

    if (r->is_humongous_start() && r->is_stw_move_allowed()) {
      // From-region candidate: movable humongous region
      oop old_obj = cast_to_oop(r->bottom());
      size_t words_size = old_obj->size();
      size_t num_regions = ShenandoahHeapRegion::required_regions(words_size * HeapWordSize);

      size_t start = to_end - num_regions;

      if (start >= to_begin && start != r->index()) {
        // Fits into current window, and the move is non-trivial. Record the move then, and continue scan.
        _preserved_marks->get(0)->push_if_necessary(old_obj, old_obj->mark());
        old_obj->forward_to(cast_to_oop(heap->get_region(start)->bottom()));
        to_end = start;
        continue;
      }
    }

    // Failed to fit. Scan starting from current region.
    to_begin = r->index();
    to_end = r->index();
  }
}

class ShenandoahEnsureHeapActiveClosure: public ShenandoahHeapRegionClosure {
private:
  ShenandoahHeap* const _heap;

public:
  ShenandoahEnsureHeapActiveClosure() : _heap(ShenandoahHeap::heap()) {}
  void heap_region_do(ShenandoahHeapRegion* r) {
    if (r->is_trash()) {
      r->recycle();
    }
    if (r->is_cset()) {
      r->make_regular_bypass();
    }
    if (r->is_empty_uncommitted()) {
      r->make_committed_bypass();
    }
    assert (r->is_committed(), "only committed regions in heap now, see region " SIZE_FORMAT, r->index());

    // Record current region occupancy: this communicates empty regions are free
    // to the rest of Full GC code.
    r->set_new_top(r->top());
  }
};

class ShenandoahTrashImmediateGarbageClosure: public ShenandoahHeapRegionClosure {
private:
  ShenandoahHeap* const _heap;
  ShenandoahMarkingContext* const _ctx;

public:
  ShenandoahTrashImmediateGarbageClosure() :
    _heap(ShenandoahHeap::heap()),
    _ctx(ShenandoahHeap::heap()->complete_marking_context()) {}

  void heap_region_do(ShenandoahHeapRegion* r) {
    if (r->is_humongous_start()) {
      oop humongous_obj = cast_to_oop(r->bottom());
      if (!_ctx->is_marked(humongous_obj)) {
        assert(!r->has_live(),
               "Region " SIZE_FORMAT " is not marked, should not have live", r->index());
        _heap->trash_humongous_region_at(r);
      } else {
        assert(r->has_live(),
               "Region " SIZE_FORMAT " should have live", r->index());
      }
    } else if (r->is_humongous_continuation()) {
      // If we hit continuation, the non-live humongous starts should have been trashed already
      assert(r->humongous_start_region()->has_live(),
             "Region " SIZE_FORMAT " should have live", r->index());
    } else if (r->is_regular()) {
      if (!r->has_live()) {
        r->make_trash_immediate();
      }
    }
  }
};

void ShenandoahFullGC::distribute_slices(ShenandoahHeapRegionSet** worker_slices) {
  ShenandoahHeap* heap = ShenandoahHeap::heap();

  uint n_workers = heap->workers()->active_workers();
  size_t n_regions = heap->num_regions();

  // What we want to accomplish: have the dense prefix of data, while still balancing
  // out the parallel work.
  //
  // Assuming the amount of work is driven by the live data that needs moving, we can slice
  // the entire heap into equal-live-sized prefix slices, and compact into them. So, each
  // thread takes all regions in its prefix subset, and then it takes some regions from
  // the tail.
  //
  // Tail region selection becomes interesting.
  //
  // First, we want to distribute the regions fairly between the workers, and those regions
  // might have different amount of live data. So, until we sure no workers need live data,
  // we need to only take what the worker needs.
  //
  // Second, since we slide everything to the left in each slice, the most busy regions
  // would be the ones on the left. Which means we want to have all workers have their after-tail
  // regions as close to the left as possible.
  //
  // The easiest way to do this is to distribute after-tail regions in round-robin between
  // workers that still need live data.
  //
  // Consider parallel workers A, B, C, then the target slice layout would be:
  //
  //  AAAAAAAABBBBBBBBCCCCCCCC|ABCABCABCABCABCABCABCABABABABABABABABABABAAAAA
  //
  //  (.....dense-prefix.....) (.....................tail...................)
  //  [all regions fully live] [left-most regions are fuller that right-most]
  //

  // Compute how much live data is there. This would approximate the size of dense prefix
  // we target to create.
  size_t total_live = 0;
  for (size_t idx = 0; idx < n_regions; idx++) {
    ShenandoahHeapRegion *r = heap->get_region(idx);
    if (ShenandoahPrepareForCompactionTask::is_candidate_region(r)) {
      total_live += r->get_live_data_words();
    }
  }

  // Estimate the size for the dense prefix. Note that we specifically count only the
  // "full" regions, so there would be some non-full regions in the slice tail.
  size_t live_per_worker = total_live / n_workers;
  size_t prefix_regions_per_worker = live_per_worker / ShenandoahHeapRegion::region_size_words();
  size_t prefix_regions_total = prefix_regions_per_worker * n_workers;
  prefix_regions_total = MIN2(prefix_regions_total, n_regions);
  assert(prefix_regions_total <= n_regions, "Sanity");

  // There might be non-candidate regions in the prefix. To compute where the tail actually
  // ends up being, we need to account those as well.
  size_t prefix_end = prefix_regions_total;
  for (size_t idx = 0; idx < prefix_regions_total; idx++) {
    ShenandoahHeapRegion *r = heap->get_region(idx);
    if (!ShenandoahPrepareForCompactionTask::is_candidate_region(r)) {
      prefix_end++;
    }
  }
  prefix_end = MIN2(prefix_end, n_regions);
  assert(prefix_end <= n_regions, "Sanity");

  // Distribute prefix regions per worker: each thread definitely gets its own same-sized
  // subset of dense prefix.
  size_t prefix_idx = 0;

  size_t* live = NEW_C_HEAP_ARRAY(size_t, n_workers, mtGC);

  for (size_t wid = 0; wid < n_workers; wid++) {
    ShenandoahHeapRegionSet* slice = worker_slices[wid];

    live[wid] = 0;
    size_t regs = 0;

    // Add all prefix regions for this worker
    while (prefix_idx < prefix_end && regs < prefix_regions_per_worker) {
      ShenandoahHeapRegion *r = heap->get_region(prefix_idx);
      if (ShenandoahPrepareForCompactionTask::is_candidate_region(r)) {
        slice->add_region(r);
        live[wid] += r->get_live_data_words();
        regs++;
      }
      prefix_idx++;
    }
  }

  // Distribute the tail among workers in round-robin fashion.
  size_t wid = n_workers - 1;

  for (size_t tail_idx = prefix_end; tail_idx < n_regions; tail_idx++) {
    ShenandoahHeapRegion *r = heap->get_region(tail_idx);
    if (ShenandoahPrepareForCompactionTask::is_candidate_region(r)) {
      assert(wid < n_workers, "Sanity");

      size_t live_region = r->get_live_data_words();

      // Select next worker that still needs live data.
      size_t old_wid = wid;
      do {
        wid++;
        if (wid == n_workers) wid = 0;
      } while (live[wid] + live_region >= live_per_worker && old_wid != wid);

      if (old_wid == wid) {
        // Circled back to the same worker? This means liveness data was
        // miscalculated. Bump the live_per_worker limit so that
        // everyone gets a piece of the leftover work.
        live_per_worker += ShenandoahHeapRegion::region_size_words();
      }

      worker_slices[wid]->add_region(r);
      live[wid] += live_region;
    }
  }

  FREE_C_HEAP_ARRAY(size_t, live);

#ifdef ASSERT
  ResourceBitMap map(n_regions);
  for (size_t wid = 0; wid < n_workers; wid++) {
    ShenandoahHeapRegionSetIterator it(worker_slices[wid]);
    ShenandoahHeapRegion* r = it.next();
    while (r != NULL) {
      size_t idx = r->index();
      assert(ShenandoahPrepareForCompactionTask::is_candidate_region(r), "Sanity: " SIZE_FORMAT, idx);
      assert(!map.at(idx), "No region distributed twice: " SIZE_FORMAT, idx);
      map.at_put(idx, true);
      r = it.next();
    }
  }

  for (size_t rid = 0; rid < n_regions; rid++) {
    bool is_candidate = ShenandoahPrepareForCompactionTask::is_candidate_region(heap->get_region(rid));
    bool is_distributed = map.at(rid);
    assert(is_distributed || !is_candidate, "All candidates are distributed: " SIZE_FORMAT, rid);
  }
#endif
}

void ShenandoahFullGC::phase2_calculate_target_addresses(ShenandoahHeapRegionSet** worker_slices) {
  GCTraceTime(Info, gc, phases) time("Phase 2: Compute new object addresses", _gc_timer);
  ShenandoahGCPhase calculate_address_phase(ShenandoahPhaseTimings::full_gc_calculate_addresses);

  ShenandoahHeap* heap = ShenandoahHeap::heap();

  // About to figure out which regions can be compacted, make sure pinning status
  // had been updated in GC prologue.
  heap->assert_pinned_region_status();

  {
    // Trash the immediately collectible regions before computing addresses
    ShenandoahTrashImmediateGarbageClosure tigcl;
    heap->heap_region_iterate(&tigcl);

    // Make sure regions are in good state: committed, active, clean.
    // This is needed because we are potentially sliding the data through them.
    ShenandoahEnsureHeapActiveClosure ecl;
    heap->heap_region_iterate(&ecl);
  }

  // Compute the new addresses for regular objects
  {
    ShenandoahGCPhase phase(ShenandoahPhaseTimings::full_gc_calculate_addresses_regular);

    distribute_slices(worker_slices);

    ShenandoahPrepareForCompactionTask task(_preserved_marks, worker_slices);
    heap->workers()->run_task(&task);
  }

  // Compute the new addresses for humongous objects
  {
    ShenandoahGCPhase phase(ShenandoahPhaseTimings::full_gc_calculate_addresses_humong);
    calculate_target_humongous_objects();
  }
}

class ShenandoahAdjustPointersClosure : public MetadataVisitingOopIterateClosure {
private:
  ShenandoahHeap* const _heap;
  ShenandoahMarkingContext* const _ctx;

  template <class T>
  inline void do_oop_work(T* p) {
    T o = RawAccess<>::oop_load(p);
    if (!CompressedOops::is_null(o)) {
      oop obj = CompressedOops::decode_not_null(o);
      assert(_ctx->is_marked(obj), "must be marked");
      if (obj->is_forwarded()) {
        oop forw = obj->forwardee();
        RawAccess<IS_NOT_NULL>::oop_store(p, forw);
      }
    }
  }

public:
  ShenandoahAdjustPointersClosure() :
    _heap(ShenandoahHeap::heap()),
    _ctx(ShenandoahHeap::heap()->complete_marking_context()) {}

  void do_oop(oop* p)       { do_oop_work(p); }
  void do_oop(narrowOop* p) { do_oop_work(p); }
};

class ShenandoahAdjustPointersObjectClosure : public ObjectClosure {
private:
  ShenandoahHeap* const _heap;
  ShenandoahAdjustPointersClosure _cl;

public:
  ShenandoahAdjustPointersObjectClosure() :
    _heap(ShenandoahHeap::heap()) {
  }
  void do_object(oop p) {
    assert(_heap->complete_marking_context()->is_marked(p), "must be marked");
    p->oop_iterate(&_cl);
  }
};

class ShenandoahAdjustPointersTask : public AbstractGangTask {
private:
  ShenandoahHeap*          const _heap;
  ShenandoahRegionIterator       _regions;

public:
  ShenandoahAdjustPointersTask() :
    AbstractGangTask("Shenandoah Adjust Pointers"),
    _heap(ShenandoahHeap::heap()) {
  }

  void work(uint worker_id) {
    ShenandoahParallelWorkerSession worker_session(worker_id);
    ShenandoahAdjustPointersObjectClosure obj_cl;
    ShenandoahHeapRegion* r = _regions.next();
    while (r != NULL) {
      if (!r->is_humongous_continuation() && r->has_live()) {
        _heap->marked_object_iterate(r, &obj_cl);
      }
      r = _regions.next();
    }
  }
};

class ShenandoahAdjustRootPointersTask : public AbstractGangTask {
private:
  ShenandoahRootAdjuster* _rp;
  PreservedMarksSet* _preserved_marks;
public:
  ShenandoahAdjustRootPointersTask(ShenandoahRootAdjuster* rp, PreservedMarksSet* preserved_marks) :
    AbstractGangTask("Shenandoah Adjust Root Pointers"),
    _rp(rp),
    _preserved_marks(preserved_marks) {}

  void work(uint worker_id) {
    ShenandoahParallelWorkerSession worker_session(worker_id);
    ShenandoahAdjustPointersClosure cl;
    _rp->roots_do(worker_id, &cl);
    _preserved_marks->get(worker_id)->adjust_during_full_gc();
  }
};

void ShenandoahFullGC::phase3_update_references() {
  GCTraceTime(Info, gc, phases) time("Phase 3: Adjust pointers", _gc_timer);
  ShenandoahGCPhase adjust_pointer_phase(ShenandoahPhaseTimings::full_gc_adjust_pointers);

  ShenandoahHeap* heap = ShenandoahHeap::heap();

  WorkGang* workers = heap->workers();
  uint nworkers = workers->active_workers();
  {
#if COMPILER2_OR_JVMCI
    DerivedPointerTable::clear();
#endif
    ShenandoahRootAdjuster rp(nworkers, ShenandoahPhaseTimings::full_gc_adjust_roots);
    ShenandoahAdjustRootPointersTask task(&rp, _preserved_marks);
    workers->run_task(&task);
#if COMPILER2_OR_JVMCI
    DerivedPointerTable::update_pointers();
#endif
  }

  ShenandoahAdjustPointersTask adjust_pointers_task;
  workers->run_task(&adjust_pointers_task);
}

class ShenandoahCompactObjectsClosure : public ObjectClosure {
private:
  ShenandoahHeap* const _heap;
  uint            const _worker_id;

public:
  ShenandoahCompactObjectsClosure(uint worker_id) :
    _heap(ShenandoahHeap::heap()), _worker_id(worker_id) {}

  void do_object(oop p) {
    assert(_heap->complete_marking_context()->is_marked(p), "must be marked");
    size_t size = (size_t)p->size();
    if (p->is_forwarded()) {
      HeapWord* compact_from = cast_from_oop<HeapWord*>(p);
      HeapWord* compact_to = cast_from_oop<HeapWord*>(p->forwardee());
      Copy::aligned_conjoint_words(compact_from, compact_to, size);
      oop new_obj = cast_to_oop(compact_to);
      new_obj->init_mark();
    }
  }
};

class ShenandoahCompactObjectsTask : public AbstractGangTask {
private:
  ShenandoahHeap* const _heap;
  ShenandoahHeapRegionSet** const _worker_slices;

public:
  ShenandoahCompactObjectsTask(ShenandoahHeapRegionSet** worker_slices) :
    AbstractGangTask("Shenandoah Compact Objects"),
    _heap(ShenandoahHeap::heap()),
    _worker_slices(worker_slices) {
  }

  void work(uint worker_id) {
    ShenandoahParallelWorkerSession worker_session(worker_id);
    ShenandoahHeapRegionSetIterator slice(_worker_slices[worker_id]);

    ShenandoahCompactObjectsClosure cl(worker_id);
    ShenandoahHeapRegion* r = slice.next();
    while (r != NULL) {
      assert(!r->is_humongous(), "must not get humongous regions here");
      if (r->has_live()) {
        _heap->marked_object_iterate(r, &cl);
      }
      r->set_top(r->new_top());
      r = slice.next();
    }
  }
};

class ShenandoahPostCompactClosure : public ShenandoahHeapRegionClosure {
private:
  ShenandoahHeap* const _heap;
  size_t _live;

public:
  ShenandoahPostCompactClosure() : _heap(ShenandoahHeap::heap()), _live(0) {
    _heap->free_set()->clear();
  }

  void heap_region_do(ShenandoahHeapRegion* r) {
    assert (!r->is_cset(), "cset regions should have been demoted already");

    // Need to reset the complete-top-at-mark-start pointer here because
    // the complete marking bitmap is no longer valid. This ensures
    // size-based iteration in marked_object_iterate().
    // NOTE: See blurb at ShenandoahMCResetCompleteBitmapTask on why we need to skip
    // pinned regions.
    if (!r->is_pinned()) {
      _heap->complete_marking_context()->reset_top_at_mark_start(r);
    }

    size_t live = r->used();

    // Make empty regions that have been allocated into regular
    if (r->is_empty() && live > 0) {
      r->make_regular_bypass();
    }

    // Reclaim regular regions that became empty
    if (r->is_regular() && live == 0) {
      r->make_trash();
    }

    // Recycle all trash regions
    if (r->is_trash()) {
      live = 0;
      r->recycle();
    }

    r->set_live_data(live);
    r->reset_alloc_metadata();
    _live += live;
  }

  size_t get_live() {
    return _live;
  }
};

void ShenandoahFullGC::compact_humongous_objects() {
  // Compact humongous regions, based on their fwdptr objects.
  //
  // This code is serial, because doing the in-slice parallel sliding is tricky. In most cases,
  // humongous regions are already compacted, and do not require further moves, which alleviates
  // sliding costs. We may consider doing this in parallel in future.

  ShenandoahHeap* heap = ShenandoahHeap::heap();

  for (size_t c = heap->num_regions(); c > 0; c--) {
    ShenandoahHeapRegion* r = heap->get_region(c - 1);
    if (r->is_humongous_start()) {
      oop old_obj = cast_to_oop(r->bottom());
      if (!old_obj->is_forwarded()) {
        // No need to move the object, it stays at the same slot
        continue;
      }
      size_t words_size = old_obj->size();
      size_t num_regions = ShenandoahHeapRegion::required_regions(words_size * HeapWordSize);

      size_t old_start = r->index();
      size_t old_end   = old_start + num_regions - 1;
      size_t new_start = heap->heap_region_index_containing(old_obj->forwardee());
      size_t new_end   = new_start + num_regions - 1;
      assert(old_start != new_start, "must be real move");
      assert(r->is_stw_move_allowed(), "Region " SIZE_FORMAT " should be movable", r->index());

      Copy::aligned_conjoint_words(heap->get_region(old_start)->bottom(),
                                   heap->get_region(new_start)->bottom(),
                                   words_size);

      oop new_obj = cast_to_oop(heap->get_region(new_start)->bottom());
      new_obj->init_mark();

      {
        for (size_t c = old_start; c <= old_end; c++) {
          ShenandoahHeapRegion* r = heap->get_region(c);
          r->make_regular_bypass();
          r->set_top(r->bottom());
        }

        for (size_t c = new_start; c <= new_end; c++) {
          ShenandoahHeapRegion* r = heap->get_region(c);
          if (c == new_start) {
            r->make_humongous_start_bypass();
          } else {
            r->make_humongous_cont_bypass();
          }

          // Trailing region may be non-full, record the remainder there
          size_t remainder = words_size & ShenandoahHeapRegion::region_size_words_mask();
          if ((c == new_end) && (remainder != 0)) {
            r->set_top(r->bottom() + remainder);
          } else {
            r->set_top(r->end());
          }

          r->reset_alloc_metadata();
        }
      }
    }
  }
}

// This is slightly different to ShHeap::reset_next_mark_bitmap:
// we need to remain able to walk pinned regions.
// Since pinned region do not move and don't get compacted, we will get holes with
// unreachable objects in them (which may have pointers to unloaded Klasses and thus
// cannot be iterated over using oop->size(). The only way to safely iterate over those is using
// a valid marking bitmap and valid TAMS pointer. This class only resets marking
// bitmaps for un-pinned regions, and later we only reset TAMS for unpinned regions.
class ShenandoahMCResetCompleteBitmapTask : public AbstractGangTask {
private:
  ShenandoahRegionIterator _regions;

public:
  ShenandoahMCResetCompleteBitmapTask() :
    AbstractGangTask("Shenandoah Reset Bitmap") {
  }

  void work(uint worker_id) {
    ShenandoahParallelWorkerSession worker_session(worker_id);
    ShenandoahHeapRegion* region = _regions.next();
    ShenandoahHeap* heap = ShenandoahHeap::heap();
    ShenandoahMarkingContext* const ctx = heap->complete_marking_context();
    while (region != NULL) {
      if (heap->is_bitmap_slice_committed(region) && !region->is_pinned() && region->has_live()) {
        ctx->clear_bitmap(region);
      }
      region = _regions.next();
    }
  }
};

void ShenandoahFullGC::phase4_compact_objects(ShenandoahHeapRegionSet** worker_slices) {
  GCTraceTime(Info, gc, phases) time("Phase 4: Move objects", _gc_timer);
  ShenandoahGCPhase compaction_phase(ShenandoahPhaseTimings::full_gc_copy_objects);

  ShenandoahHeap* heap = ShenandoahHeap::heap();

  // Compact regular objects first
  {
    ShenandoahGCPhase phase(ShenandoahPhaseTimings::full_gc_copy_objects_regular);
    ShenandoahCompactObjectsTask compact_task(worker_slices);
    heap->workers()->run_task(&compact_task);
  }

  // Compact humongous objects after regular object moves
  {
    ShenandoahGCPhase phase(ShenandoahPhaseTimings::full_gc_copy_objects_humong);
    compact_humongous_objects();
  }

  // Reset complete bitmap. We're about to reset the complete-top-at-mark-start pointer
  // and must ensure the bitmap is in sync.
  {
    ShenandoahGCPhase phase(ShenandoahPhaseTimings::full_gc_copy_objects_reset_complete);
    ShenandoahMCResetCompleteBitmapTask task;
    heap->workers()->run_task(&task);
  }

  // Bring regions in proper states after the collection, and set heap properties.
  {
    ShenandoahGCPhase phase(ShenandoahPhaseTimings::full_gc_copy_objects_rebuild);

    ShenandoahPostCompactClosure post_compact;
    heap->heap_region_iterate(&post_compact);
    heap->set_used(post_compact.get_live());

    heap->collection_set()->clear();
    heap->free_set()->rebuild();
  }

  heap->clear_cancelled_gc();
}
