/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_PARALLEL_PSPARALLELCOMPACT_INLINE_HPP
#define SHARE_GC_PARALLEL_PSPARALLELCOMPACT_INLINE_HPP

#include "gc/parallel/psParallelCompact.hpp"

#include "gc/parallel/parallelScavengeHeap.hpp"
#include "gc/parallel/parMarkBitMap.inline.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "oops/access.inline.hpp"
#include "oops/compressedOops.inline.hpp"
#include "oops/klass.hpp"
#include "oops/oop.inline.hpp"

inline bool PSParallelCompact::is_marked(oop obj) {
  return mark_bitmap()->is_marked(obj);
}

inline double PSParallelCompact::normal_distribution(double density) {
  assert(_dwl_initialized, "uninitialized");
  const double squared_term = (density - _dwl_mean) / _dwl_std_dev;
  return _dwl_first_term * exp(-0.5 * squared_term * squared_term);
}

inline bool PSParallelCompact::dead_space_crosses_boundary(const RegionData* region,
                                                           idx_t bit) {
  assert(bit > 0, "cannot call this for the first bit/region");
  assert(_summary_data.region_to_addr(region) == _mark_bitmap.bit_to_addr(bit),
         "sanity check");

  // Dead space crosses the boundary if (1) a partial object does not extend
  // onto the region, (2) an object does not start at the beginning of the
  // region, and (3) an object does not end at the end of the prior region.
  return region->partial_obj_size() == 0 &&
    !_mark_bitmap.is_obj_beg(bit) &&
    !_mark_bitmap.is_obj_end(bit - 1);
}

inline bool PSParallelCompact::is_in(HeapWord* p, HeapWord* beg_addr, HeapWord* end_addr) {
  return p >= beg_addr && p < end_addr;
}

inline bool PSParallelCompact::is_in(oop* p, HeapWord* beg_addr, HeapWord* end_addr) {
  return is_in((HeapWord*)p, beg_addr, end_addr);
}

inline MutableSpace* PSParallelCompact::space(SpaceId id) {
  assert(id < last_space_id, "id out of range");
  return _space_info[id].space();
}

inline HeapWord* PSParallelCompact::new_top(SpaceId id) {
  assert(id < last_space_id, "id out of range");
  return _space_info[id].new_top();
}

inline HeapWord* PSParallelCompact::dense_prefix(SpaceId id) {
  assert(id < last_space_id, "id out of range");
  return _space_info[id].dense_prefix();
}

inline ObjectStartArray* PSParallelCompact::start_array(SpaceId id) {
  assert(id < last_space_id, "id out of range");
  return _space_info[id].start_array();
}

#ifdef ASSERT
inline void PSParallelCompact::check_new_location(HeapWord* old_addr, HeapWord* new_addr) {
  assert(old_addr >= new_addr || space_id(old_addr) != space_id(new_addr),
         "must move left or to a different space");
  assert(is_object_aligned(old_addr) && is_object_aligned(new_addr),
         "checking alignment");
}
#endif // ASSERT

inline bool PSParallelCompact::mark_obj(oop obj) {
  const int obj_size = obj->size();
  if (mark_bitmap()->mark_obj(obj, obj_size)) {
    _summary_data.add_obj(obj, obj_size);
    return true;
  } else {
    return false;
  }
}

template <class T>
inline void PSParallelCompact::adjust_pointer(T* p, ParCompactionManager* cm) {
  T heap_oop = RawAccess<>::oop_load(p);
  if (!CompressedOops::is_null(heap_oop)) {
    oop obj = CompressedOops::decode_not_null(heap_oop);
    assert(ParallelScavengeHeap::heap()->is_in(obj), "should be in heap");

    oop new_obj = cast_to_oop(summary_data().calc_new_pointer(obj, cm));
    assert(new_obj != NULL, "non-null address for live objects");
    // Is it actually relocated at all?
    if (new_obj != obj) {
      assert(ParallelScavengeHeap::heap()->is_in_reserved(new_obj),
             "should be in object space");
      RawAccess<IS_NOT_NULL>::oop_store(p, new_obj);
    }
  }
}

class PCAdjustPointerClosure: public BasicOopIterateClosure {
public:
  PCAdjustPointerClosure(ParCompactionManager* cm) {
    verify_cm(cm);
    _cm = cm;
  }
  template <typename T> void do_oop_nv(T* p) { PSParallelCompact::adjust_pointer(p, _cm); }
  virtual void do_oop(oop* p)                { do_oop_nv(p); }
  virtual void do_oop(narrowOop* p)          { do_oop_nv(p); }

  virtual ReferenceIterationMode reference_iteration_mode() { return DO_FIELDS; }
private:
  ParCompactionManager* _cm;

  static void verify_cm(ParCompactionManager* cm) NOT_DEBUG_RETURN;
};

#endif // SHARE_GC_PARALLEL_PSPARALLELCOMPACT_INLINE_HPP
