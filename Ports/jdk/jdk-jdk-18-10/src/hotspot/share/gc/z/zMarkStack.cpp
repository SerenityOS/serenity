/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
 */

#include "precompiled.hpp"
#include "gc/z/zMarkStack.inline.hpp"
#include "gc/z/zMarkStackAllocator.hpp"
#include "logging/log.hpp"
#include "utilities/debug.hpp"
#include "utilities/powerOfTwo.hpp"

ZMarkStripe::ZMarkStripe() :
    _published(),
    _overflowed() {}

ZMarkStripeSet::ZMarkStripeSet() :
    _nstripes(0),
    _nstripes_mask(0),
    _stripes() {}

void ZMarkStripeSet::set_nstripes(size_t nstripes) {
  assert(is_power_of_2(nstripes), "Must be a power of two");
  assert(is_power_of_2(ZMarkStripesMax), "Must be a power of two");
  assert(nstripes >= 1, "Invalid number of stripes");
  assert(nstripes <= ZMarkStripesMax, "Invalid number of stripes");

  _nstripes = nstripes;
  _nstripes_mask = nstripes - 1;

  log_debug(gc, marking)("Using " SIZE_FORMAT " mark stripes", _nstripes);
}

bool ZMarkStripeSet::is_empty() const {
  for (size_t i = 0; i < _nstripes; i++) {
    if (!_stripes[i].is_empty()) {
      return false;
    }
  }

  return true;
}

ZMarkStripe* ZMarkStripeSet::stripe_for_worker(uint nworkers, uint worker_id) {
  const size_t spillover_limit = (nworkers / _nstripes) * _nstripes;
  size_t index;

  if (worker_id < spillover_limit) {
    // Not a spillover worker, use natural stripe
    index = worker_id & _nstripes_mask;
  } else {
    // Distribute spillover workers evenly across stripes
    const size_t spillover_nworkers = nworkers - spillover_limit;
    const size_t spillover_worker_id = worker_id - spillover_limit;
    const double spillover_chunk = (double)_nstripes / (double)spillover_nworkers;
    index = spillover_worker_id * spillover_chunk;
  }

  assert(index < _nstripes, "Invalid index");
  return &_stripes[index];
}

ZMarkThreadLocalStacks::ZMarkThreadLocalStacks() :
    _magazine(NULL) {
  for (size_t i = 0; i < ZMarkStripesMax; i++) {
    _stacks[i] = NULL;
  }
}

bool ZMarkThreadLocalStacks::is_empty(const ZMarkStripeSet* stripes) const {
  for (size_t i = 0; i < stripes->nstripes(); i++) {
    ZMarkStack* const stack = _stacks[i];
    if (stack != NULL) {
      return false;
    }
  }

  return true;
}

ZMarkStack* ZMarkThreadLocalStacks::allocate_stack(ZMarkStackAllocator* allocator) {
  if (_magazine == NULL) {
    // Allocate new magazine
    _magazine = allocator->alloc_magazine();
    if (_magazine == NULL) {
      return NULL;
    }
  }

  ZMarkStack* stack = NULL;

  if (!_magazine->pop(stack)) {
    // Magazine is empty, convert magazine into a new stack
    _magazine->~ZMarkStackMagazine();
    stack = new ((void*)_magazine) ZMarkStack();
    _magazine = NULL;
  }

  return stack;
}

void ZMarkThreadLocalStacks::free_stack(ZMarkStackAllocator* allocator, ZMarkStack* stack) {
  for (;;) {
    if (_magazine == NULL) {
      // Convert stack into a new magazine
      stack->~ZMarkStack();
      _magazine = new ((void*)stack) ZMarkStackMagazine();
      return;
    }

    if (_magazine->push(stack)) {
      // Success
      return;
    }

    // Free and uninstall full magazine
    allocator->free_magazine(_magazine);
    _magazine = NULL;
  }
}

bool ZMarkThreadLocalStacks::push_slow(ZMarkStackAllocator* allocator,
                                       ZMarkStripe* stripe,
                                       ZMarkStack** stackp,
                                       ZMarkStackEntry entry,
                                       bool publish) {
  ZMarkStack* stack = *stackp;

  for (;;) {
    if (stack == NULL) {
      // Allocate and install new stack
      *stackp = stack = allocate_stack(allocator);
      if (stack == NULL) {
        // Out of mark stack memory
        return false;
      }
    }

    if (stack->push(entry)) {
      // Success
      return true;
    }

    // Publish/Overflow and uninstall stack
    stripe->publish_stack(stack, publish);
    *stackp = stack = NULL;
  }
}

bool ZMarkThreadLocalStacks::pop_slow(ZMarkStackAllocator* allocator,
                                      ZMarkStripe* stripe,
                                      ZMarkStack** stackp,
                                      ZMarkStackEntry& entry) {
  ZMarkStack* stack = *stackp;

  for (;;) {
    if (stack == NULL) {
      // Try steal and install stack
      *stackp = stack = stripe->steal_stack();
      if (stack == NULL) {
        // Nothing to steal
        return false;
      }
    }

    if (stack->pop(entry)) {
      // Success
      return true;
    }

    // Free and uninstall stack
    free_stack(allocator, stack);
    *stackp = stack = NULL;
  }
}

bool ZMarkThreadLocalStacks::flush(ZMarkStackAllocator* allocator, ZMarkStripeSet* stripes) {
  bool flushed = false;

  // Flush all stacks
  for (size_t i = 0; i < stripes->nstripes(); i++) {
    ZMarkStripe* const stripe = stripes->stripe_at(i);
    ZMarkStack** const stackp = &_stacks[i];
    ZMarkStack* const stack = *stackp;
    if (stack == NULL) {
      continue;
    }

    // Free/Publish and uninstall stack
    if (stack->is_empty()) {
      free_stack(allocator, stack);
    } else {
      stripe->publish_stack(stack);
      flushed = true;
    }
    *stackp = NULL;
  }

  return flushed;
}

void ZMarkThreadLocalStacks::free(ZMarkStackAllocator* allocator) {
  // Free and uninstall magazine
  if (_magazine != NULL) {
    allocator->free_magazine(_magazine);
    _magazine = NULL;
  }
}
