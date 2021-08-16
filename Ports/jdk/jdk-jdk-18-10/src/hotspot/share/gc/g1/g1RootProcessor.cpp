/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/stringTable.hpp"
#include "code/codeCache.hpp"
#include "gc/g1/g1BarrierSet.hpp"
#include "gc/g1/g1CodeBlobClosure.hpp"
#include "gc/g1/g1CollectedHeap.inline.hpp"
#include "gc/g1/g1CollectorState.hpp"
#include "gc/g1/g1GCParPhaseTimesTracker.hpp"
#include "gc/g1/g1GCPhaseTimes.hpp"
#include "gc/g1/g1ParScanThreadState.inline.hpp"
#include "gc/g1/g1Policy.hpp"
#include "gc/g1/g1RootClosures.hpp"
#include "gc/g1/g1RootProcessor.hpp"
#include "gc/g1/heapRegion.inline.hpp"
#include "gc/shared/oopStorage.inline.hpp"
#include "gc/shared/oopStorageSet.hpp"
#include "gc/shared/oopStorageSetParState.inline.hpp"
#include "gc/shared/referenceProcessor.hpp"
#include "memory/allocation.inline.hpp"
#include "runtime/mutex.hpp"
#include "utilities/enumIterator.hpp"
#include "utilities/macros.hpp"

G1RootProcessor::G1RootProcessor(G1CollectedHeap* g1h, uint n_workers) :
    _g1h(g1h),
    _process_strong_tasks(G1RP_PS_NumElements),
    _srs(n_workers) {}

void G1RootProcessor::evacuate_roots(G1ParScanThreadState* pss, uint worker_id) {
  G1GCPhaseTimes* phase_times = _g1h->phase_times();

  G1EvacPhaseTimesTracker timer(phase_times, pss, G1GCPhaseTimes::ExtRootScan, worker_id);

  G1EvacuationRootClosures* closures = pss->closures();
  process_java_roots(closures, phase_times, worker_id);

  process_vm_roots(closures, phase_times, worker_id);

  {
    // Now the CM ref_processor roots.
    G1GCParPhaseTimesTracker x(phase_times, G1GCPhaseTimes::CMRefRoots, worker_id);
    if (_process_strong_tasks.try_claim_task(G1RP_PS_refProcessor_oops_do)) {
      // We need to treat the discovered reference lists of the
      // concurrent mark ref processor as roots and keep entries
      // (which are added by the marking threads) on them live
      // until they can be processed at the end of marking.
      _g1h->ref_processor_cm()->weak_oops_do(closures->strong_oops());
    }
  }

  // CodeCache is already processed in java roots
  _process_strong_tasks.all_tasks_claimed(G1RP_PS_CodeCache_oops_do);
}

// Adaptor to pass the closures to the strong roots in the VM.
class StrongRootsClosures : public G1RootClosures {
  OopClosure* _roots;
  CLDClosure* _clds;
  CodeBlobClosure* _blobs;
public:
  StrongRootsClosures(OopClosure* roots, CLDClosure* clds, CodeBlobClosure* blobs) :
      _roots(roots), _clds(clds), _blobs(blobs) {}

  OopClosure* weak_oops()   { return NULL; }
  OopClosure* strong_oops() { return _roots; }

  CLDClosure* weak_clds()        { return NULL; }
  CLDClosure* strong_clds()      { return _clds; }

  CodeBlobClosure* strong_codeblobs() { return _blobs; }
};

void G1RootProcessor::process_strong_roots(OopClosure* oops,
                                           CLDClosure* clds,
                                           CodeBlobClosure* blobs) {
  StrongRootsClosures closures(oops, clds, blobs);

  process_java_roots(&closures, NULL, 0);
  process_vm_roots(&closures, NULL, 0);

  // CodeCache is already processed in java roots
  // refProcessor is not needed since we are inside a safe point
  _process_strong_tasks.all_tasks_claimed(G1RP_PS_CodeCache_oops_do,
                                          G1RP_PS_refProcessor_oops_do);
}

// Adaptor to pass the closures to all the roots in the VM.
class AllRootsClosures : public G1RootClosures {
  OopClosure* _roots;
  CLDClosure* _clds;
public:
  AllRootsClosures(OopClosure* roots, CLDClosure* clds) :
      _roots(roots), _clds(clds) {}

  OopClosure* weak_oops() { return _roots; }
  OopClosure* strong_oops() { return _roots; }

