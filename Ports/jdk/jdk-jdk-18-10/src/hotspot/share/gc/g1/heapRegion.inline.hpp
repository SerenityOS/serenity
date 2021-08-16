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

#ifndef SHARE_GC_G1_HEAPREGION_INLINE_HPP
#define SHARE_GC_G1_HEAPREGION_INLINE_HPP

#include "gc/g1/heapRegion.hpp"

#include "gc/g1/g1BlockOffsetTable.inline.hpp"
#include "gc/g1/g1CollectedHeap.inline.hpp"
#include "gc/g1/g1ConcurrentMarkBitMap.inline.hpp"
#include "gc/g1/g1Predictions.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/atomic.hpp"
#include "runtime/prefetch.inline.hpp"
#include "utilities/align.hpp"
#include "utilities/globalDefinitions.hpp"

inline HeapWord* HeapRegion::allocate_impl(size_t min_word_size,
                                           size_t desired_word_size,
                                           size_t* actual_size) {
  HeapWord* obj = top();
  size_t available = pointer_delta(end(), obj);
  size_t want_to_allocate = MIN2(available, desired_word_size);
  if (want_to_allocate >= min_word_size) {
    HeapWord* new_top = obj + want_to_allocate;
    set_top(new_top);
    assert(is_object_aligned(obj) && is_object_aligned(new_top), "checking alignment");
    *actual_size = want_to_allocate;
    return obj;
  } else {
    return NULL;
  }
}

inline HeapWord* HeapRegion::par_allocate_impl(size_t min_word_size,
                                               size_t desired_word_size,
                                               size_t* actual_size) {
  do {
    HeapWord* obj = top();
    size_t available = pointer_delta(end(), obj);
    size_t want_to_allocate = MIN2(available, desired_word_size);
    if (want_to_allocate >= min_word_size) {
      HeapWord* new_top = obj + want_to_allocate;
      HeapWord* result = Atomic::cmpxchg(&_top, obj, new_top);
      // result can be one of two:
      //  the old top value: the exchange succeeded
      //  otherwise: the new value of the top is returned.
      if (result == obj) {
        assert(is_object_aligned(obj) && is_object_aligned(new_top), "checking alignment");
        *actual_size = want_to_allocate;
        return obj;
      }
    } else {
      return NULL;
    }
  } while (true);
}

inline HeapWord* HeapRegion::allocate(size_t min_word_size,
                                      size_t desired_word_size,
                                      size_t* actual_size) {
  HeapWord* res = allocate_impl(min_word_size, desired_word_size, actual_size);
  if (res != NULL) {
    _bot_part.alloc_block(res, *actual_size);
  }
  return res;
}

inline HeapWord* HeapRegion::allocate(size_t word_size) {
  size_t temp;
  return allocate(word_size, word_size, &temp);
}

inline HeapWord* HeapRegion::par_allocate(size_t word_size) {
  size_t temp;
  return par_allocate(word_size, word_size, &temp);
}

// Because of the requirement of keeping "_offsets" up to date with the
// allocations, we sequentialize these with a lock.  Therefore, best if
// this is used for larger LAB allocations only.
inline HeapWord* HeapRegion::par_allocate(size_t min_word_size,
                                          size_t desired_word_size,
                                          size_t* actual_size) {
  MutexLocker x(&_par_alloc_lock);
  return allocate(min_word_size, desired_word_size, actual_size);
}

inline HeapWord* HeapRegion::block_start(const void* p) {
  return _bot_part.block_start(p);
}

inline HeapWord* HeapRegion::block_start_const(const void* p) const {
  return _bot_part.block_start_const(p);
}

inline bool HeapRegion::is_obj_dead_with_size(const oop obj, const G1CMBitMap* const prev_bitmap, size_t* size) const {
  HeapWord* addr = cast_from_oop<HeapWord*>(obj);

  assert(addr < top(), "must be");
  assert(!is_closed_archive(),
         "Closed archive regions should not have references into other regions");
  assert(!is_humongous(), "Humongous objects not handled here");
  bool obj_is_dead = is_obj_dead(obj, prev_bitmap);

  if (ClassUnloading && obj_is_dead) {
    assert(!block_is_obj(addr), "must be");
    *size = block_size_using_bitmap(addr, prev_bitmap);
  } else {
    assert(block_is_obj(addr), "must be");
    *size = obj->size();
  }
  return obj_is_dead;
}

