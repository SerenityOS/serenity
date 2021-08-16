/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "compiler/oopMap.hpp"
#include "gc/parallel/parallelScavengeHeap.hpp"
#include "gc/parallel/psAdaptiveSizePolicy.hpp"
#include "gc/parallel/psClosure.inline.hpp"
#include "gc/parallel/psCompactionManager.hpp"
#include "gc/parallel/psParallelCompact.inline.hpp"
#include "gc/parallel/psPromotionManager.inline.hpp"
#include "gc/parallel/psRootType.hpp"
#include "gc/parallel/psScavenge.inline.hpp"
#include "gc/shared/gcCause.hpp"
#include "gc/shared/gcHeapSummary.hpp"
#include "gc/shared/gcId.hpp"
#include "gc/shared/gcLocker.hpp"
#include "gc/shared/gcTimer.hpp"
#include "gc/shared/gcTrace.hpp"
#include "gc/shared/gcTraceTime.inline.hpp"
#include "gc/shared/isGCActiveMark.hpp"
#include "gc/shared/oopStorage.inline.hpp"
#include "gc/shared/oopStorageSetParState.inline.hpp"
#include "gc/shared/oopStorageParState.inline.hpp"
#include "gc/shared/referencePolicy.hpp"
#include "gc/shared/referenceProcessor.hpp"
#include "gc/shared/referenceProcessorPhaseTimes.hpp"
#include "gc/shared/scavengableNMethods.hpp"
#include "gc/shared/spaceDecorator.inline.hpp"
#include "gc/shared/taskTerminator.hpp"
#include "gc/shared/weakProcessor.inline.hpp"
#include "gc/shared/workerPolicy.hpp"
#include "gc/shared/workgroup.hpp"
#include "memory/iterator.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "logging/log.hpp"
#include "oops/access.inline.hpp"
#include "oops/compressedOops.inline.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/threadCritical.hpp"
#include "runtime/vmThread.hpp"
#include "runtime/vmOperations.hpp"
#include "services/memoryService.hpp"
#include "utilities/stack.inline.hpp"

HeapWord*                     PSScavenge::_to_space_top_before_gc = NULL;
int                           PSScavenge::_consecutive_skipped_scavenges = 0;
SpanSubjectToDiscoveryClosure PSScavenge::_span_based_discoverer;
ReferenceProcessor*           PSScavenge::_ref_processor = NULL;
PSCardTable*                  PSScavenge::_card_table = NULL;
bool                          PSScavenge::_survivor_overflow = false;
uint                          PSScavenge::_tenuring_threshold = 0;
HeapWord*                     PSScavenge::_young_generation_boundary = NULL;
uintptr_t                     PSScavenge::_young_generation_boundary_compressed = 0;
elapsedTimer                  PSScavenge::_accumulated_time;
STWGCTimer                    PSScavenge::_gc_timer;
ParallelScavengeTracer        PSScavenge::_gc_tracer;
CollectorCounters*            PSScavenge::_counters = NULL;

static void scavenge_roots_work(ParallelRootType::Value root_type, uint worker_id) {
  assert(ParallelScavengeHeap::heap()->is_gc_active(), "called outside gc");

  PSPromotionManager* pm = PSPromotionManager::gc_thread_promotion_manager(worker_id);
  PSScavengeRootsClosure roots_closure(pm);
  PSPromoteRootsClosure  roots_to_old_closure(pm);

  switch (root_type) {
    case ParallelRootType::class_loader_data:
      {
        PSScavengeCLDClosure cld_closure(pm);
        ClassLoaderDataGraph::cld_do(&cld_closure);
      }
      break;

    case ParallelRootType::code_cache:
      {
        MarkingCodeBlobClosure code_closure(&roots_to_old_closure, CodeBlobToOopClosure::FixRelocations);
        ScavengableNMethods::nmethods_do(&code_closure);
      }
      break;

    case ParallelRootType::sentinel:
    DEBUG_ONLY(default:) // DEBUG_ONLY hack will create compile error on release builds (-Wswitch) and runtime check on debug builds
      fatal("Bad enumeration value: %u", root_type);
      break;
  }

  // Do the real work
  pm->drain_stacks(false);
}

