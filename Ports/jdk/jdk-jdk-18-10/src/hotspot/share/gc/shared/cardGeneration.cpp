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

#include "gc/shared/blockOffsetTable.inline.hpp"
#include "gc/shared/cardGeneration.inline.hpp"
#include "gc/shared/cardTableRS.hpp"
#include "gc/shared/gcLocker.hpp"
#include "gc/shared/genCollectedHeap.hpp"
#include "gc/shared/genOopClosures.inline.hpp"
#include "gc/shared/generationSpec.hpp"
#include "gc/shared/space.inline.hpp"
#include "memory/iterator.hpp"
#include "memory/memRegion.hpp"
#include "logging/log.hpp"
#include "runtime/java.hpp"

CardGeneration::CardGeneration(ReservedSpace rs,
                               size_t initial_byte_size,
                               CardTableRS* remset) :
  Generation(rs, initial_byte_size), _rs(remset),
  _min_heap_delta_bytes(), _capacity_at_prologue(),
  _used_at_prologue()
{
  // If we don't shrink the heap in steps, '_shrink_factor' is always 100%.
  _shrink_factor = ShrinkHeapInSteps ? 0 : 100;
  HeapWord* start = (HeapWord*)rs.base();
  size_t reserved_byte_size = rs.size();
  assert((uintptr_t(start) & 3) == 0, "bad alignment");
  assert((reserved_byte_size & 3) == 0, "bad alignment");
  MemRegion reserved_mr(start, heap_word_size(reserved_byte_size));
  _bts = new BlockOffsetSharedArray(reserved_mr,
                                    heap_word_size(initial_byte_size));
  MemRegion committed_mr(start, heap_word_size(initial_byte_size));
  _rs->resize_covered_region(committed_mr);

  // Verify that the start and end of this generation is the start of a card.
  // If this wasn't true, a single card could span more than on generation,
  // which would cause problems when we commit/uncommit memory, and when we
  // clear and dirty cards.
  guarantee(_rs->is_aligned(reserved_mr.start()), "generation must be card aligned");
  if (reserved_mr.end() != GenCollectedHeap::heap()->reserved_region().end()) {
    // Don't check at the very end of the heap as we'll assert that we're probing off
    // the end if we try.
    guarantee(_rs->is_aligned(reserved_mr.end()), "generation must be card aligned");
  }
  _min_heap_delta_bytes = MinHeapDeltaBytes;
  _capacity_at_prologue = initial_byte_size;
  _used_at_prologue = 0;
}

bool CardGeneration::grow_by(size_t bytes) {
  assert_correct_size_change_locking();
  bool result = _virtual_space.expand_by(bytes);
  if (result) {
    size_t new_word_size =
       heap_word_size(_virtual_space.committed_size());
    MemRegion mr(space()->bottom(), new_word_size);
    // Expand card table
    GenCollectedHeap::heap()->rem_set()->resize_covered_region(mr);
    // Expand shared block offset array
    _bts->resize(new_word_size);

    // Fix for bug #4668531
    if (ZapUnusedHeapArea) {
      MemRegion mangle_region(space()->end(),
      (HeapWord*)_virtual_space.high());
      SpaceMangler::mangle_region(mangle_region);
    }

    // Expand space -- also expands space's BOT
    // (which uses (part of) shared array above)
    space()->set_end((HeapWord*)_virtual_space.high());

    // update the space and generation capacity counters
    update_counters();

    size_t new_mem_size = _virtual_space.committed_size();
    size_t old_mem_size = new_mem_size - bytes;
    log_trace(gc, heap)("Expanding %s from " SIZE_FORMAT "K by " SIZE_FORMAT "K to " SIZE_FORMAT "K",
                    name(), old_mem_size/K, bytes/K, new_mem_size/K);
  }
  return result;
}

bool CardGeneration::expand(size_t bytes, size_t expand_bytes) {
  assert_locked_or_safepoint(Heap_lock);
  if (bytes == 0) {
    return true;  // That's what grow_by(0) would return
  }
  size_t aligned_bytes  = ReservedSpace::page_align_size_up(bytes);
  if (aligned_bytes == 0){
    // The alignment caused the number of bytes to wrap.  An expand_by(0) will
    // return true with the implication that an expansion was done when it
    // was not.  A call to expand implies a best effort to expand by "bytes"
    // but not a guarantee.  Align down to give a best effort.  This is likely
    // the most that the generation can expand since it has some capacity to
    // start with.
    aligned_bytes = ReservedSpace::page_align_size_down(bytes);
  }
  size_t aligned_expand_bytes = ReservedSpace::page_align_size_up(expand_bytes);
  bool success = false;
  if (aligned_expand_bytes > aligned_bytes) {
    success = grow_by(aligned_expand_bytes);
  }
  if (!success) {
    success = grow_by(aligned_bytes);
  }
  if (!success) {
    success = grow_to_reserved();
  }
  if (success && GCLocker::is_active_and_needs_gc()) {
    log_trace(gc, heap)("Garbage collection disabled, expanded heap instead");
  }

  return success;
}

