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

#include "compiler/oopMap.hpp"
#include "gc/shared/workgroup.hpp"
#include "gc/shenandoah/shenandoahClosures.inline.hpp"
#include "gc/shenandoah/shenandoahGC.hpp"
#include "gc/shenandoah/shenandoahHeap.hpp"
#include "gc/shenandoah/shenandoahPhaseTimings.hpp"
#include "gc/shenandoah/shenandoahRootProcessor.inline.hpp"
#include "gc/shenandoah/shenandoahUtils.hpp"

const char* ShenandoahGC::degen_point_to_string(ShenandoahDegenPoint point) {
  switch(point) {
    case _degenerated_unset:
      return "<UNSET>";
    case _degenerated_outside_cycle:
      return "Outside of Cycle";
    case _degenerated_mark:
      return "Mark";
    case _degenerated_evac:
      return "Evacuation";
    case _degenerated_updaterefs:
      return "Update References";
    default:
      ShouldNotReachHere();
      return "ERROR";
   }
}

class ShenandoahUpdateRootsTask : public AbstractGangTask {
private:
  ShenandoahRootUpdater*  _root_updater;
  bool                    _check_alive;
public:
  ShenandoahUpdateRootsTask(ShenandoahRootUpdater* root_updater, bool check_alive) :
    AbstractGangTask("Shenandoah Update Roots"),
    _root_updater(root_updater),
    _check_alive(check_alive){
  }

  void work(uint worker_id) {
    assert(ShenandoahSafepoint::is_at_shenandoah_safepoint(), "Must be at a safepoint");
    ShenandoahParallelWorkerSession worker_session(worker_id);

    ShenandoahHeap* heap = ShenandoahHeap::heap();
    ShenandoahUpdateRefsClosure cl;
    if (_check_alive) {
      ShenandoahForwardedIsAliveClosure is_alive;
      _root_updater->roots_do<ShenandoahForwardedIsAliveClosure, ShenandoahUpdateRefsClosure>(worker_id, &is_alive, &cl);
    } else {
      AlwaysTrueClosure always_true;;
      _root_updater->roots_do<AlwaysTrueClosure, ShenandoahUpdateRefsClosure>(worker_id, &always_true, &cl);
    }
  }
};

void ShenandoahGC::update_roots(bool full_gc) {
  assert(ShenandoahSafepoint::is_at_shenandoah_safepoint(), "Must be at a safepoint");
  assert(ShenandoahHeap::heap()->is_full_gc_in_progress() ||
         ShenandoahHeap::heap()->is_degenerated_gc_in_progress(),
         "Only for degenerated GC and full GC");

  bool check_alive = !full_gc;
  ShenandoahPhaseTimings::Phase p = full_gc ?
                                    ShenandoahPhaseTimings::full_gc_update_roots :
                                    ShenandoahPhaseTimings::degen_gc_update_roots;

  ShenandoahGCPhase phase(p);
#if COMPILER2_OR_JVMCI
  DerivedPointerTable::clear();
#endif

  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  WorkGang* workers = heap->workers();
  uint nworkers = workers->active_workers();

  ShenandoahRootUpdater root_updater(nworkers, p);
  ShenandoahUpdateRootsTask update_roots(&root_updater, check_alive);
  workers->run_task(&update_roots);

#if COMPILER2_OR_JVMCI
  DerivedPointerTable::update_pointers();
#endif
}
