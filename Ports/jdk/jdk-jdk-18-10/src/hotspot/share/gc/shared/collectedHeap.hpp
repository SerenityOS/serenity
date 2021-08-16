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

#ifndef SHARE_GC_SHARED_COLLECTEDHEAP_HPP
#define SHARE_GC_SHARED_COLLECTEDHEAP_HPP

#include "gc/shared/gcCause.hpp"
#include "gc/shared/gcWhen.hpp"
#include "gc/shared/verifyOption.hpp"
#include "memory/allocation.hpp"
#include "memory/metaspace.hpp"
#include "memory/universe.hpp"
#include "runtime/handles.hpp"
#include "runtime/perfDataTypes.hpp"
#include "runtime/safepoint.hpp"
#include "services/memoryUsage.hpp"
#include "utilities/debug.hpp"
#include "utilities/formatBuffer.hpp"
#include "utilities/growableArray.hpp"

// A "CollectedHeap" is an implementation of a java heap for HotSpot.  This
// is an abstract class: there may be many different kinds of heaps.  This
// class defines the functions that a heap must implement, and contains
// infrastructure common to all heaps.

class AbstractGangTask;
class AdaptiveSizePolicy;
class BarrierSet;
class GCHeapLog;
class GCHeapSummary;
class GCTimer;
class GCTracer;
class GCMemoryManager;
class MemoryPool;
class MetaspaceSummary;
class ReservedHeapSpace;
class SoftRefPolicy;
class Thread;
class ThreadClosure;
class VirtualSpaceSummary;
class WorkGang;
class nmethod;

class ParallelObjectIterator : public CHeapObj<mtGC> {
public:
  virtual void object_iterate(ObjectClosure* cl, uint worker_id) = 0;
  virtual ~ParallelObjectIterator() {}
};

//
// CollectedHeap
//   GenCollectedHeap
//     SerialHeap
//   G1CollectedHeap
//   ParallelScavengeHeap
//   ShenandoahHeap
//   ZCollectedHeap
//
class CollectedHeap : public CHeapObj<mtInternal> {
  friend class VMStructs;
  friend class JVMCIVMStructs;
  friend class IsGCActiveMark; // Block structured external access to _is_gc_active
  friend class MemAllocator;

 private:
  GCHeapLog* _gc_heap_log;

  // Historic gc information
  size_t _capacity_at_last_gc;
  size_t _used_at_last_gc;

 protected:
  // Not used by all GCs
  MemRegion _reserved;

  bool _is_gc_active;

  // Used for filler objects (static, but initialized in ctor).
  static size_t _filler_array_max_size;

  // Last time the whole heap has been examined in support of RMI
  // MaxObjectInspectionAge.
  // This timestamp must be monotonically non-decreasing to avoid
  // time-warp warnings.
  jlong _last_whole_heap_examined_time_ns;

  unsigned int _total_collections;          // ... started
  unsigned int _total_full_collections;     // ... started
  NOT_PRODUCT(volatile size_t _promotion_failure_alot_count;)
  NOT_PRODUCT(volatile size_t _promotion_failure_alot_gc_number;)

  // Reason for current garbage collection.  Should be set to
  // a value reflecting no collection between collections.
  GCCause::Cause _gc_cause;
  GCCause::Cause _gc_lastcause;
  PerfStringVariable* _perf_gc_cause;
  PerfStringVariable* _perf_gc_lastcause;

  // Constructor
  CollectedHeap();

  // Create a new tlab. All TLAB allocations must go through this.
  // To allow more flexible TLAB allocations min_size specifies
  // the minimum size needed, while requested_size is the requested
  // size based on ergonomics. The actually allocated size will be
  // returned in actual_size.
  virtual HeapWord* allocate_new_tlab(size_t min_size,
                                      size_t requested_size,
                                      size_t* actual_size);

  // Reinitialize tlabs before resuming mutators.
  virtual void resize_all_tlabs();

  // Raw memory allocation facilities
  // The obj and array allocate methods are covers for these methods.
  // mem_allocate() should never be
  // called to allocate TLABs, only individual objects.
  virtual HeapWord* mem_allocate(size_t size,
                                 bool* gc_overhead_limit_was_exceeded) = 0;

  // Filler object utilities.
  static inline size_t filler_array_hdr_size();
  static inline size_t filler_array_min_size();

  DEBUG_ONLY(static void fill_args_check(HeapWord* start, size_t words);)
  DEBUG_ONLY(static void zap_filler_array(HeapWord* start, size_t words, bool zap = true);)

  // Fill with a single array; caller must ensure filler_array_min_size() <=
  // words <= filler_array_max_size().
  static inline void fill_with_array(HeapWord* start, size_t words, bool zap = true);