static void steal_work(TaskTerminator& terminator, uint worker_id) {
  assert(ParallelScavengeHeap::heap()->is_gc_active(), "called outside gc");

  PSPromotionManager* pm =
    PSPromotionManager::gc_thread_promotion_manager(worker_id);
  pm->drain_stacks(true);
  guarantee(pm->stacks_empty(),
            "stacks should be empty at this point");

  while (true) {
    ScannerTask task;
    if (PSPromotionManager::steal_depth(worker_id, task)) {
      TASKQUEUE_STATS_ONLY(pm->record_steal(task));
      pm->process_popped_location_depth(task);
      pm->drain_stacks_depth(true);
    } else {
      if (terminator.offer_termination()) {
        break;
      }
    }
  }
  guarantee(pm->stacks_empty(), "stacks should be empty at this point");
}

// Define before use
class PSIsAliveClosure: public BoolObjectClosure {
public:
  bool do_object_b(oop p) {
    return (!PSScavenge::is_obj_in_young(p)) || p->is_forwarded();
  }
};

PSIsAliveClosure PSScavenge::_is_alive_closure;

class PSKeepAliveClosure: public OopClosure {
protected:
  MutableSpace* _to_space;
  PSPromotionManager* _promotion_manager;

public:
  PSKeepAliveClosure(PSPromotionManager* pm) : _promotion_manager(pm) {
    ParallelScavengeHeap* heap = ParallelScavengeHeap::heap();
    _to_space = heap->young_gen()->to_space();

    assert(_promotion_manager != NULL, "Sanity");
  }

  template <class T> void do_oop_work(T* p) {
    assert (oopDesc::is_oop(RawAccess<IS_NOT_NULL>::oop_load(p)),
            "expected an oop while scanning weak refs");

    // Weak refs may be visited more than once.
    if (PSScavenge::should_scavenge(p, _to_space)) {
      _promotion_manager->copy_and_push_safe_barrier</*promote_immediately=*/false>(p);
    }
  }
  virtual void do_oop(oop* p)       { PSKeepAliveClosure::do_oop_work(p); }
  virtual void do_oop(narrowOop* p) { PSKeepAliveClosure::do_oop_work(p); }
};

class PSEvacuateFollowersClosure: public VoidClosure {
 private:
  PSPromotionManager* _promotion_manager;
  TaskTerminator* _terminator;
  uint _worker_id;

 public:
  PSEvacuateFollowersClosure(PSPromotionManager* pm, TaskTerminator* terminator, uint worker_id)
    : _promotion_manager(pm), _terminator(terminator), _worker_id(worker_id) {}

  virtual void do_void() {
    assert(_promotion_manager != nullptr, "Sanity");
    _promotion_manager->drain_stacks(true);
    guarantee(_promotion_manager->stacks_empty(),
              "stacks should be empty at this point");

    if (_terminator != nullptr) {
      steal_work(*_terminator, _worker_id);
    }
  }
};

class ParallelScavengeRefProcProxyTask : public RefProcProxyTask {
  TaskTerminator _terminator;

public:
  ParallelScavengeRefProcProxyTask(uint max_workers)
    : RefProcProxyTask("ParallelScavengeRefProcProxyTask", max_workers),
      _terminator(max_workers, ParCompactionManager::oop_task_queues()) {}

  void work(uint worker_id) override {
    assert(worker_id < _max_workers, "sanity");
    PSPromotionManager* promotion_manager = (_tm == RefProcThreadModel::Single) ? PSPromotionManager::vm_thread_promotion_manager() : PSPromotionManager::gc_thread_promotion_manager(worker_id);
    PSIsAliveClosure is_alive;
    PSKeepAliveClosure keep_alive(promotion_manager);;
    PSEvacuateFollowersClosure complete_gc(promotion_manager, (_marks_oops_alive && _tm == RefProcThreadModel::Multi) ? &_terminator : nullptr, worker_id);;
    _rp_task->rp_work(worker_id, &is_alive, &keep_alive, &complete_gc);
  }

