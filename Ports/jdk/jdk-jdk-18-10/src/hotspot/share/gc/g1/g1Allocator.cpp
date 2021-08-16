/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/g1/g1Allocator.inline.hpp"
#include "gc/g1/g1AllocRegion.inline.hpp"
#include "gc/g1/g1EvacStats.inline.hpp"
#include "gc/g1/g1EvacuationInfo.hpp"
#include "gc/g1/g1CollectedHeap.inline.hpp"
#include "gc/g1/g1NUMA.hpp"
#include "gc/g1/g1Policy.hpp"
#include "gc/g1/heapRegion.inline.hpp"
#include "gc/g1/heapRegionSet.inline.hpp"
#include "gc/g1/heapRegionType.hpp"
#include "gc/shared/tlab_globals.hpp"
#include "utilities/align.hpp"

G1Allocator::G1Allocator(G1CollectedHeap* heap) :
  _g1h(heap),
  _numa(heap->numa()),
  _survivor_is_full(false),
  _old_is_full(false),
  _num_alloc_regions(_numa->num_active_nodes()),
  _mutator_alloc_regions(NULL),
  _survivor_gc_alloc_regions(NULL),
  _old_gc_alloc_region(heap->alloc_buffer_stats(G1HeapRegionAttr::Old)),
  _retained_old_gc_alloc_region(NULL) {

  _mutator_alloc_regions = NEW_C_HEAP_ARRAY(MutatorAllocRegion, _num_alloc_regions, mtGC);
  _survivor_gc_alloc_regions = NEW_C_HEAP_ARRAY(SurvivorGCAllocRegion, _num_alloc_regions, mtGC);
  G1EvacStats* stat = heap->alloc_buffer_stats(G1HeapRegionAttr::Young);

  for (uint i = 0; i < _num_alloc_regions; i++) {
    ::new(_mutator_alloc_regions + i) MutatorAllocRegion(i);
    ::new(_survivor_gc_alloc_regions + i) SurvivorGCAllocRegion(stat, i);
  }
}

G1Allocator::~G1Allocator() {
  for (uint i = 0; i < _num_alloc_regions; i++) {
    _mutator_alloc_regions[i].~MutatorAllocRegion();
    _survivor_gc_alloc_regions[i].~SurvivorGCAllocRegion();
  }
  FREE_C_HEAP_ARRAY(MutatorAllocRegion, _mutator_alloc_regions);
  FREE_C_HEAP_ARRAY(SurvivorGCAllocRegion, _survivor_gc_alloc_regions);
}

#ifdef ASSERT
bool G1Allocator::has_mutator_alloc_region() {
  uint node_index = current_node_index();
  return mutator_alloc_region(node_index)->get() != NULL;
}
#endif

void G1Allocator::init_mutator_alloc_regions() {
  for (uint i = 0; i < _num_alloc_regions; i++) {
    assert(mutator_alloc_region(i)->get() == NULL, "pre-condition");
    mutator_alloc_region(i)->init();
  }
}

void G1Allocator::release_mutator_alloc_regions() {
  for (uint i = 0; i < _num_alloc_regions; i++) {
    mutator_alloc_region(i)->release();
    assert(mutator_alloc_region(i)->get() == NULL, "post-condition");
  }
}

bool G1Allocator::is_retained_old_region(HeapRegion* hr) {
  return _retained_old_gc_alloc_region == hr;
}

void G1Allocator::reuse_retained_old_region(G1EvacuationInfo* evacuation_info,
                                            OldGCAllocRegion* old,
                                            HeapRegion** retained_old) {
  HeapRegion* retained_region = *retained_old;
  *retained_old = NULL;
  assert(retained_region == NULL || !retained_region->is_archive(),
         "Archive region should not be alloc region (index %u)", retained_region->hrm_index());

  // We will discard the current GC alloc region if:
  // a) it's in the collection set (it can happen!),
  // b) it's already full (no point in using it),
  // c) it's empty (this means that it was emptied during
  // a cleanup and it should be on the free list now), or
  // d) it's humongous (this means that it was emptied
  // during a cleanup and was added to the free list, but
  // has been subsequently used to allocate a humongous
  // object that may be less than the region size).
  if (retained_region != NULL &&
      !retained_region->in_collection_set() &&
      !(retained_region->top() == retained_region->end()) &&
      !retained_region->is_empty() &&
      !retained_region->is_humongous()) {
    // The retained region was added to the old region set when it was
    // retired. We have to remove it now, since we don't allow regions
    // we allocate to in the region sets. We'll re-add it later, when
    // it's retired again.
    _g1h->old_set_remove(retained_region);
    old->set(retained_region);
    _g1h->hr_printer()->reuse(retained_region);
    evacuation_info->set_alloc_regions_used_before(retained_region->used());
  }
}

