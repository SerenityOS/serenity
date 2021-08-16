/*
 * Copyright (c) 2015, 2019, Red Hat, Inc. All rights reserved.
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

#ifndef SHARE_GC_SHENANDOAH_SHENANDOAHHEAPREGION_INLINE_HPP
#define SHARE_GC_SHENANDOAH_SHENANDOAHHEAPREGION_INLINE_HPP

#include "gc/shenandoah/shenandoahHeapRegion.hpp"

#include "gc/shenandoah/shenandoahHeap.inline.hpp"
#include "gc/shenandoah/shenandoahPacer.inline.hpp"
#include "runtime/atomic.hpp"

HeapWord* ShenandoahHeapRegion::allocate(size_t size, ShenandoahAllocRequest::Type type) {
  shenandoah_assert_heaplocked_or_safepoint();
  assert(is_object_aligned(size), "alloc size breaks alignment: " SIZE_FORMAT, size);

  HeapWord* obj = top();
  if (pointer_delta(end(), obj) >= size) {
    make_regular_allocation();
    adjust_alloc_metadata(type, size);

    HeapWord* new_top = obj + size;
    set_top(new_top);

    assert(is_object_aligned(new_top), "new top breaks alignment: " PTR_FORMAT, p2i(new_top));
    assert(is_object_aligned(obj),     "obj is not aligned: "       PTR_FORMAT, p2i(obj));

    return obj;
  } else {
    return NULL;
  }
}

inline void ShenandoahHeapRegion::adjust_alloc_metadata(ShenandoahAllocRequest::Type type, size_t size) {
  switch (type) {
    case ShenandoahAllocRequest::_alloc_shared:
    case ShenandoahAllocRequest::_alloc_shared_gc:
      // Counted implicitly by tlab/gclab allocs
      break;
    case ShenandoahAllocRequest::_alloc_tlab:
      _tlab_allocs += size;
      break;
    case ShenandoahAllocRequest::_alloc_gclab:
      _gclab_allocs += size;
      break;
    default:
      ShouldNotReachHere();
  }
}

inline void ShenandoahHeapRegion::increase_live_data_alloc_words(size_t s) {
  internal_increase_live_data(s);
}

inline void ShenandoahHeapRegion::increase_live_data_gc_words(size_t s) {
  internal_increase_live_data(s);
  if (ShenandoahPacing) {
    ShenandoahHeap::heap()->pacer()->report_mark(s);
  }
}

inline void ShenandoahHeapRegion::internal_increase_live_data(size_t s) {
  size_t new_live_data = Atomic::add(&_live_data, s, memory_order_relaxed);
#ifdef ASSERT
  size_t live_bytes = new_live_data * HeapWordSize;
  size_t used_bytes = used();
  assert(live_bytes <= used_bytes,
         "can't have more live data than used: " SIZE_FORMAT ", " SIZE_FORMAT, live_bytes, used_bytes);
#endif
}

inline void ShenandoahHeapRegion::clear_live_data() {
  Atomic::store(&_live_data, (size_t)0);
}

inline size_t ShenandoahHeapRegion::get_live_data_words() const {
  return Atomic::load(&_live_data);
}

inline size_t ShenandoahHeapRegion::get_live_data_bytes() const {
  return get_live_data_words() * HeapWordSize;
}

inline bool ShenandoahHeapRegion::has_live() const {
  return get_live_data_words() != 0;
}

inline size_t ShenandoahHeapRegion::garbage() const {
  assert(used() >= get_live_data_bytes(),
         "Live Data must be a subset of used() live: " SIZE_FORMAT " used: " SIZE_FORMAT,
         get_live_data_bytes(), used());

  size_t result = used() - get_live_data_bytes();
  return result;
}

inline HeapWord* ShenandoahHeapRegion::get_update_watermark() const {
  HeapWord* watermark = Atomic::load_acquire(&_update_watermark);
  assert(bottom() <= watermark && watermark <= top(), "within bounds");
  return watermark;
}

inline void ShenandoahHeapRegion::set_update_watermark(HeapWord* w) {
  assert(bottom() <= w && w <= top(), "within bounds");
  Atomic::release_store(&_update_watermark, w);
}

// Fast version that avoids synchronization, only to be used at safepoints.
inline void ShenandoahHeapRegion::set_update_watermark_at_safepoint(HeapWord* w) {
  assert(bottom() <= w && w <= top(), "within bounds");
  assert(SafepointSynchronize::is_at_safepoint(), "Should be at Shenandoah safepoint");
  _update_watermark = w;
}

#endif // SHARE_GC_SHENANDOAH_SHENANDOAHHEAPREGION_INLINE_HPP