inline bool HeapRegion::block_is_obj(const HeapWord* p) const {
  G1CollectedHeap* g1h = G1CollectedHeap::heap();

  if (!this->is_in(p)) {
    assert(is_continues_humongous(), "This case can only happen for humongous regions");
    return (p == humongous_start_region()->bottom());
  }
  // When class unloading is enabled it is not safe to only consider top() to conclude if the
  // given pointer is a valid object. The situation can occur both for class unloading in a
  // Full GC and during a concurrent cycle.
  // During a Full GC regions can be excluded from compaction due to high live ratio, and
  // because of this there can be stale objects for unloaded classes left in these regions.
  // During a concurrent cycle class unloading is done after marking is complete and objects
  // for the unloaded classes will be stale until the regions are collected.
  if (ClassUnloading) {
    return !g1h->is_obj_dead(cast_to_oop(p), this);
  }
  return p < top();
}

inline size_t HeapRegion::block_size_using_bitmap(const HeapWord* addr, const G1CMBitMap* const prev_bitmap) const {
  assert(ClassUnloading,
         "All blocks should be objects if class unloading isn't used, so this method should not be called. "
         "HR: [" PTR_FORMAT ", " PTR_FORMAT ", " PTR_FORMAT ") "
         "addr: " PTR_FORMAT,
         p2i(bottom()), p2i(top()), p2i(end()), p2i(addr));

  // Old regions' dead objects may have dead classes
  // We need to find the next live object using the bitmap
  HeapWord* next = prev_bitmap->get_next_marked_addr(addr, prev_top_at_mark_start());

  assert(next > addr, "must get the next live object");
  return pointer_delta(next, addr);
}

inline bool HeapRegion::is_obj_dead(const oop obj, const G1CMBitMap* const prev_bitmap) const {
  assert(is_in_reserved(obj), "Object " PTR_FORMAT " must be in region", p2i(obj));
  return !obj_allocated_since_prev_marking(obj) &&
         !prev_bitmap->is_marked(obj) &&
         !is_closed_archive();
}

inline size_t HeapRegion::block_size(const HeapWord *addr) const {
  assert(addr < top(), "precondition");

  if (block_is_obj(addr)) {
    return cast_to_oop(addr)->size();
  }

  return block_size_using_bitmap(addr, G1CollectedHeap::heap()->concurrent_mark()->prev_mark_bitmap());
}

inline void HeapRegion::reset_compaction_top_after_compaction() {
  set_top(compaction_top());
  _compaction_top = bottom();
}

inline void HeapRegion::reset_compacted_after_full_gc() {
  assert(!is_pinned(), "must be");

  reset_compaction_top_after_compaction();
  // After a compaction the mark bitmap in a non-pinned regions is invalid.
  // We treat all objects as being above PTAMS.
  zero_marked_bytes();
  init_top_at_mark_start();

  reset_after_full_gc_common();
}

inline void HeapRegion::reset_skip_compacting_after_full_gc() {
  assert(!is_free(), "must be");

  assert(compaction_top() == bottom(),
         "region %u compaction_top " PTR_FORMAT " must not be different from bottom " PTR_FORMAT,
         hrm_index(), p2i(compaction_top()), p2i(bottom()));

  _prev_top_at_mark_start = top(); // Keep existing top and usage.
  _prev_marked_bytes = used();
  _next_top_at_mark_start = bottom();
  _next_marked_bytes = 0;

  reset_after_full_gc_common();
}

inline void HeapRegion::reset_after_full_gc_common() {
  if (is_empty()) {
    reset_bot();
  }

  // Clear unused heap memory in debug builds.
  if (ZapUnusedHeapArea) {
    mangle_unused_area();
  }
}

