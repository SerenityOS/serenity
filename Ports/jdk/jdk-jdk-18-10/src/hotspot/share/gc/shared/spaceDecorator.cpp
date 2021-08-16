/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/parallel/mutableSpace.hpp"
#include "gc/shared/space.inline.hpp"
#include "gc/shared/spaceDecorator.inline.hpp"
#include "logging/log.hpp"
#include "utilities/copy.hpp"

// Catch-all file for utility classes

#ifndef PRODUCT

// Returns true is the location q matches the mangling
// pattern.
bool SpaceMangler::is_mangled(HeapWord* q) {
  // This test loses precision but is good enough
  return badHeapWord == (max_juint & reinterpret_cast<uintptr_t>(*q));
}


void SpaceMangler::set_top_for_allocations(HeapWord* v)  {
  if (v < end()) {
    assert(!CheckZapUnusedHeapArea || is_mangled(v),
      "The high water mark is not mangled");
  }
  _top_for_allocations = v;
}

// Mangle only the unused space that has not previously
// been mangled and that has not been allocated since being
// mangled.
void SpaceMangler::mangle_unused_area() {
  assert(ZapUnusedHeapArea, "Mangling should not be in use");
  // Mangle between top and the high water mark.  Safeguard
  // against the space changing since top_for_allocations was
  // set.
  HeapWord* mangled_end = MIN2(top_for_allocations(), end());
  if (top() < mangled_end) {
    MemRegion mangle_mr(top(), mangled_end);
    SpaceMangler::mangle_region(mangle_mr);
    // Light weight check of mangling.
    check_mangled_unused_area(end());
  }
  // Complete check of unused area which is functional when
  // DEBUG_MANGLING is defined.
  check_mangled_unused_area_complete();
}

// A complete mangle is expected in the
// exceptional case where top_for_allocations is not
// properly tracking the high water mark for mangling.
// This can be the case when to-space is being used for
// scratch space during a mark-sweep-compact.  See
// contribute_scratch().
void SpaceMangler::mangle_unused_area_complete() {
  assert(ZapUnusedHeapArea, "Mangling should not be in use");
  MemRegion mangle_mr(top(), end());
  SpaceMangler::mangle_region(mangle_mr);
}

// Simply mangle the MemRegion mr.
void SpaceMangler::mangle_region(MemRegion mr) {
  assert(ZapUnusedHeapArea, "Mangling should not be in use");
#ifdef ASSERT
  Copy::fill_to_words(mr.start(), mr.word_size(), badHeapWord);
#endif
}

// Check that top, top_for_allocations and the last
// word of the space are mangled.  In a tight memory
// situation even this light weight mangling could
// cause paging by touching the end of the space.
void  SpaceMangler::check_mangled_unused_area(HeapWord* limit) {
  if (CheckZapUnusedHeapArea) {
    // This method can be called while the spaces are
    // being reshaped so skip the test if the end of the
    // space is beyond the specified limit;
    if (end() > limit) return;

    assert(top() == end() ||
           (is_mangled(top())), "Top not mangled");
    assert((top_for_allocations() < top()) ||
           (top_for_allocations() >= end()) ||
           (is_mangled(top_for_allocations())),
           "Older unused not mangled");
    assert(top() == end() ||
           (is_mangled(end() - 1)), "End not properly mangled");
    // Only does checking when DEBUG_MANGLING is defined.
    check_mangled_unused_area_complete();
  }
}

#undef DEBUG_MANGLING
// This should only be used while debugging the mangling
// because of the high cost of checking the completeness.
void  SpaceMangler::check_mangled_unused_area_complete() {
  if (CheckZapUnusedHeapArea) {
    assert(ZapUnusedHeapArea, "Not mangling unused area");
#ifdef DEBUG_MANGLING
    HeapWord* q = top();
    HeapWord* limit = end();

    bool passed = true;
    while (q < limit) {
      if (!is_mangled(q)) {
        passed = false;
        break;
      }
      q++;
    }
    assert(passed, "Mangling is not complete");
#endif
  }
}
#undef DEBUG_MANGLING
#endif // not PRODUCT