void G1Allocator::init_gc_alloc_regions(G1EvacuationInfo* evacuation_info) {
  assert_at_safepoint_on_vm_thread();

  _survivor_is_full = false;
  _old_is_full = false;

  for (uint i = 0; i < _num_alloc_regions; i++) {
    survivor_gc_alloc_region(i)->init();
  }

  _old_gc_alloc_region.init();
  reuse_retained_old_region(evacuation_info,
                            &_old_gc_alloc_region,
                            &_retained_old_gc_alloc_region);
}

void G1Allocator::release_gc_alloc_regions(G1EvacuationInfo* evacuation_info) {
  uint survivor_region_count = 0;
  for (uint node_index = 0; node_index < _num_alloc_regions; node_index++) {
    survivor_region_count += survivor_gc_alloc_region(node_index)->count();
    survivor_gc_alloc_region(node_index)->release();
  }
  evacuation_info->set_allocation_regions(survivor_region_count +
                                          old_gc_alloc_region()->count());

  // If we have an old GC alloc region to release, we'll save it in
  // _retained_old_gc_alloc_region. If we don't
  // _retained_old_gc_alloc_region will become NULL. This is what we
  // want either way so no reason to check explicitly for either
  // condition.
  _retained_old_gc_alloc_region = old_gc_alloc_region()->release();
}

void G1Allocator::abandon_gc_alloc_regions() {
  for (uint i = 0; i < _num_alloc_regions; i++) {
    assert(survivor_gc_alloc_region(i)->get() == NULL, "pre-condition");
  }
  assert(old_gc_alloc_region()->get() == NULL, "pre-condition");
  _retained_old_gc_alloc_region = NULL;
}

bool G1Allocator::survivor_is_full() const {
  return _survivor_is_full;
}

bool G1Allocator::old_is_full() const {
  return _old_is_full;
}

void G1Allocator::set_survivor_full() {
  _survivor_is_full = true;
}

void G1Allocator::set_old_full() {
  _old_is_full = true;
}

size_t G1Allocator::unsafe_max_tlab_alloc() {
  // Return the remaining space in the cur alloc region, but not less than
  // the min TLAB size.

  // Also, this value can be at most the humongous object threshold,
  // since we can't allow tlabs to grow big enough to accommodate
  // humongous objects.

  uint node_index = current_node_index();
  HeapRegion* hr = mutator_alloc_region(node_index)->get();
  size_t max_tlab = _g1h->max_tlab_size() * wordSize;
  if (hr == NULL) {
    return max_tlab;
  } else {
    return clamp(hr->free(), MinTLABSize, max_tlab);
  }
}

size_t G1Allocator::used_in_alloc_regions() {
  assert(Heap_lock->owner() != NULL, "Should be owned on this thread's behalf.");
  size_t used = 0;
  for (uint i = 0; i < _num_alloc_regions; i++) {
    used += mutator_alloc_region(i)->used_in_alloc_regions();
  }
  return used;
}


HeapWord* G1Allocator::par_allocate_during_gc(G1HeapRegionAttr dest,
                                              size_t word_size,
                                              uint node_index) {
  size_t temp = 0;
  HeapWord* result = par_allocate_during_gc(dest, word_size, word_size, &temp, node_index);
  assert(result == NULL || temp == word_size,
         "Requested " SIZE_FORMAT " words, but got " SIZE_FORMAT " at " PTR_FORMAT,
         word_size, temp, p2i(result));
  return result;
}

HeapWord* G1Allocator::par_allocate_during_gc(G1HeapRegionAttr dest,
                                              size_t min_word_size,
                                              size_t desired_word_size,
                                              size_t* actual_word_size,
                                              uint node_index) {
  switch (dest.type()) {
    case G1HeapRegionAttr::Young:
      return survivor_attempt_allocation(min_word_size, desired_word_size, actual_word_size, node_index);
    case G1HeapRegionAttr::Old:
      return old_attempt_allocation(min_word_size, desired_word_size, actual_word_size);
    default:
      ShouldNotReachHere();
      return NULL; // Keep some compilers happy
  }
}