  // By returning the same CLDClosure for both weak and strong CLDs we ensure
  // that a single walk of the CLDG will invoke the closure on all CLDs i the
  // system.
  CLDClosure* weak_clds() { return _clds; }
  CLDClosure* strong_clds() { return _clds; }

  // We don't want to visit code blobs more than once, so we return NULL for the
  // strong case and walk the entire code cache as a separate step.
  CodeBlobClosure* strong_codeblobs() { return NULL; }
};

void G1RootProcessor::process_all_roots(OopClosure* oops,
                                        CLDClosure* clds,
                                        CodeBlobClosure* blobs) {
  AllRootsClosures closures(oops, clds);

  process_java_roots(&closures, NULL, 0);
  process_vm_roots(&closures, NULL, 0);

  process_code_cache_roots(blobs, NULL, 0);

  // refProcessor is not needed since we are inside a safe point
  _process_strong_tasks.all_tasks_claimed(G1RP_PS_refProcessor_oops_do);
}

void G1RootProcessor::process_java_roots(G1RootClosures* closures,
                                         G1GCPhaseTimes* phase_times,
                                         uint worker_id) {
  // In the concurrent start pause, when class unloading is enabled, G1
  // processes nmethods in two ways, as "strong" and "weak" nmethods.
  //
  // 1) Strong nmethods are reachable from the thread stack frames. G1 applies
  // the G1RootClosures::strong_codeblobs() closure on them. The closure
  // iterates over all oops embedded inside each nmethod, and performs 3
  // operations:
  //   a) evacuates; relocate objects outside of collection set
  //   b) fixes up; remap oops to reflect new addresses
  //   c) mark; mark object alive
  // This keeps these oops alive wrt. to the upcoming marking phase, and their
  // classes will not be unloaded.
  //
  // 2) Weak nmethods are reachable only from the code root remembered set (see
  // G1CodeRootSet). G1 applies the G1RootClosures::weak_codeblobs() closure on
  // them. The closure iterates over all oops embedded inside each nmethod, and
  // performs 2 operations: a) and b).
  // Since these oops are *not* marked, their classes can potentially be
  // unloaded.
  //
  // G1 doesn't segregate strong/weak nmethods processing (finish processing
  // all strong nmethods before starting with any weak nmethods, or vice
  // versa), as that could lead to poor CPU utilization (a single slow thread
  // prevents all other thread from crossing the synchronization barrier).
  // Instead, G1 interleaves strong and weak nmethods processing via
  // per-nmethod synchronization. A nmethod is either *strongly* or *weakly*
  // claimed before processing. A weakly claimed nmethod could be strongly
  // claimed again for performing marking (the c) operation above); see
  // oops_do_process_weak and oops_do_process_strong in nmethod.hpp
  {
    G1GCParPhaseTimesTracker x(phase_times, G1GCPhaseTimes::ThreadRoots, worker_id);
    bool is_par = n_workers() > 1;
    Threads::possibly_parallel_oops_do(is_par,
                                       closures->strong_oops(),
                                       closures->strong_codeblobs());
  }

  {
    G1GCParPhaseTimesTracker x(phase_times, G1GCPhaseTimes::CLDGRoots, worker_id);
    if (_process_strong_tasks.try_claim_task(G1RP_PS_ClassLoaderDataGraph_oops_do)) {
      ClassLoaderDataGraph::roots_cld_do(closures->strong_clds(), closures->weak_clds());
    }
  }
}

void G1RootProcessor::process_vm_roots(G1RootClosures* closures,
                                       G1GCPhaseTimes* phase_times,
                                       uint worker_id) {
  OopClosure* strong_roots = closures->strong_oops();

  for (auto id : EnumRange<OopStorageSet::StrongId>()) {
    G1GCPhaseTimes::GCParPhases phase = G1GCPhaseTimes::strong_oopstorage_phase(id);
    G1GCParPhaseTimesTracker x(phase_times, phase, worker_id);
    _oop_storage_set_strong_par_state.par_state(id)->oops_do(closures->strong_oops());
  }
}

void G1RootProcessor::process_code_cache_roots(CodeBlobClosure* code_closure,
                                               G1GCPhaseTimes* phase_times,
                                               uint worker_id) {
  if (_process_strong_tasks.try_claim_task(G1RP_PS_CodeCache_oops_do)) {
    CodeCache::blobs_do(code_closure);
  }
}

uint G1RootProcessor::n_workers() const {
  return _srs.n_threads();
}
