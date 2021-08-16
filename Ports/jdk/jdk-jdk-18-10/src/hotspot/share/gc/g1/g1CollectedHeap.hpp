/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1COLLECTEDHEAP_HPP
#define SHARE_GC_G1_G1COLLECTEDHEAP_HPP

#include "gc/g1/g1BarrierSet.hpp"
#include "gc/g1/g1BiasedArray.hpp"
#include "gc/g1/g1CardSet.hpp"
#include "gc/g1/g1CardSetFreeMemoryTask.hpp"
#include "gc/g1/g1CardTable.hpp"
#include "gc/g1/g1CollectionSet.hpp"
#include "gc/g1/g1CollectorState.hpp"
#include "gc/g1/g1ConcurrentMark.hpp"
#include "gc/g1/g1EdenRegions.hpp"
#include "gc/g1/g1EvacFailure.hpp"
#include "gc/g1/g1EvacStats.hpp"
#include "gc/g1/g1EvacuationInfo.hpp"
#include "gc/g1/g1GCPhaseTimes.hpp"
#include "gc/g1/g1GCPauseType.hpp"
#include "gc/g1/g1HeapTransition.hpp"
#include "gc/g1/g1HeapVerifier.hpp"
#include "gc/g1/g1HRPrinter.hpp"
#include "gc/g1/g1HeapRegionAttr.hpp"
#include "gc/g1/g1MonitoringSupport.hpp"
#include "gc/g1/g1NUMA.hpp"
#include "gc/g1/g1RedirtyCardsQueue.hpp"
#include "gc/g1/g1SurvivorRegions.hpp"
#include "gc/g1/heapRegionManager.hpp"
#include "gc/g1/heapRegionSet.hpp"
#include "gc/shared/barrierSet.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/gcHeapSummary.hpp"
#include "gc/shared/plab.hpp"
#include "gc/shared/preservedMarks.hpp"
#include "gc/shared/softRefPolicy.hpp"
#include "gc/shared/taskqueue.hpp"
#include "memory/memRegion.hpp"
#include "utilities/bitMap.hpp"
#include "utilities/stack.hpp"

// A "G1CollectedHeap" is an implementation of a java heap for HotSpot.
// It uses the "Garbage First" heap organization and algorithm, which
// may combine concurrent marking with parallel, incremental compaction of
// heap subsets that will yield large amounts of garbage.

// Forward declarations
class HeapRegion;
class GenerationSpec;
class G1CardSetFreeMemoryTask;
class G1ParScanThreadState;
class G1ParScanThreadStateSet;
class G1ParScanThreadState;
class MemoryPool;
class MemoryManager;
class ObjectClosure;
class SpaceClosure;
class CompactibleSpaceClosure;
class Space;
class G1BatchedGangTask;
class G1CardTableEntryClosure;
class G1CollectionSet;
class G1GCCounters;
class G1Policy;
class G1HotCardCache;
class G1RemSet;
class G1ServiceTask;
class G1ServiceThread;
class G1ConcurrentMark;
class G1ConcurrentMarkThread;
class G1ConcurrentRefine;
class GenerationCounters;
class STWGCTimer;
class G1NewTracer;
class EvacuationFailedInfo;
class nmethod;
class WorkGang;
class G1Allocator;
class G1ArchiveAllocator;
class G1FullGCScope;
class G1HeapVerifier;
class G1HeapSizingPolicy;
class G1HeapSummary;
class G1EvacSummary;

typedef OverflowTaskQueue<ScannerTask, mtGC>           G1ScannerTasksQueue;
typedef GenericTaskQueueSet<G1ScannerTasksQueue, mtGC> G1ScannerTasksQueueSet;

typedef int RegionIdx_t;   // needs to hold [ 0..max_reserved_regions() )
typedef int CardIdx_t;     // needs to hold [ 0..CardsPerRegion )

// The G1 STW is alive closure.
// An instance is embedded into the G1CH and used as the
// (optional) _is_alive_non_header closure in the STW
// reference processor. It is also extensively used during
// reference processing during STW evacuation pauses.
class G1STWIsAliveClosure : public BoolObjectClosure {
  G1CollectedHeap* _g1h;
public:
  G1STWIsAliveClosure(G1CollectedHeap* g1h) : _g1h(g1h) {}
  bool do_object_b(oop p);
};

class G1STWSubjectToDiscoveryClosure : public BoolObjectClosure {
  G1CollectedHeap* _g1h;
public:
  G1STWSubjectToDiscoveryClosure(G1CollectedHeap* g1h) : _g1h(g1h) {}
  bool do_object_b(oop p);
};

class G1RegionMappingChangedListener : public G1MappingChangedListener {
 private:
  void reset_from_card_cache(uint start_idx, size_t num_regions);
 public:
  virtual void on_commit(uint start_idx, size_t num_regions, bool zero_filled);
};

class G1CollectedHeap : public CollectedHeap {
  friend class VM_CollectForMetadataAllocation;
  friend class VM_G1CollectForAllocation;
  friend class VM_G1CollectFull;
  friend class VM_G1TryInitiateConcMark;
  friend class VMStructs;
  friend class MutatorAllocRegion;
  friend class G1FullCollector;
  friend class G1GCAllocRegion;
  friend class G1HeapVerifier;

  friend class G1YoungGCVerifierMark;

  // Closures used in implementation.
  friend class G1ParScanThreadState;
  friend class G1ParScanThreadStateSet;
  friend class G1EvacuateRegionsTask;
  friend class G1PLABAllocator;

  // Other related classes.
  friend class G1HeapPrinterMark;
  friend class HeapRegionClaimer;

  // Testing classes.
  friend class G1CheckRegionAttrTableClosure;

private:
  G1ServiceThread* _service_thread;
  G1ServiceTask* _periodic_gc_task;
  G1CardSetFreeMemoryTask* _free_card_set_memory_task;

  WorkGang* _workers;
  G1CardTable* _card_table;

  Ticks _collection_pause_end;

  SoftRefPolicy      _soft_ref_policy;

  static size_t _humongous_object_threshold_in_words;

  // These sets keep track of old, archive and humongous regions respectively.
  HeapRegionSet _old_set;
  HeapRegionSet _archive_set;
  HeapRegionSet _humongous_set;

  // Young gen memory statistics before GC.
  G1CardSetMemoryStats _young_gen_card_set_stats;
  // Collection set candidates memory statistics after GC.
  G1CardSetMemoryStats _collection_set_candidates_card_set_stats;

  void rebuild_free_region_list();
  // Start a new incremental collection set for the next pause.
  void start_new_collection_set();

  // The block offset table for the G1 heap.
  G1BlockOffsetTable* _bot;

public:
  void prepare_region_for_full_compaction(HeapRegion* hr);

private:
  // Rebuilds the region sets / lists so that they are repopulated to
  // reflect the contents of the heap. The only exception is the
  // humongous set which was not torn down in the first place. If
  // free_list_only is true, it will only rebuild the free list.
  void rebuild_region_sets(bool free_list_only);

  // Callback for region mapping changed events.
  G1RegionMappingChangedListener _listener;

  // Handle G1 NUMA support.
  G1NUMA* _numa;

  // The sequence of all heap regions in the heap.
  HeapRegionManager _hrm;

  // Manages all allocations with regions except humongous object allocations.
  G1Allocator* _allocator;

