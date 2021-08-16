/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1OOPCLOSURES_INLINE_HPP
#define SHARE_GC_G1_G1OOPCLOSURES_INLINE_HPP

#include "gc/g1/g1OopClosures.hpp"

#include "gc/g1/g1CollectedHeap.hpp"
#include "gc/g1/g1ConcurrentMark.inline.hpp"
#include "gc/g1/g1ParScanThreadState.inline.hpp"
#include "gc/g1/g1RemSet.hpp"
#include "gc/g1/heapRegion.inline.hpp"
#include "gc/g1/heapRegionRemSet.inline.hpp"
#include "memory/iterator.inline.hpp"
#include "oops/access.inline.hpp"
#include "oops/compressedOops.inline.hpp"
#include "oops/oopsHierarchy.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/prefetch.inline.hpp"
#include "utilities/align.hpp"

template <class T>
inline void G1ScanClosureBase::prefetch_and_push(T* p, const oop obj) {
  // We're not going to even bother checking whether the object is
  // already forwarded or not, as this usually causes an immediate
  // stall. We'll try to prefetch the object (for write, given that
  // we might need to install the forwarding reference) and we'll
  // get back to it when pop it from the queue
  Prefetch::write(obj->mark_addr(), 0);
  Prefetch::read(obj->mark_addr(), (HeapWordSize*2));

  // slightly paranoid test; I'm trying to catch potential
  // problems before we go into push_on_queue to know where the
  // problem is coming from
  assert((obj == RawAccess<>::oop_load(p)) ||
         (obj->is_forwarded() &&
         obj->forwardee() == RawAccess<>::oop_load(p)),
         "p should still be pointing to obj or to its forwardee");

  _par_scan_state->push_on_queue(ScannerTask(p));
}

template <class T>
inline void G1ScanClosureBase::handle_non_cset_obj_common(G1HeapRegionAttr const region_attr, T* p, oop const obj) {
  if (region_attr.is_humongous()) {
    _g1h->set_humongous_is_live(obj);
  } else if (region_attr.is_optional()) {
    _par_scan_state->remember_reference_into_optional_region(p);
  }
}

inline void G1ScanClosureBase::trim_queue_partially() {
  _par_scan_state->trim_queue_partially();
}

template <class T>
inline void G1ScanEvacuatedObjClosure::do_oop_work(T* p) {
  T heap_oop = RawAccess<>::oop_load(p);

  if (CompressedOops::is_null(heap_oop)) {
    return;
  }
  oop obj = CompressedOops::decode_not_null(heap_oop);
  const G1HeapRegionAttr region_attr = _g1h->region_attr(obj);
  if (region_attr.is_in_cset()) {
    prefetch_and_push(p, obj);
  } else if (!HeapRegion::is_in_same_region(p, obj)) {
    handle_non_cset_obj_common(region_attr, p, obj);
    assert(_scanning_in_young != Uninitialized, "Scan location has not been initialized.");
    if (_scanning_in_young == True) {
      return;
    }
    _par_scan_state->enqueue_card_if_tracked(region_attr, p, obj);
  }
}

template <class T>
inline void G1CMOopClosure::do_oop_work(T* p) {
  _task->deal_with_reference(p);
}

template <class T>
inline void G1RootRegionScanClosure::do_oop_work(T* p) {
  T heap_oop = RawAccess<MO_RELAXED>::oop_load(p);
  if (CompressedOops::is_null(heap_oop)) {
    return;
  }
  oop obj = CompressedOops::decode_not_null(heap_oop);
  _cm->mark_in_next_bitmap(_worker_id, obj);
}

template <class T>
inline static void check_obj_during_refinement(T* p, oop const obj) {
#ifdef ASSERT
  G1CollectedHeap* g1h = G1CollectedHeap::heap();
  // can't do because of races
  // assert(oopDesc::is_oop_or_null(obj), "expected an oop");
  assert(is_object_aligned(obj), "oop must be aligned");
  assert(g1h->is_in_reserved(obj), "oop must be in reserved");

  HeapRegion* from = g1h->heap_region_containing(p);

  assert(from != NULL, "from region must be non-NULL");
  assert(from->is_in_reserved(p) ||
         (from->is_humongous() &&
          g1h->heap_region_containing(p)->is_humongous() &&
          from->humongous_start_region() == g1h->heap_region_containing(p)->humongous_start_region()),
         "p " PTR_FORMAT " is not in the same region %u or part of the correct humongous object starting at region %u.",
         p2i(p), from->hrm_index(), from->humongous_start_region()->hrm_index());
#endif // ASSERT
}

