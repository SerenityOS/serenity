/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/g1/g1AllocRegion.inline.hpp"
#include "gc/g1/g1EvacStats.inline.hpp"
#include "gc/g1/g1CollectedHeap.inline.hpp"
#include "gc/shared/tlab_globals.hpp"
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/orderAccess.hpp"
#include "utilities/align.hpp"

G1CollectedHeap* G1AllocRegion::_g1h = NULL;
HeapRegion* G1AllocRegion::_dummy_region = NULL;

void G1AllocRegion::setup(G1CollectedHeap* g1h, HeapRegion* dummy_region) {
  assert(_dummy_region == NULL, "should be set once");
  assert(dummy_region != NULL, "pre-condition");
  assert(dummy_region->free() == 0, "pre-condition");

  // Make sure that any allocation attempt on this region will fail
  // and will not trigger any asserts.
  assert(dummy_region->allocate_no_bot_updates(1) == NULL, "should fail");
  assert(dummy_region->allocate(1) == NULL, "should fail");
  DEBUG_ONLY(size_t assert_tmp);
  assert(dummy_region->par_allocate_no_bot_updates(1, 1, &assert_tmp) == NULL, "should fail");
  assert(dummy_region->par_allocate(1, 1, &assert_tmp) == NULL, "should fail");

  _g1h = g1h;
  _dummy_region = dummy_region;
}

size_t G1AllocRegion::fill_up_remaining_space(HeapRegion* alloc_region) {
  assert(alloc_region != NULL && alloc_region != _dummy_region,
         "pre-condition");
  size_t result = 0;

  // Other threads might still be trying to allocate using a CAS out
  // of the region we are trying to retire, as they can do so without
  // holding the lock. So, we first have to make sure that noone else
  // can allocate out of it by doing a maximal allocation. Even if our
  // CAS attempt fails a few times, we'll succeed sooner or later
  // given that failed CAS attempts mean that the region is getting
  // closed to being full.
  size_t free_word_size = alloc_region->free() / HeapWordSize;

  // This is the minimum free chunk we can turn into a dummy
  // object. If the free space falls below this, then noone can
  // allocate in this region anyway (all allocation requests will be
  // of a size larger than this) so we won't have to perform the dummy
  // allocation.
  size_t min_word_size_to_fill = CollectedHeap::min_fill_size();

  while (free_word_size >= min_word_size_to_fill) {
    HeapWord* dummy = par_allocate(alloc_region, free_word_size);
    if (dummy != NULL) {
      // If the allocation was successful we should fill in the space.
      CollectedHeap::fill_with_object(dummy, free_word_size);
      alloc_region->set_pre_dummy_top(dummy);
      result += free_word_size * HeapWordSize;
      break;
    }

    free_word_size = alloc_region->free() / HeapWordSize;
    // It's also possible that someone else beats us to the
    // allocation and they fill up the region. In that case, we can
    // just get out of the loop.
  }
  result += alloc_region->free();

  assert(alloc_region->free() / HeapWordSize < min_word_size_to_fill,
         "post-condition");
  return result;
}

size_t G1AllocRegion::retire_internal(HeapRegion* alloc_region, bool fill_up) {
  // We never have to check whether the active region is empty or not,
  // and potentially free it if it is, given that it's guaranteed that
  // it will never be empty.
  size_t waste = 0;
  assert_alloc_region(!alloc_region->is_empty(),
      "the alloc region should never be empty");

  if (fill_up) {
    waste = fill_up_remaining_space(alloc_region);
  }

  assert_alloc_region(alloc_region->used() >= _used_bytes_before, "invariant");
  size_t allocated_bytes = alloc_region->used() - _used_bytes_before;
  retire_region(alloc_region, allocated_bytes);
  _used_bytes_before = 0;

  return waste;
}

size_t G1AllocRegion::retire(bool fill_up) {
  assert_alloc_region(_alloc_region != NULL, "not initialized properly");

  size_t waste = 0;

  trace("retiring");
  HeapRegion* alloc_region = _alloc_region;
  if (alloc_region != _dummy_region) {
    waste = retire_internal(alloc_region, fill_up);
    reset_alloc_region();
  }
  trace("retired");

  return waste;
}