  void prepare_run_task_hook() override {
    _terminator.reset_for_reuse(_queue_count);
  }
};

// This method contains all heap specific policy for invoking scavenge.
// PSScavenge::invoke_no_policy() will do nothing but attempt to
// scavenge. It will not clean up after failed promotions, bail out if
// we've exceeded policy time limits, or any other special behavior.
// All such policy should be placed here.
//
// Note that this method should only be called from the vm_thread while
// at a safepoint!
bool PSScavenge::invoke() {
  assert(SafepointSynchronize::is_at_safepoint(), "should be at safepoint");
  assert(Thread::current() == (Thread*)VMThread::vm_thread(), "should be in vm thread");
  assert(!ParallelScavengeHeap::heap()->is_gc_active(), "not reentrant");

  ParallelScavengeHeap* const heap = ParallelScavengeHeap::heap();
  PSAdaptiveSizePolicy* policy = heap->size_policy();
  IsGCActiveMark mark;

  const bool scavenge_done = PSScavenge::invoke_no_policy();
  const bool need_full_gc = !scavenge_done ||
    policy->should_full_GC(heap->old_gen()->free_in_bytes());
  bool full_gc_done = false;

  if (UsePerfData) {
    PSGCAdaptivePolicyCounters* const counters = heap->gc_policy_counters();
    const int ffs_val = need_full_gc ? full_follows_scavenge : not_skipped;
    counters->update_full_follows_scavenge(ffs_val);
  }

  if (need_full_gc) {
    GCCauseSetter gccs(heap, GCCause::_adaptive_size_policy);
    SoftRefPolicy* srp = heap->soft_ref_policy();
    const bool clear_all_softrefs = srp->should_clear_all_soft_refs();

    full_gc_done = PSParallelCompact::invoke_no_policy(clear_all_softrefs);
  }

  return full_gc_done;
}

class PSThreadRootsTaskClosure : public ThreadClosure {
  uint _worker_id;
public:
  PSThreadRootsTaskClosure(uint worker_id) : _worker_id(worker_id) { }
  virtual void do_thread(Thread* thread) {
    assert(ParallelScavengeHeap::heap()->is_gc_active(), "called outside gc");

    PSPromotionManager* pm = PSPromotionManager::gc_thread_promotion_manager(_worker_id);
    PSScavengeRootsClosure roots_closure(pm);
    MarkingCodeBlobClosure roots_in_blobs(&roots_closure, CodeBlobToOopClosure::FixRelocations);

    thread->oops_do(&roots_closure, &roots_in_blobs);

    // Do the real work
    pm->drain_stacks(false);
  }
};

class ScavengeRootsTask : public AbstractGangTask {
  StrongRootsScope _strong_roots_scope; // needed for Threads::possibly_parallel_threads_do
  OopStorageSetStrongParState<false /* concurrent */, false /* is_const */> _oop_storage_strong_par_state;
  SequentialSubTasksDone _subtasks;
  PSOldGen* _old_gen;
  HeapWord* _gen_top;
  uint _active_workers;
  bool _is_empty;
  TaskTerminator _terminator;

public:
  ScavengeRootsTask(PSOldGen* old_gen,
                    HeapWord* gen_top,
                    uint active_workers,
                    bool is_empty) :
      AbstractGangTask("ScavengeRootsTask"),
      _strong_roots_scope(active_workers),
      _subtasks(ParallelRootType::sentinel),
      _old_gen(old_gen),
      _gen_top(gen_top),
      _active_workers(active_workers),
      _is_empty(is_empty),
      _terminator(active_workers, PSPromotionManager::vm_thread_promotion_manager()->stack_array_depth()) {
  }