  // Fill with a single object (either an int array or a java.lang.Object).
  static inline void fill_with_object_impl(HeapWord* start, size_t words, bool zap = true);

  virtual void trace_heap(GCWhen::Type when, const GCTracer* tracer);

  // Verification functions
  virtual void check_for_non_bad_heap_word_value(HeapWord* addr, size_t size)
    PRODUCT_RETURN;
  debug_only(static void check_for_valid_allocation_state();)

 public:
  enum Name {
    None,
    Serial,
    Parallel,
    G1,
    Epsilon,
    Z,
    Shenandoah
  };

 protected:
  // Get a pointer to the derived heap object.  Used to implement
  // derived class heap() functions rather than being called directly.
  template<typename T>
  static T* named_heap(Name kind) {
    CollectedHeap* heap = Universe::heap();
    assert(heap != NULL, "Uninitialized heap");
    assert(kind == heap->kind(), "Heap kind %u should be %u",
           static_cast<uint>(heap->kind()), static_cast<uint>(kind));
    return static_cast<T*>(heap);
  }

 public:

  static inline size_t filler_array_max_size() {
    return _filler_array_max_size;
  }

  virtual Name kind() const = 0;

  virtual const char* name() const = 0;

  /**
   * Returns JNI error code JNI_ENOMEM if memory could not be allocated,
   * and JNI_OK on success.
   */
  virtual jint initialize() = 0;

  // In many heaps, there will be a need to perform some initialization activities
  // after the Universe is fully formed, but before general heap allocation is allowed.
  // This is the correct place to place such initialization methods.
  virtual void post_initialize();

  // Stop any onging concurrent work and prepare for exit.
  virtual void stop() {}

  // Stop and resume concurrent GC threads interfering with safepoint operations
  virtual void safepoint_synchronize_begin() {}
  virtual void safepoint_synchronize_end() {}

  void initialize_reserved_region(const ReservedHeapSpace& rs);

  virtual size_t capacity() const = 0;
  virtual size_t used() const = 0;

  // Returns unused capacity.
  virtual size_t unused() const;

  // Historic gc information
  size_t free_at_last_gc() const { return _capacity_at_last_gc - _used_at_last_gc; }
  size_t used_at_last_gc() const { return _used_at_last_gc; }
  void update_capacity_and_used_at_gc();

  // Return "true" if the part of the heap that allocates Java
  // objects has reached the maximal committed limit that it can
  // reach, without a garbage collection.
  virtual bool is_maximal_no_gc() const = 0;

  // Support for java.lang.Runtime.maxMemory():  return the maximum amount of
  // memory that the vm could make available for storing 'normal' java objects.
  // This is based on the reserved address space, but should not include space
  // that the vm uses internally for bookkeeping or temporary storage
  // (e.g., in the case of the young gen, one of the survivor
  // spaces).
  virtual size_t max_capacity() const = 0;

  // Returns "TRUE" iff "p" points into the committed areas of the heap.
  // This method can be expensive so avoid using it in performance critical
  // code.
  virtual bool is_in(const void* p) const = 0;

  DEBUG_ONLY(bool is_in_or_null(const void* p) const { return p == NULL || is_in(p); })

  virtual uint32_t hash_oop(oop obj) const;

  void set_gc_cause(GCCause::Cause v);
  GCCause::Cause gc_cause() { return _gc_cause; }

  oop obj_allocate(Klass* klass, int size, TRAPS);
  virtual oop array_allocate(Klass* klass, int size, int length, bool do_zero, TRAPS);
  oop class_allocate(Klass* klass, int size, TRAPS);

  // Utilities for turning raw memory into filler objects.
  //
  // min_fill_size() is the smallest region that can be filled.
  // fill_with_objects() can fill arbitrary-sized regions of the heap using
  // multiple objects.  fill_with_object() is for regions known to be smaller
  // than the largest array of integers; it uses a single object to fill the
  // region and has slightly less overhead.
  static size_t min_fill_size() {
    return size_t(align_object_size(oopDesc::header_size()));
  }

  static void fill_with_objects(HeapWord* start, size_t words, bool zap = true);

  static void fill_with_object(HeapWord* start, size_t words, bool zap = true);
  static void fill_with_object(MemRegion region, bool zap = true) {
    fill_with_object(region.start(), region.word_size(), zap);
  }
  static void fill_with_object(HeapWord* start, HeapWord* end, bool zap = true) {
    fill_with_object(start, pointer_delta(end, start), zap);
  }

