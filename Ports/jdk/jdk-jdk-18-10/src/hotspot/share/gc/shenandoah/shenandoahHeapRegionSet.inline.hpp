/*
 * Copyright (c) 2018, 2019, Red Hat, Inc. All rights reserved.
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

#ifndef SHARE_GC_SHENANDOAH_SHENANDOAHHEAPREGIONSET_INLINE_HPP
#define SHARE_GC_SHENANDOAH_SHENANDOAHHEAPREGIONSET_INLINE_HPP

#include "gc/shenandoah/shenandoahHeapRegionSet.hpp"

#include "gc/shenandoah/shenandoahHeap.hpp"
#include "gc/shenandoah/shenandoahHeapRegion.hpp"

bool ShenandoahHeapRegionSet::is_in(size_t region_idx) const {
  assert(region_idx < _heap->num_regions(), "Sanity");
  return _set_map[region_idx] == 1;
}

bool ShenandoahHeapRegionSet::is_in(ShenandoahHeapRegion* r) const {
  return is_in(r->index());
}

#endif // SHARE_GC_SHENANDOAH_SHENANDOAHHEAPREGIONSET_INLINE_HPP