  virtual void work(uint worker_id) {
    ResourceMark rm;

    if (!_is_empty) {
      // There are only old-to-young pointers if there are objects
      // in the old gen.

      assert(_old_gen != NULL, "Sanity");
      // There are no old-to-young pointers if the old gen is empty.
      assert(!_old_gen->object_space()->is_empty(), "Should not be called is there is no work");
      assert(_old_gen->object_space()->contains(_gen_top) || _gen_top == _old_gen->object_space()->top(), "Sanity");
      assert(worker_id < ParallelGCThreads, "Sanity");

      {
        PSPromotionManager* pm = PSPromotionManager::gc_thread_promotion_manager(worker_id);
        PSCardTable* card_table = ParallelScavengeHeap::heap()->card_table();

        card_table->scavenge_contents_parallel(_old_gen->start_array(),
                                               _old_gen->object_space(),
                                               _gen_top,
                                               pm,
                                               worker_id,
                                               _active_workers);

        // Do the real work
        pm->drain_stacks(false);
      }
    }

    for (uint root_type = 0; _subtasks.try_claim_task(root_type); /* empty */ ) {
      scavenge_roots_work(static_cast<ParallelRootType::Value>(root_type), worker_id);
    }

    PSThreadRootsTaskClosure closure(worker_id);
    Threads::possibly_parallel_threads_do(true /*parallel */, &closure);

    // Scavenge OopStorages
    {
      PSPromotionManager* pm = PSPromotionManager::gc_thread_promotion_manager(worker_id);
      PSScavengeRootsClosure closure(pm);
      _oop_storage_strong_par_state.oops_do(&closure);
      // Do the real work
      pm->drain_stacks(false);
    }

    // If active_workers can exceed 1, add a steal_work().
    // PSPromotionManager::drain_stacks_depth() does not fully drain its
    // stacks and expects a steal_work() to complete the draining if
    // ParallelGCThreads is > 1.

    if (_active_workers > 1) {
      steal_work(_terminator, worker_id);
    }
  }
};

