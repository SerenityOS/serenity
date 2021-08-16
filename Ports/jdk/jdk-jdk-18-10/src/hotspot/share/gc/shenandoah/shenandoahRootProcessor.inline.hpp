/*
 * Copyright (c) 2019, 2021, Red Hat, Inc. All rights reserved.
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

#ifndef SHARE_GC_SHENANDOAH_SHENANDOAHROOTPROCESSOR_INLINE_HPP
#define SHARE_GC_SHENANDOAH_SHENANDOAHROOTPROCESSOR_INLINE_HPP

#include "gc/shenandoah/shenandoahRootProcessor.hpp"

#include "classfile/classLoaderDataGraph.hpp"
#include "gc/shared/oopStorageSetParState.inline.hpp"
#include "gc/shenandoah/shenandoahClosures.inline.hpp"
#include "gc/shenandoah/shenandoahHeap.inline.hpp"
#include "gc/shenandoah/shenandoahPhaseTimings.hpp"
#include "gc/shenandoah/shenandoahUtils.hpp"
#include "gc/shenandoah/heuristics/shenandoahHeuristics.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/safepoint.hpp"

template <bool CONCURRENT>
ShenandoahVMWeakRoots<CONCURRENT>::ShenandoahVMWeakRoots(ShenandoahPhaseTimings::Phase phase) :
  _phase(phase) {
}

template <bool CONCURRENT>
template <typename T>
void ShenandoahVMWeakRoots<CONCURRENT>::oops_do(T* cl, uint worker_id) {
  ShenandoahWorkerTimingsTracker timer(_phase, ShenandoahPhaseTimings::VMWeakRoots, worker_id);
  _weak_roots.oops_do(cl);
}

template <bool CONCURRENT>
template <typename IsAlive, typename KeepAlive>
void ShenandoahVMWeakRoots<CONCURRENT>::weak_oops_do(IsAlive* is_alive, KeepAlive* keep_alive, uint worker_id) {
  ShenandoahCleanUpdateWeakOopsClosure<CONCURRENT, IsAlive, KeepAlive> cl(is_alive, keep_alive);
  ShenandoahWorkerTimingsTracker timer(_phase, ShenandoahPhaseTimings::VMWeakRoots, worker_id);
  _weak_roots.oops_do(&cl);
}

template <bool CONCURRENT>
void ShenandoahVMWeakRoots<CONCURRENT>::report_num_dead() {
  _weak_roots.report_num_dead();
}

template <bool CONCURRENT>
ShenandoahVMRoots<CONCURRENT>::ShenandoahVMRoots(ShenandoahPhaseTimings::Phase phase) :
  _phase(phase) {
}

template <bool CONCURRENT>
template <typename T>
void ShenandoahVMRoots<CONCURRENT>::oops_do(T* cl, uint worker_id) {
  ShenandoahWorkerTimingsTracker timer(_phase, ShenandoahPhaseTimings::VMStrongRoots, worker_id);
  _strong_roots.oops_do(cl);
}

template <bool CONCURRENT, bool SINGLE_THREADED>
ShenandoahClassLoaderDataRoots<CONCURRENT, SINGLE_THREADED>::ShenandoahClassLoaderDataRoots(ShenandoahPhaseTimings::Phase phase, uint n_workers) :
  _semaphore(worker_count(n_workers)),
  _phase(phase) {
  if (!SINGLE_THREADED) {
    ClassLoaderDataGraph::clear_claimed_marks();
  }
  if (CONCURRENT && !SINGLE_THREADED) {
    ClassLoaderDataGraph_lock->lock();
  }

  // Non-concurrent mode only runs at safepoints by VM thread
  assert(CONCURRENT || SafepointSynchronize::is_at_safepoint(), "Must be at a safepoint");
  assert(CONCURRENT || Thread::current()->is_VM_thread(), "Can only be done by VM thread");
}

template <bool CONCURRENT, bool SINGLE_THREADED>
ShenandoahClassLoaderDataRoots<CONCURRENT, SINGLE_THREADED>::~ShenandoahClassLoaderDataRoots() {
  if (CONCURRENT && !SINGLE_THREADED) {
    ClassLoaderDataGraph_lock->unlock();
  }
}

template <bool CONCURRENT, bool SINGLE_THREADED>
void ShenandoahClassLoaderDataRoots<CONCURRENT, SINGLE_THREADED>::cld_do_impl(CldDo f, CLDClosure* clds, uint worker_id) {
  if (CONCURRENT) {
    if (_semaphore.try_acquire()) {
      ShenandoahWorkerTimingsTracker timer(_phase, ShenandoahPhaseTimings::CLDGRoots, worker_id);
      if (SINGLE_THREADED){
        MutexLocker ml(ClassLoaderDataGraph_lock, Mutex::_no_safepoint_check_flag);
        f(clds);
      } else {
        f(clds);
      }
      _semaphore.claim_all();
    }
  } else {
    ShenandoahWorkerTimingsTracker timer(_phase, ShenandoahPhaseTimings::CLDGRoots, worker_id);
    f(clds);
  }
}

template <bool CONCURRENT, bool SINGLE_THREADED>
void ShenandoahClassLoaderDataRoots<CONCURRENT, SINGLE_THREADED>::always_strong_cld_do(CLDClosure* clds, uint worker_id) {
  cld_do_impl(&ClassLoaderDataGraph::always_strong_cld_do, clds, worker_id);
}

template <bool CONCURRENT, bool SINGLE_THREADED>
void ShenandoahClassLoaderDataRoots<CONCURRENT, SINGLE_THREADED>::cld_do(CLDClosure* clds, uint worker_id) {
  cld_do_impl(&ClassLoaderDataGraph::cld_do, clds, worker_id);
}

class ShenandoahParallelOopsDoThreadClosure : public ThreadClosure {
private:
  OopClosure* _f;
  CodeBlobClosure* _cf;
  ThreadClosure* _thread_cl;
public:
  ShenandoahParallelOopsDoThreadClosure(OopClosure* f, CodeBlobClosure* cf, ThreadClosure* thread_cl) :
    _f(f), _cf(cf), _thread_cl(thread_cl) {}

  void do_thread(Thread* t) {
    if (_thread_cl != NULL) {
      _thread_cl->do_thread(t);
    }
    t->oops_do(_f, _cf);
  }
};

// The rationale for selecting the roots to scan is as follows:
//   a. With unload_classes = true, we only want to scan the actual strong roots from the
//      code cache. This will allow us to identify the dead classes, unload them, *and*
//      invalidate the relevant code cache blobs. This could be only done together with
//      class unloading.
//   b. With unload_classes = false, we have to nominally retain all the references from code
//      cache, because there could be the case of embedded class/oop in the generated code,
//      which we will never visit during mark. Without code cache invalidation, as in (a),
//      we risk executing that code cache blob, and crashing.
template <typename T>
void ShenandoahSTWRootScanner::roots_do(T* oops, uint worker_id) {
  MarkingCodeBlobClosure blobs_cl(oops, !CodeBlobToOopClosure::FixRelocations);
  CLDToOopClosure clds(oops, ClassLoaderData::_claim_strong);
  ResourceMark rm;

  if (_unload_classes) {
    _thread_roots.oops_do(oops, &blobs_cl, worker_id);
    _cld_roots.always_strong_cld_do(&clds, worker_id);
  } else {
    _thread_roots.oops_do(oops, NULL, worker_id);
    _code_roots.code_blobs_do(&blobs_cl, worker_id);
    _cld_roots.cld_do(&clds, worker_id);
  }

  _vm_roots.oops_do<T>(oops, worker_id);
}

template <typename IsAlive, typename KeepAlive>
void ShenandoahRootUpdater::roots_do(uint worker_id, IsAlive* is_alive, KeepAlive* keep_alive) {
  CodeBlobToOopClosure update_blobs(keep_alive, CodeBlobToOopClosure::FixRelocations);
  ShenandoahCodeBlobAndDisarmClosure blobs_and_disarm_Cl(keep_alive);
  CodeBlobToOopClosure* codes_cl = (ClassUnloading && ShenandoahNMethodBarrier) ?
                                   static_cast<CodeBlobToOopClosure*>(&blobs_and_disarm_Cl) :
                                   static_cast<CodeBlobToOopClosure*>(&update_blobs);

  CLDToOopClosure clds(keep_alive, ClassLoaderData::_claim_strong);

  // Process light-weight/limited parallel roots then
  _vm_roots.oops_do(keep_alive, worker_id);
  _weak_roots.weak_oops_do<IsAlive, KeepAlive>(is_alive, keep_alive, worker_id);
  _cld_roots.cld_do(&clds, worker_id);

  // Process heavy-weight/fully parallel roots the last
  _code_roots.code_blobs_do(codes_cl, worker_id);
  _thread_roots.oops_do(keep_alive, NULL, worker_id);
}

#endif // SHARE_GC_SHENANDOAH_SHENANDOAHROOTPROCESSOR_INLINE_HPP