  // Manages all heap verification.
  G1HeapVerifier* _verifier;

  // Outside of GC pauses, the number of bytes used in all regions other
  // than the current allocation region(s).
  volatile size_t _summary_bytes_used;

  void increase_used(size_t bytes);
  void decrease_used(size_t bytes);

  void set_used(size_t bytes);

  // Number of bytes used in all regions during GC. Typically changed when
  // retiring a GC alloc region.
  size_t _bytes_used_during_gc;

  // Class that handles archive allocation ranges.
  G1ArchiveAllocator* _archive_allocator;

  // GC allocation statistics policy for survivors.
  G1EvacStats _survivor_evac_stats;

  // GC allocation statistics policy for tenured objects.
  G1EvacStats _old_evac_stats;

  // It specifies whether we should attempt to expand the heap after a
  // region allocation failure. If heap expansion fails we set this to
  // false so that we don't re-attempt the heap expansion (it's likely
  // that subsequent expansion attempts will also fail if one fails).
  // Currently, it is only consulted during GC and it's reset at the
  // start of each GC.
  bool _expand_heap_after_alloc_failure;

  // Helper for monitoring and management support.
  G1MonitoringSupport* _monitoring_support;

  // Records whether the region at the given index is (still) a
  // candidate for eager reclaim.  Only valid for humongous start
  // regions; other regions have unspecified values.  Humongous start
  // regions are initialized at start of collection pause, with
  // candidates removed from the set as they are found reachable from
  // roots or the young generation.
  class HumongousReclaimCandidates : public G1BiasedMappedArray<bool> {
  protected:
    bool default_value() const { return false; }
  public:
    void clear() { G1BiasedMappedArray<bool>::clear(); }
    void set_candidate(uint region, bool value) {
      set_by_index(region, value);
    }
    bool is_candidate(uint region) {
      return get_by_index(region);
    }
  };

  HumongousReclaimCandidates _humongous_reclaim_candidates;
  uint _num_humongous_objects; // Current amount of (all) humongous objects found in the heap.
  uint _num_humongous_reclaim_candidates; // Number of humongous object eager reclaim candidates.
public:
  uint num_humongous_objects() const { return _num_humongous_objects; }
  uint num_humongous_reclaim_candidates() const { return _num_humongous_reclaim_candidates; }
  bool has_humongous_reclaim_candidates() const { return _num_humongous_reclaim_candidates > 0; }

  bool should_do_eager_reclaim() const;

  bool should_sample_collection_set_candidates() const;
  void set_collection_set_candidates_stats(G1CardSetMemoryStats& stats);

private:

  G1HRPrinter _hr_printer;

  // Return true if an explicit GC should start a concurrent cycle instead
  // of doing a STW full GC. A concurrent cycle should be started if:
  // (a) cause == _g1_humongous_allocation,
  // (b) cause == _java_lang_system_gc and +ExplicitGCInvokesConcurrent,
  // (c) cause == _dcmd_gc_run and +ExplicitGCInvokesConcurrent,
  // (d) cause == _wb_conc_mark or _wb_breakpoint,
  // (e) cause == _g1_periodic_collection and +G1PeriodicGCInvokesConcurrent.
  bool should_do_concurrent_full_gc(GCCause::Cause cause);

  // Attempt to start a concurrent cycle with the indicated cause.
  // precondition: should_do_concurrent_full_gc(cause)
  bool try_collect_concurrently(GCCause::Cause cause,
                                uint gc_counter,
                                uint old_marking_started_before);

  // indicates whether we are in young or mixed GC mode
  G1CollectorState _collector_state;

  // Keeps track of how many "old marking cycles" (i.e., Full GCs or
  // concurrent cycles) we have started.
  volatile uint _old_marking_cycles_started;

  // Keeps track of how many "old marking cycles" (i.e., Full GCs or
  // concurrent cycles) we have completed.
  volatile uint _old_marking_cycles_completed;

  // This is a non-product method that is helpful for testing. It is
  // called at the end of a GC and artificially expands the heap by
  // allocating a number of dead regions. This way we can induce very
  // frequent marking cycles and stress the cleanup / concurrent
  // cleanup code more (as all the regions that will be allocated by
  // this method will be found dead by the marking cycle).
  void allocate_dummy_regions() PRODUCT_RETURN;

  // Create a memory mapper for auxiliary data structures of the given size and
  // translation factor.
  static G1RegionToSpaceMapper* create_aux_memory_mapper(const char* description,
                                                         size_t size,
                                                         size_t translation_factor);

  void trace_heap(GCWhen::Type when, const GCTracer* tracer);

  // These are macros so that, if the assert fires, we get the correct
  // line number, file, etc.

#define heap_locking_asserts_params(_extra_message_)                          \
  "%s : Heap_lock locked: %s, at safepoint: %s, is VM thread: %s",            \
  (_extra_message_),                                                          \
  BOOL_TO_STR(Heap_lock->owned_by_self()),                                    \
  BOOL_TO_STR(SafepointSynchronize::is_at_safepoint()),                       \
  BOOL_TO_STR(Thread::current()->is_VM_thread())

#define assert_heap_locked()                                                  \
  do {                                                                        \
    assert(Heap_lock->owned_by_self(),                                        \
           heap_locking_asserts_params("should be holding the Heap_lock"));   \
  } while (0)

#define assert_heap_locked_or_at_safepoint(_should_be_vm_thread_)             \
  do {                                                                        \
    assert(Heap_lock->owned_by_self() ||                                      \
           (SafepointSynchronize::is_at_safepoint() &&                        \
             ((_should_be_vm_thread_) == Thread::current()->is_VM_thread())), \
           heap_locking_asserts_params("should be holding the Heap_lock or "  \
                                        "should be at a safepoint"));         \
  } while (0)

#define assert_heap_locked_and_not_at_safepoint()                             \
  do {                                                                        \
    assert(Heap_lock->owned_by_self() &&                                      \
                                    !SafepointSynchronize::is_at_safepoint(), \
          heap_locking_asserts_params("should be holding the Heap_lock and "  \
                                       "should not be at a safepoint"));      \
  } while (0)

#define assert_heap_not_locked()                                              \
  do {                                                                        \
    assert(!Heap_lock->owned_by_self(),                                       \
        heap_locking_asserts_params("should not be holding the Heap_lock"));  \
  } while (0)

#define assert_heap_not_locked_and_not_at_safepoint()                         \
  do {                                                                        \
    assert(!Heap_lock->owned_by_self() &&                                     \
                                    !SafepointSynchronize::is_at_safepoint(), \
      heap_locking_asserts_params("should not be holding the Heap_lock and "  \
                                   "should not be at a safepoint"));          \
  } while (0)

#define assert_at_safepoint_on_vm_thread()                                    \
  do {                                                                        \
    assert_at_safepoint();                                                    \
    assert(Thread::current_or_null() != NULL, "no current thread");           \
    assert(Thread::current()->is_VM_thread(), "current thread is not VM thread"); \
  } while (0)

#ifdef ASSERT
#define assert_used_and_recalculate_used_equal(g1h)                           \
  do {                                                                        \
    size_t cur_used_bytes = g1h->used();                                      \
    size_t recal_used_bytes = g1h->recalculate_used();                        \
    assert(cur_used_bytes == recal_used_bytes, "Used(" SIZE_FORMAT ") is not" \
           " same as recalculated used(" SIZE_FORMAT ").",                    \
           cur_used_bytes, recal_used_bytes);                                 \
  } while (0)
#else
#define assert_used_and_recalculate_used_equal(g1h) do {} while(0)
#endif