// This method contains no policy. You should probably
// be calling invoke() instead.
bool PSScavenge::invoke_no_policy() {
  assert(SafepointSynchronize::is_at_safepoint(), "should be at safepoint");
  assert(Thread::current() == (Thread*)VMThread::vm_thread(), "should be in vm thread");

  _gc_timer.register_gc_start();

  TimeStamp scavenge_entry;
  TimeStamp scavenge_midpoint;
  TimeStamp scavenge_exit;

  scavenge_entry.update();

  if (GCLocker::check_active_before_gc()) {
    return false;
  }

  ParallelScavengeHeap* heap = ParallelScavengeHeap::heap();
  GCCause::Cause gc_cause = heap->gc_cause();

  // Check for potential problems.
  if (!should_attempt_scavenge()) {
    return false;
  }

  GCIdMark gc_id_mark;
  _gc_tracer.report_gc_start(heap->gc_cause(), _gc_timer.gc_start());

  bool promotion_failure_occurred = false;

  PSYoungGen* young_gen = heap->young_gen();
  PSOldGen* old_gen = heap->old_gen();
  PSAdaptiveSizePolicy* size_policy = heap->size_policy();

  heap->increment_total_collections();

  if (AdaptiveSizePolicy::should_update_eden_stats(gc_cause)) {
    // Gather the feedback data for eden occupancy.
    young_gen->eden_space()->accumulate_statistics();
  }

  heap->print_heap_before_gc();
  heap->trace_heap_before_gc(&_gc_tracer);

  assert(!NeverTenure || _tenuring_threshold == markWord::max_age + 1, "Sanity");
  assert(!AlwaysTenure || _tenuring_threshold == 0, "Sanity");

  // Fill in TLABs
  heap->ensure_parsability(true);  // retire TLABs

  if (VerifyBeforeGC && heap->total_collections() >= VerifyGCStartAt) {
    Universe::verify("Before GC");
  }

  {
    ResourceMark rm;

    GCTraceCPUTime tcpu;
    GCTraceTime(Info, gc) tm("Pause Young", NULL, gc_cause, true);
    TraceCollectorStats tcs(counters());
    TraceMemoryManagerStats tms(heap->young_gc_manager(), gc_cause);

    if (log_is_enabled(Debug, gc, heap, exit)) {
      accumulated_time()->start();
    }

    // Let the size policy know we're starting
    size_policy->minor_collection_begin();

    // Verify the object start arrays.
    if (VerifyObjectStartArray &&
        VerifyBeforeGC) {
      old_gen->verify_object_start_array();
    }

    // Verify no unmarked old->young roots
    if (VerifyRememberedSets) {
      heap->card_table()->verify_all_young_refs_imprecise();
    }

    assert(young_gen->to_space()->is_empty(),
           "Attempt to scavenge with live objects in to_space");
    young_gen->to_space()->clear(SpaceDecorator::Mangle);

    save_to_space_top_before_gc();

#if COMPILER2_OR_JVMCI
    DerivedPointerTable::clear();
#endif

    reference_processor()->start_discovery(false /* always_clear */);

    const PreGenGCValues pre_gc_values = heap->get_pre_gc_values();

    // Reset our survivor overflow.
    set_survivor_overflow(false);

    // We need to save the old top values before
    // creating the promotion_manager. We pass the top
    // values to the card_table, to prevent it from
    // straying into the promotion labs.
    HeapWord* old_top = old_gen->object_space()->top();

    const uint active_workers =
      WorkerPolicy::calc_active_workers(ParallelScavengeHeap::heap()->workers().total_workers(),
                                        ParallelScavengeHeap::heap()->workers().active_workers(),
                                        Threads::number_of_non_daemon_threads());
    ParallelScavengeHeap::heap()->workers().update_active_workers(active_workers);

    PSPromotionManager::pre_scavenge();

    // We'll use the promotion manager again later.
    PSPromotionManager* promotion_manager = PSPromotionManager::vm_thread_promotion_manager();
    {
      GCTraceTime(Debug, gc, phases) tm("Scavenge", &_gc_timer);

      ScavengeRootsTask task(old_gen, old_top, active_workers, old_gen->object_space()->is_empty());
      ParallelScavengeHeap::heap()->workers().run_task(&task);
    }

    scavenge_midpoint.update();

    // Process reference objects discovered during scavenge
    {
      GCTraceTime(Debug, gc, phases) tm("Reference Processing", &_gc_timer);

      reference_processor()->set_active_mt_degree(active_workers);
      ReferenceProcessorStats stats;
      ReferenceProcessorPhaseTimes pt(&_gc_timer, reference_processor()->max_num_queues());

      ParallelScavengeRefProcProxyTask task(reference_processor()->max_num_queues());
      stats = reference_processor()->process_discovered_references(task, pt);

      _gc_tracer.report_gc_reference_stats(stats);
      pt.print_all_references();
    }

    assert(promotion_manager->stacks_empty(),"stacks should be empty at this point");

    {
      GCTraceTime(Debug, gc, phases) tm("Weak Processing", &_gc_timer);
      PSAdjustWeakRootsClosure root_closure;
      WeakProcessor::weak_oops_do(&ParallelScavengeHeap::heap()->workers(), &_is_alive_closure, &root_closure, 1);
    }

    // Verify that usage of root_closure didn't copy any objects.
    assert(promotion_manager->stacks_empty(),"stacks should be empty at this point");

    // Finally, flush the promotion_manager's labs, and deallocate its stacks.
    promotion_failure_occurred = PSPromotionManager::post_scavenge(_gc_tracer);
    if (promotion_failure_occurred) {
      clean_up_failed_promotion();
      log_info(gc, promotion)("Promotion failed");
    }

    _gc_tracer.report_tenuring_threshold(tenuring_threshold());

    // Let the size policy know we're done.  Note that we count promotion
    // failure cleanup time as part of the collection (otherwise, we're
    // implicitly saying it's mutator time).
    size_policy->minor_collection_end(gc_cause);

    if (!promotion_failure_occurred) {
      // Swap the survivor spaces.
      young_gen->eden_space()->clear(SpaceDecorator::Mangle);
      young_gen->from_space()->clear(SpaceDecorator::Mangle);
      young_gen->swap_spaces();

      size_t survived = young_gen->from_space()->used_in_bytes();
      size_t promoted = old_gen->used_in_bytes() - pre_gc_values.old_gen_used();
      size_policy->update_averages(_survivor_overflow, survived, promoted);

      // A successful scavenge should restart the GC time limit count which is
      // for full GC's.
      size_policy->reset_gc_overhead_limit_count();
      if (UseAdaptiveSizePolicy) {
        // Calculate the new survivor size and tenuring threshold

        log_debug(gc, ergo)("AdaptiveSizeStart:  collection: %d ", heap->total_collections());
        log_trace(gc, ergo)("old_gen_capacity: " SIZE_FORMAT " young_gen_capacity: " SIZE_FORMAT,
                            old_gen->capacity_in_bytes(), young_gen->capacity_in_bytes());

        if (UsePerfData) {
          PSGCAdaptivePolicyCounters* counters = heap->gc_policy_counters();
          counters->update_old_eden_size(
            size_policy->calculated_eden_size_in_bytes());
          counters->update_old_promo_size(
            size_policy->calculated_promo_size_in_bytes());
          counters->update_old_capacity(old_gen->capacity_in_bytes());
          counters->update_young_capacity(young_gen->capacity_in_bytes());
          counters->update_survived(survived);
          counters->update_promoted(promoted);
          counters->update_survivor_overflowed(_survivor_overflow);
        }

        size_t max_young_size = young_gen->max_gen_size();

        // Deciding a free ratio in the young generation is tricky, so if
        // MinHeapFreeRatio or MaxHeapFreeRatio are in use (implicating
        // that the old generation size may have been limited because of them) we
        // should then limit our young generation size using NewRatio to have it
        // follow the old generation size.
        if (MinHeapFreeRatio != 0 || MaxHeapFreeRatio != 100) {
          max_young_size = MIN2(old_gen->capacity_in_bytes() / NewRatio,
                                young_gen->max_gen_size());
        }

        size_t survivor_limit =
          size_policy->max_survivor_size(max_young_size);
        _tenuring_threshold =
          size_policy->compute_survivor_space_size_and_threshold(
                                                           _survivor_overflow,
                                                           _tenuring_threshold,
                                                           survivor_limit);

       log_debug(gc, age)("Desired survivor size " SIZE_FORMAT " bytes, new threshold %u (max threshold " UINTX_FORMAT ")",
                          size_policy->calculated_survivor_size_in_bytes(),
                          _tenuring_threshold, MaxTenuringThreshold);

        if (UsePerfData) {
          PSGCAdaptivePolicyCounters* counters = heap->gc_policy_counters();
          counters->update_tenuring_threshold(_tenuring_threshold);
          counters->update_survivor_size_counters();
        }

        // Do call at minor collections?
        // Don't check if the size_policy is ready at this
        // level.  Let the size_policy check that internally.
        if (UseAdaptiveGenerationSizePolicyAtMinorCollection &&
            AdaptiveSizePolicy::should_update_eden_stats(gc_cause)) {
          // Calculate optimal free space amounts
          assert(young_gen->max_gen_size() >
                 young_gen->from_space()->capacity_in_bytes() +
                 young_gen->to_space()->capacity_in_bytes(),
                 "Sizes of space in young gen are out-of-bounds");

          size_t young_live = young_gen->used_in_bytes();
          size_t eden_live = young_gen->eden_space()->used_in_bytes();
          size_t cur_eden = young_gen->eden_space()->capacity_in_bytes();
          size_t max_old_gen_size = old_gen->max_gen_size();
          size_t max_eden_size = max_young_size -
            young_gen->from_space()->capacity_in_bytes() -
            young_gen->to_space()->capacity_in_bytes();

          // Used for diagnostics
          size_policy->clear_generation_free_space_flags();

          size_policy->compute_eden_space_size(young_live,
                                               eden_live,
                                               cur_eden,
                                               max_eden_size,
                                               false /* not full gc*/);

          size_policy->check_gc_overhead_limit(eden_live,
                                               max_old_gen_size,
                                               max_eden_size,
                                               false /* not full gc*/,
                                               gc_cause,
                                               heap->soft_ref_policy());

          size_policy->decay_supplemental_growth(false /* not full gc*/);
        }
        // Resize the young generation at every collection
        // even if new sizes have not been calculated.  This is
        // to allow resizes that may have been inhibited by the
        // relative location of the "to" and "from" spaces.

        // Resizing the old gen at young collections can cause increases
        // that don't feed back to the generation sizing policy until
        // a full collection.  Don't resize the old gen here.

        heap->resize_young_gen(size_policy->calculated_eden_size_in_bytes(),
                        size_policy->calculated_survivor_size_in_bytes());

        log_debug(gc, ergo)("AdaptiveSizeStop: collection: %d ", heap->total_collections());
      }

      // Update the structure of the eden. With NUMA-eden CPU hotplugging or offlining can
      // cause the change of the heap layout. Make sure eden is reshaped if that's the case.
      // Also update() will case adaptive NUMA chunk resizing.
      assert(young_gen->eden_space()->is_empty(), "eden space should be empty now");
      young_gen->eden_space()->update();

      heap->gc_policy_counters()->update_counters();

      heap->resize_all_tlabs();

      assert(young_gen->to_space()->is_empty(), "to space should be empty now");
    }

#if COMPILER2_OR_JVMCI
    DerivedPointerTable::update_pointers();
#endif

    NOT_PRODUCT(reference_processor()->verify_no_references_recorded());

    // Re-verify object start arrays
    if (VerifyObjectStartArray &&
        VerifyAfterGC) {
      old_gen->verify_object_start_array();
    }

    // Verify all old -> young cards are now precise
    if (VerifyRememberedSets) {
      // Precise verification will give false positives. Until this is fixed,
      // use imprecise verification.
      // heap->card_table()->verify_all_young_refs_precise();
      heap->card_table()->verify_all_young_refs_imprecise();
    }

    if (log_is_enabled(Debug, gc, heap, exit)) {
      accumulated_time()->stop();
    }

    heap->print_heap_change(pre_gc_values);

    // Track memory usage and detect low memory
    MemoryService::track_memory_usage();
    heap->update_counters();
  }

  if (VerifyAfterGC && heap->total_collections() >= VerifyGCStartAt) {
    Universe::verify("After GC");
  }

  heap->print_heap_after_gc();
  heap->trace_heap_after_gc(&_gc_tracer);

  scavenge_exit.update();

  log_debug(gc, task, time)("VM-Thread " JLONG_FORMAT " " JLONG_FORMAT " " JLONG_FORMAT,
                            scavenge_entry.ticks(), scavenge_midpoint.ticks(),
                            scavenge_exit.ticks());

  AdaptiveSizePolicyOutput::print(size_policy, heap->total_collections());

  _gc_timer.register_gc_end();

  _gc_tracer.report_gc_end(_gc_timer.gc_end(), _gc_timer.time_partitions());

  return !promotion_failure_occurred;
}

