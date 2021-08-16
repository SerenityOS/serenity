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

#ifndef SHARE_GC_PARALLEL_PSPROMOTIONMANAGER_INLINE_HPP
#define SHARE_GC_PARALLEL_PSPROMOTIONMANAGER_INLINE_HPP

#include "gc/parallel/psPromotionManager.hpp"

#include "gc/parallel/parallelScavengeHeap.hpp"
#include "gc/parallel/parMarkBitMap.inline.hpp"
#include "gc/parallel/psOldGen.hpp"
#include "gc/parallel/psPromotionLAB.inline.hpp"
#include "gc/parallel/psScavenge.inline.hpp"
#include "gc/shared/taskqueue.inline.hpp"
#include "gc/shared/tlab_globals.hpp"
#include "logging/log.hpp"
#include "memory/iterator.inline.hpp"
#include "oops/access.inline.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/orderAccess.hpp"
#include "runtime/prefetch.inline.hpp"

inline PSPromotionManager* PSPromotionManager::manager_array(uint index) {
  assert(_manager_array != NULL, "access of NULL manager_array");
  assert(index <= ParallelGCThreads, "out of range manager_array access");
  return &_manager_array[index];
}

inline void PSPromotionManager::push_depth(ScannerTask task) {
  claimed_stack_depth()->push(task);
}

template <class T>
inline void PSPromotionManager::claim_or_forward_depth(T* p) {
  assert(should_scavenge(p, true), "revisiting object?");
  assert(ParallelScavengeHeap::heap()->is_in(p), "pointer outside heap");
  oop obj = RawAccess<IS_NOT_NULL>::oop_load(p);
  Prefetch::write(obj->mark_addr(), 0);
  push_depth(ScannerTask(p));
}

inline void PSPromotionManager::promotion_trace_event(oop new_obj, oop old_obj,
                                                      size_t obj_size,
                                                      uint age, bool tenured,
                                                      const PSPromotionLAB* lab) {
  // Skip if memory allocation failed
  if (new_obj != NULL) {
    const ParallelScavengeTracer* gc_tracer = PSScavenge::gc_tracer();

    if (lab != NULL) {
      // Promotion of object through newly allocated PLAB
      if (gc_tracer->should_report_promotion_in_new_plab_event()) {
        size_t obj_bytes = obj_size * HeapWordSize;
        size_t lab_size = lab->capacity();
        gc_tracer->report_promotion_in_new_plab_event(old_obj->klass(), obj_bytes,
                                                      age, tenured, lab_size);
      }
    } else {
      // Promotion of object directly to heap
      if (gc_tracer->should_report_promotion_outside_plab_event()) {
        size_t obj_bytes = obj_size * HeapWordSize;
        gc_tracer->report_promotion_outside_plab_event(old_obj->klass(), obj_bytes,
                                                       age, tenured);
      }
    }
  }
}

class PSPushContentsClosure: public BasicOopIterateClosure {
  PSPromotionManager* _pm;
 public:
  PSPushContentsClosure(PSPromotionManager* pm) : BasicOopIterateClosure(PSScavenge::reference_processor()), _pm(pm) {}

  template <typename T> void do_oop_nv(T* p) {
    if (PSScavenge::should_scavenge(p)) {
      _pm->claim_or_forward_depth(p);
    }
  }

  virtual void do_oop(oop* p)       { do_oop_nv(p); }
  virtual void do_oop(narrowOop* p) { do_oop_nv(p); }
};

//
// This closure specialization will override the one that is defined in
// instanceRefKlass.inline.cpp. It swaps the order of oop_oop_iterate and
// oop_oop_iterate_ref_processing. Unfortunately G1 and Parallel behaves
// significantly better (especially in the Derby benchmark) using opposite
// order of these function calls.
//
template <>
inline void InstanceRefKlass::oop_oop_iterate_reverse<oop, PSPushContentsClosure>(oop obj, PSPushContentsClosure* closure) {
  oop_oop_iterate_ref_processing<oop>(obj, closure);
  InstanceKlass::oop_oop_iterate_reverse<oop>(obj, closure);
}

template <>
inline void InstanceRefKlass::oop_oop_iterate_reverse<narrowOop, PSPushContentsClosure>(oop obj, PSPushContentsClosure* closure) {
  oop_oop_iterate_ref_processing<narrowOop>(obj, closure);
  InstanceKlass::oop_oop_iterate_reverse<narrowOop>(obj, closure);
}

inline void PSPromotionManager::push_contents(oop obj) {
  if (!obj->klass()->is_typeArray_klass()) {
    PSPushContentsClosure pcc(this);
    obj->oop_iterate_backwards(&pcc);
  }
}

