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

#ifndef SHARE_GC_G1_G1ROOTPROCESSOR_HPP
#define SHARE_GC_G1_G1ROOTPROCESSOR_HPP

#include "gc/shared/oopStorageSetParState.hpp"
#include "gc/shared/strongRootsScope.hpp"
#include "memory/allocation.hpp"
#include "runtime/mutex.hpp"

class CLDClosure;
class CodeBlobClosure;
class G1CollectedHeap;
class G1EvacuationRootClosures;
class G1GCPhaseTimes;
class G1ParScanThreadState;
class G1RootClosures;
class Monitor;
class OopClosure;
class SubTasksDone;

// Scoped object to assist in applying oop, CLD and code blob closures to
// root locations. Handles claiming of different root scanning tasks
// and takes care of global state for root scanning via a StrongRootsScope.
// In the parallel case there is a shared G1RootProcessor object where all
// worker thread call the process_roots methods.
class G1RootProcessor : public StackObj {
  G1CollectedHeap* _g1h;
  SubTasksDone _process_strong_tasks;
  StrongRootsScope _srs;
  OopStorageSetStrongParState<false, false> _oop_storage_set_strong_par_state;

  enum G1H_process_roots_tasks {
    G1RP_PS_ClassLoaderDataGraph_oops_do,
    G1RP_PS_CodeCache_oops_do,
    G1RP_PS_refProcessor_oops_do,
    // Leave this one last.
    G1RP_PS_NumElements
  };

  void process_java_roots(G1RootClosures* closures,
                          G1GCPhaseTimes* phase_times,
                          uint worker_id);

  void process_vm_roots(G1RootClosures* closures,
                        G1GCPhaseTimes* phase_times,
                        uint worker_id);

  void process_code_cache_roots(CodeBlobClosure* code_closure,
                                G1GCPhaseTimes* phase_times,
                                uint worker_id);

public:
  G1RootProcessor(G1CollectedHeap* g1h, uint n_workers);

  // Apply correct closures from pss to the strongly and weakly reachable roots in the system
  // in a single pass.
  // Record and report timing measurements for sub phases using worker_id.
  void evacuate_roots(G1ParScanThreadState* pss, uint worker_id);

  // Apply oops, clds and blobs to all strongly reachable roots in the system
  void process_strong_roots(OopClosure* oops,
                            CLDClosure* clds,
                            CodeBlobClosure* blobs);

  // Apply oops, clds and blobs to strongly and weakly reachable roots in the system
  void process_all_roots(OopClosure* oops,
                         CLDClosure* clds,
                         CodeBlobClosure* blobs);

  // Number of worker threads used by the root processor.
  uint n_workers() const;
};

#endif // SHARE_GC_G1_G1ROOTPROCESSOR_HPP