  // The young region list.
  G1EdenRegions _eden;
  G1SurvivorRegions _survivor;

  STWGCTimer* _gc_timer_stw;

  G1NewTracer* _gc_tracer_stw;

  // The current policy object for the collector.
  G1Policy* _policy;
  G1HeapSizingPolicy* _heap_sizing_policy;

  G1CollectionSet _collection_set;

  // Try to allocate a single non-humongous HeapRegion sufficient for
  // an allocation of the given word_size. If do_expand is true,
  // attempt to expand the heap if necessary to satisfy the allocation
  // request. 'type' takes the type of region to be allocated. (Use constants
  // Old, Eden, Humongous, Survivor defined in HeapRegionType.)
  HeapRegion* new_region(size_t word_size,
                         HeapRegionType type,
                         bool do_expand,
                         uint node_index = G1NUMA::AnyNodeIndex);

  // Initialize a contiguous set of free regions of length num_regions
  // and starting at index first so that they appear as a single
  // humongous region.
  HeapWord* humongous_obj_allocate_initialize_regions(HeapRegion* first_hr,
                                                      uint num_regions,
                                                      size_t word_size);

  // Attempt to allocate a humongous object of the given size. Return
  // NULL if unsuccessful.
  HeapWord* humongous_obj_allocate(size_t word_size);

  // The following two methods, allocate_new_tlab() and
  // mem_allocate(), are the two main entry points from the runtime
  // into the G1's allocation routines. They have the following
  // assumptions:
  //
  // * They should both be called outside safepoints.
  //
  // * They should both be called without holding the Heap_lock.
  //
  // * All allocation requests for new TLABs should go to
  //   allocate_new_tlab().
  //
  // * All non-TLAB allocation requests should go to mem_allocate().
  //
  // * If either call cannot satisfy the allocation request using the
  //   current allocating region, they will try to get a new one. If
  //   this fails, they will attempt to do an evacuation pause and
  //   retry the allocation.
  //
  // * If all allocation attempts fail, even after trying to schedule
  //   an evacuation pause, allocate_new_tlab() will return NULL,
  //   whereas mem_allocate() will attempt a heap expansion and/or
  //   schedule a Full GC.
  //
  // * We do not allow humongous-sized TLABs. So, allocate_new_tlab
  //   should never be called with word_size being humongous. All
  //   humongous allocation requests should go to mem_allocate() which
  //   will satisfy them with a special path.

  virtual HeapWord* allocate_new_tlab(size_t min_size,
                                      size_t requested_size,
                                      size_t* actual_size);

  virtual HeapWord* mem_allocate(size_t word_size,
                                 bool*  gc_overhead_limit_was_exceeded);

  // First-level mutator allocation attempt: try to allocate out of
  // the mutator alloc region without taking the Heap_lock. This
  // should only be used for non-humongous allocations.
  inline HeapWord* attempt_allocation(size_t min_word_size,
                                      size_t desired_word_size,
                                      size_t* actual_word_size);

  // Second-level mutator allocation attempt: take the Heap_lock and
  // retry the allocation attempt, potentially scheduling a GC
  // pause. This should only be used for non-humongous allocations.
  HeapWord* attempt_allocation_slow(size_t word_size);

  // Takes the Heap_lock and attempts a humongous allocation. It can
  // potentially schedule a GC pause.
  HeapWord* attempt_allocation_humongous(size_t word_size);

  // Allocation attempt that should be called during safepoints (e.g.,
  // at the end of a successful GC). expect_null_mutator_alloc_region
  // specifies whether the mutator alloc region is expected to be NULL
  // or not.
  HeapWord* attempt_allocation_at_safepoint(size_t word_size,
                                            bool expect_null_mutator_alloc_region);

  // These methods are the "callbacks" from the G1AllocRegion class.

  // For mutator alloc regions.
  HeapRegion* new_mutator_alloc_region(size_t word_size, bool force, uint node_index);
  void retire_mutator_alloc_region(HeapRegion* alloc_region,
                                   size_t allocated_bytes);

  // For GC alloc regions.
  bool has_more_regions(G1HeapRegionAttr dest);
  HeapRegion* new_gc_alloc_region(size_t word_size, G1HeapRegionAttr dest, uint node_index);
  void retire_gc_alloc_region(HeapRegion* alloc_region,
                              size_t allocated_bytes, G1HeapRegionAttr dest);

  // - if explicit_gc is true, the GC is for a System.gc() etc,
  //   otherwise it's for a failed allocation.
  // - if clear_all_soft_refs is true, all soft references should be
  //   cleared during the GC.
  // - if do_maximum_compaction is true, full gc will do a maximally
  //   compacting collection, leaving no dead wood.
  // - it returns false if it is unable to do the collection due to the
  //   GC locker being active, true otherwise.
  bool do_full_collection(bool explicit_gc,
                          bool clear_all_soft_refs,
                          bool do_maximum_compaction);

  // Callback from VM_G1CollectFull operation, or collect_as_vm_thread.
  virtual void do_full_collection(bool clear_all_soft_refs);

  // Helper to do a full collection that clears soft references.
  bool upgrade_to_full_collection();

  // Callback from VM_G1CollectForAllocation operation.
  // This function does everything necessary/possible to satisfy a
  // failed allocation request (including collection, expansion, etc.)
  HeapWord* satisfy_failed_allocation(size_t word_size,
                                      bool* succeeded);
  // Internal helpers used during full GC to split it up to
  // increase readability.
  void abort_concurrent_cycle();
  void verify_before_full_collection(bool explicit_gc);
  void prepare_heap_for_full_collection();
  void prepare_heap_for_mutators();
  void abort_refinement();
  void verify_after_full_collection();
  void print_heap_after_full_collection();

  // Helper method for satisfy_failed_allocation()
  HeapWord* satisfy_failed_allocation_helper(size_t word_size,
                                             bool do_gc,
                                             bool maximum_compaction,
                                             bool expect_null_mutator_alloc_region,
                                             bool* gc_succeeded);

  // Attempting to expand the heap sufficiently
  // to support an allocation of the given "word_size".  If
  // successful, perform the allocation and return the address of the
  // allocated block, or else "NULL".
  HeapWord* expand_and_allocate(size_t word_size);

  // Process any reference objects discovered.
  void process_discovered_references(G1ParScanThreadStateSet* per_thread_states);

  // If during a concurrent start pause we may install a pending list head which is not
  // otherwise reachable, ensure that it is marked in the bitmap for concurrent marking
  // to discover.
  void make_pending_list_reachable();

  void verify_numa_regions(const char* desc);

public:
  G1ServiceThread* service_thread() const { return _service_thread; }

  WorkGang* workers() const { return _workers; }

  // Runs the given AbstractGangTask with the current active workers,
  // returning the total time taken.
  Tickspan run_task_timed(AbstractGangTask* task);
  // Run the given batch task using the work gang.
  void run_batch_task(G1BatchedGangTask* cl);

  G1Allocator* allocator() {
    return _allocator;
  }

  G1HeapVerifier* verifier() {
    return _verifier;
  }

  G1MonitoringSupport* monitoring_support() {
    assert(_monitoring_support != nullptr, "should have been initialized");
    return _monitoring_support;
  }

