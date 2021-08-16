/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_MODREFBARRIERSET_HPP
#define SHARE_GC_SHARED_MODREFBARRIERSET_HPP

#include "gc/shared/barrierSet.hpp"
#include "memory/memRegion.hpp"

class Klass;

class ModRefBarrierSet: public BarrierSet {
protected:
  ModRefBarrierSet(BarrierSetAssembler* barrier_set_assembler,
                   BarrierSetC1* barrier_set_c1,
                   BarrierSetC2* barrier_set_c2,
                   const BarrierSet::FakeRtti& fake_rtti)
    : BarrierSet(barrier_set_assembler,
                 barrier_set_c1,
                 barrier_set_c2,
                 NULL /* barrier_set_nmethod */,
                 fake_rtti.add_tag(BarrierSet::ModRef)) { }
  ~ModRefBarrierSet() { }

public:
  template <DecoratorSet decorators, typename T>
  inline void write_ref_field_pre(T* addr) {}

  template <DecoratorSet decorators, typename T>
  inline void write_ref_field_post(T *addr, oop new_value) {}

  // Causes all refs in "mr" to be assumed to be modified.
  virtual void invalidate(MemRegion mr) = 0;
  virtual void write_region(MemRegion mr) = 0;

  // Operations on arrays, or general regions (e.g., for "clone") may be
  // optimized by some barriers.

  // Below length is the # array elements being written
  virtual void write_ref_array_pre(oop* dst, size_t length,
                                   bool dest_uninitialized = false) {}
  virtual void write_ref_array_pre(narrowOop* dst, size_t length,
                                   bool dest_uninitialized = false) {}
  // Below count is the # array elements being written, starting
  // at the address "start", which may not necessarily be HeapWord-aligned
  inline void write_ref_array(HeapWord* start, size_t count);

 protected:
  virtual void write_ref_array_work(MemRegion mr) = 0;

 public:
  // The ModRef abstraction introduces pre and post barriers
  template <DecoratorSet decorators, typename BarrierSetT>
  class AccessBarrier: public BarrierSet::AccessBarrier<decorators, BarrierSetT> {
    typedef BarrierSet::AccessBarrier<decorators, BarrierSetT> Raw;

  public:
    template <typename T>
    static void oop_store_in_heap(T* addr, oop value);
    template <typename T>
    static oop oop_atomic_cmpxchg_in_heap(T* addr, oop compare_value, oop new_value);
    template <typename T>
    static oop oop_atomic_xchg_in_heap(T* addr, oop new_value);

    template <typename T>
    static bool oop_arraycopy_in_heap(arrayOop src_obj, size_t src_offset_in_bytes, T* src_raw,
                                      arrayOop dst_obj, size_t dst_offset_in_bytes, T* dst_raw,
                                      size_t length);

    static void clone_in_heap(oop src, oop dst, size_t size);

    static void oop_store_in_heap_at(oop base, ptrdiff_t offset, oop value) {
      oop_store_in_heap(AccessInternal::oop_field_addr<decorators>(base, offset), value);
    }

    static oop oop_atomic_xchg_in_heap_at(oop base, ptrdiff_t offset, oop new_value) {
      return oop_atomic_xchg_in_heap(AccessInternal::oop_field_addr<decorators>(base, offset), new_value);
    }

    static oop oop_atomic_cmpxchg_in_heap_at(oop base, ptrdiff_t offset, oop compare_value, oop new_value) {
      return oop_atomic_cmpxchg_in_heap(AccessInternal::oop_field_addr<decorators>(base, offset), compare_value, new_value);
    }
  };
};

template<>
struct BarrierSet::GetName<ModRefBarrierSet> {
  static const BarrierSet::Name value = BarrierSet::ModRef;
};

#endif // SHARE_GC_SHARED_MODREFBARRIERSET_HPP
