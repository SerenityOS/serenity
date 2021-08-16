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
 */

#include "precompiled.hpp"
#include "gc/shared/gcId.hpp"
#include "gc/shared/gcLocker.hpp"
#include "gc/shared/gcVMOperations.hpp"
#include "gc/shared/isGCActiveMark.hpp"
#include "gc/z/zAbort.inline.hpp"
#include "gc/z/zBreakpoint.hpp"
#include "gc/z/zCollectedHeap.hpp"
#include "gc/z/zDriver.hpp"
#include "gc/z/zHeap.inline.hpp"
#include "gc/z/zMessagePort.inline.hpp"
#include "gc/z/zServiceability.hpp"
#include "gc/z/zStat.hpp"
#include "gc/z/zVerify.hpp"
#include "logging/log.hpp"
#include "memory/universe.hpp"
#include "runtime/vmOperations.hpp"
#include "runtime/vmThread.hpp"

static const ZStatPhaseCycle      ZPhaseCycle("Garbage Collection Cycle");
static const ZStatPhasePause      ZPhasePauseMarkStart("Pause Mark Start");
static const ZStatPhaseConcurrent ZPhaseConcurrentMark("Concurrent Mark");
static const ZStatPhaseConcurrent ZPhaseConcurrentMarkContinue("Concurrent Mark Continue");
static const ZStatPhaseConcurrent ZPhaseConcurrentMarkFree("Concurrent Mark Free");
static const ZStatPhasePause      ZPhasePauseMarkEnd("Pause Mark End");
static const ZStatPhaseConcurrent ZPhaseConcurrentProcessNonStrongReferences("Concurrent Process Non-Strong References");
static const ZStatPhaseConcurrent ZPhaseConcurrentResetRelocationSet("Concurrent Reset Relocation Set");
static const ZStatPhaseConcurrent ZPhaseConcurrentSelectRelocationSet("Concurrent Select Relocation Set");
static const ZStatPhasePause      ZPhasePauseRelocateStart("Pause Relocate Start");
static const ZStatPhaseConcurrent ZPhaseConcurrentRelocated("Concurrent Relocate");
static const ZStatCriticalPhase   ZCriticalPhaseGCLockerStall("GC Locker Stall", false /* verbose */);
static const ZStatSampler         ZSamplerJavaThreads("System", "Java Threads", ZStatUnitThreads);

ZDriverRequest::ZDriverRequest() :
    ZDriverRequest(GCCause::_no_gc) {}

ZDriverRequest::ZDriverRequest(GCCause::Cause cause) :
    ZDriverRequest(cause, ConcGCThreads) {}

ZDriverRequest::ZDriverRequest(GCCause::Cause cause, uint nworkers) :
    _cause(cause),
    _nworkers(nworkers) {}

bool ZDriverRequest::operator==(const ZDriverRequest& other) const {
  return _cause == other._cause;
}

GCCause::Cause ZDriverRequest::cause() const {
  return _cause;
}

uint ZDriverRequest::nworkers() const {
  return _nworkers;
}

class VM_ZOperation : public VM_Operation {
private:
  const uint _gc_id;
  bool       _gc_locked;
  bool       _success;

public:
  VM_ZOperation() :
      _gc_id(GCId::current()),
      _gc_locked(false),
      _success(false) {}

  virtual bool needs_inactive_gc_locker() const {
    // An inactive GC locker is needed in operations where we change the bad
    // mask or move objects. Changing the bad mask will invalidate all oops,
    // which makes it conceptually the same thing as moving all objects.
    return false;
  }

  virtual bool skip_thread_oop_barriers() const {
    return true;
  }

  virtual bool do_operation() = 0;

  virtual bool doit_prologue() {
    Heap_lock->lock();
    return true;
  }

  virtual void doit() {
    // Abort if GC locker state is incompatible
    if (needs_inactive_gc_locker() && GCLocker::check_active_before_gc()) {
      _gc_locked = true;
      return;
    }

    // Setup GC id and active marker
    GCIdMark gc_id_mark(_gc_id);
    IsGCActiveMark gc_active_mark;

    // Verify before operation
    ZVerify::before_zoperation();

    // Execute operation
    _success = do_operation();

    // Update statistics
    ZStatSample(ZSamplerJavaThreads, Threads::number_of_threads());
  }

  virtual void doit_epilogue() {
    Heap_lock->unlock();
  }

  bool gc_locked() const {
    return _gc_locked;
  }

  bool success() const {
    return _success;
  }
};

class VM_ZMarkStart : public VM_ZOperation {
public:
  virtual VMOp_Type type() const {
    return VMOp_ZMarkStart;
  }

  virtual bool needs_inactive_gc_locker() const {
    return true;
  }

  virtual bool do_operation() {
    ZStatTimer timer(ZPhasePauseMarkStart);
    ZServiceabilityPauseTracer tracer;

    ZCollectedHeap::heap()->increment_total_collections(true /* full */);

    ZHeap::heap()->mark_start();
    return true;
  }
};

