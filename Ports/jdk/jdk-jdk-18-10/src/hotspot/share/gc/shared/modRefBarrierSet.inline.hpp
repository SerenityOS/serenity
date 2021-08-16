/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_MODREFBARRIERSET_INLINE_HPP
#define SHARE_GC_SHARED_MODREFBARRIERSET_INLINE_HPP

#include "gc/shared/modRefBarrierSet.hpp"

#include "gc/shared/barrierSet.hpp"
#include "oops/compressedOops.inline.hpp"
#include "oops/objArrayOop.hpp"
#include "oops/oop.hpp"

class Klass;

// count is number of array elements being written
void ModRefBarrierSet::write_ref_array(HeapWord* start, size_t count) {
  HeapWord* end = (HeapWord*)((char*)start + (count*heapOopSize));
  // In the case of compressed oops, start and end may potentially be misaligned;
  // so we need to conservatively align the first downward (this is not
  // strictly necessary for current uses, but a case of good hygiene and,
  // if you will, aesthetics) and the second upward (this is essential for
  // current uses) to a HeapWord boundary, so we mark all cards overlapping
  // this write. If this evolves in the future to calling a
  // logging barrier of narrow oop granularity, like the pre-barrier for G1
  // (mentioned here merely by way of example), we will need to change this
  // interface, so it is "exactly precise" (if i may be allowed the adverbial
  // redundancy for emphasis) and does not include narrow oop slots not
  // included in the original write interval.
  HeapWord* aligned_start = align_down(start, HeapWordSize);
  HeapWord* aligned_end   = align_up  (end,   HeapWordSize);
  // If compressed oops were not being used, these should already be aligned
  assert(UseCompressedOops || (aligned_start == start && aligned_end == end),
         "Expected heap word alignment of start and end");
  write_ref_array_work(MemRegion(aligned_start, aligned_end));
}

template <DecoratorSet decorators, typename BarrierSetT>
template <typename T>
inline void ModRefBarrierSet::AccessBarrier<decorators, BarrierSetT>::
oop_store_in_heap(T* addr, oop value) {
  BarrierSetT *bs = barrier_set_cast<BarrierSetT>(barrier_set());
  bs->template write_ref_field_pre<decorators>(addr);
  Raw::oop_store(addr, value);
  bs->template write_ref_field_post<decorators>(addr, value);
}

template <DecoratorSet decorators, typename BarrierSetT>
template <typename T>
inline oop ModRefBarrierSet::AccessBarrier<decorators, BarrierSetT>::
oop_atomic_cmpxchg_in_heap(T* addr, oop compare_value, oop new_value) {
  BarrierSetT *bs = barrier_set_cast<BarrierSetT>(barrier_set());
  bs->template write_ref_field_pre<decorators>(addr);
  oop result = Raw::oop_atomic_cmpxchg(addr, compare_value, new_value);
  if (result == compare_value) {
    bs->template write_ref_field_post<decorators>(addr, new_value);
  }
  return result;
}

template <DecoratorSet decorators, typename BarrierSetT>
template <typename T>
inline oop ModRefBarrierSet::AccessBarrier<decorators, BarrierSetT>::
oop_atomic_xchg_in_heap(T* addr, oop new_value) {
  BarrierSetT *bs = barrier_set_cast<BarrierSetT>(barrier_set());
  bs->template write_ref_field_pre<decorators>(addr);
  oop result = Raw::oop_atomic_xchg(addr, new_value);
  bs->template write_ref_field_post<decorators>(addr, new_value);
  return result;
}

template <DecoratorSet decorators, typename BarrierSetT>
template <typename T>
inline bool ModRefBarrierSet::AccessBarrier<decorators, BarrierSetT>::
oop_arraycopy_in_heap(arrayOop src_obj, size_t src_offset_in_bytes, T* src_raw,
                      arrayOop dst_obj, size_t dst_offset_in_bytes, T* dst_raw,
                      size_t length) {
  BarrierSetT *bs = barrier_set_cast<BarrierSetT>(barrier_set());

  src_raw = arrayOopDesc::obj_offset_to_raw(src_obj, src_offset_in_bytes, src_raw);
  dst_raw = arrayOopDesc::obj_offset_to_raw(dst_obj, dst_offset_in_bytes, dst_raw);

  if (!HasDecorator<decorators, ARRAYCOPY_CHECKCAST>::value) {
    // Optimized covariant case
    bs->write_ref_array_pre(dst_raw, length,
                            HasDecorator<decorators, IS_DEST_UNINITIALIZED>::value);
    Raw::oop_arraycopy(NULL, 0, src_raw, NULL, 0, dst_raw, length);
    bs->write_ref_array((HeapWord*)dst_raw, length);
  } else {
    assert(dst_obj != NULL, "better have an actual oop");
    Klass* bound = objArrayOop(dst_obj)->element_klass();
    T* from = const_cast<T*>(src_raw);
    T* end = from + length;
    for (T* p = dst_raw; from < end; from++, p++) {
      T element = *from;
      if (oopDesc::is_instanceof_or_null(CompressedOops::decode(element), bound)) {
        bs->template write_ref_field_pre<decorators>(p);
        *p = element;
      } else {
        // We must do a barrier to cover the partial copy.
        const size_t pd = pointer_delta(p, dst_raw, (size_t)heapOopSize);
        // pointer delta is scaled to number of elements (length field in
        // objArrayOop) which we assume is 32 bit.
        assert(pd == (size_t)(int)pd, "length field overflow");
        bs->write_ref_array((HeapWord*)dst_raw, pd);
        return false;
      }
    }
    bs->write_ref_array((HeapWord*)dst_raw, length);
  }
  return true;
}

template <DecoratorSet decorators, typename BarrierSetT>
inline void ModRefBarrierSet::AccessBarrier<decorators, BarrierSetT>::
clone_in_heap(oop src, oop dst, size_t size) {
  Raw::clone(src, dst, size);
  BarrierSetT *bs = barrier_set_cast<BarrierSetT>(barrier_set());
  bs->write_region(MemRegion((HeapWord*)(void*)dst, size));
}

#endif // SHARE_GC_SHARED_MODREFBARRIERSET_INLINE_HPP