  void resize_heap_if_necessary();

  // Check if there is memory to uncommit and if so schedule a task to do it.
  void uncommit_regions_if_necessary();
  // Immediately uncommit uncommittable regions.
  uint uncommit_regions(uint region_limit);
  bool has_uncommittable_regions();

  G1NUMA* numa() const { return _numa; }

  // Expand the garbage-first heap by at least the given size (in bytes!).
  // Returns true if the heap was expanded by the requested amount;
  // false otherwise.
  // (Rounds up to a HeapRegion boundary.)
  bool expand(size_t expand_bytes, WorkGang* pretouch_workers = NULL, double* expand_time_ms = NULL);
  bool expand_single_region(uint node_index);

  // Returns the PLAB statistics for a given destination.
  inline G1EvacStats* alloc_buffer_stats(G1HeapRegionAttr dest);

  // Determines PLAB size for a given destination.
  inline size_t desired_plab_sz(G1HeapRegionAttr dest);

  // Do anything common to GC's.
  void gc_prologue(bool full);
  void gc_epilogue(bool full);

  // Does the given region fulfill remembered set based eager reclaim candidate requirements?
  bool is_potential_eager_reclaim_candidate(HeapRegion* r) const;

  // Modify the reclaim candidate set and test for presence.
  // These are only valid for starts_humongous regions.
  inline void set_humongous_reclaim_candidate(uint region, bool value);
  inline bool is_humongous_reclaim_candidate(uint region);

  // Remove from the reclaim candidate set.  Also remove from the
  // collection set so that later encounters avoid the slow path.
  inline void set_humongous_is_live(oop obj);

  // Register the given region to be part of the collection set.
  inline void register_humongous_region_with_region_attr(uint index);

  // We register a region with the fast "in collection set" test. We
  // simply set to true the array slot corresponding to this region.
  void register_young_region_with_region_attr(HeapRegion* r) {
    _region_attr.set_in_young(r->hrm_index());
  }
  inline void register_region_with_region_attr(HeapRegion* r);
  inline void register_old_region_with_region_attr(HeapRegion* r);
  inline void register_optional_region_with_region_attr(HeapRegion* r);

  void clear_region_attr(const HeapRegion* hr) {
    _region_attr.clear(hr);
  }

  void clear_region_attr() {
    _region_attr.clear();
  }

  // Verify that the G1RegionAttr remset tracking corresponds to actual remset tracking
  // for all regions.
  void verify_region_attr_remset_update() PRODUCT_RETURN;

  bool is_user_requested_concurrent_full_gc(GCCause::Cause cause);

  // This is called at the start of either a concurrent cycle or a Full
  // GC to update the number of old marking cycles started.
  void increment_old_marking_cycles_started();

  // This is called at the end of either a concurrent cycle or a Full
  // GC to update the number of old marking cycles completed. Those two
  // can happen in a nested fashion, i.e., we start a concurrent
  // cycle, a Full GC happens half-way through it which ends first,
  // and then the cycle notices that a Full GC happened and ends
  // too. The concurrent parameter is a boolean to help us do a bit
  // tighter consistency checking in the method. If concurrent is
  // false, the caller is the inner caller in the nesting (i.e., the
  // Full GC). If concurrent is true, the caller is the outer caller
  // in this nesting (i.e., the concurrent cycle). Further nesting is
  // not currently supported. The end of this call also notifies
  // the G1OldGCCount_lock in case a Java thread is waiting for a full
  // GC to happen (e.g., it called System.gc() with
  // +ExplicitGCInvokesConcurrent).
  // whole_heap_examined should indicate that during that old marking
  // cycle the whole heap has been examined for live objects (as opposed
  // to only parts, or aborted before completion).
  void increment_old_marking_cycles_completed(bool concurrent, bool whole_heap_examined);

  uint old_marking_cycles_started() const {
    return _old_marking_cycles_started;
  }

  uint old_marking_cycles_completed() const {
    return _old_marking_cycles_completed;
  }

  G1HRPrinter* hr_printer() { return &_hr_printer; }

  // Allocates a new heap region instance.
  HeapRegion* new_heap_region(uint hrs_index, MemRegion mr);

  // Allocate the highest free region in the reserved heap. This will commit
  // regions as necessary.
  HeapRegion* alloc_highest_free_region();

  // Frees a region by resetting its metadata and adding it to the free list
  // passed as a parameter (this is usually a local list which will be appended
  // to the master free list later or NULL if free list management is handled
  // in another way).
  // Callers must ensure they are the only one calling free on the given region
  // at the same time.
  void free_region(HeapRegion* hr, FreeRegionList* free_list);

  // It dirties the cards that cover the block so that the post
  // write barrier never queues anything when updating objects on this
  // block. It is assumed (and in fact we assert) that the block
  // belongs to a young region.
  inline void dirty_young_block(HeapWord* start, size_t word_size);

  // Frees a humongous region by collapsing it into individual regions
  // and calling free_region() for each of them. The freed regions
  // will be added to the free list that's passed as a parameter (this
  // is usually a local list which will be appended to the master free
  // list later).
  // The method assumes that only a single thread is ever calling
  // this for a particular region at once.
  void free_humongous_region(HeapRegion* hr,
                             FreeRegionList* free_list);

  // Facility for allocating in 'archive' regions in high heap memory and
  // recording the allocated ranges. These should all be called from the
  // VM thread at safepoints, without the heap lock held. They can be used
  // to create and archive a set of heap regions which can be mapped at the
  // same fixed addresses in a subsequent JVM invocation.
  void begin_archive_alloc_range(bool open = false);

  // Check if the requested size would be too large for an archive allocation.
  bool is_archive_alloc_too_large(size_t word_size);

  // Allocate memory of the requested size from the archive region. This will
  // return NULL if the size is too large or if no memory is available. It
  // does not trigger a garbage collection.
  HeapWord* archive_mem_allocate(size_t word_size);

  // Optionally aligns the end address and returns the allocated ranges in
  // an array of MemRegions in order of ascending addresses.
  void end_archive_alloc_range(GrowableArray<MemRegion>* ranges,
                               size_t end_alignment_in_bytes = 0);

  // Facility for allocating a fixed range within the heap and marking
  // the containing regions as 'archive'. For use at JVM init time, when the
  // caller may mmap archived heap data at the specified range(s).
  // Verify that the MemRegions specified in the argument array are within the
  // reserved heap.
  bool check_archive_addresses(MemRegion* range, size_t count);

  // Commit the appropriate G1 regions containing the specified MemRegions
  // and mark them as 'archive' regions. The regions in the array must be
  // non-overlapping and in order of ascending address.
  bool alloc_archive_regions(MemRegion* range, size_t count, bool open);

  // Insert any required filler objects in the G1 regions around the specified
  // ranges to make the regions parseable. This must be called after
  // alloc_archive_regions, and after class loading has occurred.
  void fill_archive_regions(MemRegion* range, size_t count);

  // Populate the G1BlockOffsetTablePart for archived regions with the given
  // memory ranges.
  void populate_archive_regions_bot_part(MemRegion* range, size_t count);

  // For each of the specified MemRegions, uncommit the containing G1 regions
  // which had been allocated by alloc_archive_regions. This should be called
  // rather than fill_archive_regions at JVM init time if the archive file
  // mapping failed, with the same non-overlapping and sorted MemRegion array.
  void dealloc_archive_regions(MemRegion* range, size_t count);

private:

  // Shrink the garbage-first heap by at most the given size (in bytes!).
  // (Rounds down to a HeapRegion boundary.)
  void shrink(size_t shrink_bytes);
  void shrink_helper(size_t expand_bytes);

  #if TASKQUEUE_STATS
  static void print_taskqueue_stats_hdr(outputStream* const st);
  void print_taskqueue_stats() const;
  void reset_taskqueue_stats();
  #endif // TASKQUEUE_STATS

  // Start a concurrent cycle.
  void start_concurrent_cycle(bool concurrent_operation_is_full_mark);

  // Schedule the VM operation that will do an evacuation pause to
  // satisfy an allocation request of word_size. *succeeded will
  // return whether the VM operation was successful (it did do an
  // evacuation pause) or not (another thread beat us to it or the GC
  // locker was active). Given that we should not be holding the
  // Heap_lock when we enter this method, we will pass the
  // gc_count_before (i.e., total_collections()) as a parameter since
  // it has to be read while holding the Heap_lock. Currently, both
  // methods that call do_collection_pause() release the Heap_lock
  // before the call, so it's easy to read gc_count_before just before.
  HeapWord* do_collection_pause(size_t         word_size,
                                uint           gc_count_before,
                                bool*          succeeded,
                                GCCause::Cause gc_cause);

  void wait_for_root_region_scanning();

  // Perform an incremental collection at a safepoint, possibly
  // followed by a by-policy upgrade to a full collection.  Returns
  // false if unable to do the collection due to the GC locker being
  // active, true otherwise.
  // precondition: at safepoint on VM thread
  // precondition: !is_gc_active()
  bool do_collection_pause_at_safepoint(double target_pause_time_ms);

  // Helper for do_collection_pause_at_safepoint, containing the guts
  // of the incremental collection pause, executed by the vm thread.
  void do_collection_pause_at_safepoint_helper(double target_pause_time_ms);

  void set_young_collection_default_active_worker_threads();

  void prepare_tlabs_for_mutator();

  void retire_tlabs();

  G1HeapVerifier::G1VerifyType young_collection_verify_type() const;
  void verify_before_young_collection(G1HeapVerifier::G1VerifyType type);
  void verify_after_young_collection(G1HeapVerifier::G1VerifyType type);

  void calculate_collection_set(G1EvacuationInfo* evacuation_info, double target_pause_time_ms);

  // Actually do the work of evacuating the parts of the collection set.
  // The has_optional_evacuation_work flag for the initial collection set
  // evacuation indicates whether one or more optional evacuation steps may
  // follow.
  // If not set, G1 can avoid clearing the card tables of regions that we scan
  // for roots from the heap: when scanning the card table for dirty cards after
  // all remembered sets have been dumped onto it, for optional evacuation we
  // mark these cards as "Scanned" to know that we do not need to re-scan them
  // in the additional optional evacuation passes. This means that in the "Clear
  // Card Table" phase we need to clear those marks. However, if there is no
  // optional evacuation, g1 can immediately clean the dirty cards it encounters
  // as nobody else will be looking at them again, saving the clear card table
  // work later.
  // This case is very common (young only collections and most mixed gcs), so
  // depending on the ratio between scanned and evacuated regions (which g1 always
  // needs to clear), this is a big win.
  void evacuate_initial_collection_set(G1ParScanThreadStateSet* per_thread_states,
                                       bool has_optional_evacuation_work);
  void evacuate_optional_collection_set(G1ParScanThreadStateSet* per_thread_states);
private:
  // Evacuate the next set of optional regions.
  void evacuate_next_optional_regions(G1ParScanThreadStateSet* per_thread_states);

public:
  void pre_evacuate_collection_set(G1EvacuationInfo* evacuation_info, G1ParScanThreadStateSet* pss);
  void post_evacuate_collection_set(G1EvacuationInfo* evacuation_info,
                                    G1RedirtyCardsQueueSet* rdcqs,
                                    G1ParScanThreadStateSet* pss);

  void expand_heap_after_young_collection();
  // Update object copying statistics.
  void record_obj_copy_mem_stats();

  // The hot card cache for remembered set insertion optimization.
  G1HotCardCache* _hot_card_cache;

  // The g1 remembered set of the heap.
  G1RemSet* _rem_set;
  // Global card set configuration
  G1CardSetConfiguration _card_set_config;

  void post_evacuate_cleanup_1(G1ParScanThreadStateSet* per_thread_states,
                               G1RedirtyCardsQueueSet* rdcqs);
  void post_evacuate_cleanup_2(PreservedMarksSet* preserved_marks,
                               G1RedirtyCardsQueueSet* rdcqs,
                               G1EvacuationInfo* evacuation_info,
                               const size_t* surviving_young_words);

  // After a collection pause, reset eden and the collection set.
  void clear_eden();
  void clear_collection_set();

  // Abandon the current collection set without recording policy
  // statistics or updating free lists.
  void abandon_collection_set(G1CollectionSet* collection_set);

  // The concurrent marker (and the thread it runs in.)
  G1ConcurrentMark* _cm;
  G1ConcurrentMarkThread* _cm_thread;

  // The concurrent refiner.
  G1ConcurrentRefine* _cr;

  // The parallel task queues
  G1ScannerTasksQueueSet *_task_queues;

  // Number of regions evacuation failed in the current collection.
  volatile uint _num_regions_failed_evacuation;
  // Records for every region on the heap whether evacuation failed for it.
  CHeapBitMap _regions_failed_evacuation;

  EvacuationFailedInfo* _evacuation_failed_info_array;

  PreservedMarksSet _preserved_marks_set;

  // Preserve the mark of "obj", if necessary, in preparation for its mark
  // word being overwritten with a self-forwarding-pointer.
  void preserve_mark_during_evac_failure(uint worker_id, oop obj, markWord m);

#ifndef PRODUCT
  // Support for forcing evacuation failures. Analogous to
  // PromotionFailureALot for the other collectors.

  // Records whether G1EvacuationFailureALot should be in effect
  // for the current GC
  bool _evacuation_failure_alot_for_current_gc;

  // Used to record the GC number for interval checking when
  // determining whether G1EvaucationFailureALot is in effect
  // for the current GC.
  size_t _evacuation_failure_alot_gc_number;

  // Count of the number of evacuations between failures.
  volatile size_t _evacuation_failure_alot_count;

  // Set whether G1EvacuationFailureALot should be in effect
  // for the current GC (based upon the type of GC and which
  // command line flags are set);
  inline bool evacuation_failure_alot_for_gc_type(bool for_young_gc,
                                                  bool during_concurrent_start,
                                                  bool mark_or_rebuild_in_progress);

  inline void set_evacuation_failure_alot_for_current_gc();

  // Return true if it's time to cause an evacuation failure.
  inline bool evacuation_should_fail();

  // Reset the G1EvacuationFailureALot counters.  Should be called at
  // the end of an evacuation pause in which an evacuation failure occurred.
  inline void reset_evacuation_should_fail();
#endif // !PRODUCT