class VM_ZMarkEnd : public VM_ZOperation {
public:
  virtual VMOp_Type type() const {
    return VMOp_ZMarkEnd;
  }

  virtual bool do_operation() {
    ZStatTimer timer(ZPhasePauseMarkEnd);
    ZServiceabilityPauseTracer tracer;
    return ZHeap::heap()->mark_end();
  }
};

class VM_ZRelocateStart : public VM_ZOperation {
public:
  virtual VMOp_Type type() const {
    return VMOp_ZRelocateStart;
  }

  virtual bool needs_inactive_gc_locker() const {
    return true;
  }

  virtual bool do_operation() {
    ZStatTimer timer(ZPhasePauseRelocateStart);
    ZServiceabilityPauseTracer tracer;
    ZHeap::heap()->relocate_start();
    return true;
  }
};

class VM_ZVerify : public VM_Operation {
public:
  virtual VMOp_Type type() const {
    return VMOp_ZVerify;
  }

  virtual bool skip_thread_oop_barriers() const {
    return true;
  }

  virtual void doit() {
    ZVerify::after_weak_processing();
  }
};

ZDriver::ZDriver() :
    _gc_cycle_port(),
    _gc_locker_port() {
  set_name("ZDriver");
  create_and_start();
}

bool ZDriver::is_busy() const {
  return _gc_cycle_port.is_busy();
}

void ZDriver::collect(const ZDriverRequest& request) {
  switch (request.cause()) {
  case GCCause::_wb_young_gc:
  case GCCause::_wb_conc_mark:
  case GCCause::_wb_full_gc:
  case GCCause::_dcmd_gc_run:
  case GCCause::_java_lang_system_gc:
  case GCCause::_full_gc_alot:
  case GCCause::_scavenge_alot:
  case GCCause::_jvmti_force_gc:
  case GCCause::_metadata_GC_clear_soft_refs:
    // Start synchronous GC
    _gc_cycle_port.send_sync(request);
    break;

  case GCCause::_z_timer:
  case GCCause::_z_warmup:
  case GCCause::_z_allocation_rate:
  case GCCause::_z_allocation_stall:
  case GCCause::_z_proactive:
  case GCCause::_z_high_usage:
  case GCCause::_metadata_GC_threshold:
    // Start asynchronous GC
    _gc_cycle_port.send_async(request);
    break;

  case GCCause::_gc_locker:
    // Restart VM operation previously blocked by the GC locker
    _gc_locker_port.signal();
    break;

  case GCCause::_wb_breakpoint:
    ZBreakpoint::start_gc();
    _gc_cycle_port.send_async(request);
    break;

  default:
    // Other causes not supported
    fatal("Unsupported GC cause (%s)", GCCause::to_string(request.cause()));
    break;
  }
}

template <typename T>
bool ZDriver::pause() {
  for (;;) {
    T op;
    VMThread::execute(&op);
    if (op.gc_locked()) {
      // Wait for GC to become unlocked and restart the VM operation
      ZStatTimer timer(ZCriticalPhaseGCLockerStall);
      _gc_locker_port.wait();
      continue;
    }

    // Notify VM operation completed
    _gc_locker_port.ack();

    return op.success();
  }
}

void ZDriver::pause_mark_start() {
  pause<VM_ZMarkStart>();
}

void ZDriver::concurrent_mark() {
  ZStatTimer timer(ZPhaseConcurrentMark);
  ZBreakpoint::at_after_marking_started();
  ZHeap::heap()->mark(true /* initial */);
  ZBreakpoint::at_before_marking_completed();
}

bool ZDriver::pause_mark_end() {
  return pause<VM_ZMarkEnd>();
}

void ZDriver::concurrent_mark_continue() {
  ZStatTimer timer(ZPhaseConcurrentMarkContinue);
  ZHeap::heap()->mark(false /* initial */);
}

void ZDriver::concurrent_mark_free() {
  ZStatTimer timer(ZPhaseConcurrentMarkFree);
  ZHeap::heap()->mark_free();
}

void ZDriver::concurrent_process_non_strong_references() {
  ZStatTimer timer(ZPhaseConcurrentProcessNonStrongReferences);
  ZBreakpoint::at_after_reference_processing_started();
  ZHeap::heap()->process_non_strong_references();
}

void ZDriver::concurrent_reset_relocation_set() {
  ZStatTimer timer(ZPhaseConcurrentResetRelocationSet);
  ZHeap::heap()->reset_relocation_set();
}

void ZDriver::pause_verify() {
  if (VerifyBeforeGC || VerifyDuringGC || VerifyAfterGC) {
    // Full verification
    VM_Verify op;
    VMThread::execute(&op);
  } else if (ZVerifyRoots || ZVerifyObjects) {
    // Limited verification
    VM_ZVerify op;
    VMThread::execute(&op);
  }
}

void ZDriver::concurrent_select_relocation_set() {
  ZStatTimer timer(ZPhaseConcurrentSelectRelocationSet);
  ZHeap::heap()->select_relocation_set();
}

void ZDriver::pause_relocate_start() {
  pause<VM_ZRelocateStart>();
}