template<bool promote_immediately>
inline oop PSPromotionManager::copy_to_survivor_space(oop o) {
  assert(should_scavenge(&o), "Sanity");

  // NOTE! We must be very careful with any methods that access the mark
  // in o. There may be multiple threads racing on it, and it may be forwarded
  // at any time.
  markWord m = o->mark();
  if (!m.is_marked()) {
    return copy_unmarked_to_survivor_space<promote_immediately>(o, m);
  } else {
    // Ensure any loads from the forwardee follow all changes that precede
    // the release-cmpxchg that performed the forwarding, possibly in some
    // other thread.
    OrderAccess::acquire();
    // Return the already installed forwardee.
    return cast_to_oop(m.decode_pointer());
  }
}

//
// This method is pretty bulky. It would be nice to split it up
// into smaller submethods, but we need to be careful not to hurt
// performance.
//
template<bool promote_immediately>
inline oop PSPromotionManager::copy_unmarked_to_survivor_space(oop o,
                                                               markWord test_mark) {
  assert(should_scavenge(&o), "Sanity");

  oop new_obj = NULL;
  bool new_obj_is_tenured = false;
  size_t new_obj_size = o->size();

  // Find the objects age, MT safe.
  uint age = (test_mark.has_displaced_mark_helper() /* o->has_displaced_mark() */) ?
      test_mark.displaced_mark_helper().age() : test_mark.age();

  if (!promote_immediately) {
    // Try allocating obj in to-space (unless too old)
    if (age < PSScavenge::tenuring_threshold()) {
      new_obj = cast_to_oop(_young_lab.allocate(new_obj_size));
      if (new_obj == NULL && !_young_gen_is_full) {
        // Do we allocate directly, or flush and refill?
        if (new_obj_size > (YoungPLABSize / 2)) {
          // Allocate this object directly
          new_obj = cast_to_oop(young_space()->cas_allocate(new_obj_size));
          promotion_trace_event(new_obj, o, new_obj_size, age, false, NULL);
        } else {
          // Flush and fill
          _young_lab.flush();

          HeapWord* lab_base = young_space()->cas_allocate(YoungPLABSize);
          if (lab_base != NULL) {
            _young_lab.initialize(MemRegion(lab_base, YoungPLABSize));
            // Try the young lab allocation again.
            new_obj = cast_to_oop(_young_lab.allocate(new_obj_size));
            promotion_trace_event(new_obj, o, new_obj_size, age, false, &_young_lab);
          } else {
            _young_gen_is_full = true;
          }
        }
      }
    }
  }

  // Otherwise try allocating obj tenured
  if (new_obj == NULL) {
#ifndef PRODUCT
    if (ParallelScavengeHeap::heap()->promotion_should_fail()) {
      return oop_promotion_failed(o, test_mark);
    }
#endif  // #ifndef PRODUCT

    new_obj = cast_to_oop(_old_lab.allocate(new_obj_size));
    new_obj_is_tenured = true;

    if (new_obj == NULL) {
      if (!_old_gen_is_full) {
        // Do we allocate directly, or flush and refill?
        if (new_obj_size > (OldPLABSize / 2)) {
          // Allocate this object directly
          new_obj = cast_to_oop(old_gen()->allocate(new_obj_size));
          promotion_trace_event(new_obj, o, new_obj_size, age, true, NULL);
        } else {
          // Flush and fill
          _old_lab.flush();

          HeapWord* lab_base = old_gen()->allocate(OldPLABSize);
          if(lab_base != NULL) {
#ifdef ASSERT
            // Delay the initialization of the promotion lab (plab).
            // This exposes uninitialized plabs to card table processing.
            if (GCWorkerDelayMillis > 0) {
              os::naked_sleep(GCWorkerDelayMillis);
            }
#endif
            _old_lab.initialize(MemRegion(lab_base, OldPLABSize));
            // Try the old lab allocation again.
            new_obj = cast_to_oop(_old_lab.allocate(new_obj_size));
            promotion_trace_event(new_obj, o, new_obj_size, age, true, &_old_lab);
          }
        }
      }

      // This is the promotion failed test, and code handling.
      // The code belongs here for two reasons. It is slightly
      // different than the code below, and cannot share the
      // CAS testing code. Keeping the code here also minimizes
      // the impact on the common case fast path code.

      if (new_obj == NULL) {
        _old_gen_is_full = true;
        return oop_promotion_failed(o, test_mark);
      }
    }
  }

  assert(new_obj != NULL, "allocation should have succeeded");

  // Copy obj
  Copy::aligned_disjoint_words(cast_from_oop<HeapWord*>(o), cast_from_oop<HeapWord*>(new_obj), new_obj_size);

  // Now we have to CAS in the header.
  // Make copy visible to threads reading the forwardee.
  oop forwardee = o->forward_to_atomic(new_obj, test_mark, memory_order_release);
  if (forwardee == NULL) {  // forwardee is NULL when forwarding is successful
    // We won any races, we "own" this object.
    assert(new_obj == o->forwardee(), "Sanity");

    // Increment age if obj still in new generation. Now that
    // we're dealing with a markWord that cannot change, it is
    // okay to use the non mt safe oop methods.
    if (!new_obj_is_tenured) {
      new_obj->incr_age();
      assert(young_space()->contains(new_obj), "Attempt to push non-promoted obj");
    }

    log_develop_trace(gc, scavenge)("{%s %s " PTR_FORMAT " -> " PTR_FORMAT " (%d)}",
                                    new_obj_is_tenured ? "copying" : "tenuring",
                                    new_obj->klass()->internal_name(),
                                    p2i((void *)o), p2i((void *)new_obj), new_obj->size());

    // Do the size comparison first with new_obj_size, which we
    // already have. Hopefully, only a few objects are larger than
    // _min_array_size_for_chunking, and most of them will be arrays.
    // So, the is->objArray() test would be very infrequent.
    if (new_obj_size > _min_array_size_for_chunking &&
        new_obj->is_objArray() &&
        PSChunkLargeArrays) {
      // we'll chunk it
      push_depth(ScannerTask(PartialArrayScanTask(o)));
      TASKQUEUE_STATS_ONLY(++_arrays_chunked; ++_array_chunk_pushes);
    } else {
      // we'll just push its contents
      push_contents(new_obj);
    }
    return new_obj;
  } else {
    // We lost, someone else "owns" this object.
    // Ensure loads from the forwardee follow all changes that preceeded the
    // release-cmpxchg that performed the forwarding in another thread.
    OrderAccess::acquire();

    assert(o->is_forwarded(), "Object must be forwarded if the cas failed.");
    assert(o->forwardee() == forwardee, "invariant");

    // Try to deallocate the space.  If it was directly allocated we cannot
    // deallocate it, so we have to test.  If the deallocation fails,
    // overwrite with a filler object.
    if (new_obj_is_tenured) {
      if (!_old_lab.unallocate_object(cast_from_oop<HeapWord*>(new_obj), new_obj_size)) {
        CollectedHeap::fill_with_object(cast_from_oop<HeapWord*>(new_obj), new_obj_size);
      }
    } else if (!_young_lab.unallocate_object(cast_from_oop<HeapWord*>(new_obj), new_obj_size)) {
      CollectedHeap::fill_with_object(cast_from_oop<HeapWord*>(new_obj), new_obj_size);
    }
    return forwardee;
  }
}

