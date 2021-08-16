/*
 * Copyright (c) 2021, Red Hat, Inc. All rights reserved.
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

#include "gc/shared/collectorCounters.hpp"
#include "gc/shenandoah/shenandoahCollectorPolicy.hpp"
#include "gc/shenandoah/shenandoahConcurrentMark.hpp"
#include "gc/shenandoah/shenandoahDegeneratedGC.hpp"
#include "gc/shenandoah/shenandoahFullGC.hpp"
#include "gc/shenandoah/shenandoahHeap.inline.hpp"
#include "gc/shenandoah/shenandoahMetrics.hpp"
#include "gc/shenandoah/shenandoahMonitoringSupport.hpp"
#include "gc/shenandoah/shenandoahOopClosures.inline.hpp"
#include "gc/shenandoah/shenandoahRootProcessor.inline.hpp"
#include "gc/shenandoah/shenandoahSTWMark.hpp"
#include "gc/shenandoah/shenandoahUtils.hpp"
#include "gc/shenandoah/shenandoahVerifier.hpp"
#include "gc/shenandoah/shenandoahWorkerPolicy.hpp"
#include "gc/shenandoah/shenandoahVMOperations.hpp"
#include "runtime/vmThread.hpp"
#include "utilities/events.hpp"

ShenandoahDegenGC::ShenandoahDegenGC(ShenandoahDegenPoint degen_point) :
  ShenandoahGC(),
  _degen_point(degen_point) {
}

bool ShenandoahDegenGC::collect(GCCause::Cause cause) {
  vmop_degenerated();
  return true;
}

void ShenandoahDegenGC::vmop_degenerated() {
  TraceCollectorStats tcs(ShenandoahHeap::heap()->monitoring_support()->full_stw_collection_counters());
  ShenandoahTimingsTracker timing(ShenandoahPhaseTimings::degen_gc_gross);
  VM_ShenandoahDegeneratedGC degenerated_gc(this);
  VMThread::execute(&degenerated_gc);
}

void ShenandoahDegenGC::entry_degenerated() {
  const char* msg = degen_event_message(_degen_point);
  ShenandoahPausePhase gc_phase(msg, ShenandoahPhaseTimings::degen_gc, true /* log_heap_usage */);
  EventMark em("%s", msg);
  ShenandoahHeap* const heap = ShenandoahHeap::heap();

  ShenandoahWorkerScope scope(heap->workers(),
                              ShenandoahWorkerPolicy::calc_workers_for_stw_degenerated(),
                              "stw degenerated gc");

  heap->set_degenerated_gc_in_progress(true);
  op_degenerated();
  heap->set_degenerated_gc_in_progress(false);
}

