/*
 * Copyright (c) 2013, 2021, Red Hat, Inc. All rights reserved.
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

#ifndef SHARE_GC_SHENANDOAH_SHENANDOAHHEAP_HPP
#define SHARE_GC_SHENANDOAH_SHENANDOAHHEAP_HPP

#include "gc/shared/markBitMap.hpp"
#include "gc/shared/softRefPolicy.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "gc/shenandoah/shenandoahAsserts.hpp"
#include "gc/shenandoah/shenandoahAllocRequest.hpp"
#include "gc/shenandoah/shenandoahLock.hpp"
#include "gc/shenandoah/shenandoahEvacOOMHandler.hpp"
#include "gc/shenandoah/shenandoahPadding.hpp"
#include "gc/shenandoah/shenandoahSharedVariables.hpp"
#include "gc/shenandoah/shenandoahUnload.hpp"
#include "memory/metaspace.hpp"
#include "services/memoryManager.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/stack.hpp"

class ConcurrentGCTimer;
class ObjectIterateScanRootClosure;
class ShenandoahCollectorPolicy;
class ShenandoahControlThread;
class ShenandoahGCSession;
class ShenandoahGCStateResetter;
class ShenandoahHeuristics;
class ShenandoahMarkingContext;
class ShenandoahMode;
class ShenandoahPhaseTimings;
class ShenandoahHeap;
class ShenandoahHeapRegion;
class ShenandoahHeapRegionClosure;
class ShenandoahCollectionSet;
class ShenandoahFreeSet;
class ShenandoahConcurrentMark;
class ShenandoahFullGC;
class ShenandoahMonitoringSupport;
class ShenandoahPacer;
class ShenandoahReferenceProcessor;
class ShenandoahVerifier;
class ShenandoahWorkGang;
class VMStructs;

// Used for buffering per-region liveness data.
// Needed since ShenandoahHeapRegion uses atomics to update liveness.
// The ShenandoahHeap array has max-workers elements, each of which is an array of
// uint16_t * max_regions. The choice of uint16_t is not accidental:
// there is a tradeoff between static/dynamic footprint that translates
// into cache pressure (which is already high during marking), and
// too many atomic updates. uint32_t is too large, uint8_t is too small.
typedef uint16_t ShenandoahLiveData;
#define SHENANDOAH_LIVEDATA_MAX ((ShenandoahLiveData)-1)

class ShenandoahRegionIterator : public StackObj {
private:
  ShenandoahHeap* _heap;

  shenandoah_padding(0);
  volatile size_t _index;
  shenandoah_padding(1);

  // No implicit copying: iterators should be passed by reference to capture the state
  NONCOPYABLE(ShenandoahRegionIterator);

public:
  ShenandoahRegionIterator();
  ShenandoahRegionIterator(ShenandoahHeap* heap);

  // Reset iterator to default state
  void reset();

  // Returns next region, or NULL if there are no more regions.
  // This is multi-thread-safe.
  inline ShenandoahHeapRegion* next();

  // This is *not* MT safe. However, in the absence of multithreaded access, it
  // can be used to determine if there is more work to do.
  bool has_next() const;
};

class ShenandoahHeapRegionClosure : public StackObj {
public:
  virtual void heap_region_do(ShenandoahHeapRegion* r) = 0;
  virtual bool is_thread_safe() { return false; }
};

typedef ShenandoahLock    ShenandoahHeapLock;
typedef ShenandoahLocker  ShenandoahHeapLocker;
typedef Stack<oop, mtGC>  ShenandoahScanObjectStack;

// Shenandoah GC is low-pause concurrent GC that uses Brooks forwarding pointers
// to encode forwarding data. See BrooksPointer for details on forwarding data encoding.
// See ShenandoahControlThread for GC cycle structure.
//
class ShenandoahHeap : public CollectedHeap {
  friend class ShenandoahAsserts;
  friend class VMStructs;
  friend class ShenandoahGCSession;
  friend class ShenandoahGCStateResetter;
  friend class ShenandoahParallelObjectIterator;
  friend class ShenandoahSafepoint;
  // Supported GC
  friend class ShenandoahConcurrentGC;
  friend class ShenandoahDegenGC;
  friend class ShenandoahFullGC;
  friend class ShenandoahUnload;

// ---------- Locks that guard important data structures in Heap
//
private:
  ShenandoahHeapLock _lock;

public:
  ShenandoahHeapLock* lock() {
    return &_lock;
  }

// ---------- Initialization, termination, identification, printing routines
//
public:
  static ShenandoahHeap* heap();

  const char* name()          const { return "Shenandoah"; }
  ShenandoahHeap::Name kind() const { return CollectedHeap::Shenandoah; }

  ShenandoahHeap(ShenandoahCollectorPolicy* policy);
  jint initialize();
  void post_initialize();
  void initialize_mode();
  void initialize_heuristics();

  void initialize_serviceability();

  void print_on(outputStream* st)              const;
  void print_extended_on(outputStream *st)     const;
  void print_tracing_info()                    const;
  void print_heap_regions_on(outputStream* st) const;

  void stop();

  void prepare_for_verify();
  void verify(VerifyOption vo);

// WhiteBox testing support.
  bool supports_concurrent_gc_breakpoints() const {
    return true;
  }

// ---------- Heap counters and metrics
//
private:
           size_t _initial_size;
           size_t _minimum_size;
  volatile size_t _soft_max_size;
  shenandoah_padding(0);
  volatile size_t _used;
  volatile size_t _committed;
  volatile size_t _bytes_allocated_since_gc_start;
  shenandoah_padding(1);

public:
  void increase_used(size_t bytes);
  void decrease_used(size_t bytes);
  void set_used(size_t bytes);

  void increase_committed(size_t bytes);
  void decrease_committed(size_t bytes);
  void increase_allocated(size_t bytes);

  size_t bytes_allocated_since_gc_start();
  void reset_bytes_allocated_since_gc_start();

  size_t min_capacity()      const;
  size_t max_capacity()      const;
  size_t soft_max_capacity() const;
  size_t initial_capacity()  const;
  size_t capacity()          const;
  size_t used()              const;
  size_t committed()         const;

  void set_soft_max_capacity(size_t v);

// ---------- Workers handling
//
private:
  uint _max_workers;
  ShenandoahWorkGang* _workers;
  ShenandoahWorkGang* _safepoint_workers;

public:
  uint max_workers();
  void assert_gc_workers(uint nworker) NOT_DEBUG_RETURN;

  WorkGang* workers() const;
  WorkGang* safepoint_workers();

  void gc_threads_do(ThreadClosure* tcl) const;

// ---------- Heap regions handling machinery
//
private:
  MemRegion _heap_region;
  bool      _heap_region_special;
  size_t    _num_regions;
  ShenandoahHeapRegion** _regions;
  ShenandoahRegionIterator _update_refs_iterator;

public:

  inline HeapWord* base() const { return _heap_region.start(); }

  inline size_t num_regions() const { return _num_regions; }
  inline bool is_heap_region_special() { return _heap_region_special; }

  inline ShenandoahHeapRegion* const heap_region_containing(const void* addr) const;
  inline size_t heap_region_index_containing(const void* addr) const;

  inline ShenandoahHeapRegion* const get_region(size_t region_idx) const;

  void heap_region_iterate(ShenandoahHeapRegionClosure* blk) const;
  void parallel_heap_region_iterate(ShenandoahHeapRegionClosure* blk) const;

// ---------- GC state machinery
//
// GC state describes the important parts of collector state, that may be
// used to make barrier selection decisions in the native and generated code.
// Multiple bits can be set at once.
//
// Important invariant: when GC state is zero, the heap is stable, and no barriers
// are required.
//
public:
  enum GCStateBitPos {
    // Heap has forwarded objects: needs LRB barriers.
    HAS_FORWARDED_BITPOS   = 0,

    // Heap is under marking: needs SATB barriers.
    MARKING_BITPOS    = 1,

    // Heap is under evacuation: needs LRB barriers. (Set together with HAS_FORWARDED)
    EVACUATION_BITPOS = 2,

    // Heap is under updating: needs no additional barriers.
    UPDATEREFS_BITPOS = 3,

    // Heap is under weak-reference/roots processing: needs weak-LRB barriers.
    WEAK_ROOTS_BITPOS  = 4,
  };

  enum GCState {
    STABLE        = 0,
    HAS_FORWARDED = 1 << HAS_FORWARDED_BITPOS,
    MARKING       = 1 << MARKING_BITPOS,
    EVACUATION    = 1 << EVACUATION_BITPOS,
    UPDATEREFS    = 1 << UPDATEREFS_BITPOS,
    WEAK_ROOTS    = 1 << WEAK_ROOTS_BITPOS,
  };

private:
  ShenandoahSharedBitmap _gc_state;
  ShenandoahSharedFlag   _degenerated_gc_in_progress;
  ShenandoahSharedFlag   _full_gc_in_progress;
  ShenandoahSharedFlag   _full_gc_move_in_progress;
  ShenandoahSharedFlag   _progress_last_gc;
  ShenandoahSharedFlag   _concurrent_strong_root_in_progress;

  void set_gc_state_all_threads(char state);
  void set_gc_state_mask(uint mask, bool value);

public:
  char gc_state() const;
  static address gc_state_addr();

  void set_concurrent_mark_in_progress(bool in_progress);
  void set_evacuation_in_progress(bool in_progress);
  void set_update_refs_in_progress(bool in_progress);
  void set_degenerated_gc_in_progress(bool in_progress);
  void set_full_gc_in_progress(bool in_progress);
  void set_full_gc_move_in_progress(bool in_progress);
  void set_has_forwarded_objects(bool cond);
  void set_concurrent_strong_root_in_progress(bool cond);
  void set_concurrent_weak_root_in_progress(bool cond);

  inline bool is_stable() const;
  inline bool is_idle() const;
  inline bool is_concurrent_mark_in_progress() const;
  inline bool is_update_refs_in_progress() const;
  inline bool is_evacuation_in_progress() const;
  inline bool is_degenerated_gc_in_progress() const;
  inline bool is_full_gc_in_progress() const;
  inline bool is_full_gc_move_in_progress() const;
  inline bool has_forwarded_objects() const;
  inline bool is_gc_in_progress_mask(uint mask) const;
  inline bool is_stw_gc_in_progress() const;
  inline bool is_concurrent_strong_root_in_progress() const;
  inline bool is_concurrent_weak_root_in_progress() const;

private:
  enum CancelState {
    // Normal state. GC has not been cancelled and is open for cancellation.
    // Worker threads can suspend for safepoint.
    CANCELLABLE,

    // GC has been cancelled. Worker threads can not suspend for
    // safepoint but must finish their work as soon as possible.
    CANCELLED,

    // GC has not been cancelled and must not be cancelled. At least
    // one worker thread checks for pending safepoint and may suspend
    // if a safepoint is pending.
    NOT_CANCELLED
  };

  ShenandoahSharedEnumFlag<CancelState> _cancelled_gc;
  bool try_cancel_gc();

public:
  static address cancelled_gc_addr();

  inline bool cancelled_gc() const;
  inline bool check_cancelled_gc_and_yield(bool sts_active = true);

  inline void clear_cancelled_gc();

  void cancel_gc(GCCause::Cause cause);

public:
  // Elastic heap support
  void entry_uncommit(double shrink_before, size_t shrink_until);
  void op_uncommit(double shrink_before, size_t shrink_until);

private:
  // GC support
  // Reset bitmap, prepare regions for new GC cycle
  void prepare_gc();
  void prepare_regions_and_collection_set(bool concurrent);
  // Evacuation
  void prepare_evacuation(bool concurrent);
  void evacuate_collection_set(bool concurrent);
  // Concurrent root processing
  void prepare_concurrent_roots();
  void finish_concurrent_roots();
  // Concurrent class unloading support
  void do_class_unloading();
  // Reference updating
  void prepare_update_heap_references(bool concurrent);
  void update_heap_references(bool concurrent);
  // Final update region states
  void update_heap_region_states(bool concurrent);
  void rebuild_free_set(bool concurrent);

  void rendezvous_threads();
  void recycle_trash();
public:
  void notify_gc_progress()    { _progress_last_gc.set();   }
  void notify_gc_no_progress() { _progress_last_gc.unset(); }

//
// Mark support
private:
  ShenandoahControlThread*   _control_thread;
  ShenandoahCollectorPolicy* _shenandoah_policy;
  ShenandoahMode*            _gc_mode;
  ShenandoahHeuristics*      _heuristics;
  ShenandoahFreeSet*         _free_set;
  ShenandoahPacer*           _pacer;
  ShenandoahVerifier*        _verifier;

  ShenandoahPhaseTimings*    _phase_timings;

  ShenandoahControlThread*   control_thread()          { return _control_thread;    }

public:
  ShenandoahCollectorPolicy* shenandoah_policy() const { return _shenandoah_policy; }
  ShenandoahMode*            mode()              const { return _gc_mode;           }
  ShenandoahHeuristics*      heuristics()        const { return _heuristics;        }
  ShenandoahFreeSet*         free_set()          const { return _free_set;          }
  ShenandoahPacer*           pacer()             const { return _pacer;             }

  ShenandoahPhaseTimings*    phase_timings()     const { return _phase_timings;     }

  ShenandoahVerifier*        verifier();

// ---------- VM subsystem bindings
//
private:
  ShenandoahMonitoringSupport* _monitoring_support;
  MemoryPool*                  _memory_pool;
  GCMemoryManager              _stw_memory_manager;
  GCMemoryManager              _cycle_memory_manager;
  ConcurrentGCTimer*           _gc_timer;
  SoftRefPolicy                _soft_ref_policy;

  // For exporting to SA
  int                          _log_min_obj_alignment_in_bytes;
public:
  ShenandoahMonitoringSupport* monitoring_support() { return _monitoring_support;    }
  GCMemoryManager* cycle_memory_manager()           { return &_cycle_memory_manager; }
  GCMemoryManager* stw_memory_manager()             { return &_stw_memory_manager;   }
  SoftRefPolicy* soft_ref_policy()                  { return &_soft_ref_policy;      }

  GrowableArray<GCMemoryManager*> memory_managers();
  GrowableArray<MemoryPool*> memory_pools();
  MemoryUsage memory_usage();
  GCTracer* tracer();
  ConcurrentGCTimer* gc_timer() const;

// ---------- Reference processing
//
private:
  ShenandoahReferenceProcessor* const _ref_processor;

public:
  ShenandoahReferenceProcessor* ref_processor() { return _ref_processor; }

// ---------- Class Unloading
//
private:
  ShenandoahSharedFlag _unload_classes;
  ShenandoahUnload     _unloader;

public:
  void set_unload_classes(bool uc);
  bool unload_classes() const;

  // Perform STW class unloading and weak root cleaning
  void parallel_cleaning(bool full_gc);

private:
  void stw_unload_classes(bool full_gc);
  void stw_process_weak_roots(bool full_gc);
  void stw_weak_refs(bool full_gc);

  // Heap iteration support
  void scan_roots_for_iteration(ShenandoahScanObjectStack* oop_stack, ObjectIterateScanRootClosure* oops);
  bool prepare_aux_bitmap_for_iteration();
  void reclaim_aux_bitmap_for_iteration();

// ---------- Generic interface hooks
// Minor things that super-interface expects us to implement to play nice with
// the rest of runtime. Some of the things here are not required to be implemented,
// and can be stubbed out.
//
public:
  AdaptiveSizePolicy* size_policy() shenandoah_not_implemented_return(NULL);
  bool is_maximal_no_gc() const shenandoah_not_implemented_return(false);

  bool is_in(const void* p) const;

  MemRegion reserved_region() const { return _reserved; }
  bool is_in_reserved(const void* addr) const { return _reserved.contains(addr); }

  void collect(GCCause::Cause cause);
  void do_full_collection(bool clear_all_soft_refs);

  // Used for parsing heap during error printing
  HeapWord* block_start(const void* addr) const;
  bool block_is_obj(const HeapWord* addr) const;
  bool print_location(outputStream* st, void* addr) const;

  // Used for native heap walkers: heap dumpers, mostly
  void object_iterate(ObjectClosure* cl);
  // Parallel heap iteration support
  virtual ParallelObjectIterator* parallel_object_iterator(uint workers);

  // Keep alive an object that was loaded with AS_NO_KEEPALIVE.
  void keep_alive(oop obj);

// ---------- Safepoint interface hooks
//
public:
  void safepoint_synchronize_begin();
  void safepoint_synchronize_end();

// ---------- Code roots handling hooks
//
public:
  void register_nmethod(nmethod* nm);
  void unregister_nmethod(nmethod* nm);
  void flush_nmethod(nmethod* nm);
  void verify_nmethod(nmethod* nm) {}

// ---------- Pinning hooks
//
public:
  // Shenandoah supports per-object (per-region) pinning
  bool supports_object_pinning() const { return true; }

  oop pin_object(JavaThread* thread, oop obj);
  void unpin_object(JavaThread* thread, oop obj);

  void sync_pinned_region_status();
  void assert_pinned_region_status() NOT_DEBUG_RETURN;

// ---------- Concurrent Stack Processing support
//
public:
  bool uses_stack_watermark_barrier() const { return true; }

// ---------- Allocation support
//
private:
  HeapWord* allocate_memory_under_lock(ShenandoahAllocRequest& request, bool& in_new_region);
  inline HeapWord* allocate_from_gclab(Thread* thread, size_t size);
  HeapWord* allocate_from_gclab_slow(Thread* thread, size_t size);
  HeapWord* allocate_new_gclab(size_t min_size, size_t word_size, size_t* actual_size);

public:
  HeapWord* allocate_memory(ShenandoahAllocRequest& request);
  HeapWord* mem_allocate(size_t size, bool* what);
  MetaWord* satisfy_failed_metadata_allocation(ClassLoaderData* loader_data,
                                               size_t size,
                                               Metaspace::MetadataType mdtype);

  void notify_mutator_alloc_words(size_t words, bool waste);

  HeapWord* allocate_new_tlab(size_t min_size, size_t requested_size, size_t* actual_size);
  size_t tlab_capacity(Thread *thr) const;
  size_t unsafe_max_tlab_alloc(Thread *thread) const;
  size_t max_tlab_size() const;
  size_t tlab_used(Thread* ignored) const;

  void ensure_parsability(bool retire_labs);

  void labs_make_parsable();
  void tlabs_retire(bool resize);
  void gclabs_retire(bool resize);

// ---------- Marking support
//
private:
  ShenandoahMarkingContext* _marking_context;
  MemRegion  _bitmap_region;
  MemRegion  _aux_bitmap_region;
  MarkBitMap _verification_bit_map;
  MarkBitMap _aux_bit_map;

  size_t _bitmap_size;
  size_t _bitmap_regions_per_slice;
  size_t _bitmap_bytes_per_slice;

  size_t _pretouch_heap_page_size;
  size_t _pretouch_bitmap_page_size;

  bool _bitmap_region_special;
  bool _aux_bitmap_region_special;

  ShenandoahLiveData** _liveness_cache;

public:
  inline ShenandoahMarkingContext* complete_marking_context() const;
  inline ShenandoahMarkingContext* marking_context() const;
  inline void mark_complete_marking_context();
  inline void mark_incomplete_marking_context();

  template<class T>
  inline void marked_object_iterate(ShenandoahHeapRegion* region, T* cl);

  template<class T>
  inline void marked_object_iterate(ShenandoahHeapRegion* region, T* cl, HeapWord* limit);

  template<class T>
  inline void marked_object_oop_iterate(ShenandoahHeapRegion* region, T* cl, HeapWord* limit);

  void reset_mark_bitmap();

  // SATB barriers hooks
  inline bool requires_marking(const void* entry) const;

  // Support for bitmap uncommits
  bool commit_bitmap_slice(ShenandoahHeapRegion *r);
  bool uncommit_bitmap_slice(ShenandoahHeapRegion *r);
  bool is_bitmap_slice_committed(ShenandoahHeapRegion* r, bool skip_self = false);

  // Liveness caching support
  ShenandoahLiveData* get_liveness_cache(uint worker_id);
  void flush_liveness_cache(uint worker_id);

  size_t pretouch_heap_page_size() { return _pretouch_heap_page_size; }

// ---------- Evacuation support
//
private:
  ShenandoahCollectionSet* _collection_set;
  ShenandoahEvacOOMHandler _oom_evac_handler;

public:
  static address in_cset_fast_test_addr();

  ShenandoahCollectionSet* collection_set() const { return _collection_set; }

  // Checks if object is in the collection set.
  inline bool in_collection_set(oop obj) const;

  // Checks if location is in the collection set. Can be interior pointer, not the oop itself.
  inline bool in_collection_set_loc(void* loc) const;

  // Evacuates object src. Returns the evacuated object, either evacuated
  // by this thread, or by some other thread.
  inline oop evacuate_object(oop src, Thread* thread);

  // Call before/after evacuation.
  inline void enter_evacuation(Thread* t);
  inline void leave_evacuation(Thread* t);

// ---------- Helper functions
//
public:
  template <class T>
  inline void conc_update_with_forwarded(T* p);

  template <class T>
  inline void update_with_forwarded(T* p);

  static inline void atomic_update_oop(oop update,       oop* addr,       oop compare);
  static inline void atomic_update_oop(oop update, narrowOop* addr,       oop compare);
  static inline void atomic_update_oop(oop update, narrowOop* addr, narrowOop compare);

  static inline bool atomic_update_oop_check(oop update,       oop* addr,       oop compare);
  static inline bool atomic_update_oop_check(oop update, narrowOop* addr,       oop compare);
  static inline bool atomic_update_oop_check(oop update, narrowOop* addr, narrowOop compare);

  static inline void atomic_clear_oop(      oop* addr,       oop compare);
  static inline void atomic_clear_oop(narrowOop* addr,       oop compare);
  static inline void atomic_clear_oop(narrowOop* addr, narrowOop compare);

  void trash_humongous_region_at(ShenandoahHeapRegion *r);

private:
  void trash_cset_regions();

// ---------- Testing helpers functions
//
private:
  ShenandoahSharedFlag _inject_alloc_failure;

  void try_inject_alloc_failure();
  bool should_inject_alloc_failure();
};

#endif // SHARE_GC_SHENANDOAH_SHENANDOAHHEAP_HPP
