/*
 * Copyright (c) 2017, 2020, Red Hat, Inc. All rights reserved.
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

#ifndef SHARE_GC_SHENANDOAH_SHENANDOAHCOLLECTIONSET_INLINE_HPP
#define SHARE_GC_SHENANDOAH_SHENANDOAHCOLLECTIONSET_INLINE_HPP

#include "gc/shenandoah/shenandoahCollectionSet.hpp"

#include "gc/shenandoah/shenandoahHeap.hpp"
#include "gc/shenandoah/shenandoahHeap.inline.hpp"
#include "gc/shenandoah/shenandoahHeapRegion.hpp"

bool ShenandoahCollectionSet::is_in(size_t region_idx) const {
  assert(region_idx < _heap->num_regions(), "Sanity");
  return _cset_map[region_idx] == 1;
}

bool ShenandoahCollectionSet::is_in(ShenandoahHeapRegion* r) const {
  return is_in(r->index());
}

bool ShenandoahCollectionSet::is_in(oop p) const {
  shenandoah_assert_in_heap_or_null(NULL, p);
  return is_in_loc(cast_from_oop<void*>(p));
}

bool ShenandoahCollectionSet::is_in_loc(void* p) const {
  assert(p == NULL || _heap->is_in(p), "Must be in the heap");
  uintx index = ((uintx) p) >> _region_size_bytes_shift;
  // no need to subtract the bottom of the heap from p,
  // _biased_cset_map is biased
  return _biased_cset_map[index] == 1;
}

#endif // SHARE_GC_SHENANDOAH_SHENANDOAHCOLLECTIONSET_INLINE_HPP
