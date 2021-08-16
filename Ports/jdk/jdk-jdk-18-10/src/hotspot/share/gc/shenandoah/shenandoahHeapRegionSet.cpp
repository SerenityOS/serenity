/*
 * Copyright (c) 2013, 2019, Red Hat, Inc. All rights reserved.
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
#include "gc/shenandoah/shenandoahHeapRegionSet.inline.hpp"
#include "gc/shenandoah/shenandoahHeap.inline.hpp"
#include "gc/shenandoah/shenandoahHeapRegion.hpp"
#include "gc/shenandoah/shenandoahUtils.hpp"
#include "runtime/atomic.hpp"
#include "utilities/copy.hpp"

ShenandoahHeapRegionSetIterator::ShenandoahHeapRegionSetIterator(const ShenandoahHeapRegionSet* const set) :
        _set(set), _heap(ShenandoahHeap::heap()), _current_index(0) {}

ShenandoahHeapRegionSet::ShenandoahHeapRegionSet() :
  _heap(ShenandoahHeap::heap()),
  _map_size(_heap->num_regions()),
  _set_map(NEW_C_HEAP_ARRAY(jbyte, _map_size, mtGC)),
  _region_count(0)
{
  // Use 1-byte data type
  STATIC_ASSERT(sizeof(jbyte) == 1);

  // Initialize cset map
  Copy::zero_to_bytes(_set_map, _map_size);
}

ShenandoahHeapRegionSet::~ShenandoahHeapRegionSet() {
  FREE_C_HEAP_ARRAY(jbyte, _set_map);
}

void ShenandoahHeapRegionSet::add_region(ShenandoahHeapRegion* r) {
  assert(!is_in(r), "Already in region set");
  _set_map[r->index()] = 1;
  _region_count++;
}

void ShenandoahHeapRegionSet::remove_region(ShenandoahHeapRegion* r) {
  assert(ShenandoahSafepoint::is_at_shenandoah_safepoint(), "Must be at a safepoint");
  assert(Thread::current()->is_VM_thread(), "Must be VMThread");
  assert(is_in(r), "Not in region set");
  _set_map[r->index()] = 0;
  _region_count--;
}

void ShenandoahHeapRegionSet::clear() {
  assert(ShenandoahSafepoint::is_at_shenandoah_safepoint(), "Must be at a safepoint");
  Copy::zero_to_bytes(_set_map, _map_size);
  _region_count = 0;
}

ShenandoahHeapRegion* ShenandoahHeapRegionSetIterator::next() {
  for (size_t index = _current_index; index < _heap->num_regions(); index++) {
    if (_set->is_in(index)) {
      _current_index = index + 1;
      return _heap->get_region(index);
    }
  }
  return NULL;
}

void ShenandoahHeapRegionSet::print_on(outputStream* out) const {
  out->print_cr("Region Set : " SIZE_FORMAT "", count());
  for (size_t index = 0; index < _heap->num_regions(); index++) {
    if (is_in(index)) {
      _heap->get_region(index)->print_on(out);
    }
  }
}