void ZDriver::concurrent_relocate() {
  ZStatTimer timer(ZPhaseConcurrentRelocated);
  ZHeap::heap()->relocate();
}

void ZDriver::check_out_of_memory() {
  ZHeap::heap()->check_out_of_memory();
}

static bool should_clear_soft_references(const ZDriverRequest& request) {
  // Clear soft references if implied by the GC cause
  if (request.cause() == GCCause::_wb_full_gc ||
      request.cause() == GCCause::_metadata_GC_clear_soft_refs ||
      request.cause() == GCCause::_z_allocation_stall) {
    // Clear
    return true;
  }

  // Don't clear
  return false;
}

static uint select_active_worker_threads_dynamic(const ZDriverRequest& request) {
  // Use requested number of worker threads
  return request.nworkers();
}

static uint select_active_worker_threads_static(const ZDriverRequest& request) {
  const GCCause::Cause cause = request.cause();
  const uint nworkers = request.nworkers();

  // Boost number of worker threads if implied by the GC cause
  if (cause == GCCause::_wb_full_gc ||
      cause == GCCause::_java_lang_system_gc ||
      cause == GCCause::_metadata_GC_clear_soft_refs ||
      cause == GCCause::_z_allocation_stall) {
    // Boost
    const uint boosted_nworkers = MAX2(nworkers, ParallelGCThreads);
    return boosted_nworkers;
  }

  // Use requested number of worker threads
  return nworkers;
}

static uint select_active_worker_threads(const ZDriverRequest& request) {
  if (UseDynamicNumberOfGCThreads) {
    return select_active_worker_threads_dynamic(request);
  } else {
    return select_active_worker_threads_static(request);
  }
}

class ZDriverGCScope : public StackObj {
private:
  GCIdMark                   _gc_id;
  GCCause::Cause             _gc_cause;
  GCCauseSetter              _gc_cause_setter;
  ZStatTimer                 _timer;
  ZServiceabilityCycleTracer _tracer;

public:
  ZDriverGCScope(const ZDriverRequest& request) :
      _gc_id(),
      _gc_cause(request.cause()),
      _gc_cause_setter(ZCollectedHeap::heap(), _gc_cause),
      _timer(ZPhaseCycle),
      _tracer() {
    // Update statistics
    ZStatCycle::at_start();

    // Set up soft reference policy
    const bool clear = should_clear_soft_references(request);
    ZHeap::heap()->set_soft_reference_policy(clear);

    // Select number of worker threads to use
    const uint nworkers = select_active_worker_threads(request);
    ZHeap::heap()->set_active_workers(nworkers);
  }

  ~ZDriverGCScope() {
    // Update statistics
    ZStatCycle::at_end(_gc_cause, ZHeap::heap()->active_workers());

    // Update data used by soft reference policy
    Universe::heap()->update_capacity_and_used_at_gc();

    // Signal that we have completed a visit to all live objects
    Universe::heap()->record_whole_heap_examined_timestamp();
  }
};

// Macro to execute a termination check after a concurrent phase. Note
// that it's important that the termination check comes after the call
// to the function f, since we can't abort between pause_relocate_start()
// and concurrent_relocate(). We need to let concurrent_relocate() call
// abort_page() on the remaining entries in the relocation set.
#define concurrent(f)                 \
  do {                                \
    concurrent_##f();                 \
    if (should_terminate()) {         \
      return;                         \
    }                                 \
  } while (false)

void ZDriver::gc(const ZDriverRequest& request) {
  ZDriverGCScope scope(request);

  // Phase 1: Pause Mark Start
  pause_mark_start();

  // Phase 2: Concurrent Mark
  concurrent(mark);

  // Phase 3: Pause Mark End
  while (!pause_mark_end()) {
    // Phase 3.5: Concurrent Mark Continue
    concurrent(mark_continue);
  }

  // Phase 4: Concurrent Mark Free
  concurrent(mark_free);

  // Phase 5: Concurrent Process Non-Strong References
  concurrent(process_non_strong_references);

  // Phase 6: Concurrent Reset Relocation Set
  concurrent(reset_relocation_set);

  // Phase 7: Pause Verify
  pause_verify();

  // Phase 8: Concurrent Select Relocation Set
  concurrent(select_relocation_set);

  // Phase 9: Pause Relocate Start
  pause_relocate_start();

  // Phase 10: Concurrent Relocate
  concurrent(relocate);
}

void ZDriver::run_service() {
  // Main loop
  while (!should_terminate()) {
    // Wait for GC request
    const ZDriverRequest request = _gc_cycle_port.receive();
    if (request.cause() == GCCause::_no_gc) {
      continue;
    }

    ZBreakpoint::at_before_gc();

    // Run GC
    gc(request);

    // Notify GC completed
    _gc_cycle_port.ack();

    // Check for out of memory condition
    check_out_of_memory();

    ZBreakpoint::at_after_gc();
  }
}

void ZDriver::stop_service() {
  ZAbort::abort();
  _gc_cycle_port.send_async(GCCause::_no_gc);
}