HeapWord* G1Allocator::survivor_attempt_allocation(size_t min_word_size,
                                                   size_t desired_word_size,
                                                   size_t* actual_word_size,
                                                   uint node_index) {
  assert(!_g1h->is_humongous(desired_word_size),
         "we should not be seeing humongous-size allocations in this path");

  HeapWord* result = survivor_gc_alloc_region(node_index)->attempt_allocation(min_word_size,
                                                                              desired_word_size,
                                                                              actual_word_size);
  if (result == NULL && !survivor_is_full()) {
    MutexLocker x(FreeList_lock, Mutex::_no_safepoint_check_flag);
    result = survivor_gc_alloc_region(node_index)->attempt_allocation_locked(min_word_size,
                                                                             desired_word_size,
                                                                             actual_word_size);
    if (result == NULL) {
      set_survivor_full();
    }
  }
  if (result != NULL) {
    _g1h->dirty_young_block(result, *actual_word_size);
  }
  return result;
}

HeapWord* G1Allocator::old_attempt_allocation(size_t min_word_size,
                                              size_t desired_word_size,
                                              size_t* actual_word_size) {
  assert(!_g1h->is_humongous(desired_word_size),
         "we should not be seeing humongous-size allocations in this path");

  HeapWord* result = old_gc_alloc_region()->attempt_allocation(min_word_size,
                                                               desired_word_size,
                                                               actual_word_size);
  if (result == NULL && !old_is_full()) {
    MutexLocker x(FreeList_lock, Mutex::_no_safepoint_check_flag);
    result = old_gc_alloc_region()->attempt_allocation_locked(min_word_size,
                                                              desired_word_size,
                                                              actual_word_size);
    if (result == NULL) {
      set_old_full();
    }
  }
  return result;
}

G1PLABAllocator::G1PLABAllocator(G1Allocator* allocator) :
  _g1h(G1CollectedHeap::heap()),
  _allocator(allocator) {
  for (region_type_t state = 0; state < G1HeapRegionAttr::Num; state++) {
    _direct_allocated[state] = 0;
    uint length = alloc_buffers_length(state);
    _alloc_buffers[state] = NEW_C_HEAP_ARRAY(PLAB*, length, mtGC);
    for (uint node_index = 0; node_index < length; node_index++) {
      _alloc_buffers[state][node_index] = new PLAB(_g1h->desired_plab_sz(state));
    }
  }
}

G1PLABAllocator::~G1PLABAllocator() {
  for (region_type_t state = 0; state < G1HeapRegionAttr::Num; state++) {
    uint length = alloc_buffers_length(state);
    for (uint node_index = 0; node_index < length; node_index++) {
      delete _alloc_buffers[state][node_index];
    }
    FREE_C_HEAP_ARRAY(PLAB*, _alloc_buffers[state]);
  }
}

bool G1PLABAllocator::may_throw_away_buffer(size_t const allocation_word_sz, size_t const buffer_size) const {
  return (allocation_word_sz * 100 < buffer_size * ParallelGCBufferWastePct);
}

HeapWord* G1PLABAllocator::allocate_direct_or_new_plab(G1HeapRegionAttr dest,
                                                       size_t word_sz,
                                                       bool* plab_refill_failed,
                                                       uint node_index) {
  size_t plab_word_size = _g1h->desired_plab_sz(dest);
  size_t required_in_plab = PLAB::size_required_for_allocation(word_sz);

  // Only get a new PLAB if the allocation fits and it would not waste more than
  // ParallelGCBufferWastePct in the existing buffer.
  if ((required_in_plab <= plab_word_size) &&
    may_throw_away_buffer(required_in_plab, plab_word_size)) {

    PLAB* alloc_buf = alloc_buffer(dest, node_index);
    alloc_buf->retire();

    size_t actual_plab_size = 0;
    HeapWord* buf = _allocator->par_allocate_during_gc(dest,
                                                       required_in_plab,
                                                       plab_word_size,
                                                       &actual_plab_size,
                                                       node_index);

    assert(buf == NULL || ((actual_plab_size >= required_in_plab) && (actual_plab_size <= plab_word_size)),
           "Requested at minimum " SIZE_FORMAT ", desired " SIZE_FORMAT " words, but got " SIZE_FORMAT " at " PTR_FORMAT,
           required_in_plab, plab_word_size, actual_plab_size, p2i(buf));

    if (buf != NULL) {
      alloc_buf->set_buf(buf, actual_plab_size);

      HeapWord* const obj = alloc_buf->allocate(word_sz);
      assert(obj != NULL, "PLAB should have been big enough, tried to allocate "
                          SIZE_FORMAT " requiring " SIZE_FORMAT " PLAB size " SIZE_FORMAT,
                          word_sz, required_in_plab, plab_word_size);
      return obj;
    }
    // Otherwise.
    *plab_refill_failed = true;
  }
  // Try direct allocation.
  HeapWord* result = _allocator->par_allocate_during_gc(dest, word_sz, node_index);
  if (result != NULL) {
    _direct_allocated[dest.type()] += word_sz;
  }
  return result;
}