  // ("Weak") Reference processing support.
  //
  // G1 has 2 instances of the reference processor class.
  //
  // One (_ref_processor_cm) handles reference object discovery and subsequent
  // processing during concurrent marking cycles. Discovery is enabled/disabled
  // at the start/end of a concurrent marking cycle.
  //
  // The other (_ref_processor_stw) handles reference object discovery and
  // processing during incremental evacuation pauses and full GC pauses.
  //
  // ## Incremental evacuation pauses
  //
  // STW ref processor discovery is enabled/disabled at the start/end of an
  // incremental evacuation pause. No particular handling of the CM ref
  // processor is needed, apart from treating the discovered references as
  // roots; CM discovery does not need to be temporarily disabled as all
  // marking threads are paused during incremental evacuation pauses.
  //
  // ## Full GC pauses
  //
  // We abort any ongoing concurrent marking cycle, disable CM discovery, and
  // temporarily substitute a new closure for the STW ref processor's
  // _is_alive_non_header field (old value is restored after the full GC). Then
  // STW ref processor discovery is enabled, and marking & compaction
  // commences.

  // The (stw) reference processor...
  ReferenceProcessor* _ref_processor_stw;

  // During reference object discovery, the _is_alive_non_header
  // closure (if non-null) is applied to the referent object to
  // determine whether the referent is live. If so then the
  // reference object does not need to be 'discovered' and can
  // be treated as a regular oop. This has the benefit of reducing
  // the number of 'discovered' reference objects that need to
  // be processed.
  //
  // Instance of the is_alive closure for embedding into the
  // STW reference processor as the _is_alive_non_header field.
  // Supplying a value for the _is_alive_non_header field is
  // optional but doing so prevents unnecessary additions to
  // the discovered lists during reference discovery.
  G1STWIsAliveClosure _is_alive_closure_stw;

  G1STWSubjectToDiscoveryClosure _is_subject_to_discovery_stw;

  // The (concurrent marking) reference processor...
  ReferenceProcessor* _ref_processor_cm;

  // Instance of the concurrent mark is_alive closure for embedding
  // into the Concurrent Marking reference processor as the
  // _is_alive_non_header field. Supplying a value for the
  // _is_alive_non_header field is optional but doing so prevents
  // unnecessary additions to the discovered lists during reference
  // discovery.
  G1CMIsAliveClosure _is_alive_closure_cm;

  G1CMSubjectToDiscoveryClosure _is_subject_to_discovery_cm;
public:

  G1ScannerTasksQueue* task_queue(uint i) const;

  uint num_task_queues() const;

  // Create a G1CollectedHeap.
  // Must call the initialize method afterwards.
  // May not return if something goes wrong.
  G1CollectedHeap();

private:
  jint initialize_concurrent_refinement();
  jint initialize_service_thread();
public:
  // Initialize the G1CollectedHeap to have the initial and
  // maximum sizes and remembered and barrier sets
  // specified by the policy object.
  jint initialize();

  // Returns whether concurrent mark threads (and the VM) are about to terminate.
  bool concurrent_mark_is_terminating() const;

  virtual void stop();
  virtual void safepoint_synchronize_begin();
  virtual void safepoint_synchronize_end();

  // Does operations required after initialization has been done.
  void post_initialize();

  // Initialize weak reference processing.
  void ref_processing_init();

  virtual Name kind() const {
    return CollectedHeap::G1;
  }

  virtual const char* name() const {
    return "G1";
  }

  const G1CollectorState* collector_state() const { return &_collector_state; }
  G1CollectorState* collector_state() { return &_collector_state; }

  // The current policy object for the collector.
  G1Policy* policy() const { return _policy; }
  // The remembered set.
  G1RemSet* rem_set() const { return _rem_set; }

  inline G1GCPhaseTimes* phase_times() const;

  const G1CollectionSet* collection_set() const { return &_collection_set; }
  G1CollectionSet* collection_set() { return &_collection_set; }

  virtual SoftRefPolicy* soft_ref_policy();

  virtual void initialize_serviceability();
  virtual MemoryUsage memory_usage();
  virtual GrowableArray<GCMemoryManager*> memory_managers();
  virtual GrowableArray<MemoryPool*> memory_pools();

  // Try to minimize the remembered set.
  void scrub_rem_set();

  // Apply the given closure on all cards in the Hot Card Cache, emptying it.
  void iterate_hcc_closure(G1CardTableEntryClosure* cl, uint worker_id);

  // The shared block offset table array.
  G1BlockOffsetTable* bot() const { return _bot; }

  // Reference Processing accessors

  // The STW reference processor....
  ReferenceProcessor* ref_processor_stw() const { return _ref_processor_stw; }

  G1NewTracer* gc_tracer_stw() const { return _gc_tracer_stw; }

  // The Concurrent Marking reference processor...
  ReferenceProcessor* ref_processor_cm() const { return _ref_processor_cm; }

  size_t unused_committed_regions_in_bytes() const;

  virtual size_t capacity() const;
  virtual size_t used() const;
  // This should be called when we're not holding the heap lock. The
  // result might be a bit inaccurate.
  size_t used_unlocked() const;
  size_t recalculate_used() const;

  // These virtual functions do the actual allocation.
  // Some heaps may offer a contiguous region for shared non-blocking
  // allocation, via inlined code (by exporting the address of the top and
  // end fields defining the extent of the contiguous allocation region.)
  // But G1CollectedHeap doesn't yet support this.

  virtual bool is_maximal_no_gc() const {
    return _hrm.available() == 0;
  }

  // Returns true if an incremental GC should be upgrade to a full gc. This
  // is done when there are no free regions and the heap can't be expanded.
  bool should_upgrade_to_full_gc() const {
    return is_maximal_no_gc() && num_free_regions() == 0;
  }

  // The current number of regions in the heap.
  uint num_regions() const { return _hrm.length(); }

  // The max number of regions reserved for the heap. Except for static array
  // sizing purposes you probably want to use max_regions().
  uint max_reserved_regions() const { return _hrm.reserved_length(); }

  // Max number of regions that can be committed.
  uint max_regions() const { return _hrm.max_length(); }

  // The number of regions that are completely free.
  uint num_free_regions() const { return _hrm.num_free_regions(); }

  // The number of regions that can be allocated into.
  uint num_free_or_available_regions() const { return num_free_regions() + _hrm.available(); }

  MemoryUsage get_auxiliary_data_memory_usage() const {
    return _hrm.get_auxiliary_data_memory_usage();
  }

  // The number of regions that are not completely free.
  uint num_used_regions() const { return num_regions() - num_free_regions(); }

#ifdef ASSERT
  bool is_on_master_free_list(HeapRegion* hr) {
    return _hrm.is_free(hr);
  }
#endif // ASSERT

  inline void old_set_add(HeapRegion* hr);
  inline void old_set_remove(HeapRegion* hr);

  inline void archive_set_add(HeapRegion* hr);

  size_t non_young_capacity_bytes() {
    return (old_regions_count() + _archive_set.length() + humongous_regions_count()) * HeapRegion::GrainBytes;
  }

  // Determine whether the given region is one that we are using as an
  // old GC alloc region.
  bool is_old_gc_alloc_region(HeapRegion* hr);

  // Perform a collection of the heap; intended for use in implementing
  // "System.gc".  This probably implies as full a collection as the
  // "CollectedHeap" supports.
  virtual void collect(GCCause::Cause cause);

  // Perform a collection of the heap with the given cause.
  // Returns whether this collection actually executed.
  bool try_collect(GCCause::Cause cause, const G1GCCounters& counters_before);

