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

#ifndef SHARE_GC_G1_G1COLLECTEDHEAP_INLINE_HPP
#define SHARE_GC_G1_G1COLLECTEDHEAP_INLINE_HPP

#include "gc/g1/g1CollectedHeap.hpp"

#include "gc/g1/g1BarrierSet.hpp"
#include "gc/g1/g1CollectorState.hpp"
#include "gc/g1/g1Policy.hpp"
#include "gc/g1/g1RemSet.hpp"
#include "gc/g1/heapRegionManager.inline.hpp"
#include "gc/g1/heapRegionRemSet.hpp"
#include "gc/g1/heapRegionSet.inline.hpp"
#include "gc/shared/markBitMap.inline.hpp"
#include "gc/shared/taskqueue.inline.hpp"
#include "runtime/atomic.hpp"
#include "utilities/bitMap.inline.hpp"

G1GCPhaseTimes* G1CollectedHeap::phase_times() const {
  return _policy->phase_times();
}

G1EvacStats* G1CollectedHeap::alloc_buffer_stats(G1HeapRegionAttr dest) {
  switch (dest.type()) {
    case G1HeapRegionAttr::Young:
      return &_survivor_evac_stats;
    case G1HeapRegionAttr::Old:
      return &_old_evac_stats;
    default:
      ShouldNotReachHere();
      return NULL; // Keep some compilers happy
  }
}

size_t G1CollectedHeap::desired_plab_sz(G1HeapRegionAttr dest) {
  size_t gclab_word_size = alloc_buffer_stats(dest)->desired_plab_sz(workers()->active_workers());
  // Prevent humongous PLAB sizes for two reasons:
  // * PLABs are allocated using a similar paths as oops, but should
  //   never be in a humongous region
  // * Allowing humongous PLABs needlessly churns the region free lists
  return MIN2(_humongous_object_threshold_in_words, gclab_word_size);
}

// Inline functions for G1CollectedHeap

// Return the region with the given index. It assumes the index is valid.
inline HeapRegion* G1CollectedHeap::region_at(uint index) const { return _hrm.at(index); }

// Return the region with the given index, or NULL if unmapped. It assumes the index is valid.
inline HeapRegion* G1CollectedHeap::region_at_or_null(uint index) const { return _hrm.at_or_null(index); }

inline HeapRegion* G1CollectedHeap::next_region_in_humongous(HeapRegion* hr) const {
  return _hrm.next_region_in_humongous(hr);
}

inline uint G1CollectedHeap::addr_to_region(HeapWord* addr) const {
  assert(is_in_reserved(addr),
         "Cannot calculate region index for address " PTR_FORMAT " that is outside of the heap [" PTR_FORMAT ", " PTR_FORMAT ")",
         p2i(addr), p2i(reserved().start()), p2i(reserved().end()));
  return (uint)(pointer_delta(addr, reserved().start(), sizeof(uint8_t)) >> HeapRegion::LogOfHRGrainBytes);
}

inline HeapWord* G1CollectedHeap::bottom_addr_for_region(uint index) const {
  return _hrm.reserved().start() + index * HeapRegion::GrainWords;
}

template <class T>
inline HeapRegion* G1CollectedHeap::heap_region_containing(const T addr) const {
  assert(addr != NULL, "invariant");
  assert(is_in_reserved((const void*) addr),
         "Address " PTR_FORMAT " is outside of the heap ranging from [" PTR_FORMAT " to " PTR_FORMAT ")",
         p2i((void*)addr), p2i(reserved().start()), p2i(reserved().end()));
  return _hrm.addr_to_region((HeapWord*)(void*) addr);
}

template <class T>
inline HeapRegion* G1CollectedHeap::heap_region_containing_or_null(const T addr) const {
  assert(addr != NULL, "invariant");
  assert(is_in_reserved((const void*) addr),
         "Address " PTR_FORMAT " is outside of the heap ranging from [" PTR_FORMAT " to " PTR_FORMAT ")",
         p2i((void*)addr), p2i(reserved().start()), p2i(reserved().end()));
  uint const region_idx = addr_to_region(addr);
  return region_at_or_null(region_idx);
}

inline void G1CollectedHeap::old_set_add(HeapRegion* hr) {
  _old_set.add(hr);
}

inline void G1CollectedHeap::old_set_remove(HeapRegion* hr) {
  _old_set.remove(hr);
}

inline void G1CollectedHeap::archive_set_add(HeapRegion* hr) {
  _archive_set.add(hr);
}