void G1PLABAllocator::undo_allocation(G1HeapRegionAttr dest, HeapWord* obj, size_t word_sz, uint node_index) {
  alloc_buffer(dest, node_index)->undo_allocation(obj, word_sz);
}

void G1PLABAllocator::flush_and_retire_stats() {
  for (region_type_t state = 0; state < G1HeapRegionAttr::Num; state++) {
    G1EvacStats* stats = _g1h->alloc_buffer_stats(state);
    for (uint node_index = 0; node_index < alloc_buffers_length(state); node_index++) {
      PLAB* const buf = alloc_buffer(state, node_index);
      if (buf != NULL) {
        buf->flush_and_retire_stats(stats);
      }
    }
    stats->add_direct_allocated(_direct_allocated[state]);
    _direct_allocated[state] = 0;
  }
}

size_t G1PLABAllocator::waste() const {
  size_t result = 0;
  for (region_type_t state = 0; state < G1HeapRegionAttr::Num; state++) {
    for (uint node_index = 0; node_index < alloc_buffers_length(state); node_index++) {
      PLAB* const buf = alloc_buffer(state, node_index);
      if (buf != NULL) {
        result += buf->waste();
      }
    }
  }
  return result;
}

size_t G1PLABAllocator::undo_waste() const {
  size_t result = 0;
  for (region_type_t state = 0; state < G1HeapRegionAttr::Num; state++) {
    for (uint node_index = 0; node_index < alloc_buffers_length(state); node_index++) {
      PLAB* const buf = alloc_buffer(state, node_index);
      if (buf != NULL) {
        result += buf->undo_waste();
      }
    }
  }
  return result;
}

G1ArchiveAllocator* G1ArchiveAllocator::create_allocator(G1CollectedHeap* g1h, bool open) {
  return new G1ArchiveAllocator(g1h, open);
}

bool G1ArchiveAllocator::alloc_new_region() {
  // Allocate the highest free region in the reserved heap,
  // and add it to our list of allocated regions. It is marked
  // archive and added to the old set.
  HeapRegion* hr = _g1h->alloc_highest_free_region();
  if (hr == NULL) {
    return false;
  }
  assert(hr->is_empty(), "expected empty region (index %u)", hr->hrm_index());
  if (_open) {
    hr->set_open_archive();
  } else {
    hr->set_closed_archive();
  }
  _g1h->policy()->remset_tracker()->update_at_allocate(hr);
  _g1h->archive_set_add(hr);
  _g1h->hr_printer()->alloc(hr);
  _allocated_regions.append(hr);
  _allocation_region = hr;

  // Set up _bottom and _max to begin allocating in the lowest
  // min_region_size'd chunk of the allocated G1 region.
  _bottom = hr->bottom();
  _max = _bottom + HeapRegion::min_region_size_in_words();

  // Since we've modified the old set, call update_sizes.
  _g1h->monitoring_support()->update_sizes();
  return true;
}

