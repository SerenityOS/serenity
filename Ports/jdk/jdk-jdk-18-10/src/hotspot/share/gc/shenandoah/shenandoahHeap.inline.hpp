/*
 * Copyright (c) 2015, 2020, Red Hat, Inc. All rights reserved.
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

#ifndef SHARE_GC_SHENANDOAH_SHENANDOAHHEAP_INLINE_HPP
#define SHARE_GC_SHENANDOAH_SHENANDOAHHEAP_INLINE_HPP

#include "gc/shenandoah/shenandoahHeap.hpp"

#include "classfile/javaClasses.inline.hpp"
#include "gc/shared/markBitMap.inline.hpp"
#include "gc/shared/threadLocalAllocBuffer.inline.hpp"
#include "gc/shared/suspendibleThreadSet.hpp"
#include "gc/shared/tlab_globals.hpp"
#include "gc/shenandoah/shenandoahAsserts.hpp"
#include "gc/shenandoah/shenandoahBarrierSet.inline.hpp"
#include "gc/shenandoah/shenandoahCollectionSet.inline.hpp"
#include "gc/shenandoah/shenandoahForwarding.inline.hpp"
#include "gc/shenandoah/shenandoahWorkGroup.hpp"
#include "gc/shenandoah/shenandoahHeapRegionSet.inline.hpp"
#include "gc/shenandoah/shenandoahHeapRegion.inline.hpp"
#include "gc/shenandoah/shenandoahControlThread.hpp"
#include "gc/shenandoah/shenandoahMarkingContext.inline.hpp"
#include "gc/shenandoah/shenandoahThreadLocalData.hpp"
#include "oops/compressedOops.inline.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/atomic.hpp"
#include "runtime/prefetch.inline.hpp"
#include "runtime/thread.hpp"
#include "utilities/copy.hpp"
#include "utilities/globalDefinitions.hpp"

inline ShenandoahHeap* ShenandoahHeap::heap() {
  return named_heap<ShenandoahHeap>(CollectedHeap::Shenandoah);
}

inline ShenandoahHeapRegion* ShenandoahRegionIterator::next() {
  size_t new_index = Atomic::add(&_index, (size_t) 1, memory_order_relaxed);
  // get_region() provides the bounds-check and returns NULL on OOB.
  return _heap->get_region(new_index - 1);
}

inline bool ShenandoahHeap::has_forwarded_objects() const {
  return _gc_state.is_set(HAS_FORWARDED);
}

inline WorkGang* ShenandoahHeap::workers() const {
  return _workers;
}

inline WorkGang* ShenandoahHeap::safepoint_workers() {
  return _safepoint_workers;
}

inline size_t ShenandoahHeap::heap_region_index_containing(const void* addr) const {
  uintptr_t region_start = ((uintptr_t) addr);
  uintptr_t index = (region_start - (uintptr_t) base()) >> ShenandoahHeapRegion::region_size_bytes_shift();
  assert(index < num_regions(), "Region index is in bounds: " PTR_FORMAT, p2i(addr));
  return index;
}

inline ShenandoahHeapRegion* const ShenandoahHeap::heap_region_containing(const void* addr) const {
  size_t index = heap_region_index_containing(addr);
  ShenandoahHeapRegion* const result = get_region(index);
  assert(addr >= result->bottom() && addr < result->end(), "Heap region contains the address: " PTR_FORMAT, p2i(addr));
  return result;
}

inline void ShenandoahHeap::enter_evacuation(Thread* t) {
  _oom_evac_handler.enter_evacuation(t);
}

inline void ShenandoahHeap::leave_evacuation(Thread* t) {
  _oom_evac_handler.leave_evacuation(t);
}

template <class T>
inline void ShenandoahHeap::update_with_forwarded(T* p) {
  T o = RawAccess<>::oop_load(p);
  if (!CompressedOops::is_null(o)) {
    oop obj = CompressedOops::decode_not_null(o);
    if (in_collection_set(obj)) {
      // Corner case: when evacuation fails, there are objects in collection
      // set that are not really forwarded. We can still go and try and update them
      // (uselessly) to simplify the common path.
      shenandoah_assert_forwarded_except(p, obj, cancelled_gc());
      oop fwd = ShenandoahBarrierSet::resolve_forwarded_not_null(obj);
      shenandoah_assert_not_in_cset_except(p, fwd, cancelled_gc());

      // Unconditionally store the update: no concurrent updates expected.
      RawAccess<IS_NOT_NULL>::oop_store(p, fwd);
    }
  }
}

template <class T>
inline void ShenandoahHeap::conc_update_with_forwarded(T* p) {
  T o = RawAccess<>::oop_load(p);
  if (!CompressedOops::is_null(o)) {
    oop obj = CompressedOops::decode_not_null(o);
    if (in_collection_set(obj)) {
      // Corner case: when evacuation fails, there are objects in collection
      // set that are not really forwarded. We can still go and try CAS-update them
      // (uselessly) to simplify the common path.
      shenandoah_assert_forwarded_except(p, obj, cancelled_gc());
      oop fwd = ShenandoahBarrierSet::resolve_forwarded_not_null(obj);
      shenandoah_assert_not_in_cset_except(p, fwd, cancelled_gc());

      // Sanity check: we should not be updating the cset regions themselves,
      // unless we are recovering from the evacuation failure.
      shenandoah_assert_not_in_cset_loc_except(p, !is_in(p) || cancelled_gc());

      // Either we succeed in updating the reference, or something else gets in our way.
      // We don't care if that is another concurrent GC update, or another mutator update.
      atomic_update_oop(fwd, p, obj);
    }
  }
}

// Atomic updates of heap location. This is only expected to work with updating the same
// logical object with its forwardee. The reason why we need stronger-than-relaxed memory
// ordering has to do with coordination with GC barriers and mutator accesses.
//
// In essence, stronger CAS access is required to maintain the transitive chains that mutator
// accesses build by themselves. To illustrate this point, consider the following example.
//
// Suppose "o" is the object that has a field "x" and the reference to "o" is stored
// to field at "addr", which happens to be Java volatile field. Normally, the accesses to volatile
// field at "addr" would be matched with release/acquire barriers. This changes when GC moves
// the object under mutator feet.
//
// Thread 1 (Java)
//         // --- previous access starts here
//         ...
//   T1.1: store(&o.x, 1, mo_relaxed)
//   T1.2: store(&addr, o, mo_release) // volatile store
//
//         // --- new access starts here
//         // LRB: copy and install the new copy to fwdptr
//   T1.3: var copy = copy(o)
//   T1.4: cas(&fwd, t, copy, mo_release) // pointer-mediated publication
//         <access continues>
//
// Thread 2 (GC updater)
//   T2.1: var f = load(&fwd, mo_{consume|acquire}) // pointer-mediated acquisition
//   T2.2: cas(&addr, o, f, mo_release) // this method
//
// Thread 3 (Java)
//   T3.1: var o = load(&addr, mo_acquire) // volatile read
//   T3.2: if (o != null)
//   T3.3:   var r = load(&o.x, mo_relaxed)
//
// r is guaranteed to contain "1".
//
// Without GC involvement, there is synchronizes-with edge from T1.2 to T3.1,
// which guarantees this. With GC involvement, when LRB copies the object and
// another thread updates the reference to it, we need to have the transitive edge
// from T1.4 to T2.1 (that one is guaranteed by forwarding accesses), plus the edge
// from T2.2 to T3.1 (which is brought by this CAS).
//
// Note that we do not need to "acquire" in these methods, because we do not read the
// failure witnesses contents on any path, and "release" is enough.
//

inline void ShenandoahHeap::atomic_update_oop(oop update, oop* addr, oop compare) {
  assert(is_aligned(addr, HeapWordSize), "Address should be aligned: " PTR_FORMAT, p2i(addr));
  Atomic::cmpxchg(addr, compare, update, memory_order_release);
}

inline void ShenandoahHeap::atomic_update_oop(oop update, narrowOop* addr, narrowOop compare) {
  assert(is_aligned(addr, sizeof(narrowOop)), "Address should be aligned: " PTR_FORMAT, p2i(addr));
  narrowOop u = CompressedOops::encode(update);
  Atomic::cmpxchg(addr, compare, u, memory_order_release);
}

inline void ShenandoahHeap::atomic_update_oop(oop update, narrowOop* addr, oop compare) {
  assert(is_aligned(addr, sizeof(narrowOop)), "Address should be aligned: " PTR_FORMAT, p2i(addr));
  narrowOop c = CompressedOops::encode(compare);
  narrowOop u = CompressedOops::encode(update);
  Atomic::cmpxchg(addr, c, u, memory_order_release);
}

inline bool ShenandoahHeap::atomic_update_oop_check(oop update, oop* addr, oop compare) {
  assert(is_aligned(addr, HeapWordSize), "Address should be aligned: " PTR_FORMAT, p2i(addr));
  return (oop) Atomic::cmpxchg(addr, compare, update, memory_order_release) == compare;
}

inline bool ShenandoahHeap::atomic_update_oop_check(oop update, narrowOop* addr, narrowOop compare) {
  assert(is_aligned(addr, sizeof(narrowOop)), "Address should be aligned: " PTR_FORMAT, p2i(addr));
  narrowOop u = CompressedOops::encode(update);
  return (narrowOop) Atomic::cmpxchg(addr, compare, u, memory_order_release) == compare;
}

inline bool ShenandoahHeap::atomic_update_oop_check(oop update, narrowOop* addr, oop compare) {
  assert(is_aligned(addr, sizeof(narrowOop)), "Address should be aligned: " PTR_FORMAT, p2i(addr));
  narrowOop c = CompressedOops::encode(compare);
  narrowOop u = CompressedOops::encode(update);
  return CompressedOops::decode(Atomic::cmpxchg(addr, c, u, memory_order_release)) == compare;
}

// The memory ordering discussion above does not apply for methods that store NULLs:
// then, there is no transitive reads in mutator (as we see NULLs), and we can do
// relaxed memory ordering there.

inline void ShenandoahHeap::atomic_clear_oop(oop* addr, oop compare) {
  assert(is_aligned(addr, HeapWordSize), "Address should be aligned: " PTR_FORMAT, p2i(addr));
  Atomic::cmpxchg(addr, compare, oop(), memory_order_relaxed);
}

inline void ShenandoahHeap::atomic_clear_oop(narrowOop* addr, oop compare) {
  assert(is_aligned(addr, sizeof(narrowOop)), "Address should be aligned: " PTR_FORMAT, p2i(addr));
  narrowOop cmp = CompressedOops::encode(compare);
  Atomic::cmpxchg(addr, cmp, narrowOop(), memory_order_relaxed);
}

inline void ShenandoahHeap::atomic_clear_oop(narrowOop* addr, narrowOop compare) {
  assert(is_aligned(addr, sizeof(narrowOop)), "Address should be aligned: " PTR_FORMAT, p2i(addr));
  Atomic::cmpxchg(addr, compare, narrowOop(), memory_order_relaxed);
}

inline bool ShenandoahHeap::cancelled_gc() const {
  return _cancelled_gc.get() == CANCELLED;
}

inline bool ShenandoahHeap::check_cancelled_gc_and_yield(bool sts_active) {
  if (! (sts_active && ShenandoahSuspendibleWorkers)) {
    return cancelled_gc();
  }

  jbyte prev = _cancelled_gc.cmpxchg(NOT_CANCELLED, CANCELLABLE);
  if (prev == CANCELLABLE || prev == NOT_CANCELLED) {
    if (SuspendibleThreadSet::should_yield()) {
      SuspendibleThreadSet::yield();
    }

    // Back to CANCELLABLE. The thread that poked NOT_CANCELLED first gets
    // to restore to CANCELLABLE.
    if (prev == CANCELLABLE) {
      _cancelled_gc.set(CANCELLABLE);
    }
    return false;
  } else {
    return true;
  }
}

inline void ShenandoahHeap::clear_cancelled_gc() {
  _cancelled_gc.set(CANCELLABLE);
  _oom_evac_handler.clear();
}

inline HeapWord* ShenandoahHeap::allocate_from_gclab(Thread* thread, size_t size) {
  assert(UseTLAB, "TLABs should be enabled");

  PLAB* gclab = ShenandoahThreadLocalData::gclab(thread);
  if (gclab == NULL) {
    assert(!thread->is_Java_thread() && !thread->is_Worker_thread(),
           "Performance: thread should have GCLAB: %s", thread->name());
    // No GCLABs in this thread, fallback to shared allocation
    return NULL;
  }
  HeapWord* obj = gclab->allocate(size);
  if (obj != NULL) {
    return obj;
  }
  // Otherwise...
  return allocate_from_gclab_slow(thread, size);
}

inline oop ShenandoahHeap::evacuate_object(oop p, Thread* thread) {
  if (ShenandoahThreadLocalData::is_oom_during_evac(Thread::current())) {
    // This thread went through the OOM during evac protocol and it is safe to return
    // the forward pointer. It must not attempt to evacuate any more.
    return ShenandoahBarrierSet::resolve_forwarded(p);
  }

  assert(ShenandoahThreadLocalData::is_evac_allowed(thread), "must be enclosed in oom-evac scope");

  size_t size = p->size();

  assert(!heap_region_containing(p)->is_humongous(), "never evacuate humongous objects");

  bool alloc_from_gclab = true;
  HeapWord* copy = NULL;

#ifdef ASSERT
  if (ShenandoahOOMDuringEvacALot &&
      (os::random() & 1) == 0) { // Simulate OOM every ~2nd slow-path call
        copy = NULL;
  } else {
#endif
    if (UseTLAB) {
      copy = allocate_from_gclab(thread, size);
    }
    if (copy == NULL) {
      ShenandoahAllocRequest req = ShenandoahAllocRequest::for_shared_gc(size);
      copy = allocate_memory(req);
      alloc_from_gclab = false;
    }
#ifdef ASSERT
  }
#endif

  if (copy == NULL) {
    control_thread()->handle_alloc_failure_evac(size);

    _oom_evac_handler.handle_out_of_memory_during_evacuation();

    return ShenandoahBarrierSet::resolve_forwarded(p);
  }

  // Copy the object:
  Copy::aligned_disjoint_words(cast_from_oop<HeapWord*>(p), copy, size);

  // Try to install the new forwarding pointer.
  oop copy_val = cast_to_oop(copy);
  oop result = ShenandoahForwarding::try_update_forwardee(p, copy_val);
  if (result == copy_val) {
    // Successfully evacuated. Our copy is now the public one!
    shenandoah_assert_correct(NULL, copy_val);
    return copy_val;
  }  else {
    // Failed to evacuate. We need to deal with the object that is left behind. Since this
    // new allocation is certainly after TAMS, it will be considered live in the next cycle.
    // But if it happens to contain references to evacuated regions, those references would
    // not get updated for this stale copy during this cycle, and we will crash while scanning
    // it the next cycle.
    //
    // For GCLAB allocations, it is enough to rollback the allocation ptr. Either the next
    // object will overwrite this stale copy, or the filler object on LAB retirement will
    // do this. For non-GCLAB allocations, we have no way to retract the allocation, and
    // have to explicitly overwrite the copy with the filler object. With that overwrite,
    // we have to keep the fwdptr initialized and pointing to our (stale) copy.
    if (alloc_from_gclab) {
      ShenandoahThreadLocalData::gclab(thread)->undo_allocation(copy, size);
    } else {
      fill_with_object(copy, size);
      shenandoah_assert_correct(NULL, copy_val);
    }
    shenandoah_assert_correct(NULL, result);
    return result;
  }
}

inline bool ShenandoahHeap::requires_marking(const void* entry) const {
  oop obj = cast_to_oop(entry);
  return !_marking_context->is_marked_strong(obj);
}

inline bool ShenandoahHeap::in_collection_set(oop p) const {
  assert(collection_set() != NULL, "Sanity");
  return collection_set()->is_in(p);
}

inline bool ShenandoahHeap::in_collection_set_loc(void* p) const {
  assert(collection_set() != NULL, "Sanity");
  return collection_set()->is_in_loc(p);
}

inline bool ShenandoahHeap::is_stable() const {
  return _gc_state.is_clear();
}

inline bool ShenandoahHeap::is_idle() const {
  return _gc_state.is_unset(MARKING | EVACUATION | UPDATEREFS);
}

inline bool ShenandoahHeap::is_concurrent_mark_in_progress() const {
  return _gc_state.is_set(MARKING);
}

inline bool ShenandoahHeap::is_evacuation_in_progress() const {
  return _gc_state.is_set(EVACUATION);
}

inline bool ShenandoahHeap::is_gc_in_progress_mask(uint mask) const {
  return _gc_state.is_set(mask);
}

inline bool ShenandoahHeap::is_degenerated_gc_in_progress() const {
  return _degenerated_gc_in_progress.is_set();
}

inline bool ShenandoahHeap::is_full_gc_in_progress() const {
  return _full_gc_in_progress.is_set();
}

inline bool ShenandoahHeap::is_full_gc_move_in_progress() const {
  return _full_gc_move_in_progress.is_set();
}

inline bool ShenandoahHeap::is_update_refs_in_progress() const {
  return _gc_state.is_set(UPDATEREFS);
}

inline bool ShenandoahHeap::is_stw_gc_in_progress() const {
  return is_full_gc_in_progress() || is_degenerated_gc_in_progress();
}

inline bool ShenandoahHeap::is_concurrent_strong_root_in_progress() const {
  return _concurrent_strong_root_in_progress.is_set();
}

inline bool ShenandoahHeap::is_concurrent_weak_root_in_progress() const {
  return _gc_state.is_set(WEAK_ROOTS);
}

template<class T>
inline void ShenandoahHeap::marked_object_iterate(ShenandoahHeapRegion* region, T* cl) {
  marked_object_iterate(region, cl, region->top());
}

template<class T>
inline void ShenandoahHeap::marked_object_iterate(ShenandoahHeapRegion* region, T* cl, HeapWord* limit) {
  assert(! region->is_humongous_continuation(), "no humongous continuation regions here");

  ShenandoahMarkingContext* const ctx = complete_marking_context();
  assert(ctx->is_complete(), "sanity");

  HeapWord* tams = ctx->top_at_mark_start(region);

  size_t skip_bitmap_delta = 1;
  HeapWord* start = region->bottom();
  HeapWord* end = MIN2(tams, region->end());

  // Step 1. Scan below the TAMS based on bitmap data.
  HeapWord* limit_bitmap = MIN2(limit, tams);

  // Try to scan the initial candidate. If the candidate is above the TAMS, it would
  // fail the subsequent "< limit_bitmap" checks, and fall through to Step 2.
  HeapWord* cb = ctx->get_next_marked_addr(start, end);

  intx dist = ShenandoahMarkScanPrefetch;
  if (dist > 0) {
    // Batched scan that prefetches the oop data, anticipating the access to
    // either header, oop field, or forwarding pointer. Not that we cannot
    // touch anything in oop, while it still being prefetched to get enough
    // time for prefetch to work. This is why we try to scan the bitmap linearly,
    // disregarding the object size. However, since we know forwarding pointer
    // preceeds the object, we can skip over it. Once we cannot trust the bitmap,
    // there is no point for prefetching the oop contents, as oop->size() will
    // touch it prematurely.

    // No variable-length arrays in standard C++, have enough slots to fit
    // the prefetch distance.
    static const int SLOT_COUNT = 256;
    guarantee(dist <= SLOT_COUNT, "adjust slot count");
    HeapWord* slots[SLOT_COUNT];

    int avail;
    do {
      avail = 0;
      for (int c = 0; (c < dist) && (cb < limit_bitmap); c++) {
        Prefetch::read(cb, oopDesc::mark_offset_in_bytes());
        slots[avail++] = cb;
        cb += skip_bitmap_delta;
        if (cb < limit_bitmap) {
          cb = ctx->get_next_marked_addr(cb, limit_bitmap);
        }
      }

      for (int c = 0; c < avail; c++) {
        assert (slots[c] < tams,  "only objects below TAMS here: "  PTR_FORMAT " (" PTR_FORMAT ")", p2i(slots[c]), p2i(tams));
        assert (slots[c] < limit, "only objects below limit here: " PTR_FORMAT " (" PTR_FORMAT ")", p2i(slots[c]), p2i(limit));
        oop obj = cast_to_oop(slots[c]);
        assert(oopDesc::is_oop(obj), "sanity");
        assert(ctx->is_marked(obj), "object expected to be marked");
        cl->do_object(obj);
      }
    } while (avail > 0);
  } else {
    while (cb < limit_bitmap) {
      assert (cb < tams,  "only objects below TAMS here: "  PTR_FORMAT " (" PTR_FORMAT ")", p2i(cb), p2i(tams));
      assert (cb < limit, "only objects below limit here: " PTR_FORMAT " (" PTR_FORMAT ")", p2i(cb), p2i(limit));
      oop obj = cast_to_oop(cb);
      assert(oopDesc::is_oop(obj), "sanity");
      assert(ctx->is_marked(obj), "object expected to be marked");
      cl->do_object(obj);
      cb += skip_bitmap_delta;
      if (cb < limit_bitmap) {
        cb = ctx->get_next_marked_addr(cb, limit_bitmap);
      }
    }
  }

  // Step 2. Accurate size-based traversal, happens past the TAMS.
  // This restarts the scan at TAMS, which makes sure we traverse all objects,
  // regardless of what happened at Step 1.
  HeapWord* cs = tams;
  while (cs < limit) {
    assert (cs >= tams, "only objects past TAMS here: "   PTR_FORMAT " (" PTR_FORMAT ")", p2i(cs), p2i(tams));
    assert (cs < limit, "only objects below limit here: " PTR_FORMAT " (" PTR_FORMAT ")", p2i(cs), p2i(limit));
    oop obj = cast_to_oop(cs);
    assert(oopDesc::is_oop(obj), "sanity");
    assert(ctx->is_marked(obj), "object expected to be marked");
    int size = obj->size();
    cl->do_object(obj);
    cs += size;
  }
}

template <class T>
class ShenandoahObjectToOopClosure : public ObjectClosure {
  T* _cl;
public:
  ShenandoahObjectToOopClosure(T* cl) : _cl(cl) {}

  void do_object(oop obj) {
    obj->oop_iterate(_cl);
  }
};

template <class T>
class ShenandoahObjectToOopBoundedClosure : public ObjectClosure {
  T* _cl;
  MemRegion _bounds;
public:
  ShenandoahObjectToOopBoundedClosure(T* cl, HeapWord* bottom, HeapWord* top) :
    _cl(cl), _bounds(bottom, top) {}

  void do_object(oop obj) {
    obj->oop_iterate(_cl, _bounds);
  }
};

template<class T>
inline void ShenandoahHeap::marked_object_oop_iterate(ShenandoahHeapRegion* region, T* cl, HeapWord* top) {
  if (region->is_humongous()) {
    HeapWord* bottom = region->bottom();
    if (top > bottom) {
      region = region->humongous_start_region();
      ShenandoahObjectToOopBoundedClosure<T> objs(cl, bottom, top);
      marked_object_iterate(region, &objs);
    }
  } else {
    ShenandoahObjectToOopClosure<T> objs(cl);
    marked_object_iterate(region, &objs, top);
  }
}

inline ShenandoahHeapRegion* const ShenandoahHeap::get_region(size_t region_idx) const {
  if (region_idx < _num_regions) {
    return _regions[region_idx];
  } else {
    return NULL;
  }
}

inline void ShenandoahHeap::mark_complete_marking_context() {
  _marking_context->mark_complete();
}

inline void ShenandoahHeap::mark_incomplete_marking_context() {
  _marking_context->mark_incomplete();
}

inline ShenandoahMarkingContext* ShenandoahHeap::complete_marking_context() const {
  assert (_marking_context->is_complete()," sanity");
  return _marking_context;
}

inline ShenandoahMarkingContext* ShenandoahHeap::marking_context() const {
  return _marking_context;
}

#endif // SHARE_GC_SHENANDOAH_SHENANDOAHHEAP_INLINE_HPP