  // True iff an evacuation has failed in the most-recent collection.
  inline bool evacuation_failed() const;
  // True iff the given region encountered an evacuation failure in the most-recent
  // collection.
  inline bool evacuation_failed(uint region_idx) const;

  inline uint num_regions_failed_evacuation() const;
  // Notify that the garbage collection encountered an evacuation failure in the
  // given region. Returns whether this has been the first occurrence of an evacuation
  // failure in that region.
  inline bool notify_region_failed_evacuation(uint const region_idx);

  void remove_from_old_gen_sets(const uint old_regions_removed,
                                const uint archive_regions_removed,
                                const uint humongous_regions_removed);
  void prepend_to_freelist(FreeRegionList* list);
  void decrement_summary_bytes(size_t bytes);

  virtual bool is_in(const void* p) const;

  // Return "TRUE" iff the given object address is within the collection
  // set. Assumes that the reference points into the heap.
  inline bool is_in_cset(const HeapRegion *hr);
  inline bool is_in_cset(oop obj);
  inline bool is_in_cset(HeapWord* addr);

  inline bool is_in_cset_or_humongous(const oop obj);

 private:
  // This array is used for a quick test on whether a reference points into
  // the collection set or not. Each of the array's elements denotes whether the
  // corresponding region is in the collection set or not.
  G1HeapRegionAttrBiasedMappedArray _region_attr;

 public:

  inline G1HeapRegionAttr region_attr(const void* obj) const;
  inline G1HeapRegionAttr region_attr(uint idx) const;

  MemRegion reserved() const {
    return _hrm.reserved();
  }

  bool is_in_reserved(const void* addr) const {
    return reserved().contains(addr);
  }

  G1HotCardCache* hot_card_cache() const { return _hot_card_cache; }

  G1CardTable* card_table() const {
    return _card_table;
  }

  // Iteration functions.

  void object_iterate_parallel(ObjectClosure* cl, uint worker_id, HeapRegionClaimer* claimer);

  // Iterate over all objects, calling "cl.do_object" on each.
  virtual void object_iterate(ObjectClosure* cl);

  virtual ParallelObjectIterator* parallel_object_iterator(uint thread_num);

  // Keep alive an object that was loaded with AS_NO_KEEPALIVE.
  virtual void keep_alive(oop obj);

  // Iterate over heap regions, in address order, terminating the
  // iteration early if the "do_heap_region" method returns "true".
  void heap_region_iterate(HeapRegionClosure* blk) const;

  // Return the region with the given index. It assumes the index is valid.
  inline HeapRegion* region_at(uint index) const;
  inline HeapRegion* region_at_or_null(uint index) const;

  // Return the next region (by index) that is part of the same
  // humongous object that hr is part of.
  inline HeapRegion* next_region_in_humongous(HeapRegion* hr) const;

  // Calculate the region index of the given address. Given address must be
  // within the heap.
  inline uint addr_to_region(HeapWord* addr) const;

  inline HeapWord* bottom_addr_for_region(uint index) const;

  // Two functions to iterate over the heap regions in parallel. Threads
  // compete using the HeapRegionClaimer to claim the regions before
  // applying the closure on them.
  // The _from_worker_offset version uses the HeapRegionClaimer and
  // the worker id to calculate a start offset to prevent all workers to
  // start from the point.
  void heap_region_par_iterate_from_worker_offset(HeapRegionClosure* cl,
                                                  HeapRegionClaimer* hrclaimer,
                                                  uint worker_id) const;

  void heap_region_par_iterate_from_start(HeapRegionClosure* cl,
                                          HeapRegionClaimer* hrclaimer) const;

  // Iterate over all regions in the collection set in parallel.
  void collection_set_par_iterate_all(HeapRegionClosure* cl,
                                      HeapRegionClaimer* hr_claimer,
                                      uint worker_id);

  // Iterate over all regions currently in the current collection set.
  void collection_set_iterate_all(HeapRegionClosure* blk);

  // Iterate over the regions in the current increment of the collection set.
  // Starts the iteration so that the start regions of a given worker id over the
  // set active_workers are evenly spread across the set of collection set regions
  // to be iterated.
  // The variant with the HeapRegionClaimer guarantees that the closure will be
  // applied to a particular region exactly once.
  void collection_set_iterate_increment_from(HeapRegionClosure *blk, uint worker_id) {
    collection_set_iterate_increment_from(blk, NULL, worker_id);
  }
  void collection_set_iterate_increment_from(HeapRegionClosure *blk, HeapRegionClaimer* hr_claimer, uint worker_id);

  // Returns the HeapRegion that contains addr. addr must not be NULL.
  template <class T>
  inline HeapRegion* heap_region_containing(const T addr) const;

  // Returns the HeapRegion that contains addr, or NULL if that is an uncommitted
  // region. addr must not be NULL.
  template <class T>
  inline HeapRegion* heap_region_containing_or_null(const T addr) const;

  // A CollectedHeap is divided into a dense sequence of "blocks"; that is,
  // each address in the (reserved) heap is a member of exactly
  // one block.  The defining characteristic of a block is that it is
  // possible to find its size, and thus to progress forward to the next
  // block.  (Blocks may be of different sizes.)  Thus, blocks may
  // represent Java objects, or they might be free blocks in a
  // free-list-based heap (or subheap), as long as the two kinds are
  // distinguishable and the size of each is determinable.

  // Returns the address of the start of the "block" that contains the
  // address "addr".  We say "blocks" instead of "object" since some heaps
  // may not pack objects densely; a chunk may either be an object or a
  // non-object.
  HeapWord* block_start(const void* addr) const;

  // Requires "addr" to be the start of a block, and returns "TRUE" iff
  // the block is an object.
  bool block_is_obj(const HeapWord* addr) const;

  // Section on thread-local allocation buffers (TLABs)
  // See CollectedHeap for semantics.

  size_t tlab_capacity(Thread* ignored) const;
  size_t tlab_used(Thread* ignored) const;
  size_t max_tlab_size() const;
  size_t unsafe_max_tlab_alloc(Thread* ignored) const;

  inline bool is_in_young(const oop obj);

  // Returns "true" iff the given word_size is "very large".
  static bool is_humongous(size_t word_size) {
    // Note this has to be strictly greater-than as the TLABs
    // are capped at the humongous threshold and we want to
    // ensure that we don't try to allocate a TLAB as
    // humongous and that we don't allocate a humongous
    // object in a TLAB.
    return word_size > _humongous_object_threshold_in_words;
  }

  // Returns the humongous threshold for a specific region size
  static size_t humongous_threshold_for(size_t region_size) {
    return (region_size / 2);
  }

  // Returns the number of regions the humongous object of the given word size
  // requires.
  static size_t humongous_obj_size_in_regions(size_t word_size);

  // Print the maximum heap capacity.
  virtual size_t max_capacity() const;

  Tickspan time_since_last_collection() const { return Ticks::now() - _collection_pause_end; }

  // Convenience function to be used in situations where the heap type can be
  // asserted to be this type.
  static G1CollectedHeap* heap() {
    return named_heap<G1CollectedHeap>(CollectedHeap::G1);
  }

  void set_region_short_lived_locked(HeapRegion* hr);
  // add appropriate methods for any other surv rate groups

  const G1SurvivorRegions* survivor() const { return &_survivor; }

