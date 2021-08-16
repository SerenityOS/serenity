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

#include "precompiled.hpp"
#include "gc/serial/genMarkSweep.hpp"
#include "gc/serial/tenuredGeneration.inline.hpp"
#include "gc/shared/blockOffsetTable.inline.hpp"
#include "gc/shared/cardGeneration.inline.hpp"
#include "gc/shared/collectorCounters.hpp"
#include "gc/shared/gcTimer.hpp"
#include "gc/shared/gcTrace.hpp"
#include "gc/shared/genCollectedHeap.hpp"
#include "gc/shared/genOopClosures.inline.hpp"
#include "gc/shared/generationSpec.hpp"
#include "gc/shared/space.hpp"
#include "logging/log.hpp"
#include "memory/allocation.inline.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/java.hpp"
#include "utilities/macros.hpp"

TenuredGeneration::TenuredGeneration(ReservedSpace rs,
                                     size_t initial_byte_size,
                                     size_t min_byte_size,
                                     size_t max_byte_size,
                                     CardTableRS* remset) :
  CardGeneration(rs, initial_byte_size, remset)
{
  HeapWord* bottom = (HeapWord*) _virtual_space.low();
  HeapWord* end    = (HeapWord*) _virtual_space.high();
  _the_space  = new TenuredSpace(_bts, MemRegion(bottom, end));
  _the_space->reset_saved_mark();
  // If we don't shrink the heap in steps, '_shrink_factor' is always 100%.
  _shrink_factor = ShrinkHeapInSteps ? 0 : 100;
  _capacity_at_prologue = 0;

  _gc_stats = new GCStats();

  // initialize performance counters

  const char* gen_name = "old";
  // Generation Counters -- generation 1, 1 subspace
  _gen_counters = new GenerationCounters(gen_name, 1, 1,
      min_byte_size, max_byte_size, &_virtual_space);

  _gc_counters = new CollectorCounters("Serial full collection pauses", 1);

  _space_counters = new CSpaceCounters(gen_name, 0,
                                       _virtual_space.reserved_size(),
                                       _the_space, _gen_counters);
}

void TenuredGeneration::gc_prologue(bool full) {
  _capacity_at_prologue = capacity();
  _used_at_prologue = used();
}

bool TenuredGeneration::should_collect(bool  full,
                                       size_t size,
                                       bool   is_tlab) {
  // This should be one big conditional or (||), but I want to be able to tell
  // why it returns what it returns (without re-evaluating the conditionals
  // in case they aren't idempotent), so I'm doing it this way.
  // DeMorgan says it's okay.
  if (full) {
    log_trace(gc)("TenuredGeneration::should_collect: because full");
    return true;
  }
  if (should_allocate(size, is_tlab)) {
    log_trace(gc)("TenuredGeneration::should_collect: because should_allocate(" SIZE_FORMAT ")", size);
    return true;
  }
  // If we don't have very much free space.
  // XXX: 10000 should be a percentage of the capacity!!!
  if (free() < 10000) {
    log_trace(gc)("TenuredGeneration::should_collect: because free(): " SIZE_FORMAT, free());
    return true;
  }
  // If we had to expand to accommodate promotions from the young generation
  if (_capacity_at_prologue < capacity()) {
    log_trace(gc)("TenuredGeneration::should_collect: because_capacity_at_prologue: " SIZE_FORMAT " < capacity(): " SIZE_FORMAT,
        _capacity_at_prologue, capacity());
    return true;
  }

  return false;
}

void TenuredGeneration::compute_new_size() {
  assert_locked_or_safepoint(Heap_lock);

  // Compute some numbers about the state of the heap.
  const size_t used_after_gc = used();
  const size_t capacity_after_gc = capacity();

  CardGeneration::compute_new_size();

  assert(used() == used_after_gc && used_after_gc <= capacity(),
         "used: " SIZE_FORMAT " used_after_gc: " SIZE_FORMAT
         " capacity: " SIZE_FORMAT, used(), used_after_gc, capacity());
}

