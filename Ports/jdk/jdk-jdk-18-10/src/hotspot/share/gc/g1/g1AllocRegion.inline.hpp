/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1ALLOCREGION_INLINE_HPP
#define SHARE_GC_G1_G1ALLOCREGION_INLINE_HPP

#include "gc/g1/g1AllocRegion.hpp"

#include "gc/g1/heapRegion.inline.hpp"

#define assert_alloc_region(p, message)                                  \
  do {                                                                   \
    assert((p), "[%s] %s c: %u b: %s r: " PTR_FORMAT " u: " SIZE_FORMAT, \
           _name, (message), _count, BOOL_TO_STR(_bot_updates),          \
           p2i(_alloc_region), _used_bytes_before);                      \
  } while (0)


inline void G1AllocRegion::reset_alloc_region() {
  _alloc_region = _dummy_region;
}

inline HeapWord* G1AllocRegion::allocate(HeapRegion* alloc_region,
                                         size_t word_size) {
  assert(alloc_region != NULL, "pre-condition");

  if (!_bot_updates) {
    return alloc_region->allocate_no_bot_updates(word_size);
  } else {
    return alloc_region->allocate(word_size);
  }
}

inline HeapWord* G1AllocRegion::par_allocate(HeapRegion* alloc_region, size_t word_size) {
  size_t temp;
  return par_allocate(alloc_region, word_size, word_size, &temp);
}

inline HeapWord* G1AllocRegion::par_allocate(HeapRegion* alloc_region,
                                             size_t min_word_size,
                                             size_t desired_word_size,
                                             size_t* actual_word_size) {
  assert(alloc_region != NULL, "pre-condition");
  assert(!alloc_region->is_empty(), "pre-condition");

  if (!_bot_updates) {
    return alloc_region->par_allocate_no_bot_updates(min_word_size, desired_word_size, actual_word_size);
  } else {
    return alloc_region->par_allocate(min_word_size, desired_word_size, actual_word_size);
  }
}

inline HeapWord* G1AllocRegion::attempt_allocation(size_t word_size) {
  size_t temp;
  return attempt_allocation(word_size, word_size, &temp);
}

inline HeapWord* G1AllocRegion::attempt_allocation(size_t min_word_size,
                                                   size_t desired_word_size,
                                                   size_t* actual_word_size) {
  HeapRegion* alloc_region = _alloc_region;
  assert_alloc_region(alloc_region != NULL, "not initialized properly");

  HeapWord* result = par_allocate(alloc_region, min_word_size, desired_word_size, actual_word_size);
  if (result != NULL) {
    trace("alloc", min_word_size, desired_word_size, *actual_word_size, result);
    return result;
  }
  trace("alloc failed", min_word_size, desired_word_size);
  return NULL;
}

inline HeapWord* G1AllocRegion::attempt_allocation_locked(size_t word_size) {
  size_t temp;
  return attempt_allocation_locked(word_size, word_size, &temp);
}

inline HeapWord* G1AllocRegion::attempt_allocation_locked(size_t min_word_size,
                                                          size_t desired_word_size,
                                                          size_t* actual_word_size) {
  HeapWord* result = attempt_allocation(min_word_size, desired_word_size, actual_word_size);
  if (result != NULL) {
    return result;
  }

  return attempt_allocation_using_new_region(min_word_size, desired_word_size, actual_word_size);
}

inline HeapWord* G1AllocRegion::attempt_allocation_using_new_region(size_t min_word_size,
                                                                    size_t desired_word_size,
                                                                    size_t* actual_word_size) {
  retire(true /* fill_up */);
  HeapWord* result = new_alloc_region_and_allocate(desired_word_size, false /* force */);
  if (result != NULL) {
    *actual_word_size = desired_word_size;
    trace("alloc locked (second attempt)", min_word_size, desired_word_size, *actual_word_size, result);
    return result;
  }
  trace("alloc locked failed", min_word_size, desired_word_size);
  return NULL;
}

inline HeapWord* G1AllocRegion::attempt_allocation_force(size_t word_size) {
  assert_alloc_region(_alloc_region != NULL, "not initialized properly");

  trace("forcing alloc", word_size, word_size);
  HeapWord* result = new_alloc_region_and_allocate(word_size, true /* force */);
  if (result != NULL) {
    trace("alloc forced", word_size, word_size, word_size, result);
    return result;
  }
  trace("alloc forced failed", word_size, word_size);
  return NULL;
}

inline HeapWord* MutatorAllocRegion::attempt_retained_allocation(size_t min_word_size,
                                                                 size_t desired_word_size,
                                                                 size_t* actual_word_size) {
  if (_retained_alloc_region != NULL) {
    HeapWord* result = par_allocate(_retained_alloc_region, min_word_size, desired_word_size, actual_word_size);
    if (result != NULL) {
      trace("alloc retained", min_word_size, desired_word_size, *actual_word_size, result);
      return result;
    }
  }
  return NULL;
}

#endif // SHARE_GC_G1_G1ALLOCREGION_INLINE_HPP