HeapWord* G1AllocRegion::new_alloc_region_and_allocate(size_t word_size,
                                                       bool force) {
  assert_alloc_region(_alloc_region == _dummy_region, "pre-condition");
  assert_alloc_region(_used_bytes_before == 0, "pre-condition");

  trace("attempting region allocation");
  HeapRegion* new_alloc_region = allocate_new_region(word_size, force);
  if (new_alloc_region != NULL) {
    new_alloc_region->reset_pre_dummy_top();
    // Need to do this before the allocation
    _used_bytes_before = new_alloc_region->used();
    HeapWord* result = allocate(new_alloc_region, word_size);
    assert_alloc_region(result != NULL, "the allocation should succeeded");

    OrderAccess::storestore();
    // Note that we first perform the allocation and then we store the
    // region in _alloc_region. This is the reason why an active region
    // can never be empty.
    update_alloc_region(new_alloc_region);
    trace("region allocation successful");
    return result;
  } else {
    trace("region allocation failed");
    return NULL;
  }
  ShouldNotReachHere();
}

void G1AllocRegion::init() {
  trace("initializing");
  assert_alloc_region(_alloc_region == NULL && _used_bytes_before == 0, "pre-condition");
  assert_alloc_region(_dummy_region != NULL, "should have been set");
  _alloc_region = _dummy_region;
  _count = 0;
  trace("initialized");
}

void G1AllocRegion::set(HeapRegion* alloc_region) {
  trace("setting");
  // We explicitly check that the region is not empty to make sure we
  // maintain the "the alloc region cannot be empty" invariant.
  assert_alloc_region(alloc_region != NULL && !alloc_region->is_empty(), "pre-condition");
  assert_alloc_region(_alloc_region == _dummy_region &&
                         _used_bytes_before == 0 && _count == 0,
                         "pre-condition");

  _used_bytes_before = alloc_region->used();
  _alloc_region = alloc_region;
  _count += 1;
  trace("set");
}

void G1AllocRegion::update_alloc_region(HeapRegion* alloc_region) {
  trace("update");
  // We explicitly check that the region is not empty to make sure we
  // maintain the "the alloc region cannot be empty" invariant.
  assert_alloc_region(alloc_region != NULL && !alloc_region->is_empty(), "pre-condition");

  _alloc_region = alloc_region;
  _count += 1;
  trace("updated");
}

HeapRegion* G1AllocRegion::release() {
  trace("releasing");
  HeapRegion* alloc_region = _alloc_region;
  retire(false /* fill_up */);
  assert_alloc_region(_alloc_region == _dummy_region, "post-condition of retire()");
  _alloc_region = NULL;
  trace("released");
  return (alloc_region == _dummy_region) ? NULL : alloc_region;
}

#ifndef PRODUCT
void G1AllocRegion::trace(const char* str, size_t min_word_size, size_t desired_word_size, size_t actual_word_size, HeapWord* result) {
  // All the calls to trace that set either just the size or the size
  // and the result are considered part of detailed tracing and are
  // skipped during other tracing.

  Log(gc, alloc, region) log;

  if (!log.is_debug()) {
    return;
  }

  bool detailed_info = log.is_trace();

  if ((actual_word_size == 0 && result == NULL) || detailed_info) {
    ResourceMark rm;
    LogStream ls_trace(log.trace());
    LogStream ls_debug(log.debug());
    outputStream* out = detailed_info ? &ls_trace : &ls_debug;

    out->print("%s: %u ", _name, _count);

    if (_alloc_region == NULL) {
      out->print("NULL");
    } else if (_alloc_region == _dummy_region) {
      out->print("DUMMY");
    } else {
      out->print(HR_FORMAT, HR_FORMAT_PARAMS(_alloc_region));
    }

    out->print(" : %s", str);

    if (detailed_info) {
      if (result != NULL) {
        out->print(" min " SIZE_FORMAT " desired " SIZE_FORMAT " actual " SIZE_FORMAT " " PTR_FORMAT,
                     min_word_size, desired_word_size, actual_word_size, p2i(result));
      } else if (min_word_size != 0) {
        out->print(" min " SIZE_FORMAT " desired " SIZE_FORMAT, min_word_size, desired_word_size);
      }
    }
    out->cr();
  }
}
#endif // PRODUCT

G1AllocRegion::G1AllocRegion(const char* name,
                             bool bot_updates,
                             uint node_index)
  : _alloc_region(NULL),
    _count(0),
    _used_bytes_before(0),
    _bot_updates(bot_updates),
    _name(name),
    _node_index(node_index)
 { }

HeapRegion* MutatorAllocRegion::allocate_new_region(size_t word_size,
                                                    bool force) {
  return _g1h->new_mutator_alloc_region(word_size, force, _node_index);
}