void ShenandoahDegenGC::op_degenerated() {
  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  // Degenerated GC is STW, but it can also fail. Current mechanics communicates
  // GC failure via cancelled_concgc() flag. So, if we detect the failure after
  // some phase, we have to upgrade the Degenerate GC to Full GC.
  heap->clear_cancelled_gc();

  ShenandoahMetricsSnapshot metrics;
  metrics.snap_before();

  switch (_degen_point) {
    // The cases below form the Duff's-like device: it describes the actual GC cycle,
    // but enters it at different points, depending on which concurrent phase had
    // degenerated.

    case _degenerated_outside_cycle:
      // We have degenerated from outside the cycle, which means something is bad with
      // the heap, most probably heavy humongous fragmentation, or we are very low on free
      // space. It makes little sense to wait for Full GC to reclaim as much as it can, when
      // we can do the most aggressive degen cycle, which includes processing references and
      // class unloading, unless those features are explicitly disabled.
      //

      // Degenerated from concurrent root mark, reset the flag for STW mark
      if (heap->is_concurrent_mark_in_progress()) {
        ShenandoahConcurrentMark::cancel();
        heap->set_concurrent_mark_in_progress(false);
      }

      // Note that we can only do this for "outside-cycle" degens, otherwise we would risk
      // changing the cycle parameters mid-cycle during concurrent -> degenerated handover.
      heap->set_unload_classes(heap->heuristics()->can_unload_classes());

      op_reset();

      // STW mark
      op_mark();

    case _degenerated_mark:
      // No fallthrough. Continue mark, handed over from concurrent mark
      if (_degen_point == ShenandoahDegenPoint::_degenerated_mark) {
        op_finish_mark();
      }
      assert(!heap->cancelled_gc(), "STW mark can not OOM");

      /* Degen select Collection Set. etc. */
      op_prepare_evacuation();

      op_cleanup_early();

    case _degenerated_evac:
      // If heuristics thinks we should do the cycle, this flag would be set,
      // and we can do evacuation. Otherwise, it would be the shortcut cycle.
      if (heap->is_evacuation_in_progress()) {

        // Degeneration under oom-evac protocol might have left some objects in
        // collection set un-evacuated. Restart evacuation from the beginning to
        // capture all objects. For all the objects that are already evacuated,
        // it would be a simple check, which is supposed to be fast. This is also
        // safe to do even without degeneration, as CSet iterator is at beginning
        // in preparation for evacuation anyway.
        //
        // Before doing that, we need to make sure we never had any cset-pinned
        // regions. This may happen if allocation failure happened when evacuating
        // the about-to-be-pinned object, oom-evac protocol left the object in
        // the collection set, and then the pin reached the cset region. If we continue
        // the cycle here, we would trash the cset and alive objects in it. To avoid
        // it, we fail degeneration right away and slide into Full GC to recover.

        {
          heap->sync_pinned_region_status();
          heap->collection_set()->clear_current_index();

          ShenandoahHeapRegion* r;
          while ((r = heap->collection_set()->next()) != NULL) {
            if (r->is_pinned()) {
              heap->cancel_gc(GCCause::_shenandoah_upgrade_to_full_gc);
              op_degenerated_fail();
              return;
            }
          }

          heap->collection_set()->clear_current_index();
        }
        op_evacuate();
        if (heap->cancelled_gc()) {
          op_degenerated_fail();
          return;
        }
      }

      // If heuristics thinks we should do the cycle, this flag would be set,
      // and we need to do update-refs. Otherwise, it would be the shortcut cycle.
      if (heap->has_forwarded_objects()) {
        op_init_updaterefs();
        assert(!heap->cancelled_gc(), "STW reference update can not OOM");
      }

    case _degenerated_updaterefs:
      if (heap->has_forwarded_objects()) {
        op_updaterefs();
        op_update_roots();
        assert(!heap->cancelled_gc(), "STW reference update can not OOM");
      }

      if (ClassUnloading) {
         // Disarm nmethods that armed in concurrent cycle.
         // In above case, update roots should disarm them
         ShenandoahCodeRoots::disarm_nmethods();
      }

      op_cleanup_complete();
      break;
    default:
      ShouldNotReachHere();
  }

  if (ShenandoahVerify) {
    heap->verifier()->verify_after_degenerated();
  }

  if (VerifyAfterGC) {
    Universe::verify();
  }

  metrics.snap_after();

  // Check for futility and fail. There is no reason to do several back-to-back Degenerated cycles,
  // because that probably means the heap is overloaded and/or fragmented.
  if (!metrics.is_good_progress()) {
    heap->notify_gc_no_progress();
    heap->cancel_gc(GCCause::_shenandoah_upgrade_to_full_gc);
    op_degenerated_futile();
  } else {
    heap->notify_gc_progress();
  }
}

void ShenandoahDegenGC::op_reset() {
  ShenandoahHeap::heap()->prepare_gc();
}

void ShenandoahDegenGC::op_mark() {
  assert(!ShenandoahHeap::heap()->is_concurrent_mark_in_progress(), "Should be reset");
  ShenandoahGCPhase phase(ShenandoahPhaseTimings::degen_gc_stw_mark);
  ShenandoahSTWMark mark(false /*full gc*/);
  mark.clear();
  mark.mark();
}

void ShenandoahDegenGC::op_finish_mark() {
  ShenandoahConcurrentMark mark;
  mark.finish_mark();
}