bool CardGeneration::grow_to_reserved() {
  assert_correct_size_change_locking();
  bool success = true;
  const size_t remaining_bytes = _virtual_space.uncommitted_size();
  if (remaining_bytes > 0) {
    success = grow_by(remaining_bytes);
    DEBUG_ONLY(if (!success) log_warning(gc)("grow to reserved failed");)
  }
  return success;
}

void CardGeneration::shrink(size_t bytes) {
  assert_correct_size_change_locking();

  size_t size = ReservedSpace::page_align_size_down(bytes);
  if (size == 0) {
    return;
  }

  // Shrink committed space
  _virtual_space.shrink_by(size);
  // Shrink space; this also shrinks the space's BOT
  space()->set_end((HeapWord*) _virtual_space.high());
  size_t new_word_size = heap_word_size(space()->capacity());
  // Shrink the shared block offset array
  _bts->resize(new_word_size);
  MemRegion mr(space()->bottom(), new_word_size);
  // Shrink the card table
  GenCollectedHeap::heap()->rem_set()->resize_covered_region(mr);

  size_t new_mem_size = _virtual_space.committed_size();
  size_t old_mem_size = new_mem_size + size;
  log_trace(gc, heap)("Shrinking %s from " SIZE_FORMAT "K to " SIZE_FORMAT "K",
                      name(), old_mem_size/K, new_mem_size/K);
}

// No young generation references, clear this generation's cards.
void CardGeneration::clear_remembered_set() {
  _rs->clear(reserved());
}

// Objects in this generation may have moved, invalidate this
// generation's cards.
void CardGeneration::invalidate_remembered_set() {
  _rs->invalidate(used_region());
}