HeapWord* G1ArchiveAllocator::archive_mem_allocate(size_t word_size) {
  assert(word_size != 0, "size must not be zero");
  if (_allocation_region == NULL) {
    if (!alloc_new_region()) {
      return NULL;
    }
  }
  HeapWord* old_top = _allocation_region->top();
  assert(_bottom >= _allocation_region->bottom(),
         "inconsistent allocation state: " PTR_FORMAT " < " PTR_FORMAT,
         p2i(_bottom), p2i(_allocation_region->bottom()));
  assert(_max <= _allocation_region->end(),
         "inconsistent allocation state: " PTR_FORMAT " > " PTR_FORMAT,
         p2i(_max), p2i(_allocation_region->end()));
  assert(_bottom <= old_top && old_top <= _max,
         "inconsistent allocation state: expected "
         PTR_FORMAT " <= " PTR_FORMAT " <= " PTR_FORMAT,
         p2i(_bottom), p2i(old_top), p2i(_max));

  // Try to allocate word_size in the current allocation chunk. Two cases
  // require special treatment:
  // 1. no enough space for word_size
  // 2. after allocating word_size, there's non-zero space left, but too small for the minimal filler
  // In both cases, we retire the current chunk and move on to the next one.
  size_t free_words = pointer_delta(_max, old_top);
  if (free_words < word_size ||
      ((free_words - word_size != 0) && (free_words - word_size < CollectedHeap::min_fill_size()))) {
    // Retiring the current chunk
    if (old_top != _max) {
      // Non-zero space; need to insert the filler
      size_t fill_size = free_words;
      CollectedHeap::fill_with_object(old_top, fill_size);
      _summary_bytes_used += fill_size * HeapWordSize;
    }
    // Set the current chunk as "full"
    _allocation_region->set_top(_max);

    // Check if we've just used up the last min_region_size'd chunk
    // in the current region, and if so, allocate a new one.
    if (_max != _allocation_region->end()) {
      // Shift to the next chunk
      old_top = _bottom = _max;
      _max = _bottom + HeapRegion::min_region_size_in_words();
    } else {
      if (!alloc_new_region()) {
        return NULL;
      }
      old_top = _allocation_region->bottom();
    }
  }
  assert(pointer_delta(_max, old_top) >= word_size, "enough space left");
  _allocation_region->set_top(old_top + word_size);
  _summary_bytes_used += word_size * HeapWordSize;

  return old_top;
}

void G1ArchiveAllocator::complete_archive(GrowableArray<MemRegion>* ranges,
                                          size_t end_alignment_in_bytes) {
  assert((end_alignment_in_bytes >> LogHeapWordSize) < HeapRegion::min_region_size_in_words(),
         "alignment " SIZE_FORMAT " too large", end_alignment_in_bytes);
  assert(is_aligned(end_alignment_in_bytes, HeapWordSize),
         "alignment " SIZE_FORMAT " is not HeapWord (%u) aligned", end_alignment_in_bytes, HeapWordSize);

  // If we've allocated nothing, simply return.
  if (_allocation_region == NULL) {
    return;
  }

  // If an end alignment was requested, insert filler objects.
  if (end_alignment_in_bytes != 0) {
    HeapWord* currtop = _allocation_region->top();
    HeapWord* newtop = align_up(currtop, end_alignment_in_bytes);
    size_t fill_size = pointer_delta(newtop, currtop);
    if (fill_size != 0) {
      if (fill_size < CollectedHeap::min_fill_size()) {
        // If the required fill is smaller than we can represent,
        // bump up to the next aligned address. We know we won't exceed the current
        // region boundary because the max supported alignment is smaller than the min
        // region size, and because the allocation code never leaves space smaller than
        // the min_fill_size at the top of the current allocation region.
        newtop = align_up(currtop + CollectedHeap::min_fill_size(),
                          end_alignment_in_bytes);
        fill_size = pointer_delta(newtop, currtop);
      }
      HeapWord* fill = archive_mem_allocate(fill_size);
      CollectedHeap::fill_with_objects(fill, fill_size);
    }
  }

  // Loop through the allocated regions, and create MemRegions summarizing
  // the allocated address range, combining contiguous ranges. Add the
  // MemRegions to the GrowableArray provided by the caller.
  int index = _allocated_regions.length() - 1;
  assert(_allocated_regions.at(index) == _allocation_region,
         "expected region %u at end of array, found %u",
         _allocation_region->hrm_index(), _allocated_regions.at(index)->hrm_index());
  HeapWord* base_address = _allocation_region->bottom();
  HeapWord* top = base_address;

  while (index >= 0) {
    HeapRegion* next = _allocated_regions.at(index);
    HeapWord* new_base = next->bottom();
    HeapWord* new_top = next->top();
    if (new_base != top) {
      ranges->append(MemRegion(base_address, pointer_delta(top, base_address)));
      base_address = new_base;
    }
    top = new_top;
    index = index - 1;
  }

  assert(top != base_address, "zero-sized range, address " PTR_FORMAT, p2i(base_address));
  ranges->append(MemRegion(base_address, pointer_delta(top, base_address)));
  _allocated_regions.clear();
  _allocation_region = NULL;
};
