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

#include "gc/shared/strongRootsScope.hpp"
#include "gc/shared/taskTerminator.hpp"
#include "gc/shared/workgroup.hpp"
#include "gc/shenandoah/shenandoahClosures.inline.hpp"
#include "gc/shenandoah/shenandoahMark.inline.hpp"
#include "gc/shenandoah/shenandoahOopClosures.inline.hpp"
#include "gc/shenandoah/shenandoahReferenceProcessor.hpp"
#include "gc/shenandoah/shenandoahRootProcessor.inline.hpp"
#include "gc/shenandoah/shenandoahSTWMark.hpp"
#include "gc/shenandoah/shenandoahVerifier.hpp"

class ShenandoahInitMarkRootsClosure : public OopClosure {
private:
  ShenandoahObjToScanQueue* const _queue;
  ShenandoahMarkingContext* const _mark_context;

  template <class T>
  inline void do_oop_work(T* p);
public:
  ShenandoahInitMarkRootsClosure(ShenandoahObjToScanQueue* q);

  void do_oop(narrowOop* p) { do_oop_work(p); }
  void do_oop(oop* p)       { do_oop_work(p); }
};

ShenandoahInitMarkRootsClosure::ShenandoahInitMarkRootsClosure(ShenandoahObjToScanQueue* q) :
  _queue(q),
  _mark_context(ShenandoahHeap::heap()->marking_context()) {
}

template <class T>
void ShenandoahInitMarkRootsClosure::do_oop_work(T* p) {
  ShenandoahMark::mark_through_ref<T, NO_DEDUP>(p, _queue, _mark_context, NULL, false);
}

class ShenandoahSTWMarkTask : public AbstractGangTask {
private:
  ShenandoahSTWMark* const _mark;

public:
  ShenandoahSTWMarkTask(ShenandoahSTWMark* mark);
  void work(uint worker_id);
};

ShenandoahSTWMarkTask::ShenandoahSTWMarkTask(ShenandoahSTWMark* mark) :
  AbstractGangTask("Shenandoah STW mark"),
  _mark(mark) {
}

void ShenandoahSTWMarkTask::work(uint worker_id) {
  ShenandoahParallelWorkerSession worker_session(worker_id);
  _mark->mark_roots(worker_id);
  _mark->finish_mark(worker_id);
}

ShenandoahSTWMark::ShenandoahSTWMark(bool full_gc) :
  ShenandoahMark(),
  _root_scanner(full_gc ? ShenandoahPhaseTimings::full_gc_mark : ShenandoahPhaseTimings::degen_gc_stw_mark),
  _terminator(ShenandoahHeap::heap()->workers()->active_workers(), ShenandoahHeap::heap()->marking_context()->task_queues()),
  _full_gc(full_gc) {
  assert(ShenandoahSafepoint::is_at_shenandoah_safepoint(), "Must be at a Shenandoah safepoint");
}

void ShenandoahSTWMark::mark() {
  // Weak reference processing
  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  ShenandoahReferenceProcessor* rp = heap->ref_processor();
  rp->reset_thread_locals();
  rp->set_soft_reference_policy(heap->soft_ref_policy()->should_clear_all_soft_refs());

  // Init mark, do not expect forwarded pointers in roots
  if (ShenandoahVerify) {
    assert(Thread::current()->is_VM_thread(), "Must be");
    heap->verifier()->verify_roots_no_forwarded();
  }

  uint nworkers = heap->workers()->active_workers();
  task_queues()->reserve(nworkers);

  TASKQUEUE_STATS_ONLY(task_queues()->reset_taskqueue_stats());

  {
    // Mark
    StrongRootsScope scope(nworkers);
    ShenandoahSTWMarkTask task(this);
    heap->workers()->run_task(&task);

    assert(task_queues()->is_empty(), "Should be empty");
  }

  heap->mark_complete_marking_context();

  assert(task_queues()->is_empty(), "Should be empty");
  TASKQUEUE_STATS_ONLY(task_queues()->print_taskqueue_stats());
  TASKQUEUE_STATS_ONLY(task_queues()->reset_taskqueue_stats());
}

void ShenandoahSTWMark::mark_roots(uint worker_id) {
  ShenandoahInitMarkRootsClosure  init_mark(task_queues()->queue(worker_id));
  _root_scanner.roots_do(&init_mark, worker_id);
}

void ShenandoahSTWMark::finish_mark(uint worker_id) {
  ShenandoahPhaseTimings::Phase phase = _full_gc ? ShenandoahPhaseTimings::full_gc_mark : ShenandoahPhaseTimings::degen_gc_stw_mark;
  ShenandoahWorkerTimingsTracker timer(phase, ShenandoahPhaseTimings::ParallelMark, worker_id);
  ShenandoahReferenceProcessor* rp = ShenandoahHeap::heap()->ref_processor();

  mark_loop(worker_id, &_terminator, rp,
            false /* not cancellable */,
            ShenandoahStringDedup::is_enabled() ? ALWAYS_DEDUP : NO_DEDUP);
}

