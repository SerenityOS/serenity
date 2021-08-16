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

#ifndef SHARE_GC_PARALLEL_PARALLELSCAVENGEHEAP_HPP
#define SHARE_GC_PARALLEL_PARALLELSCAVENGEHEAP_HPP

#include "gc/parallel/objectStartArray.hpp"
#include "gc/parallel/psGCAdaptivePolicyCounters.hpp"
#include "gc/parallel/psOldGen.hpp"
#include "gc/parallel/psYoungGen.hpp"
#include "gc/shared/cardTableBarrierSet.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/gcPolicyCounters.hpp"
#include "gc/shared/gcWhen.hpp"
#include "gc/shared/preGCValues.hpp"
#include "gc/shared/referenceProcessor.hpp"
#include "gc/shared/softRefPolicy.hpp"
#include "gc/shared/strongRootsScope.hpp"
#include "gc/shared/workgroup.hpp"
#include "logging/log.hpp"
#include "utilities/growableArray.hpp"
#include "utilities/ostream.hpp"

class GCHeapSummary;
class HeapBlockClaimer;
class MemoryManager;
class MemoryPool;
class PSAdaptiveSizePolicy;
class PSCardTable;
class PSHeapSummary;

class ParallelScavengeHeap : public CollectedHeap {
  friend class VMStructs;
 private:
  static PSYoungGen* _young_gen;
  static PSOldGen*   _old_gen;

  // Sizing policy for entire heap
  static PSAdaptiveSizePolicy*       _size_policy;
  static PSGCAdaptivePolicyCounters* _gc_policy_counters;

  SoftRefPolicy _soft_ref_policy;

  unsigned int _death_march_count;

  GCMemoryManager* _young_manager;
  GCMemoryManager* _old_manager;

  MemoryPool* _eden_pool;
  MemoryPool* _survivor_pool;
  MemoryPool* _old_pool;

  WorkGang _workers;

  virtual void initialize_serviceability();

  void trace_actual_reserved_page_size(const size_t reserved_heap_size, const ReservedSpace rs);
  void trace_heap(GCWhen::Type when, const GCTracer* tracer);

  // Allocate in oldgen and record the allocation with the size_policy.
  HeapWord* allocate_old_gen_and_record(size_t word_size);

 protected:
  static inline size_t total_invocations();
  HeapWord* allocate_new_tlab(size_t min_size, size_t requested_size, size_t* actual_size);

  inline bool should_alloc_in_eden(size_t size) const;
  inline void death_march_check(HeapWord* const result, size_t size);
  HeapWord* mem_allocate_old_gen(size_t size);

 public:
  ParallelScavengeHeap() :
    CollectedHeap(),
    _death_march_count(0),
    _young_manager(NULL),
    _old_manager(NULL),
    _eden_pool(NULL),
    _survivor_pool(NULL),
    _old_pool(NULL),
    _workers("GC Thread",
             ParallelGCThreads,
             true /* are_GC_task_threads */,
             false /* are_ConcurrentGC_threads */) { }

  // For use by VM operations
  enum CollectionType {
    Scavenge,
    MarkSweep
  };

  virtual Name kind() const {
    return CollectedHeap::Parallel;
  }

  virtual const char* name() const {
    return "Parallel";
  }

  virtual SoftRefPolicy* soft_ref_policy() { return &_soft_ref_policy; }

  virtual GrowableArray<GCMemoryManager*> memory_managers();
  virtual GrowableArray<MemoryPool*> memory_pools();

  static PSYoungGen* young_gen() { return _young_gen; }
  static PSOldGen* old_gen()     { return _old_gen; }

  virtual PSAdaptiveSizePolicy* size_policy() { return _size_policy; }

  static PSGCAdaptivePolicyCounters* gc_policy_counters() { return _gc_policy_counters; }

  static ParallelScavengeHeap* heap() {
    return named_heap<ParallelScavengeHeap>(CollectedHeap::Parallel);
  }

  CardTableBarrierSet* barrier_set();
  PSCardTable* card_table();

  // Returns JNI_OK on success
  virtual jint initialize();

  void post_initialize();
  void update_counters();

  size_t capacity() const;
  size_t used() const;

  // Return "true" if all generations have reached the
  // maximal committed limit that they can reach, without a garbage
  // collection.
  virtual bool is_maximal_no_gc() const;

  virtual void register_nmethod(nmethod* nm);
  virtual void unregister_nmethod(nmethod* nm);
  virtual void verify_nmethod(nmethod* nm);
  virtual void flush_nmethod(nmethod* nm);

  void prune_scavengable_nmethods();

  size_t max_capacity() const;

  // Whether p is in the allocated part of the heap
  bool is_in(const void* p) const;