template <class T>
inline void G1ConcurrentRefineOopClosure::do_oop_work(T* p) {
  T o = RawAccess<MO_RELAXED>::oop_load(p);
  if (CompressedOops::is_null(o)) {
    return;
  }
  oop obj = CompressedOops::decode_not_null(o);

  check_obj_during_refinement(p, obj);

  if (HeapRegion::is_in_same_region(p, obj)) {
    // Normally this closure should only be called with cross-region references.
    // But since Java threads are manipulating the references concurrently and we
    // reload the values things may have changed.
    // Also this check lets slip through references from a humongous continues region
    // to its humongous start region, as they are in different regions, and adds a
    // remembered set entry. This is benign (apart from memory usage), as we never
    // try to either evacuate or eager reclaim humonguous arrays of j.l.O.
    return;
  }

  HeapRegionRemSet* to_rem_set = _g1h->heap_region_containing(obj)->rem_set();

  assert(to_rem_set != NULL, "Need per-region 'into' remsets.");
  if (to_rem_set->is_tracked()) {
    to_rem_set->add_reference(p, _worker_id);
  }
}

template <class T>
inline void G1ScanCardClosure::do_oop_work(T* p) {
  T o = RawAccess<>::oop_load(p);
  if (CompressedOops::is_null(o)) {
    return;
  }
  oop obj = CompressedOops::decode_not_null(o);

  check_obj_during_refinement(p, obj);

  assert(!_g1h->is_in_cset((HeapWord*)p),
         "Oop originates from " PTR_FORMAT " (region: %u) which is in the collection set.",
         p2i(p), _g1h->addr_to_region((HeapWord*)p));

  const G1HeapRegionAttr region_attr = _g1h->region_attr(obj);
  if (region_attr.is_in_cset()) {
    // Since the source is always from outside the collection set, here we implicitly know
    // that this is a cross-region reference too.
    prefetch_and_push(p, obj);
  } else if (!HeapRegion::is_in_same_region(p, obj)) {
    handle_non_cset_obj_common(region_attr, p, obj);
    _par_scan_state->enqueue_card_if_tracked(region_attr, p, obj);
  }
}

template <class T>
inline void G1ScanRSForOptionalClosure::do_oop_work(T* p) {
  const G1HeapRegionAttr region_attr = _g1h->region_attr(p);
  // Entries in the optional collection set may start to originate from the collection
  // set after one or more increments. In this case, previously optional regions
  // became actual collection set regions. Filter them out here.
  if (region_attr.is_in_cset()) {
    return;
  }
  _scan_cl->do_oop_work(p);
  _scan_cl->trim_queue_partially();
}

void G1ParCopyHelper::do_cld_barrier(oop new_obj) {
  if (_g1h->heap_region_containing(new_obj)->is_young()) {
    _scanned_cld->record_modified_oops();
  }
}

void G1ParCopyHelper::mark_object(oop obj) {
  assert(!_g1h->heap_region_containing(obj)->in_collection_set(), "should not mark objects in the CSet");

  // We know that the object is not moving so it's safe to read its size.
  _cm->mark_in_next_bitmap(_worker_id, obj);
}

void G1ParCopyHelper::trim_queue_partially() {
  _par_scan_state->trim_queue_partially();
}

template <G1Barrier barrier, bool should_mark>
template <class T>
void G1ParCopyClosure<barrier, should_mark>::do_oop_work(T* p) {
  T heap_oop = RawAccess<>::oop_load(p);

  if (CompressedOops::is_null(heap_oop)) {
    return;
  }

  oop obj = CompressedOops::decode_not_null(heap_oop);

  assert(_worker_id == _par_scan_state->worker_id(), "sanity");

  const G1HeapRegionAttr state = _g1h->region_attr(obj);
  if (state.is_in_cset()) {
    oop forwardee;
    markWord m = obj->mark();
    if (m.is_marked()) {
      forwardee = cast_to_oop(m.decode_pointer());
    } else {
      forwardee = _par_scan_state->copy_to_survivor_space(state, obj, m);
    }
    assert(forwardee != NULL, "forwardee should not be NULL");
    RawAccess<IS_NOT_NULL>::oop_store(p, forwardee);

    if (barrier == G1BarrierCLD) {
      do_cld_barrier(forwardee);
    }
  } else {
    if (state.is_humongous()) {
      _g1h->set_humongous_is_live(obj);
    } else if ((barrier != G1BarrierNoOptRoots) && state.is_optional()) {
      _par_scan_state->remember_root_into_optional_region(p);
    }

    // The object is not in the collection set. should_mark is true iff the
    // current closure is applied on strong roots (and weak roots when class
    // unloading is disabled) in a concurrent mark start pause.
    if (should_mark) {
      mark_object(obj);
    }
  }
  trim_queue_partially();
}

template <class T> void G1RebuildRemSetClosure::do_oop_work(T* p) {
  oop const obj = RawAccess<MO_RELAXED>::oop_load(p);
  if (obj == NULL) {
    return;
  }

  if (HeapRegion::is_in_same_region(p, obj)) {
    return;
  }

  HeapRegion* to = _g1h->heap_region_containing(obj);
  HeapRegionRemSet* rem_set = to->rem_set();
  rem_set->add_reference(p, _worker_id);
}

#endif // SHARE_GC_G1_G1OOPCLOSURES_INLINE_HPP