  virtual void fill_with_dummy_object(HeapWord* start, HeapWord* end, bool zap);
  virtual size_t min_dummy_object_size() const;
  size_t tlab_alloc_reserve() const;

  // Some heaps may offer a contiguous region for shared non-blocking
  // allocation, via inlined code (by exporting the address of the top and
  // end fields defining the extent of the contiguous allocation region.)

  // This function returns "true" iff the heap supports this kind of
  // allocation.  (Default is "no".)
  virtual bool supports_inline_contig_alloc() const {
    return false;
  }
  // These functions return the addresses of the fields that define the
  // boundaries of the contiguous allocation area.  (These fields should be
  // physically near to one another.)
  virtual HeapWord* volatile* top_addr() const {
    guarantee(false, "inline contiguous allocation not supported");
    return NULL;
  }
  virtual HeapWord** end_addr() const {
    guarantee(false, "inline contiguous allocation not supported");
    return NULL;
  }

  // Some heaps may be in an unparseable state at certain times between
  // collections. This may be necessary for efficient implementation of
  // certain allocation-related activities. Calling this function before
  // attempting to parse a heap ensures that the heap is in a parsable
  // state (provided other concurrent activity does not introduce
  // unparsability). It is normally expected, therefore, that this
  // method is invoked with the world stopped.
  // NOTE: if you override this method, make sure you call
  // super::ensure_parsability so that the non-generational
  // part of the work gets done. See implementation of
  // CollectedHeap::ensure_parsability and, for instance,
  // that of GenCollectedHeap::ensure_parsability().
  // The argument "retire_tlabs" controls whether existing TLABs
  // are merely filled or also retired, thus preventing further
  // allocation from them and necessitating allocation of new TLABs.
  virtual void ensure_parsability(bool retire_tlabs);

  // The amount of space available for thread-local allocation buffers.
  virtual size_t tlab_capacity(Thread *thr) const = 0;

  // The amount of used space for thread-local allocation buffers for the given thread.
  virtual size_t tlab_used(Thread *thr) const = 0;

  virtual size_t max_tlab_size() const;

  // An estimate of the maximum allocation that could be performed
  // for thread-local allocation buffers without triggering any
  // collection or expansion activity.
  virtual size_t unsafe_max_tlab_alloc(Thread *thr) const {
    guarantee(false, "thread-local allocation buffers not supported");
    return 0;
  }

  // If a GC uses a stack watermark barrier, the stack processing is lazy, concurrent,
  // incremental and cooperative. In order for that to work well, mechanisms that stop
  // another thread might want to ensure its roots are in a sane state.
  virtual bool uses_stack_watermark_barrier() const { return false; }

  // Perform a collection of the heap; intended for use in implementing
  // "System.gc".  This probably implies as full a collection as the
  // "CollectedHeap" supports.
  virtual void collect(GCCause::Cause cause) = 0;

  // Perform a full collection
  virtual void do_full_collection(bool clear_all_soft_refs) = 0;

  // This interface assumes that it's being called by the
  // vm thread. It collects the heap assuming that the
  // heap lock is already held and that we are executing in
  // the context of the vm thread.
  virtual void collect_as_vm_thread(GCCause::Cause cause);

  virtual MetaWord* satisfy_failed_metadata_allocation(ClassLoaderData* loader_data,
                                                       size_t size,
                                                       Metaspace::MetadataType mdtype);

  // Returns "true" iff there is a stop-world GC in progress.  (I assume
  // that it should answer "false" for the concurrent part of a concurrent
  // collector -- dld).
  bool is_gc_active() const { return _is_gc_active; }

  // Total number of GC collections (started)
  unsigned int total_collections() const { return _total_collections; }
  unsigned int total_full_collections() const { return _total_full_collections;}

  // Increment total number of GC collections (started)
  void increment_total_collections(bool full = false) {
    _total_collections++;
    if (full) {
      increment_total_full_collections();
    }
  }

  void increment_total_full_collections() { _total_full_collections++; }

  // Return the SoftRefPolicy for the heap;
  virtual SoftRefPolicy* soft_ref_policy() = 0;

  virtual MemoryUsage memory_usage();
  virtual GrowableArray<GCMemoryManager*> memory_managers() = 0;
  virtual GrowableArray<MemoryPool*> memory_pools() = 0;

  // Iterate over all objects, calling "cl.do_object" on each.
  virtual void object_iterate(ObjectClosure* cl) = 0;

  virtual ParallelObjectIterator* parallel_object_iterator(uint thread_num) {
    return NULL;
  }

  // Keep alive an object that was loaded with AS_NO_KEEPALIVE.
  virtual void keep_alive(oop obj) {}