// It dirties the cards that cover the block so that the post
// write barrier never queues anything when updating objects on this
// block. It is assumed (and in fact we assert) that the block
// belongs to a young region.
inline void
G1CollectedHeap::dirty_young_block(HeapWord* start, size_t word_size) {
  assert_heap_not_locked();

  // Assign the containing region to containing_hr so that we don't
  // have to keep calling heap_region_containing() in the
  // asserts below.
  DEBUG_ONLY(HeapRegion* containing_hr = heap_region_containing(start);)
  assert(word_size > 0, "pre-condition");
  assert(containing_hr->is_in(start), "it should contain start");
  assert(containing_hr->is_young(), "it should be young");
  assert(!containing_hr->is_humongous(), "it should not be humongous");

  HeapWord* end = start + word_size;
  assert(containing_hr->is_in(end - 1), "it should also contain end - 1");

  MemRegion mr(start, end);
  card_table()->g1_mark_as_young(mr);
}

inline G1ScannerTasksQueue* G1CollectedHeap::task_queue(uint i) const {
  return _task_queues->queue(i);
}

inline bool G1CollectedHeap::is_marked_next(oop obj) const {
  return _cm->next_mark_bitmap()->is_marked(obj);
}

inline bool G1CollectedHeap::is_in_cset(oop obj) {
  return is_in_cset(cast_from_oop<HeapWord*>(obj));
}

inline bool G1CollectedHeap::is_in_cset(HeapWord* addr) {
  return _region_attr.is_in_cset(addr);
}

bool G1CollectedHeap::is_in_cset(const HeapRegion* hr) {
  return _region_attr.is_in_cset(hr);
}

bool G1CollectedHeap::is_in_cset_or_humongous(const oop obj) {
  return _region_attr.is_in_cset_or_humongous(cast_from_oop<HeapWord*>(obj));
}

G1HeapRegionAttr G1CollectedHeap::region_attr(const void* addr) const {
  return _region_attr.at((HeapWord*)addr);
}

G1HeapRegionAttr G1CollectedHeap::region_attr(uint idx) const {
  return _region_attr.get_by_index(idx);
}

void G1CollectedHeap::register_humongous_region_with_region_attr(uint index) {
  _region_attr.set_humongous(index, region_at(index)->rem_set()->is_tracked());
}

void G1CollectedHeap::register_region_with_region_attr(HeapRegion* r) {
  _region_attr.set_has_remset(r->hrm_index(), r->rem_set()->is_tracked());
}

void G1CollectedHeap::register_old_region_with_region_attr(HeapRegion* r) {
  _region_attr.set_in_old(r->hrm_index(), r->rem_set()->is_tracked());
  _rem_set->exclude_region_from_scan(r->hrm_index());
}

void G1CollectedHeap::register_optional_region_with_region_attr(HeapRegion* r) {
  _region_attr.set_optional(r->hrm_index(), r->rem_set()->is_tracked());
}

bool G1CollectedHeap::evacuation_failed() const {
  return num_regions_failed_evacuation() > 0;
}

bool G1CollectedHeap::evacuation_failed(uint region_idx) const {
  return _regions_failed_evacuation.par_at(region_idx, memory_order_relaxed);
}

uint G1CollectedHeap::num_regions_failed_evacuation() const {
  return Atomic::load(&_num_regions_failed_evacuation);
}

bool G1CollectedHeap::notify_region_failed_evacuation(uint const region_idx) {
  bool result = _regions_failed_evacuation.par_set_bit(region_idx, memory_order_relaxed);
  if (result) {
    Atomic::inc(&_num_regions_failed_evacuation, memory_order_relaxed);
  }
  return result;
}

#ifndef PRODUCT
// Support for G1EvacuationFailureALot

inline bool
G1CollectedHeap::evacuation_failure_alot_for_gc_type(bool for_young_gc,
                                                     bool during_concurrent_start,
                                                     bool mark_or_rebuild_in_progress) {
  bool res = false;
  if (mark_or_rebuild_in_progress) {
    res |= G1EvacuationFailureALotDuringConcMark;
  }
  if (during_concurrent_start) {
    res |= G1EvacuationFailureALotDuringConcurrentStart;
  }
  if (for_young_gc) {
    res |= G1EvacuationFailureALotDuringYoungGC;
  } else {
    // GCs are mixed
    res |= G1EvacuationFailureALotDuringMixedGC;
  }
  return res;
}