// Attempt to "claim" oop at p via CAS, push the new obj if successful
// This version tests the oop* to make sure it is within the heap before
// attempting marking.
template <bool promote_immediately, class T>
inline void PSPromotionManager::copy_and_push_safe_barrier(T* p) {
  assert(should_scavenge(p, true), "revisiting object?");

  oop o = RawAccess<IS_NOT_NULL>::oop_load(p);
  oop new_obj = copy_to_survivor_space<promote_immediately>(o);
  RawAccess<IS_NOT_NULL>::oop_store(p, new_obj);

  // We cannot mark without test, as some code passes us pointers
  // that are outside the heap. These pointers are either from roots
  // or from metadata.
  if ((!PSScavenge::is_obj_in_young((HeapWord*)p)) &&
      ParallelScavengeHeap::heap()->is_in_reserved(p)) {
    if (PSScavenge::is_obj_in_young(new_obj)) {
      PSScavenge::card_table()->inline_write_ref_field_gc(p, new_obj);
    }
  }
}

inline void PSPromotionManager::process_popped_location_depth(ScannerTask task) {
  if (task.is_partial_array_task()) {
    assert(PSChunkLargeArrays, "invariant");
    process_array_chunk(task.to_partial_array_task());
  } else {
    if (task.is_narrow_oop_ptr()) {
      assert(UseCompressedOops, "Error");
      copy_and_push_safe_barrier</*promote_immediately=*/false>(task.to_narrow_oop_ptr());
    } else {
      copy_and_push_safe_barrier</*promote_immediately=*/false>(task.to_oop_ptr());
    }
  }
}

inline bool PSPromotionManager::steal_depth(int queue_num, ScannerTask& t) {
  return stack_array_depth()->steal(queue_num, t);
}

#if TASKQUEUE_STATS
void PSPromotionManager::record_steal(ScannerTask task) {
  if (task.is_partial_array_task()) {
    ++_array_chunk_steals;
  }
}
#endif // TASKQUEUE_STATS

#endif // SHARE_GC_PARALLEL_PSPROMOTIONMANAGER_INLINE_HPP