void CardGeneration::compute_new_size() {
  assert(_shrink_factor <= 100, "invalid shrink factor");
  size_t current_shrink_factor = _shrink_factor;
  if (ShrinkHeapInSteps) {
    // Always reset '_shrink_factor' if the heap is shrunk in steps.
    // If we shrink the heap in this iteration, '_shrink_factor' will
    // be recomputed based on the old value further down in this fuction.
    _shrink_factor = 0;
  }

  // We don't have floating point command-line arguments
  // Note:  argument processing ensures that MinHeapFreeRatio < 100.
  const double minimum_free_percentage = MinHeapFreeRatio / 100.0;
  const double maximum_used_percentage = 1.0 - minimum_free_percentage;

  // Compute some numbers about the state of the heap.
  const size_t used_after_gc = used();
  const size_t capacity_after_gc = capacity();

  const double min_tmp = used_after_gc / maximum_used_percentage;
  size_t minimum_desired_capacity = (size_t)MIN2(min_tmp, double(max_uintx));
  // Don't shrink less than the initial generation size
  minimum_desired_capacity = MAX2(minimum_desired_capacity, initial_size());
  assert(used_after_gc <= minimum_desired_capacity, "sanity check");

    const size_t free_after_gc = free();
    const double free_percentage = ((double)free_after_gc) / capacity_after_gc;
    log_trace(gc, heap)("CardGeneration::compute_new_size:");
    log_trace(gc, heap)("    minimum_free_percentage: %6.2f  maximum_used_percentage: %6.2f",
                  minimum_free_percentage,
                  maximum_used_percentage);
    log_trace(gc, heap)("     free_after_gc   : %6.1fK   used_after_gc   : %6.1fK   capacity_after_gc   : %6.1fK",
                  free_after_gc / (double) K,
                  used_after_gc / (double) K,
                  capacity_after_gc / (double) K);
    log_trace(gc, heap)("     free_percentage: %6.2f", free_percentage);

  if (capacity_after_gc < minimum_desired_capacity) {
    // If we have less free space than we want then expand
    size_t expand_bytes = minimum_desired_capacity - capacity_after_gc;
    // Don't expand unless it's significant
    if (expand_bytes >= _min_heap_delta_bytes) {
      expand(expand_bytes, 0); // safe if expansion fails
    }
    log_trace(gc, heap)("    expanding:  minimum_desired_capacity: %6.1fK  expand_bytes: %6.1fK  _min_heap_delta_bytes: %6.1fK",
                  minimum_desired_capacity / (double) K,
                  expand_bytes / (double) K,
                  _min_heap_delta_bytes / (double) K);
    return;
  }

  // No expansion, now see if we want to shrink
  size_t shrink_bytes = 0;
  // We would never want to shrink more than this
  size_t max_shrink_bytes = capacity_after_gc - minimum_desired_capacity;

  if (MaxHeapFreeRatio < 100) {
    const double maximum_free_percentage = MaxHeapFreeRatio / 100.0;
    const double minimum_used_percentage = 1.0 - maximum_free_percentage;
    const double max_tmp = used_after_gc / minimum_used_percentage;
    size_t maximum_desired_capacity = (size_t)MIN2(max_tmp, double(max_uintx));
    maximum_desired_capacity = MAX2(maximum_desired_capacity, initial_size());
    log_trace(gc, heap)("    maximum_free_percentage: %6.2f  minimum_used_percentage: %6.2f",
                             maximum_free_percentage, minimum_used_percentage);
    log_trace(gc, heap)("    _capacity_at_prologue: %6.1fK  minimum_desired_capacity: %6.1fK  maximum_desired_capacity: %6.1fK",
                             _capacity_at_prologue / (double) K,
                             minimum_desired_capacity / (double) K,
                             maximum_desired_capacity / (double) K);
    assert(minimum_desired_capacity <= maximum_desired_capacity,
           "sanity check");

    if (capacity_after_gc > maximum_desired_capacity) {
      // Capacity too large, compute shrinking size
      shrink_bytes = capacity_after_gc - maximum_desired_capacity;
      if (ShrinkHeapInSteps) {
        // If ShrinkHeapInSteps is true (the default),
        // we don't want to shrink all the way back to initSize if people call
        // System.gc(), because some programs do that between "phases" and then
        // we'd just have to grow the heap up again for the next phase.  So we
        // damp the shrinking: 0% on the first call, 10% on the second call, 40%
        // on the third call, and 100% by the fourth call.  But if we recompute
        // size without shrinking, it goes back to 0%.
        shrink_bytes = shrink_bytes / 100 * current_shrink_factor;
        if (current_shrink_factor == 0) {
          _shrink_factor = 10;
        } else {
          _shrink_factor = MIN2(current_shrink_factor * 4, (size_t) 100);
        }
      }
      assert(shrink_bytes <= max_shrink_bytes, "invalid shrink size");
      log_trace(gc, heap)("    shrinking:  initSize: %.1fK  maximum_desired_capacity: %.1fK",
                               initial_size() / (double) K, maximum_desired_capacity / (double) K);
      log_trace(gc, heap)("    shrink_bytes: %.1fK  current_shrink_factor: " SIZE_FORMAT "  new shrink factor: " SIZE_FORMAT "  _min_heap_delta_bytes: %.1fK",
                               shrink_bytes / (double) K,
                               current_shrink_factor,
                               _shrink_factor,
                               _min_heap_delta_bytes / (double) K);
    }
  }

  if (capacity_after_gc > _capacity_at_prologue) {
    // We might have expanded for promotions, in which case we might want to
    // take back that expansion if there's room after GC.  That keeps us from
    // stretching the heap with promotions when there's plenty of room.
    size_t expansion_for_promotion = capacity_after_gc - _capacity_at_prologue;
    expansion_for_promotion = MIN2(expansion_for_promotion, max_shrink_bytes);
    // We have two shrinking computations, take the largest
    shrink_bytes = MAX2(shrink_bytes, expansion_for_promotion);
    assert(shrink_bytes <= max_shrink_bytes, "invalid shrink size");
    log_trace(gc, heap)("    aggressive shrinking:  _capacity_at_prologue: %.1fK  capacity_after_gc: %.1fK  expansion_for_promotion: %.1fK  shrink_bytes: %.1fK",
                        capacity_after_gc / (double) K,
                        _capacity_at_prologue / (double) K,
                        expansion_for_promotion / (double) K,
                        shrink_bytes / (double) K);
  }
  // Don't shrink unless it's significant
  if (shrink_bytes >= _min_heap_delta_bytes) {
    shrink(shrink_bytes);
  }
}

// Currently nothing to do.
void CardGeneration::prepare_for_verify() {}

void CardGeneration::space_iterate(SpaceClosure* blk,
                                                 bool usedOnly) {
  blk->do_space(space());
}

void CardGeneration::younger_refs_iterate(OopIterateClosure* blk) {
  // Apply "cl->do_oop" to (the address of) (exactly) all the ref fields in
  // "sp" that point into the young generation.
  // The iteration is only over objects allocated at the start of the
  // iterations; objects allocated as a result of applying the closure are
  // not included.

  HeapWord* gen_boundary = reserved().start();
  _rs->younger_refs_in_space_iterate(space(), gen_boundary, blk);
}