void ShenandoahDegenGC::op_prepare_evacuation() {
  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  if (ShenandoahVerify) {
    heap->verifier()->verify_roots_no_forwarded();
  }

  // STW cleanup weak roots and unload classes
  heap->parallel_cleaning(false /*full gc*/);
  // Prepare regions and collection set
  heap->prepare_regions_and_collection_set(false /*concurrent*/);

  // Retire the TLABs, which will force threads to reacquire their TLABs after the pause.
  // This is needed for two reasons. Strong one: new allocations would be with new freeset,
  // which would be outside the collection set, so no cset writes would happen there.
  // Weaker one: new allocations would happen past update watermark, and so less work would
  // be needed for reference updates (would update the large filler instead).
  if (UseTLAB) {
    ShenandoahGCPhase phase(ShenandoahPhaseTimings::degen_gc_final_manage_labs);
    heap->tlabs_retire(false);
  }

  if (!heap->collection_set()->is_empty()) {
    heap->set_evacuation_in_progress(true);
    heap->set_has_forwarded_objects(true);

    if(ShenandoahVerify) {
      heap->verifier()->verify_during_evacuation();
    }
  } else {
    if (ShenandoahVerify) {
      heap->verifier()->verify_after_concmark();
    }

    if (VerifyAfterGC) {
      Universe::verify();
    }
  }
}

void ShenandoahDegenGC::op_cleanup_early() {
  ShenandoahHeap::heap()->recycle_trash();
}

void ShenandoahDegenGC::op_evacuate() {
  ShenandoahGCPhase phase(ShenandoahPhaseTimings::degen_gc_stw_evac);
  ShenandoahHeap::heap()->evacuate_collection_set(false /* concurrent*/);
}

void ShenandoahDegenGC::op_init_updaterefs() {
  // Evacuation has completed
  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  heap->set_evacuation_in_progress(false);
  heap->set_concurrent_weak_root_in_progress(false);
  heap->set_concurrent_strong_root_in_progress(false);

  heap->prepare_update_heap_references(false /*concurrent*/);
  heap->set_update_refs_in_progress(true);
}

void ShenandoahDegenGC::op_updaterefs() {
  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  ShenandoahGCPhase phase(ShenandoahPhaseTimings::degen_gc_updaterefs);
  // Handed over from concurrent update references phase
  heap->update_heap_references(false /*concurrent*/);

  heap->set_update_refs_in_progress(false);
  heap->set_has_forwarded_objects(false);
}

void ShenandoahDegenGC::op_update_roots() {
  ShenandoahHeap* const heap = ShenandoahHeap::heap();

  update_roots(false /*full_gc*/);

  heap->update_heap_region_states(false /*concurrent*/);

  if (ShenandoahVerify) {
    heap->verifier()->verify_after_updaterefs();
  }

  if (VerifyAfterGC) {
    Universe::verify();
  }

  heap->rebuild_free_set(false /*concurrent*/);
}

void ShenandoahDegenGC::op_cleanup_complete() {
  ShenandoahGCPhase phase(ShenandoahPhaseTimings::degen_gc_cleanup_complete);
  ShenandoahHeap::heap()->recycle_trash();
}

void ShenandoahDegenGC::op_degenerated_fail() {
  log_info(gc)("Cannot finish degeneration, upgrading to Full GC");
  ShenandoahHeap::heap()->shenandoah_policy()->record_degenerated_upgrade_to_full();

  ShenandoahFullGC full_gc;
  full_gc.op_full(GCCause::_shenandoah_upgrade_to_full_gc);
}

void ShenandoahDegenGC::op_degenerated_futile() {
  ShenandoahHeap::heap()->shenandoah_policy()->record_degenerated_upgrade_to_full();
  ShenandoahFullGC full_gc;
  full_gc.op_full(GCCause::_shenandoah_upgrade_to_full_gc);
}

const char* ShenandoahDegenGC::degen_event_message(ShenandoahDegenPoint point) const {
  switch (point) {
    case _degenerated_unset:
      return "Pause Degenerated GC (<UNSET>)";
    case _degenerated_outside_cycle:
      return "Pause Degenerated GC (Outside of Cycle)";
    case _degenerated_mark:
      return "Pause Degenerated GC (Mark)";
    case _degenerated_evac:
      return "Pause Degenerated GC (Evacuation)";
    case _degenerated_updaterefs:
      return "Pause Degenerated GC (Update Refs)";
    default:
      ShouldNotReachHere();
      return "ERROR";
  }
}