inline void
G1CollectedHeap::set_evacuation_failure_alot_for_current_gc() {
  if (G1EvacuationFailureALot) {
    // Note we can't assert that _evacuation_failure_alot_for_current_gc
    // is clear here. It may have been set during a previous GC but that GC
    // did not copy enough objects (i.e. G1EvacuationFailureALotCount) to
    // trigger an evacuation failure and clear the flags and and counts.

    // Check if we have gone over the interval.
    const size_t gc_num = total_collections();
    const size_t elapsed_gcs = gc_num - _evacuation_failure_alot_gc_number;

    _evacuation_failure_alot_for_current_gc = (elapsed_gcs >= G1EvacuationFailureALotInterval);

    // Now check if G1EvacuationFailureALot is enabled for the current GC type.
    const bool in_young_only_phase = collector_state()->in_young_only_phase();
    const bool in_concurrent_start_gc = collector_state()->in_concurrent_start_gc();
    const bool mark_or_rebuild_in_progress = collector_state()->mark_or_rebuild_in_progress();

    _evacuation_failure_alot_for_current_gc &=
      evacuation_failure_alot_for_gc_type(in_young_only_phase,
                                          in_concurrent_start_gc,
                                          mark_or_rebuild_in_progress);
  }
}

inline bool G1CollectedHeap::evacuation_should_fail() {
  if (!G1EvacuationFailureALot || !_evacuation_failure_alot_for_current_gc) {
    return false;
  }
  // G1EvacuationFailureALot is in effect for current GC
  // Access to _evacuation_failure_alot_count is not atomic;
  // the value does not have to be exact.
  if (++_evacuation_failure_alot_count < G1EvacuationFailureALotCount) {
    return false;
  }
  _evacuation_failure_alot_count = 0;
  return true;
}

inline void G1CollectedHeap::reset_evacuation_should_fail() {
  if (G1EvacuationFailureALot) {
    _evacuation_failure_alot_gc_number = total_collections();
    _evacuation_failure_alot_count = 0;
    _evacuation_failure_alot_for_current_gc = false;
  }
}
#endif  // #ifndef PRODUCT

inline bool G1CollectedHeap::is_in_young(const oop obj) {
  if (obj == NULL) {
    return false;
  }
  return heap_region_containing(obj)->is_young();
}

inline bool G1CollectedHeap::is_obj_dead(const oop obj) const {
  if (obj == NULL) {
    return false;
  }
  return is_obj_dead(obj, heap_region_containing(obj));
}

inline bool G1CollectedHeap::is_obj_ill(const oop obj) const {
  if (obj == NULL) {
    return false;
  }
  return is_obj_ill(obj, heap_region_containing(obj));
}

inline bool G1CollectedHeap::is_obj_dead_full(const oop obj, const HeapRegion* hr) const {
   return !is_marked_next(obj) && !hr->is_closed_archive();
}

inline bool G1CollectedHeap::is_obj_dead_full(const oop obj) const {
    return is_obj_dead_full(obj, heap_region_containing(obj));
}

inline void G1CollectedHeap::set_humongous_reclaim_candidate(uint region, bool value) {
  assert(_hrm.at(region)->is_starts_humongous(), "Must start a humongous object");
  _humongous_reclaim_candidates.set_candidate(region, value);
}

inline bool G1CollectedHeap::is_humongous_reclaim_candidate(uint region) {
  assert(_hrm.at(region)->is_starts_humongous(), "Must start a humongous object");
  return _humongous_reclaim_candidates.is_candidate(region);
}

inline void G1CollectedHeap::set_humongous_is_live(oop obj) {
  uint region = addr_to_region(cast_from_oop<HeapWord*>(obj));
  // Clear the flag in the humongous_reclaim_candidates table.  Also
  // reset the entry in the region attribute table so that subsequent references
  // to the same humongous object do not go into the slow path again.
  // This is racy, as multiple threads may at the same time enter here, but this
  // is benign.
  // During collection we only ever clear the "candidate" flag, and only ever clear the
  // entry in the in_cset_fast_table.
  // We only ever evaluate the contents of these tables (in the VM thread) after
  // having synchronized the worker threads with the VM thread, or in the same
  // thread (i.e. within the VM thread).
  if (is_humongous_reclaim_candidate(region)) {
    set_humongous_reclaim_candidate(region, false);
    _region_attr.clear_humongous(region);
  }
}

#endif // SHARE_GC_G1_G1COLLECTEDHEAP_INLINE_HPP