// This method iterates over all objects in the young generation,
// removing all forwarding references. It then restores any preserved marks.
void PSScavenge::clean_up_failed_promotion() {
  ParallelScavengeHeap* heap = ParallelScavengeHeap::heap();
  PSYoungGen* young_gen = heap->young_gen();

  RemoveForwardedPointerClosure remove_fwd_ptr_closure;
  young_gen->object_iterate(&remove_fwd_ptr_closure);

  PSPromotionManager::restore_preserved_marks();

  // Reset the PromotionFailureALot counters.
  NOT_PRODUCT(heap->reset_promotion_should_fail();)
}

bool PSScavenge::should_attempt_scavenge() {
  ParallelScavengeHeap* heap = ParallelScavengeHeap::heap();
  PSGCAdaptivePolicyCounters* counters = heap->gc_policy_counters();

  if (UsePerfData) {
    counters->update_scavenge_skipped(not_skipped);
  }

  PSYoungGen* young_gen = heap->young_gen();
  PSOldGen* old_gen = heap->old_gen();

  // Do not attempt to promote unless to_space is empty
  if (!young_gen->to_space()->is_empty()) {
    _consecutive_skipped_scavenges++;
    if (UsePerfData) {
      counters->update_scavenge_skipped(to_space_not_empty);
    }
    return false;
  }

  // Test to see if the scavenge will likely fail.
  PSAdaptiveSizePolicy* policy = heap->size_policy();

  // A similar test is done in the policy's should_full_GC().  If this is
  // changed, decide if that test should also be changed.
  size_t avg_promoted = (size_t) policy->padded_average_promoted_in_bytes();
  size_t promotion_estimate = MIN2(avg_promoted, young_gen->used_in_bytes());
  bool result = promotion_estimate < old_gen->free_in_bytes();

  log_trace(ergo)("%s scavenge: average_promoted " SIZE_FORMAT " padded_average_promoted " SIZE_FORMAT " free in old gen " SIZE_FORMAT,
                result ? "Do" : "Skip", (size_t) policy->average_promoted_in_bytes(),
                (size_t) policy->padded_average_promoted_in_bytes(),
                old_gen->free_in_bytes());
  if (young_gen->used_in_bytes() < (size_t) policy->padded_average_promoted_in_bytes()) {
    log_trace(ergo)(" padded_promoted_average is greater than maximum promotion = " SIZE_FORMAT, young_gen->used_in_bytes());
  }

  if (result) {
    _consecutive_skipped_scavenges = 0;
  } else {
    _consecutive_skipped_scavenges++;
    if (UsePerfData) {
      counters->update_scavenge_skipped(promoted_too_large);
    }
  }
  return result;
}