template<typename ApplyToMarkedClosure>
inline void HeapRegion::apply_to_marked_objects(G1CMBitMap* bitmap, ApplyToMarkedClosure* closure) {
  HeapWord* limit = top();
  HeapWord* next_addr = bottom();

  while (next_addr < limit) {
    Prefetch::write(next_addr, PrefetchScanIntervalInBytes);
    // This explicit is_marked check is a way to avoid
    // some extra work done by get_next_marked_addr for
    // the case where next_addr is marked.
    if (bitmap->is_marked(next_addr)) {
      oop current = cast_to_oop(next_addr);
      next_addr += closure->apply(current);
    } else {
      next_addr = bitmap->get_next_marked_addr(next_addr, limit);
    }
  }

  assert(next_addr == limit, "Should stop the scan at the limit.");
}

inline HeapWord* HeapRegion::par_allocate_no_bot_updates(size_t min_word_size,
                                                         size_t desired_word_size,
                                                         size_t* actual_word_size) {
  assert(is_young(), "we can only skip BOT updates on young regions");
  return par_allocate_impl(min_word_size, desired_word_size, actual_word_size);
}

inline HeapWord* HeapRegion::allocate_no_bot_updates(size_t word_size) {
  size_t temp;
  return allocate_no_bot_updates(word_size, word_size, &temp);
}

inline HeapWord* HeapRegion::allocate_no_bot_updates(size_t min_word_size,
                                                     size_t desired_word_size,
                                                     size_t* actual_word_size) {
  assert(is_young(), "we can only skip BOT updates on young regions");
  return allocate_impl(min_word_size, desired_word_size, actual_word_size);
}

inline void HeapRegion::note_start_of_marking() {
  _next_marked_bytes = 0;
  _next_top_at_mark_start = top();
  _gc_efficiency = -1.0;
}

inline void HeapRegion::note_end_of_marking() {
  _prev_top_at_mark_start = _next_top_at_mark_start;
  _next_top_at_mark_start = bottom();
  _prev_marked_bytes = _next_marked_bytes;
  _next_marked_bytes = 0;
}

inline bool HeapRegion::in_collection_set() const {
  return G1CollectedHeap::heap()->is_in_cset(this);
}

template <class Closure, bool is_gc_active>
HeapWord* HeapRegion::do_oops_on_memregion_in_humongous(MemRegion mr,
                                                        Closure* cl,
                                                        G1CollectedHeap* g1h) {
  assert(is_humongous(), "precondition");
  HeapRegion* sr = humongous_start_region();
  oop obj = cast_to_oop(sr->bottom());

  // If concurrent and klass_or_null is NULL, then space has been
  // allocated but the object has not yet been published by setting
  // the klass.  That can only happen if the card is stale.  However,
  // we've already set the card clean, so we must return failure,
  // since the allocating thread could have performed a write to the
  // card that might be missed otherwise.
  if (!is_gc_active && (obj->klass_or_null_acquire() == NULL)) {
    return NULL;
  }

  // We have a well-formed humongous object at the start of sr.
  // Only filler objects follow a humongous object in the containing
  // regions, and we can ignore those.  So only process the one
  // humongous object.
  if (g1h->is_obj_dead(obj, sr)) {
    // The object is dead. There can be no other object in this region, so return
    // the end of that region.
    return end();
  }
  if (obj->is_objArray() || (sr->bottom() < mr.start())) {
    // objArrays are always marked precisely, so limit processing
    // with mr.  Non-objArrays might be precisely marked, and since
    // it's humongous it's worthwhile avoiding full processing.
    // However, the card could be stale and only cover filler
    // objects.  That should be rare, so not worth checking for;
    // instead let it fall out from the bounded iteration.
    obj->oop_iterate(cl, mr);
    return mr.end();
  } else {
    // If obj is not an objArray and mr contains the start of the
    // obj, then this could be an imprecise mark, and we need to
    // process the entire object.
    int size = obj->oop_iterate_size(cl);
    // We have scanned to the end of the object, but since there can be no objects
    // after this humongous object in the region, we can return the end of the
    // region if it is greater.
    return MAX2(cast_from_oop<HeapWord*>(obj) + size, mr.end());
  }
}