  bool is_in_reserved(const void* p) const;

  bool is_in_young(oop p);  // reserved part
  bool is_in_old(oop p);    // reserved part

  MemRegion reserved_region() const { return _reserved; }
  HeapWord* base() const { return _reserved.start(); }

  // Memory allocation.   "gc_time_limit_was_exceeded" will
  // be set to true if the adaptive size policy determine that
  // an excessive amount of time is being spent doing collections
  // and caused a NULL to be returned.  If a NULL is not returned,
  // "gc_time_limit_was_exceeded" has an undefined meaning.
  HeapWord* mem_allocate(size_t size, bool* gc_overhead_limit_was_exceeded);

  // Allocation attempt(s) during a safepoint. It should never be called
  // to allocate a new TLAB as this allocation might be satisfied out
  // of the old generation.
  HeapWord* failed_mem_allocate(size_t size);

  // Support for System.gc()
  void collect(GCCause::Cause cause);

  // These also should be called by the vm thread at a safepoint (e.g., from a
  // VM operation).
  //
  // The first collects the young generation only, unless the scavenge fails; it
  // will then attempt a full gc.  The second collects the entire heap; if
  // maximum_compaction is true, it will compact everything and clear all soft
  // references.
  inline void invoke_scavenge();

  // Perform a full collection
  virtual void do_full_collection(bool clear_all_soft_refs);

  bool supports_inline_contig_alloc() const { return !UseNUMA; }

  HeapWord* volatile* top_addr() const { return !UseNUMA ? young_gen()->top_addr() : (HeapWord* volatile*)-1; }
  HeapWord** end_addr() const { return !UseNUMA ? young_gen()->end_addr() : (HeapWord**)-1; }

  void ensure_parsability(bool retire_tlabs);
  void resize_all_tlabs();

  size_t tlab_capacity(Thread* thr) const;
  size_t tlab_used(Thread* thr) const;
  size_t unsafe_max_tlab_alloc(Thread* thr) const;

  void object_iterate(ObjectClosure* cl);
  void object_iterate_parallel(ObjectClosure* cl, HeapBlockClaimer* claimer);
  virtual ParallelObjectIterator* parallel_object_iterator(uint thread_num);

  HeapWord* block_start(const void* addr) const;
  bool block_is_obj(const HeapWord* addr) const;

  void prepare_for_verify();
  PSHeapSummary create_ps_heap_summary();
  virtual void print_on(outputStream* st) const;
  virtual void print_on_error(outputStream* st) const;
  virtual void gc_threads_do(ThreadClosure* tc) const;
  virtual void print_tracing_info() const;

  virtual WorkGang* safepoint_workers() { return &_workers; }

  PreGenGCValues get_pre_gc_values() const;
  void print_heap_change(const PreGenGCValues& pre_gc_values) const;

  // Used to print information about locations in the hs_err file.
  virtual bool print_location(outputStream* st, void* addr) const;

  void verify(VerifyOption option /* ignored */);

  // Resize the young generation.  The reserved space for the
  // generation may be expanded in preparation for the resize.
  void resize_young_gen(size_t eden_size, size_t survivor_size);

  // Resize the old generation.  The reserved space for the
  // generation may be expanded in preparation for the resize.
  void resize_old_gen(size_t desired_free_space);

  // Save the tops of the spaces in all generations
  void record_gen_tops_before_GC() PRODUCT_RETURN;

  // Mangle the unused parts of all spaces in the heap
  void gen_mangle_unused_area() PRODUCT_RETURN;

  GCMemoryManager* old_gc_manager() const { return _old_manager; }
  GCMemoryManager* young_gc_manager() const { return _young_manager; }

  WorkGang& workers() {
    return _workers;
  }
};

// Class that can be used to print information about the
// adaptive size policy at intervals specified by
// AdaptiveSizePolicyOutputInterval.  Only print information
// if an adaptive size policy is in use.
class AdaptiveSizePolicyOutput : AllStatic {
  static bool enabled() {
    return UseParallelGC &&
           UseAdaptiveSizePolicy &&
           log_is_enabled(Debug, gc, ergo);
  }
 public:
  static void print() {
    if (enabled()) {
      ParallelScavengeHeap::heap()->size_policy()->print();
    }
  }

  static void print(AdaptiveSizePolicy* size_policy, uint count) {
    bool do_print =
        enabled() &&
        (AdaptiveSizePolicyOutputInterval > 0) &&
        (count % AdaptiveSizePolicyOutputInterval) == 0;

    if (do_print) {
      size_policy->print();
    }
  }
};

#endif // SHARE_GC_PARALLEL_PARALLELSCAVENGEHEAP_HPP