  // Perform any cleanup actions necessary before allowing a verification.
  virtual void prepare_for_verify() = 0;

  // Returns the longest time (in ms) that has elapsed since the last
  // time that the whole heap has been examined by a garbage collection.
  jlong millis_since_last_whole_heap_examined();
  // GC should call this when the next whole heap analysis has completed to
  // satisfy above requirement.
  void record_whole_heap_examined_timestamp();

 private:
  // Generate any dumps preceding or following a full gc
  void full_gc_dump(GCTimer* timer, bool before);

  virtual void initialize_serviceability() = 0;

 public:
  void pre_full_gc_dump(GCTimer* timer);
  void post_full_gc_dump(GCTimer* timer);

  virtual VirtualSpaceSummary create_heap_space_summary();
  GCHeapSummary create_heap_summary();

  MetaspaceSummary create_metaspace_summary();

  // Print heap information on the given outputStream.
  virtual void print_on(outputStream* st) const = 0;
  // The default behavior is to call print_on() on tty.
  virtual void print() const;

  // Print more detailed heap information on the given
  // outputStream. The default behavior is to call print_on(). It is
  // up to each subclass to override it and add any additional output
  // it needs.
  virtual void print_extended_on(outputStream* st) const {
    print_on(st);
  }

  virtual void print_on_error(outputStream* st) const;

  // Used to print information about locations in the hs_err file.
  virtual bool print_location(outputStream* st, void* addr) const = 0;

  // Iterator for all GC threads (other than VM thread)
  virtual void gc_threads_do(ThreadClosure* tc) const = 0;

  // Print any relevant tracing info that flags imply.
  // Default implementation does nothing.
  virtual void print_tracing_info() const = 0;

  void print_heap_before_gc();
  void print_heap_after_gc();

  // Registering and unregistering an nmethod (compiled code) with the heap.
  virtual void register_nmethod(nmethod* nm) = 0;
  virtual void unregister_nmethod(nmethod* nm) = 0;
  // Callback for when nmethod is about to be deleted.
  virtual void flush_nmethod(nmethod* nm) = 0;
  virtual void verify_nmethod(nmethod* nm) = 0;

  void trace_heap_before_gc(const GCTracer* gc_tracer);
  void trace_heap_after_gc(const GCTracer* gc_tracer);

  // Heap verification
  virtual void verify(VerifyOption option) = 0;

  // Return true if concurrent gc control via WhiteBox is supported by
  // this collector.  The default implementation returns false.
  virtual bool supports_concurrent_gc_breakpoints() const;

  // Provides a thread pool to SafepointSynchronize to use
  // for parallel safepoint cleanup.
  // GCs that use a GC worker thread pool may want to share
  // it for use during safepoint cleanup. This is only possible
  // if the GC can pause and resume concurrent work (e.g. G1
  // concurrent marking) for an intermittent non-GC safepoint.
  // If this method returns NULL, SafepointSynchronize will
  // perform cleanup tasks serially in the VMThread.
  virtual WorkGang* safepoint_workers() { return NULL; }

  // Support for object pinning. This is used by JNI Get*Critical()
  // and Release*Critical() family of functions. If supported, the GC
  // must guarantee that pinned objects never move.
  virtual bool supports_object_pinning() const;
  virtual oop pin_object(JavaThread* thread, oop obj);
  virtual void unpin_object(JavaThread* thread, oop obj);

  // Is the given object inside a CDS archive area?
  virtual bool is_archived_object(oop object) const;

  virtual bool is_oop(oop object) const;
  // Non product verification and debugging.
#ifndef PRODUCT
  // Support for PromotionFailureALot.  Return true if it's time to cause a
  // promotion failure.  The no-argument version uses
  // this->_promotion_failure_alot_count as the counter.
  bool promotion_should_fail(volatile size_t* count);
  bool promotion_should_fail();

  // Reset the PromotionFailureALot counters.  Should be called at the end of a
  // GC in which promotion failure occurred.
  void reset_promotion_should_fail(volatile size_t* count);
  void reset_promotion_should_fail();
#endif  // #ifndef PRODUCT
};

// Class to set and reset the GC cause for a CollectedHeap.

class GCCauseSetter : StackObj {
  CollectedHeap* _heap;
  GCCause::Cause _previous_cause;
 public:
  GCCauseSetter(CollectedHeap* heap, GCCause::Cause cause) {
    _heap = heap;
    _previous_cause = _heap->gc_cause();
    _heap->set_gc_cause(cause);
  }

  ~GCCauseSetter() {
    _heap->set_gc_cause(_previous_cause);
  }
};

#endif // SHARE_GC_SHARED_COLLECTEDHEAP_HPP