template <bool is_gc_active, class Closure>
HeapWord* HeapRegion::oops_on_memregion_seq_iterate_careful(MemRegion mr,
                                                            Closure* cl) {
  assert(MemRegion(bottom(), end()).contains(mr), "Card region not in heap region");
  G1CollectedHeap* g1h = G1CollectedHeap::heap();

  // Special handling for humongous regions.
  if (is_humongous()) {
    return do_oops_on_memregion_in_humongous<Closure, is_gc_active>(mr, cl, g1h);
  }
  assert(is_old() || is_archive(), "Wrongly trying to iterate over region %u type %s", _hrm_index, get_type_str());

  // Because mr has been trimmed to what's been allocated in this
  // region, the parts of the heap that are examined here are always
  // parsable; there's no need to use klass_or_null to detect
  // in-progress allocation.

  // Cache the boundaries of the memory region in some const locals
  HeapWord* const start = mr.start();
  HeapWord* const end = mr.end();

  // Find the obj that extends onto mr.start().
  // Update BOT as needed while finding start of (possibly dead)
  // object containing the start of the region.
  HeapWord* cur = block_start(start);

#ifdef ASSERT
  {
    assert(cur <= start,
           "cur: " PTR_FORMAT ", start: " PTR_FORMAT, p2i(cur), p2i(start));
    HeapWord* next = cur + block_size(cur);
    assert(start < next,
           "start: " PTR_FORMAT ", next: " PTR_FORMAT, p2i(start), p2i(next));
  }
#endif

  const G1CMBitMap* const bitmap = g1h->concurrent_mark()->prev_mark_bitmap();
  while (true) {
    oop obj = cast_to_oop(cur);
    assert(oopDesc::is_oop(obj, true), "Not an oop at " PTR_FORMAT, p2i(cur));
    assert(obj->klass_or_null() != NULL,
           "Unparsable heap at " PTR_FORMAT, p2i(cur));

    size_t size;
    bool is_dead = is_obj_dead_with_size(obj, bitmap, &size);
    bool is_precise = false;

    cur += size;
    if (!is_dead) {
      // Process live object's references.

      // Non-objArrays are usually marked imprecise at the object
      // start, in which case we need to iterate over them in full.
      // objArrays are precisely marked, but can still be iterated
      // over in full if completely covered.
      if (!obj->is_objArray() || (cast_from_oop<HeapWord*>(obj) >= start && cur <= end)) {
        obj->oop_iterate(cl);
      } else {
        obj->oop_iterate(cl, mr);
        is_precise = true;
      }
    }
    if (cur >= end) {
      return is_precise ? end : cur;
    }
  }
}

inline int HeapRegion::age_in_surv_rate_group() const {
  assert(has_surv_rate_group(), "pre-condition");
  assert(has_valid_age_in_surv_rate(), "pre-condition");
  return _surv_rate_group->age_in_group(_age_index);
}

inline bool HeapRegion::has_valid_age_in_surv_rate() const {
  return G1SurvRateGroup::is_valid_age_index(_age_index);
}

inline bool HeapRegion::has_surv_rate_group() const {
  return _surv_rate_group != NULL;
}

inline double HeapRegion::surv_rate_prediction(G1Predictions const& predictor) const {
  assert(has_surv_rate_group(), "pre-condition");
  return _surv_rate_group->surv_rate_pred(predictor, age_in_surv_rate_group());
}

inline void HeapRegion::install_surv_rate_group(G1SurvRateGroup* surv_rate_group) {
  assert(surv_rate_group != NULL, "pre-condition");
  assert(!has_surv_rate_group(), "pre-condition");
  assert(is_young(), "pre-condition");

  _surv_rate_group = surv_rate_group;
  _age_index = surv_rate_group->next_age_index();
}

inline void HeapRegion::uninstall_surv_rate_group() {
  if (has_surv_rate_group()) {
    assert(has_valid_age_in_surv_rate(), "pre-condition");
    assert(is_young(), "pre-condition");

    _surv_rate_group = NULL;
    _age_index = G1SurvRateGroup::InvalidAgeIndex;
  } else {
    assert(!has_valid_age_in_surv_rate(), "pre-condition");
  }
}

inline void HeapRegion::record_surv_words_in_group(size_t words_survived) {
  assert(has_surv_rate_group(), "pre-condition");
  assert(has_valid_age_in_surv_rate(), "pre-condition");
  int age_in_group = age_in_surv_rate_group();
  _surv_rate_group->record_surviving_words(age_in_group, words_survived);
}

#endif // SHARE_GC_G1_HEAPREGION_INLINE_HPP