  uint eden_regions_count() const { return _eden.length(); }
  uint eden_regions_count(uint node_index) const { return _eden.regions_on_node(node_index); }
  uint survivor_regions_count() const { return _survivor.length(); }
  uint survivor_regions_count(uint node_index) const { return _survivor.regions_on_node(node_index); }
  size_t eden_regions_used_bytes() const { return _eden.used_bytes(); }
  size_t survivor_regions_used_bytes() const { return _survivor.used_bytes(); }
  uint young_regions_count() const { return _eden.length() + _survivor.length(); }
  uint old_regions_count() const { return _old_set.length(); }
  uint archive_regions_count() const { return _archive_set.length(); }
  uint humongous_regions_count() const { return _humongous_set.length(); }

#ifdef ASSERT
  bool check_young_list_empty();
#endif

  bool is_marked_next(oop obj) const;

  // Determine if an object is dead, given the object and also
  // the region to which the object belongs.
  bool is_obj_dead(const oop obj, const HeapRegion* hr) const {
    return hr->is_obj_dead(obj, _cm->prev_mark_bitmap());
  }

  // This function returns true when an object has been
  // around since the previous marking and hasn't yet
  // been marked during this marking, and is not in a closed archive region.
  bool is_obj_ill(const oop obj, const HeapRegion* hr) const {
    return
      !hr->obj_allocated_since_next_marking(obj) &&
      !is_marked_next(obj) &&
      !hr->is_closed_archive();
  }

  // Determine if an object is dead, given only the object itself.
  // This will find the region to which the object belongs and
  // then call the region version of the same function.

  // Added if it is NULL it isn't dead.

  inline bool is_obj_dead(const oop obj) const;

  inline bool is_obj_ill(const oop obj) const;

  inline bool is_obj_dead_full(const oop obj, const HeapRegion* hr) const;
  inline bool is_obj_dead_full(const oop obj) const;

  G1ConcurrentMark* concurrent_mark() const { return _cm; }

  // Refinement

  G1ConcurrentRefine* concurrent_refine() const { return _cr; }

  // Optimized nmethod scanning support routines

  // Register the given nmethod with the G1 heap.
  virtual void register_nmethod(nmethod* nm);

  // Unregister the given nmethod from the G1 heap.
  virtual void unregister_nmethod(nmethod* nm);

  // No nmethod flushing needed.
  virtual void flush_nmethod(nmethod* nm) {}

  // No nmethod verification implemented.
  virtual void verify_nmethod(nmethod* nm) {}

  // Recalculate amount of used memory after GC. Must be called after all allocation
  // has finished.
  void update_used_after_gc();
  // Reset and re-enable the hot card cache.
  // Note the counts for the cards in the regions in the
  // collection set are reset when the collection set is freed.
  void reset_hot_card_cache();
  // Free up superfluous code root memory.
  void purge_code_root_memory();

  // Rebuild the strong code root lists for each region
  // after a full GC.
  void rebuild_strong_code_roots();

  // Performs cleaning of data structures after class unloading.
  void complete_cleaning(BoolObjectClosure* is_alive, bool class_unloading_occurred);

  // Verification

  // Perform any cleanup actions necessary before allowing a verification.
  virtual void prepare_for_verify();

  // Perform verification.

  // vo == UsePrevMarking -> use "prev" marking information,
  // vo == UseNextMarking -> use "next" marking information
  // vo == UseFullMarking -> use "next" marking bitmap but no TAMS
  //
  // NOTE: Only the "prev" marking information is guaranteed to be
  // consistent most of the time, so most calls to this should use
  // vo == UsePrevMarking.
  // Currently, there is only one case where this is called with
  // vo == UseNextMarking, which is to verify the "next" marking
  // information at the end of remark.
  // Currently there is only one place where this is called with
  // vo == UseFullMarking, which is to verify the marking during a
  // full GC.
  void verify(VerifyOption vo);

  // WhiteBox testing support.
  virtual bool supports_concurrent_gc_breakpoints() const;

  virtual WorkGang* safepoint_workers() { return _workers; }

  virtual bool is_archived_object(oop object) const;

  // The methods below are here for convenience and dispatch the
  // appropriate method depending on value of the given VerifyOption
  // parameter. The values for that parameter, and their meanings,
  // are the same as those above.

  bool is_obj_dead_cond(const oop obj,
                        const HeapRegion* hr,
                        const VerifyOption vo) const;

  bool is_obj_dead_cond(const oop obj,
                        const VerifyOption vo) const;

  G1HeapSummary create_g1_heap_summary();
  G1EvacSummary create_g1_evac_summary(G1EvacStats* stats);

  // Printing
private:
  void print_heap_regions() const;
  void print_regions_on(outputStream* st) const;

public:
  virtual void print_on(outputStream* st) const;
  virtual void print_extended_on(outputStream* st) const;
  virtual void print_on_error(outputStream* st) const;

  virtual void gc_threads_do(ThreadClosure* tc) const;

  // Override
  void print_tracing_info() const;

  // The following two methods are helpful for debugging RSet issues.
  void print_cset_rsets() PRODUCT_RETURN;
  void print_all_rsets() PRODUCT_RETURN;

  // Used to print information about locations in the hs_err file.
  virtual bool print_location(outputStream* st, void* addr) const;
};

// Scoped object that performs common pre- and post-gc heap printing operations.
class G1HeapPrinterMark : public StackObj {
  G1CollectedHeap* _g1h;
  G1HeapTransition _heap_transition;

public:
  G1HeapPrinterMark(G1CollectedHeap* g1h);
  ~G1HeapPrinterMark();
};

// Scoped object that performs common pre- and post-gc operations related to
// JFR events.
class G1JFRTracerMark : public StackObj {
protected:
  STWGCTimer* _timer;
  GCTracer* _tracer;

public:
  G1JFRTracerMark(STWGCTimer* timer, GCTracer* tracer);
  ~G1JFRTracerMark();
};

class G1ParEvacuateFollowersClosure : public VoidClosure {
private:
  double _start_term;
  double _term_time;
  size_t _term_attempts;

  void start_term_time() { _term_attempts++; _start_term = os::elapsedTime(); }
  void end_term_time() { _term_time += (os::elapsedTime() - _start_term); }
protected:
  G1CollectedHeap*              _g1h;
  G1ParScanThreadState*         _par_scan_state;
  G1ScannerTasksQueueSet*       _queues;
  TaskTerminator*               _terminator;
  G1GCPhaseTimes::GCParPhases   _phase;

  G1ParScanThreadState*   par_scan_state() { return _par_scan_state; }
  G1ScannerTasksQueueSet* queues()         { return _queues; }
  TaskTerminator*         terminator()     { return _terminator; }

public:
  G1ParEvacuateFollowersClosure(G1CollectedHeap* g1h,
                                G1ParScanThreadState* par_scan_state,
                                G1ScannerTasksQueueSet* queues,
                                TaskTerminator* terminator,
                                G1GCPhaseTimes::GCParPhases phase)
    : _start_term(0.0), _term_time(0.0), _term_attempts(0),
      _g1h(g1h), _par_scan_state(par_scan_state),
      _queues(queues), _terminator(terminator), _phase(phase) {}

  void do_void();

  double term_time() const { return _term_time; }
  size_t term_attempts() const { return _term_attempts; }

private:
  inline bool offer_termination();
};

#endif // SHARE_GC_G1_G1COLLECTEDHEAP_HPP