void MutatorAllocRegion::retire_region(HeapRegion* alloc_region,
                                       size_t allocated_bytes) {
  _g1h->retire_mutator_alloc_region(alloc_region, allocated_bytes);
}

void MutatorAllocRegion::init() {
  assert(_retained_alloc_region == NULL, "Pre-condition");
  G1AllocRegion::init();
  _wasted_bytes = 0;
}

bool MutatorAllocRegion::should_retain(HeapRegion* region) {
  size_t free_bytes = region->free();
  if (free_bytes < MinTLABSize) {
    return false;
  }

  if (_retained_alloc_region != NULL &&
      free_bytes < _retained_alloc_region->free()) {
    return false;
  }

  return true;
}

size_t MutatorAllocRegion::retire(bool fill_up) {
  size_t waste = 0;
  trace("retiring");
  HeapRegion* current_region = get();
  if (current_region != NULL) {
    // Retain the current region if it fits a TLAB and has more
    // free than the currently retained region.
    if (should_retain(current_region)) {
      trace("mutator retained");
      if (_retained_alloc_region != NULL) {
        waste = retire_internal(_retained_alloc_region, true);
      }
      _retained_alloc_region = current_region;
    } else {
      waste = retire_internal(current_region, fill_up);
    }
    reset_alloc_region();
  }

  _wasted_bytes += waste;
  trace("retired");
  return waste;
}

size_t MutatorAllocRegion::used_in_alloc_regions() {
  size_t used = 0;
  HeapRegion* hr = get();
  if (hr != NULL) {
    used += hr->used();
  }

  hr = _retained_alloc_region;
  if (hr != NULL) {
    used += hr->used();
  }
  return used;
}

HeapRegion* MutatorAllocRegion::release() {
  HeapRegion* ret = G1AllocRegion::release();

  // The retained alloc region must be retired and this must be
  // done after the above call to release the mutator alloc region,
  // since it might update the _retained_alloc_region member.
  if (_retained_alloc_region != NULL) {
    _wasted_bytes += retire_internal(_retained_alloc_region, false);
    _retained_alloc_region = NULL;
  }
  log_debug(gc, alloc, region)("Mutator Allocation stats, regions: %u, wasted size: " SIZE_FORMAT "%s (%4.1f%%)",
                               count(),
                               byte_size_in_proper_unit(_wasted_bytes),
                               proper_unit_for_byte_size(_wasted_bytes),
                               percent_of(_wasted_bytes, count() * HeapRegion::GrainBytes));
  return ret;
}

HeapRegion* G1GCAllocRegion::allocate_new_region(size_t word_size,
                                                 bool force) {
  assert(!force, "not supported for GC alloc regions");
  return _g1h->new_gc_alloc_region(word_size, _purpose, _node_index);
}

void G1GCAllocRegion::retire_region(HeapRegion* alloc_region,
                                    size_t allocated_bytes) {
  _g1h->retire_gc_alloc_region(alloc_region, allocated_bytes, _purpose);
}

size_t G1GCAllocRegion::retire(bool fill_up) {
  HeapRegion* retired = get();
  size_t end_waste = G1AllocRegion::retire(fill_up);
  // Do not count retirement of the dummy allocation region.
  if (retired != NULL) {
    _stats->add_region_end_waste(end_waste / HeapWordSize);
  }
  return end_waste;
}

HeapRegion* OldGCAllocRegion::release() {
  HeapRegion* cur = get();
  if (cur != NULL) {
    // Determine how far we are from the next card boundary. If it is smaller than
    // the minimum object size we can allocate into, expand into the next card.
    HeapWord* top = cur->top();
    HeapWord* aligned_top = align_up(top, BOTConstants::N_bytes);

    size_t to_allocate_words = pointer_delta(aligned_top, top, HeapWordSize);

    if (to_allocate_words != 0) {
      // We are not at a card boundary. Fill up, possibly into the next, taking the
      // end of the region and the minimum object size into account.
      to_allocate_words = MIN2(pointer_delta(cur->end(), cur->top(), HeapWordSize),
                               MAX2(to_allocate_words, G1CollectedHeap::min_fill_size()));

      // Skip allocation if there is not enough space to allocate even the smallest
      // possible object. In this case this region will not be retained, so the
      // original problem cannot occur.
      if (to_allocate_words >= G1CollectedHeap::min_fill_size()) {
        HeapWord* dummy = attempt_allocation(to_allocate_words);
        CollectedHeap::fill_with_object(dummy, to_allocate_words);
      }
    }
  }
  return G1AllocRegion::release();
}