void TenuredGeneration::update_gc_stats(Generation* current_generation,
                                        bool full) {
  // If the young generation has been collected, gather any statistics
  // that are of interest at this point.
  bool current_is_young = GenCollectedHeap::heap()->is_young_gen(current_generation);
  if (!full && current_is_young) {
    // Calculate size of data promoted from the young generation
    // before doing the collection.
    size_t used_before_gc = used();

    // If the young gen collection was skipped, then the
    // number of promoted bytes will be 0 and adding it to the
    // average will incorrectly lessen the average.  It is, however,
    // also possible that no promotion was needed.
    if (used_before_gc >= _used_at_prologue) {
      size_t promoted_in_bytes = used_before_gc - _used_at_prologue;
      gc_stats()->avg_promoted()->sample(promoted_in_bytes);
    }
  }
}

void TenuredGeneration::update_counters() {
  if (UsePerfData) {
    _space_counters->update_all();
    _gen_counters->update_all();
  }
}

bool TenuredGeneration::promotion_attempt_is_safe(size_t max_promotion_in_bytes) const {
  size_t available = max_contiguous_available();
  size_t av_promo  = (size_t)gc_stats()->avg_promoted()->padded_average();
  bool   res = (available >= av_promo) || (available >= max_promotion_in_bytes);

  log_trace(gc)("Tenured: promo attempt is%s safe: available(" SIZE_FORMAT ") %s av_promo(" SIZE_FORMAT "), max_promo(" SIZE_FORMAT ")",
    res? "":" not", available, res? ">=":"<", av_promo, max_promotion_in_bytes);

  return res;
}

void TenuredGeneration::collect(bool   full,
                                bool   clear_all_soft_refs,
                                size_t size,
                                bool   is_tlab) {
  GenCollectedHeap* gch = GenCollectedHeap::heap();

  // Temporarily expand the span of our ref processor, so
  // refs discovery is over the entire heap, not just this generation
  ReferenceProcessorSpanMutator
    x(ref_processor(), gch->reserved_region());

  STWGCTimer* gc_timer = GenMarkSweep::gc_timer();
  gc_timer->register_gc_start();

  SerialOldTracer* gc_tracer = GenMarkSweep::gc_tracer();
  gc_tracer->report_gc_start(gch->gc_cause(), gc_timer->gc_start());

  gch->pre_full_gc_dump(gc_timer);

  GenMarkSweep::invoke_at_safepoint(ref_processor(), clear_all_soft_refs);

  gch->post_full_gc_dump(gc_timer);

  gc_timer->register_gc_end();

  gc_tracer->report_gc_end(gc_timer->gc_end(), gc_timer->time_partitions());
}

HeapWord*
TenuredGeneration::expand_and_allocate(size_t word_size, bool is_tlab) {
  assert(!is_tlab, "TenuredGeneration does not support TLAB allocation");
  expand(word_size*HeapWordSize, _min_heap_delta_bytes);
  return _the_space->allocate(word_size);
}

bool TenuredGeneration::expand(size_t bytes, size_t expand_bytes) {
  GCMutexLocker x(ExpandHeap_lock);
  return CardGeneration::expand(bytes, expand_bytes);
}

size_t TenuredGeneration::unsafe_max_alloc_nogc() const {
  return _the_space->free();
}

size_t TenuredGeneration::contiguous_available() const {
  return _the_space->free() + _virtual_space.uncommitted_size();
}

void TenuredGeneration::assert_correct_size_change_locking() {
  assert_locked_or_safepoint(ExpandHeap_lock);
}

// Currently nothing to do.
void TenuredGeneration::prepare_for_verify() {}

void TenuredGeneration::object_iterate(ObjectClosure* blk) {
  _the_space->object_iterate(blk);
}

void TenuredGeneration::save_marks() {
  _the_space->set_saved_mark();
}

void TenuredGeneration::reset_saved_marks() {
  _the_space->reset_saved_mark();
}

bool TenuredGeneration::no_allocs_since_save_marks() {
  return _the_space->saved_mark_at_top();
}

void TenuredGeneration::gc_epilogue(bool full) {
  // update the generation and space performance counters
  update_counters();
  if (ZapUnusedHeapArea) {
    _the_space->check_mangled_unused_area_complete();
  }
}

void TenuredGeneration::record_spaces_top() {
  assert(ZapUnusedHeapArea, "Not mangling unused space");
  _the_space->set_top_for_allocations();
}

void TenuredGeneration::verify() {
  _the_space->verify();
}

void TenuredGeneration::print_on(outputStream* st)  const {
  Generation::print_on(st);
  st->print("   the");
  _the_space->print_on(st);
}