// Adaptive size policy support.
void PSScavenge::set_young_generation_boundary(HeapWord* v) {
  _young_generation_boundary = v;
  if (UseCompressedOops) {
    _young_generation_boundary_compressed = (uintptr_t)CompressedOops::encode(cast_to_oop(v));
  }
}

void PSScavenge::initialize() {
  // Arguments must have been parsed

  if (AlwaysTenure || NeverTenure) {
    assert(MaxTenuringThreshold == 0 || MaxTenuringThreshold == markWord::max_age + 1,
           "MaxTenuringThreshold should be 0 or markWord::max_age + 1, but is %d", (int) MaxTenuringThreshold);
    _tenuring_threshold = MaxTenuringThreshold;
  } else {
    // We want to smooth out our startup times for the AdaptiveSizePolicy
    _tenuring_threshold = (UseAdaptiveSizePolicy) ? InitialTenuringThreshold :
                                                    MaxTenuringThreshold;
  }

  ParallelScavengeHeap* heap = ParallelScavengeHeap::heap();
  PSYoungGen* young_gen = heap->young_gen();
  PSOldGen* old_gen = heap->old_gen();

  // Set boundary between young_gen and old_gen
  assert(old_gen->reserved().end() <= young_gen->eden_space()->bottom(),
         "old above young");
  set_young_generation_boundary(young_gen->eden_space()->bottom());

  // Initialize ref handling object for scavenging.
  _span_based_discoverer.set_span(young_gen->reserved());
  _ref_processor =
    new ReferenceProcessor(&_span_based_discoverer,
                           ParallelGCThreads,          // mt processing degree
                           true,                       // mt discovery
                           ParallelGCThreads,          // mt discovery degree
                           true,                       // atomic_discovery
                           NULL);                      // header provides liveness info

  // Cache the cardtable
  _card_table = heap->card_table();

  _counters = new CollectorCounters("Parallel young collection pauses", 0);
}
